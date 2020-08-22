import java.io.*;

public class CompilationEngine {

    Tokenizer tokenizer;
    XMLLogger logger;

    SymbolTable table;
    VMWriter writer;

    String className;
    String subroutineName;

    boolean logFlag = false;

    int labelCount = 0;

    public CompilationEngine(File inputFile, File outputDir) {
        tokenizer = new Tokenizer(inputFile);
        logger = new XMLLogger(tokenizer, logFlag);
        table = new SymbolTable();
        writer = new VMWriter();
        try {
            compileClass();
            writer.writeVMFile(outputDir,className);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void compileClass() throws Exception {
        logger.log("<class>");
        eat(KeyWordType.CLASS);
        className = tokenizer.tokenName();
        eat();
        eat('{');
        compileClassVarDec();
        compileSubroutineDec();
        eat('}');
        logger.log("</class>");
    }

    private void compileClassVarDec() throws Exception {

        while (tokenizer.keyWord() == KeyWordType.STATIC || tokenizer.keyWord() == KeyWordType.FIELD) {
            logger.log("<classVarDec>");
            String kind = tokenizer.tokenName().toUpperCase();// kind = static | field
            eat(tokenizer.keyWord());
            String type = tokenizer.tokenName();
            eat();// log type
            String name = tokenizer.tokenName();
            eat();// log varName
            table.define(name, type, kind);
            while (tokenizer.tokenName().equals(",")) {
                eat(',');
                name = tokenizer.tokenName();
                eat();// varName
                table.define(name, type, kind);
            }
            eat(';');
            logger.log("</classVarDec>");
        }

    }

    private void compileSubroutineDec() throws Exception {

        while (tokenizer.keyWord() == KeyWordType.CONSTRUCTOR || tokenizer.keyWord() == KeyWordType.METHOD
                || tokenizer.keyWord() == KeyWordType.FUNCTION) {
            logger.log("<subroutineDec>");
            table.startSubroutine();// reset subroutine symbols
            switch (tokenizer.keyWord()) {
                case CONSTRUCTOR:
                    compileConstructor();
                    break;
                case METHOD: table.define("this","var","ARG");//this takes the place of argument 0 on stack
                case FUNCTION:
                    compileSubroutine();
                    break;
                default: break;
            }
            logger.log("</subroutineDec>");
        }

    }

    public void compileConstructor() throws Exception {
        eat("constructor");
        eat(className);// eat return type = class name
        subroutineName = "new";
        eat("new");
        eat('(');
        compileParameterList();
        eat(')');
        compileConstructorBody();
    }

    public void compileSubroutine() throws Exception {
        String subroutineType = tokenizer.tokenName();
        eat();// eat subroutine type
        eat();// eat return type.
        subroutineName = tokenizer.tokenName();
        eat();// eat subroutine name
        eat('(');
        compileParameterList();
        eat(')');
        compileSubroutineBody(subroutineType);
    }

    private void compileParameterList() throws Exception {
        logger.log("<parameterList>");
        while (!tokenizer.tokenName().equals(")")) {
            String type = tokenizer.tokenName();
            eat();// type
            String name = tokenizer.tokenName();
            eat();// varName
            table.define(name, type, "ARG");
            if (tokenizer.tokenName().equals(",")) {
                eat(',');
            } else {
                break;
            }
        }
        logger.log("</parameterList>");
    }

    private void compileConstructorBody() throws Exception {
        logger.log("<subroutineBody>");
        eat('{');
        while (tokenizer.tokenName().equals("var")) {
            compileVarDec();
        }
        writer.writeFunction(className + "." + subroutineName, table.varCount("VAR"));
        writer.writePush("CONST", table.varCount("FIELD"));
        writer.writeCall("Memory.alloc", 1);
        writer.writePop("POINTER", 0);
        compileStatements();
        eat('}');
        logger.log("</subroutineBody>");
    }

    private void compileSubroutineBody(String subroutineType) throws Exception {
        logger.log("<subroutineBody>");
        eat('{');
        while (tokenizer.tokenName().equals("var")) {
            compileVarDec();
        }
        writer.writeFunction(className + "." + subroutineName, table.varCount("VAR"));
        if(subroutineType.equals("method")){
            writer.writePush("ARG", 0);
            writer.writePop("POINTER", 0);
        }
        compileStatements();
        eat('}');
        logger.log("</subroutineBody>");
    }

    private void compileVarDec() throws Exception {
        logger.log("<varDec>");
        eat("var");
        String type = tokenizer.tokenName();
        eat();// log type
        String name = tokenizer.tokenName();
        eat();// log name
        table.define(name, type, "VAR");
        while (tokenizer.tokenName().equals(",")) {
            eat(',');
            name = tokenizer.tokenName();
            eat();// varName
            table.define(name, type, "VAR");
        }
        eat(';');
        logger.log("</varDec>");
    }

    private void compileStatements() throws Exception {
        logger.log("<statements>");
        boolean moreStatements = true;
        while (moreStatements && tokenizer.tokenType() == TokenType.KEYWORD) {
            switch (tokenizer.keyWord()) {
                case LET:
                    compileLetStatement();
                    break;
                case IF:
                    compileIfStatement();
                    break;
                case WHILE:
                    compileWhileStatement();
                    break;
                case DO:
                    compileDoStatement();
                    break;
                case RETURN:
                    compileReturnStatement();
                    break;
                default:
                    moreStatements = false;
                    break;
            }
        }
        logger.log("</statements>");
    }

    private void compileLetStatement() throws Exception {
        logger.log("<letStatement>");
        String destSegment = null;
        int destIndex = 0;

        eat("let");
        String destName = tokenizer.tokenName();
        eat();// log varName
        switch(table.kindOf(destName)){
            case "FIELD": destSegment = "THIS"; break;
            case "STATIC": destSegment = "STATIC"; break;
            case "VAR":destSegment = "LOCAL"; break;
            case "ARG": destSegment = "ARG"; break;
        }
        destIndex = table.indexOf(destName);
        if (tokenizer.tokenName().equals("[")) {
            writer.writePush(destSegment, destIndex);//push array base addr
            eat('[');
            compileExpression();//calculate array index and push on stack
            eat(']');
            writer.writeArithmetic("ADD");//destination = array base addr + array index
            eat('=');
            compileExpression();
            writer.writePop("TEMP", 0);// pop result of expression 2 in temp + now top of stack contains destination
                                       // array[expression1]
            writer.writePop("POINTER", 1);// pop destination in THAT pointer register
            writer.writePush("TEMP", 0);
            writer.writePop("THAT", 0);// pop expression 2 in RAM[a[expression 1]]
        } 
        else {
            eat('=');
            compileExpression();
            writer.writePop(destSegment,destIndex);
        }
        eat(';');
        logger.log("</letStatement>");
    }

    private void compileIfStatement() throws Exception {
        logger.log("<ifStatement>");
        eat("if");
        eat('(');
        compileExpression();
        eat(')');
        writer.writeArithmetic("NOT");
        String elseLabel = className + ".BEGINELSE" +labelCount++;
        writer.writeIf(elseLabel);
        eat('{');
        compileStatements();
        eat('}');
        String endElseLabel = className + ".ENDELSE" +labelCount++;
        writer.writeGoto(endElseLabel);
        if (tokenizer.keyWord() == KeyWordType.ELSE) {
            eat("else");
            writer.writeLabel(elseLabel);
            eat('{');
            compileStatements();
            eat('}');
            writer.writeLabel(endElseLabel);
        }
        else{
            writer.writeLabel(elseLabel);
            writer.writeLabel(endElseLabel);
        }
        logger.log("</ifStatement>");
    }

    private void compileWhileStatement() throws Exception {
        logger.log("<whileStatement>");
        eat("while");
        String whileBegin = className + ".WHILEBEGIN" + labelCount++;
        String whileEnd = className + ".ENDWHILE" + labelCount++;
        writer.writeLabel(whileBegin);
        eat("(");
        compileExpression();
        eat(")");
        writer.writeArithmetic("NOT");
        writer.writeIf(whileEnd);
        eat("{");
        compileStatements();
        eat("}");
        writer.writeGoto(whileBegin);
        writer.writeLabel(whileEnd);
        logger.log("</whileStatement>");
    }

    private void compileDoStatement() throws Exception {
        logger.log("<doStatement>");
        eat("do");
        compileSubroutineCall();
        eat(";");
        writer.writePop("TEMP", 0);
        logger.log("</doStatement>");
    }

    private void compileReturnStatement() throws Exception {
        logger.log("<returnStatement>");
        eat("return");
        if (tokenizer.symbol() == (';')) {
            writer.writePush("CONST", 0);// push dummy value for void subroutines
        } else {
            compileExpression();
        }
        eat(";");
        writer.writeReturn();
        logger.log("</returnStatement>");
    }

    private void compileExpression() throws Exception {
        logger.log("<expression>");
        compileTerm();
        while (tokenizer.tokenType() == TokenType.SYMBOL && !tokenizer.tokenName().equals(";") && !tokenizer.tokenName().equals(")") && !tokenizer.tokenName().equals(",")) {
            switch (tokenizer.tokenName()) {
                         // eat symbol: + - * / & | < > =
                case "+":eat();compileTerm();writer.writeArithmetic("ADD"); continue;
                case "-":eat();compileTerm();writer.writeArithmetic("SUB"); continue;
                case "*":eat();compileTerm();writer.writeCall("Math.multiply",2); continue;
                case "/":eat();compileTerm();writer.writeCall("Math.divide",2); continue;
                case "&":eat();compileTerm();writer.writeArithmetic("AND"); continue;
                case "|":eat();compileTerm();writer.writeArithmetic("OR"); continue;
                case "<":eat();compileTerm();writer.writeArithmetic("LT"); continue;
                case ">":eat();compileTerm();writer.writeArithmetic("GT"); continue;
                case "=":eat();compileTerm();writer.writeArithmetic("EQ"); continue;      
            }
            break;
        }
        logger.log("</expression>");
    }

    private void compileTerm() throws Exception {
        logger.log("<term>");
        compileTermBody();
        logger.log("</term>");
    }

    private void compileTermBody() throws Exception {

        if (tokenizer.tokenName().startsWith("(")) {
            eat("(");
            compileExpression();
            eat(")");
            return;
        }
        switch (tokenizer.tokenName()) {
            case "-":
                eat("-");
                compileTerm();
                writer.writeArithmetic("NEG");
                return;
            case "~":
                eat("~");
                compileTerm();
                writer.writeArithmetic("NOT");
                return;
            default:
                break;
        }
        switch (tokenizer.tokenType()) {
            case INT_CONST:
                writer.writePush("CONST", tokenizer.intVal());
                eat();
                return;
            case STRING_CONST:
                writer.writePush("CONST", tokenizer.stringVal().length());
                writer.writeCall("String.new", 1);
                for(char c : tokenizer.stringVal().toCharArray()){
                    writer.writePush("CONST", (int)c);
                    writer.writeCall("String.appendChar", 2);
                }
                eat();
                return;
            case KEYWORD:
                switch(tokenizer.keyWord()){
                    case THIS: writer.writePush("POINTER", 0); eat(); return;
                    case TRUE: writer.writePush("CONST",0);writer.writeArithmetic("NOT"); eat();return;
                    case FALSE: writer.writePush("CONST", 0);eat(); return;
                    case NULL: writer.writePush("CONST", 0); eat();return;
                    default: break;
                }
                eat();
                return;// allowed keywords: true, false, null, this
            case IDENTIFIER:
                    
                switch (tokenizer.tokenNameAhead(1)) {
                    case "[":
                    String kind = table.kindOf(tokenizer.tokenName());
                    String segment = null;
                    switch(kind){
                        case "FIELD": segment = "THIS"; break;
                        case "STATIC": segment = "STATIC"; break;
                        case "VAR": segment = "LOCAL"; break;
                        case "ARG": segment = "ARG"; break;
                    }
                    int index = table.indexOf(tokenizer.tokenName());

                        writer.writePush(segment, index);//push arr pointer
                        eat();
                        eat("[");
                        compileExpression();
                        eat("]");
                        writer.writeArithmetic("ADD");//add array base addr to offset i
                        writer.writePop("POINTER", 1);
                        writer.writePush("THAT", 0);
                        return;// first eat varName before eating "["
                    case ".":
                        compileSubroutineCall();
                        return;
                    case "(":
                        compileSubroutineCall();
                        return;
                    default:
                    kind = table.kindOf(tokenizer.tokenName());
                    segment = null;
                    switch(kind){
                        case "FIELD": segment = "THIS"; break;
                        case "STATIC": segment = "STATIC"; break;
                        case "VAR": segment = "LOCAL"; break;
                        case "ARG": segment = "ARG"; break;
                    }
                    index = table.indexOf(tokenizer.tokenName());

                    writer.writePush(segment, index);
                    eat();// eat varName
                }
                return;

            default:
                System.out.println("unknown term in compileTermBody()");
                
        }

    }

    private void compileSubroutineCall() throws Exception {
        
        if (tokenizer.tokenNameAhead(1).equals("(")) {
            String methodName = tokenizer.tokenName();
            eat();//eat name
            writer.writePush("POINTER", 0);
            eat("(");
            int nArgs = compileExpressionList();
            eat(")");
            writer.writeCall(className+"."+methodName, nArgs+1);// push nArgs + this pointer on stack
            return;
        }
        if (tokenizer.tokenNameAhead(1).equals(".")) {
            if(table.kindOf(tokenizer.tokenName()).equals("NONE")){
                String className = tokenizer.tokenName();
                eat();//eat className
                eat(".");
                String funcName = tokenizer.tokenName();
                eat();//eat subroutineName
                eat("(");
                int nArgs = compileExpressionList();
                eat(")");
                writer.writeCall(className+"."+funcName, nArgs);
            }
            else if(!tokenizer.tokenNameAhead(2).equals("new")){
                String segment = null;
                switch(table.kindOf(tokenizer.tokenName()).toUpperCase()){
                    case "FIELD": segment = "THIS"; break;
                    case "STATIC": segment = "STATIC"; break;
                    case "VAR":segment = "LOCAL"; break;
                    case "ARG": segment = "ARG"; break;
                }
                writer.writePush(segment, table.indexOf(tokenizer.tokenName()));
                String identifierClass = table.typeOf(tokenizer.tokenName());
                eat();//eat instance variable name
                eat(".");
                String methodName = tokenizer.tokenName();
                eat();//eat method name
                eat("(");
                int nArgs = compileExpressionList();
                eat(")");
                writer.writeCall(identifierClass+"."+methodName, nArgs+1);// push nArgs + this pointer on stack
            }
            else{
                eat();//eat instance variable name
                eat(".");
                String methodName = tokenizer.tokenName();
                eat();//eat "new"
                eat("(");
                int nArgs = compileExpressionList();
                eat(")");
                writer.writeCall(className+"."+methodName, nArgs);

            }
        }

    }

    private int compileExpressionList() throws Exception {
        int nArgs = 0;
        logger.log("<expressionList>");
        if (!tokenizer.tokenName().equals(")")) {
            compileExpression();
            nArgs++;
            while (tokenizer.tokenName().equals(",")) {
                eat(",");
                compileExpression();
                nArgs++;
            }
        }
        logger.log("</expressionList>");
        return nArgs;
    }

    private void eat(String s) throws Exception {
        logger.logToken();
        check(s);
        tokenizer.advance();
    }

    private void eat(char c) throws Exception {
        logger.logToken();
        check(c);
        tokenizer.advance();
    }

    private void eat() {
        logger.logToken();
        tokenizer.advance();
    }

    private void eat(KeyWordType keyword) throws Exception {
        logger.logToken();
        check(keyword);
        tokenizer.advance();
    }

    private void check(KeyWordType... keywords) throws Exception {
        for (KeyWordType keyword : keywords) {
            if (keyword != tokenizer.keyWord()) {
                throw new Exception();
            }
        }
    }

    private void check(char c) throws Exception {
        if (c != tokenizer.symbol()) {
            throw new Exception();
        }

    }

    private void check(String s) throws Exception {
        if (!s.equals(tokenizer.tokenName())) {
            throw new Exception();
        }

    }

}
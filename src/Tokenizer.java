import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class Tokenizer {

    private BufferedReader reader;

    private ArrayList<String> tokens = new ArrayList<String>();
    private int tokenIndex = 0;

    private ArrayList<String> symbols = new ArrayList<String>(Arrays.asList(" ","{","}","(",")","[","]",".",",",";","+","-","*","/","&","|","<",">","~","="));
    private ArrayList<String> keywords = new ArrayList<String>(Arrays.asList("class","constructor","method","function","field","static","var","int","char","boolean","void","true","false","null","this","let","do","if","else","while","return"));

    public Tokenizer(File file) {
        ArrayList<String> lines = new ArrayList<String>();
        try {
            FileReader r = new FileReader(file);
            reader = new BufferedReader(r);
            String line = reader.readLine(); 
            while(line != null){
                int commentIndex = line.indexOf("//");
                if(commentIndex > -1)line = (line.substring(0, commentIndex));
                line = line.replace("\t", " ");
                lines.add(line.stripTrailing());
                line = reader.readLine();
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        lines = commentCleaner(lines);

        for(String line : lines){
            splitWithSymbols(line,tokens);
        }
        for(String token : tokens) System.out.println(token);

    }

    private ArrayList<String> commentCleaner(ArrayList<String> lines){
        ArrayList<String> newLines = new ArrayList<String>();
        boolean skip = false;
        for(String line : lines){
            StringBuilder oLine = new StringBuilder(100);
            for(int x = 0; x < line.length(); x++){
                if(skip){
                    if(line.charAt(x) == '*' && x < line.length()-1 && line.charAt(x+1) == '/'){
                        skip = false;
                        x++;
                    }
                    continue;
                }
                if(line.charAt(x) == '/' && x < line.length()-1 && line.charAt(x+1) == '*'){
                    skip = true;
                }
                else{
                    oLine.append(line.charAt(x));
                }
            }
            newLines.add(oLine.toString());
        }
        return newLines;
    }

    public boolean hasMoreTokens() {
        return tokenIndex < (tokens.size()-1);
    }

    public void advance() {
        tokenIndex++;
    }

    public TokenType tokenType(){

        String currentToken = tokens.get(tokenIndex);

        char firstChar = currentToken.charAt(0);

        if(symbols.contains(currentToken)) return TokenType.SYMBOL;
        if(firstChar == '\"') return TokenType.STRING_CONST;
        if(Character.isDigit(firstChar)) return TokenType.INT_CONST;
        if(keywords.contains(currentToken)) return TokenType.KEYWORD;
        else{return TokenType.IDENTIFIER;}
    }

    public KeyWordType keyWord(){
        switch(tokens.get(tokenIndex)){
            case "class" : return KeyWordType.CLASS;
            case "constructor" : return KeyWordType.CONSTRUCTOR;
            case "method":return KeyWordType.METHOD;
            case "function": return KeyWordType.FUNCTION;
            case "field":return KeyWordType.FIELD;
            case "static":return KeyWordType.STATIC;
            case "var":return KeyWordType.VAR;
            case "int":return KeyWordType.INT;
            case "char":return KeyWordType.CHAR;
            case "boolean":return KeyWordType.BOOLEAN;
            case "void":return KeyWordType.VOID;
            case "true":return KeyWordType.TRUE;
            case "false":return KeyWordType.FALSE;
            case "null":return KeyWordType.NULL;
            case "this":return KeyWordType.THIS;
            case "let":return KeyWordType.LET;
            case "do":return KeyWordType.DO;
            case "if":return KeyWordType.IF;
            case "else":return KeyWordType.ELSE;
            case "while":return KeyWordType.WHILE;
            case "return":return KeyWordType.RETURN;
        }

        return null;
    }
    public char symbol(){
        return tokens.get(tokenIndex).charAt(0);
    }

    public String identifier(){
        return tokens.get(tokenIndex);
    }

    public int intVal(){
        return Integer.parseInt(tokens.get(tokenIndex));
    }

    public String stringVal(){
        String currentToken = tokens.get(tokenIndex);
        return currentToken.substring(1,currentToken.length()-1);
    }

    public String tokenName(){
        return tokens.get(tokenIndex);
    }

    public String tokenNameAhead(int amount){
        return tokens.get(tokenIndex+amount);
    }

    private void splitWithSymbols(String line, ArrayList<String> dest){

        int previousIndex = 0;
        for(int i = 0; i < line.length();i++){
            char c = line.charAt(i);
            if(symbols.contains(""+c) || c =='\"'){
                   
                    if(i > previousIndex)dest.add(line.substring(previousIndex,i));
                    //"this is string", don't split text between quotes
                    if(c == '\"'){
                        previousIndex = i++;
                        while(line.charAt(i) != '\"')i++;           
                        dest.add(line.substring(previousIndex,i+1));
                        previousIndex = i+1;
                        continue;
                    }
                    if(c!=' ')dest.add(""+c);
                    previousIndex = i+1;
                }
            }
        }
    }


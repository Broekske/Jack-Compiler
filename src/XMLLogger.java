public class XMLLogger {

    int nestingLevel = 0;

    Tokenizer tokenizer;

    boolean logFlag = false;

    public XMLLogger(Tokenizer tokenizer, boolean logFlag){
        this.tokenizer = tokenizer;
    }

    public void logToken(){
        if(logFlag){
        String content = tokenizer.tokenName();
        String label = tokenizer.tokenType().name().toLowerCase();
        if(label.equals("string_const")){
            label = "stringConstant";
            content = content.replace('\"', (char)0x0);
        }
        if(label.equals("int_const")){
            label = "integerConstant";
        }
        logIndent();
        System.out.println("<"+label+"> "+content+" </"+label+">");
    }
    }

    public void log(String line){
        if(logFlag){
        if(line.startsWith("</")){
            nestingLevel--;
        }
        logIndent();
        System.out.println(line);
        if(!line.startsWith("</")){
            nestingLevel++;
        }
    }

    }

    private void logIndent(){
        if(logFlag){
        String indent= "";
        for(int i=0;i<nestingLevel;i++){
            indent = indent + "    ";
        }
        System.out.print(indent);
    }
    }
    
}
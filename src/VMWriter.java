import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;

public class VMWriter {

    ArrayList<String> vmLines;

    Hashtable<String,String> consts;

    public VMWriter(){
        vmLines = new ArrayList<String>();
        consts = new Hashtable<String,String>();
        consts.put("CONST","constant");
        consts.put("ARG","argument");
        consts.put("LOCAL","local");
        consts.put("STATIC","static");
        consts.put("THIS","this");
        consts.put("THAT","that");
        consts.put("POINTER","pointer");
        consts.put("TEMP","temp");
    }

    public void writeVMFile(File dir, String className){
        try{
        String fileName = dir.getAbsolutePath()+"/"+className+".vm";
        FileWriter fileWriter = new FileWriter(fileName);
        for(String line : vmLines){
            fileWriter.write(line);
            fileWriter.write("\n");
        }
        fileWriter.close();
        }
        catch(IOException e){
            e.printStackTrace();
        }
    }
    
    public void writePush(String segment,int index){   
        String line = "push "+ consts.get(segment) + " " + index; 
        System.out.println(line);
        vmLines.add(line);
    }

    public void writePop(String segment, int index){
        String line = "pop "+ consts.get(segment) + " " + index; 
        System.out.println(line);
        vmLines.add(line);
    }

    public void writeFunction(String name, int nLocals){
        String line = "function "+ name + " " + nLocals;
        System.out.println(line);
        vmLines.add(line);
    }

    public void writeCall(String name, int nArgs){
        String line = "call "+ name + " " + nArgs;
        System.out.println(line);
        vmLines.add(line);
    }

    public void writeReturn(){
        String line = "return";
        System.out.println(line);
        vmLines.add(line);
    }

    public void writeArithmetic(String command){
        String line = command.toLowerCase();
        System.out.println(line);
        vmLines.add(line);
    }

    public void writeLabel(String label){
        String line = "label "+label;
        System.out.println(line);
        vmLines.add(line);
    }

    public void writeGoto(String label){
        String line = "goto "+label;
        System.out.println(line);
        vmLines.add(line);
    }

    public void writeIf(String label){
        String line = "if-goto "+ label;
        System.out.println(line);
        vmLines.add(line);
    }

    public void close(){

    }

}
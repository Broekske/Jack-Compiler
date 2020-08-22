import java.io.File;
import java.util.ArrayList;

public class JackCompiler {

    public static void main(String[] args) throws Exception {
        ArrayList<File> jackFiles = new ArrayList<File>();
        String dirPath = "/home/kobedb/Projects/jack/HackOS";
        File dir = new File(dirPath);

        for(File file : dir.listFiles()){
            if(file.getName().endsWith(".jack")){
                jackFiles.add(file);
            }
        }
        for(File file : jackFiles){
            CompilationEngine engine = new CompilationEngine(file, dir);
        }
	
    }
}

import ij.*;
import java.io.File;
import ij.plugin.*;
public class Orange_Hot implements PlugIn {
    public void run(String arg) {
        String lutDir = Prefs.getHomeDir()+File.separator+"lut"+File.separator;
        IJ.run("LUT... ", "open="+"'"+lutDir+"Orange Hot.lut"+"'");
    }
} 




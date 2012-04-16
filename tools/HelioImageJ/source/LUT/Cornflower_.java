import ij.*;
import java.io.File;
import ij.plugin.*;
public class Cornflower_ implements PlugIn {
    public void run(String arg) {
        String lutDir = Prefs.getHomeDir()+File.separator+"lut"+File.separator;
        IJ.run("LUT... ", "open="+"'"+lutDir+"Cornflower.lut"+"'");
    }
} 


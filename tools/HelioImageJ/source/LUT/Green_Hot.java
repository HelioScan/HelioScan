import ij.*;
import java.io.File;
import ij.plugin.*;
public class Green_Hot implements PlugIn {
    public void run(String arg) {
        String lutDir = Prefs.getHomeDir()+File.separator+"lut"+File.separator;
        IJ.run("LUT... ", "open="+"'"+lutDir+"Green Hot.lut"+"'");
    }
} 




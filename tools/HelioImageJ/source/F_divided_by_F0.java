import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import java.io.File;

public class F_divided_by_F0 implements PlugIn {

	public void run(String arg) {
		
			GenericDialog gd = new GenericDialog("F÷F0");
	gd.addNumericField("How many frames to average?",6,0);
		gd.showDialog();
       	if (gd.wasCanceled()) return ;
	int ageFrames=  gd.getNextChoiceIndex();

  String path= System.getProperty("user.dir")+File.separator+"macros"+File.separator;
	String macroString = "run="+path+"FdivF0.txt how="+ageFrames;
		IJ.run("Run... ",macroString  );
	}

}

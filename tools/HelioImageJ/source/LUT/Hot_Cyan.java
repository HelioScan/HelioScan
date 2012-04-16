import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Hot_Cyan implements PlugIn {

	public void run(String arg) {
		IJ.run("LUT... ", "open=C:\\ImageJ\\lut\\Hot_Cyan.lut");
	}

}

import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.plugin.PlugIn;
import ij.measure.*;


public class HotKey_D implements PlugIn {

	public void run(String arg) {
		ImagePlus imp1, imp2;
		ImageWindow winimp1, winimp2;
		boolean altKey = IJ.altKeyDown();
		int[] wList = WindowManager.getIDList();
		if (wList==null) 
			{
			IJ.noImage();
			return ;
        			}
		imp1 = WindowManager.getCurrentImage();
		winimp1 = imp1.getWindow();

		String Ch1fileName = imp1.getTitle();
		String lutDir = System.getProperty("user.dir")+File.separator+"macro"+File.separator;
		Roi roi = imp1.getRoi();
		imp1.setRoi(roi);
		//if(roi==null) IJ.run("Duplicate...", "title='"+Ch1fileName+" - copy' duplicate");

		 if (!altKey)  
			{IJ.run("Duplicate...", "title='"+Ch1fileName+" - copy' duplicate");
			IJ.run("Restore Selection");
			imp1.setRoi(roi);
			}		
		else
			{
			IJ.run("Duplicate...", "title='"+Ch1fileName+" - clone' duplicate");
			IJ.run("Restore Selection");
			IJ.run("Clear Outside", "stack");
			imp1.setRoi(roi);
			}
	}

}

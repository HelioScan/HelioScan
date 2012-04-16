import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.io.*;



public class Open_Scaled_Image implements PlugIn {

	public void run(String arg) {

		double scale = 0.1;	
		GenericDialog gd = new GenericDialog("Open Scaled Image");
		gd.addNumericField("Scale factor",scale,3);
		gd.showDialog();
        		if (gd.wasCanceled())     return ;
	       	scale= gd.getNextNumber();
		IJ.showStatus("1");
		OpenDialog od = new OpenDialog("Select Image...", "");
	            	String directory = od.getDirectory();
	            	String name = od.getFileName();
	            	if (name==null)
                	return;
		String path = directory + name;

		ImagePlus imp1 = (new Opener()).openImage(path); 	
		ImageProcessor ip1 = imp1.getProcessor();
		ip1.setInterpolate(true);
		ip1.scale(scale ,scale);

		int width = (int)((double)imp1.getWidth()*scale);
		int height = (int)((double)imp1.getHeight()*scale);
		
		int xOffset = (int)(((double)imp1.getWidth()/2)-(width/2));
		int yOffset = (int)(((double)imp1.getHeight()/2)-(height/2));

		ip1.setRoi(xOffset, yOffset,width,height);
		
		ip1=ip1.crop();	
		new ImagePlus("Scaled Image",ip1).show();
		
	}

}

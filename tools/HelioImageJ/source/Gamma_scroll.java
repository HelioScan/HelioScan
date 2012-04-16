
import java.awt.*;
import java.awt.image.*;
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.measure.*;
import java.awt.event.*;


public class Gamma_scroll implements PlugIn {

 	protected GenericRecallableDialog gd;
protected float cur_level;

	public void run(String arg) {
ImagePlus imp =  WindowManager.getCurrentImage();
       if (imp==null) {
     	  IJ.error("No images are open.");
     	return;
    }

IJ.run("Copy");
IJ.setPasteMode("Copy");
IJ.run("Duplicate...", "title='Gamma result'");
//make gamma plot
    ImageProcessor plot = new ShortProcessor(128, 64);
    	 for (int y=0; y<64; y++) {
          		  for (int x=0; x<128; x++) {
			plot.putPixel(x,y,0);}}
new ImagePlus("Gamma", plot).show();	


//int windcnt  = WindowManager.getWindowCount();
//ImagePlus imp2 =  WindowManager.getImage(windcnt);
//ImageProcessor ip2 = imp2.getProcessor();
//ImageProcessor ip = imp.getProcessor();

float cur_level = 1;
gd = new GenericRecallableDialog ("Gamma", IJ.getInstance());
	gd.addScrollBar("Gamma", cur_level, 1, 0.10, 5.00); 					
		gd.showDialog(); 
	
		float  prev_level;
		while (!(gd.wasCanceled())) { // continuously runs through loop
	  	
	  		if (gd.getFocusOwner() == null) 
				IJ.wait(500); 
	 	  	else {
	  			prev_level = cur_level;
		  		cur_level = (float)(gd.getScrollBarValue(0));
		  		if (prev_level != cur_level) {
					double value = (double)cur_level;

//reset imp2 to be imp so that new gamma can be reapplied...
					IJ.run("Paste");
					//ip2 = ip;

//set new gamma value

				String gammaString = "value="+IJ.d2s(value);		
//apply new gamma
	//	ip2.gamma(value);
	//imp2 = imp2("Gamma", ip2);
	//mp2.updateAndDraw();
		IJ.run("Gamma...", gammaString );
					}
			  }
					}

		
	}
}

import java.io.*;
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
String title = imp.getTitle();
IJ.run("Copy");
IJ.setPasteMode("Copy");
IJ.run("Duplicate...", "title='Gamma result'");
ImagePlus gammaimp =  WindowManager.getCurrentImage();
String gammatitle= gammaimp.getTitle();
//make gamma plot

ImageProcessor plot = new FloatProcessor(128,64);
int y = 64;
int x = 128;
 for (x=0; x<128; x++) {
	 for (y=0; y<64; y++) {
		plot.putPixel(x,y,255);}}
	
	 for (y=0; y<64; y++) {
  		x=128-(y*2); 
			plot.putPixel(x,y,0);
		}

new ImagePlus("Gamma", plot).show();	
ImagePlus gammaplotimp =  WindowManager.getCurrentImage();

IJ.selectWindow(gammatitle);

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
				IJ.selectWindow(gammatitle);	
					IJ.run("Paste");
					//ip2 = ip;
//reset plot

 for (x=0; x<128; x++) {
	 for (y=0; y<64; y++) {
		plot.putPixel(x,y,255);}}
double newx, newy;

    	 for (x=0; x<128; x++) {
		newx = (((double)x)/128);
		
		//newy = (64*Math.pow(newx, value));
		//y = (int)newy;
		//y=(int)(64*value*newx);
		y=(int)(64-(64*Math.pow(newx, value)));
		plot.putPixel(x,y,0);

}
	
gammaplotimp.updateAndDraw();
//set new gamma value

				String gammaString = "value="+IJ.d2s(value);		
//apply new gamma
		IJ.run("Gamma...", gammaString );
					}
		

	  }

					}

		
	}
}

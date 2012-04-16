//package ij.plugin.frame;
import java.awt.*;
import java.awt.image.*;
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;

/** Walter O'Dell PhD,  wodell@rochester.edu,   11/6/02
  * Adjusts window and level of the active image in the manner similar
  * to that implemented on medical image terminals -- replacement for 
  * brightness/contrast adjustment.
	*	Works on Stacks color images also */
public class WindowLevelAdjuster_ implements PlugIn, Runnable {
  protected ImagePlus imp; 
  protected ImageProcessor[] ips; // all for stack
  protected int nSlices;
 	protected GenericRecallableDialog gd;
  protected String Label;
	protected float cur_window, cur_level, defaultWindow, defaultLevel;

	public void run() {
    Label = "Window/Level Adjuster";
		run(Label);
	}
			
	public void run(String arg) {
		if (arg.equals("about"))  {showAbout(); return;}
    imp = WindowManager.getCurrentImage();
    if (imp==null) {
     	IJ.write("Error: no image defined..");
     	return;
    }
    Label = "Window/Level Adjuster";
    ImageStack stack = imp.getStack();
    nSlices = stack.getSize();
    ips = new ImageProcessor[nSlices];
		initWindowAndLevel();

 		gd = new GenericRecallableDialog(
 				"bammAdjuster", IJ.getInstance());
		gd.addScrollBar("Window", cur_window, 1, 1.0, 2*cur_window); 
							   // curVal, ndigits, min,  max
		gd.addScrollBar("Level", cur_level, 1, cur_level-cur_window, 
										 				cur_level+cur_window); 
		gd.addButtons("Auto W/L", "About W/L");
		gd.showDialog(); 
    float prev_window, prev_level;
		while (!(gd.wasCanceled())) { // continuously runs through loop
	  	  // if this window does not have focus, don't waste CPU time on it
	  	if (gd.getFocusOwner() == null) IJ.wait(500); 
	 	  else {
	  		prev_window = cur_window;
	  		prev_level = cur_level;
	  		cur_window = (float)(gd.getScrollBarValue(0));
	  		cur_level = (float)(gd.getScrollBarValue(1));
	  		if (prev_window != cur_window  || prev_level != cur_level) 
					for (int i=1; i<=nSlices; i++) 
						setWindowAndLevel(ips[i-1]);
				if (gd.getButtonValue(0)) reset();
				if (gd.getButtonValue(1)) showAbout();
		  }
		}// end while
	}
	
	/** run in a new thread -- when the plugin is executed from the main
	  * menu bar, it is already running in a new thread, so this procedure is
	  * not needed in those instances.  Only needed when invoked directly from 
	  * another procedure */
	public void runInThread(String arg) {
		Thread thread = new Thread(this, "WindowLevelAdjuster");
		//thread.setPriority(thread.getPriority()-1);
		thread.start(); // automatically invokes the run(void) routine
	}

  void showAbout()  {
		IJ.showMessage("About "+Label+  
		":\n This performs on-the-fly window and level adjustment of the image \n"+
		" in the manner typically implemented on an medical image console.\n"+ 
		" Replacement for Brightness/Contrast adjuster. Note: keyboard arrow keys\n"+
		" affect slider values for the last slider to have been selected by the mouse."+
		" Works on Stacks color images also."
		);
  } // end showAbout()
  
  public String toString() {
 		return (" "+Label);
 	}
	
	void initWindowAndLevel() {
		// find min/max of image/entire stack
    float imgMax = -Float.MAX_VALUE, imgMin = Float.MAX_VALUE;
    int type = imp.getType();
    Rectangle rect2 = null;
    ImageStack stack = imp.getStack();

		for (int i=1; i<=nSlices; i++) {
    	if (i==1) {
    		ips[0] = imp.getProcessor();
    		rect2 = ips[0].getRoi();
    		//imp.killRoi();  // else only windows the roi region
    	}
    	else
				ips[i-1] = stack.getProcessor(i);
			imgMax = Math.max(imgMax, getRegionMax(ips[i-1], rect2));
			imgMin = Math.min(imgMin, getRegionMin(ips[i-1], rect2));
 			if (type==ImagePlus.COLOR_RGB || type==ImagePlus.GRAY8) 
 				ips[i-1].snapshot(); // need this in order to reset later
		}
    // window = max-min;  level = min + window/2
		cur_window = defaultWindow = (imgMax-imgMin)/2f;
		cur_level = defaultLevel = (imgMin + imgMax)/2f;
		for (int i=1; i<=nSlices; i++) 
			setWindowAndLevel(ips[i-1]);
	}
	
	void setWindowAndLevel(ImageProcessor ip) {
		double min = cur_level - cur_window;
		double max = cur_level + cur_window;
		ip.setMinAndMax(min, max);
		imp.updateAndDraw();
	}
	
	public void reset() {
	  int type = imp.getType();
 		if (type==ImagePlus.COLOR_RGB || type==ImagePlus.GRAY8)
			for (int i=1; i<=nSlices; i++) 
				ips[i-1].reset();
		if ((ips[0] instanceof ShortProcessor) ||
			  (ips[0] instanceof FloatProcessor) ||
			  (ips[0] instanceof ByteProcessor)) 
			for (int i=1; i<=nSlices; i++) 
				ips[i-1].resetMinAndMax();
		cur_window = defaultWindow;
		cur_level = defaultLevel;
		gd.setScrollBarValue(0, cur_window);
		gd.setScrollBarValue(1, cur_level);
		for (int i=1; i<=nSlices; i++) 
			setWindowAndLevel(ips[i-1]);
	}

	/** these really should be made a function in the ImageProcessor class */
	public float getRegionMax(ImageProcessor ip, Rectangle roiRect) {
		int xcen = roiRect.x+ roiRect.width/2;
		int ycen = roiRect.y+ roiRect.height/2;
		int rangex = roiRect.width;
		int rangey = roiRect.height;
		int width = ip.getWidth();
		int height = ip.getHeight();
		float new_val, cur_max = -Float.MAX_VALUE;
		int y, x, y_at, x_at;
    for (y=ycen-rangey/2; y<=ycen+rangey/2; y++) {
      y_at = Math.max(y,0);  
      y_at = Math.min(y_at,height-1);  
      for (x=xcen-rangex/2; x<=xcen+rangex/2; x++) {
        x_at = Math.max(x,0);  
        x_at = Math.min(x_at,width-1);  
        new_val = ip.getPixelValue(x_at, y_at);
		  	if (new_val > cur_max)
		     	cur_max = new_val;
		  }
		}
		return cur_max;
	} 

	public float getRegionMin(ImageProcessor ip, Rectangle roiRect) {
		return (getRegionMin(ip, roiRect, -Float.MAX_VALUE));
	}
	public float getRegionMin(ImageProcessor ip, Rectangle roiRect, float minthresh) {
		int xcen = roiRect.x+ roiRect.width/2;
		int ycen = roiRect.y+ roiRect.height/2;
		int rangex = roiRect.width;
		int rangey = roiRect.height;
		int width = ip.getWidth();
		int height = ip.getHeight();
		float new_val, cur_min = Float.MAX_VALUE;
		int y, x, y_at, x_at;
    for (y=ycen-rangey/2; y<=ycen+rangey/2; y++) {
      y_at = Math.max(y,0);  
      y_at = Math.min(y_at,height-1);  
      for (x=xcen-rangex/2; x<=xcen+rangex/2; x++) {
        x_at = Math.max(x,0);  
        x_at = Math.min(x_at,width-1);  
        new_val = ip.getPixelValue(x_at, y_at);
		    if (new_val < cur_min && new_val >= minthresh)
		     	cur_min = new_val;
		  }
		}
		return cur_min;
	} 
	
} // WindowLevelAdjuster class

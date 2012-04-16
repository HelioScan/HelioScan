import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.measure.*;

/** Splits an RGB image or stack into three 8-bit grayscale images or stacks. */
//Mofified to split single slice RGB to a 3 slice stack

public class RGB_to_Montage implements PlugInFilter {
    ImagePlus imp;

    public int setup(String arg, ImagePlus imp) {
        this.imp = imp;
        return DOES_RGB+NO_UNDO;
    }

    public void run(ImageProcessor ip) {
        splitStack(imp);
    }

    public void splitStack(ImagePlus imp) {
         	int w = imp.getWidth();
         	int h = imp.getHeight();
               	ImageStack rgbStack = imp.getStack();
	int n = rgbStack.getSize();
	if (n>1)
		{IJ.showMessage("Does not work with stacks. Use Image/Colour/RGB split"); 
		return;}  
	Calibration cal = imp.getCalibration();

	ImageStack splitStack = new ImageStack(w,h);
         	ColorProcessor cp;
	//processors for each channel
	ColorProcessor rcp= new ColorProcessor (w,h);
	ColorProcessor gcp= new ColorProcessor (w,h);
	ColorProcessor bcp= new ColorProcessor (w,h);

  	//byte arrays to pull each channel from original image
	byte[] r,g,b;
             	r = new byte[w*h];
             	g = new byte[w*h];
             	b = new byte[w*h];
             	cp = (ColorProcessor)rgbStack.getProcessor(1);
	cp.getRGB(r,g,b);
	//create image processors from byte arrays
             	ImageProcessor rip = new ByteProcessor(w, h, r, null);
        	ImageProcessor gip = new ByteProcessor(w, h, g, null);
        	ImageProcessor bip = new ByteProcessor(w, h, b, null);

	//create arrays for 'colour'
	int [] red= new int [3]; 
	int [] green= new int [3]; 
	int [] blue = new int [3]; 
	int [] colour = new int [3];

	//create int to test for presence of each channel.
	int sumR=0, sumG =0, sumB =0;	

	for (int y=0; y<h;y++)
		{
		for (int x=0; x<w; x++)
			{
			red[0] = rip.getPixel(x,y);
			green[1]=gip.getPixel(x,y);
			blue[2]=bip.getPixel(x,y);
			sumR +=rip.getPixel(x,y);
			sumG +=gip.getPixel(x,y);
			sumB +=bip.getPixel(x,y);
				
			rcp.putPixel(x,y,red);
			gcp.putPixel(x,y,green);
			bcp.putPixel(x,y,blue);
			
			}	
		}
             	cp = (ColorProcessor)rgbStack.getProcessor(1);
             	cp.getRGB(r,g,b);
	if (sumR!=0)	splitStack.addSlice("red",rcp);
	if(sumG!=0)       	splitStack.addSlice("green",gcp);
	if(sumB!=0)       	splitStack.addSlice("blue",bcp);
            	splitStack.addSlice("merge",cp);
     	String title = imp.getTitle();
       	new ImagePlus(title+" (split)",splitStack).show();

	ImagePlus imp2 = WindowManager.getCurrentImage();
	imp2.setCalibration(cal);
          	imp2.getWindow().repaint();
	n = splitStack.getSize();
	String [] 	mont1= {"2×2", "4×1", "1×4"};	
	String [] 	mont2= {"2×2", "3×1", "1×3"};	
	String [] mont3=null;
	if (n==4) mont3 = mont1;
	if (n==3) mont3 = mont2;
	
	GenericDialog gd = new GenericDialog("Montage");
	int rows = 2, cols = 2;
	int border = 3;
	gd.addChoice("Rows × Columns", mont3,mont3[0]);
	gd.addNumericField("Border width", border, 0);
	gd.addMessage("Click cancel to exit dialog \nand keep the split-stack");
	gd.showDialog();
	if (gd.wasCanceled())
	            return;
	int montIndex  = gd.getNextChoiceIndex();
	border = (int)gd.getNextNumber();
//	IJ.showMessage(""+montIndex);
	if (montIndex==1&&n==4) {rows = 4; cols=1;}
	if (montIndex==2&&n==4) {rows = 1; cols=4;}
	if (montIndex==1&&n==3) {rows = 3; cols=1;}
	if (montIndex==1&&n==3) {rows = 1; cols=3;}
	IJ.run("Make Montage...", "columns="+cols +" rows="+rows+" scale=1 first=1 last="+n+" increment=1 border="+border);
	imp2.hide();

    }
}




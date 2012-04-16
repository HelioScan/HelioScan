import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;

/** This plugin does various calculations on two images or stacks.
*/ 

public class Bleach_ implements PlugIn {



    public void run(String arg) {
   int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No images are open.");
            return;
        }


       double thalf = 0.5;
	boolean keep;

      
        GenericDialog gd = new GenericDialog("Bleach correction");
    
        gd.addNumericField("t½:", thalf, 1);
	gd.addCheckbox("Keep source stack:",true);
             gd.showDialog();
        if (gd.wasCanceled())
            return ;
          
            long start = System.currentTimeMillis();
        thalf = gd.getNextNumber();
	keep = gd.getNextBoolean();
if (keep) IJ.run("Duplicate...", "title='Bleach corrected' duplicate");
  ImagePlus imp1 = WindowManager.getCurrentImage();
        int d1 = imp1.getStackSize();
        double v1, v2;
        int width  = imp1.getWidth();
        int height = imp1.getHeight();
        ImageProcessor ip1, ip2, ip3;

        int slices = imp1.getStackSize();
         ImageStack stack1 = imp1.getStack();
         ImageStack stack2 = imp1.getStack();
        int currentSlice = imp1.getCurrentSlice();

        for (int n=1; n<=slices; n++) {
            ip1 = stack1.getProcessor(n);
            ip3 = stack1.getProcessor(1);
	    ip2 = stack2.getProcessor(n);
            for (int x=0; x<width; x++) {
                for (int y=0; y<height; y++) {
      	                v1 = ip1.getPixelValue(x,y);
                	v2 = ip3.getPixelValue(x,y);

//=B8/(EXP(-C$7*A8))
                                v1 = (v1/Math.exp(-n*thalf));
		ip2.putPixelValue(x, y, v1);
                    }
	
         
            
                }      IJ.showProgress((double)n/slices);
            IJ.showStatus(n+"/"+slices);
            }  

       // stack2.show();
       imp1.updateAndDraw();
            }     
        }


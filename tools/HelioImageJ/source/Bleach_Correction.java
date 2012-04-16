import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;

/** This plugin does various calculations on two images or stacks.
*/ 

public class Bleach_Correction implements PlugIn {



    public void run(String arg) {
   int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No images are open.");
            return;
        }


       double k = 0.005;
double a,b,c,d,bleachfactor;
	boolean keep;

      
        GenericDialog gd = new GenericDialog("Bleach correction");
    gd.addNumericField("Decay constant (k)", k, 1);
//gd.addNumericField("a", 0, 1);
//gd.addNumericField("b", 0, 1);
//gd.addNumericField("c", 0, 1);
//gd.addNumericField("d", 0, 1);
	gd.addCheckbox("Keep source stack:",true);
             gd.showDialog();
        if (gd.wasCanceled())
            return ;
          
            long start = System.currentTimeMillis();
        k = gd.getNextNumber();
//a = gd.getNextNumber();
//b = gd.getNextNumber();
//c = gd.getNextNumber();
//d = gd.getNextNumber();

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
                                v1 = (v1/Math.exp(-n*k));
//  y = d + (a - d) / (1 + (x/c)^b) 
//Rodbard
    //=C$6+((C$3-C$6)/(1+(B13/C$5)^(C$4)))
//v1 = v1*(v2/(d+(a-d)/(1+ Math.pow(n/c,b))));
  		
		ip2.putPixelValue(x, y, v1);
                    }
	

            
                }      IJ.showProgress((double)n/slices);
            IJ.showStatus(n+"/"+slices);
            }  

       // stack2.show();
       imp1.updateAndDraw();
      IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");        }       
        }


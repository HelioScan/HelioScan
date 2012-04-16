import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Image_Enhance implements PlugIn {

	public void run(String arg) {
	double rollingBall = 50;
	boolean sigma= true;
	boolean median3D= false;
	boolean gamma= true;
	boolean anisoDiff=false;
	double gammaVal= 0.7;
	int filterID=0;
	
	ImagePlus imp1 = WindowManager.getCurrentImage();
	if (imp1==null)
			{IJ.noImage(); return;}
	if (imp1.getType()==imp1.COLOR_RGB) {
	                IJ.showMessage("Image Enhance", "Images must be 8-bit or 16-bit grayscale.");
		return;
	 }

boolean isStack = imp1.getStackSize()>1;
String[] filters =  { "None","Median", "Sigma", "Smooth","Anisotropic Diffusion"};

        GenericDialog gd = new GenericDialog("Image Enhance");
	gd.addNumericField("BG subtraction radius (zero for no adjustment)", rollingBall,0);
	gd.addChoice("Filter to reduce noise", filters, filters[filterID]);
	 gd.addNumericField("Percentage saturation (100 for no adjustment)", 0.1,2);
	gd.addNumericField("Gamma Value (zero for no adjustment)", gammaVal,2);
	gd.showDialog();
        
        if (gd.wasCanceled())
            return ;
  
	rollingBall= gd.getNextNumber();
	filterID= gd.getNextChoiceIndex();
	double percentSaturation= gd.getNextNumber();
	gammaVal= gd.getNextNumber();
	
	IJ.run("Hi Lo Indicator");
	String bgSubtraction = "rolling="+rollingBall+"  stack";
	 if(rollingBall!=0) IJ.run("Subtract Background...", bgSubtraction );
	if ((filterID==1)&&(!isStack)) IJ.run("Median...", "radius=1");
	if ((filterID==1)&&(isStack)) IJ.run("Median 3D   ", "include");
	if (filterID==2) IJ.run("Sigma Filter", "kernel=3×3 sigma=2 minimum=1 do modified");
	if (filterID==3) IJ.run("Mean...", "radius=1 separable");
	if (filterID==4) IJ.run("Anisotropic Diffusion...", "picture=Fibroblast_Cell_Nucleus.jpg iterations=1 k=10 lambda=0.20 big");

	if (percentSaturation!=100) IJ.run("Enhance Contrast", "saturated="+percentSaturation+" normalize normalize_all use");
	if (gammaVal!=0) IJ.run("Gamma...", "stack value="+gammaVal);
	
	}

}

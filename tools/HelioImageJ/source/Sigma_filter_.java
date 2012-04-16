import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Sigma_filter_ implements PlugIn {

	public void run(String arg) {

  int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No images are open.");
            return;
        }
	double kernel=3;
	double kernelsum = 0;
	double kernelvarsum =0;
	double kernalvar = 0;
	double sigmawidth = 2;
	int kernelindex, minpixnumber;
	String[] kernelsize =  { "3×3","5×5", "7×7", "9×9"};

	GenericDialog gd = new GenericDialog("Sigma Filter");
	gd.addChoice("Kernel size", kernelsize, kernelsize[0]);
	gd.addNumericField("Sigma width",sigmawidth , 2);
	gd.addNumericField("Minimum number of pixels", 1, 0);

	gd.addCheckbox("Keep source:",true);
	gd.addCheckbox("Do all stack:",true);
	gd.addCheckbox("Modified Lee's FIlter:",true);
	       	
	gd.showDialog();
       	if (gd.wasCanceled()) return ;
	kernelindex =  gd.getNextChoiceIndex();
          	sigmawidth = gd.getNextNumber();
          	minpixnumber = ((int)gd.getNextNumber());
          	boolean keep = gd.getNextBoolean();
	boolean doallstack = gd.getNextBoolean();
	boolean modified = gd.getNextBoolean();
	if (kernelindex==0) kernel = 3;
	if (kernelindex==1) kernel = 5;
	if (kernelindex==2) kernel = 7;
	if (kernelindex==3) kernel = 9;
    	long start = System.currentTimeMillis();
	
if (minpixnumber> (kernel*kernel)){
	      IJ.showMessage("Sigma filter", "There must be more pixels in the kernel than+\n" + "the minimum number to be included");
            return;
        }
	double v, midintensity;
	int   x, y, ix, iy;
	double sum = 0;
	double backupsum =0;
	int count = 0;
	int n = 0;
	if (keep) {IJ.run("Select All"); IJ.run("Duplicate...", "title='Sigma filtered' duplicate");}

	int radius = (int)(kernel-1)/2;
	ImagePlus imp = WindowManager.getCurrentImage();
	ImageStack stack1 = imp.getStack();
	int width = imp.getWidth();
	int height = imp.getHeight();
	int nslices = stack1.getSize();
	int cslice = imp.getCurrentSlice();
	double status = width*height*nslices;
	
	ImageProcessor  ip = imp.getProcessor();
	int sstart = 1;
	if (!doallstack) {sstart = cslice; nslices=sstart;status = status/nslices;};

 for (int i=sstart; i<=nslices; i++) {
                imp.setSlice(i);
                    
for (x=radius;x<width+radius;x++)	{
		for (y=radius;y<height+radius;y++)	{
			
			midintensity = ip.getPixelValue(x,y);
			count = 0;
			sum = 0;
			kernelsum =0;
			kernalvar =0;
			kernelvarsum =0;
			backupsum = 0;

		//calculate mean of kernel value
			for (ix=0;ix<kernel;ix++)	{
					for (iy=0;iy<kernel;iy++)	{
							v = ip.getPixelValue(x+ix-radius,y+iy-radius);
							kernelsum = kernelsum+v;
								}
						}
			double sigmacalcmean = (kernelsum/(kernel*kernel));

		//calculate variance of kernel
			for (ix=0;ix<kernel;ix++)	{
					for (iy=0;iy<kernel;iy++)	{
							v = ip.getPixelValue(x+ix-radius,y+iy-radius);
							kernalvar = (v-sigmacalcmean)*(v-sigmacalcmean);
							kernelvarsum = kernelvarsum + kernalvar;
								}
						}
			//double variance = kernelvarsum/kernel;
			double sigmacalcvar = kernelvarsum/((kernel*kernel)-1);

			//calcuate sigma range = sqrt(variance/(mean^2)) × sigmawidth
			double sigmarange  = sigmawidth*(Math.sqrt((sigmacalcvar) /(sigmacalcmean*sigmacalcmean)));
			//calulate sigma top value and bottom value
			double sigmatop = midintensity*(1+sigmarange);
			double sigmabottom = midintensity*(1-sigmarange);
			//calculate mean of values that differ are in sigma range.
			for (ix=0;ix<kernel;ix++)	{
					for (iy=0;iy<kernel;iy++)	{
							v = ip.getPixelValue(x+ix-radius,y+iy-radius);
							if ((v>=sigmabottom)&&(v<=sigmatop)){
								sum = sum+v;
								count = count+1;   }
								backupsum = v+ backupsum;
										}		
						}
//if there are too few pixels in the kernal that are within sigma range, the 
//mean of the entire kernal is taken. My modification of Lee's filter is to exclude the central value 
//from the calculation of the mean as I assume it to be spuriously high or low 
			if (!(count>(minpixnumber)))
				{sum = (backupsum-midintensity);
				count = (int)((kernel*kernel)-1);
				if (!modified)
					{sum = (backupsum);
					count  = (int)(kernel*kernel);}
				}
			
			double val =  (sum/count);
			ip.putPixelValue(x,y, val);
			n = n+1;
	double percentage = (((double)n/status)*100);
			 IJ.showStatus(IJ.d2s(percentage,0) +"% done");		
			
}

	//		IJ.showProgress(i, status);
					}}
			imp.updateAndDraw();
 			IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");        }      


}

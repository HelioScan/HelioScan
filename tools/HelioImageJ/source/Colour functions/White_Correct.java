//Modified from Colour Correct v1.1  by G.Landini
//Tony Collins		


import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;

public class White_Correct implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about"))
			{showAbout(); return DONE;}
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		
		double scale = Prefs.get("WB_scale.double",0.90);
		GenericDialog gd = new GenericDialog("White Correct");
		gd.addNumericField("Scaling factor", scale,3);
		gd.showDialog();
		if (gd.wasCanceled())	return;
		scale = gd.getNextNumber();
		Prefs.set("WB_scale.double",scale);
		int [] xyzf = new int [4]; //[0]=x, [1]=y, [2]=z, [3]=flags
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y, x2=-1, y2=-1, p=0;
		int nblack=0, blackred=0, blackgreen=0, blackblue=0;
		long nwhite=0, whitered=0, whitegreen=0, whiteblue=0;
		
		double tred, tgreen, tblue;
		ImagePlus imp = WindowManager.getCurrentImage();
		ImageStack img = imp.getStack();
		//IJ.showMessage("Colour Correct v1.1  by G.Landini\n \nSelect BLACK points (right-click to end)");
		
		//IJ.run("Select None");
		
		ImageProcessor ipmask = imp.getMask();
		Roi roi1 = imp.getRoi();
		Rectangle rect =ip.getRoi();
		int xOffset = rect.x;
		int yOffset = rect.y;
		int rwidth=rect.width;
		int rheight  =rect.height;
		long countW=0;
		int mask=0;
		//IJ.showMessage("rWidth: " +rwidth);
		int nslices = imp.getStackSize();
				
		for (int yr=0; yr<rheight; yr++) 
			{IJ.showStatus("Press 'Esc' to abort");	
			if (IJ.escapePressed()) 
				{IJ.beep();  return;}
	            		for (int xr=0; xr<rwidth; xr++) 
				{mask = 1;
				if(ipmask!=null) mask = (int)ipmask.getPixelValue(xr,yr);
				if (mask!=0)
					{
		           	    		p = (int)ip.getPixel(xr+xOffset,yr+yOffset); 
					whitered+=(p & 0xff0000)>>16;
					whitegreen+=(p & 0x00ff00)>>8;
					whiteblue+= p & 0x0000ff;
					countW++;
					}
				}
			}
		
		IJ.run("Select None");
		if (countW>0){

			whitered/=countW;
			whitegreen/=countW;
			whiteblue/=countW;
			//IJ.showMessage("RGB: " +whitered+" "+whitegreen+" "+whiteblue);
		//	IJ.showMessage("N: " +countW);
			
		for(int s=1; s<=nslices; s++)
		{ip = img.getProcessor(s);
	
			for(y=0;y<ye;y++){
				for (x=0;x<xe;x++){
					p=ip.getPixel(x,y);
					tred=(((p & 0xff0000) >> 16) / (double) (whitered)) * (255*scale);
					if (tred<0) tred=0;
					if (tred>255) tred=255*scale;
					tgreen=(((p & 0x00ff00) >> 8) / (double) (whitegreen)) * (255*scale);
					if (tgreen<0) tgreen=0;
					if (tgreen>255) tgreen=(255*scale);
					tblue=((p & 0x0000ff) / (double) (whiteblue)) * (255*scale);
					if (tblue<0) tblue=0;
					if (tblue>255) tblue=255*scale;
					ip.putPixel(x,y, (((int)tred & 0xff) <<16)+ (((int)tgreen & 0xff) << 8) + ((int)tblue & 0xff));
				}
			}
		}
		}
	}

	void getCursorLoc(int [] xyzf, ImagePlus imp ) {
		ImageWindow win = imp.getWindow();
		ImageCanvas ic = win.getCanvas();
		Point p = ic.getCursorLoc();
		xyzf[0]=p.x;
		xyzf[1]=p.y;
		xyzf[2]=imp.getCurrentSlice()-1;
		xyzf[3]=ic.getModifiers();
	}

	void showAbout() {
		IJ.showMessage("About Colour_Correct...",
		"Colour_Correct by Gabriel Landini,  G.Landini@bham.ac.uk\n"+
		"Corrects the colours of an image by first subtracting the mean RGB values of\n"+
		"a number of points considered to be \'black\' and then subtracts the background\n"+
		"by performing the ratio of the image and the mean RGB values of a number of points\n"+
		"considered to be \'white\' minus the \'black\'. It does *not* correct for uneven\n"+
		"illumination.  The formula is:"+
		"           image = [(original-black)/(white-black)]*255\n"+
		"This is a simple & quick (not the best, though) method to compensate the filament\n"+
		"temperature colour of light transmitted images such as bright field microscopy\n"+
		"when there is no original illumination source to correct from.");
	}
}


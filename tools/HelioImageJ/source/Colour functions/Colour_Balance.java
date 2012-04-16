//Modified from Colour Correct v1.1  by G.Landini
//Tony Collins		


import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;

public class Colour_Balance implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		
		double scale = Prefs.get("WB_scale.double",0.90);
		
		int [] xyzf = new int [4]; //[0]=x, [1]=y, [2]=z, [3]=flags
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		long countW=0;
		int x, y, x2=-1, y2=-1, p=0;
		long nmean=0, meanred=0, meangreen=0, meanblue=0;
		long nwhite=0, whitered=0, whitegreen=0, whiteblue=0;
		long ngrey=0, blackred=255, blackgreen=255, blackblue=255;
		//long nwhite=0, whitered=0, whitegreen=0, whiteblue=0;
		double tred, tgreen, tblue;
		ImagePlus imp = WindowManager.getCurrentImage();

		//IJ.showMessage("Colour Correct v1.1  by G.Landini\n \nSelect BLACK points (right-click to end)");
		
		//IJ.run("Select None");
		Roi roi1 = imp.getRoi();
		if(roi1==null)
			{IJ.showMessage("No 'white' roi selected.");
			return;}

GenericDialog gd = new GenericDialog("Colour Balance Correct");
		gd.addNumericField("Enter scaling factor", scale,3);
		gd.showDialog();
		if (gd.wasCanceled())	return;
		scale = gd.getNextNumber();
		Prefs.set("WB_scale.double",scale);
		Rectangle rect =ip.getRoi();
		ImageProcessor ipmask = imp.getMask();
		IJ.run("Select All");
		
		
		int xOffset = rect.x;
		int yOffset = rect.y;
		int rwidth=rect.width;
		int rheight  =rect.height;
		long countB=0;
		int mask=0;
		//IJ.showMessage("rWidth: " +rwidth);
					
		for (y=0; y<ye; y++) 
			{IJ.showStatus("Normalising mean values. Press 'Esc' to abort");	
			if (IJ.escapePressed()) 
				{IJ.beep();  return;}
	            		for (x=0; x<xe; x++) 
				{       	    		p = (int)ip.getPixel(x,y); 
					meanred+=(p & 0xff0000)>>16;
					meangreen+=(p & 0x00ff00)>>8;
					meanblue+= p & 0x0000ff;
					countB++;
					
				}
			}
		
		IJ.run("Select None");
		if (countB>0){

			meanred/=countB;
			meangreen/=countB;
			meanblue/=countB;
		//	IJ.showMessage("RGB: " +meanred+" "+meangreen+" "+meanblue);
		//	IJ.showMessage("N: " +countW);
			//meanred*=scale;
			//meangreen*=scale;
			//meanblue*=scale;
			
			meanred+=-128;
			meangreen+=-128;
			meanblue+=-128;

			for(y=0;y<ye;y++){
				for (x=0;x<xe;x++){
					p=ip.getPixel(x,y);
					tred=(((p & 0xff0000) >> 16)- meanred);
					if (tred<0) tred=0;
					tgreen=(((p & 0x00ff00) >> 8)- meangreen);
					if (tgreen<0) tgreen=0;
					tblue=((p & 0x0000ff) - meanblue);
					if (tblue<0) tblue=0;
					ip.putPixel(x,y, (((int)tred & 0xff) <<16)+ (((int)tgreen & 0xff) << 8) + ((int)tblue & 0xff));
				}
			}
		}

//correct black

		for (y=0; y<ye; y++) 
			{IJ.showStatus("Correcting black level. Press 'Esc' to abort");	
			if (IJ.escapePressed()) 
				{IJ.beep();  return;}
	            		for (x=0; x<xe; x++) 
				{
		           	    		p = (int)ip.getPixel(xe,ye); 

					if (blackred>(p & 0xff0000)>>16) 	blackred=(p & 0xff0000)>>16 ;
					if (blackgreen>(p & 0x00ff00)>>8) 	blackgreen=(p & 0x00ff00)>>8;
					if (blackblue>(p & 0x0000ff) )	blackblue=(p & 0x0000ff) ;
		
				}
			}

			blackred*=scale;
			blackgreen*=scale;
			blackblue*=scale;
		
			for(y=0;y<ye;y++){
				for (x=0;x<xe;x++){
					p=ip.getPixel(x,y);
					tred=(((p & 0xff0000) >> 16)- blackred);
					if (tred<0) tred=0;
					tgreen=(((p & 0x00ff00) >> 8)- blackgreen);
					if (tgreen<0) tgreen=0;
					tblue=((p & 0x0000ff) - blackblue);
					if (tblue<0) tblue=0;
					ip.putPixel(x,y, (((int)tred & 0xff) <<16)+ (((int)tgreen & 0xff) << 8) + ((int)tblue & 0xff));
				}
			}



//correct white

		for (int yr=0; yr<rheight; yr++) 
			{IJ.showStatus("Correcting White level. Press 'Esc' to abort");	
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
		//	IJ.showMessage("RGB: " +whitered+" "+whitegreen+" "+whiteblue);
		//	IJ.showMessage("N: " +countW);
			
		
	
			for(y=0;y<ye;y++){
				for (x=0;x<xe;x++){p=ip.getPixel(x,y);
					tred=(((p & 0xff0000) >> 16) / (double) (whitered-blackred)) * 255;
					if (tred<0) tred=0;
					if (tred>255) tred=255;
					tgreen=(((p & 0x00ff00) >> 8) / (double) (whitegreen-blackgreen)) * 255;
					if (tgreen<0) tgreen=0;
					if (tgreen>255) tgreen=255;
					tblue=((p & 0x0000ff) / (double) (whiteblue-blackblue)) * 255;
					if (tblue<0) tblue=0;
					if (tblue>255) tblue=255;
					ip.putPixel(x,y, (((int)tred & 0xff) <<16)+ (((int)tgreen & 0xff) << 8) + ((int)tblue & 0xff));
				}
			}
		}







	}


}


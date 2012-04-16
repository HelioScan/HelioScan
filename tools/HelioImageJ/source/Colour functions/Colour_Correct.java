import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;

public class Colour_Correct implements PlugInFilter {

	public int setup(String arg, ImagePlus imp) {
		if (arg.equals("about"))
			{showAbout(); return DONE;}
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		int [] xyzf = new int [4]; //[0]=x, [1]=y, [2]=z, [3]=flags
		int xe = ip.getWidth();
		int ye = ip.getHeight();
		int x, y, x2=-1, y2=-1, p=0;
		int nblack=0, blackred=0, blackgreen=0, blackblue=0;
		int nwhite=0, whitered=0, whitegreen=0, whiteblue=0;
		
		double tred, tgreen, tblue;
		ImagePlus imp = WindowManager.getCurrentImage();

		IJ.showMessage("Colour Correct v1.1  by G.Landini\n \nSelect BLACK points (right-click to end)");
		getCursorLoc( xyzf, imp );

		while ((xyzf[3] & 4) == 0){//until right pressed
			getCursorLoc( xyzf, imp );
			if ((xyzf[3] & 16)!=0){ //left pressed
				x=xyzf[0];
				y=xyzf[1];
				nblack++;
				IJ.makeRectangle(x, y, 1, 1);
				p=ip.getPixel(x,y);
				//IJ.write("Black  x:" + x +"  y:" + y + "  R:" + ((p&0xff0000)>>16 )+ "  G:" + ((p&0x00ff00)>>8)+ "  B:" + (p&0x0000ff));
				blackred+=(p & 0xff0000)>>16;
				blackgreen+=(p & 0x00ff00)>>8 ;
				blackblue+= p & 0x0000ff;
			}
			while ((xyzf[3] & 16) !=0){  //trap until left released
				getCursorLoc( xyzf, imp );
			}
		}
		IJ.run("Select None");
		if (nblack>0){

			blackred/=nblack;
			blackgreen/=nblack;
			blackblue/=nblack;

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
		}

		IJ.showMessage("Now select WHITE points (right-click to end)");

		getCursorLoc( xyzf, imp );
		while ((xyzf[3] & 4) !=0){   //trap until right released
			getCursorLoc( xyzf, imp );
		}

		while ((xyzf[3] & 4) == 0){//until right pressed
			getCursorLoc( xyzf, imp );
			if ((xyzf[3] & 16)!=0){ //left pressed
				x=xyzf[0];
				y=xyzf[1];
				nwhite++;
				IJ.makeRectangle(x, y, 1, 1);
				p=ip.getPixel(x,y);
				//IJ.write("White  x:" + x +"  y:" + y + "  R:" + ((p & 0xff0000)>>16 )+ "  G:" + ((p & 0x00ff00)>>8)+ "  B:" + (p & 0x0000ff));
				whitered+=(p & 0xff0000)>>16;
				whitegreen+=(p & 0x00ff00)>>8;
				whiteblue+= p & 0x0000ff;
			}
			while ((xyzf[3] & 16) !=0){  //trap until left released
				getCursorLoc( xyzf, imp );
			}
		}

		IJ.run("Select None");
		if (nwhite>0){

			whitered/=nwhite;
			whitegreen/=nwhite;
			whiteblue/=nwhite;

			for(y=0;y<ye;y++){
				for (x=0;x<xe;x++){
					p=ip.getPixel(x,y);
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


import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.plugin.filter.*;

public class Add_Ramp implements PlugIn {

	public void run(String arg) {

	ImagePlus imp1 = WindowManager.getCurrentImage();
	if (imp1==null) 
		{IJ.showMessage("No Image open");
		return;}

	ImageProcessor ip1=imp1.getProcessor();

	boolean drawBorder = true;
	if (IJ.altKeyDown()) drawBorder=false;

	Rectangle r = ip1.getRoi();
	int width = r.width;
	int height = r.height;
	int xLoc = r.x;
	int yLoc = r.y;
	if (imp1.getRoi()==null)
		{
		//IJ.showMessage("No Selection. Adding defualt Ramp");
		width = 25;
		height = imp1.getHeight()/2;
		xLoc = 0;
		yLoc = height/2;
		}
	
	double max = 255;
	int count=0;
	int value = 0;

//portrait ramp
	if (width<height) 
		{
		for (int y=0; y<height; y++) 
			{for (int x=0; x<width; x++)
				 {value =(int) max-(int)((max/ (double)height)*(double)y);	
				ip1.putPixelValue(x+xLoc, y+yLoc, value);
				}
			}
		}
//landscape ramp
	if (width>=height) 
		{
		for (int x=0; x<width; x++) 
			{for (int y=0; y<height; y++)
				 {value =(int)((max/ (double)width)*(double)x);	
				ip1.putPixelValue(x+xLoc, y+yLoc, value);
				}
			}

		}

//add border
	if(drawBorder){
		for (int x=0; x<width; x++)
			{ip1.putPixelValue(x+xLoc, yLoc, max/2);
			ip1.putPixelValue(x+xLoc, yLoc+height, max/2);
			}
		for (int y=0; y<height; y++)
			{ip1.putPixelValue(xLoc, yLoc+y, max/2);
			ip1.putPixelValue(xLoc+width-1, yLoc+y, max/2);
			}
		}

	imp1.setRoi(xLoc, yLoc,width,height);
	imp1.updateAndDraw();
		
	}

}

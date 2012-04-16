import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Duplicate_TjC implements PlugIn {
  
     public void run(String arg) 
	{
	int g=0;

	ImagePlus imp1 = WindowManager.getCurrentImage();
	if(imp1.getRoi()==null) {IJ.showMessage("Duplicate error", "No ROI. Exiting"); return;}
	
	ImageProcessor ip1 = imp1.getProcessor();
	ImageStack img1 = imp1.getStack();
	Rectangle r = ip1.getRoi();
	int width = r.width;
	int height = r.height;
	int xOffset= r.x;
	int yOffset = r.y;
	int nslices = imp1.getStackSize();
	ImageProcessor ip2;

	ImageProcessor ipmask = imp1.getMask();

   	ImageStack img2 = new ImageStack(width,height);
	
	for (int s=1; s<nslices;s++)
		{ip1 = img1.getProcessor(s);
		ip2 =  new ByteProcessor(width, height);
		for (int y=0;y<=height; y++)
			{
			for (int x = 0; x<width; x++)
				{
				g = ip1.getPixel(x+xOffset,y+yOffset);
				if (ipmask.getPixel(x,y)!=0)
					ip2.putPixel(x,y,g);
				}
			}
		img2.addSlice("Ch1", ip2);
		}	
	new ImagePlus("Mask", ipmask).show();
	new ImagePlus("Duplicate", img2).show();
	}

}

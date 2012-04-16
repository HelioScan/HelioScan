import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.plugin.*;


public class Select_Half implements PlugIn {

	public void run(String arg) {
		
   int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No images are open.");
            return;
        }
 int             iX;
    int             iY;
    int             iXROI;
    int             iYROI;

ImagePlus imp1= WindowManager.getCurrentImage(); 
ImageProcessor ip = imp1.getProcessor();   
int fullwidth = imp1.getWidth();
int fullheight = imp1.getHeight();

//setting roi width
 Rectangle r = ip.getRoi();
        int width = r.width;
        int height = r.height;
        iXROI = r.x;
        iYROI = r.y;
	
 imp1.setRoi (0,0, fullwidth/2, fullheight );
if (width==fullwidth) imp1.setRoi (0,0, fullwidth/2, fullheight );

if (iXROI==(fullwidth/2-1)) imp1.setRoi (0,0, fullwidth/2, fullheight );
if ((iXROI==0)&(width==(fullwidth/2))) imp1.setRoi(((fullwidth/2)-1), 0,  fullwidth/2, fullheight );

	

			
			}
		
	}
	




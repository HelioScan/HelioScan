import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;


public class Rename_Slice implements PlugIn {

	public void run(String arg) {
   int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No images are open.");
            return;
        }
		ImagePlus imp = WindowManager.getCurrentImage();
		ImageStack stack = imp.getStack();
		String title = imp.getTitle();
		int slice = imp.getCurrentSlice();
		int stacksize = stack.getSize();
		String label = stack.getSliceLabel(slice);
		Calibration cal = imp.getCalibration();

		String units = cal.getUnit();
		double z = cal.pixelDepth;
		
		if ((label==null)||(label=="")) label = title;
		
		GenericDialog gd = new GenericDialog("Rename slice");
		gd.addStringField("New slice name:", label );
		gd.addNumericField("z-interval",z,3 );
		gd.addStringField("units",units );
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		title = gd.getNextString();
		String totalz = IJ.d2s(z*stacksize);
		int i;
		double currentz = 0;
		for (i=0;i<stacksize;i++){
		stack.setSliceLabel(title, slice);
		currentz = currentz+z;
		title = IJ.d2s(z) + units +"/" +  totalz;}
	
		imp.updateAndDraw();

	}

}

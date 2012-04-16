import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

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
		int slice = stack.getSlice();
		GenericDialog gd = new GenericDialog("Rename slice");
		gd.addStringFieldChoice("New slice name:",title );
		gd.showDialog();
		if (gd.wasCanceled())
			{canceled = true; return;}
		title = gd.getNextString();
		stack.setSliceLabel(title, slice);

	}

}

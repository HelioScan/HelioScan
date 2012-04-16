import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.*;

public class Add_Slice_Labels implements PlugIn {

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
		int i=0;
		String units = cal.getUnit();
		double z = cal.pixelDepth;

		//double start = 0; change it here if you typically have timecourses
		double start = ((((z*(stacksize-1))/2))-(z*(stacksize-1)));

		int decimalplaces = 0;
		if ((z>1)&&(z<9)) decimalplaces = 1;
		if (z<1) decimalplaces = 2;
		boolean totaldepth = false;
		if ((label==null)||(label=="")) label = title;
		
		GenericDialog gd = new GenericDialog("Rename Slices");
		gd.addStringField("Slice Label:", label);	
		gd.addNumericField("Start at", start, 1 );
		gd.addMessage("Set interval to zero to label current slice only");
		gd.addNumericField("z or t-interval",z, decimalplaces );
		gd.addNumericField("Decimal places", decimalplaces ,0);
		gd.addStringField("units",units );
		gd.addCheckbox("Include total depth", totaldepth);
	//	gd.addMessage("Alt+230 = µ; Alt+248  = °");
		gd.showDialog();
		if (gd.wasCanceled())
			return;

		
		label = gd.getNextString();
		start= (int)gd.getNextNumber();
		z = gd.getNextNumber();
		decimalplaces = (int)gd.getNextNumber();
		units = gd.getNextString();
		totaldepth = gd.getNextBoolean();
		String totalz = "";
		if (totaldepth) totalz = "/" +  IJ.d2s(z*(stacksize-1), decimalplaces) ;
		double currentz = start;
		stack.setSliceLabel(label, slice);
		String prefix = label + " ";
		if (!(z==0)){
			for (i=1;i<stacksize+1;i++){
			label = prefix + IJ.d2s(currentz,decimalplaces) + totalz + " "+ units;
			stack.setSliceLabel(label,i);
			currentz = currentz+z;}
			}
		
		imp.getWindow().repaint();


	}

}

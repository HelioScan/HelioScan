import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.ImageStack.*;

public class Delta_F_down implements PlugIn {

	public void run(String arg) {
  if (!IJ.altKeyDown()) IJ.run("Duplicate...", "title=result duplicate");
		ImagePlus imp = WindowManager.getCurrentImage();
   		if (imp==null)
			{IJ.noImage(); return;}
String fileName = imp.getTitle();

	
		ImageStack stack1 = imp.getStack();
		
		int endslice = stack1.getSize();


		ImagePlus imp2 = duplicateStack(imp);
		imp2.show();
		String duplicateName = imp2.getTitle();
//IJ.showMessage("Box",fileName);
	 	ImageStack stack2 = imp2.getStack();
		stack2.deleteSlice(1);
		stack1.deleteSlice(endslice);


	String calculatorstring = ("image1='"+fileName+"' operation=Subtract image2="+ imp2.getTitle()+" create stack");

		IJ.run("Image Calculator...", calculatorstring );
ImagePlus imp3 = WindowManager.getCurrentImage();
imp3.setTitle(fileName + " DeltaF down");
		imp2.getWindow().close();
	imp.getWindow().close();

	}

ImagePlus duplicateStack(ImagePlus img1) {
        ImageStack stack1 = img1.getStack();
        int width = stack1.getWidth();
        int height = stack1.getHeight();
        int n = stack1.getSize();
        ImageStack stack2 = img1.createEmptyStack();
        try {
            for (int i=1; i<=n; i++) {
                ImageProcessor ip1 = stack1.getProcessor(i);
                ip1.resetRoi(); 
                ImageProcessor ip2 = ip1.crop(); 
                stack2.addSlice(stack1.getSliceLabel(i), ip2);
            }
        }
        catch(OutOfMemoryError e) {
            stack2.trim();
            stack2 = null;
            return null;
        }
        return new ImagePlus("Duplicate", stack2);
    }


}

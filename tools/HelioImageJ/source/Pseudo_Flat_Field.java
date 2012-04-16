import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Pseudo_Flat_Field implements PlugIn {

	public void run(String arg)
	{
		int[] wList = WindowManager.getIDList();
       		 if (wList==null) {
       			     IJ.error("No images are open.");
      			      return;}
		boolean keep;	
		boolean bit;
		boolean doStack;
		boolean wholeStack = false;
		boolean isStack = false;
		int nSlices = 27;
		ImagePlus imp= WindowManager.getCurrentImage(); 
		ImageWindow winimp = imp.getWindow();
		 if (!(imp.getType()==ImagePlus.GRAY8||imp.getType()==ImagePlus.COLOR_RGB)){
			IJ.error("Requires RGB or 8-bit Gray image.");
     			return;}
		nSlices = imp.getStackSize();
		if (nSlices>1) isStack = true;		

		String title = imp.getTitle();
		ImageStatistics s = imp.getStatistics();
			int meanint = (int)s.mean;

		 if (imp.getType()==imp.COLOR_RGB) {
			GenericDialog gd = new GenericDialog("Pseudo-flat field");

			gd.addNumericField("Mean filter kernel size:",150,0);
			gd.addCheckbox("Keep flat field:",false);
			if(isStack) {gd.addCheckbox("Process stack",false);
			gd.addCheckbox("Use whole stack for flat-field",false);}
								
			gd.showDialog();
			
			if (gd.wasCanceled())return ;
			int kernel = (int)gd.getNextNumber();
			keep = gd.getNextBoolean();
			
			if (isStack) {doStack = gd.getNextBoolean();
				wholeStack =gd.getNextBoolean();
				}

    			IJ.run("Select All");

		if(isStack) {	if (!wholeStack) IJ.run("Z Project...", "start=1 stop="+nSlices+" projection='Average Intensity'");
				IJ.run("Rename...", "title=Flat");}

		if(!isStack)IJ.run("Duplicate...", "title=Flat");

			String filterset = "radius="+kernel+" separable";
			IJ.run("Mean...", filterset);
			IJ.run("RGB Split");
			IJ.selectWindow("flat (red)");
			IJ.selectWindow("flat (red)");
			IJ.selectWindow("flat (red)");
			WindowManager.setCurrentWindow(winimp);
			IJ.run("Duplicate...", title);
			IJ.run("RGB Split");
			ImagePlus redimp =WindowManager.getImage(WindowManager.getWindowCount()-2);
			ImagePlus greenimp =WindowManager.getImage(WindowManager.getWindowCount()-1);
			ImagePlus blueimp =WindowManager.getImage(WindowManager.getWindowCount());
			ImageWindow winred = redimp.getWindow();
			ImageWindow wingreen = greenimp.getWindow();
			ImageWindow winblue = blueimp.getWindow();
			ImageStatistics reds = redimp.getStatistics();
			int redmeanint = (int)reds.mean;
			ImageStatistics greens = greenimp.getStatistics();
			int greenmeanint = (int)greens.mean;
			ImageStatistics blues = blueimp.getStatistics();
			int bluemeanint = (int)blues.mean;
			String redtitle = redimp.getTitle();
			String greentitle = greenimp.getTitle(); 
			String bluetitle = blueimp.getTitle();
			IJ.run("Image Calculator...", "image1='" + title +"-copy (red)' operation=Divide image2='flat (red)' create 32-bit stack");
			IJ.run("Rename...", "title='red flattened'");
			IJ.run("Multiply...", "value="+redmeanint);
			IJ.setMinAndMax(0, 255);
			IJ.run("8-bit");
			IJ.run("Image Calculator...", "image1='" + title +"-copy (green)' operation=Divide image2='flat (green)' create 32-bit stack");
			IJ.run("Rename...", "title='green flattened'");
			IJ.run("Multiply...", "value="+greenmeanint);
			IJ.setMinAndMax(0, 255);
			IJ.run("8-bit");
			IJ.run("Image Calculator...", "image1='" + title +"-copy (blue)' operation=Divide image2='flat (blue)' create 32-bit stack");
			IJ.run("Rename...", "title='blue flattened'");
			IJ.run("Multiply...", "value="+bluemeanint);
			IJ.setMinAndMax(0, 255);
			IJ.run("8-bit");
			IJ.run("RGB Merge...", "red='red flattened' green='green flattened' blue='blue flattened' ");
			IJ.run("Rename...", "title='"+ title +" FFC'");			
			IJ.selectWindow(redtitle);IJ.run("Close");
			IJ.selectWindow(greentitle);IJ.run("Close");
			IJ.selectWindow(bluetitle);IJ.run("Close");
			if (!keep){ IJ.selectWindow("flat (red)");
				IJ.run("Close");
				IJ.selectWindow("flat (green)");
				IJ.run("Close");
				IJ.selectWindow("flat (blue)");
				IJ.run("Close");
				}
			}


		 if (imp.getType()==imp.GRAY8)
			 {
			GenericDialog gd = new GenericDialog("Pseudo-flat field");
			gd.addNumericField("Mean filter kernel size:",150,0);
			gd.addCheckbox("Keep flat field:",false);
			gd.addCheckbox("32-bit result:",true);
			if(isStack) {gd.addCheckbox("Process stack",false);
			gd.addCheckbox("Use whole stack for flat-field",false);}
			gd.showDialog();

			if (gd.wasCanceled())return ;
			int kernel = (int)gd.getNextNumber();
			keep = gd.getNextBoolean();
			bit = gd.getNextBoolean();
			if (isStack) {doStack = gd.getNextBoolean();
				wholeStack =gd.getNextBoolean();
				}

			IJ.run("Select All");
			
 			if(isStack) 
				{ if(!wholeStack) IJ.run("Z Project...", "start=1 stop="+nSlices+" projection='Average Intensity'");
				if(wholeStack) IJ.run("Duplicate...", "title=Flat duplicate");
				IJ.run("Rename...", "title=Flat");
				}

			if(!isStack) 	IJ.run("Duplicate...", "title=Flat duplicate");
			
			String filterset = "radius="+kernel+" separable stack";
			IJ.run("Mean...", filterset);

			IJ.run("Image Calculator...", "image1='"+ title + "' operation=Divide image2=Flat create 32-bit stack");
			if (!bit){
				IJ.run("Multiply...", " stack value="+meanint);
				IJ.setMinAndMax(0, 255);
				IJ.run("8-bit");}			
			if (!keep){ IJ.selectWindow("Flat");
				IJ.run("Close");}
			}
		}

}

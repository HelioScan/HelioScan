import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
import ij.measure.Calibration;

public class Stereo_Pair implements PlugIn {

	public void run(String arg) {
int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No images are open.");
            return;
        }

ImagePlus imp= WindowManager.getCurrentImage(); 
ImageWindow winimp = imp.getWindow();
WindowManager.setCurrentWindow(winimp);

  if (imp.getStackSize()<2) {
            IJ.showMessage("Stereo Pair", "This command requires a stack.");
            return;
        }
Calibration cal = imp.getCalibration();
   
int rotat = 7;
double slice =cal.pixelDepth;

        GenericDialog gd = new GenericDialog("Stereo Pairs");
gd.addNumericField("Angle of rotation (±°):",rotat,0);
gd.addMessage("Try angles between 5-10°");
 gd.addCheckbox("Red-Cyan Anaglyph", true);
 gd.addCheckbox("Red-Green Anaglyph", false);
 gd.addCheckbox("Stereo Pair", false);
gd.addCheckbox("Rotation movie",false);

 gd.showDialog();
  if (gd.wasCanceled())
            return ;



rotat = (int)gd.getNextNumber();
boolean RC_anal = gd.getNextBoolean();
boolean RG_anal = gd.getNextBoolean();
boolean SterP = gd.getNextBoolean();
boolean movie = gd.getNextBoolean();

if ((RC_anal==true)|(RG_anal==true)){
	 if (imp.getType()!=imp.GRAY8) {
		           IJ.showMessage("Stereo Pair", "Stack must be 8-bit grayscale for anaglyph.");
			RC_anal = false;
			RG_anal=false;
					}
				}

if ((RC_anal==false)&(RG_anal==false)&(SterP==false)) {
          IJ.showMessage("Stereo Pair", "Nothing to do - exiting");
		return;}

int projrotat = 360-(rotat/2);
int totalrotat = 0;
int increment = totalrotat;
if (movie) {totalrotat =360;
	increment = 10;}


//need to get these values to project methods
String projectsettings = "projection='Brightest Point' axis=Y-Axis initial="+ projrotat + " total="+ totalrotat +" rotation="+ increment +" lower=1 upper=255 opacity=0 surface=100 interior=50 interpolate";
IJ.run("3D Project...", projectsettings);	
ImagePlus impleft= WindowManager.getCurrentImage(); 
ImageWindow winimpleft = impleft.getWindow();
WindowManager.setCurrentWindow(winimpleft);
IJ.run("Rename...", "title=left");
WindowManager.setCurrentWindow(winimp);
projrotat = (rotat/2);
projectsettings = "projection='Brightest Point' axis=Y-Axis initial="+ projrotat + " total="+ totalrotat +" rotation="+ increment+" lower=1 upper=255 opacity=0 surface=100 interior=50 interpolate";
IJ.run("3D Project...", projectsettings);	
ImagePlus impright= WindowManager.getCurrentImage(); 
ImageWindow winimpright = impright.getWindow();
WindowManager.setCurrentWindow(winimpright);
IJ.run("Rename...", "title=right");
IJ.selectWindow("left");
IJ.run("Rename...", "title=left");
ImagePlus imp1= WindowManager.getCurrentImage(); 
IJ.run("Duplicate...", "title=red duplicate");
IJ.selectWindow("right");
IJ.run("Rename...", "title=right");
ImagePlus imp2= WindowManager.getCurrentImage(); 
IJ.run("Duplicate...", "title=green duplicate");

if  (RG_anal){
  
	IJ.run("RGB Merge...", "red=red green=green blue=*None* keep");
	IJ.selectWindow("RGB");
	IJ.run("Rename...", "title=Red-Green Anaglyph");
}
 if (SterP){
		ImageStack stack1b = imp1.getStack();
		ImageStack stack2b= imp2.getStack();
		ImageStack stack3b = combineHorizontally(stack1b, stack2b);
		new ImagePlus("Stereo Pair", stack3b).show();
}

 if (RC_anal){	
  		
	IJ.run("RGB Merge...", "red=red green=green blue=green keep");
	IJ.selectWindow("RGB");
		IJ.run("Rename...", "title=Red-Cyan Anaglyph");
}
		IJ.selectWindow("red");
		IJ.run("Close");
		IJ.selectWindow("green");
		IJ.run("Close");

		IJ.selectWindow("left");
		IJ.run("Close");
		IJ.selectWindow("right");
		IJ.run("Close");
	}

	public ImageStack combineHorizontally(ImageStack stack1, ImageStack stack2) {
	//taken from Wayne Rasband Stack Combiner - it did strange things when recorded
		int d1 = stack1.getSize();
		int d2 = stack2.getSize();
		int d3 = Math.max(d1, d2);
		int w1 = stack1.getWidth();
		int h1 = stack1.getHeight();
 		int w2 = stack2.getWidth();
		int h2 = stack2.getHeight();
		int w3 = w1 + w2;
		int h3 = Math.max(h1, h2);
		ImageStack stack3 = new ImageStack(w3, h3, stack1.getColorModel());
		ImageProcessor ip = stack1.getProcessor(1);
		ImageProcessor ip1, ip2, ip3;
		Color background = Toolbar.getBackgroundColor();
 		for (int i=1; i<=d3; i++) {
 			IJ.showProgress((double)i/d3);
 			ip3 = ip.createProcessor(w3, h3);
 			if (h1!=h2) {
 				ip3.setColor(background);
 				ip3.fill();
 			}
 			if  (i<=d1) {
				ip3.insert(stack1.getProcessor(1),0,0);
				if (stack2!=stack1)
					stack1.deleteSlice(1);
			}
			if  (i<=d2) {
				ip3.insert(stack2.getProcessor(1),w1,0);
				stack2.deleteSlice(1);
			}
		stack3.addSlice(null, ip3);
		}
		return stack3;
	}
}

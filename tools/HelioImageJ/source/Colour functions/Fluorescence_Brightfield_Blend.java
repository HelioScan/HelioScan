import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Fluorescence_Brightfield_Blend implements PlugIn {

	public void run(String arg) {
         int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.error("No images are open.");
            return;
        }

        String[] titles = new String[wList.length+1];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            titles[i] = imp!=null?imp.getTitle():"";
        }
        String none = "*None*";

        titles[wList.length] = none;
        GenericDialog gd = new GenericDialog("Colour Merge");
        gd.addChoice("Fluorescence Stack:", titles, titles[0]);
     gd.addChoice("Brightfield Stack:", titles, titles[1]);
	gd.addNumericField("% pre-subtraction",60,0);
     
        gd.showDialog();

        if (gd.wasCanceled())
            return;
   int[] index = new int[2];
     index[0] = gd.getNextChoiceIndex();
     index[1] = gd.getNextChoiceIndex();
       int percentagePresub= (int)gd.getNextNumber();




ImagePlus impFluor = WindowManager.getImage(wList[index[0]]);
ImagePlus impGrey = WindowManager.getImage(wList[index[1]]);


ImageWindow winGrey = impGrey.getWindow();
WindowManager.setCurrentWindow(winGrey);
String greyTitle = winGrey.getTitle();

ImageWindow winFluor = impFluor.getWindow();
WindowManager.setCurrentWindow(winFluor);
String fluorTitle = winFluor.getTitle();

//check for both RGB
//if (impFluor.getType!=RGB)||impGrey


IJ.run("Duplicate...", "title=[Fluor] duplicate");
IJ.run("RGB Color");
IJ.run("Multiply...", "value="+ (double)percentagePresub/(double)100);
IJ.run("RGB Split ");


IJ.selectWindow("Fluor (red)");
IJ.run("Rename...", "title='FluorRed'");
ImagePlus impRed = WindowManager.getCurrentImage();
ImageWindow winRed= impRed.getWindow();
String redTitle = winRed.getTitle();


IJ.selectWindow("Fluor (green)");
IJ.run("Rename...", "title='FluorGreen'");
ImagePlus impGreen = WindowManager.getCurrentImage();
ImageWindow winGreen= impGreen.getWindow();
String greenTitle = winGreen.getTitle();
	IJ.selectWindow("Fluor (blue)");
IJ.run("Rename...", "title='FluorBlue'");
ImagePlus impBlue = WindowManager.getCurrentImage();
ImageWindow winBlue= impBlue.getWindow();
String blueTitle = winBlue.getTitle();

WindowManager.setCurrentWindow(winGrey);
IJ.run("Duplicate...", "title=[Grey] duplicate");
IJ.run("RGB Color");
IJ.run("Image Calculator...", "image1='Grey' operation=Subtract image2="+redTitle);
IJ.run("Image Calculator...", "image1='Grey' operation=Subtract image2="+greenTitle);
IJ.run("Image Calculator...", "image1='Grey' operation=Subtract image2="+blueTitle);

IJ.run("Image Calculator...", "image1='Grey' operation=Add image2="+fluorTitle);
IJ.run("Rename...", "title='"+greyTitle+" Merged'");
//IJ.selectWindow("Fluor copy");
//		IJ.run("Close");

IJ.selectWindow("FluorRed");
IJ.run("Close");
IJ.selectWindow("FluorGreen");
IJ.run("Close");
IJ.selectWindow("FluorBlue");
IJ.run("Close");
	}

}

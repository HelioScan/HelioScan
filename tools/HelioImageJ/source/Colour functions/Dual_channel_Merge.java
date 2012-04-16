import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Dual_channel_Merge implements PlugIn {

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
  private static String[] psdocolours =  { "Cyan", "Magenta", "Yellow", "Orange","Red", "Green","Blue", "Grey", "Pale Cyan", "Pale Magenta", "Pale Yellow"};

        titles[wList.length] = none;

        GenericDialog gd = new GenericDialog("Cyan-Magenta Stack Merge");
        gd.addChoice("First Stack:", titles, titles[0]);
                gd.addChoice("First colour", pscolours, pscolours[pscol1]);

        gd.addChoice("Second Stack:", titles, titles[1]);
                      gd.addChoice("Second colour", pscolours, pscolours[pscol2])

        String title3 = titles.length>2?titles[2]:none;

       gd.addCheckbox("Use 'Add' operator?", false);
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        int[] index = new int[3];
        index[0] = gd.getNextChoiceIndex();
        index[1] = gd.getNextChoiceIndex();
       boolean UseOR = gd.getNextBoolean();
  //boolean UseOR = false;
        ImagePlus[] image = new ImagePlus[3];
ImagePlus impMagenta = WindowManager.getImage(wList[index[0]]);
ImagePlus impCyan = WindowManager.getImage(wList[index[1]]);
        int stackSize = 0;
        int width = 0;
        int height = 0;
        for (int i=0; i<3; i++) {
            if (index[i]<wList.length) {
                image[i] = WindowManager.getImage(wList[index[i]]);
                width = image[i].getWidth();
                height = image[i].getHeight();
                stackSize = image[i].getStackSize();
            }
        }
        if (width==0) {
            IJ.error("There must be at least one 8-bit or RGB source stack.");
            return;
        }
        
//get origina magenta image

ImageWindow winMagenta = impMagenta.getWindow();
WindowManager.setCurrentWindow(winMagenta);

//duplicate and assign vars

IJ.run("Duplicate...", "title=Magenta duplicate");
ImagePlus impMagentacopy = WindowManager.getCurrentImage();
ImageWindow winMagentacopy = impMagenta.getWindow();
IJ.run("Magenta");
IJ.run("RGB Color");

//get orignial cyan image


ImageWindow winCyan = impCyan.getWindow();
WindowManager.setCurrentWindow(winCyan);

//Duplicate and assign vars
IJ.run("Duplicate...", "title=Cyan duplicate");


ImagePlus impCyancopy = WindowManager.getCurrentImage();
ImageWindow winCyancopy = impCyancopy.getWindow();

IJ.run("Cyan");
IJ.run("RGB Color");

//merge
if (UseOR==true)
IJ.run("Image Calculator...", "image1='Magenta' operation=Add  image2=Cyan stack");

if (UseOR==false)
IJ.run("Image Calculator...", "image1='Magenta' operation=Difference image2=Cyan stack");

//rename merge
IJ.run("Rename...", "title='Cyan Magenta merge'");


//close cyan copy
impCyancopy.changes = false;
winCyancopy.close();
//winMagenta.close();

	}

}

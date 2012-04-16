import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Colour_merge_original implements PlugIn {

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
int pscol1 = 1;
int pscol2 = 2;
String[] pscolours =  { "<Current>","Cyan", "Magenta", "Yellow", "Orange ","Red", "Green","Blue", "Grays", "Cyan Pale", "Magenta pale", "Yellow pale"};


        GenericDialog gd = new GenericDialog("Colour Merge");
        gd.addChoice("First Stack:", titles, titles[0]);
      gd.addChoice("First colour", pscolours, pscolours[1]);

        gd.addChoice("Second Stack:", titles, titles[1]);



      gd.addChoice("Second colour", pscolours, pscolours[2]);
 gd.addCheckbox("Use 'Add' operator?", true);
 gd.addCheckbox("Keep source stacks?", true);
gd.addCheckbox("Presubtraction of 2nd from 1st?:",false);

        String title3 = titles.length>2?titles[2]:none;

      
        gd.showDialog();

        if (gd.wasCanceled())
            return;
        int[] index = new int[3];
        int[] colourindex = new int [3];

        index[0] = gd.getNextChoiceIndex();
   colourindex[0] = gd.getNextChoiceIndex();

        index[1] = gd.getNextChoiceIndex();

        colourindex [1] = gd.getNextChoiceIndex();

       boolean UseOR = gd.getNextBoolean();
  boolean keep = gd.getNextBoolean();
boolean Presub = gd.getNextBoolean();

ImagePlus impMagenta = WindowManager.getImage(wList[index[0]]);

ImagePlus impCyan = WindowManager.getImage(wList[index[1]]);

String firstcol = pscolours[colourindex[0]];
String secondcol = pscolours[colourindex[1]];

        ImagePlus[] image = new ImagePlus[3];



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
if (keep) IJ.run("Duplicate...", "title=Magenta duplicate");
if (!keep) IJ.run("Rename...", "title=Magenta");


ImagePlus impMagentacopy = WindowManager.getCurrentImage();
ImageWindow winMagentacopy = impMagenta.getWindow();
if (firstcol!="<Current>") IJ.run(firstcol);
IJ.run("RGB Color");

//get orignial cyan image


ImageWindow winCyan = impCyan.getWindow();
WindowManager.setCurrentWindow(winCyan);

//Duplicate and assign vars
if (keep) IJ.run("Duplicate...", "title=Cyan duplicate");
if (!keep) IJ.run("Rename...", "title=Cyan");

ImagePlus impCyancopy = WindowManager.getCurrentImage();
ImageWindow winCyancopy = impCyancopy.getWindow();

if (Presub==true)
IJ.run("Image Calculator...", "image1='Magenta' operation=Subtract image2='Cyan'  ");


if (secondcol!="<Current>") IJ.run(secondcol);
IJ.run("RGB Color");

//merge
if (UseOR==true)
IJ.run("Image Calculator...", "image1='Magenta' operation=Add  image2=Cyan stack");

if (UseOR==false)
IJ.run("Image Calculator...", "image1='Magenta' operation=Difference image2=Cyan stack");

//rename merge
IJ.run("Rename...", "title='Colour merge");


//close cyan copy
impCyancopy.changes = false;
winCyancopy.close();
//winMagenta.close();
IJ.run("Rename...", "title='Colour merge'");

	}

}

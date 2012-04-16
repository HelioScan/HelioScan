import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.plugin.*;


public class Biorad_Aligner implements PlugIn {

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


ImagePlus imp1= WindowManager.getCurrentImage(); 
ImageWindow winimp1= imp1.getWindow();
     if (imp1.getType()!=imp1.GRAY8) {
            IJ.showMessage("Biorad Aligner", "Image must be 8-bit grayscale.");
return;}

String Optotitle = imp1.getTitle();
int width = imp1.getWidth();
int height = imp1.getHeight();

int pscol1 = 1;
int pscol2 = 2;
String[] pscolours =  { "Green", "Red","Blue", "Grays","Cyan", "Magenta", "Yellow", "Orange ","Red Hot", "Green Hot", "Green Fire blue ", "Cyan Pale", "Magenta pale", "Yellow pale"};

String current = Optotitle;
        GenericDialog gd = new GenericDialog("Old Biorad Colour Merge");
   //     gd.addChoice("Image:", titles, titles[0]);
 //       gd.addChoice("Image:", titles, Optotitle);
      gd.addChoice("First colour", pscolours, pscolours[0]);
      gd.addChoice("Second colour", pscolours, pscolours[1]);
 gd.addCheckbox("Use 'Add' operator?", true);
		

		    gd.showDialog();
 int[] colourindex = new int [3];
//int imageindex = gd.getNextChoiceIndex();
int   colourindex1 = gd.getNextChoiceIndex();
  int colourindex2 = gd.getNextChoiceIndex();
  boolean UseOR = gd.getNextBoolean();

String firstcol = pscolours[colourindex1];
String secondcol = pscolours[colourindex2];


//setting roi width


	

imp1.setRoi (0,0, width/2, height );
IJ.run("Duplicate...", "title=Channel1 duplicate");
ImagePlus impch1= WindowManager.getCurrentImage(); 
ImageWindow winch1= impch1.getWindow();
WindowManager.setCurrentWindow(winch1);
IJ.run(firstcol);
IJ.run("RGB Color");

WindowManager.setCurrentWindow(winimp1);
imp1.setRoi((width/2)-1, 0,  width/2, height );
IJ.run("Duplicate...", "title=Channel2 duplicate");
ImagePlus impch2= WindowManager.getCurrentImage(); 
ImageWindow winch2= impch2.getWindow();
WindowManager.setCurrentWindow(winch2);
IJ.run(secondcol);
IJ.run("RGB Color");

if (UseOR==true)
IJ.run("Image Calculator...", "image1='Channel1' operation=Add  image2=Channel2 stack");

if (UseOR==false)
IJ.run("Image Calculator...", "image1='Channel1' operation=Difference image2=Channel2 stack");


//close cyan copy
impch2.changes = false;
impch1.changes = false;
winch2.close();
//winMagenta.close();
WindowManager.setCurrentWindow(winch1);
IJ.run("Rename...", "title='Colour merge'");

//rename merge


IJ.run("Rename...", "title='Colour merge");
	

			
			}
		
	}
	




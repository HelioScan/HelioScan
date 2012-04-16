import ij.process.*;
import ij.util.*;
import ij.gui.ImageWindow;
import ij.plugin.MacroInstaller;
import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.applet.Applet;
import java.awt.event.*;
import java.util.zip.*;



import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;

/**   
 * This ImageJ plugin sets "changes" to false for all windows and then
 *   closesAllWindows().  This is similar to the Dispose_ALL_Windows
 *   command in NucMed_Image a version of NIH_Image written
 *   by Mark D. Wittry, MD.
 *
 * @author Tony Collins, PhD <tonyc@uhnresearch.ca>
 * @author J. Anthony Parker, MD PhD <J.A.Parker@IEEE.org>
 * @version 8March2004
 */

public class Dispose_All_Windows implements PlugIn {

    public void run(String arg) {

        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            //  IJ.error("No images are open.");
        return;
        }

        if (IJ.altKeyDown()) {
            for (int i=0; i<wList.length; i++) {
                ImagePlus imp = WindowManager.getImage(wList[i]);
                imp.unlock();
                imp.changes = false;
                imp.getWindow().close();   
            }
            return;
        }
	
	boolean [] doClose = new boolean [wList.length];

        String[] items = {"Close selected windows without saving",
                            "Save changed windows before closing",
                            "Close only unchanged windows"};
        GenericDialog gd = new GenericDialog("Select Windows to Close");
       
gd.addChoice("", items, items[0]);
        for (int i=0; i<wList.length; i++) {
            String title;
            ImagePlus imp = WindowManager.getImage(wList[i]);
            title = imp.getTitle();
            if (imp.changes) title = title +" *";
            gd.addCheckbox(title,true);
        }

int rows = 15;
int columns = (int)Math.round(0.5+(double)wList.length/(double)rows);
rows =(int)Math.round((double)wList.length/(double)columns);

gd.addCheckBoxGroup(rows, columns, wList, doClose);

//get other windows

int nItems = window.getItemCount();
int nImages = wList.length;
IJ.showMessage("All= "+ nItems+"   Images= "+  nImages);

        gd.addMessage("Changed images marked with *\n"+
                    "Run with <alt> pressed to skip this screen");
        gd.showDialog();
        if (gd.wasCanceled())
                    return ;
        int index = gd.getNextChoiceIndex();
        for (int i=0; i<wList.length; i++) {
            if (gd.getNextBoolean()){
               ImagePlus imp = WindowManager.getImage(wList[i]);
                   switch(index) {
                    case 0:        // close everything
                        imp.changes = false;
                    case 2:        // close unchanged
                        if (imp.changes) break;       
                    case 1:        // save and close
                        imp.unlock();
                        imp.getWindow().close();
                        break;
                }
            }
        }

    }

}

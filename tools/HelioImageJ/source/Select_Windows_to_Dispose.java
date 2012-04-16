import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;
/**	
 * This ImageJ plugin sets "changes" to false for all windows and then
 *   closesAllWindows().  This is similar to the Dispose_ALL_Windows
 *   command in NucMed_Image a version of NIH_Image written 
 *   by Mark D. Wittry, MD.
 *
 * @author J. Anthony Parker, MD PhD &lt;J.A.Parker@IEEE.org&gt;
 * @version 31December2001
 */

//added option boxes to select which files to keep tonyc@uhnresearch.ca

public class Select_Windows_to_Dispose implements PlugIn {

	public void run(String arg) {

        int[] wList = WindowManager.getIDList();
        if (wList==null) {
          //  IJ.error("No images are open.");
            return;
        }

       
String title = "temp";
boolean shut,unchanged,prompt;

GenericDialog gd = new GenericDialog("Select Windows to dispose");

gd.addCheckbox("Close unchanged images only",false);
gd.addCheckbox("Prompt to save changed images",false);
gd.addMessage("");
gd.addMessage("Changed images marked with *");
for (int i=0; i<wList.length; i++) {
	ImagePlus imp = WindowManager.getImage(wList[i]);
	title = imp.getTitle();
	
if (imp.changes) title = title +" *";

	gd.addCheckbox(title,true);}

if (!IJ.altKeyDown()) gd.showDialog();
if (gd.wasCanceled())
            return ;
unchanged = gd.getNextBoolean();
prompt = gd.getNextBoolean();

if (!unchanged){
		for (int i=0; i<wList.length; i++) {
			shut = gd.getNextBoolean();
			if (shut){
				ImagePlus imp = WindowManager.getImage(wList[i]);
				imp.unlock();
				if (!prompt) imp.changes = false;
				imp.getWindow().close();	
				}
					}
		}
if (unchanged){
	for (int i=0; i<wList.length; i++) {
			ImagePlus imp = WindowManager.getImage(wList[i]);
			if (!imp.changes){		
				imp.unlock();
				imp.getWindow().close();	
					}
				}
					
		}

	}


}

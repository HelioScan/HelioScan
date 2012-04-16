
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.io.*;

public class Save_All implements PlugIn {

    public void run(String arg) {
          boolean makelist = true;
          FileInfo fi = null;
          ImagePlus imp;
          String title, path,messagestring;
          int[] wList = WindowManager.getIDList();
          if (wList==null) {
          IJ.error("No images are open.");
          return;  }
          if (IJ.altKeyDown()) makelist=false;  

          messagestring = "";
	String fullfilename = "";
	String filelistname ="";
          for (int i=0; i<wList.length; i++) {

	imp = WindowManager.getImage(wList[i]);
                ImageWindow winimp = imp.getWindow();
                WindowManager.setCurrentWindow(winimp);
	fi = imp.getOriginalFileInfo();
	title = imp.getTitle();

	if (!(fi==null)){
		path = fi.directory;
		fullfilename = path + title;	
		IJ.run("Save", "save='"+fullfilename+"'");}

//if image has not been saved yet - i.e. there's no file details then open a save dialog
	if(fi==null){
		IJ.run("Save");
		fullfilename = OpenDialog.getDefaultDirectory() + title;}

	messagestring = messagestring+"\n"+fullfilename;
       				 }

IJ.showMessage("Files saved: \n"+messagestring);
if (makelist) { IJ.write(messagestring); IJ.selectWindow("Results");}

                                          }
}



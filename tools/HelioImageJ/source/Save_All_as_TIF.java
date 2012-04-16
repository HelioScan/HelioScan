
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.io.*;

public class Save_All_as_TIF implements PlugIn {

    public void run(String arg) {

       
          FileInfo fi = null;
          ImagePlus imp;
          String title, path,messagestring;
          int[] wList = WindowManager.getIDList();
          if (wList==null) {
          IJ.error("No images are open.");
          return;  }
        
          messagestring = "";
	String fullfilename = "";
	String filelistname ="";
	String filetitle= "";
          for (int i=0; i<wList.length; i++) {

	imp = WindowManager.getImage(wList[i]);
                ImageWindow winimp = imp.getWindow();
                WindowManager.setCurrentWindow(winimp);
	
	IJ.run("Save");
	
	fi = imp.getOriginalFileInfo();
	title = imp.getTitle();

	int dotIndex = title.lastIndexOf(".");
           	 if (dotIndex>=0)
                // remove extension and replace with ".tif"
                	title = title.substring(0, dotIndex) + ".tif";
            	else
                	// add ".tif" extension
	                title += ".tif"; 

	if (!(fi==null)){
		path = fi.directory;
		
		fullfilename = path + title;	
		IJ.run("Save", "save='"+fullfilename+"'");}

//if image has not been saved yet - i.e. there's no file details then open a save dialog
	if(fi==null){
		IJ.run("Save");
		fullfilename = OpenDialog.getDefaultDirectory() + title;}

	messagestring = messagestring+fullfilename;
	if (!(i==wList.length-1)) messagestring = messagestring+"\n";
	int j = i+1;
	IJ.showStatus("Saving: "+j+"/"+wList.length);
	
       	 }
	if (IJ.altKeyDown()) { IJ.write(messagestring);
			IJ.selectWindow("Results");
			}
	if (!IJ.altKeyDown()) IJ.showMessage("Files saved: \n"+messagestring);
 	IJ.showStatus("");

                                          }
}



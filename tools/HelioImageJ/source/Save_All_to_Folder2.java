
import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.io.*;

public class Save_All_to_Folder2 implements PlugIn
{
    public void run(String arg) 
	{
	FileInfo fi = null;
	ImagePlus imp;
	String title, path,messagestring;
	int[] wList = WindowManager.getIDList();
	if (wList==null)
		{
		IJ.error("No images are open.");
		return;
		}
	messagestring = "";
	String fullfilename = "";
	String filelistname ="";
	String filetitle= "";
	

        GenericDialog gd = new GenericDialog("Select Windows to Close");
       
        for (int i=0; i<wList.length; i++) {
         //   String title;
            imp = WindowManager.getImage(wList[i]);

            title = imp.getTitle();
            if (imp.changes) title = title +" *";
            gd.addCheckbox(title,true);
        }
        gd.addMessage("Select images to save\n");
        gd.showDialog();
        if (gd.wasCanceled())
                    return ;

 	for (int j=0; j<wList.length; j++)
		 {
            		if (gd.getNextBoolean())
			{
			imp = WindowManager.getImage(1);
                		ImageWindow winimp = imp.getWindow();
	                	WindowManager.setCurrentWindow(winimp);
			title = imp.getTitle();
			SaveDialog sd = new SaveDialog("Select folder...", imp.getTitle(), ".tif");
			path  = sd.getDirectory();
			fullfilename = path + title;	
			messagestring = messagestring+fullfilename+"\n";
			IJ.saveAs("Tiff", fullfilename);
			for (int i=1; i<wList.length; i++)
				 {
				imp = WindowManager.getImage(wList[i]);
	                		winimp = imp.getWindow();
			                WindowManager.setCurrentWindow(winimp);
				title = imp.getTitle();
				int dotIndex = title.lastIndexOf(".");
           				if (dotIndex>=0)	title = title.substring(0, dotIndex) + ".tif";
	            		else	title += ".tif"; 
				fullfilename = path + title;	
				IJ.saveAs("Tiff",fullfilename);
				messagestring = messagestring+fullfilename;
				if (!(i==wList.length-1)) messagestring = messagestring+"\n";
				int k = i+1;
				IJ.showStatus("Saving: "+k+"/"+wList.length);
			       	 }
			}
	
		}
		IJ.showMessage("Files saved: \n"+messagestring);
 	IJ.showStatus("");
	}
}



import ij.plugin.*;
import ij.*;
import ij.io.*;
import ij.process.ImageProcessor;
import ij.process.TypeConverter;
import java.io.*;
import javax.swing.*;
import javax.swing.filechooser.*;
import java.util.Arrays;
import java.util.Vector;

/* Select a list of files and open them all as a stack

IMPORTANT: 
	- only jpg, png, gif and tiff will be loaded into an RGB stack
	- only first slice of multi-tif and multi-gif images is opened

This plugin by Albert Cardona  (albert@pensament.net ) at July 12 2003
modified from File_Opener.java available at http://rsb.info.nih.gov/ij/plugins

NOTE: only first file of list will open in some Java Virtual Machines
NOTE: in Mac OS X, multiple files open only if consecutive in selection
*/

/** Uses the JFileChooser from Swing to open one or more images. */

public class Open_as_Stack implements PlugIn {
	static File dir;

	public void run(String arg) {
		openFilesAsStack();
		IJ.register(Open_as_Stack.class);
	}

	public void openFilesAsStack() {
		JFileChooser fc = null;
		try {fc = new JFileChooser();}
		catch (Throwable e) {IJ.error("This plugin requires Java 2 or Swing."); return;}
		fc.setMultiSelectionEnabled(true);
		if (dir==null) {
			String sdir = OpenDialog.getDefaultDirectory();
			if (sdir!=null)
				dir = new File(sdir);
		}
		if (dir!=null)
			fc.setCurrentDirectory(dir);
		int returnVal = fc.showOpenDialog(IJ.getInstance());
		if (returnVal!=JFileChooser.APPROVE_OPTION)
			return;
		File[] files = fc.getSelectedFiles();
		if (files.length==0) { // getSelectedFiles does not work on some JVMs
			files = new File[1];
			files[0] = fc.getSelectedFile();
		}
		
		String supported_image_types = "tif jpg gif png";
		Vector v_files = new Vector();
		for (int n=0; n<files.length; n++) {
		    String name = files[n].getName().toLowerCase();
			if (-1 != supported_image_types.indexOf(name.substring(name.length()-3))) {
			    v_files.addElement(files[n]);
			}
		}
		
		File[] files2 = new File[v_files.size()];
		v_files.toArray(files2);
		
		String path = fc.getCurrentDirectory().getPath()+Prefs.getFileSeparator();
		dir = fc.getCurrentDirectory();
		Opener opener = new Opener();
		
		ImagePlus[] all_images = new ImagePlus[files2.length];
		int[] all_width = new int[files2.length];
		int[] all_height = new int[files2.length];
		
		for (int j=0; j<files2.length; j++) {
		    all_images[j] = opener.openImage(path, files2[j].getName());
		    all_width[j] = all_images[j].getWidth();
		    all_height[j] = all_images[j].getHeight();
		}
		
		int largest_width = 0;
		int largest_height = 0;  //initializing variables
		
		for (int k=0; k<files2.length; k++) {
		    if (all_width[k] > largest_width) largest_width = all_width[k];
		    if (all_height[k] > largest_height) largest_height = all_height[k];
		}
		
		ImageStack istack = new ImageStack(largest_width, largest_height);
		
		//int centerx, centery;
		
		for (int i=0; i<files2.length; i++) {
		    	String name = files2[i].getName();
			//if (-1 != supported_image_types.indexOf(name.substring(name.length()-3))) {
				    
				    ImageProcessor ipr = all_images[i].getProcessor();
				    int type = all_images[i].getType();
				    
				    if (type != ImagePlus.COLOR_RGB) {
				    	TypeConverter tp = new TypeConverter(ipr, false);
					ipr = tp.convertToRGB();
				    }
				    
				    ImageProcessor ipr2 = ipr.createProcessor(largest_width, largest_height);
				    ipr2.invert();
				    
				    //new, to center the images:
				    //centerx = largest_width/2 - all_width[i]/2;
				    //centery = largest_height/2 - all_height[i]/2;
				    //ipr2.insert(ipr, centerx, centery);
				    
				    ipr2.insert(ipr, largest_width/2 - all_width[i]/2, largest_height/2 - all_height[i]/2);
				    
				    //ipr2.insert(ipr, 0, 0);
				    istack.addSlice(name,ipr2);
			//}
		}
		
		ImagePlus stack2 = new ImagePlus("Stack", istack);
		stack2.show();
	}

}

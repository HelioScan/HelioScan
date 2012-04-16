//Plugin

// local thresholding and unsharp masking by median filtering
// Other operations can be substituted for median filter
// For unsharp masking just cancel thresholding
// Rex Couture 11-13-03
// Based on macro by Gabriel Landini.  Thanks to Wayne for help with Java.

import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import ij.plugin.*;

public class Local_Threshold implements PlugIn {

        public void run(String arg) {
                IJ.run("Duplicate...", "title=Original");
                IJ.run("32-bit");
                IJ.run("Duplicate...", "title=Filtered");
                double a = IJ.getNumber("Radius", 7);
                IJ.selectWindow("Filtered");
                IJ.run("Median...", "radius=" +a);
                IJ.run("Image Calculator...", "image1=Original operation=Subtract image2=Filtered create");
                IJ.run("Rename...", "title=Result");
                IJ.run("Threshold...");
        }

}




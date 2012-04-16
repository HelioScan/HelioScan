/*
 * Anisotropic diffusion plugin
 *
 * Copyright (c) 2003 by Christopher Mei (christopher.mei@sophia.inria.fr)
 *
 * This plugin is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this plugin; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;
import ij.plugin.frame.PlugInFrame;

import java.awt.*;
import java.util.*;

/**
 * This algorithm is an implementation of the Anisotropic
 * Diffusion from [PeronaMalik90].
 *  
 * @Article{PeronaMalik90,
 *  author =       "Pietro Perona and Jitendra Malik",
 *  title =        "Scale-Space and Edge Detection Using Anisotropic
 *                 Diffusion",
 *  journal =      "IEEE Transactions on Pattern Analysis and Machine
 *                 Intelligence",
 *  pages =        "629--639",
 *  volume =       "PAMI-12",
 *  number =       7,
 *  month =        jul,
 *  year =         1990,
 * }
 **/

public class AnisotropicDiffusion_ implements PlugIn {
    private int threshold;
    final static int HMIN = 0;
    final static int HMAX = 256;

    /** Number of iterations **/
    private int ITERATIONS;
    private int K;
    private float lambda;
    private boolean bigRegionFunction;

    private ImagePlus inputImage;

    public void run(String arg) {
	if (arg.equals("about"))
	    showAbout(); 
	
	 if(arg.equals("")) { 
	     if (!showDialog())
		 return;
	 }

	 applyAnisotropicDiffusion(inputImage.getProcessor());
    }

    public boolean showDialog() {
	String title = "TreeMixtureModeling";
	int[] wList = WindowManager.getIDList();
        if (wList==null || wList.length<1) { 
            IJ.showMessage(title, "There must be at least one window open");
            return false;
        }
	
	Vector pictures = new Vector();
        String[] titles = new String[wList.length];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null) {
                titles[i] = imp.getTitle();
		pictures.add(imp);
	    }
            else {
                titles[i] = "";
	 	pictures.add(null);
	    }        
	}
	
        GenericDialog gd = new GenericDialog(title);
        gd.addChoice("Picture :", titles, titles[0]);
	gd.addNumericField("Iterations :", 1 , 0);
	gd.addNumericField("K :", 10 , 0);
	gd.addNumericField("Lambda :", .2 , 2);
	gd.addCheckbox("Big region method (1/(1+...))", true);

	gd.showDialog();
        if (gd.wasCanceled())
            return false;

        int i1Index = gd.getNextChoiceIndex();
 	ITERATIONS = (int)gd.getNextNumber();
	K = (int)gd.getNextNumber();
	lambda = (float)gd.getNextNumber();
	bigRegionFunction = gd.getNextBoolean();

	inputImage = (ImagePlus)pictures.get(i1Index);

        return true;
    }

    public void applyAnisotropicDiffusion(ImageProcessor ip) {
	boolean debug = false;

	IJ.showStatus("Organising pixels...");
	IJ.showProgress(0.1);

	/** First step : the pixels are organised for easy access to neighbours**/
	AnisotropicDiffusionStructure anisotropicDiffusionStructure = new AnisotropicDiffusionStructure(ip);
	if(debug)
	    IJ.write(""+anisotropicDiffusionStructure);

	IJ.showProgress(0.5);
	IJ.showStatus("Initialising...");

	if(debug)
	    IJ.write("Starting algorithm...\n");

	/** Fix values for calculation **/
	AnisotropicDiffusionPixel.K = K;
	/** lambda must belong to [0 , 0.25] for stability */
	AnisotropicDiffusionPixel.lambda = lambda;

	IJ.showStatus("Iterating...");
	/** Start diffusion **/
	for(int i=0 ; i<ITERATIONS ; i++) {
	    IJ.showStatus("Iteration "+i+"/"+ITERATIONS+"...");
	    IJ.showProgress((i/ITERATIONS)*.4+0.5);

	    anisotropicDiffusionStructure.iterate(bigRegionFunction);
	}

	IJ.showProgress(0.9);
	IJ.showStatus("Putting result in a new image...");

	/** Put the result in a new image **/
	int width = ip.getWidth();

	ImageProcessor outputImage = new ByteProcessor(width, ip.getHeight());
	byte[] newPixels = (byte[]) outputImage.getPixels();

	for(int pixelIndex = 0 ; pixelIndex<anisotropicDiffusionStructure.size() ; pixelIndex++) {
	    AnisotropicDiffusionPixel p = anisotropicDiffusionStructure.get(pixelIndex);
	
	    newPixels[p.getX()+p.getY()*width] = p.getHeight();
	}

	IJ.showProgress(1);
	IJ.showStatus("Displaying result...");

	new ImagePlus("AnisotropicDiffusion", outputImage).show();
    }
    
    void showAbout() {
	IJ.showMessage("About AnisotropicDiffusion_...",
		       "This plug-in filter calculates the anisotropicDiffusion of a 8-bit images.\n"
		       );
    }
}




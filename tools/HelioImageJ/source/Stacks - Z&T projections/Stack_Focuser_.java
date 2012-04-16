/*
Mikhail Umorin <Mikhael_Umorin@baylor.edu>

09.10.02 (i.e. October 9th, 2002)
modified 
10.07.03 -- added generation of height map;
4.08.03 -- changed interface for k_size and yes/no height map;
7.08.03 -- Plugin can be initialized with arg string to setup method and then runs non-interactively;

Inspired by the mentioning of the capability of "flattening" a set of images of 
different focal planes in ImagePro 4.0.
The author, however came up with the algorithm idea (and implementation) totally 
on his own without any reference to any similar algorithms,
printed, digital, or otherwise. The author is open to any suggestions regarding 
algorithm(s), implementation, 
and/or features of the program. The program may be distributed and modified 
freely with the reference of the original source. No implicit or explicit warranty 
or suitabiluty for a particular purpose is given.

Contains a very simple algorythm of patching a *focused * image from a stack of 
images corresponding to
different focal planes. It is very important that images in the stack are of the 
same brightness; otherwise pasting together
pieces of different images will create artificial edges.

The principle:  
1) find edges for each image in a stack by running a Sobel
filter (after noise-reducing 3x3 median filter);
2) create a "map" of how far
the influence of edges extends; i.e. how far from a focused edge we still think 
that the image is focused
by taking the maximum value in the neighborhood of the specified size; 
3)  for
every pixel (x, y) based on the choice
of the maximum "edge" value among different slices in the "map" stack
copy the pixel value from the appropriate original image to the new image.

Program works on 8-bit and 16-bit grayscale stacks, and accepts rectangular ROIs; 
 if no ROI is given, plugin works on the whole image; 
plugin converts stacks
to 32-bit float to preserve precision before any manipulation is performed. The 
size in pixels (odd integer > 1) of the Maximum
square filter is requested; trial and error would be the fastest way to optimize 
the result. The final image is written to
"Focused"+<original stack title> window, but not saved on the disk (the user can do that him/her
self). Optionally, the plugin generates a "height map", i.e. an image of the heights of 
focused parts of the image.
The home-grown  maximum filter  maxFilter  has a square kernel and is MUCH 
faster
then available in Process -> Filters -> Maximum menu.
The sacrifice in quality is believed negligible for this kind of application 
even though
the squareness makes it anisotropic (?)

For a short but good reference on image analysis see
http://www.cee.hw.ac.uk/hipr/html/hipr_top.html
*/

/*
TO DO:
use of arg passed to setup to initialize the plugin and run non-interactively
*/


import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import java.awt.*;
import java.awt.image.*;


public class Stack_Focuser_ implements PlugInFilter
{
	private ImageStack i_stack, m_stack;
	private static final int BYTE=0, SHORT=1, FLOAT=2, RGB=3;
	protected int k_size;
	protected int type;
	protected int o_width, o_height, n_width, n_height, n_slices;
	protected int o_dim, n_dim;
	private Rectangle r;
	private String o_title;
	private boolean create_map = false;
	private GenericDialog input_dialog;
	private boolean interact = true;


	public int setup(String arg, ImagePlus imp)
	{
		k_size = 11;
		if (arg.equalsIgnoreCase("about"))
		{
			showAbout();
			return DONE;
		}
		// check if the arg string has parameters to set
		if (arg.indexOf("ksize=")>=0)
		{interact = false;
		int pos = arg.indexOf("ksize=")+6;
		if (arg.charAt(pos)!=' ')
			{String kss;
			int posn = arg.indexOf(' ', pos+1);
			if (posn >0) {
				kss = arg.substring(pos, posn);}
				else{kss = arg.substring(pos);}
			k_size = Integer.parseInt(kss);}
		}
		if (arg.indexOf("hmap=")>=0)
		{interact = false;
		int pos = arg.indexOf("hmap=")+5;
		if (arg.charAt(pos)!=' ')
			{String hms;
			int posn = arg.indexOf(' ', pos+1);
			if (posn>0){
				hms = arg.substring(pos, posn);}
				else{hms = arg.substring(pos);}
			create_map = hms.equalsIgnoreCase("true");}
		}

		if (imp==null)
                        {IJ.noImage(); return DONE;}

		ImageProcessor ip_p = imp.getProcessor();
		o_title = imp.getTitle();
		int dot_i = o_title.indexOf(".");
		if (dot_i>0)
			o_title = o_title.substring(0, dot_i);
		if (ip_p instanceof ByteProcessor)
		 	type = BYTE;
		else if (ip_p instanceof ShortProcessor)
		 	type = SHORT;
		else if (ip_p instanceof FloatProcessor)
		 	type = FLOAT;
		else
			type = RGB;
		i_stack = imp.getStack();
		o_width = imp.getWidth();
		o_height = imp.getHeight();
		o_dim = o_width*o_height;
		n_slices =imp.getStackSize();
		// obtain ROI and if ROI not set set ROI to the whole image
		r = i_stack.getRoi();
		if ((r == null) || (r.width<2)||(r.height<2))
		{
			r = new Rectangle(0, 0, o_width, o_height);
			i_stack.setRoi(r);
		}
		n_width = r.width;
		n_height = r.height;
		n_dim = n_width*n_height;
		return DOES_8G+DOES_16+STACK_REQUIRED+NO_CHANGES+NO_UNDO;
	}


	public void run(ImageProcessor ip)
	{
		ImageProcessor dfloat_ip;
		ImageProcessor i_ip;
		ImageProcessor m_ip;
		// read options
		// allow for different x and y kern_size later
		if (interact) {
		    input_dialog = new GenericDialog("Options");
		    input_dialog.addNumericField("Enter the n (>2) for n x n kernel: ", k_size, 0);
		    input_dialog.addCheckbox("Generate height map", false);
		    input_dialog.showDialog();
		    if (input_dialog.wasCanceled()) return;
		    k_size = (int)input_dialog.getNextNumber();
		    create_map = input_dialog.getNextBoolean();
		    if ( input_dialog.invalidNumber() || k_size<3 ) {
		    	IJ.error("Invalid number or " +k_size+" is incorrect! ");
		    	return;
		    }
		}
		// now lets create a stack for max in the neighbourhood
		float[] m_slice;
		m_stack = new ImageStack(n_width, n_height);
		// match input stack and the new one slice by slice
		IJ.showProgress(0.0f);
		for (int i=1; i<=n_slices; i++)
		{
			IJ.showStatus("Converting...");
			i_ip = i_stack.getProcessor(i);
			// copy from input stack to the newer one slice by slice and
			// convert to floating point grayscale to avoid precision loss later
			int o_offset, o_i, ii;
			float[] dfloat_array = new float[n_dim];
			switch (type)
			{
				case BYTE:
					ii = 0;
					byte[] bi_pixels = (byte[])i_ip.getPixels();
					for (int y=r.y; y<(r.y+r.height); y++)
					{
						o_offset = y*o_width;
						for(int x=r.x; x<(r.x+r.width); x++)
						{
							o_i = o_offset+x;
							dfloat_array[ii] = bi_pixels[o_i]&0xff;
							ii++;
						}
					}
					break;
				case SHORT:
					ii = 0;
					short[] si_pixels = (short[])i_ip.getPixels();
					for (int y=r.y; y<(r.y+r.height); y++)
					{
						o_offset = y*o_width;
						for(int x=r.x; x<(r.x+r.width); x++)
						{
							o_i = o_offset+x;
							dfloat_array[ii] = si_pixels[o_i]&0xffff;
							ii++;
						}
					}
					break;
				default:
					break;
			}// switch
			dfloat_ip = new FloatProcessor(n_width, n_height, dfloat_array, i_ip.getColorModel());
			// run median filter on the new one to get rid of some noise
			dfloat_ip.medianFilter();
			// run Sobel edge detecting filter
			IJ.showStatus("Finding edges....");
			dfloat_ip.findEdges();
			// run Max filter
			m_slice = new float[n_dim];
			m_ip = new FloatProcessor(n_width, n_height, m_slice, null);
			//  a dialog with user input at trhe beginning of run specifies k_size.
			IJ.showStatus("Applying "+k_size+"x"+k_size+" filter...");
			maxFilter(dfloat_ip, m_ip, k_size);
			// and add to the new stack
			m_stack.addSlice(null, m_ip);
			IJ.showProgress(1.0*i/n_slices);
		}
		// now by comparing max values for the same point in different slices we decide which
		// original slice to use to paste into the new image at that location
		byte[] orig_pixels8 = null;
		short[] orig_pixels16 = null;
		byte[] dest_pixels8 = null;
		short[] dest_pixels16 = null;
		//
		ImagePlus focused = null;
		ImageProcessor foc_ip = null;
		//
		ImagePlus height_map = null;
		ImageProcessor h_ip = null;
		int scale = 0;
		// prepare height map
		if (create_map) {
		h_ip = new ByteProcessor(n_width, n_height);
		scale = 255/n_slices;
		}
		//
		switch (type)
		{
			case BYTE:
				dest_pixels8 = new byte[n_dim];
				break;
			case SHORT:
				dest_pixels16 = new short[n_dim];
				break;
			default:
				break;
		}
		int offset, i;
		int copy_i, copy_x, copy_y;
		int pix;
		IJ.showStatus("Pasting the new image...");
		IJ.showProgress(0.0f);
		for (int y=0; y<n_height; y++)
		{
			offset = n_width*y;
			for (int x=0; x<n_width; x++)
			{
				i = offset + x;
				float[] curr_pixels;
				float max_e = 0.0f;
				int max_slice = 1;
				int z;
				// find the slice to copy from
				for (z=1; z<=m_stack.getSize(); z++)
				{
					curr_pixels = (float[]) m_stack.getPixels(z);
					if (curr_pixels[i]>max_e)
					{
						max_e = curr_pixels[i];
						max_slice = z;
					}
				}
				copy_x = r.x+x;
				copy_y = r.y+y;
				copy_i = copy_x+copy_y*o_width;
				if (create_map) {
					h_ip.putPixel(x, y, max_slice*scale);}
				switch (type)
				{
					case BYTE:
						orig_pixels8 = (byte[]) i_stack.getPixels(max_slice);
						dest_pixels8[i] = orig_pixels8[copy_i];
						break;
					case SHORT:
						orig_pixels16 = (short[]) i_stack.getPixels(max_slice);
						dest_pixels16[i] = orig_pixels16[copy_i];
						break;
					default:
						break;
				}
				IJ.showStatus("Pasting the new image...");
				IJ.showProgress(1.0*i/n_dim);
			}
		}
		m_stack = null;
		switch (type)
		{
			case BYTE:
				foc_ip = new ByteProcessor(n_width, n_height, dest_pixels8, null);
				break;
			case SHORT:
				foc_ip = new ShortProcessor(n_width, n_height, dest_pixels16, null);
				break;
			default:
				break;
		}
		// construct the title of the new window
		String n_title = "Focused_"+o_title;
		focused = new ImagePlus(n_title, foc_ip);
		focused.show();
		focused.updateAndDraw();
		if (create_map) {
		String nm_title = "HeightMap_"+o_title;
		height_map = new ImagePlus(nm_title, h_ip);
		height_map.show();
		height_map.updateAndDraw();}
	}


	void maxFilter(ImageProcessor source_ip, ImageProcessor dest_ip, int kern_size)
	{
	// expects FloatProcessor as source_ip and dest_ip; and odd k_size
			float[] dest_pixels = (float[]) dest_ip.getPixels();
			int width = source_ip.getWidth();
			int height = source_ip.getHeight();
			int offset, i;
			for (int y=0; y<height; y++)
			{
				offset = width*y;
				for (int x=0; x<width; x++)
				{
					i = offset + x;
					dest_pixels[i] = findMaxInNeigh(source_ip, x, y, kern_size, kern_size);
				}
			}
	}


	float findMaxInNeigh(ImageProcessor ip_, int center_x, int center_y, int size_x, int size_y)
	{
	// expects FloatProcessor as ip_
	//  size>1; odd and even do not matter, i.e. size=2 is same as size=3
		float maxVal = 0.0f;
		int width_ = ip_.getWidth();
		int height_ = ip_.getHeight();
		float[] pixels_ = (float[]) ip_.getPixels();
		int half_x= size_x / 2;
		int half_y= size_y / 2; 
		int start_x = center_x-half_x;
		int start_y = center_y-half_y;
		if (start_x<0) {start_x = 0;}
		if (start_y<0) {start_y = 0;}
		int end_x = center_x+half_x;
		int end_y = center_y+half_y;
		if (end_x>width_) {end_x = width_;}
		if (end_y>height_) {end_y = height_;}
		int offset_, i_;
		for (int y=start_y; y<end_y; y++)
		{
			offset_ = width_*y;
			for (int x=start_x; x<end_x; x++)
			{
				i_ = offset_ + x;
				if (pixels_[i_]>maxVal) {maxVal = pixels_[i_];}
			}
		}
		return maxVal;
	}


	void showAbout()
	{
		IJ.showMessage("About Stack Focuser...",
					"Patches a *focused* image\n"+
					" from a stack of images \n"+
					"corresponding to different focal planes\n"+
					"\n Mikhail Umorin <Mikhael_Umorin@baylor.edu>");
	}
}

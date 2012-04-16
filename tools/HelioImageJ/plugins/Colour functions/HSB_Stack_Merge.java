import ij.*;
import ij.plugin.PlugIn;
import ij.process.*;
import ij.gui.*;

public class HSB_Stack_Merge implements PlugIn {

    private ImagePlus imp;
    private byte[] blank;
 
    /** Merges one, two or three 8-bit images or stacks into a single HSB stack. */

	public void run(String arg) {
		 imp = WindowManager.getCurrentImage();
       		 MergeStacks();
  	  }

  /** Combines three grayscale stacks into one RGB stack. */
    public void MergeStacks() {
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

        GenericDialog gd = new GenericDialog("HSB Stack Merge");
        gd.addChoice("Hue Stack:", titles, titles[0]);
        gd.addChoice("Saturation Image:", titles, titles[1]);
        String title3 = titles.length>2?titles[2]:none;
        gd.addChoice("Brightness Image:", titles, title3);
        gd.addCheckbox("Keep source stacks", false);
        gd.showDialog();
        if (gd.wasCanceled())
            return;
        int[] index = new int[3];
        index[0] = gd.getNextChoiceIndex();
        index[1] = gd.getNextChoiceIndex();
        index[2] = gd.getNextChoiceIndex();
        boolean keep = gd.getNextBoolean();

        ImagePlus[] image = new ImagePlus[3];
        int stackSize = 0;
        int width = 0;
        int height = 0;
        for (int i=0; i<3; i++) {
            if (index[i]<wList.length) {
                image[i] = WindowManager.getImage(wList[index[i]]);
                width = image[i].getWidth();
                height = image[i].getHeight();
                stackSize = image[i].getStackSize();
            }
        stackSize = image[0].getStackSize();
        }
        if (width==0) {
            IJ.error("There must be at least one 8-bit or RGB source stack.");
            return;
        }
        for (int i=0; i<3; i++) {
            ImagePlus img = image[i];
            if (img!=null) {
               // if (img.getStackSize()!=stackSize) {
                //    IJ.error("The source stacks must all have the same number of slices.");
                //    return;
                //}
                if (!(img.getType()==ImagePlus.GRAY8)) {
                    IJ.error("The source stacks must be 8-bit grayscale.");
                    return;
                }
                if (img.getWidth()!=width || image[i].getHeight()!=height) {
                    IJ.error("The source stacks must have the same width and height.");
                    return;
                }
            }
        }

        ImageStack hue = image[0]!=null?image[0].getStack():null;
        ImageStack sat = image[1]!=null?image[1].getStack():null;
        ImageStack bright = image[2]!=null?image[2].getStack():null;
        ImageStack hsb = MergeStacks(width, height, stackSize, hue, sat, bright, keep);
        if (!keep)
            for (int i=0; i<3; i++) {
                if (image[i]!=null) {
                    image[i].changes = false;
                    ImageWindow win = image[i].getWindow();
                    if (win!=null)
                        win.close();
                }
            }
        new ImagePlus("HSB", hsb).show();
    }
    
    public ImageStack MergeStacks(int w, int h, int d, ImageStack hue, ImageStack sat, ImageStack bright, boolean keep) {
        ImageStack hsb = new ImageStack(w, h);
        int inc = d/10;
        if (inc<1) inc = 1;
        ColorProcessor cp;
        int slice = 1;
        blank = new byte[w*h];
        byte[] huePixels, satPixels, brightPixels;
            boolean invertedHue = hue!=null?hue.getProcessor(1).isInvertedLut():false;
            boolean invertedSat = sat!=null?sat.getProcessor(1).isInvertedLut():false;
            boolean invertedBright = bright!=null?bright.getProcessor(1).isInvertedLut():false;
        try {
            for (int i=1; i<=d; i++) {
            cp = new ColorProcessor(w, h);
                huePixels = getPixels(hue, slice, 0);
                // satPixels = getPixels(sat, slice, 1);
                //brightPixels = getPixels(bright, slice, 2);
                satPixels = getPixels(sat, 1, 1);
               brightPixels = getPixels(bright, 1, 2);
                if (invertedHue) invert(huePixels);
                if (invertedSat) invert(satPixels);
                if (invertedBright) invert(brightPixels);
                cp.setHSB(huePixels, satPixels, brightPixels);
            if (keep) {
                slice++;
                    if (invertedHue) invert(huePixels);
                    if (invertedSat) invert(satPixels);
                    if (invertedBright) invert(brightPixels);
            } else {
                    if (hue!=null) hue.deleteSlice(1);
                //if (sat!=null &&sat!=hue) sat.deleteSlice(1);
                //if (bright!=null&&bright!=hue && bright!=sat) bright.deleteSlice(1);
                //System.gc();
            }
            hsb.addSlice(null, cp);
            if ((i%inc) == 0) IJ.showProgress((double)i/d);
            }
        IJ.showProgress(1.0);
        } catch(OutOfMemoryError o) {
            IJ.outOfMemory("MergeStacks");
            IJ.showProgress(1.0);
        }
        return hsb;
    }
    
     byte[] getPixels(ImageStack stack, int slice, int color) {
         if (stack==null)
            return blank;
        if (stack.getPixels(slice) instanceof byte[])
            return (byte[])stack.getPixels(slice);
        else {
            byte[] h,s,b;
            int size = stack.getWidth()*stack.getHeight();
            h = new byte[size];
            s = new byte[size];
            b = new byte[size];
            ColorProcessor cp = (ColorProcessor)stack.getProcessor(slice);
            cp.getHSB(h, s, b);
            switch (color) {
                case 0: return h;
                case 1: return s;
                case 2: return b;
            }
        }
        return null;
    }

    void invert(byte[] pixels) {
        for (int i=0; i<pixels.length; i++)
            pixels[i] = (byte)(255-pixels[i]&255);
    }

}

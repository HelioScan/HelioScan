import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;

/** This plugin does various calculations on two images or stacks.
*/ 

public class Calculator_Plus implements PlugIn {

    static String title = "Calculator Plus";
    static final int SCALE=0, ADD=1, SUBTRACT=2, MULTIPLY=3, DIVIDE=4;
    static String[] ops = {"Scale: i2 = i1 x k1 + k2", "Add: i2 = (i1+i2) x k1 + k2", "Subtract: i2 = (i1-i2) x k1 + k2",
        "Multiply: i2 = (i1*i2) x k1 + k2", "Divide: i2 = (i1/i2) x k1 + k2"};
    static int operation = SCALE;
    static double k1 = 1;
    static double k2 = 0;
    static boolean createWindow = true;
    int[] wList;
    private String[] titles;
    int i1Index;
    int i2Index;
    ImagePlus i1;
    ImagePlus i2;
    boolean replicate;

    public void run(String arg) {
        if (IJ.versionLessThan("1.27w"))
            return;
        wList = WindowManager.getIDList();
        if (wList==null || wList.length<2) {
            IJ.showMessage(title, "There must be at least two windows open");
            return;
        }
        titles = new String[wList.length];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null)
                titles[i] = imp.getTitle();
            else
                titles[i] = "";
        }
        
        if (!showDialog())
            return;
        
        long start = System.currentTimeMillis();
        boolean calibrated = i1.getCalibration().calibrated() || i2.getCalibration().calibrated();
        if (calibrated)
            createWindow = true;
        if (createWindow) {
            if (replicate)
                i2 = replicateImage(i2, calibrated, i1.getStackSize());
            else
               i2 = duplicateImage(i2, calibrated);
            if (i2==null)
                {IJ.showMessage(title, "Out of memory"); return;}
            i2.show();
        } 
        calculate(i1, i2, k1, k2);
        IJ.showStatus(IJ.d2s((System.currentTimeMillis()-start)/1000.0, 2)+" seconds");
    }
    
    public boolean showDialog() {
        GenericDialog gd = new GenericDialog(title);
        gd.addChoice("i1:", titles, titles[0]);
        gd.addChoice("i2:", titles, titles[1]);
        gd.addChoice("Operation:", ops, ops[operation]);
        gd.addNumericField("k1:", k1, 1);
        gd.addNumericField("k2:", k2, 1);
        gd.addCheckbox("Create New Window", createWindow);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        int i1Index = gd.getNextChoiceIndex();
        int i2Index = gd.getNextChoiceIndex();
        operation = gd.getNextChoiceIndex();
        k1 = gd.getNextNumber();
        k2 = gd.getNextNumber();
        createWindow = gd.getNextBoolean();
        i1 = WindowManager.getImage(wList[i1Index]);
        i2 = WindowManager.getImage(wList[i2Index]);
        int d1 = i1.getStackSize();
        int d2 = i2.getStackSize();
        if (d2==1 && d1>1) {
            createWindow = true;
            replicate = true;
        }
        return true;
    }

    public void calculate(ImagePlus i1, ImagePlus i2, double k1, double k2) {
        double v1, v2;
        int width  = i1.getWidth();
        int height = i1.getHeight();
        ImageProcessor ip1, ip2;
        int slices1 = i1.getStackSize();
        int slices2 = i2.getStackSize();
        float[] ctable1 = i1.getCalibration().getCTable();
        float[] ctable2 = i2.getCalibration().getCTable();
        ImageStack stack1 = i1.getStack();
        ImageStack stack2 = i2.getStack();
        int currentSlice = i2.getCurrentSlice();

        for (int n=1; n<=slices2; n++) {
            ip1 = stack1.getProcessor(n<=slices1?n:slices1);
            ip2 = stack2.getProcessor(n);
            ip1.setCalibrationTable(ctable1);
            ip2.setCalibrationTable(ctable2);
            for (int x=0; x<width; x++) {
                for (int y=0; y<height; y++) {
                    v1 = ip1.getPixelValue(x,y);
                    v2 = ip2.getPixelValue(x,y);
                    switch (operation) {
                        case SCALE: v2 = v1; break;
                        case ADD: v2 += v1; break;
                        case SUBTRACT: v2 -= v1; break;
                        case MULTIPLY: v2 *= v1; break;
                        case DIVIDE: v2 = v2!=0.0?v1/v2:0.0; break;
                    }
                    v2 = v2*k1 + k2;
             ip2.putPixelValue(x, y, v2);
                }   
            }  
            if (n==currentSlice) {
                i2.getProcessor().resetMinAndMax();
                i2.updateAndDraw();
            }     
            IJ.showProgress((double)n/slices2);
            IJ.showStatus(n+"/"+slices2);
        }
    }

   ImagePlus duplicateImage(ImagePlus img1, boolean calibrated) {
        ImageStack stack1 = img1.getStack();
        int width = stack1.getWidth();
        int height = stack1.getHeight();
        int n = stack1.getSize();
        ImageStack stack2 = img1.createEmptyStack();
        float[] ctable = img1.getCalibration().getCTable();
        try {
            for (int i=1; i<=n; i++) {
                ImageProcessor ip1 = stack1.getProcessor(i);
                ImageProcessor ip2 = ip1.duplicate(); 
                if (calibrated) {
                    ip2.setCalibrationTable(ctable);
                    ip2 = ip2.convertToFloat();
                }
                stack2.addSlice(stack1.getSliceLabel(i), ip2);
            }
        }
        catch(OutOfMemoryError e) {
            stack2.trim();
            stack2 = null;
            return null;
        }
        ImagePlus img2 =  new ImagePlus("Result", stack2);
        return img2;
    }

  ImagePlus replicateImage(ImagePlus img1, boolean calibrated, int n) {
        ImageProcessor ip1 = img1.getProcessor();
        int width = ip1.getWidth();
        int height = ip1.getHeight();
        ImageStack stack2 = img1.createEmptyStack();
        float[] ctable = img1.getCalibration().getCTable();
        try {
            for (int i=1; i<=n; i++) {
                ImageProcessor ip2 = ip1.duplicate(); 
                if (calibrated) {
                    ip2.setCalibrationTable(ctable);
                    ip2 = ip2.convertToFloat();
                }
                stack2.addSlice(null, ip2);
            }
        }
        catch(OutOfMemoryError e) {
            stack2.trim();
            stack2 = null;
            return null;
        }
        ImagePlus img2 =  new ImagePlus("Result", stack2);
        return img2;
    }

} 


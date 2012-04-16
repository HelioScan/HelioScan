import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.plugin.PlugIn;
import java.text.DecimalFormat; 

public class Image_Correlator_plus implements PlugIn {

    private static int index1;
    private static int index2;
    private static boolean displayCounts;
    private ImagePlus imp1, imp2;
private static boolean threshold;
            
    public void run(String arg) {
        if (showDialog())
            correlate(imp1, imp2);
    }
    
    public boolean showDialog() {
        int[] wList = WindowManager.getIDList();
        if (wList==null) {
            IJ.noImage();
            return false;
        }
        String[] titles = new String[wList.length];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null)
                titles[i] = imp.getTitle();
            else
                titles[i] = "";
        }
        if (index1>=titles.length)index1 = 0;
        if (index2>=titles.length)index2 = 0;
       displayCounts = false;
	threshold = false;
        GenericDialog gd = new GenericDialog("Image Correlator");
        gd.addChoice("Red ", titles, titles[index1]);
        gd.addChoice("Green ", titles, titles[index2]);
        gd.addCheckbox("Display Counts: ", displayCounts);
        gd.addCheckbox("Use threshold:",threshold);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        index1 = gd.getNextChoiceIndex();
        index2 = gd.getNextChoiceIndex();
        displayCounts = gd.getNextBoolean();
threshold = gd.getNextBoolean();
        String title1 = titles[index1];
        String title2 = titles[index2];
        imp1 = WindowManager.getImage(wList[index1]);
        imp2 = WindowManager.getImage(wList[index2]);
        if (imp1.getType()!=imp1.GRAY8 || imp2.getType()!=imp1.GRAY8) {
            IJ.showMessage("Image Correlator", "Both images must be 8-bit grayscale.");
            return false;
        }
        return true;
   }
    
    public void correlate(ImagePlus imp1, ImagePlus imp2) {
String Ch1fileName = imp1.getTitle();
String Ch2fileName = imp2.getTitle();
String fileName = Ch1fileName +  " and " + Ch2fileName;


        ImageProcessor ip1 = imp1.getProcessor();
        ImageProcessor ip2 = imp2.getProcessor();


int greenthreshmin=0;
int greenthreshmax=255;
int redthreshmin=0;
int redthreshmax=255;

if((threshold)&(ip2.getMinThreshold()!=ip2.NO_THRESHOLD)){
greenthreshmin=(int)ip2.getMinThreshold();
greenthreshmax=(int)ip2.getMaxThreshold();
redthreshmin=(int)ip1.getMinThreshold();
redthreshmax=(int)ip1.getMaxThreshold();
}

        boolean unsigned = true;
        ImageProcessor plot = new FloatProcessor(256, 256);
 ImageProcessor plot16bit = new ShortProcessor(256, 256);
  int nslices = imp1.getStackSize();
        int width = imp1.getWidth();
        int height = imp1.getHeight();
        int z1, z2, z3, count;
double sumX = 0;
double sumXY = 0;
double sumXX = 0;
double sumYY = 0;
double sumY = 0;
double colocX = 0;
double colocY = 0;
double countX = 0;
double countY = 0;
double Bred = 0;
double Bgreen = 0;
int N = 0;
int N2 = 0;
int Nzero=0;

//start pixel counting

for (int i=1; i<=nslices; i++) {
                imp1.setSlice(i);
                imp2.setSlice(i);
        	for (int y=0; y<height; y++) {
            		for (int x=0; x<width; x++) {
           		    	 z1 = (int)ip1.getPixel(x,y); // z-value of pixel (x,y)in image #1
		    	z2 = (int)255-ip2.getPixel(x,y); // z-value of pixel (x,y)in image #2
			z3 = (int)ip2.getPixel(x,y);
		
	if((redthreshmin<=z1)&(z1<=redthreshmax)&(greenthreshmin<=z3)&(z3<=greenthreshmax))	{
				sumX = sumX+z1;
				sumXY = sumXY + (z1 * z3);
				sumXX = sumXX + (z1 * z1);
				sumYY = sumYY + (z3 * z3);
				sumY = sumY + z3;
				if (z3!=0){
					colocX = colocX + z1;
					countY = countY + 1;}	
					if (z1!=0) {
						colocY = colocY + z3;
						countX = countX + 1;}
				N = N+1;
		//caclulates nu,ber of non-zero pixels...
				if ((z1==0)&(z3==0)){
					Nzero = Nzero+1;	}
			                count = plot.getPixel(z1, z2);
			                count++;
			                plot.putPixel(z1, z2, count);
				 if (count<65535) count++;
			                plot16bit.putPixel(z1, z2, count);
					}
            				}
			        }
	}

//finished pixel counting


        plot.invertLut();
        plot.resetMinAndMax();
new ImagePlus("Correlation Plot", plot16bit).show();
ImagePlus imp3 = WindowManager.getCurrentImage();
       if (displayCounts)
        displayCounts(plot);
plot16bit.resetMinAndMax();
N2=N-Nzero;
double pearsons1 = sumXY - (sumX*sumY/N);// =U24-(U25*U27/C2)
double pearsons2 = sumXX - (sumX*sumX/N);//=U26-((U25^2)/C2)
double pearsons3 = sumYY - (sumY*sumY/N);
double r = pearsons1/(Math.sqrt(pearsons2*pearsons3));

pearsons1 = sumXY - (sumX*sumY/N2);// =U24-(U25*U27/C2)
pearsons2 = sumXX - (sumX*sumX/N2);//=U26-((U25^2)/C2)
pearsons3 = sumYY - (sumY*sumY/N2);

double r2= pearsons1/(Math.sqrt(pearsons2*pearsons3));

double overlap = sumXY / (Math.sqrt(sumXX*sumYY));
double k1 = sumXY/sumXX;
double k2 = sumXY/sumYY;
double colocM1 = colocX/sumX;
double colocM2 = colocY/sumY;
double pixelratio = countX/countY;


 DecimalFormat df = new DecimalFormat("##0.000");
 IJ.write("\n\nColocalisation coefficients for "+ fileName+
"\n\nPearson's correlation  coeff. (Rp) = " + df.format(r) +
//"\nPearson's correlation  coeff. excluding black pixels (Rp) = " + df.format(r2) +
"\nOverlap coeff. (R) = " +df.format(overlap)+
//"\nOverlap coefficient for channel 1(k1)=" +df.format(k1)+
//"\nOverlap coefficient for channel 2 (k2) =" +df.format(k2)+
"\nCh1:Ch2 pixel ratio = " + df.format(pixelratio) +
"\nColocalisation coefficient for red (Mred) = " +df.format(colocM1)+
"\nColocalisation coefficient for green (Mgreen) = " +df.format(colocM2)+
"\nred threshold min= "+redthreshmin+
"\nred threshold max= "+redthreshmax+
"\ngreen threshold min= "+greenthreshmin+
"\ngreen threshold max= "+greenthreshmax

);



IJ.run("Enhance Contrast", "saturated=0.5 equalize");
IJ.run("Fire");
imp3.setTitle(fileName + " Freq. CP");


IJ.selectWindow("Results");
IJ.run("Duplicate...", "title='Red-Green Correlation Plot'");
		IJ.run("Grays");
		IJ.run("8-bit");
		IJ.setThreshold(1, 255);
IJ.run("Threshold", "thresholded remaining black");
		IJ.run("Select All");
		IJ.run("Copy");
		IJ.run("Close");
		 String path = Menus.getPlugInsPath();
                                 path = "open='"+path+"red green.tif'";
		IJ.run("Open...", path);
	ImagePlus imp4 = WindowManager.getCurrentImage();
	IJ.setPasteMode("Subtract");
		IJ.run("Paste");
imp4.setTitle(fileName+ " Red-Green CP");		
IJ.run("Enhance Contrast", "saturated=0.5 equalize");
IJ.setPasteMode("Copy");
    }

    void displayCounts(ImageProcessor plot) {
        StringBuffer sb = new StringBuffer();
        int count;
        for (int x=0; x<256; x++)
            for (int y=255; y>=0; y--) {
                count = plot.getPixel(x,y);
                if (count>0)
                    sb.append(x+"\t"+(255-y)+"\t"+count+"\n");
            }
       new TextWindow( "Pixel intensities and frequencies", "Ch1\tCh2\tFreq.", sb.toString(), 300, 400);

       }
 
}

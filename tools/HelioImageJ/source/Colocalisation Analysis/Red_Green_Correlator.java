import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.plugin.PlugIn;
import java.text.DecimalFormat; 
import ij.plugin.filter.PlugInFilter;


public class Red_Green_Correlator implements PlugIn {

private static int index1;
private static int index2;
private static boolean displayCounts;
private ImagePlus imp1, imp2;
public String fileName;
public String ROIstring;
public boolean red = true;
public boolean green = true;
public boolean blue = false;
public String channels = "";


public void run(String arg) {

	ImagePlus imp = WindowManager.getCurrentImage();
	if (imp==null)
           		 {IJ.noImage(); return;}
	fileName = imp.getTitle();
	ImageProcessor ip = imp.getProcessor();
	if (imp.getType()!=imp.COLOR_RGB){
		IJ.showMessage("RGB Color Comparison", "Image must be RGB.");
		return;
		}

Roi roi = imp.getRoi();


if (!(roi==null)) {
	Rectangle r;
	r = roi.getBoundingRect();
	double iXROI = r.x;
	double iYROI = r.y;
	double iWidth = r.width;
	double iHeight = r.height;
	ROIstring = " ROI = x" + IJ.d2s(iXROI,0) + ";y" + IJ.d2s(iYROI,0) + ";w"+IJ.d2s(iWidth,0)+";h" + IJ.d2s(iHeight,0) + " ";
	IJ.run("Duplicate...", "title='Channel' duplicate");
	IJ.run("Restore Selection");
	IJ.run("Clear Outside", "stack");
	IJ.run("Crop");
	}

if (roi==null){
	ROIstring = " Whole Image ";
	IJ.run("Duplicate...", "title='Channel' duplicate");}

IJ.run("RGB Split");

int windcnt  = WindowManager.getWindowCount();
ImagePlus impBlue = WindowManager.getImage(windcnt);
ImagePlus impGreen = WindowManager.getImage(windcnt - 1);
ImagePlus impRed = WindowManager.getImage(windcnt - 2);
ImageWindow winBlue = impBlue.getWindow();
ImageWindow winRed = impRed.getWindow();
 ImageWindow winGreen = impGreen.getWindow();

if (!(red)) {
channels = "Green-Blue";
correlate(impGreen, impBlue);
}
if (!(green)) {
channels = "Red-Blue";
correlate(impRed, impBlue);
}
if (!(blue)) {
channels = "Red-Green";
correlate(impRed, impGreen);
}

winRed.close();		
winGreen.close();
winBlue.close();

imp.setRoi(roi);

}
    

    public void correlate(ImagePlus imp1, ImagePlus imp2) {
	ImageProcessor ip1 = imp1.getProcessor();
	ImageProcessor ip2 = imp2.getProcessor();
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
	int N = 0;int N2 = 0;
int Nzero=0;
	 for (int i=1; i<=nslices; i++) {
                	imp1.setSlice(i);
                              	imp2.setSlice(i);
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				z1 = (int)ip1.getPixel(x,y); // z-value of pixel (x,y)in image #1
			                z2 = (int)255-ip2.getPixel(x,y); // z-value of pixel (x,y)in image #2
				z3 = (int)ip2.getPixel(x,y);
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
				if ((z1==0)&(z3==0)){
					Nzero = Nzero+1;
						}
			                count = plot.getPixel(z1, z2);
		                	count++;
			          	plot.putPixel(z1, z2, count);
				if (count<65535) count++;
			                plot16bit.putPixel(z1, z2, count);
 		        	}
		}
	}	


	plot.invertLut();
	plot.resetMinAndMax();
	new ImagePlus("Correlation Plot", plot16bit).show();
	ImagePlus imp3 = WindowManager.getCurrentImage();
	//     if (displayCounts)
	//      displayCounts(plot);
	plot16bit.resetMinAndMax();
	double pearsons1 = sumXY - (sumX*sumY/N);
	double pearsons2 = sumXX - (sumX*sumX/N);
	double pearsons3 = sumYY - (sumY*sumY/N);
	double r = pearsons1/(Math.sqrt(pearsons2*pearsons3));
N2=N-Nzero;
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
	 IJ.write("\n\n" + channels + " colocalisation coefficients for "+ fileName+ 
		"\n" + ROIstring +
		"\n\nPearson's correlation  coeff. (Rr) = " + df.format(r) +
		"\nPearson's correlation  coeff. excluding black pixels (Rp) = " + df.format(r2) +
		"\nOverlap coeff. (R) = " +df.format(overlap)+
		"\nCh1:Ch2 pixel ratio = " + df.format(pixelratio) +
		"\nColocalisation coefficient for red (Mred) = " +df.format(colocM1)+
		"\nColocalisation coefficient for green (Mgreen) = " +df.format(colocM2));
	IJ.run("Enhance Contrast", "saturated=0.5 equalize");
	IJ.run("Ratio ");
	imp3.setTitle(fileName + ROIstring+  "Freq. CP ");
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




	IJ.selectWindow("Results");
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
       	new TextWindow( "Pixel intensities and frequencies" + fileName, "Ch1\tCh2\tFreq.", sb.toString(), 300, 400);

       }
 
}

import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.plugin.PlugIn;
import java.text.DecimalFormat;
import ij.measure.*; 

public class Colocalization_Coefficients implements PlugIn
	 {
	static boolean headingsSet;
	private static int index1;
	private static int index2;
	private ImageStack img1, img2;
	private Roi roi, roi2;
	private int currentSlice ;
	ImageProcessor ip1, ip2, ipmask;
	private int dualChannelIndex = (int)Prefs.get("ICP_channels.int",0);
	private boolean useSlice = Prefs.get("ICP_slice.boolean",true);
	private int indexRoi= (int)Prefs.get("ICP_indexRoi.int",0);
	private static boolean useRoi=true;
	private static boolean showFreqPlot = Prefs.get("ICP_showFreq.boolean",true);
    	private static boolean showColourPlot = Prefs.get("ICP_showColour.boolean",true);
    	private static boolean displayCounts =Prefs.get("ICP_counts.boolean",true);
    	private static boolean threshold =Prefs.get("ICP_useThresh.boolean",true);
    	private static boolean exclude=Prefs.get("ICP_exclude.boolean",true);
    	private ImagePlus imp1, imp2;
            	DecimalFormat df3 = new DecimalFormat("##0.000");
	DecimalFormat df2 = new DecimalFormat("##0.00");
	DecimalFormat df1 = new DecimalFormat("##0.0");
	DecimalFormat df0 = new DecimalFormat("##0");
	String[] dualChannels=  { "Red : Green","Red : Blue", "Green : Blue"};
	String[] chooseROI=  { "None","Channel 1", "Channel 2",};
	private int width, height, rwidth, rheight, xOffset, yOffset;

public void run(String arg) 
	{
	if (showDialog())	
	            correlate(img1, img2);
	}
    	
public boolean showDialog() {
	int[] wList = WindowManager.getIDList();
	if (wList.length<2) 
		{
	              	IJ.showMessage("Image Correlator", "Requires at least two images to be open");
	               	return false;
        		}
	int xLoc=0;
	int yLoc=0;
  	String[] titles = new String[wList.length];
        	for (int i=0; i<wList.length; i++) 
		{
	            	ImagePlus imp = WindowManager.getImage(wList[i]);
	           	 if (imp!=null)
               			titles[i] = imp.getTitle();
            		else
			titles[i] = "";
       		}
	index1 = 0;
	index2 = 1;
            	GenericDialog gd = new GenericDialog("Mander's Calculator");
       	gd.addChoice("Red ", titles, titles[index1]);
	gd.addChoice("Green ", titles, titles[index2]);
	gd.addChoice("Channel Combination", dualChannels, dualChannels[dualChannelIndex]);

	gd.addChoice("Use ROI", chooseROI, chooseROI[indexRoi]);
		gd.addCheckbox("Use Current Slice only", useSlice);
	gd.addCheckbox("Display Frequency Scatterplot ", showFreqPlot);
        	gd.addCheckbox("Display Colour Scatterplot ", showColourPlot);
	gd.addCheckbox("Display Counts ", displayCounts);
        	gd.addCheckbox("Use threshold",threshold);
	gd.addCheckbox("Exclude zero-zero pixels", exclude);
	gd.showDialog();
        	if (gd.wasCanceled())
	            return false;
       	index1 = gd.getNextChoiceIndex();
	index2 = gd.getNextChoiceIndex();
	dualChannelIndex = gd.getNextChoiceIndex();

	indexRoi = gd.getNextChoiceIndex();
		useSlice =gd.getNextBoolean();

	showFreqPlot= gd.getNextBoolean();
	showColourPlot= gd.getNextBoolean();
        	displayCounts = gd.getNextBoolean();	
	threshold = gd.getNextBoolean();
	exclude= gd.getNextBoolean();
	String title1 = titles[index1];
	String title2 = titles[index2];
        	imp1 = WindowManager.getImage(wList[index1]);
	imp2 = WindowManager.getImage(wList[index2]);
	ImageWindow winimp1= imp1.getWindow();
	ImageWindow winimp2= imp2.getWindow();
        	if (imp1.getType()!=imp1.GRAY8&&imp1.getType()!=imp1.GRAY16&&imp2.getType()!=imp2.GRAY16 &&imp2.getType()!=imp1.GRAY8) 
		{
		IJ.showMessage("Image Correlator", "Both images must be 8-bit or 16-bit grayscale.");
            		return false;
        		}
	ip1 = imp1.getProcessor();
	ip2 = imp2.getProcessor();
	Roi roi1 = imp1.getRoi();
	Roi roi2= imp2.getRoi();
	boolean keepROIimage=true;
	int width = imp1.getWidth();
        	int height = imp1.getHeight();

	if (indexRoi== 0) useRoi = false;
	Rectangle rect =ip1.getRoi();
	
	if ((indexRoi==1)) 
		{if(roi1==null) 
			{useRoi=false;}
		else
			{if (roi1.getType()==Roi.RECTANGLE) {
				IJ.showMessage("Does not work with rectangular ROIs");
				return false;}
			ipmask = imp1.getMask();
			if (keepROIimage) new ImagePlus("Mask",ipmask).show();
			rect = ip1.getRoi();
			}
		}
			
	if ((indexRoi==2))
		{if(roi2==null) 
			{useRoi=false;}
		else
			{if (roi2.getType()==Roi.RECTANGLE) {
				IJ.showMessage("Does not work with rectangular ROIs");
				return false;}
			ipmask = imp2.getMask();
			if (keepROIimage) new ImagePlus("Mask",ipmask).show();
			rect = ip2.getRoi();
			}
		}
	if (useRoi==false)
 		{
		xOffset = 0;yOffset = 0; rwidth=width; rheight  =height;
		}
	else {	xOffset = rect.x; yOffset = rect.y; rwidth=rect.width; rheight  =rect.height;

		}
	currentSlice = imp1.getCurrentSlice();
	img1 = imp1.getStack();
        	img2 = imp2.getStack();
	return true;
}
    
public void correlate(ImageStack img1, ImageStack img2) 
	{
	String Ch1fileName = imp1.getTitle();
	String Ch2fileName = imp2.getTitle();
	String fileName = Ch1fileName +  " and " + Ch2fileName;
	ImageProcessor ip1 = imp1.getProcessor();
	ImageProcessor ip2 = imp2.getProcessor();

	ImageStatistics ch1Stats = imp1.getStatistics();
	ImageStatistics ch2Stats = imp2.getStatistics();
	int nslices = imp1.getStackSize();
        	
	double bitDepth = 65536;
	if ((imp1.getType()==imp1.GRAY8)||(imp1.getType()==imp1.COLOR_256) ) bitDepth=255;
	double ch1min=ch1Stats.min;
	double ch1max=ch1Stats.max;
	double ch2min=ch2Stats.min;
	double ch2max=ch2Stats.max;	

	for (int i=1; i<=nslices; i++)
		{ip1 = img1.getProcessor(i);
		ip1.resetMinAndMax();
		if (ch1max<ip1.getMax()) ch1max = ip1.getMax();
		if (ch1min<ip1.getMin()) ch1min = ip1.getMin();
		ip2 = img2.getProcessor(i);
		ip2.resetMinAndMax();
		if (ch2max<ip2.getMax()) ch2max = ip2.getMax();
		if (ch2min>ip2.getMin()) ch2min = ip2.getMin();
		}	
		
	
	int ch1threshmin=0;
	int ch1threshmax=(int)bitDepth;
	int ch2threshmin=0;
	int ch2threshmax=(int)bitDepth;
	


	if((threshold))
		{
		ch2threshmin=(int)ip2.getMinThreshold();
		ch2threshmax=(int)ip2.getMaxThreshold();
		ch1threshmin=(int)ip1.getMinThreshold();
		ch1threshmax=(int)ip1.getMaxThreshold();
		ch1min=(int)ch1threshmin;
		ch1max=(int)ch1threshmax;
		ch2min=(int)ch2threshmin;
		ch2max=(int)ch2threshmax;
		}
	int scaledZ1=0;
	int scaledZ2=0;





	boolean unsigned = true;
	
        	int ch1, ch2, count;
	double sumX = 0;
	double sumXY = 0;
	double sumXX = 0;
	double sumYY = 0;
	double sumY = 0;
	double colocX = 0;
	double colocY = 0;
	double countX = 0;
	double countY = 0;
	int N = 0;
	int N2 = 0;
	int Nzero=0;
	int sumXtotal=0;
	int sumYtotal=0;
	double stackMax=0;
	double currentMax = 0;
	ImageProcessor plot = new FloatProcessor(316, 296); 
	ImageProcessor plot16bit = new ShortProcessor(316, 296);
	int mask;
	boolean include = true;
	int Ninclude=0;
	for (int i=1; i<=nslices; i++) 
		{
		if (IJ.escapePressed()) 
			{IJ.beep();  return;}
		IJ.showStatus("Correlating slice: "+ i +"/" + nslices+ "  Esc to cancel");
		if(useSlice) 
			{
			i = currentSlice;
			nslices = currentSlice;
			}
	             	ip1 = img1.getProcessor(i);
          		ip2 = img2.getProcessor(i);
	        	for (int y=0; y<rheight; y++)
			{mask = 1;
            			for (int x=0; x<rwidth; x++)
				{if(useRoi)	mask = (int)ipmask.getPixelValue(x,y);
				if (mask!=0)	{
           			    		ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset); 
					ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset);  
					sumXtotal = sumXtotal+ch1;
					sumYtotal = sumYtotal+ch2;
					N++;
					if(ch2>0) {colocX = colocX + ch1;
						countY++;}	
					if(ch1>0) {
						colocY = colocY + ch2;
						countX++;}
	
					if (threshold)
						{
						if ((ch1>ch1threshmin)&&(ch2>ch2threshmin)) include = true;
						else include = false;	
						Ninclude++;
						}				
					if (ch1+ch2!=0) N2++;	
					if(include)
						{
						sumXtotal = sumXtotal+ch1;
						sumYtotal = sumYtotal+ch2;
						sumX = sumX+ch1;
						sumXY = sumXY + (ch1 * ch2);
						sumXX = sumXX + (ch1 * ch1);
						sumYY = sumYY + (ch2 *ch2);
						sumY = sumY + ch2;
		
						scaledZ1= (int)((double)255*(((double)ch1-(double)ch1min)/((double)ch1max-(double)ch1min)));
						scaledZ2= 275-(int)((double)255*(((double)ch2-(double)ch2min)/((double)ch2max-(double)ch2min)));
						count = plot.getPixel(scaledZ1+45, scaledZ2);
						count++;
				                	plot.putPixel(scaledZ1+45, scaledZ2, count);
						if (count<65535) count++;
						plot16bit.putPixel(scaledZ1+45, scaledZ2, count);
						}
					}
	            			}
			}
		}
if (exclude) N=N2;
if (threshold) N=Ninclude;

	double pearsons1 = sumXY - (sumX*sumY/N);
	double pearsons2 = sumXX - (sumX*sumX/N);
	double pearsons3 = sumYY - (sumY*sumY/N);
IJ.showMessage("p1: "+pearsons1+"    p2: "+pearsons2+"     p3: "+pearsons3);

	double r = pearsons1/(Math.sqrt(pearsons2*pearsons3));
	
	double overlap = sumXY / (Math.sqrt(sumXX*sumYY));
	double k1 = sumXY/sumXX;
	double k2 = sumXY/sumYY;
	double colocM1 = colocX/sumXtotal;
	double colocM2 = colocY/sumYtotal;
	double pixelratio = countX/countY;


//finished pixel counting

	plot16bit.setColor(0xffffff);
	plot16bit.invertLut();
        	plot16bit.resetMinAndMax();
	int plotaxis = (int)plot16bit.getMax();
	String ch1String = "Ch1 ";
	String ch2String = " Ch2 ";
	Font font;
                font = new Font("SansSerif", Font.PLAIN, 12);
	int labelx=169;
	int labelx2=30;
	int labelx3=290;
	int labely=295;
	String labelRedMax=df0.format(ch1max);
	String labelGreenMax=df0.format(ch2max);
	String ch2MinLabel = df0.format(ch2min);
	String ch1MinLabel = df0.format(ch1min);
	int ch1AxisOffset = ip1.getStringWidth(labelRedMax);
	int ch2AxisOffset = ip1.getStringWidth(labelGreenMax);
	int ch1AxisOffset2 = ip1.getStringWidth(ch1MinLabel );
	int ch2AxisOffset2 = ip1.getStringWidth(ch2MinLabel );
	int w=10;
	plot16bit.drawString(ch1String ,165,295);
	plot16bit.drawString(ch2String ,10,150);
	plot16bit.drawString(ch1MinLabel ,39-ch1AxisOffset2+w,295);
	plot16bit.drawString(ch2MinLabel ,labelx2-ch2AxisOffset2+w,labely-12);
	plot16bit.drawString(labelRedMax,300-ch1AxisOffset+w,295);
	plot16bit.drawString(labelGreenMax,labelx2-ch2AxisOffset+w,labely-268);
	for (int c=20; c<280; c++)
		{
		plot16bit.putPixel(45, c,plotaxis );
		plot16bit.putPixel(300, c,plotaxis );
		}

	for (int c=42; c<300; c++) 
		{
		plot16bit.putPixel(c,20,plotaxis );
		plot16bit.putPixel(c,275,plotaxis );
		}
	
	if (showFreqPlot) 
		{
		new ImagePlus("Correlation Plot", plot16bit).show();
		ImagePlus imp3 = WindowManager.getCurrentImage();
		imp3.setTitle(fileName + " Freq. CP");
		IJ.run("Enhance Contrast", "saturated=90 normalize equalize normalize_all use");
		IJ.run("Fire");
		}

       	if (displayCounts)       displayCounts(plot);
	plot16bit.resetMinAndMax();
	
	
	DecimalFormat df = new DecimalFormat("##0.000");
	String useRoistring = "";
	StringBuffer ResultsOutput = new StringBuffer();
	String Headings = "Image\tRr\tR\tch1:ch2\tM1\tM2\tCh1 Thresh\tCh2 Thresh\n";
	if ((!headingsSet))
		{ 
		IJ.setColumnHeadings(Headings);
		headingsSet = true;
	      	IJ.write(Headings);
  		}	
	IJ.write(fileName  +"\t"+df3.format(r) 
		+"\t" +df3.format(overlap)+
		"\t" + df3.format(pixelratio) +
		"\t" +df3.format(colocM1)+
		"\t" +df3.format(colocM2)+
		"\t" + df0.format(ch1threshmin)+"; "+df0.format(ch1threshmax)+
		"\t" + df0.format(ch2threshmin)+"; "+df0.format(ch2threshmax)
		);
	IJ.selectWindow("Results");


	if (showColourPlot) 
		{ int colIndex1 = 0;
		int colIndex2 = 1;
		if (dualChannelIndex==1) colIndex2 = 2;
		if (dualChannelIndex==2) {colIndex1 = 1;colIndex2 =2 ;}
		int [] color  = new int [3]; 
		ColorProcessor ipCol = new ColorProcessor(316, 296);
		ipCol.setColor(0xffffff);
		for (int y=0; y<256; y++)
			{
			for (int x=0; x<256; x++) 
			{
			ch1 = (int)plot16bit.getPixel(x+45,275-y); 
			if(ch1>0)
				{color[colIndex1]=x;
				color[colIndex2]=y;
				ipCol.putPixel(x+45,275-y,color);
				}
			}
		}
	for (int c=0; c<=2;c++)
		{
		color [c]= 255;
		}

	ipCol.drawString(ch1String ,165,295);
	ipCol.drawString(ch2String ,10,150);
	ipCol.drawString(ch1MinLabel ,39-ch1AxisOffset2+w,295);
	ipCol.drawString(ch2MinLabel ,labelx2-ch2AxisOffset2+w,labely-12);
	ipCol.drawString(labelRedMax,300-ch1AxisOffset+w,295);
	ipCol.drawString(labelGreenMax,labelx2-ch2AxisOffset+w,labely-268);
	for (int c=20; c<280; c++)
		{
		ipCol.putPixel(45, c,color );
		ipCol.putPixel(300, c,color );
		}

	for (int c=42; c<300; c++) 
		{
		ipCol.putPixel(c,20,color );
		ipCol.putPixel(c,275,color );
		}
	new ImagePlus("Correlation Plot", ipCol).show();
	ImagePlus imp4 = WindowManager.getCurrentImage();
	ImageWindow winimp4= imp4.getWindow();
	IJ.run("Enhance Contrast", "saturated=90 normalize equalize normalize_all use");
	imp4.setTitle(fileName + " Freq. CP");
	}




//reset prefs
	
	Prefs.set("ICP_slice.boolean", useSlice);
	Prefs.set("ICP_useRoi.boolean", useRoi);
	Prefs.set("ICP_indexRoi.int",0);
	Prefs.set("ICP_showFreq.boolean", showFreqPlot);
	Prefs.set("ICP_showColour.boolean", showColourPlot);
	Prefs.set("ICP_counts.boolean", displayCounts);
	Prefs.set("ICP_useThresh.boolean", threshold);
	Prefs.set("ICP_exclude.boolean", exclude);
	Prefs.set("ICP_channels.int", (int)dualChannelIndex );
	imp1.changes = false;
	imp2.changes = false;
	
	imp1.setSlice(currentSlice);
	imp2.setSlice(currentSlice);

}

    void displayCounts(ImageProcessor plot) {
	StringBuffer sb = new StringBuffer();
	int count;
	for (int x=0; x<256; x++)
		for (int y=255; y>=0; y--) 
			{
		                count = plot.getPixel(x,y);
		                if (count>0)
		                sb.append(x+"\t"+(255-y)+"\t"+count+"\n");
            			}
       new TextWindow( "Pixel intensities and frequencies", "Ch1\tCh2\tFreq.", sb.toString(), 300, 400);
       }
 
}

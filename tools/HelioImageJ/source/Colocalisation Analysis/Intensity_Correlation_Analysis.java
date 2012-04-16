//Version 2.0
//Sept.5.05
//T.J. Collins tonyc@uhnresearch.ca
//E.Stanley EStanley@uhnresearch.ca

import java.awt.*;
import java.io.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.text.*;
import ij.plugin.PlugIn;
import java.text.DecimalFormat; 
import ij.measure.*;
import ij.measure.ResultsTable.*;
import ij.plugin.filter.*;


public class Intensity_Correlation_Analysis implements PlugIn 
	{
	static boolean headingsSet;
	private ImageStack img1, img2;
	private static int index1;
	private static int index2;
	private Roi roi, roi1, roi2;
	private ImagePlus imp1, imp2, impmask;
	private ImageProcessor ip1, ip2, ipmask;
	private static boolean displayCounts = Prefs.get("ICA_counts.boolean",false);
	private boolean displayRGHist = Prefs.get("ICA_RGB.boolean",false);
	private boolean displayFreqHist = Prefs.get("ICA_freq.boolean",true);
	private boolean displayICQplots = Prefs.get("ICA_ICQplots.boolean",true);
	private boolean displayPositives = Prefs.get("ICA_positives.boolean",true);
	private boolean displayPDMs =Prefs.get("ICA_displayPDMs.boolean",false);
	private boolean keepROIimage= Prefs.get("ICA_keepROI.boolean",true);
	private boolean useROI ;
	private int bitDepth = 255;
	private int dualChannelIndex = (int)Prefs.get("ICA_channels.int",0);
	private boolean currentSliceOnly= Prefs.get("ICA_current.boolean",true);
	private boolean overlayPositives=Prefs.get("ICA_overlayPositives.boolean",false);
	private boolean useThreshold=Prefs.get("ICA_threshold.boolean",false);
	private int indexRoi= (int)Prefs.get("ICA_indexRoi.int",0);
	private int ch2threshmin=1;
	private int ch1threshmin=1;	
	private int ch2threshmax=255;
	private int ch1threshmax=255;
	
	private int xLoc=0;
	private int yLoc=0;
	private int dot=3;
	private int currentSlice ;
	private int nslices;
	private int width, height, rwidth, rheight, xOffset, yOffset;
	private 	int colIndex1 = 0;
	private int colIndex2 = 1;
	private int [] color  = new int [3]; 
 
	private ImageWindow winimpROI2,winimpROI1;
	public void run(String arg) {
      	 if (showDialog())
          		correlate(img1, img2);
    	}
    
public boolean showDialog() 
{
	int[] wList = WindowManager.getIDList();
	if (wList==null)
	{
		IJ.noImage();
		return false;
	}
	String[] titles = new String[wList.length];
	for (int i=0; i<wList.length; i++) 
		{
		ImagePlus imp = WindowManager.getImage(wList[i]);
		if (imp!=null)
			titles[i] = imp.getTitle();
		else
			titles[i] = "";
	       	}
	if (wList.length<2) 
		{
                	IJ.showMessage("Image Correlator", "Requires at least two images to be open");
	                return false;
        		}
	String[] dualChannels=  { "Red : Green","Red : Blue", "Green : Blue",};
	String[] chooseROI=  { "None","Channel 1", "Channel 2",};

	if (index1>=titles.length)index1 = 0;
	if (index2>=titles.length)index2 = 0;
	
	GenericDialog gd = new GenericDialog("Image Correlation Analysis");
	gd.addChoice("Channel 1 ", titles, titles[0]);
	gd.addChoice("Channel 2 ", titles, titles[1]);
	gd.addChoice("Channel Combination", dualChannels, dualChannels[dualChannelIndex]);
	gd.addChoice("Use ROI", chooseROI, chooseROI[indexRoi]);
	gd.addCheckbox("Use Thresholds", useThreshold);

	gd.addCheckbox("Keep merged ROI", keepROIimage);
	gd.addCheckbox("Display Colour Scatter plot", displayRGHist);
	gd.addCheckbox("Display Frequency Scatter plot", displayFreqHist);
	gd.addCheckbox("Display Intensity Counts ", displayCounts);
	gd.addCheckbox("Current Slice only:", currentSliceOnly);
	gd.addMessage("Intensity Correlation Analysis");
	gd.addCheckbox("Display ICA plots", displayICQplots);
	gd.addNumericField("Crosshair size (pixels)", dot,0);
	gd.addCheckbox("Display PDM Image", displayPositives);
	gd.addCheckbox("Display +ve PDMs only", overlayPositives);
	gd.addCheckbox("List PDM values", displayPDMs);
	gd.addMessage("For details of Intensity Correlation Analysis (ICA) "+"\n"+"see and cite: Li, Lau, Morris, Guo & Stanley. "+"\n"+"J Neurosci. (2004) 24:4070-81. ");
	gd.showDialog();
        	
	if (gd.wasCanceled())
		return false;
	index1 = gd.getNextChoiceIndex();
	index2 = gd.getNextChoiceIndex();
	dualChannelIndex = gd.getNextChoiceIndex();
	indexRoi = gd.getNextChoiceIndex();
	useThreshold = gd.getNextBoolean();
	keepROIimage =gd.getNextBoolean();
	displayRGHist = gd.getNextBoolean();
	displayFreqHist = gd.getNextBoolean();
	displayCounts = gd.getNextBoolean();
  	currentSliceOnly = gd.getNextBoolean();
	displayICQplots =gd.getNextBoolean();
	dot = (int)gd.getNextNumber();
	displayPositives =gd.getNextBoolean();
	overlayPositives =gd.getNextBoolean();
	displayPDMs =gd.getNextBoolean();
	String title1 = titles[index1];
	String title2 = titles[index2];
	imp1 = WindowManager.getImage(wList[index1]);
	imp2 = WindowManager.getImage(wList[index2]);
	ImageStack img1 = imp1.getStack();
	ImageStack img2 = imp2.getStack();
	width = img1.getWidth();
	height = img1.getHeight();

	if (imp1.getType()==imp1.COLOR_RGB || imp2.getType()==imp2.COLOR_RGB) 
		{
            		IJ.showMessage("Image Correlator", "Both images must be 8- or 16-bit grayscale.");
            		return false;
            		}
	currentSlice = imp1.getCurrentSlice();
	ImageWindow winimp1= imp1.getWindow();
	ImageWindow winimp2= imp2.getWindow();
	ImageProcessor ipthresh1 = imp1.getProcessor();
	ImageProcessor ipthresh2 = imp2.getProcessor();
	

	
	if (dualChannelIndex==1) colIndex2 = 2;
	if (dualChannelIndex==2) {colIndex1 = 1;colIndex2 =2 ;}
	

	if (imp1.getType()!=imp1.GRAY8)
		{bitDepth= 65208;
		ch2threshmax=65208;
		ch1threshmax=65208;
		}	
	if((useThreshold))
		{
		if (ipthresh2.getMinThreshold()!=ipthresh2.NO_THRESHOLD)
			{
			ch2threshmin=(int)ipthresh2.getMinThreshold();
			ch2threshmax=(int)ipthresh2.getMaxThreshold();
			}
		if (ipthresh1.getMinThreshold()!=ipthresh1.NO_THRESHOLD)
			{
			ch1threshmin=(int)ipthresh1.getMinThreshold();
			ch1threshmax=(int)ipthresh1.getMaxThreshold();
			}
		}


	//IJ.showMessage("ch1 "+ch1threshmin+"  "+ch1threshmax+";   ch2 " +ch2threshmin+"  "+ ch2threshmax);
	nslices = imp1.getStackSize();
	
	if (indexRoi==0)	useROI = false;
	else useROI=true;

	ip1 = imp1.getProcessor();
	ip2 = imp2.getProcessor();
	roi1 = imp1.getRoi();
	roi2 = imp2.getRoi();

	Rectangle rect =ip1.getRoi();
	
	if ((indexRoi==1)) 
		{if(roi1==null) 
			{useROI=false;}
		else
			{
			ipmask = imp1.getMask();
			rect = ip1.getRoi();
			}
		}
			
	if ((indexRoi==2))
		{if(roi2==null) 
			{useROI=false;}
		else
			{ipmask = imp2.getMask();
			rect = ip2.getRoi();
			}
		}
	
	

	if (useROI==false)
 		{
		xOffset = 0;yOffset = 0; rwidth=width; rheight  =height;
		}
	else {	xOffset = rect.x; yOffset = rect.y; rwidth=rect.width; rheight  =rect.height;

		}
//.showMessage(""+useROI);
	int g1=0;int g2=0;
	
		
     	return true; 
}
    
public void correlate(ImageStack img1, ImageStack img2)
	{
	boolean resultsOpen = IJ.isResultsWindow();
	if (!resultsOpen) headingsSet = false;
	String Ch1fileName = imp1.getTitle();
	String Ch2fileName = imp2.getTitle();
	String fileName = Ch1fileName +  " and " + Ch2fileName;
	img1 = imp1.getStack();
	img2 = imp2.getStack();
	int width2 = img2.getWidth();
	int height2 = img2.getHeight();
	if ((height!=height2)||(width!=width2))
		{
		IJ.showMessage("Image Correlator", "Images selected are not the same size.");
		 return ;
		}
	String path = Menus.getPlugInsPath();
	ImageProcessor ip1 = imp1.getProcessor();
	ImageProcessor ip2 = imp2.getProcessor();
	int cdash=0;
	ImageProcessor plot = new FloatProcessor(316, 296); 
	ImageProcessor plot16bit = new ShortProcessor(316, 296);
	ImageProcessor plotICQch1 = new ShortProcessor(316, 296);
	ImageProcessor plotICQch2 = new ShortProcessor(316, 296);
	ImageProcessor plotICQch12 = new ShortProcessor(316, 296);
	ImageProcessor plotICQch22 = new ShortProcessor(316, 296);
	ImageProcessor ipMerge= new ColorProcessor(rwidth, rheight);
	ImageStack mergeStack = new ImageStack (rwidth,rheight);

	ImageStack plotStackPDM = new ImageStack (rwidth+35,rheight);
	ImageStack plotStackPDMpositives= new ImageStack (rwidth,rheight+15);
	ImageStack plotStackPDMnegatives= new ImageStack (rwidth,rheight+15);
	
	StringBuffer sb2 = new StringBuffer();
	StringBuffer sb3 = new StringBuffer();
	int ch1, z2,ch2;
	int sumCh1 = 0;
	int sumCh2 = 0;
	double sumX = 0;
	double sumXY = 0;
	double sumXX = 0;
	double sumYY = 0;
	double sumY = 0;
	double sumXtotal = 0;
	double sumYtotal = 0;
	double colocX = 0;
	double colocY = 0;
	double countX = 0;
	double countY = 0;
	int N = 0;
	int countnegative = 0;
	int  countpositive = 0;
	int signcountch1=0;
int signcountch2=0;
	int countZeroPair=0;
	int countNonZeroPair=0;
	int ch1max = 0;
	int ch2max = 0;
	int ch1min = 65208;
	int ch2min  = 65208;
	double ch1Norm = 1;
	double ch2Norm = 1;
	double ch1normdble =0;
	double ch2normdble =0;
	DecimalFormat df4 = new DecimalFormat("##0.0000");
	DecimalFormat df3 = new DecimalFormat("##0.000");
	DecimalFormat df2 = new DecimalFormat("##0.00");
	DecimalFormat df1 = new DecimalFormat("##0.0");
	DecimalFormat df0 = new DecimalFormat("##0");
	double ch2NormMax = 0;
	double ch1NormMax=0;
	double ch2NormMin = 0;
	double ch1NormMin=0;
	int PDM2plot=0;
	double PDMmax = 0;
	double PDMmin = 0;
	double PDM=0;
	double PDMrange=0;
	double PDMnorm=0;
	int count=0;
	int countTotal=0;
	int ch22Plot=128;
	int ch12Plot=128;
	int numberCh1Pixels = 0;
	int numberCh2Pixels=0;
	int numberBothNonZero=0;
	int mask=1;
int countP=0;
	int scaledZ1=0;
	int scaledZ2=0;
//run1
int countAll=0;
int countZeroZero=0;

// getting mean ch1
//getting mean ch2
//

//IJ.showMessage("ch1 "+ch1threshmin+"  "+ch1threshmax+";   ch2 " +ch2threshmin+"  "+ ch2threshmax);

boolean include = false;
	 for (int i=1; i<=nslices; i++) 
		{
		if(currentSliceOnly) 
			{
			i = currentSlice;
			nslices = currentSlice;
			}
		IJ.showStatus("Correlating (1/3 steps): "+ i +"/" + nslices);
		ip1 = img1.getProcessor(i);
          		ip2 = img2.getProcessor(i);
		for (int y=0; y<rheight; y++) 
			{
			for (int x=0; x<rwidth; x++) 
				{mask=1;
				if((useROI)&&(ipmask!=null))	mask = (int)ipmask.getPixelValue(x,y);
				if (mask!=0)
					{
					countTotal++;
		     			ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset); 
					ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset);  
					if (ch1+ch2==0) countZeroZero++;
					countAll++;
					if ((ch1>=ch1threshmin)&&(ch2>=ch2threshmin)) include = true;
					else include = false;	

					if(!useThreshold) include = true;
					
					if((ch1>=ch1threshmin))
						{sumCh1 = sumCh1 + ch1;
						if(ch1>=ch1max) ch1max = ch1;
						if(ch1<=ch1min) ch1min = ch1;
						numberCh1Pixels++;
						}
					if((ch2>=ch2threshmin))
						{if(ch2>=ch2max) ch2max = ch2;
						if(ch2<=ch2min) ch2min = ch2;
						numberCh2Pixels++;
						sumCh2 = sumCh2 + ch2;
						}
				
					if(include)
						{
										
						sumX = sumX+ch1;
						sumXY = sumXY + (ch1 * ch2);
						sumXX = sumXX + (ch1 * ch1);
						sumYY = sumYY + (ch2 *ch2);
						sumY = sumY + ch2;
						countP++;
						}
					}
				}
			}
		}
	countNonZeroPair = countAll-countZeroZero;

	double meanCh1= (double)sumCh1/numberCh1Pixels;
	double meanCh2 = (double)sumCh2/numberCh2Pixels;
	ch1min-=1;
	ch1max+=1;
	ch2min-=1;
	ch2max+=1;

	//IJ.showMessage("ch1max: "+ch1max+"  ch1min  "+ch1min);

//	IJ.showMessage("Ch1: "+meanCh1+"  Ch2:  "+meanCh2+"   Countnon00:"+ countNonZeroPair);
	double normMeanCh1 = ((double)meanCh1-(double)ch1min)/((double)ch1max-(double)ch1min);
	double normMeanCh2=((double)meanCh2-(double)ch2min)/((double)ch2max-(double)ch2min);
	int ch2Mean2Plot = (int)((((double)meanCh2-(double)ch2min)/((double)ch2max-(double)ch2min))*(double)256);
	int ch1Mean2Plot = (int)((((double)meanCh1-(double)ch1min)/((double)ch1max-(double)ch1min))*(double)256);
//IJ.showMessage("mean ch1= "+meanCh1+"ch1 min= "+ch1min+"  ch1max= "+ch1max );

if(!useThreshold) countP=countAll-countZeroZero;


double pearsons1 = sumXY - (sumX*sumY/countP);
double pearsons2 = sumXX - (sumX*sumX/countP);
double pearsons3 = sumYY - (sumY*sumY/countP);


//run2 to get PDM extreme

	for (int i=1; i<=nslices; i++) 
		{
		 IJ.showStatus("Correlating (2/3 steps): "+ i +"/" + nslices);
		if(currentSliceOnly) 
			{
			i = currentSlice;
			nslices = currentSlice;
			}
 		ip1 = img1.getProcessor(i);
          		ip2 = img2.getProcessor(i);
		for (int y=0; y<rheight; y++) 
			{
		                for (int x=0; x<rwidth; x++) 
				{mask=1;
				if((useROI)&&(ipmask!=null))	mask = ipmask.getPixel(x,y);
				if (mask!=0)
					{
					ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset); 
					ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset);  
					if ((ch1>=ch1threshmin)&&(ch2>=ch1threshmin)) include = true;
					else include = false;	
					if(!useThreshold) include = true;
					
					if(include)
						{
						ch1Norm = (((double)ch1-(double)ch1min)/((double)ch1max-(double)ch1min));
						ch2Norm = (((double)ch2-(double)ch2min)/((double)ch2max-(double)ch2min));
						PDM =    ((double)ch1Norm-(double)normMeanCh1)*((double)ch2Norm-(double)normMeanCh2);
						if (PDM>=PDMmax) PDMmax=PDM;
						if (PDM<=PDMmin) PDMmin=PDM;
						}
  					}
				}
			}
		}
	
	if (PDMmax+PDMmin>0)  PDMrange = 1.05*(PDMmax);
	if (PDMmax+PDMmin<0)  PDMrange = -1.05*(PDMmin);
//IJ.showMessage(""+PDMrange);
	ImageProcessor ipPDMpositives = new FloatProcessor(width,height+15);	
	ImageProcessor ipPDMnegatives = new FloatProcessor(width,height+15);
//run 3 to clculate final coefficients and plots
countP=0;
sumX = 0;
sumXY = 0;
sumXX = 0;
sumYY = 0;
sumY = 0;
PDM=0;
int countInclude=0;
countZeroPair=0;
countpositive=0;
	for (int i=1; i<=nslices; i++)
		{
		ImageProcessor ipPDM = new FloatProcessor(rwidth+35,rheight);
		ipPDMpositives = new FloatProcessor(rwidth,rheight+15);	
		ipPDMnegatives = new FloatProcessor(rwidth,rheight+15);
		if(currentSliceOnly) 
			{
			i = currentSlice;
			nslices = currentSlice;
			}
		IJ.showStatus("Correlating (3/3 steps): "+ i +"/" + nslices);
                	ip1 = img1.getProcessor(i);
	          	ip2 = img2.getProcessor(i);
		for (int y=0; y<=rheight; y++) 
			{
			for (int x=0; x<=rwidth; x++) 
				{mask=1;
				if((useROI)&&(ipmask!=null))	mask = ipmask.getPixel(x,y);
				if (mask!=0)
					{
				               	ch1 = (int)ip1.getPixel(x+xOffset,y+yOffset); 
					ch2 = (int)ip2.getPixel(x+xOffset,y+yOffset);  
					if (ch1+ch2==0) countZeroPair++;
					//crreate merged image		
					color[colIndex1]=(int)((ch1*bitDepth)/bitDepth);
					color[colIndex2]=(int)((ch2*bitDepth)/bitDepth);
					ipMerge.putPixel(x,y,color);
					sumXtotal = sumXtotal+ch1;
					sumYtotal = sumYtotal+ch2;


						if((ch2>=ch2threshmin)&(ch2<=ch2threshmax))
							{
							colocX = colocX + ch1;
							countY = countY + 1;
							}	
						if((ch1threshmin<=ch1)&(ch1<=ch1threshmax))
							{
							colocY = colocY + ch2;
							countX = countX + 1;
							}
				

					if ((ch1threshmin<=ch1)&&(ch1<=ch1threshmax)) numberCh1Pixels++;
					if ((ch2threshmin<=ch2)&&(ch2<=ch2threshmax)) numberCh2Pixels++;

					if(((ch1>=ch1threshmin)&&(ch2>=ch2threshmin))||!useThreshold)
						{
						countInclude++;
						ipPDM.putPixel(x+35,y,0);countP++;
						sumX = sumX+ch1;
						sumXY = sumXY + (ch1 * ch2);
						sumXX = sumXX + (ch1 * ch1);
						sumYY = sumYY + (ch2 * ch2);
						sumY = sumY + ch2;


						ch1Norm = (((double)ch1-ch1min)/(ch1max-ch1min));
						ch2Norm = (((double)ch2-ch2min)/(ch2max-ch2min));	
						PDMnorm =((double)ch1Norm-(double)normMeanCh1)*((double)ch2Norm-(double)normMeanCh2);
					
						PDM = ((double)ch1-(double)meanCh1)*((double)ch2-(double)meanCh2);
						if ((PDM >0)&&(ch1+ch2!=0)) countpositive++;	
	
						//PDM2plot = 127+(int)((((double)PDMnorm/(double)PDMrange))*127);
						PDM2plot = (int)(128+(128*(PDMnorm/PDMrange)));
							
						ch12Plot = (int)((((double)ch1-((double)ch1min))/(((double)ch1max)-((double)ch1min)))*(double)256);
						ch22Plot = (int)((((double)ch2-((double)ch2min))/(((double)ch2max)-((double)ch2min)))*(double)256);
						
						sb2.append(x+"\t"+y+"\t"+df4.format(PDMnorm )+"\t"+ch1+"\t"+ch2+"\n");
						ipPDM.putPixelValue(x+35,y,PDMnorm);

						if(PDM>0)
							{//ipPDMpositives.putPixelValue(x,y,PDMnorm);	
							if ((ch1>meanCh1)&&(ch2>meanCh2)) ipPDMpositives.putPixelValue(x,y,PDMnorm);
							if ((ch1<meanCh1)&&(ch2<meanCh2)) ipPDMnegatives.putPixelValue(x,y,PDMnorm);
							}

						signcountch1 = plotICQch12.getPixel(PDM2plot+45, 275-ch12Plot );
						signcountch1++;
						signcountch2 = plotICQch22.getPixel(PDM2plot +45,275-ch22Plot );
						signcountch2++;
						plotICQch1.putPixel(PDM2plot+45,275-ch12Plot ,signcountch1);
						plotICQch2.putPixel(PDM2plot+45,275-ch22Plot ,signcountch2);

						int dotWidth = (dot-1)/2;
						//graph plots
						plotICQch12.putPixel(PDM2plot+45,275-ch12Plot ,signcountch1);
						plotICQch22.putPixel(PDM2plot+45,275-ch22Plot ,signcountch2);
						for (int q=0;q<dot;q++)
			
							{
							plotICQch1.putPixel(PDM2plot+45-dotWidth+q,275-ch12Plot ,signcountch1);
							

							plotICQch2.putPixel(PDM2plot+45-dotWidth+q,275-ch22Plot ,signcountch2);

							plotICQch1.putPixel(PDM2plot+45,275-dotWidth+q-ch12Plot ,signcountch1);
							plotICQch2.putPixel(PDM2plot+45,275-dotWidth+q-ch22Plot ,signcountch2);
							}
	
						scaledZ1= (int)((double)256*(((double)ch1-(double)ch1min)/((double)ch1max-(double)ch1min)));
						scaledZ2= 275-(int)((double)256*(((double)ch2-(double)ch2min)/((double)ch2max-(double)ch2min)));
						
						count = plot.getPixel(scaledZ1+45, scaledZ2);
						count++;
				                	plot.putPixel(scaledZ1+45, scaledZ2, count);	
						if (count<65535) count++;
						plot16bit.putPixel(scaledZ1+45, scaledZ2, count);
						}
					}
				}
			}
		//add ramp
	
		double scale = (rheight/2)/ (2*PDMrange);
		for (int y=1; y<(double)(rheight/2); y++) 
			{for (int x=1; x<25; x++)
				 {
				ipPDM.putPixelValue(5+x, (int)(rheight/4)+y, (double)(PDMrange-(y/scale)));
    				}
			}
		//add text

		int fontSize = 12;
		int fontDisplacement  =(int)((12*0.934211)+1.10526+4);
		ipPDM.drawString("+"+df2.format(PDMrange),1,(int)(rheight/4));
		ipPDM.drawString(df2.format(-PDMrange),1, (int)(0.75*rheight)+fontDisplacement);	

		ipPDMpositives.setColor((int)PDMrange);
		ipPDMnegatives.setColor((int)PDMrange);
		ipPDM.setMinAndMax(-PDMrange, PDMrange);   
		ipPDMpositives.setMinAndMax(0, PDMrange/2);
		ipPDMnegatives.setMinAndMax(0, PDMrange/2);
		ipPDMpositives.drawString("+ve × +ve",10,(int)rheight-fontDisplacement+30);
		ipPDMnegatives.drawString("-ve × -ve",10,(int)rheight-fontDisplacement+30);
		plotStackPDM.addSlice("PDM Stack", ipPDM);
		plotStackPDMpositives.addSlice("+vePDMs (+ × +)", ipPDMpositives);  
 		plotStackPDMnegatives.addSlice("+vePDM (- × -)", ipPDMnegatives);   	
		mergeStack.addSlice("Merged ROI", ipMerge);
		}


//collate positives
	for (int i=1; i<=nslices;i++)
		{if(currentSliceOnly) 
			{
			nslices = 1;
			}
		ip1 = plotStackPDMnegatives.getProcessor(i);
		ip1.setMinAndMax(0, PDMrange);
		plotStackPDMpositives.addSlice("+vePDM (- × -)", ip1);  	
		}



double r = pearsons1/(Math.sqrt(pearsons2*pearsons3));
	plot.invertLut();
	plot.resetMinAndMax();
	plot16bit.resetMinAndMax();	
	String ch1String = " ch1";
	String ch2String = " ch2";
	if (dualChannelIndex!=0) ch2String = " blue";
	if (dualChannelIndex==2) ch1String = " ch2";
	Font font;
                font = new Font("SansSerif", Font.PLAIN, 12);
	int labelx=169;
	int labelx2=30;
	int labelx3=290;
	int labely=295;
	String labelCh1Max=df0.format(ch1max);
	String labelCh2Max=df0.format(ch2max);
	String ch1MeanLabel = df1.format(meanCh1);
	String ch2MeanLabel = df1.format(meanCh2);
	String ch2MinLabel = df0.format(ch2min);
	String ch1MinLabel = df0.format(ch1min);
	int ch1AxisOffset = ip1.getStringWidth(labelCh1Max);
	int ch2AxisOffset = ip1.getStringWidth(labelCh2Max);
	int ch1AxisOffset2 = ip1.getStringWidth(ch1MinLabel );
	int ch2AxisOffset2 = ip1.getStringWidth(ch2MinLabel );
	int ch1AxisOffset3 = ip1.getStringWidth(ch1MeanLabel );
	int ch2AxisOffset3 = ip1.getStringWidth(ch2MeanLabel );
	int w=10;
	String s="0";
	String s1= "-"+df2.format(PDMrange);
	String s2 = df2.format(PDMrange);
	String s3= "-"+df2.format(PDMrange);
	String s4 = df2.format(PDMrange);

	if  (displayFreqHist)
		{
		int plotaxis = (int) plot16bit.getMax();
		plot16bit.setColor(plotaxis);
		plot16bit.drawString(ch1String ,150,295);
		plot16bit.drawString(ch2String ,1,150);
		plot16bit.drawString(ch1MinLabel ,39-ch1AxisOffset2+w,295);
		plot16bit.drawString(ch2MinLabel ,labelx2-ch2AxisOffset2+w,labely-12);
		plot16bit.drawString(labelCh1Max,300-ch1AxisOffset+w,295);
		plot16bit.drawString(labelCh2Max,labelx2-ch2AxisOffset+w,labely-268);
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
		new ImagePlus("Correlation Plot", plot16bit).show();
		ImagePlus imp3 = WindowManager.getCurrentImage();
		ImageWindow winimp3 = imp3.getWindow();
		imp3.setTitle(fileName + " Freq. CP");
		WindowManager.setCurrentWindow(winimp3);
		IJ.run("Enhance Contrast", "saturated=0.2 normalize");
		IJ.run("Ratio ");
		}
if  (displayRGHist)
	{
	ColorProcessor ipCol = new ColorProcessor(316, 296);
	ipCol.setColor(0xffffff);
	for (int y=0; y<height; y++)
		{
		for (int x=0; x<width; x++) 
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
	ipCol.drawString(labelCh1Max,300-ch1AxisOffset+w,295);
	ipCol.drawString(labelCh2Max,labelx2-ch2AxisOffset+w,labely-268);
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
	IJ.run("Enhance Contrast", "saturated=0.1 normalize");
	imp4.setTitle(fileName + " Freq. CP");
	}

if  (displayICQplots){


	plotICQch1.resetMinAndMax();
	plotICQch2.resetMinAndMax();
	int ch1axis = (int) plotICQch1.getMax();
	int ch2axis = (int) plotICQch2.getMax();
	for (int c=20; c<280; c++)
		{
		plotICQch1.putPixel(45, c,ch1axis);
		plotICQch2.putPixel(45,c,ch2axis );
		plotICQch1.putPixel(173, c,ch1axis);
		plotICQch2.putPixel(173,c,ch2axis );
		plotICQch1.putPixel(300, c,ch1axis);
		plotICQch2.putPixel(300,c,ch2axis );
		}
	for (int c=42; c<300; c++) 
		{
		plotICQch1.putPixel(c,20,ch1axis);
		plotICQch2.putPixel(c,20,ch2axis );
		plotICQch1.putPixel(c,275,ch1axis);
		plotICQch2.putPixel(c,275,ch2axis );
		cdash++;
		if (cdash==2)
			{	
			plotICQch1.putPixel(c,275-ch1Mean2Plot,ch1axis);
			plotICQch2.putPixel(c,275-ch2Mean2Plot,ch2axis);
			cdash=0;	
			}
		}
	
	plotICQch2.setFont(font);
	plotICQch1.setFont(font);
	plotICQch2.setColor(ch2axis);
	plotICQch2.drawString(s ,labelx+1,labely);
	plotICQch2.drawString(s1,labelx2,labely);
	plotICQch2.drawString(s2,labelx3,labely);
	plotICQch1.setColor(ch1axis);

	plotICQch1.drawString(s,labelx+1,labely);
	plotICQch1.drawString(s3,labelx2,labely);
	plotICQch1.drawString(s4,labelx3,labely);

//add titles
	plotICQch1.drawString(Ch1fileName ,labelx2-ch1AxisOffset2+w+20,18);
	plotICQch2.drawString(Ch2fileName ,labelx2-ch1AxisOffset2+w+20,18);


//add y axis labels
	plotICQch1.drawString(ch1MinLabel ,labelx2-ch1AxisOffset2+w,labely-12);
	plotICQch2.drawString(ch2MinLabel ,labelx2-ch2AxisOffset2+w,labely-12);
	plotICQch1.drawString(labelCh1Max,labelx2-ch1AxisOffset+w,labely-268);
	plotICQch2.drawString(labelCh2Max,labelx2-ch2AxisOffset+w,labely-268);
	plotICQch2.drawString(ch2MeanLabel,labelx2-ch2AxisOffset3+w,282-ch2Mean2Plot);
	plotICQch1.drawString(ch1MeanLabel,labelx2-ch1AxisOffset3+w,282-ch1Mean2Plot);

	new ImagePlus("ICA plot -"+Ch1fileName +": "+ ch1String , plotICQch1).show();
	IJ.run("Enhance Contrast", "saturated=0.0 equalize");
	ImagePlus ICQch1 = WindowManager.getCurrentImage();

	new ImagePlus("ICA plot -"+Ch2fileName +": "+ ch2String , plotICQch2).show();
	IJ.run("Enhance Contrast", "saturated=0.0 equalize");

	ImagePlus ICQch2 = WindowManager.getCurrentImage();
	ImageWindow winICQch1 = ICQch1.getWindow();
	ImageWindow winICQch2 = ICQch2.getWindow();
	WindowManager.setCurrentWindow(winICQch2);
	
	IJ.run("Cyan");	
	if (dualChannelIndex==0) IJ.run("Green");	
	WindowManager.setCurrentWindow(winICQch1);
	
	IJ.run("Red");	
	if (dualChannelIndex==2) IJ.run("Green");
}
 
 if (displayCounts)  displayCounts(plot);

if(overlayPositives ) {new ImagePlus("+vePDM  Values - " +fileName , plotStackPDMpositives).show();
		IJ.run("Fire");
		}

if (keepROIimage)new ImagePlus("Merged ROI - "+fileName , mergeStack).show();

if (displayPositives)
	{ String lutDir = System.getProperty("user.dir")+File.separator+"lut"+File.separator;
	new ImagePlus("PDM Values - "+fileName , plotStackPDM).show();
	IJ.run("LUT... ", "open="+"'"+lutDir+"ICA.lut"+"'");
	}

if(displayPDMs) new TextWindow( "PDMs", "X-loc\tY-loc\tPDM\tCh1\tCh2", sb2.toString(),300, 400);


double overlap = sumXY / (Math.sqrt(sumXX*sumYY));
double k1 = sumXY/sumXX;
double k2 = sumXY/sumYY;
double colocM1 = colocX/sumXtotal;
double colocM2 = colocY/sumYtotal;
double pixelratio = (double)countX/(double)countY;

//calculate ICQ

if (!useThreshold) countInclude -=countZeroPair;

//IJ.showMessage("count+ =  "+countpositive	+"   CountNonZeroPair=  "+countNonZeroPair);
double ICQ = ((double)countpositive/(double)countInclude)-0.5;
countnegative = countNonZeroPair-countpositive;

//if (countZeroPair==0) IJ.showMessage("Did you background-correct both images? \nThere appears to be no zero-zero pixel pairs");

//calculate "percentage overlap"

//int percCh1 = 100*numberBothNonZero/(numberCh1Pixels);
//int percCh2 = 100*numberBothNonZero/(numberCh2Pixels);



	String useROIstring = "";
	if (useROI) useROIstring = "\nROI used: x "+xLoc+" y"+yLoc+" w"+width+" h"+height;
  	StringBuffer ResultsOutput = new StringBuffer();
	String Headings = "Image\tRr\tR\tch1:ch2\tM1\tM2\tN+ve\tNtotal\tICQ\tCh1 Thresh\tCh2 Thresh\n";
	if ((!headingsSet)){ 
		    IJ.setColumnHeadings(Headings);
		    headingsSet = true;
	      	IJ.write(Headings);
  		}	
	useROIstring ="";
	if (currentSliceOnly) useROIstring = " z"+currentSlice;

if(!useThreshold) {ch1threshmin=0;
ch1threshmax=0;ch2threshmin=0;ch2threshmax=0;}

IJ.write(fileName+"  x "+xLoc+" y"+yLoc+ useROIstring+" w"+width+" h"+height+"\t"+df3.format(r) 
+"\t" +df3.format(overlap)+
"\t" + df3.format(pixelratio) +
"\t" +df3.format(colocM1)+
"\t" +df3.format(colocM2)+
"\t" +df0.format(countpositive)+
"\t" +df0.format(countNonZeroPair)+
"\t" + df3.format(ICQ)+
"\t" + df0.format(ch1threshmin)+"; "+df0.format(ch1threshmax)+
"\t" + df0.format(ch2threshmin)+"; "+df0.format(ch2threshmax)
);


	
	Prefs.set("ICA_ROI.boolean", useROI);
	Prefs.set("ICA_RGB.boolean",displayRGHist );
	Prefs.set("ICA_freq.boolean",displayFreqHist );
	Prefs.set("ICA_ICQplots.boolean",displayICQplots );
	Prefs.set("ICA_positives.boolean",displayPositives );
	Prefs.set("ICA_PDMs.boolean",false);
	Prefs.set("ICA_channels.int", (int)dualChannelIndex );
	Prefs.set("ICA_current.boolean",currentSliceOnly);
	Prefs.set("ICA_overlayPositives.boolean",overlayPositives);
	Prefs.set("ICA_counts.boolean",displayCounts);
	Prefs.set("ICA_displayPDMs.boolean",displayPDMs);
	Prefs.set("ICA_threshold.boolean", useThreshold);
	Prefs.set("ICA_keepROI.boolean", keepROIimage);
	Prefs.set("ICA_indexRoi.int",indexRoi);

imp1.changes = false;
imp2.changes = false;
 IJ.setBackgroundColor( 0, 0,0);
IJ.setForegroundColor(255,255,255 );
//imp1.setRoi(roi);
	IJ.showStatus("Done");
	//new ImagePlus("ICA plot -"+Ch2fileName +": "+ ch2String , plotICQch22).show();


System.gc();
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

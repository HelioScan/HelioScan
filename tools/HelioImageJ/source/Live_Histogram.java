import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.Filler;
import ij.plugin.filter.PlugInFilter;
import ij.text.TextWindow;
import ij.gui.*;

/** Driver for HistogramLive. */

public class Live_Histogram implements PlugInFilter {
	ImagePlus imp;
	int NBINS = 256;
    public int setup(String arg, ImagePlus imp) {
    	this.imp = imp;
        return DOES_ALL;
    }

    public void run(ImageProcessor ip) {
    	LiveHistogram lh = new LiveHistogram("Live histogram of "+imp.getShortTitle(),imp, NBINS);
 	}   
}

/** This class is an extended ImageWindow that displays histograms. This version is "live".
	Version 0 5/24/2002
	Version 1 5/25/2002.  Handle case with no ROI.
	Version 2 5/26/2002.  Fixed a problem with non-rectangular ROIs 
	Version 3 5/27/2002.  dB scale.
	Version 4 5/28/2002.  Fixed treament of 8-bit color images.
	Version 5 5/29/2002   Adjustable display limits, thresholding, more display options.
	Version 6 5/29/2002   Added square root scale and fixed "fixed-range" plots.
	Version 7 5/30/2002   FocusLost for TextFields.  Blank plot if everything thresholded.
	Version 7.1 5/30      Corrects the default value of maxBin in case of input error.
	Version 8 6/5/2002    Adopt current window.  Min Bin and Max Bin adjustments w/ 16-bit images.
	Version 8.5 6/15      minBin and maxBin adjsted by Adjust Threshold plugin, at least for 8-bit data.*/
class LiveHistogram extends ImageWindow implements Measurements, ActionListener, FocusListener,ClipboardOwner,
				Runnable{
	static final int WIN_WIDTH = 328;
	static final int WIN_HEIGHT = 240;
	static final int HIST_WIDTH = 256;
	static final int HIST_HEIGHT = 128;
	static final int BAR_HEIGHT = 12;
	static final int XMARGIN = 20;
	static final int YMARGIN = 10;
	
	protected ImageStatistics stats;
	protected int[] histogramL;
	protected LookUpTable lut;
	protected Rectangle frame = null;
	protected Button list, save, copy;
	protected Choice display;
	protected TextField minHistTF, maxHistTF, minBinTF, maxBinTF;
	protected CheckboxGroup controlGroup;
	protected Checkbox auto, manual;
	private GridBagLayout grid;
	private GridBagConstraints c;
	protected Label value, count;
	protected static String defaultDirectory = null;
	protected int decimalPlaces;
	protected int digits;
	protected int newMaxCount;
	protected Calibration cal;
	public int numBins;
	
	protected Thread thread;
	protected ImagePlus impData;
	protected boolean done;
	protected boolean color;
	protected int minHist = 0;
	protected int maxHist = 1000;
	protected int minBin = 0;
	protected int maxBin = 255;
	protected double hist_dB_range;
	protected int total;
	protected boolean messageMin = true;
	protected boolean messageMax = true;
	protected boolean oldShort;

	/** Displays a histogram using the specified title. */
	public LiveHistogram(String title, ImagePlus imp, int numBins) {
		super(NewImage.createByteImage(title, WIN_WIDTH, WIN_HEIGHT, 1, NewImage.FILL_WHITE));
		this.numBins = numBins;
		
		impData = imp;
		thread = new Thread(this, "HistogramLive");
		//thread.setPriority(Thread.currentThread().getPriority()-1); //Can't seem to do it since current = 1.
		oldShort = ImageJ.VERSION.compareTo("1.28i")<0;
		if (oldShort)
			/*IJ.showMessage("ImageJ version is < 1.28i.  Adjust Threshold will not\nintegrate with Live Histogram for 16 bit data.");*/
		setup();
		thread.start();
	}

	public void showHistogram() {
		ImagePlus impCurr = WindowManager.getCurrentImage();
		ImagePlus imp1 = (impCurr==this.imp)?impData:impCurr;
		if (imp1 == null){
			shutDown();
			return;
		}
		try{
			cal = imp1.getCalibration();
			ImageProcessor ipData = imp1.getProcessor();
			if ((minBin == 0)&&(maxBin == 255)){
				stats = imp1.getStatistics(AREA+MEAN+MODE+MIN_MAX+LIMIT, numBins);
			} else {
				//if ((ipData instanceof ShortProcessor)&oldShort){
				if (ipData instanceof ShortProcessor){
					stats = new LS(ipData, AREA+MEAN+MODE+MIN_MAX+LIMIT, imp1.getCalibration());
				} else {
					stats = imp1.getStatistics(AREA+MEAN+MODE+MIN_MAX+LIMIT, numBins);
				}				
			}		
			histogramL = stats.histogram;
			
			//Update minBin and maxBin from image threshold values.
			double hmin = cal.getCValue(stats.histMin);
			double hmax = cal.getCValue(stats.histMax);
			double minThresh = ipData.getMinThreshold();
			double maxThresh = ipData.getMaxThreshold();
			if (hmax != hmin){
				int indexMinThresh = (int)((HIST_WIDTH-1)*(minThresh - hmin)/(hmax - hmin));
				int indexMaxThresh = (int)((HIST_WIDTH-1)*(maxThresh - hmin)/(hmax - hmin));
				if ((indexMinThresh!=minBin)&&(indexMinThresh>=0)&&(indexMinThresh<=255)){
					minBin = indexMinThresh;
					minBinTF.setText(d2s(minBin));
				}
				if ((indexMaxThresh!=maxBin)&&(indexMaxThresh>=0)&&(indexMaxThresh<=255)){
					maxBin = indexMaxThresh;
					maxBinTF.setText(d2s(maxBin));
				}
			}
			
			 
			if(histogramL.length != numBins){
				IJ.showMessage("Error: numBins = "+numBins+" histogram.length = "+histogramL.length);
				shutDown();
				return;
			}
			lut = imp1.createLut();
			int type = imp1.getType();
			boolean fixedRange = type==ImagePlus.GRAY8 || type==ImagePlus.COLOR_256 || type==ImagePlus.COLOR_RGB;
			ImageProcessor ip = this.imp.getProcessor();
			color = !(imp1.getProcessor() instanceof ColorProcessor) && !lut.isGrayscale();
			if (color)
				ip = ip.convertToRGB();
			drawHistogram(ip, fixedRange);
			if (imp1 != impData){
				impData = imp1;
				this.imp.setTitle("Live histogram of "+imp1.getShortTitle());
			}
			if (color)
				this.imp.setProcessor(null, ip);
			else
				this.imp.updateAndDraw();
		} catch (java.lang.NullPointerException e) {
			this.imp.setTitle("Live histogram of: No Image");
			//shutDown();
			//clear(this.imp.getProcessor());
		} catch (java.lang.IllegalArgumentException ea){
			//clear(this.imp.getProcessor());
		} catch (java.lang.ArrayIndexOutOfBoundsException eo){
			//clear(this.imp.getProcessor());
		}
	}	

	public void run() {
		while (!done) {
			try {Thread.sleep(100);}
			catch(InterruptedException e) {}
			showHistogram();
		}
	}
		
    public void windowClosing(WindowEvent e) {
		super.windowClosing(e);
		shutDown();
	}
	
	public void shutDown(){
		done = true;
	}		

	public void setup() {
 		Panel buttons = new Panel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		list = new Button("List");
		list.addActionListener(this);
		buttons.add(list);
		copy = new Button("Copy");
		copy.addActionListener(this);
		buttons.add(copy);
		display = new Choice();
		display.add("Linear");//0
		display.add("Log");//1
		display.add("Sqrt");//2
		display.add("Lin fxd");//3
		display.add("Sqrt fxd");//4
		display.add("30 dB");//5
		display.add("40 dB");//6
		display.add("50 dB");//7
		buttons.add(display);
		Panel valueAndCount = new Panel();
		valueAndCount.setLayout(new GridLayout(2, 1));
		value = new Label("                  "); //21
		value.setFont(new Font("Monospaced", Font.PLAIN, 12));
		valueAndCount.add(value);
		count = new Label("                  ");
		count.setFont(new Font("Monospaced", Font.PLAIN, 12));
		valueAndCount.add(count);
		buttons.add(valueAndCount);
		add(buttons);
		
 		Panel limits = new Panel();
		limits.setLayout(new GridLayout(2, 6));
		Label lowLabel, highLabel;
    	if (IJ.isMacintosh()){
    		lowLabel = new Label("Min Plot ");
   			highLabel = new Label("Max Plot ");
    	} else {
    		lowLabel = new Label("Min Plot");
   			highLabel = new Label("Max Plot");
   		}
   		minHistTF = new TextField(d2s(minHist));
   		minHistTF.addActionListener(this);
   		minHistTF.addFocusListener(this);
   		maxHistTF = new TextField(d2s(maxHist));
   		maxHistTF.addActionListener(this);
   		maxHistTF.addFocusListener(this);
   		limits.add(lowLabel);
   		limits.add(minHistTF);
   		limits.add(highLabel);
    	limits.add(maxHistTF);
    	
		controlGroup = new CheckboxGroup();
		
		auto = new Checkbox("Auto");
		auto.setCheckboxGroup(controlGroup);
		limits.add(auto);
		
		manual = new Checkbox("Manual");
		manual.setCheckboxGroup(controlGroup);
		limits.add(manual);	
		auto.setState(true);
		    	
		Label lowThresh, highThresh;
    	if (IJ.isMacintosh()){
    		lowThresh = new Label("Min Bin ");
   			highThresh = new Label("Max Bin ");
    	} else {
    		lowThresh = new Label("Min Bin");
   			highThresh = new Label("Max Bin");
   		}
   		minBinTF = new TextField(d2s(minBin));
   		minBinTF.addActionListener(this);
   		minBinTF.addFocusListener(this);
   		maxBinTF = new TextField(d2s(maxBin));
   		maxBinTF.addActionListener(this);
   		maxBinTF.addFocusListener(this);
   		limits.add(lowThresh);
   		limits.add(minBinTF);
   		limits.add(highThresh);
    	limits.add(maxBinTF);    	
    	limits.add(new Label(" "));
       	limits.add(new Label(" "));
    	  	
    	add(limits);
 	
		pack();
		
    }
    

	public void mouseMoved(int x, int y) {
		if (value==null || count==null)
			return;
		if ((frame!=null)  && x>=frame.x && x<=(frame.x+frame.width)) {
			x = x - frame.x;
			if (x>255) x = 255;
			int index = (int)(x*((double)histogramL.length)/HIST_WIDTH);
			value.setText("  Value: " + IJ.d2s(cal.getCValue(stats.histMin+index*stats.binSize), digits));
			count.setText("  Count: " + histogramL[index]);
		} else {
			value.setText("");
			count.setText("");
		}
	}
    
	protected void drawHistogram(ImageProcessor ip, boolean fixedRange) {
		int x, y;
		int maxCount2 = 0;
		if(ip == null){
			shutDown();
			return;
		}		    	
		ip.setColor(Color.black);
		ip.setLineWidth(1);
		decimalPlaces = Analyzer.getPrecision();
		digits = cal.calibrated()||stats.binSize!=1.0?decimalPlaces:0;
		total = 0;
		for (int i = minBin; i <= maxBin; i++){
			total += histogramL[i];
 			if ((histogramL[i] > maxCount2) && (i != stats.mode)) {
				maxCount2 = histogramL[i];
  			}
  		}
 		newMaxCount = stats.maxCount;
		if ((newMaxCount>(maxCount2 * 2)) && (maxCount2 != 0)) {
			newMaxCount = (int)(maxCount2 * 1.5);
 		}
 		
		int disp = display.getSelectedIndex();
 		if (auto.getState()){
			if (disp <= 2){
 				maxHist = newMaxCount;
 			} else {
 				maxHist = total;
 			} 			
 			maxHistTF.setText(d2s(maxHist));
 			if (disp == 5){
 				minHist = maxHist/1000;
 			} else if (disp == 6){
 				minHist = maxHist/10000;
 			} else if (disp == 7){
 				minHist = maxHist/100000;
 			} else {
 				minHist = 0;
 			}
  			minHistTF.setText(d2s(minHist));
 		} else {
  			if (disp == 5){
 				minHist = maxHist/1000;
  				minHistTF.setText(d2s(minHist));
			} else if (disp == 6){
 				minHist = maxHist/10000;
  				minHistTF.setText(d2s(minHist));
 			} else if (disp == 7){
 				minHist = maxHist/100000;
  				minHistTF.setText(d2s(minHist));
 			}
 		}
		if(newMaxCount > 0){
			if (disp == 0) {	
				clear(ip);
				drawPlot(ip);
			} else if (disp == 1){
				clear(ip);				
				drawLogPlot(ip);
				drawPlot(ip);
			} else if (disp == 2){
				clear(ip);				
				drawSqrtPlot(ip);
				drawPlot(ip);
			} else if (disp == 3){
				clear(ip);				
				drawPlot(ip);
			} else if (disp == 4){
				clear(ip);				
				drawSqrtPlot(ip);
				drawPlot(ip);
			} else if (disp == 5){
				hist_dB_range = 30;
				clear(ip);				
				drawdBPlot(ip);
			} else if (disp == 6){
				hist_dB_range = 40;
				clear(ip);				
				drawdBPlot(ip);
			} else if (disp == 7){
				hist_dB_range = 50;
				clear(ip);				
				drawdBPlot(ip);
			}
		} else {
			clear(ip);
		}
		
 		x = XMARGIN + 1;
		y = YMARGIN + HIST_HEIGHT + 2;
		lut.drawUnscaledColorBar(ip, x-1, y, 256, BAR_HEIGHT);
		y += BAR_HEIGHT+15;
  		drawText(ip, x, y, fixedRange);
	}
	
	void clear(ImageProcessor ip){
		if(ip == null)return;
		if(color){
			int white = 0xffffff;
			int[] pixels = (int[])ip.getPixels();
			int n = pixels.length;
			for (int i = 0; i < n; i++) pixels[i] = white;	
		} else {
			byte white = (byte)255;
			byte[] pixels = (byte[])ip.getPixels();
			int n = pixels.length;
			for (int i = 0; i < n; i++) pixels[i] = white;
		}
	}

       
	void drawPlot(ImageProcessor ip) {
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.drawRect(frame.x-1, frame.y, frame.width+2, frame.height+1);
		int index, y;
		int histRange = maxHist - minHist;
		if (histRange <= 0)return;
		for (int i = 0; i<HIST_WIDTH; i++) {
			index = (int)(i*(double)histogramL.length/HIST_WIDTH); 
			if((index < minBin) || (index > maxBin)){
				y = 0;
			} else {
				y = (int)(HIST_HEIGHT*(histogramL[index] - minHist)/histRange);
				if (y > HIST_HEIGHT) y = HIST_HEIGHT;
				if (y < 0) y = 0;
			}
			ip.drawLine(i+XMARGIN, YMARGIN+HIST_HEIGHT, i+XMARGIN, YMARGIN+HIST_HEIGHT-y);
		}
	}
	void drawSqrtPlot(ImageProcessor ip) {
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.drawRect(frame.x-1, frame.y, frame.width+2, frame.height+1);
		int index, y;
		if(maxHist <= 0)return;
		if(minHist < 0)return;
		double max = Math.sqrt(maxHist);
		double min = Math.sqrt(minHist);
		double histRange = max - min;
		if (histRange <= 0)return;
		ip.setColor(Color.gray);
		for (int i = 0; i<HIST_WIDTH; i++) {
			index = (int)(i*(double)histogramL.length/HIST_WIDTH); 
			if((index < minBin) || (index > maxBin)){
				y = 0;
			} else {
				y = (int)(HIST_HEIGHT*(Math.sqrt(histogramL[index]) - min)/histRange);
				if (y > HIST_HEIGHT) y = HIST_HEIGHT;
				if (y < 0) y = 0;
			}
			ip.drawLine(i+XMARGIN, YMARGIN+HIST_HEIGHT, i+XMARGIN, YMARGIN+HIST_HEIGHT-y);
		}
		ip.setColor(Color.black);
	}
		
	void drawLogPlot (ImageProcessor ip) {
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.drawRect(frame.x-1, frame.y, frame.width+2, frame.height+1);
		if(maxHist <= 0)return;
		double max = Math.log(maxHist);
		ip.setColor(Color.gray);
		int index, y;
		for (int i = 0; i<HIST_WIDTH; i++) {
			index = (int)(i*(double)histogramL.length/HIST_WIDTH); 
			if((index < minBin) || (index > maxBin)){
				y = 0;
			} else {
				y = histogramL[index]<=0?0:(int)(HIST_HEIGHT*Math.log(histogramL[index])/max);
				if (y>HIST_HEIGHT)
					y = HIST_HEIGHT;
			}
			ip.drawLine(i+XMARGIN, YMARGIN+HIST_HEIGHT, i+XMARGIN, YMARGIN+HIST_HEIGHT-y);
		}
		ip.setColor(Color.black);
	}
	void drawdBPlot (ImageProcessor ip) {
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.drawRect(frame.x-1, frame.y, frame.width+2, frame.height+1);
		ip.setColor(Color.gray);
		int index, y;
		double scale = (HIST_HEIGHT/hist_dB_range)*(10/Math.log(10));
		/* Decibel scale. */
		if (maxHist <= 0) return;
		for (int i = 0; i<HIST_WIDTH; i++) {
			index = (int)(i*(double)histogramL.length/HIST_WIDTH); 
			if((index < minBin) || (index > maxBin)){
				y = 0;
			} else {
				y = histogramL[index]<=0?0:(int)(HIST_HEIGHT + scale*Math.log(histogramL[index]/(double)maxHist)); 
				if (y < 0)
					y = 0;
				if (y>HIST_HEIGHT)
					y = HIST_HEIGHT;
			}
			ip.drawLine(i+XMARGIN, YMARGIN+HIST_HEIGHT, i+XMARGIN, YMARGIN+HIST_HEIGHT-y);
		}
		ip.setColor(Color.black);
	}
		
	void drawText(ImageProcessor ip, int x, int y, boolean fixedRange) {
		ip.setFont(new Font("SansSerif",Font.PLAIN,12));
		double hmin = cal.getCValue(stats.histMin);
		double hmax = cal.getCValue(stats.histMax);
        
		double binWidth = fixedRange&&!cal.calibrated()?stats.binSize:(hmax-hmin)/stats.nBins;
		binWidth = Math.abs(binWidth);
		boolean showBins = binWidth!=1.0 || !fixedRange;
		int col1 = XMARGIN + 5;
		int col2 = XMARGIN + HIST_WIDTH/2;
		int row1 = y+25;
		if (showBins) row1 -= 8;
		int row2 = row1 + 15;
		int row3 = row2 + 15;
		int row4 = row3 + 15;
		ip.drawString("Count: " + stats.pixelCount, col1, row1);
		ip.drawString("Mean: " + d2s(stats.mean), col1, row2);
		ip.drawString("StdDev: " + d2s(stats.stdDev), col1, row3);
		ip.drawString("Mode: " + d2s(stats.dmode) + " (" + stats.maxCount + ")", col2, row3);
		ip.drawString("Min: " + d2s(stats.min), col2, row1);
		ip.drawString("Max: " + d2s(stats.max), col2, row2);
		
		
		ip.setFont(new Font("SansSerif",Font.PLAIN,10));
		ip.drawString(d2s(hmin), x - 4, y);
		ip.drawString(d2s(hmax), x + HIST_WIDTH - getWidth(hmax, ip) + 10, y);	
		int right = 1;
		int up = 22;
		ip.drawString(d2s(minHist),x + HIST_WIDTH + right, y - up);
		ip.drawString(d2s(maxHist),x + HIST_WIDTH + right, y - up - HIST_HEIGHT);
		
		if (showBins) {
			ip.drawString("Bins: " + d2s(stats.nBins), col1, row4);
			ip.drawString("Bin Width: " + d2s(binWidth), col2, row4);
		}
	}

	String d2s(double d) {
		if ((int)d==d)
			return IJ.d2s(d,0);
		else
			return IJ.d2s(d,decimalPlaces);
	}
	
	int getWidth(double d, ImageProcessor ip) {
		return ip.getStringWidth(d2s(d));
	}

	void showList() {
		StringBuffer sb = new StringBuffer();
		for (int i=minBin; i<= maxBin; i++)
			sb.append(IJ.d2s(cal.getCValue(stats.histMin+i*stats.binSize), digits)+"\t"+histogramL[i]+"\n");
		TextWindow tw = new TextWindow(getTitle(), "value\tcount", sb.toString(), 200, 400);
	}

	void copyToClipboard() {
		Clipboard systemClipboard = null;
		try {systemClipboard = getToolkit().getSystemClipboard();}
		catch (Exception e) {systemClipboard = null; }
		if (systemClipboard==null)
			{IJ.error("Unable to copy to Clipboard."); return;}
		IJ.showStatus("Copying histogram values...");
		CharArrayWriter aw = new CharArrayWriter(stats.nBins*4);
		PrintWriter pw = new PrintWriter(aw);
		for (int i=minBin; i<=maxBin; i++)
			pw.print(IJ.d2s(cal.getCValue(stats.histMin+i*stats.binSize), digits)+"\t"+histogramL[i]+"\n");
		String text = aw.toString();
		pw.close();
		StringSelection contents = new StringSelection(text);
		systemClipboard.setContents(contents, this);
		IJ.showStatus(text.length() + " characters copied to Clipboard");
	}
			
	public void actionPerformed(ActionEvent e) {
		Object b = e.getSource();
		if (b instanceof TextField) {
			handleText((TextField)b);
		} else {
			if (b==list)
				showList();
			else if (b==copy)
				copyToClipboard();
		}
	}
	
	public void focusGained(FocusEvent e) {
		Component c = e.getComponent();
		if (c instanceof TextField)
			((TextField)c).selectAll();
	}

	public void focusLost(FocusEvent e) {
		Component c = e.getComponent();
		if (c instanceof TextField){
			handleText((TextField)c);
		}
	}
	
	protected void handleText(TextField b){
		try{
			if (WindowManager.getWindowCount() < 2) return;
			if (impData == null) return;
			if (impData == this.imp) return;
			if (b == minHistTF){
				int newMin = readNumber(minHistTF);
				if (auto.getState()){
					minHistTF.setText(d2s(minHist));			
				} else if (newMin < 0) {
					IJ.beep();
					minHistTF.setText(d2s(minHist));
				} else {
					minHist = newMin;
				}
			}
			else if (b == maxHistTF){
				int newMax = readNumber(maxHistTF);
				if (auto.getState()){
					maxHistTF.setText(d2s(maxHist));			
				} else if (newMax < 0) {
					IJ.beep();
					maxHistTF.setText(d2s(maxHist));
				} else {
					maxHist = newMax;
				}
			}
			else if (b == minBinTF){
				int newMin = readNumber(minBinTF);
				if ((newMin < 0)||(newMin > 255)){
					IJ.beep();
					minBin = 0;
					minBinTF.setText(d2s(minBin));
				} else {
					ImageProcessor ip = impData.getProcessor();
					boolean typeOK = (ip instanceof ByteProcessor)||(ip instanceof ShortProcessor);
					if ((newMin!=0)&&(!typeOK)){
						if(messageMin){
							messageMin = false; //It actually matters that this stamement preceed the next one!
							IJ.showMessage("Min Bin adjustments are limited to 8-bit and 16-bit images.");
						}
						minBin = 0;
						minBinTF.setText(d2s(minBin));
					} else {
						minBin = newMin;
					}
				}
				impData.getProcessor().setThreshold(minBin,maxBin,ImageProcessor.NO_LUT_UPDATE);
			}
			else if (b == maxBinTF){
				int newMax = readNumber(maxBinTF);
				if ((newMax < 0)||(newMax > 255)){
					IJ.beep();
					maxBin = 255;
					maxBinTF.setText(d2s(maxBin));
				} else {
					ImageProcessor ip = impData.getProcessor();
					boolean typeOK = (ip instanceof ByteProcessor)||(ip instanceof ShortProcessor);
					if ((newMax!=255)&&(!typeOK)){
						if(messageMax){
							messageMax = false;
							IJ.showMessage("Max Bin adjustments are limited to 8-bit and 16-bit images.");
						}
						maxBin = 255;
						maxBinTF.setText(d2s(maxBin));
					} else {
						maxBin = newMax;
					}
				}
				impData.getProcessor().setThreshold(minBin,maxBin,ImageProcessor.NO_LUT_UPDATE);
			}
		} catch (java.lang.NullPointerException e){}	
	}
	
	public int readNumber(TextField tf){
		String s = tf.getText();
		int result;
		try {
			Integer Int = new Integer(s.trim());
			result = Int.intValue();
		} catch (NumberFormatException e) {
			result = -1;
		}
		return result;
	}
			
	
	public void lostOwnership(Clipboard clipboard, Transferable contents) {}
	
	
	//The following will not be necessary when the statistics calculations for 16-bit images
	//reflect the trehsold settings.


/** 16-bit image statistics, including histogram. */
	public class LS extends ImageStatistics {

		public LS(ImageProcessor ip, int mOptions, Calibration cal) {
			this.width = ip.getWidth();
			this.height = ip.getHeight();
			setupSS(ip, cal);
			//minThreshold=0;
			//maxThreshold=65535;

			double minT = ip.getMinThreshold();
			int minThreshold,maxThreshold;
	
			int[] hist = ip.getHistogram(); // 65536 bin histogram
			minThreshold = minBin;
			maxThreshold = maxBin;
			float[] cTable = cal!=null?cal.getCTable():null;
			getRawMinAndMax(hist);
			histMin = min;
			histMax = max;
			getStatistics(hist, (int)min, (int)max, minThreshold, maxThreshold, cTable);
			getMode(minThreshold, maxThreshold,cTable);
			getFinalMinAndMax(hist, (int)min, (int)max, minThreshold, maxThreshold, cTable);
		}
		void setupSS(ImageProcessor ip, Calibration cal) {
			width = ip.getWidth();
			height = ip.getHeight();
			Rectangle roi = ip.getRoi();
			if (roi != null) {
				rx = roi.x;
				ry = roi.y;
				rw = roi.width;
				rh = roi.height;
			}
			else {
				rx = 0;
				ry = 0;
				rw = width;
				rh = height;
			}
		
			if (cal!=null) {
				pw = cal.pixelWidth;
				ph = cal.pixelHeight;
			} else {
				pw = 1.0;
				ph = 1.0;
			}
		
			roiX = rx*pw;
			roiY = ry*ph;
			roiWidth = rw*pw;
			roiHeight = rh*ph;
		}

		void getRawMinAndMax(int[] hist) {
			int min = 0;
			while ((hist[min]==0) && (min<65535))
				min++;
			this.min = min;
			
			int max = 65535;
			while ((hist[max]==0) && (max>0))
				max--;
			this.max = max;
		}

		void getStatistics(int[] hist, int min, int max, int minThreshold, int maxThreshold, float[] cTable) {
			int count;
			double value;
			double sum = 0.0;
			double sum2 = 0.0;
			double scale = (double)(nBins/(histMax-histMin));
			int hMin = (int)histMin;
			binSize = (histMax-histMin)/nBins;
			histogram = new int[nBins]; // 256 bin histogram
			int index;
				
			for (int i=min; i<=max; i++) {
				count = hist[i];
				index = (int)(scale*(i-hMin));
				if (index>=nBins)
					index = nBins-1;
				histogram[index] += count;
				if((index >= minThreshold)&(index <= maxThreshold)){
					pixelCount += count;
					value = cTable==null?i:cTable[i];
					sum += value*count;
					sum2 += (value*value)*count;
				}
			}
			area = pixelCount*pw*ph;
			if(pixelCount > 0){
				mean = sum/pixelCount;
			} else {
				mean = 0;
			}
			umean = mean;
			calculateStdDevSS(pixelCount, sum, sum2);
		}
	
		void getMode(int minThreshold, int maxThreshold,float[] cTable) {
        	int count;
        	maxCount = 0;
        	for (int i=minThreshold; i<= maxThreshold; i++) {
        		count = histogram[i];
            	if (count > maxCount) {
                	maxCount = count;
                	mode = i;
            	}
        	}
        	dmode = histMin+mode*binSize;
        	if (cTable!=null)
        	dmode = cTable[(int)dmode];
			//ij.IJ.write("mode2: "+mode+" "+dmode+" "+maxCount);
		}

		
		void calculateStdDevSS(int n, double sum, double sum2) {
			//ij.IJ.write("calculateStdDev: "+n+" "+sum+" "+sum2);
			if (n>0) {
				stdDev = (n*sum2-sum*sum)/n;
				if (stdDev>0.0)
					stdDev = Math.sqrt(stdDev/(n-1.0));
				else
					stdDev = 0.0;
			}
			else
				stdDev = 0.0;
		}


		void getFinalMinAndMax(int[] hist, int minValue, int maxValue, int minThreshold, int maxThreshold, float[] cTable) {
			min = Double.MAX_VALUE;
			max = -Double.MAX_VALUE;
			double v = 0.0;
			double scale = (double)(nBins/(histMax-histMin));
			int hMin = (int)histMin;
			int index;
			if (cTable!=null) {
				for (int i=minValue; i<=maxValue; i++) {
					index = (int)(scale*(i-hMin));
					if (index>=nBins)index = nBins-1;			
					if((index >= minThreshold)&(index <= maxThreshold)){
						if (hist[i]>0) {
							v = cTable[i];
							if (v<min) min = v;
							if (v>max) max = v;
						}
					}
				}
			} else {
				for (int i=minValue; i<=maxValue; i++) {
					index = (int)(scale*(i-hMin));
					if (index>=nBins)index = nBins-1;			
					if((index >= minThreshold)&(index <= maxThreshold)){
						if (hist[i]>0) {
							if (i<min) min = i;
							if (i>max) max = i;
						}
					}
				}
			}
		}//getFinalMinAndMax
	}//ShortStatisticsLH
}//LiveHistogram










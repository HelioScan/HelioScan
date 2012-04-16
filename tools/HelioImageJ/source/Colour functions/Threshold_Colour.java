import ij.*;
import ij.plugin.filter.PlugInFilter;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.awt.event.*;
import ij.measure.*;
import ij.plugin.frame.*;
import ij.plugin.*;

/*	This plugin isolates pixels in an RGB image or stack according to a range of Hue.
	Bob Dougherty  Some code borrowed from ThresholdAdjuster by Wayne Rasband.

	Version 0 5/12/2002.
	Version 1 5/13			Filtered pixels set to foreground color.  Speed improved.
	Version 2 5/13.			Fixed a bug in setting the restore pixels that was causing problems with stacks.
							Explicitly get the foreground color from the toolbar in apply.
	Version 3 17/Feb/2004.	Modified by G. Landini. The changes are seen as the sliders/checkboxes
							are adjusted. Added hue strip to histogram window, changed histogram scale
							factor
	Version 4 19/Feb/2004.	Modified by G. Landini. Added Saturation and Brightness histograms,
							Added Pass/Stop checkboxes for each HSB channel.
							Added threshold, added inversion of threshold
							Cleaned some variables. Changed name to Threshold_HSB
	Version 5 22/Feb/2004   Modified by G. Landini. Threshold in RGB or HSB space
							Changed name to Threshold_Colour, changed button names.
							Added thresholding by "sampling". Hue band sampled selection may not'
							always work if there are 0 valued histograms. Thread now finishes properly
 */

public class Threshold_Colour implements PlugInFilter {
	ImagePlus imp;
	protected boolean isRGB = false, flag = false;

	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		BandAdjuster ba = new BandAdjuster();
 	}

	/**  Selects pixels according to hsb or rgb components.  */
	public class BandAdjuster extends PlugInFrame implements PlugIn, Measurements,
	 ActionListener, AdjustmentListener, FocusListener, ItemListener, Runnable{


		protected Thread thread;
		Frame instance;

		BandPlot plot = new BandPlot();
		BandPlot splot = new BandPlot();
		BandPlot bplot = new BandPlot();
		int sliderRange = 256;
		Panel panelh, panels, panelb, panel, panelt, panelMode;
		Button  originalB, filteredB, stackB, helpB, sampleB,resetallB;
		Checkbox bandPassH, bandStopH, bandPassS, bandStopS,bandPassB, bandStopB, threshold, invert, hsb, rgb;
		CheckboxGroup filterTypeH, filterTypeS, filterTypeB, colourMode;
		int previousImageID = -1;
		int previousSlice = -1;
		ImageJ ij;
		int minHue = 0, minSat = 0, minBri = 0;
		int maxHue = 255, maxSat = 255, maxBri = 255;
		Scrollbar minSlider, maxSlider,minSlider2, maxSlider2,minSlider3, maxSlider3;
		Label label1, label2,label3, label4,label5, label6, labelh, labels, labelb, labelf;
		boolean done;
		byte[] hSource,sSource,bSource;
		int[] fillMask,restore;
		ImagePlus imp;
		ImageProcessor ip;

		int numSlices ;
		ImageStack stack;
		int width,height,numPixels;

		public BandAdjuster() {

			super("Threshold Colour");
			if (instance!=null) {
				instance.toFront();
				return;
			}
			imp = WindowManager.getCurrentImage();
			if (imp==null) {
				IJ.beep();
				IJ.showStatus("No image");
				return;
			}
			IJ.run("Select None");
			thread = new Thread(this, "BandAdjuster");
			WindowManager.addWindow(this);
			instance = this;
			IJ.register(PasteController.class);

			ij = IJ.getInstance();
			Font font = new Font("SansSerif", Font.PLAIN, 10);
			GridBagLayout gridbag = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			setLayout(gridbag);

			int y = 0;
			c.gridx = 0;
			c.gridy = y;
			c.gridwidth = 1;
			c.weightx = 0;
			c.insets = new Insets(5, 0, 0, 0);
			labelh = new Label("Hue", Label.CENTER);
			add(labelh, c);

			c.gridx = 1;
			c.gridy = y++;
			c.gridwidth = 1;
			c.weightx = 0;
			c.insets = new Insets(7, 0, 0, 0);
			labelf = new Label("Filter type", Label.RIGHT);
			add(labelf, c);

			// plot
			c.gridx = 0;
			c.gridy = y;
			c.gridwidth = 1;
			c.fill = c.BOTH;
			c.anchor = c.CENTER;
			c.insets = new Insets(0, 5, 0, 0);
			add(plot, c);

			// checkboxes
			panelh = new Panel();
			filterTypeH = new CheckboxGroup();
			bandPassH = new Checkbox("Pass");
			bandPassH.setCheckboxGroup(filterTypeH);
			bandPassH.addItemListener(this);
			panelh.add(bandPassH);
			bandStopH = new Checkbox("Stop");
			bandStopH.setCheckboxGroup(filterTypeH);
			bandStopH.addItemListener(this);
			panelh.add(bandStopH);
			bandPassH.setState(true);
			c.gridx = 1;
			c.gridy = y++;
			c.gridwidth = 2;
			c.insets = new Insets(5, 0, 0, 0);
			add(panelh, c);

			// minHue slider
			minSlider = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, sliderRange);
			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 1;
			c.weightx = IJ.isMacintosh()?90:100;
			c.fill = c.HORIZONTAL;
			c.insets = new Insets(5, 5, 0, 0);

			add(minSlider, c);
			minSlider.addAdjustmentListener(this);
			minSlider.setUnitIncrement(1);

			// minHue slider label
			c.gridx = 1;
			c.gridwidth = 1;
			c.weightx = IJ.isMacintosh()?10:0;
			c.insets = new Insets(5, 0, 0, 0);
			label1 = new Label("       ", Label.LEFT);
			label1.setFont(font);
			add(label1, c);

			// maxHue sliderHue
			maxSlider = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, sliderRange);
			c.gridx = 0;
			c.gridy = y;
			c.gridwidth = 1;
			c.weightx = 100;
			c.insets = new Insets(5, 5, 0, 0);
			add(maxSlider, c);
			maxSlider.addAdjustmentListener(this);
			maxSlider.setUnitIncrement(1);

			// maxHue slider label
			c.gridx = 1;
			c.gridwidth = 1;
			c.gridy = y++;
			c.weightx = 0;
			c.insets = new Insets(5, 0, 0, 0);
			label2 = new Label("       ", Label.LEFT);
			label2.setFont(font);
			add(label2, c);

			//=====
			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 1;
			c.weightx = 0;
			c.insets = new Insets(10, 0, 0, 0);
			labels = new Label("Saturation", Label.CENTER);
			add(labels, c);

			// plot
			c.gridx = 0;
			c.gridy = y;
			c.gridwidth = 1;
			c.fill = c.BOTH;
			c.anchor = c.CENTER;
			c.insets = new Insets(0, 5, 0, 0);
			add(splot, c);

			// checkboxes
			panels = new Panel();
			filterTypeS = new CheckboxGroup();
			bandPassS = new Checkbox("Pass");
			bandPassS.setCheckboxGroup(filterTypeS);
			bandPassS.addItemListener(this);
			panels.add(bandPassS);
			bandStopS = new Checkbox("Stop");
			bandStopS.setCheckboxGroup(filterTypeS);
			bandStopS.addItemListener(this);
			panels.add(bandStopS);
			bandPassS.setState(true);
			c.gridx = 1;
			c.gridy = y++;
			c.gridwidth = 2;
			c.insets = new Insets(5, 0, 0, 0);
			add(panels, c);

			// minSat slider
			minSlider2 = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, sliderRange);
			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 1;
			c.weightx = IJ.isMacintosh()?90:100;
			c.fill = c.HORIZONTAL;
			c.insets = new Insets(5, 5, 0, 0);
			add(minSlider2, c);
			minSlider2.addAdjustmentListener(this);
			minSlider2.setUnitIncrement(1);

			// minSat slider label
			c.gridx = 1;
			c.gridwidth = 1;
			c.weightx = IJ.isMacintosh()?10:0;
			c.insets = new Insets(5, 0, 0, 0);
			label3 = new Label("       ", Label.LEFT);
			label3.setFont(font);
			add(label3, c);

			// maxSat slider
			maxSlider2 = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, sliderRange);
			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 1;
			c.weightx = 100;
			c.insets = new Insets(5, 5, 0, 0);
			add(maxSlider2, c);
			maxSlider2.addAdjustmentListener(this);
			maxSlider2.setUnitIncrement(1);

			// maxSat slider label
			c.gridx = 1;
			c.gridwidth = 1;
			c.weightx = 0;
			c.insets = new Insets(5, 0, 0, 0);
			label4 = new Label("       ", Label.LEFT);
			label4.setFont(font);
			add(label4, c);

			//=====
			c.gridx = 0;
			c.gridwidth = 1;
			c.gridy = y++;
			c.weightx = 0;
			c.insets = new Insets(10, 0, 0, 0);
			labelb = new Label("Brightness", Label.CENTER);
			add(labelb, c);

			c.gridx = 0;
			c.gridwidth = 1;
			c.gridy = y;
			c.fill = c.BOTH;
			c.anchor = c.CENTER;
			c.insets = new Insets(0, 5, 0, 0);
			add(bplot, c);

			// checkboxes
			panelb = new Panel();
			filterTypeB = new CheckboxGroup();
			bandPassB = new Checkbox("Pass");
			bandPassB.setCheckboxGroup(filterTypeB);
			bandPassB.addItemListener(this);
			panelb.add(bandPassB);
			bandStopB = new Checkbox("Stop");
			bandStopB.setCheckboxGroup(filterTypeB);
			bandStopB.addItemListener(this);
			panelb.add(bandStopB);
			bandPassB.setState(true);
			c.gridx = 1;
			c.gridy = y++;
			c.gridwidth = 2;
			c.insets = new Insets(5, 0, 0, 0);
			add(panelb, c);

			// minBri slider
			minSlider3 = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, sliderRange);
			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 1;
			c.weightx = IJ.isMacintosh()?90:100;
			c.fill = c.HORIZONTAL;
			c.insets = new Insets(5, 5, 0, 0);
			add(minSlider3, c);
			minSlider3.addAdjustmentListener(this);
			minSlider3.setUnitIncrement(1);

			// minBri slider label
			c.gridx = 1;
			c.gridwidth = 1;
			c.weightx = IJ.isMacintosh()?10:0;
			c.insets = new Insets(5, 0, 0, 0);
			label5 = new Label("       ", Label.LEFT);
			label5.setFont(font);
			add(label5, c);

			// maxBri slider
			maxSlider3 = new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, 0, sliderRange);
			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 1;
			c.weightx = 100;
			c.insets = new Insets(5, 5, 0, 0);
			add(maxSlider3, c);
			maxSlider3.addAdjustmentListener(this);
			maxSlider3.setUnitIncrement(1);

			// maxBri slider label
			c.gridx = 1;
			c.gridwidth = 1;
			c.weightx = 0;
			c.insets = new Insets(5, 0, 0, 0);
			label6 = new Label("       ", Label.LEFT);
			label6.setFont(font);
			add(label6, c);

			//=====
			panelt = new Panel();
			threshold = new Checkbox("Threshold");
			threshold.addItemListener(this);
			panelt.add(threshold);

			invert = new Checkbox("Invert");
			invert.addItemListener(this);
			panelt.add(invert);

			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 2;
			c.insets = new Insets(0, 0, 0, 0);
			add(panelt, c);

			// buttons
			panel = new Panel();
			//panel.setLayout(new GridLayout(2, 2, 0, 0));
			originalB = new Button("Original");
			originalB.setEnabled(false);
			originalB.addActionListener(this);
			originalB.addKeyListener(ij);
			panel.add(originalB);

			filteredB = new Button("Filtered");
			filteredB.setEnabled(false);
			filteredB.addActionListener(this);
			filteredB.addKeyListener(ij);
			panel.add(filteredB);

			stackB = new Button("Stack");
			stackB.addActionListener(this);
			stackB.addKeyListener(ij);
			panel.add(stackB);


			helpB = new Button("Help");
			helpB.addActionListener(this);
			helpB.addKeyListener(ij);
			panel.add(helpB);

			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 2;
			c.insets = new Insets(0, 0, 0, 0);
			add(panel, c);

			panelMode = new Panel();

			sampleB = new Button("Sample");
			sampleB.addActionListener(this);
			sampleB.addKeyListener(ij);
			panelMode.add(sampleB);

			colourMode = new CheckboxGroup();
			hsb = new Checkbox("HSB");
			hsb.setCheckboxGroup(colourMode);
			hsb.addItemListener(this);
			panelMode.add(hsb);
			hsb.setState(true);
			rgb = new Checkbox("RGB");
			rgb.setCheckboxGroup(colourMode);
			rgb.addItemListener(this);
			panelMode.add(rgb);

			c.gridx = 0;
			c.gridy = y++;
			c.gridwidth = 2;
			c.insets = new Insets(0, 0, 0, 0);
			add(panelMode, c);

			addKeyListener(ij);  // ImageJ handles keyboard shortcuts
			pack();
			GUI.center(this);
			setVisible(true);

			ip = setup(imp);
			if (ip==null) {
				imp.unlock();
				IJ.beep();
				IJ.showStatus("RGB image cannot be thresholded");
				return;
			}
			thread.start();
		}

		public void run() {
			while (!done) {
				synchronized(this) {
					try {wait();}
					catch(InterruptedException e) {}
					reset(imp,ip);//GL
					apply(imp,ip);//GL
					imp.updateAndDraw();//GL
				}
			}
		}

		public synchronized void adjustmentValueChanged(AdjustmentEvent e) {

			if (!checkImage()){
				IJ.beep();
				IJ.showStatus("No Image");
				return;
			}

			if (e.getSource() == minSlider){
				adjustMinHue((int) minSlider.getValue());
			}
			else if (e.getSource() == maxSlider){
				adjustMaxHue((int) maxSlider.getValue());
			}
			else if (e.getSource() == minSlider2){
				adjustMinSat((int) minSlider2.getValue());
			}
			else if (e.getSource() == maxSlider2){
				adjustMaxSat((int) maxSlider2.getValue());
			}
			else if (e.getSource() == minSlider3){
				adjustMinBri((int) minSlider3.getValue());
			}
			else if (e.getSource() == maxSlider3){
				adjustMaxBri((int) maxSlider3.getValue());
			}
			originalB.setEnabled(true);
			updateLabels();
			updatePlot();
			notify();
		}

		//GL
		public synchronized void itemStateChanged(ItemEvent e) {
			if(e.getSource()==hsb)
				isRGB = false;
			 if(e.getSource()==rgb)
				isRGB = true;

			if (e.getSource()==hsb || e.getSource()==rgb){
				flag = true;
				originalB.setEnabled(false);
				filteredB.setEnabled(false);

				minHue=minSat=minBri=0;
				maxHue=maxSat=maxBri=255;

				bandPassH.setState(true);
				bandPassS.setState(true);
				bandPassB.setState(true);
			}
			reset(imp,ip);
			ip = setup(imp);
			apply(imp,ip);
			updateNames();
			notify();
		}


		public void focusGained(FocusEvent e){
			if (!checkImage()){
				IJ.beep();
				IJ.showStatus("No Image");
			}
		}

		public void focusLost(FocusEvent e){}

		public void actionPerformed(ActionEvent e) {
			Button b = (Button)e.getSource();
			if (b==null) return;

			boolean imageThere = checkImage();

			if (imageThere){
				if (b==originalB){
					reset(imp,ip);
					filteredB.setEnabled(true);
				} else if (b==filteredB){
					apply(imp,ip);
				} else if (b==sampleB){
					reset(imp,ip);
					sample();
					apply(imp,ip);
				} else if (b==stackB){
					applyStack();
				} else if (b==helpB){
				IJ.showMessage("Help","Threshold Colour  v1.0\n \n"+
					"Modification of Bob Dougherty's BandPass2 plugin by G.Landini to\n"+
					"threshold 24 bit RGB images based on Hue, Saturation and Brightness\n"+
					"or Red, Green and Blue components.\n \n"+
					"Pass: Band-pass filter (anything within range is displayed).\n \n"+
					"Stop: Band-reject filter (anything within range is NOT displayed).\n \n"+
					"Original: Shows the original image and updates the buffer when\n"+
					" switching to another image.\n \n"+
					"Filtered: Shows the filtered image.\n \n"+
					"Stack: Processes the rest of the slices in the stack (if any)\n"+
					" using the current settings.\n \n"+
					"Threshold: Shows the object/background in the foreground and\n"+
					" background colours selected in the ImageJ toolbar.\n \n"+
					"Invert: Swaps the fore/background colours.\n \n"+
					"Sample: (experimental) Sets the ranges of the filters based on the\n"+
					" pixel value componentd in a rectangular, user-defined, ROI.\n \n"+
					"HSB RGB: Selects HSB or RGB space and resets all the filters.\n \n"+
					"Note that the \'thresholded\' image is RGB, not 8 bit grey.");
				}
				updatePlot();
				updateLabels();
				imp.updateAndDraw();
			} else {
				IJ.beep();
				IJ.showStatus("No Image");
			}
			notify();
		}

		void sample(){
			byte[] hsSource,ssSource,bsSource;
			//ImageProcessor ip2;
			//ip2 = imp.getProcessor();
			Rectangle myroi = ip.getRoi();
			int swidth = myroi.width;
			int sheight = myroi.height;
			int sy = myroi.y;
			int sx = myroi.x;
			if (swidth==width && sheight==height){
				IJ.showMessage("Select a rectangular ROI");
				IJ.beep();
				return;
			}

			IJ.run("Select None");

			int snumPixels = swidth*sheight;

			hsSource = new byte[snumPixels];
			ssSource = new byte[snumPixels];
			bsSource = new byte[snumPixels];

			int [] pixs = new int[snumPixels];
			int [] bin = new int[256];

			int counter=0, pi=0, rangePassH = 0, rangeStopH = 0, rangePassL = 0, rangeStopL = 0, i, j;


			for (i=sy; i<sy+sheight; i++){
				for (j=sx; j<sx+swidth; j++){
					pixs[counter++] = ip.getPixel(j,i);
				}
			}

			//Get hsb or rgb from roi.
			ColorProcessor cp2 = new ColorProcessor(swidth, sheight, pixs);

			int iminhue=256, imaxhue=-1, iminsat=256, imaxsat=-1, iminbri=256, imaxbri=-1;
			int iminred=256, imaxred=-1, imingre=256, imaxgre=-1, iminblu=256, imaxblu=-1;

			if(isRGB)
				cp2.getRGB(hsSource,ssSource,bsSource);
			else
				cp2.getHSB(hsSource,ssSource,bsSource);


			for (i = 0; i < snumPixels; i++){
				bin[hsSource[i]&255]=1;
				if ((hsSource[i]&255)>imaxhue) imaxhue=(hsSource[i]&255);
				if ((hsSource[i]&255)<iminhue) iminhue=(hsSource[i]&255);
				if ((ssSource[i]&255)>imaxsat) imaxsat=(ssSource[i]&255);
				if ((ssSource[i]&255)<iminsat) iminsat=(ssSource[i]&255);
				if ((bsSource[i]&255)>imaxbri) imaxbri=(bsSource[i]&255);
				if ((bsSource[i]&255)<iminbri) iminbri=(bsSource[i]&255);
				//IJ.showMessage("h:"+minhue+"H:"+maxhue+"s:"+minsat+"S:"+maxsat+"b:"+minbri+"B:"+maxbri);
			}

			if(!isRGB){ // get pass or stop filter whichever has a narrower range
				for (i = 0; i < 256; i++){
					if (bin[i]>0){
						rangePassL = i;
						break;
					}
				}
				for (i = 255; i >= 0; i--){
					if (bin[i]>0){
						rangePassH = i;
						break;
					}
				}
				for (i = 0; i < 256; i++){
					if (bin[i]==0){
						rangeStopL = i;
						break;
					}
				}
				for (i = 255; i >= 0; i--){
					if (bin[i]==0){
						rangeStopH = i;
						break;
					}
				}
				if ((rangePassH-rangePassL)<(rangeStopH-rangeStopL)){
					bandPassH.setState(true);
					bandStopH.setState(false);
					iminhue=rangePassL;
					imaxhue=rangePassH;
				}
				else{
					bandPassH.setState(false);
					bandStopH.setState(true);
					iminhue=rangeStopL;
					imaxhue=rangeStopH;
				}
			}
			else{
				bandPassH.setState(true);
				bandStopH.setState(false);
			}

			adjustMinHue(iminhue);
			minSlider.setValue(iminhue);
			adjustMaxHue(imaxhue);
			maxSlider.setValue(imaxhue);
			adjustMinSat(iminsat);
			minSlider2.setValue(iminsat);
			adjustMaxSat(imaxsat);
			maxSlider2.setValue(imaxsat);
			adjustMinBri(iminbri);
			minSlider3.setValue(iminbri);
			adjustMaxBri(imaxbri);
			maxSlider3.setValue(imaxbri);
			originalB.setEnabled(true);
			//IJ.showStatus("done");
		}


		private boolean checkImage(){
			imp = WindowManager.getCurrentImage();
			if (imp==null) {
				IJ.beep();
				IJ.showStatus("No image");
				return false;
			}
			ip = setup(imp);
			if (ip==null)
				return false;
			return true;
		}

		ImageProcessor setup(ImagePlus imp) {

			ImageProcessor ip;
			int type = imp.getType();
			if (type!=ImagePlus.COLOR_RGB)
				return null;
			ip = imp.getProcessor();
			int id = imp.getID();
			int slice = imp.getCurrentSlice();

			if ((id!=previousImageID)|(slice!=previousSlice)|(flag) ) {
				flag = false; //if true, flags a change from HSB to RGB or viceversa
				numSlices = imp.getStackSize();
				stack = imp.getStack();
				width = stack.getWidth();
				height = stack.getHeight();
				numPixels = width*height;

				hSource = new byte[numPixels];
				sSource = new byte[numPixels];
				bSource = new byte[numPixels];

				//restore = (int[])ip.getPixelsCopy(); //This runs into trouble sometimes, so do it the long way:
				int[] temp = (int[])ip.getPixels();
				restore = new int[numPixels];
				for (int i = 0; i < numPixels; i++)restore[i] = temp[i];

				fillMask = new int[numPixels];

				//Get hsb or rgb from image.
				ColorProcessor cp = (ColorProcessor)ip;
				IJ.showStatus("Gathering data");

				if(isRGB)
					cp.getRGB(hSource,sSource,bSource);
				else
					cp.getHSB(hSource,sSource,bSource);

				IJ.showStatus("done");

				//Create a spectrum ColorModel for the Hue histogram plot.
				Color c;
				byte[] reds = new byte[256];
				byte[] greens = new byte[256];
				byte[] blues = new byte[256];
				for (int i=0; i<256; i++) {
					c = Color.getHSBColor(i/255f, 1f, 1f);

					reds[i] = (byte)c.getRed();
					greens[i] = (byte)c.getGreen();
					blues[i] = (byte)c.getBlue();
				}
				ColorModel cm = new IndexColorModel(8, 256, reds, greens, blues);

				//Make an image with just the hue from the RGB image and the spectrum LUT.
				//This is just for a hue histogram for the plot.  Do not show it.
				//ByteProcessor bpHue = new ByteProcessor(width,height,h,cm);
				ByteProcessor bpHue = new ByteProcessor(width,height,hSource,cm);
				ImagePlus impHue = new ImagePlus("Hue",bpHue);
				//impHue.show();

				ByteProcessor bpSat = new ByteProcessor(width,height,sSource,cm);
				ImagePlus impSat = new ImagePlus("Sat",bpSat);
				//impSat.show();

				ByteProcessor bpBri = new ByteProcessor(width,height,bSource,cm);
				ImagePlus impBri = new ImagePlus("Bri",bpBri);
				//impBri.show();

				plot.setHistogram(impHue, 0);
				splot.setHistogram(impSat, 1);
				bplot.setHistogram(impBri, 2);

				updateLabels();
				updatePlot();
				updateScrollBars();
				imp.updateAndDraw();
			}
		 	previousImageID = id;
		 	previousSlice = slice;
		 	return ip;
		}

		void updatePlot() {
			plot.minHue = minHue;
			plot.maxHue = maxHue;
			plot.repaint();
			splot.minHue = minSat;
			splot.maxHue = maxSat;
			splot.repaint();
			bplot.minHue = minBri;
			bplot.maxHue = maxBri;
			bplot.repaint();
		}

		void updateLabels() {
			label1.setText(""+((int)minHue));
			label2.setText(""+((int)maxHue));
			label3.setText(""+((int)minSat));
			label4.setText(""+((int)maxSat));
			label5.setText(""+((int)minBri));
			label6.setText(""+((int)maxBri));
		}

		void updateNames() {
			if (isRGB){
				labelh.setText("Red");
				labels.setText("Green");
				labelb.setText("Blue");
			}
			else{
				labelh.setText("Hue");
				labels.setText("Saturation");
				labelb.setText("Brightness");
			}
		}

		void updateScrollBars() {
			minSlider.setValue((int)minHue);
			maxSlider.setValue((int)maxHue);
			minSlider2.setValue((int)minSat);
			maxSlider2.setValue((int)maxSat);
			minSlider3.setValue((int)minBri);
			maxSlider3.setValue((int)maxBri);
		}

		void adjustMinHue(int value) {
			minHue = value;
			if (maxHue<minHue) {
				maxHue = minHue;
				maxSlider.setValue((int)maxHue);
			}
		}

		void adjustMaxHue(int value) {
			maxHue = value;
			if (minHue>maxHue) {
				minHue = maxHue;
				minSlider.setValue((int)minHue);
			}
		}

		void adjustMinSat(int value) {
			minSat = value;
			if (maxSat<minSat) {
				maxSat = minSat;
				maxSlider2.setValue((int)maxSat);
			}
		}

		void adjustMaxSat(int value) {
			maxSat = value;
			if (minSat>maxSat) {
				minSat = maxSat;
				minSlider2.setValue((int)minSat);
			}
		}

		void adjustMinBri(int value) {
			minBri = value;
			if (maxBri<minBri) {
				maxBri = minBri;
				maxSlider3.setValue((int)maxBri);
			}
		}

		void adjustMaxBri(int value) {
			maxBri = value;
			if (minBri>maxBri) {
				minBri = maxBri;
				minSlider3.setValue((int)minBri);
			}
		}

		void apply(ImagePlus imp, ImageProcessor ip) {
			//this.setCursor(wait);
			//IJ.showStatus("Bandpassing slice "+previousSlice);
			java.awt.Color col;

			if(invert.getState())
				col = Toolbar.getForegroundColor();
			else
				col = Toolbar.getBackgroundColor();

			ip.setColor(col);

			int fill = ip.BLACK;
			int keep = 0;

			if (bandPassH.getState() && bandPassS.getState() && bandPassB.getState()){ //PPP All pass
				for (int j = 0; j < numPixels; j++){
					int hue = hSource[j]&0xff;
					int sat = sSource[j]&0xff;
					int bri = bSource[j]&0xff;
					if (((hue < minHue)||(hue > maxHue)) || ((sat < minSat)||(sat > maxSat)) || ((bri < minBri)||(bri > maxBri)))
						fillMask[j] = fill;
					else
						fillMask[j] = keep;
				}
			}
			else if(!bandPassH.getState() && !bandPassS.getState() && !bandPassB.getState()){ //SSS All stop
				for (int j = 0; j < numPixels; j++){
					int hue = hSource[j]&0xff;
					int sat = sSource[j]&0xff;
					int bri = bSource[j]&0xff;
					if (((hue >= minHue)&&(hue <= maxHue)) || ((sat >= minSat)&&(sat <= maxSat)) || ((bri >= minBri)&&(bri <= maxBri)))
						fillMask[j] = fill;
					else
						fillMask[j] = keep;
				}
			}
			else if(bandPassH.getState() && bandPassS.getState() && !bandPassB.getState()){ //PPS
				for (int j = 0; j < numPixels; j++){
					int hue = hSource[j]&0xff;
					int sat = sSource[j]&0xff;
					int bri = bSource[j]&0xff;
					if (((hue < minHue)||(hue > maxHue)) || ((sat < minSat)||(sat > maxSat)) || ((bri >= minBri) && (bri <= maxBri)))
						fillMask[j] = fill;
					else
						fillMask[j] = keep;
				}
			}
			else if(!bandPassH.getState() && !bandPassS.getState() && bandPassB.getState()){ //SSP
				for (int j = 0; j < numPixels; j++){
					int hue = hSource[j]&0xff;
					int sat = sSource[j]&0xff;
					int bri = bSource[j]&0xff;
					if (((hue >= minHue) && (hue <= maxHue)) || ((sat >= minSat) && (sat <= maxSat)) || ((bri < minBri) || (bri > maxBri)))
						fillMask[j] = fill;
					else
						fillMask[j] = keep;
				}
			}
			else if (bandPassH.getState() && !bandPassS.getState() && !bandPassB.getState()){ //PSS
				for (int j = 0; j < numPixels; j++){
					int hue = hSource[j]&0xff;
					int sat = sSource[j]&0xff;
					int bri = bSource[j]&0xff;
					if (((hue < minHue) || (hue > maxHue)) || ((sat >= minSat) && (sat <= maxSat)) || ((bri >= minBri) && (bri <= maxBri)))
						fillMask[j] = fill;
					else
						fillMask[j] = keep;
				}
			}
			else if(!bandPassH.getState() && bandPassS.getState() && bandPassB.getState()){ //SPP
				for (int j = 0; j < numPixels; j++){
					int hue = hSource[j]&0xff;
					int sat = sSource[j]&0xff;
					int bri = bSource[j]&0xff;
					if (((hue >= minHue) && (hue <= maxHue))|| ((sat < minSat) || (sat > maxSat)) || ((bri < minBri) || (bri > maxBri)))
						fillMask[j] = fill;
					else
						fillMask[j] = keep;
				}
			}
			else if (!bandPassH.getState() && bandPassS.getState() && !bandPassB.getState()){ //SPS
				for (int j = 0; j < numPixels; j++){
					int hue = hSource[j]&0xff;
					int sat = sSource[j]&0xff;
					int bri = bSource[j]&0xff;
					if (((hue >= minHue)&& (hue <= maxHue)) || ((sat < minSat)||(sat > maxSat)) || ((bri >= minBri) && (bri <= maxBri)))
						fillMask[j] = fill;
					else
						fillMask[j] = keep;
				}
			}
			else if(bandPassH.getState() && !bandPassS.getState() && bandPassB.getState()){ //PSP
				for (int j = 0; j < numPixels; j++){
					int hue = hSource[j]&0xff;
					int sat = sSource[j]&0xff;
					int bri = bSource[j]&0xff;
					if (((hue < minHue) || (hue > maxHue)) || ((sat >= minSat)&&(sat <= maxSat)) || ((bri < minBri) || (bri > maxBri)))
						fillMask[j] = fill;
					else
						fillMask[j] = keep;
				}
			}

			ip.fill(fillMask);

			if (threshold.getState()){
				ip.invert();
				for (int j = 0; j < numPixels; j++){
					if(fillMask[j] == fill)
						fillMask[j] = keep;
					else
						fillMask[j] = fill;
				}
				ip.fill(fillMask);
				ip.invert();
			}
		}

		void applyStack() {
			//int minKeepH = minHue, maxKeepH = maxHue; //GL not needed?
			//int minKeepS = minSat, maxKeepS = maxSat;
			//int minKeepB = minBri, maxKeepB = maxBri;
			for (int i = 1; i <= numSlices; i++){
				imp.setSlice(i);
				if (!checkImage()){
					IJ.beep();
					IJ.showStatus("No Image");
					return;
				}
			//	minHue = minKeepH;
			//	maxHue = maxKeepH;
			//	minSat = minKeepS;
			//	maxSat = maxKeepS;
			//	minBri = minKeepB;
			//	maxBri = maxKeepB;
				apply(imp,ip);
			}
		}

		void reset(ImagePlus imp, ImageProcessor ip) {
			//Assign the pixels of ip to the data in the restore array, while
			//taking care to not give the address the restore array to the
			//image processor.
			int[] pixels = (int[])ip.getPixels();
			for (int i = 0; i < numPixels; i++) pixels[i] = restore[i];
		}

		public void windowClosing(WindowEvent e) {
			close();
		}

		public void close() {
			super.close();
			instance = null;
			done = true;
			synchronized(this) {
				notify();
			}
		}
	} // BandAdjuster class


	class BandPlot extends Canvas implements Measurements, MouseListener {

		final int WIDTH = 256, HEIGHT=80;
		double minHue = 0, minSat=0, minBri=0;
		double maxHue = 255, maxSat= 255, maxBri=255;
		int[] histogram;
		Color[] hColors;
		int hmax;
		Image os;
		Graphics osg;

		public BandPlot() {
			addMouseListener(this);
			setSize(WIDTH+1, HEIGHT+1);
		}

		void setHistogram(ImagePlus imp, int j) {
			ImageProcessor ip = imp.getProcessor();
			ImageStatistics stats = ImageStatistics.getStatistics(ip, AREA+MODE, null);
			int maxCount2 = 0;
			histogram = stats.histogram;
			for (int i = 0; i < stats.nBins; i++)
				if ((histogram[i] > maxCount2) && (i != stats.mode))
					 maxCount2 = histogram[i];
			hmax = stats.maxCount;
			if ((hmax>(maxCount2 * 1.5)) && (maxCount2 != 0)) { //GL 1.5 was 2
				hmax = (int)(maxCount2 * 1.1);//GL 1.1 was 1.5
				histogram[stats.mode] = hmax;
   	     	}
			os = null;
			ColorModel cm = ip.getColorModel();
			if (!(cm instanceof IndexColorModel))
				return;
			IndexColorModel icm = (IndexColorModel)cm;
			int mapSize = icm.getMapSize();
			if (mapSize!=256)
				return;
			byte[] r = new byte[256];
			byte[] g = new byte[256];
			byte[] b = new byte[256];
			icm.getReds(r);
			icm.getGreens(g);
			icm.getBlues(b);
			hColors = new Color[256];

			if (isRGB){
				if (j==0){
					for (int i=0; i<256; i++)
						hColors[i] = new Color(i&255, 0&255, 0&255);
					}
				else if (j==1){
					for (int i=0; i<256; i++)
						hColors[i] = new Color(0&255, i&255, 0&255);
				}
				else if (j==2){
					for (int i=0; i<256; i++)
						hColors[i] = new Color(0&255, 0&255, i&255);
				}
			}
			else{
				if (j==0){
					for (int i=0; i<256; i++)
						hColors[i] = new Color(r[i]&255, g[i]&255, b[i]&255);
				}
				else if (j==1){
					for (int i=0; i<256; i++)
						//hColors[i] = new Color(127-i/2&255, 127+i/2&255, 127-i/2&255);
						hColors[i] = new Color(192-i/4&255, 192+i/4&255, 192-i/4&255);
				}
				else if (j==2){
					for (int i=0; i<256; i++)
						hColors[i] = new Color(i&255, i&255, 0&255);
				}
			}
		}

		public void update(Graphics g) {
			paint(g);
		}

		public void paint(Graphics g ) {
			if (histogram!=null) {
				if (os==null) {
					os = createImage(WIDTH,HEIGHT);
					osg = os.getGraphics();
					osg.setColor(Color.white);
					osg.fillRect(0, 0, WIDTH, HEIGHT);
					osg.setColor(Color.gray);
					for (int i = 0; i < WIDTH; i++) {
						if (hColors!=null) osg.setColor(hColors[i]);
						osg.drawLine(i, HEIGHT, i, HEIGHT - ((int)(HEIGHT * histogram[i])/hmax)-4);
					}
					osg.dispose();
				}
				g.drawImage(os, 0, 0, this);
			} else {
				g.setColor(Color.white);
				g.fillRect(0, 0, WIDTH, HEIGHT);
			}
			g.setColor(Color.black);
			g.drawLine(0, HEIGHT -4, 256, HEIGHT-4);
			g.drawRect(0, 0, WIDTH, HEIGHT);
			g.drawRect((int)minHue, 1, (int)(maxHue-minHue), HEIGHT-5);
		}

		public void mousePressed(MouseEvent e) {}
		public void mouseReleased(MouseEvent e) {}
		public void mouseExited(MouseEvent e) {}
		public void mouseClicked(MouseEvent e) {}
		public void mouseEntered(MouseEvent e) {}
	} // BandPlot class
}







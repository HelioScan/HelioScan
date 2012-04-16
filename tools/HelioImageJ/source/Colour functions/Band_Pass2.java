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


/** This plugin isolates pixels in an RGB image or stack according to a range of Hue.
	Bob Dougherty  Some code borrowed from ThresholdAdjuster by Wayne Rasband.
	
	Version 0 5/12/2002.
	Version 1 5/13.  Filtered pixels set to foreground color.  Speed improved.
	Version 2 5/13.  Fixed a bug in setting the restore pixels that was causing problems with stacks.
					Explicitly get the foreground color from the toolbar in apply.
 */

public class Band_Pass2 implements PlugInFilter {

	ImagePlus imp;
	boolean canceled = false;
	int low = 0;
	int high = 255;
	
	public int setup(String arg, ImagePlus imp) {
		this.imp = imp;
		return DOES_RGB;
	}

	public void run(ImageProcessor ip) {
		
		BandAdjuster ba = new BandAdjuster();
 	}
	
	private void getBand() {
		GenericDialog gd = new GenericDialog("Band Selection", IJ.getInstance());
		gd.addNumericField("Lowest Hue to keep", low, 0);
		gd.addNumericField("Highest Hue to keep", high, 0);
		gd.showDialog();
		if (gd.wasCanceled()) {
			canceled = true;
		}
		low = (int)gd.getNextNumber();
		high = (int)gd.getNextNumber();
	}
	
	
/**  Selects pixels according to hue.  */
public class BandAdjuster extends PlugInFrame implements PlugIn, Measurements,
	ActionListener, AdjustmentListener, FocusListener {

	Frame instance; 
	
	HuePlot plot = new HuePlot();
	
	int sliderRange = 256;
	
	Panel panel;
	Button  resetB, applyB, setB;
	Checkbox bandPass, bandStop;
	CheckboxGroup filterType;
	int previousImageID = -1;
	int previousSlice = -1;
	ImageJ ij;
	double minHue = 0;
	double maxHue = 255;
	Scrollbar minSlider, maxSlider;
	Label label1, label2;
	boolean done;
	boolean invertedLut;
	boolean blackAndWhite;
	int lutColor = ImageProcessor.RED_LUT;
	byte[] hSource,sSource,bSource;
	int[] fillMask,restore;
	ImagePlus imp;
	ImageProcessor ip;
	
	int numSlices ;
	ImageStack stack;
	int width,height,numPixels;
	
	Cursor wait, ok;

	public BandAdjuster() {
		super("Hue Band");
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

		WindowManager.addWindow(this);
		instance = this;
		IJ.register(PasteController.class);

		ij = IJ.getInstance();
		Font font = new Font("SansSerif", Font.PLAIN, 10);
		GridBagLayout gridbag = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gridbag);
		
		wait = new Cursor(Cursor.WAIT_CURSOR);
		ok = new Cursor(Cursor.DEFAULT_CURSOR);
		
		// plot
		int y = 0;
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.fill = c.BOTH;
		c.anchor = c.CENTER;
		c.insets = new Insets(10, 10, 0, 10);
		add(plot, c);
		
		// minHue slider
		minSlider = new Scrollbar(Scrollbar.HORIZONTAL, sliderRange/3, 1, 0, sliderRange);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 1;
		c.weightx = IJ.isMacintosh()?90:100;
		c.fill = c.HORIZONTAL;
		c.insets = new Insets(5, 10, 0, 0);
		add(minSlider, c);
		minSlider.addAdjustmentListener(this);
		minSlider.setUnitIncrement(1);
		
		// minHue slider label
		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = IJ.isMacintosh()?10:0;
		c.insets = new Insets(5, 0, 0, 10);
		label1 = new Label("       ", Label.RIGHT);
    	label1.setFont(font);
		add(label1, c);
		
		// maxHue slider
		maxSlider = new Scrollbar(Scrollbar.HORIZONTAL, sliderRange*2/3, 1, 0, sliderRange);
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 1;
		c.weightx = 100;
		c.insets = new Insets(0, 10, 0, 0);
		add(maxSlider, c);
		maxSlider.addAdjustmentListener(this);
		maxSlider.setUnitIncrement(1);
		
		// maxHue slider label
		c.gridx = 1;
		c.gridwidth = 1;
		c.weightx = 0;
		c.insets = new Insets(0, 0, 0, 10);
		label2 = new Label("       ", Label.RIGHT);
    	label2.setFont(font);
		add(label2, c);
				
		// buttons
		panel = new Panel();
		//panel.setLayout(new GridLayout(2, 2, 0, 0));
		applyB = new Button("Apply");
		applyB.addActionListener(this);
		applyB.addKeyListener(ij);
		panel.add(applyB);
		
		resetB = new Button("Reset");
		resetB.addActionListener(this);
		resetB.addKeyListener(ij);
		panel.add(resetB);
		
		setB = new Button("Apply stack");
		setB.addActionListener(this);
		setB.addKeyListener(ij);
		panel.add(setB);
		
		//Checkboxes		
		filterType = new CheckboxGroup();
		
		bandPass = new Checkbox("Pass");
		bandPass.setCheckboxGroup(filterType);
		//bandPass.addItemListener(this);
		panel.add(bandPass);
		
		bandStop = new Checkbox("Stop");
		bandStop.setCheckboxGroup(filterType);
		//bandStop.addItemListener(this);
		panel.add(bandStop);
		
		bandPass.setState(true);
				
		c.gridx = 0;
		c.gridy = y++;
		c.gridwidth = 2;
		c.insets = new Insets(5, 5, 10, 5);
		add(panel, c);
		
 		addKeyListener(ij);  // ImageJ handles keyboard shortcuts
		pack();
		GUI.center(this);
		setVisible(true);

		ip = setup(imp);
		if (ip==null) {
			imp.unlock();
			IJ.beep();
			IJ.showStatus("RGB images cannot be thresolded");
			return;
		}
	}
	
	public void adjustmentValueChanged(AdjustmentEvent e) {
		if (!checkImage()){
			IJ.beep();
			IJ.showStatus("No Image");
			return;
		}
		if (e.getSource()==minSlider){
			double minValue = minSlider.getValue();
			adjustMinHue(minValue); 
		} else {
			double maxValue = maxSlider.getValue();
			adjustMaxHue(maxValue);
		}
		updateLabels();
		updatePlot();
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
			if (b==resetB){
				reset(imp,ip);
			} else if (b==applyB){
				apply(imp,ip);
			} else if (b==setB){
				applyStack();
			}
			updatePlot();
			updateLabels();
			imp.updateAndDraw();
		} else {
			IJ.beep();
			IJ.showStatus("No Image");
		}			
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
		if ((id!=previousImageID)|(slice!=previousSlice) ) {
			
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
		
			//Get hue, saturation and brightness of the RGB image.
			ColorProcessor cp = (ColorProcessor)ip;
			
			IJ.showStatus("Bandpass: gathering data");
			
			setCursor(wait);

			cp.getHSB(hSource,sSource,bSource);
			IJ.showStatus("done");
			this.setCursor(ok);
		
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
			//This is just for a histogram for the plot.  Do not show it.
			//ByteProcessor bpHue = new ByteProcessor(width,height,h,cm);
			ByteProcessor bpHue = new ByteProcessor(width,height,hSource,cm);
			ImagePlus impHue = new ImagePlus("Hue",bpHue);
			//impHue.show();
			plot.setHistogram(impHue);
			
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
	}
	
	void updateLabels() {
		label1.setText(""+((int)minHue));
		label2.setText(""+((int)maxHue));
	}

	void updateScrollBars() {
		minSlider.setValue((int)minHue);
		maxSlider.setValue((int)maxHue);
	}
	
	/** Restore image outside non-rectangular roi. */
  	/*void doMasking(ImagePlus imp, ImageProcessor ip) {
		int[] mask = imp.getMask();
		if (mask!=null)
			ip.reset(mask);
	}*/

	void adjustMinHue(double value) {
		if (IJ.altKeyDown()) {
			double width = maxHue-minHue;
			if (width<1.0) width = 1.0;
			minHue = value;
			maxHue = minHue+width;
			if ((minHue+width)>255) {
				minHue = 255-width;
				maxHue = minHue+width;
				minSlider.setValue((int)minHue);
			}
			maxSlider.setValue((int)maxHue);
			//scaleUpAndSet(ip, minHue, maxHue);
			return;
		}
		minHue = value;
		if (maxHue<minHue) {
			maxHue = minHue;
			maxSlider.setValue((int)maxHue);
		}
		//scaleUpAndSet(ip, minHue, maxHue);
	}

	void adjustMaxHue(double cvalue) {
		maxHue = cvalue;
		if (minHue>maxHue) {
			minHue = maxHue;
			minSlider.setValue((int)minHue);
		}
		//scaleUpAndSet(ip, minHue, maxHue);
	}

	void apply(ImagePlus imp, ImageProcessor ip) {
		this.setCursor(wait);
		IJ.showStatus("Bandpassing slice "+previousSlice);
		

		//Make sure the ip is up to speed on the toolbar foreground color.
		java.awt.Color col = Toolbar.getForegroundColor();
		ip.setColor(col);

		int fill = ip.BLACK;
		int keep = 0;
		
		if (bandPass.getState()){		
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				if ((hue < minHue)||(hue > maxHue)){
					fillMask[j] = fill;
				} else {
					fillMask[j] = keep;
				}			
			}
		} else { //stop band
			for (int j = 0; j < numPixels; j++){
				int hue = hSource[j]&0xff;
				if ((hue >= minHue)&&(hue <= maxHue)){
					fillMask[j] = fill;
				} else {
					fillMask[j] = keep;
				}			
			}
		}
		ip.fill(fillMask);
		this.setCursor(ok);
		IJ.showStatus("done");
	}
	void applyStack() {
		double minKeep = minHue;
		double maxKeep = maxHue;
		for (int i = 1; i <= numSlices; i++){
			imp.setSlice(i);			
			if (!checkImage()){
				IJ.beep();
				IJ.showStatus("No Image");
				return;
			}
			minHue = minKeep;
			maxHue = maxKeep;
			apply(imp,ip);		
		}
	}
	void reset(ImagePlus imp, ImageProcessor ip) {
		//Assign the pixels of ip to the data in the restore array, while
		//taking care to not give the address the restore array to the 
		//image processor.
		int[] pixels = (int[])ip.getPixels();
		for (int i = 0; i < numPixels; i++)pixels[i] = restore[i];
	}

    public void windowClosing(WindowEvent e) {
		super.windowClosing(e);
		instance = null;
		done = true;
	}

} // HueAdjuster class


class HuePlot extends Canvas implements Measurements, MouseListener {
	
	final int WIDTH = 256, HEIGHT=64;
	double minHue = 0;
	double maxHue = 255;
	int[] histogram;
	Color[] hColors;
	int hmax;
	Image os;
	Graphics osg;
	
	public HuePlot() {
		addMouseListener(this);
		setSize(WIDTH+1, HEIGHT+1);
	}

	void setHistogram(ImagePlus imp) {
		ImageProcessor ip = imp.getProcessor();
		ImageStatistics stats = ImageStatistics.getStatistics(ip, AREA+MODE, null);
		int maxCount2 = 0;
		histogram = stats.histogram;
		for (int i = 0; i < stats.nBins; i++)
		if ((histogram[i] > maxCount2) && (i != stats.mode))
			maxCount2 = histogram[i];
		hmax = stats.maxCount;
		if ((hmax>(maxCount2 * 2)) && (maxCount2 != 0)) {
			hmax = (int)(maxCount2 * 1.5);
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
		for (int i=0; i<256; i++)
			hColors[i] = new Color(r[i]&255, g[i]&255, b[i]&255);
	}

	public void update(Graphics g) {
		paint(g);
	}

	public void paint(Graphics g) {
		if (histogram!=null) {
			if (os==null) {
				os = createImage(WIDTH,HEIGHT);
				osg = os.getGraphics();
				osg.setColor(Color.white);
				osg.fillRect(0, 0, WIDTH, HEIGHT);
				osg.setColor(Color.gray);
				for (int i = 0; i < WIDTH; i++) {
					if (hColors!=null) osg.setColor(hColors[i]);
					osg.drawLine(i, HEIGHT, i, HEIGHT - ((int)(HEIGHT * histogram[i])/hmax));
				}
				osg.dispose();
			}
			g.drawImage(os, 0, 0, this);
		} else {
			g.setColor(Color.white);
			g.fillRect(0, 0, WIDTH, HEIGHT);
		}
		g.setColor(Color.black);
 		g.drawRect(0, 0, WIDTH, HEIGHT);
 		//g.setColor(Color.red);
 		g.drawRect((int)minHue, 1, (int)(maxHue-minHue), HEIGHT);
     }

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mouseClicked(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}

} // HuePlot class

} //Band_Pass2 class

	
	




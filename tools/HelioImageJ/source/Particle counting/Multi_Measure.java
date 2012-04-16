import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.awt.List;
import ij.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.measure.ResultsTable;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.plugin.filter.Analyzer;
import ij.plugin.frame.PlugInFrame;
import java.awt.datatransfer.*;

/** Bob Dougherty.  This is a copy of Wayne Rasband's ROI Manager with
one new method added: multi() performs measurements for several ROI's in a stack
and arranges the results with one line per slice.  By constast, the measure() method
produces several lines per slice.  The results from multi() may be easier to import into
a spreadsheet program for plotting or additional analysis.  This capability was
requested by Kurt M. Scudder

Version 0 6/24/2002
Version 1 6/26/2002 Updated to Wayne's version of ImageJ 1.28k
Version 2 11/24/2002 Made some changes suggested by Tony Collins
Version 3 7/20/2004  Added "Add Particles"
Version 3.1 7/21 Fixed bugs spotted by Andreas Schleifenbaum
Version 3.2 3/12/2005 Updated save method.  Requires ImageJ 1.33 for IJ.saveAs.

*/

public class Multi_Measure implements PlugIn {
	MultiMeasure mm;

	public void run(String arg) {
		if (IJ.versionLessThan("1.33"))return;
		mm = new MultiMeasure();
	}
}


/**
*/
/**
*/
class MultiMeasure extends PlugInFrame implements ActionListener, ItemListener,
				ClipboardOwner, KeyListener, Runnable {

	Panel panel;
	static Frame instance;
	java.awt.List list;
	Hashtable rois = new Hashtable();
	Checkbox coordinates, center;
	CheckboxGroup labelType;
	boolean done = false;
	Canvas previousCanvas = null;
	Thread thread;


	public MultiMeasure() {
		super("Multi Measure");
		if (instance!=null) {
			instance.toFront();
			return;
		}
		instance = this;
		setLayout(new FlowLayout(FlowLayout.CENTER,5,5));
		int rows = 25;
		list = new List(rows, true);
		list.add("012345678901234567");
		list.addItemListener(this);
		add(list);
		panel = new Panel();
		panel.setLayout(new GridLayout(16, 1, 1, 1));
		addButton("Add <SP>");
		addButton("Add+Draw<CR>");
		addButton("Add Particles");
		addButton("Delete");
		addButton("Open");
		addButton("Open All");
		addButton("Save");
		addButton("Select All");
		addButton("Measure");
		addButton("Multi");
		addButton("Draw");
		addButton("Fill");
		addButton("Copy List");

		//Checkboxes

		labelType = new CheckboxGroup();
		panel.add(new Label("Labels:"));

		center = new Checkbox("Center");
		center.setCheckboxGroup(labelType);
		panel.add(center);

		coordinates = new Checkbox("Coord.");
		coordinates.setCheckboxGroup(labelType);
		panel.add(coordinates);

		center.setState(true);

		add(panel);

		pack();
		list.delItem(0);
		GUI.center(this);
		show();
		thread = new Thread(this, "Multi_Measure");
		thread.start();
	}


	public void run() {
		while (!done) {
			try {Thread.sleep(500);}
			catch(InterruptedException e) {}
			ImagePlus imp = WindowManager.getCurrentImage();
			if (imp != null){
				ImageWindow win = imp.getWindow();
				ImageCanvas canvas = win.getCanvas();
				if (canvas != previousCanvas){
					if(previousCanvas != null)
						previousCanvas.removeKeyListener(this);
					canvas.addKeyListener(this);
					previousCanvas = canvas;
				}
			}
		}
	}

	public void windowClosing(WindowEvent e) {
		super.windowClosing(e);
		done = true;
	}



	void addButton(String label) {
		Button b = new Button(label);
		b.addActionListener(this);
		panel.add(b);
	}

	public void actionPerformed(ActionEvent e) {
		String label = e.getActionCommand();
		if (label==null)
			return;
		String command = label;
		if (command.equals("Add <SP>"))
			add();
		if (command.equals("Add+Draw<CR>"))
			addAndDraw();
		if (command.equals("Add Particles"))
			addParticles();
		else if (command.equals("Delete"))
			delete();
		else if (command.equals("Open"))
			open();
		else if (command.equals("Open All"))
			openAll();
		else if (command.equals("Save"))
			save();
		else if (command.equals("Select All"))
			selectAll();
		else if (command.equals("Measure"))
			measure();
		else if (command.equals("Multi"))
			multi();
		else if (command.equals("Draw"))
			draw();
		else if (command.equals("Fill"))
			fill();
		else if (command.equals("Copy List"))
			copyList();
	}

	public void itemStateChanged(ItemEvent e) {
		if (e.getStateChange()==ItemEvent.SELECTED
		&& WindowManager.getCurrentImage()!=null) {
			int index = 0;
            try {index = Integer.parseInt(e.getItem().toString());}
            catch (NumberFormatException ex) {}
			restore(index);
		}
	}

	boolean add() {
		ImagePlus imp = getImage();
		if (imp==null)
			return false;
		Roi roi = imp.getRoi();
		if (roi==null) {
			error("The active image does not have an ROI.");
			return false;
		}
		String type = null;
		switch (roi.getType()) {
			case Roi.RECTANGLE: type ="R"; break;
			case Roi.OVAL: type = "O"; break;
			case Roi.POLYGON: type = "PG"; break;
			case Roi.FREEROI: type = "FH"; break;
			case Roi.TRACED_ROI: type = "T"; break;
			case Roi.LINE: type = "L"; break;
			case Roi.POLYLINE: type = "PL"; break;
			case Roi.FREELINE: type = "FL"; break;
		}
		if (type==null)
			return false;
		Rectangle r = roi.getBoundingRect();
		//String label = type+" ("+(r.x+r.width/2)+","+(r.y+r.height/2)+")";

		String label = null;
		if(center.getState())
			label = type+(r.x+r.width/2)+"-"+(r.y+r.height/2);
		else
			label =  type+".x"+ (r.x)+".y"+(r.y)+".w"+(r.width)+".h"+(r.height);
		list.add(label);
		rois.put(label, roi.clone());
		return true;
	}
	boolean addParticles() {
		ImagePlus imp = getImage();
		if (imp==null)
			return false;
		ResultsTable rt = Analyzer.getResultsTable();
		if(rt == null){
			IJ.showMessage("Add Particles requres Analyze -> Analyze Particles to \n"+
							"be run first with Record Stats selected.");
			return false;
		}
		int nP = rt.getCounter();
		if(nP == 0)
			return false;
		int xCol = rt.getColumnIndex("XStart");
		int yCol = rt.getColumnIndex("YStart");
		if((xCol == ResultsTable.COLUMN_NOT_FOUND)||(yCol == ResultsTable.COLUMN_NOT_FOUND)){
			IJ.showMessage("Add Particles requres Analyze -> Analyze Particles to \n"+
							"be run first with Record Stats selected.");
			return false;
		}
		ImageProcessor ip = imp.getProcessor();
		int tMin = (int)ip.getMinThreshold();
 		for (int i = 0; i < nP; i++){
          	Wand w = new Wand(ip);
  			int xStart = (int)rt.getValue("XStart", i);
			int yStart = (int)rt.getValue("YStart", i);
      		if (tMin==ip.NO_THRESHOLD)
            	w.autoOutline(xStart, yStart);
			else
				w.autoOutline(xStart, yStart, (int)tMin, (int)ip.getMaxThreshold());
			if (w.npoints>0) {
            	Roi roi = new PolygonRoi(w.xpoints, w.ypoints, w.npoints, Roi.TRACED_ROI);
				Rectangle r = roi.getBoundingRect();

				String label = null;
				if(center.getState())
					label = "PG"+(r.x+r.width/2)+"-"+(r.y+r.height/2);
				else
					label =  "PG"+".x"+ (r.x)+".y"+(r.y)+".w"+(r.width)+".h"+(r.height);
				list.add(label);
				rois.put(label, roi.clone());
			}
		}
	return true;
	}

	void addAndDraw() {
		if (add()) {
			list.select(list.getItemCount()-1);
			draw();
		}
		IJ.run("Restore Selection");
	}

	boolean delete() {
		if (list.getItemCount()==0)
			return error("The ROI list is empty.");
		int index[] = list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI in the list must be selected.");
		for (int i=index.length-1; i>=0; i--) {
			rois.remove(list.getItem(index[i]));
			list.delItem(index[i]);
		}
		return true;
	}

	boolean restore(int index) {
		String label = list.getItem(index);
		Roi roi = (Roi)rois.get(label);
		ImagePlus imp = getImage();
		if (imp==null)
			return false;
		Rectangle r = roi.getBoundingRect();
		if (r.x+r.width>imp.getWidth() || r.y+r.height>imp.getHeight())
			return error("This ROI does not fit the current image.");
		imp.setRoi(roi);
		return true;
	}

	void open() {
		Macro.setOptions(null);
		OpenDialog od = new OpenDialog("Open ROI...", "");
		String directory = od.getDirectory();
		String name = od.getFileName();
		if (name==null)
			return;
		String path = directory + name;
		Opener o = new Opener();
		Roi roi = o.openRoi(path);
		if (roi!=null) {
			list.add(name);
			rois.put(name, roi);
		}
	}

	void openAll() {
		Macro.setOptions(null);
		Macro.setOptions(null);
		OpenDialog od = new OpenDialog("Select a file in the folder...", "");
		if (od.getFileName()==null) return;
		String dir  = od.getDirectory();
		String[] files = new File(dir).list();
		if (files==null) return;
		for (int i=0; i<files.length; i++) {
			File f = new File(dir+files[i]);
			if (!f.isDirectory() && files[i].endsWith(".roi")) {
                			Roi roi = new Opener().openRoi(dir+files[i]);
  				if (roi!=null) {
					list.add(files[i]);
					rois.put(files[i], roi);
				}
			}
		}
	}

	boolean save() {
		if (list.getItemCount()==0)
			return error("The ROI list is empty.");
		int index[] = list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI in the list must be selected.");
		String name = list.getItem(index[0]);
		Macro.setOptions(null);
		SaveDialog sd = new SaveDialog("Save ROI...", name, ".roi");
		name = sd.getFileName();
		if (name == null)
			return false;
		String dir = sd.getDirectory();
		for (int i=0; i<index.length; i++) {
			if (restore(index[i])) {
				if (index.length>1)
					name = list.getItem(index[i])+".roi";
				//IJ.run("ROI...", "path='"+dir+name+"'");
				IJ.saveAs("Selection", dir+name);
			} else
				break;
		}
		return true;
	}

	void selectAll() {
		boolean allSelected = true;
		int count = list.getItemCount();
		for (int i=0; i<count; i++) {
			if (!list.isIndexSelected(i))
				allSelected = false;
		}
		for (int i=0; i<count; i++) {
			if (allSelected)
				list.deselect(i);
			else
				list.select(i);
		}
	}

	boolean measure() {
		ImagePlus imp = getImage();
		if (imp==null)
			return false;
		int[] index = list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI must be selected from the list.");

		int setup = IJ.setupDialog(imp, 0);
		if (setup==PlugInFilter.DONE)
			return false;
		int nSlices = setup==PlugInFilter.DOES_STACKS?imp.getStackSize():1;
		int currentSlice = imp.getCurrentSlice();
		for (int slice=1; slice<=nSlices; slice++) {
			imp.setSlice(slice);
			for (int i=0; i<index.length; i++) {
				if (restore(index[i]))
					IJ.run("Measure");
				else
					break;
			}
		}
		imp.setSlice(currentSlice);
		if (index.length>1)
			IJ.run("Select None");
		return true;
	}

	boolean multi() {
		ImagePlus imp = getImage();
		if (imp==null)
			return false;
		int[] index = list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI must be selected from the list.");

		IJ.run("Clear Results");
		int setup = IJ.setupDialog(imp, 0);
		if (setup==PlugInFilter.DONE)
			return false;
		int nSlices = setup==PlugInFilter.DOES_STACKS?imp.getStackSize():1;
		int currentSlice = imp.getCurrentSlice();

		int measurements = Analyzer.getMeasurements();
		Analyzer.setMeasurements(measurements);
		Analyzer aSys = new Analyzer(); //System Analyzer
		ResultsTable rtSys = Analyzer.getResultsTable();
		ResultsTable rtMulti = new ResultsTable();
		Analyzer aMulti = new Analyzer(imp,Analyzer.getMeasurements(),rtMulti); //Private Analyzer


		for (int slice=1; slice<=nSlices; slice++) {
			imp.setSlice(slice);
			rtMulti.incrementCounter();
			int roiIndex = 0;
			for (int i=0; i<index.length; i++) {
				if (restore(index[i])){
					roiIndex++;
					Roi roi = imp.getRoi();
					ImageStatistics stats = imp.getStatistics(measurements);
					aSys.saveResults(stats,roi); //Save measurements in system results table;
					for (int j = 0; j < ResultsTable.MAX_COLUMNS; j++){
						float[] col = rtSys.getColumn(j);
						String head = rtSys.getColumnHeading(j);
						if ((head != null)&&(col != null))
							//Transfer results from system results table to private results table
							rtMulti.addValue(head+roiIndex,rtSys.getValue(j,rtSys.getCounter()-1));
					}
				}
				else
					break;
			}
			aMulti.displayResults();
			aMulti.updateHeadings();
		}

		imp.setSlice(currentSlice);
		if (index.length>1)
			IJ.run("Select None");
		return true;
	}

	boolean copyList(){
		String s="";

		if (list.getItemCount()==0)
			return error("The ROI list is empty.");
		int index[] = list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI in the list must be selected.");
		String name = list.getItem(index[0]);
		int numPad = numMeasurements() - 2;
		for (int i=0; i<index.length; i++) {
			if (restore(index[i])) {
				if (index.length>1){
					s +=  list.getItem(index[i]);
					if (i < (index.length-1) )
						s += "\t";
						for (int j = 0; j < numPad; j++)
							s += "\t";
				}

			} else
				break;
		}
		Clipboard clip = getToolkit().getSystemClipboard();
		if (clip==null) return error("System clipboard missing");
		StringSelection cont = new StringSelection(s);
		clip.setContents(cont,this);
		return true;
	}

	public void lostOwnership (Clipboard clip, Transferable cont) {}

	int numMeasurements(){
		ResultsTable rt = Analyzer.getResultsTable();
		String headings = rt.getColumnHeadings();
		int len = headings.length();
		if (len == 0) return 0;
		int count = 0;
		for (int i = 0; i < len; i++)
			if (headings.charAt(i) == '\t') count++;
		return count;
	}


	boolean fill() {
		int[] index = list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI must be selected from the list.");
		ImagePlus imp = WindowManager.getCurrentImage();
		Undo.setup(Undo.COMPOUND_FILTER, imp);
		for (int i=0; i<index.length; i++) {
			if (restore(index[i])) {
				IJ.run("Fill");
				IJ.run("Select None");
			} else
				break;
		}
		Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
		return true;
	}

	boolean draw() {
		int[] index = list.getSelectedIndexes();
		if (index.length==0)
			return error("At least one ROI must be selected from the list.");
		ImagePlus imp = WindowManager.getCurrentImage();
		Undo.setup(Undo.COMPOUND_FILTER, imp);
		for (int i=0; i<index.length; i++) {
			if (restore(index[i])) {
				IJ.run("Draw");
				IJ.run("Select None");
			} else
				break;
		}
		Undo.setup(Undo.COMPOUND_FILTER_DONE, imp);
		return true;
	}

	ImagePlus getImage() {
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp==null) {
			error("There are no images open.");
			return null;
		} else
			return imp;
	}

	boolean error(String msg) {
		new MessageDialog(this, "Multi Measure", msg);
		return false;
	}

	public void processWindowEvent(WindowEvent e) {
		super.processWindowEvent(e);
		if (e.getID()==WindowEvent.WINDOW_CLOSING) {
			instance = null;
		}
	}

	/** Returns a reference to the ROI Manager
		or null if it is not open. */
	public static MultiMeasure getInstance() {
		return (MultiMeasure)instance;
	}

	/** Returns the ROI Hashtable. */
	public Hashtable getROIs() {
		return rois;
	}

	/** Returns the ROI list. */
	public List getList() {
		return list;
	}


	public void keyPressed(KeyEvent e) {
		final int SPACE = 32;
		final int CR = 10;
		int keyCode = e.getKeyCode();
		if (keyCode == SPACE)
			add();
		else if (keyCode == CR)
			addAndDraw();
	}

	public void keyReleased (KeyEvent e) {}
	public void keyTyped (KeyEvent e) {}


}


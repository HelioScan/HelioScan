// package ij.gui;  if integrated into ImageJ proper, would likely belong to the ij.gui group
import java.awt.*;
import java.awt.event.*;
import ij.*;
import ij.gui.*;

/** Walter O'Dell PhD,  wodell@rochester.edu,   11/6/02
  * Overview of GenericDialogPlus and GenericRecallableDialog classes:
  *		these classes enable the specification of scrollbars, buttons, and
  *		the objects available in the GenericDialog class that remain active 
  *		until the dialog window is actively closed.
  *
  * Attributes:
  *		Scrollbars: enables scrollbars for integer, float and/or double values.
  *				float/double values are facilitated by maintaining a scaling factor 
  *				for each scrollbar, and an Ndigits parameter.
  *				Keyboard arrow keys adjust values of most recently mouse-activated scrollbar.
  *		Buttons: enables buttons that perform actions (besides 'exit')
  *
  *		rowOfItems(): enables placement of multiple objects in a row, 
  *				e.g. the scrollbar title, current value and the scrollbar itsself.
  *		addButtons(): enables easy specification of multiple buttons across a row
  *		getBooleanValue(int index_to_list); getNumericValue(); getButtonValue(); getScrollbarValue()
  *				enables access to the value of any individual object without having to go 
  *				through the entire list of like-objects with getNext...() functions.
  *
  * minor changes to the parent GenericDialog were needed, as described in the header 
  *	for the temporary GenericDialog2 class, namely:
	*   1. Changed 'private' attribute to 'protected' for several variables in parent 
	*			 class to facilitate use of these variables by GenericDialogPlus class
	*		2. Added variables (int) x; // GridBagConstraints height variable
	*									and  (Container) activePanel; // facilitates row of items option
	*				and code altered to initialize and update these new variables.
	* It is hoped that these modifications will be made to the parent GenericDialog class
	* in future versions of ImageJ, negating the need for the GenericDialog2 class
  **/
class GenericDialogPlus extends GenericDialog2 
					implements AdjustmentListener, KeyListener, FocusListener {
	/** Maximum number of each component (numeric field, checkbox, etc). */
	public static final int MAX_ITEMS = 20;
	protected Scrollbar[] scrollbars;
	protected double[] SBscales; 
	protected double[] SBcurValues; 
	private Label[] SBcurValueLabels; 
  protected int sbIndex, SBtotal;
  private int[] SBdigits; // Ndigits to right of decimal pt (0==integer)
  protected int x;
  protected int SBlastTouched; // last scrollbar touched; needed for arrow key usage

	// second panel(s) enables placement of multiple items on same line (row)
	private Panel twoPanel;
	private GridBagConstraints tmpc;
	private GridBagLayout tmpgrid;
	private int tmpy;
	
	public GenericDialogPlus(String title) {
		super(title);
	}

  /** Creates a new GenericDialog using the specified title and parent frame. */
  public GenericDialogPlus(String title, Frame parent) {
		super(title, parent);
	}

  /** access the value of the i'th checkbox */
  public boolean getBooleanValue(int i) {
		if (checkbox==null)
			return false;
		// else
		Checkbox cb = (Checkbox)(checkbox.elementAt(i));
		return cb.getState();
  }
  
  /** access the value of the i'th numeric field */
  public double getNumericValue(int i) {
		if (numberField==null)
			return 0;
		// else
		TextField tf = (TextField)numberField.elementAt(i);
		String theText = tf.getText();
		String originalText = (String)defaultText.elementAt(i);
		double defaultValue = ((Double)(defaultValues.elementAt(i))).doubleValue();
		double value;
		if (theText.equals(originalText))
			value = defaultValue;
		else {
			Double d = getValue(theText);
			if (d!=null)
				value = d.doubleValue();
			else {
				// invalidNumber = true;
				value = 0.0;
			}
		}
		return value;
  }

  public void beginRowOfItems() {
   	tmpc = c; tmpgrid = grid;  tmpy = y;
   	twoPanel = new Panel();
   	activePanel = twoPanel;
   	grid = new GridBagLayout();
		twoPanel.setLayout(grid);
   	c = new GridBagConstraints();
   	x = y = 0;
	}
	public void endRowOfItems() {
		activePanel = this;
		c = tmpc;  grid = tmpgrid;  y = tmpy;
   	c.gridwidth = 1;
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, 0, 0, 0);
		grid.setConstraints(twoPanel, c);
		add(twoPanel);
		x = 0;
		y++;
	}

 /** Adds adjustable scrollbar field.
	* param label	the label
	* param defaultValue	initial state
	* param digits	the number of digits to the right of the decimal place
	* param minval	the range minimum (left side value of slider)
	* param maxval	the range maximum (right side value of slider)
	*/    
  public void addScrollBar(String label, double defaultValue, int digits,
   					double minval, double maxval) {
   	// use default 100 clicks
    addScrollBar(label, defaultValue, digits, minval, maxval, 100);
  }
  public void addScrollBar(String label, double defaultValue, int digits,
   					double minval, double maxval, int maxClicks) {
   	if (sbIndex >= MAX_ITEMS) {
   		IJ.write("  cannot add another slider, have maxed out at: "+sbIndex);
   		return;
   	}
    if (scrollbars==null) {
    	scrollbars = new Scrollbar[MAX_ITEMS]; 
			SBscales = new double[MAX_ITEMS]; 
			SBcurValues = new double[MAX_ITEMS]; 
			SBcurValueLabels = new Label[MAX_ITEMS]; 
			SBdigits = new int[MAX_ITEMS];
		}    		
		// create new panel that is 3 cells wide for SBlabel, SB, SBcurVal
		Panel sbPanel = new Panel();
		GridBagLayout sbGrid = new GridBagLayout();
		GridBagConstraints sbc  = new GridBagConstraints();
		sbPanel.setLayout(sbGrid);

   	// label
		Label theLabel = new Label(label);
		sbc.insets = new Insets(5, 0, 0, 0);
		sbc.gridx = 0; sbc.gridy = 0;
		sbc.anchor = GridBagConstraints.WEST;
		sbGrid.setConstraints(theLabel, sbc);
		sbPanel.add(theLabel);
		
		// scrollbar: only works with integer values so use scaling to mimic float/double
		SBscales[sbIndex] = Math.pow(10.0, digits);
		SBcurValues[sbIndex] = defaultValue;
		int visible = (int)Math.round((maxval-minval)* SBscales[sbIndex]/10.0);
		scrollbars[sbIndex] = new Scrollbar(Scrollbar.HORIZONTAL, 
					(int)Math.round(defaultValue*SBscales[sbIndex]), 
					visible, /* 'visible' == width of bar inside slider == 
						  increment taken when click inside slider window */
					(int)Math.round(minval*SBscales[sbIndex]), 
					(int)Math.round(maxval*SBscales[sbIndex] +visible) );
					/* Note that the actual maximum value of the scroll bar is 
					the maximum minus the visible. The left side of the bubble 
					indicates the value of the scroll bar. */
		scrollbars[sbIndex].addAdjustmentListener(this);
		scrollbars[sbIndex].setUnitIncrement(Math.max(1,
				(int)Math.round((maxval-minval)*SBscales[sbIndex]/maxClicks)));
		sbc.gridx = 1;
		sbc.ipadx = 75; // set the scrollbar width (internal padding) to 75 pixels
		sbGrid.setConstraints(scrollbars[sbIndex], sbc);
		sbPanel.add(scrollbars[sbIndex]);
		sbc.ipadx = 0;  // reset
		
		// current value label
		SBdigits[sbIndex] = digits;
		SBcurValueLabels[sbIndex] = new Label(IJ.d2s(SBcurValues[sbIndex], digits));

		sbc.gridx = 2;
		sbc.insets = new Insets(5, 5, 0, 0);
		sbc.anchor = GridBagConstraints.EAST;
		sbGrid.setConstraints(SBcurValueLabels[sbIndex], sbc);
		sbPanel.add(SBcurValueLabels[sbIndex]);
		
		c.gridwidth = 2; // this panel will take up one grid in overall GUI
		c.gridx = x; c.gridy = y;
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.CENTER;
		grid.setConstraints(sbPanel, c);
		activePanel.add(sbPanel);

		sbIndex++; 
		if (activePanel == this) { x=0; y++; }
		else x++;
		SBtotal = sbIndex;
  } // end scrollbar field
  
  public void setScrollBarUnitIncrement(int inc) {
    scrollbars[sbIndex-1].setUnitIncrement(inc);
	}
	
	/** Returns the contents of the next scrollbar field. */
  public double getNextScrollBar() {
		if (scrollbars[sbIndex]==null)
			return -1.0;
		else return SBcurValues[sbIndex++];
  }
  /** Returns the contents of scrollbar field 'i' */
  public double getScrollBarValue(int i) {
		if (i<0 || i>=SBtotal || scrollbars[i]==null)
			return -1.0;
		else return SBcurValues[i];
  }
  /** Sets the contents of scrollbar field 'i' to 'value' */
  public void setScrollBarValue(int i, double value) {
		if (i<0 || i>=SBtotal || scrollbars[i]==null)  return;
		scrollbars[i].setValue((int)Math.round(value*SBscales[i]));
		SBcurValues[i] = value;
		SBcurValueLabels[i].setText(IJ.d2s(SBcurValues[i], SBdigits[i]));
  }

  public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		for (int i=0; i<SBtotal; i++) {
		  if (e.getSource()==scrollbars[i]) {
				SBcurValues[i] = scrollbars[i].getValue()/ SBscales[i];
		    setScrollBarValue(i,SBcurValues[i]);
		    SBlastTouched = i; // set keyboard input to be directed to this scrollbar
		  }
		}
		sbIndex = 0; // reset for next call to getNextScrollBar()
	}
	 
  /** Displays this dialog box. */
  public void showDialog() {
		sbIndex = 0; 
		super.showDialog();
  }
}

/** create a GenericDialog that remains active to allow for repeated 
  * runs through target program, enabling the use of add-on buttons to 
  * perform different and repeatable functions.
  * See code at bottom and WindowLevelAdjuster class for implementation examples 
 	*/
public class GenericRecallableDialog extends GenericDialogPlus
			implements AdjustmentListener, KeyListener, FocusListener {
	private Button[] buttons = new Button[MAX_ITEMS]; 
	private boolean[] buttons_touched = new boolean[MAX_ITEMS]; 
	private int butIndex, butTot;
	Thread thread;
	public final int WEST=0, CENTER=1; // location flags

	public GenericRecallableDialog(String title) {
 		super(title);
 		setModal(false);
 	}
 	
 	public GenericRecallableDialog(String title, Frame parent) {
		super(title, parent);
 		setModal(false);
  }
    
  /** changes from parent showDialog(): remove accept button */
  public void showDialog() {
		nfIndex = 0;
		sfIndex = 0;
		cbIndex = 0;
		choiceIndex = 0;
		sbIndex = 0;
		butIndex = 0;
		if (macro) {
			//IJ.write("showDialog: "+macroOptions);
			dispose();
			return;
		}
    if (stringField!=null&&numberField==null) {
    	TextField tf = (TextField)(stringField.elementAt(0));
    	tf.selectAll();
    }
		cancel = new Button(" Done "); // changed from "Cancel"
		cancel.addActionListener(this);
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(5, 0, 0, 0); // top,left,bot,right coords
		grid.setConstraints(cancel, c);
		add(cancel);
    if (IJ.isMacintosh())
      setResizable(false);
		pack();
		GUI.center(this);
		setVisible(true);
		IJ.wait(250); // work around for Sun/WinNT bug
  }
  
	/** the keyboard input (arrow keys) will be caught by whichever button 
		* has the current focus, but will affect the scrollbar last touched	*/
	public void keyPressed(KeyEvent e) {
		int keyCode = e.getKeyCode();
		IJ.setKeyDown(keyCode);
		if (scrollbars[SBlastTouched] != null) {
			// left is 37, right is 39;  numpad4 is 100, numpad6 is 102
			if (keyCode == KeyEvent.VK_LEFT || keyCode == KeyEvent.VK_NUMPAD4) {
				SBcurValues[SBlastTouched] -= 
						scrollbars[SBlastTouched].getUnitIncrement()/SBscales[SBlastTouched];
				setScrollBarValue(SBlastTouched,SBcurValues[SBlastTouched]);
			}
			if (keyCode == KeyEvent.VK_RIGHT || keyCode == KeyEvent.VK_NUMPAD6) {
				SBcurValues[SBlastTouched] += 
						scrollbars[SBlastTouched].getUnitIncrement()/SBscales[SBlastTouched];
				setScrollBarValue(SBlastTouched,SBcurValues[SBlastTouched]);
			}
		}
	}

	public void actionPerformed(ActionEvent e) {
		wasCanceled = (e.getSource()==cancel);
		nfIndex = 0; // reset these so that call to getNext...() will work
		sfIndex = 0;
		cbIndex = 0;
		sbIndex = 0; 
		choiceIndex = 0;
		for (int i=0; i<butTot; i++)
			buttons_touched[i] = (e.getSource()==buttons[i]);
		butIndex = 0;
		if (wasCanceled) { setVisible(false); dispose(); }
	}

  /** Adds a button to the dialog window */
  public void addButton(String text) {
    	addButton(text, WEST);
  }
  public void addButton(String text, int location) {
   	if (butIndex >= MAX_ITEMS) {
   		IJ.write("  cannot add another button, have maxed out at: "+butIndex);
   		return;
   	}
		buttons[butIndex] = new Button(text);
		buttons[butIndex].addActionListener(this);
		buttons[butIndex].addKeyListener(this); // for scrollbar keyboard control

		c.gridwidth = 1;
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		if (location == CENTER)
		  c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(5, 0, 5, 0);
		grid.setConstraints(buttons[butIndex], c);
		activePanel.add(buttons[butIndex]);
		buttons_touched[butIndex] = false;
		butIndex++; butTot = butIndex; y++;
  }
    
  /** adds 2(3,4) buttons in a row to the dialog window. 
  	* Easily extendable to add more buttons */
  public void addButtons(String text1, String text2) {
   	Panel butPanel = new Panel();
   	GridBagLayout butGrid = new GridBagLayout();
		butPanel.setLayout(butGrid);
   	addButtonToPanel(text1, butPanel, butGrid, 0);
   	addButtonToPanel(text2, butPanel, butGrid, 1);
   	c.gridwidth = 2;
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(5, 0, 5, 0);
		grid.setConstraints(butPanel, c);
		activePanel.add(butPanel);
		y++;
	}
  public void addButtons(String text1, String text2, String text3) {
   	Panel butPanel = new Panel();
   	GridBagLayout butGrid = new GridBagLayout();
		butPanel.setLayout(butGrid);
   	addButtonToPanel(text1, butPanel, butGrid, 0); // label, panel, row in grid
   	addButtonToPanel(text2, butPanel, butGrid, 1);
   	addButtonToPanel(text3, butPanel, butGrid, 2);
   	c.gridwidth = 2;
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(5, 0, 5, 0);
		grid.setConstraints(butPanel, c);
		activePanel.add(butPanel);
		y++;
	}
  public void addButtons(String text1, String text2, String text3, String text4){
   	Panel butPanel = new Panel();
   	GridBagLayout butGrid = new GridBagLayout();
		butPanel.setLayout(butGrid);
   	addButtonToPanel(text1, butPanel, butGrid, 0); // label, panel, row in grid
   	addButtonToPanel(text2, butPanel, butGrid, 1);
   	addButtonToPanel(text3, butPanel, butGrid, 2);
   	addButtonToPanel(text4, butPanel, butGrid, 3);
   	c.gridwidth = 1;
		c.gridx = 0; c.gridy = y;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(5, 0, 5, 0);
		grid.setConstraints(butPanel, c);
		activePanel.add(butPanel);
		y++;
	}
  public void addButtonToPanel(String text, Panel panel, 
    							 GridBagLayout grid, int row) {
    if (butIndex >= MAX_ITEMS) {
   		IJ.write("  cannot add another button, have maxed out at: "+butIndex);
   		return;
   	}
		GridBagConstraints butc  = new GridBagConstraints();
		buttons[butIndex] = new Button(text);
		buttons[butIndex].addActionListener(this);	
		buttons[butIndex].addKeyListener(this);	
		butc.gridx = row; butc.gridy = 0;
		butc.anchor = GridBagConstraints.WEST;
		butc.insets = new Insets(0, 5, 0, 10);
		grid.setConstraints(buttons[butIndex], butc);
		panel.add(buttons[butIndex]);
		buttons_touched[butIndex] = false;
		butIndex++; butTot = butIndex; 
  }
    
  /** Returns the contents of the next buttons_touched field. */
  public boolean getNextButton() {
		if (butIndex>=butTot)
			return false;
		if (buttons_touched[butIndex]) {
			buttons_touched[butIndex++] = false;
			return true;
		}
		butIndex++;
		return false; // else
  }
    
  /** Returns the contents of button 'i' field. */
  public boolean getButtonValue(int i) {
		if (i<0 || i>=butTot)
			return false;
		else if (!buttons_touched[i]) 
			return false; 
		buttons_touched[i] = false; // reset vale to false
		return true; // else
  }
}

/** example implementation for a GenericRecallableDialog
    excerpted from an InterActiveSnake program
    
 		GenericRecallableDialog gd = new GenericRecallableDialog("Snake Attributes", IJ.getInstance());
		gd.addScrollBar("Bending Energy Coeff", curEnergy_Bend, 1, 0.0, 50.0);
													// label, 				curVal,      ndigits, min, max
		gd.addScrollBar("Stretch Energy Coeff", curEnergy_Stretch, 1, 0.0, 10.0);
		gd.addScrollBar("Snake Point Sep[mm]", cur_ptSep, 0, 1.0, 15.0);
		gd.addButtons("Screen Select Mode", "Regularize Spacing");
		gd.addButtons("Save Slice", "Save All", "Redisplay", "Help");
		gd.addButton("Recompute Snake",gd.CENTER);
		gd.addCheckbox("Show Snake Point Labels",ShowLabels);
		gd.showDialog(); 
		IJ.wait(500); // give system time to initialize GUI
		while (!(gd.wasCanceled())) { // continuously runs through loop until actively canceled
		  // if this dialog window does not have the focus, don't waste CPU time on it
		  if (gd.getFocusOwner() == null) IJ.wait(500); 
		  else {
		  	// check the "Screen Select Mode" button
				if (gd.getButtonValue(0)) Toolbar.getInstance().setTool(Toolbar.CROSSHAIR);
				
				// check for new values of the scrollbars
		  	prev_ptSep = cur_ptSep;
		  	prev_Ebend = curEnergy_Bend;
		  	prev_Estretch = curEnergy_Stretch;
		  	curEnergy_Bend = (float)(gd.getScrollBarValue(0)); 
		  	curEnergy_Stretch = (float)(gd.getScrollBarValue(1));
		  	cur_ptSep = (float)(gd.getScrollBarValue(2));
		  	
		  	// if new values were found, or if the user hits either the "Regularize Spacing"
		  	//  button or the "Recompute Snake" button, then do new calculation
				if (gd.getButtonValue(1) || gd.getButtonValue(6) || prev_ptSep!=cur_ptSep
							|| prev_Ebend!=curEnergy_Bend || prev_Estretch!=curEnergy_Stretch) {
		    	UpdateAttributes(curEnergy_Bend, curEnergy_Stretch, cur_ptSep);
			  	IJ.wait(500); // give snake time to work
		  	}
		  	
		  	// check the other buttons for activity
				if (gd.getButtonValue(2)) snakeStack.Write("this");
				if (gd.getButtonValue(3)) snakeStack.Write("all");
				if (gd.getButtonValue(4)) working_canvas.update(working_graphics);
				if (gd.getButtonValue(5)) showAbout();
				
				// check the checkbox for activity -- note the order of checking does not matter
				if (gd.getBooleanValue(0)!=ShowLabels ) {
					ShowLabels = !ShowLabels;
					working_canvas.update(working_graphics);
				}
	 	 	} 
		}	 
		// exit the dialog window cleanly
		gd.setVisible(false);   
		gd.dispose();
		
 **/

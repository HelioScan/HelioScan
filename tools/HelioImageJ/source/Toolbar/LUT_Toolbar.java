import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.Math.*;
import java.awt.image.*;
import java.io.File;


public class LUT_Toolbar extends Dialog
implements ActionListener, AdjustmentListener, ItemListener, WindowListener, Runnable {
	private Thread threadProcess = null;

	public LUT_Toolbar () {
		super(new Frame(), "LUT Toolbar");
		if (IJ.versionLessThan("1.21a"))
			return;

		
		doDialog();
	}
public void run() {
		// You will never be here...
	}


	/*
	 * Build the dialog box.
	 */
	private GridBagLayout 	layout;
	private GridBagConstraints 	constraint;
	private Button 		lutRed;
	private Button 		lutGreen;
	private Button 		lutBlue;
	private Button 		lutHotRed;
	private Button 		lutHotGreen;
	private Button 		lutHotBlue;
	private Button 		lutHiLo;
private Button lutRedFire;
private Button lutGreenFire;
private Button lutBlueFire;
private Button lutClose;

	private void doDialog() {
		// Layout
		layout = new GridBagLayout();
		constraint = new GridBagConstraints();
		
		lutRed= new Button("Red");
		lutGreen= new Button("Green");
		lutBlue= new Button("Blue");
		lutHotRed= new Button("Hot Red");
		lutHotGreen= new Button("Hot Green");
		lutHotBlue= new Button("Hot Blue");

		lutRedFire= new Button("Red Fire");
		lutGreenFire= new Button("Green Fire");
		lutBlueFire= new Button("Blue Fire");

		lutHiLo = new Button("Hi-Lo");
		lutClose = new Button("Close");

		

		// Panel parameters
		Panel pnMain = new Panel();
		pnMain.setLayout(layout);


		addComponent(pnMain, 0, 0, 1, 1, 5, lutRed);
		addComponent(pnMain, 1, 0, 1, 1, 5, lutGreen);
//TjC
		addComponent(pnMain, 2, 0, 1, 1, 5, lutBlue);
		addComponent(pnMain, 3, 0, 1, 1, 5, lutHotRed);
		addComponent(pnMain, 4, 0, 1, 1, 5, lutHotGreen);
		addComponent(pnMain, 5, 0, 1, 1, 5, lutHotBlue);
	
		addComponent(pnMain, 6, 0, 1, 1, 5, lutRedFire);
		addComponent(pnMain, 7, 0, 1, 1, 5, lutGreenFire);
		addComponent(pnMain, 8, 0, 1, 1, 5, lutBlueFire);


		addComponent(pnMain, 9, 0, 1, 1, 5, lutHiLo);
		addComponent(pnMain, 10, 0, 1, 1, 5, lutClose);

		// Add Listeners
		lutRed.addActionListener(this);
		lutGreen.addActionListener(this);
		lutBlue.addActionListener(this);
		lutHotRed.addActionListener(this);
//TjC
		lutHotGreen.addActionListener(this);
		lutHotBlue.addActionListener(this);

		lutRedFire.addActionListener(this);
		lutGreenFire.addActionListener(this);
		lutBlueFire.addActionListener(this);
		lutHiLo.addActionListener(this);
		lutClose.addActionListener(this);

	

		// Build panel
		add(pnMain);
		pack();
		setResizable(false);
		//GUI.center(this);
		setVisible(true);
		IJ.wait(250); // work around for Sun/WinNT bug
	}

	final private void addComponent(
	final Panel pn,
	final int row, final int col,
	final int width, final int height,
	final int space,
	final Component comp) {
		constraint.gridx = col;
		constraint.gridy = row;
		constraint.gridwidth = width;
		constraint.gridheight = height;
		constraint.anchor = GridBagConstraints.NORTHWEST;
		constraint.insets = new Insets(space, space, space, space);
		constraint.weightx = IJ.isMacintosh()?90:100;
		constraint.fill = constraint.HORIZONTAL;
		layout.setConstraints(comp, constraint);
		pn.add(comp);
	}

	/*
	 * Implements the listeners
	 */

    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		
		notify();
    }

public synchronized void itemStateChanged(ItemEvent e) {
		}

	public synchronized  void actionPerformed(ActionEvent e) {
		    String lutDir = System.getProperty("user.dir")+File.separator+"lut"+File.separator;
      
		if (e.getSource() == lutClose) {
			dispose();
		}
	
	else if (e.getSource() == lutRed)  IJ.run("Red");
	else if (e.getSource() == lutGreen)  IJ.run("Green");
	else if (e.getSource() == lutBlue)  IJ.run("Blue");
	else if (e.getSource() == lutHotRed)  IJ.run("LUT... ", "open="+"'"+lutDir+"Red Hot.lut"+"'");
	else if (e.getSource() == lutHotGreen)  IJ.run("LUT... ", "open="+"'"+lutDir+"Green Hot.lut"+"'");
	else if (e.getSource() == lutHotBlue)  IJ.run("LUT... ", "open="+"'"+lutDir+"Blue_Hot.lut"+"'");
	else if (e.getSource() == lutRedFire)  IJ.run("Fire");
	else if (e.getSource() == lutGreenFire)  IJ.run("LUT... ", "open="+"'"+lutDir+"Green Fire.lut"+"'");
	else if (e.getSource() == lutBlueFire)  IJ.run("LUT... ", "open="+"'"+lutDir+"Green Fire blue2.lut"+"'");
	else if (e.getSource() == lutHiLo)  IJ.run("LUT... ", "open="+"'"+lutDir+"HiLo.lut"+"'");

	
		



		notify();
		
		
	}


	

	public void windowActivated(WindowEvent e) {
	}

	public void windowClosing(WindowEvent e) {
		dispose();
	}

	public void windowClosed(WindowEvent e) {
	}

	public void windowDeactivated(WindowEvent e) {
	}

	public void windowDeiconified(WindowEvent e){
	}

	public void windowIconified(WindowEvent e){
	}

	public void windowOpened(WindowEvent e){
	}
}

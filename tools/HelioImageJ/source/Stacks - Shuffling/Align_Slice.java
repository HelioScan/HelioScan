import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.Math.*;
import java.awt.image.*;


// Align_Slice  v1.0  by G.Landini 12/Feb/2004
// With thanks to Wayne Rasband for help with the no-image test.

public class Align_Slice extends Dialog
implements ActionListener, AdjustmentListener, WindowListener, Runnable {
	private Thread threadProcess = null;

	protected int par1 = 1;
	protected ImagePlus imp;
	protected ImageProcessor ip;
	protected int xe, ye, p, x, y;
	protected int c[] = new int [2]; //[xy]
	protected int r[][];


	public Align_Slice () {
		super(new Frame(), "Align Slice");
		if (IJ.versionLessThan("1.21a"))
			return;

		imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Image required.");
			return;
		}

		ip = imp.getProcessor();
		xe = ip.getWidth(); ye = ip.getHeight();
		r = new int [xe][ye];

		// get the default slice
		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				r[x][y]=ip.getPixel(x,y);
			}
		}
		doDialog();
	}

	public void run() {
		// You will never be here...
	}


	void align(){
		int sx=0, ex=0, sy=0, ey=0;

			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y, 0 );
			}
			if(c[0]>=0 && c[1]>=0) {sx=c[0]; ex= xe; sy=c[1]; ey=ye; }
			else if (c[0]<=0 && c[1]<=0){ sx=0; ex= xe+c[0]; sy=0; ey=ye+c[1]; }
			else if (c[0]<=0 && c[1]>=0){ sx=0; ex= xe+c[0]; sy=c[1]; ey=ye; }
			else if (c[0]>=0 && c[1]<=0){ sx=c[0]; ex= xe; sy=0; ey=ye+c[1]; }
			for(y=sy;y<ey;y++) {
				for(x=sx;x<ex;x++)
					ip.putPixel(x, y, r[x-c[0]][y-c[1]]);
			}
		imp.updateAndDraw();
	}

	// Build the dialog box.
	private GridBagLayout 	layout;
	private GridBagConstraints 	constraint;
	private Button 		bnClose;
	private Button 		bnHelp;
	private Button 		bnUp;
	private Button 		bnDn;
	private Button 		bnRt;
	private Button 		bnLt;
	private Button 		bnRvt;
	private TextField	txtpar1;
	private Scrollbar	scrpar1;
	private Button 		bnNextS;
	private Button 		bnPrevS;


	private void doDialog() {
		// Layout
		layout = new GridBagLayout();
		constraint = new GridBagConstraints();
		bnClose = new Button("   Close   ");
		bnHelp = new Button("Help");
		bnUp = new Button("Up");
		bnDn = new Button("Down");
		bnRt = new Button("Right");
		bnLt = new Button("Left");
		bnNextS = new Button("Next >");
		bnPrevS = new Button("< Prev");
		txtpar1 = new TextField(""+par1, 3);
		scrpar1	= new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, 101);

		// Panel parameters
		Panel pnMain = new Panel();
		pnMain.setLayout(layout);
		addComponent(pnMain, 0, 0, 1, 1, 2, new Label("  Distance"));
		addComponent(pnMain, 0, 1, 1, 1, 2, txtpar1);
		addComponent(pnMain, 0, 2, 1, 1, 2, scrpar1);
		addComponent(pnMain, 3, 1, 1, 1, 5, bnUp);
		addComponent(pnMain, 4, 0, 1, 1, 5, bnLt);
		addComponent(pnMain, 4, 2, 1, 1, 5, bnRt);
		addComponent(pnMain, 5, 1, 1, 1, 5, bnDn);
		addComponent(pnMain, 6, 1, 1, 1, 2, new Label("    ---"));
		addComponent(pnMain, 7, 0, 1, 1, 5, bnPrevS);
		addComponent(pnMain, 7, 1, 1, 1, 2, new Label("   Slice"));
		addComponent(pnMain, 7, 2, 1, 1, 5, bnNextS);
		addComponent(pnMain, 10, 2, 1, 1, 5, bnClose);
		addComponent(pnMain, 10, 0, 1, 1, 5, bnHelp);

		// Add Listeners
		bnUp.addActionListener(this);
		bnDn.addActionListener(this);
		bnRt.addActionListener(this);
		bnLt.addActionListener(this);
		bnNextS.addActionListener(this);
		bnPrevS.addActionListener(this);
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);
		scrpar1.addAdjustmentListener(this);
		scrpar1.setUnitIncrement(1);

		// Build panel
		add(pnMain);
		pack();
		setResizable(false);
		GUI.center(this);
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

	// Implement the listeners
    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getSource() == scrpar1) {
			//System.out.println("Event: " + e);
			par1=scrpar1.getValue();
			txtpar1.setText( "" + par1);
		}
		notify();
    }


	public synchronized  void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnClose) {
			dispose();
		}
		else if (e.getSource() == bnNextS){
			IJ.run("Next Slice [>]");
			imp = WindowManager.getCurrentImage();
			ip = imp.getProcessor();
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++){
					r[x][y]=ip.getPixel(x,y);
				}
			}
			c[0]=0;
			c[1]=0;
		}
		else if (e.getSource() == bnPrevS){
			IJ.run("Previous Slice [<]");
			imp = WindowManager.getCurrentImage();
			ip = imp.getProcessor();
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++){
					r[x][y]=ip.getPixel(x,y);
				}
			}
			c[0]=0;
			c[1]=0;
		}


		else if (e.getSource() == bnUp){
			c[1]-=par1; // move up
		}
		else if (e.getSource() == bnDn){
			c[1]+=par1; // move dn
		}
		else if (e.getSource() == bnRt){
			c[0]+=par1; // move right
		}
		else if (e.getSource() == bnLt){
			c[0]-=par1; // move left
		}
		else if (e.getSource() == bnHelp) {
			IJ.showMessage("Help","Align Slice  v1.0  by G.Landini\n"+
							"Manual alignment of individual slices in a stack.\n"+
							"Note: Do not use the slide bar in the stack window.\n"+
							"Instead, use the panel buttons of this plugin.");
		}
		notify();
		IJ.showStatus("R:"+c[0]+","+c[1]);
		align();
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

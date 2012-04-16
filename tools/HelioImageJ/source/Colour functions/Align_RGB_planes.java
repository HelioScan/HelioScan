import ij.*;
import ij.gui.*;
import ij.plugin.*;
import ij.process.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.Math.*;
import java.awt.image.*;

// Align RGB planes  by G.Landini
// v1.0 12/Jan/2004 released
// v1.1 12/Feb/2004 avoids error if image does not exist
// v1.2 27/May/2005 added rotation of the planes, reverting resets the plane checkboxes
// v1.3 30/May/2005 added stretching of the planes, requires 1.34o
// v1.4  9/Jun/2005 added log output based on Leon Espinosa modification
// v1.5 12/Jun/2005 fixed stretching handling

public class Align_RGB_planes extends Dialog
implements ActionListener, AdjustmentListener, ItemListener, WindowListener, Runnable {
	private Thread threadProcess = null;

	protected int par1 = 1;
	protected double par2= 0.0, par3=0.0;
	protected ImagePlus imp;
	protected ImageProcessor ip;
	protected int xe, ye, p, x, y;
	protected int f[] ={1, 1, 1}; //flag [RGB]
	protected int c[][] = new int [3][2]; //[RGB][xy]
	protected int mp[][] = new int [3][2]; //[RGB][xy]
	protected double rp[] = new double [3]; //[RGB] +-angle
	protected double sp[][] = new double [3][2]; //[RGB] [wh]
	protected double xep[] = new double [3]; //[RGB]
	protected double yep[] = new double [3]; //[RGB]

	protected int r[][];
	protected int g[][];
	protected int b[][];

	public Align_RGB_planes () {
		super(new Frame(), "Align RGB");
		if (IJ.versionLessThan("1.34o"))
			return;

		imp = WindowManager.getCurrentImage();
		if (imp == null) {
			IJ.showMessage("Image required.");
			return;
		}

		if (imp.getType() != ImagePlus.COLOR_RGB) {
			IJ.showMessage("RGB image required.");
			return;
		}

		ip = imp.getProcessor();
		xe = ip.getWidth();
		xep[0]=xep[1]=xep[2]=(double)xe;

		ye = ip.getHeight();
		yep[0]=yep[1]=yep[2]=(double)ye;
		r = new int [xe][ye];
		g = new int [xe][ye];
		b = new int [xe][ye];

		// get the 3 planes
		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				p=ip.getPixel(x,y);
				r[x][y]=((p & 0xff0000) >> 16);
				g[x][y]=((p & 0x00ff00) >> 8);
				b[x][y]=(p & 0x0000ff);
			}
		}
		doDialog();
	}

	public void run() {
		// You will never be here...
	}


	void align(){
		int sx=0, ex=0, sy=0, ey=0, plane = choiceLUT.getSelectedIndex();

		if(plane==0){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y, ((0 & 0xff) <<16)+ ((g[x][y] & 0xff) << 8) + (b[x][y] & 0xff));
			}
			if(c[0][0]>=0 && c[0][1]>=0) {sx=c[0][0]; ex= xe; sy=c[0][1]; ey=ye; }
			else if (c[0][0]<=0 && c[0][1]<=0){ sx=0; ex= xe+c[0][0]; sy=0; ey=ye+c[0][1]; }
			else if (c[0][0]<=0 && c[0][1]>=0){ sx=0; ex= xe+c[0][0]; sy=c[0][1]; ey=ye; }
			else if (c[0][0]>=0 && c[0][1]<=0){ sx=c[0][0]; ex= xe; sy=0; ey=ye+c[0][1]; }
			for(y=sy;y<ey;y++) {
				for(x=sx;x<ex;x++)
					ip.putPixel(x, y, ((r[x-c[0][0]][y-c[0][1]] & 0xff) <<16)+ ((g[x][y] & 0xff) << 8) + (b[x][y] & 0xff));
			}
		}

		else if (plane==1){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y, ((r[x][y] & 0xff) <<16)+ ((0 & 0xff) << 8) + (b[x][y] & 0xff));
			}
			if(c[1][0]>=0 && c[1][1]>=0) {sx=c[1][0]; ex= xe; sy=c[1][1]; ey=ye; }
			else if (c[1][0]<=0 && c[1][1]<=0){ sx=0; ex= xe+c[1][0]; sy=0; ey=ye+c[1][1]; }
			else if (c[1][0]<=0 && c[1][1]>=0){ sx=0; ex= xe+c[1][0]; sy=c[1][1]; ey=ye; }
			else if (c[1][0]>=0 && c[1][1]<=0){ sx=c[1][0]; ex= xe; sy=0; ey=ye+c[1][1]; }
			for(y=sy;y<ey;y++) {
				for(x=sx;x<ex;x++)
					ip.putPixel(x, y, ((r[x][y] & 0xff) <<16)+ ((g[x-c[1][0]][y-c[1][1]] & 0xff) << 8) + (b[x][y] & 0xff));
			}
		}
		else if (plane==2){
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y, ((r[x][y] & 0xff) <<16)+ ((g[x][y] & 0xff) << 8) + (0 & 0xff));
			}
			if(c[2][0]>=0 && c[2][1]>=0) {sx=c[2][0]; ex= xe; sy=c[2][1]; ey=ye; }
			else if (c[2][0]<=0 && c[2][1]<=0){ sx=0; ex= xe+c[2][0]; sy=0; ey=ye+c[2][1]; }
			else if (c[2][0]<=0 && c[2][1]>=0){ sx=0; ex= xe+c[2][0]; sy=c[2][1]; ey=ye; }
			else if (c[2][0]>=0 && c[2][1]<=0){ sx=c[2][0]; ex= xe; sy=0; ey=ye+c[2][1]; }
			for(y=sy;y<ey;y++) {
				for(x=sx;x<ex;x++){
					ip.putPixel(x, y, ((r[x][y] & 0xff) <<16)+ ((g[x][y] & 0xff) << 8) + (b[x-c[2][0]][y-c[2][1]] & 0xff));
				}
			}
		}
		imp.updateAndDraw();
	}

	void updatergb(){
		for(y=0;y<ye;y++) {
			for(x=0;x<xe;x++){
				p=ip.getPixel(x,y);
				r[x][y]=((p & 0xff0000) >> 16);
				g[x][y]=((p & 0x00ff00) >> 8);
				b[x][y]=(p & 0x0000ff);
			}
		}
	}

	/*
	 * Build the dialog box.
	 */
	private GridBagLayout 	layout;
	private GridBagConstraints 	constraint;
	private Button 		bnClose;
	private Button 		bnHelp;
	private Button 		bnUp;
	private Button 		bnDn;
	private Button 		bnRt;
	private Button 		bnLt;
	private Button 		bnRvt;
	private Button 		bnRot;
	private Button 		bnWid;
	private Button 		bnHei;
	
	private TextField	txtpar1;
	private TextField	txtpar2;
	private TextField	txtpar3;
	
	private Choice		choiceLUT;
	private Scrollbar	scrpar1;
	private Scrollbar	scrpar2;
	private Scrollbar	scrpar3;
	
	private Checkbox	chkRed;
	private Checkbox	chkGreen;
	private Checkbox	chkBlue;
	private Checkbox	chkLog;

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
		bnRvt = new Button("Revert");
		bnRot = new Button("Rotate");
		bnWid = new Button("Width");
		bnHei = new Button("Height");
		
		txtpar1 = new TextField(""+par1, 3);
		txtpar2 = new TextField(""+par2, 3);
		txtpar3 = new TextField(""+par3, 3);

		chkRed = new Checkbox("Red", null, true);
		chkGreen = new Checkbox("Green", null, true);
		chkBlue = new Checkbox("Blue", null, true);
		chkLog = new Checkbox("Log", null, false);

		choiceLUT = new Choice();
		choiceLUT.add("Red");
		choiceLUT.add("Green");
		choiceLUT.add("Blue");
		choiceLUT.select(0);
		scrpar1	= new Scrollbar(Scrollbar.HORIZONTAL, 1, 1, 1, 101);
		scrpar2	= new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, -180, 181);
		scrpar3	= new Scrollbar(Scrollbar.HORIZONTAL, 0, 1, -100, 101);
		
		// Panel parameters
		Panel pnMain = new Panel();
		pnMain.setLayout(layout);

		addComponent(pnMain, 0, 0, 1, 1, 2, new Label("Plane ---"));
		addComponent(pnMain, 0, 1, 1, 1, 3, choiceLUT);
		addComponent(pnMain, 1, 0, 1, 1, 3, new Label("  Distance"));
		addComponent(pnMain, 1, 1, 1, 1, 3, txtpar1);
		addComponent(pnMain, 1, 2, 1, 1, 3, scrpar1);
		addComponent(pnMain, 4, 1, 1, 1, 3, bnUp);
		addComponent(pnMain, 5, 0, 1, 1, 3, bnLt);
		addComponent(pnMain, 5, 2, 1, 1, 3, bnRt);
		addComponent(pnMain, 6, 1, 1, 1, 3, bnDn);
		addComponent(pnMain, 7, 0, 1, 1, 1, new Label(" --------"));
		addComponent(pnMain, 7, 1, 1, 1, 1, new Label(" --------"));
		addComponent(pnMain, 7, 2, 1, 1, 1, new Label(" --------"));

		addComponent(pnMain, 8, 0, 1, 1, 3, bnRot);
		addComponent(pnMain, 8, 1, 1, 1, 3, txtpar2);
		addComponent(pnMain, 8, 2, 1, 1, 3, scrpar2);
		
		addComponent(pnMain, 9, 0, 1, 1, 1, new Label(" --------"));
		addComponent(pnMain, 9, 1, 1, 1, 1, new Label(" --------"));
		addComponent(pnMain, 9, 2, 1, 1, 1, new Label(" --------"));
				
		addComponent(pnMain, 10, 0, 1, 1, 3, bnWid);
		addComponent(pnMain, 10, 1, 1, 1, 3, txtpar3);
		addComponent(pnMain, 10, 2, 1, 1, 3, scrpar3);
		addComponent(pnMain, 11, 0, 1, 1, 3, bnHei);
		
		addComponent(pnMain, 12, 0, 1, 1, 1, new Label(" --------"));
		addComponent(pnMain, 12, 1, 1, 1, 1, new Label(" --------"));
		addComponent(pnMain, 12, 2, 1, 1, 1, new Label(" --------"));
				
		addComponent(pnMain, 13, 0, 1, 1, 1, chkRed);
		addComponent(pnMain, 13, 2, 1, 1, 1, chkLog);
		addComponent(pnMain, 14, 0, 1, 1, 1, chkGreen);
		addComponent(pnMain, 14, 1, 1, 1, 1, bnRvt);
		addComponent(pnMain, 15, 0, 1, 1, 1, chkBlue);
		addComponent(pnMain, 16, 2, 1, 1, 4, bnClose);
		addComponent(pnMain, 16, 0, 1, 1, 4, bnHelp);

		// Add Listeners
		bnUp.addActionListener(this);
		bnDn.addActionListener(this);
		bnRt.addActionListener(this);
		bnLt.addActionListener(this);
				
		bnClose.addActionListener(this);
		bnHelp.addActionListener(this);
		scrpar1.addAdjustmentListener(this);
		scrpar1.setUnitIncrement(1);
		txtpar2.addActionListener(this);
		scrpar2.addAdjustmentListener(this);
		scrpar2.setUnitIncrement(1);
		
		txtpar3.addActionListener(this);
		scrpar3.addAdjustmentListener(this);
		scrpar3.setUnitIncrement(1);
		
		choiceLUT.addItemListener(this);
		chkRed.addItemListener(this);
		chkGreen.addItemListener(this);
		chkBlue.addItemListener(this);
		chkLog.addItemListener(this);
		bnRvt.addActionListener(this);
		bnRot.addActionListener(this);
		bnWid.addActionListener(this);
		bnHei.addActionListener(this);

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

	/*
	 * Implements the listeners
	 */
    public synchronized void adjustmentValueChanged(AdjustmentEvent e) {
		if (e.getSource() == scrpar1) {
			//System.out.println("Event: " + e);
			par1=scrpar1.getValue();
			txtpar1.setText( "" + par1);
		}
		else if (e.getSource() == scrpar2){
			par2=scrpar2.getValue();
			txtpar2.setText( "" + par2);
		}
		else if (e.getSource() == scrpar3){
			par3=scrpar3.getValue();
			txtpar3.setText( "" + par3);
		}
		notify();
    }


	public synchronized  void actionPerformed(ActionEvent e) {
		if (e.getSource() == bnClose) {
			dispose();
		}
		else if (e.getSource() == bnUp){
			if(f[0]+f[1]+f[2]<3){
				IJ.error("Swtich channels ON!");
				return;
			}
			c[choiceLUT.getSelectedIndex()][1]-=par1; // move up
			mp[choiceLUT.getSelectedIndex()][1]-=par1;
		}
		else if (e.getSource() == bnDn){
			if(f[0]+f[1]+f[2]<3){
				IJ.error("Switch all channels ON!");
				return;
			}
			c[choiceLUT.getSelectedIndex()][1]+=par1; // move dn
			mp[choiceLUT.getSelectedIndex()][1]+=par1;
		}
		else if (e.getSource() == bnRt){
			if(f[0]+f[1]+f[2]<3){
				IJ.error("Switch all channels ON!");
				return;
			}
			c[choiceLUT.getSelectedIndex()][0]+=par1; // move right
			mp[choiceLUT.getSelectedIndex()][0]+=par1;
		}
		else if (e.getSource() == bnLt){
			if(f[0]+f[1]+f[2]<3){
				IJ.error("Switch all channels ON!");
				return;
			}
			c[choiceLUT.getSelectedIndex()][0]-=par1; // move left
			mp[choiceLUT.getSelectedIndex()][0]-=par1;
		}
		else if (e.getSource() == bnRvt){
			IJ.run("Revert");
			imp = WindowManager.getCurrentImage();
			ip = imp.getProcessor();
			// get the 3 planes
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++){
					p=ip.getPixel(x,y);
					r[x][y]=((p & 0xff0000) >> 16);
					g[x][y]=((p & 0x00ff00) >> 8);
					b[x][y]=(p & 0x0000ff);
				}
			}
			c[0][0]=c[0][1]=c[1][0]=0;
			c[1][1]=c[2][0]=c[2][1]=0;
			mp[0][0]=mp[0][1]=mp[1][0]=0;
			mp[1][1]=mp[2][0]=mp[2][1]=0;
			rp[0]=rp[1]=rp[2]=0;
			sp[0][0]=sp[0][1]=sp[1][0]=0;
			sp[1][1]=sp[2][0]=sp[2][1]=0;
			xep[0]=xep[1]=xep[2]=(double)xe;
			yep[0]=yep[1]=yep[2]=(double)ye;

			chkRed.setState(true);
			chkGreen.setState(true);
			chkBlue.setState(true);

			f[0] = 1;
			f[1] = 1;
			f[2] = 1;
		}
		else if (e.getSource() == txtpar2){
			par2= Double.parseDouble(txtpar2.getText().trim());
			//IJ.log("txtpar2 has changed");
			}
		else if (e.getSource() == bnRot){
			//rotate plane
			if(f[0]+f[1]+f[2]<3){
				IJ.error("Switch all channels ON!");
				return;
			}
			IJ.run("RGB Stack");
			x=choiceLUT.getSelectedIndex();
			rp[x]+=par2;
			if (rp[x]>360)
				rp[x]-=360;
			else if (rp[x]<-360)
				rp[x]+=360;

			IJ.run("Previous Slice [<]");//just in case
			IJ.run("Previous Slice [<]");
			IJ.run("Previous Slice [<]");
			for (y=0; y<x; y++)
				IJ.run("Next Slice [>]");
			IJ.run("Arbitrarily...", "slice angle="+par2+" interpolate fill");
			IJ.run("RGB Color");
			// get the 3 planes again
			imp = WindowManager.getCurrentImage();
			ip = imp.getProcessor();
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++){
					p=ip.getPixel(x,y);
					r[x][y]=((p & 0xff0000) >> 16);
					g[x][y]=((p & 0x00ff00) >> 8);
					b[x][y]=(p & 0x0000ff);
				}
			}
			c[0][0]=0;
			c[0][1]=0;
			c[1][0]=0;
			c[1][1]=0;
			c[2][0]=0;
			c[2][1]=0;
		}
		else if (e.getSource() == txtpar3){
			par3= Double.parseDouble(txtpar3.getText().trim());
			//IJ.log("txtpar3 has changed");
			}
		else if (e.getSource() == bnWid){
			//IJ.log("Width button pressed");
			if(f[0]+f[1]+f[2]<3){
				IJ.error("Switch all channels ON!");
				return;
			}
			IJ.run("RGB Stack");
			x=choiceLUT.getSelectedIndex();
			sp[x][0]+=par3;
			IJ.run("Previous Slice [<]");//just in case
			IJ.run("Previous Slice [<]");
			IJ.run("Previous Slice [<]");
			for (y=0; y<x; y++)
				IJ.run("Next Slice [>]");
			IJ.run("Scale...", "x="+((xep[x]+par3)/xep[x])+" y=1 interpolate fill title=Untitled");
			IJ.run("RGB Color");
			xep[x]+=par3;
			// get the 3 planes again
			imp = WindowManager.getCurrentImage();
			ip = imp.getProcessor();
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++){
					p=ip.getPixel(x,y);
					r[x][y]=((p & 0xff0000) >> 16);
					g[x][y]=((p & 0x00ff00) >> 8);
					b[x][y]=(p & 0x0000ff);
				}
			}
			c[0][0]=0;
			c[0][1]=0;
			c[1][0]=0;
			c[1][1]=0;
			c[2][0]=0;
			c[2][1]=0;
		}
		else if (e.getSource() == bnHei){
			//IJ.log("Height button pressed");
			if(f[0]+f[1]+f[2]<3){
				IJ.error("Switch all channels ON!");
				return;
			}
			IJ.run("RGB Stack");
			x=choiceLUT.getSelectedIndex();
			sp[x][1]+=par3;
			IJ.run("Previous Slice [<]");//just in case
			IJ.run("Previous Slice [<]");
			IJ.run("Previous Slice [<]");
			for (y=0; y<x; y++)
				IJ.run("Next Slice [>]");

			IJ.run("Scale...", "x=1 y="+((yep[x]+par3)/yep[x])+" interpolate fill title=Untitled");
			IJ.run("RGB Color");
			yep[x]+=par3;
			// get the 3 planes again
			imp = WindowManager.getCurrentImage();
			ip = imp.getProcessor();
			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++){
					p=ip.getPixel(x,y);
					r[x][y]=((p & 0xff0000) >> 16);
					g[x][y]=((p & 0x00ff00) >> 8);
					b[x][y]=(p & 0x0000ff);
				}
			}
			c[0][0]=0;
			c[0][1]=0;
			c[1][0]=0;
			c[1][1]=0;
			c[2][0]=0;
			c[2][1]=0;
		}
		else if (e.getSource() == bnHelp) {
			IJ.showMessage("Help","Align RGB planes v1.5 by G.Landini\n"+
							"Changes the alignment of the RGB planes independently.\n \n"+
							"\'Red\' \'Green\' and \'Blue\' checkboxes switch ON and OFF the\n"+
							"planes and undo the alignment since last plane change.\n \n"+
							"Note that when switching planes, the portion of the previously\n"+
							"edited plane left outside the image frame is lost.\n"+
							"Rotation, Width and Height changes are interpolated (so there is\n"+
							"some loss of sharpness) and do not retain the image portions\n"+
							"outside the image frame.  You can use the \'Resize2Rotate\' macro\n"+
							"to avoid loosing any image data.\n \n"+
							"The \'Rotate\', \'Width\' and \'Height\' sliders set integer values,\n"+
							"but fractional values can also be typed in the entry boxes.\n"+
							"Just make sure you press [RETURN] after the number is typed." );
		}
		notify();
		//IJ.showStatus("R:"+c[0][0]+","+c[0][1]+"  G:"+c[1][0]+","+c[1][1]+"  B:"+c[2][0]+","+c[2][1]);
		align();
		if(chkLog.getState()){
			IJ.log(
				"Red Plane ---"+(mp[0][0]>0?"\n   [right]: "+mp[0][0]:mp[0][0]<0?"\n   [left]: "+Math.abs(mp[0][0]):"")+
							(mp[0][1]>0? "\n   [down]: "+mp[0][1]:mp[0][1]<0?"\n   [up]: "+Math.abs(mp[0][1]):"")+
							(rp[0]!=0?"\n   [rotate]: "+rp[0]:"")+
							(sp[0][0]!=0?"\n   [width]: "+sp[0][0]:"")+
							(sp[0][1]!=0?"\n   [height]: "+sp[0][1]:"")+
				"\nGreen Plane ---"+(mp[1][0]>0?"\n   [right]: "+mp[1][0]:mp[1][0]<0?"\n   [left]: "+Math.abs(mp[1][0]):"")+
							(mp[1][1]>0? "\n   [down]: "+mp[1][1]:mp[1][1]<0?"\n   [up]: "+Math.abs(mp[1][1]):"")+
							(rp[1]!=0?"\n   [rotate]: "+rp[1]:"")+
							(sp[1][0]!=0?"\n   [width]: "+sp[1][0]:"")+
							(sp[1][1]!=0?"\n   [height]: "+sp[1][1]:"")+
				"\nBlue Plane ---"+(mp[2][0]>0?"\n   [right]: "+mp[2][0]:mp[2][0]<0?"\n   [left]: "+Math.abs(mp[2][0]):"")+
							(mp[2][1]>0? "\n   [down]: "+mp[2][1]:mp[2][1]<0?"\n   [up]: "+Math.abs(mp[2][1]):"")+
							(rp[2]!=0?"\n   [rotate]: "+rp[2]:"")+
							(sp[2][0]!=0?"\n   [width]: "+sp[2][0]:"")+
							(sp[2][1]!=0?"\n   [height]: "+sp[2][1]:"")+"\n---------------");
		}
	}


	public synchronized void itemStateChanged(ItemEvent e) {
		if(e.getSource() == chkLog)
			IJ.beep();
		else{
			if (e.getSource() == choiceLUT) {
				updatergb(); // update planes (out of frame image data is lost!)
			}
			else if (e.getSource() == chkRed)
				f[0] = chkRed.getState()?1:0;

			else if (e.getSource() == chkGreen)
				f[1] = chkGreen.getState()?1:0;

			else if (e.getSource() == chkBlue)
				f[2] = chkBlue.getState()?1:0;

			for(y=0;y<ye;y++) {
				for(x=0;x<xe;x++)
					ip.putPixel(x,y, ((f[0] * r[x][y] & 0xff)<<16 ) + ((f[1] * g[x][y] & 0xff) << 8) + (f[2] * b[x][y] & 0xff));
			}
			c[0][0]=0;
			c[0][1]=0;
			c[1][0]=0;
			c[1][1]=0;
			c[2][0]=0;
			c[2][1]=0;
			imp.updateAndDraw();
		}
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

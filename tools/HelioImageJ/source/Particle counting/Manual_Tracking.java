import java.awt.*;
import java.awt.event.*; 
import java.awt.SystemColor;
import java.io.*;
import java.lang.*;
import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.plugin.Converter;
import ij.plugin.frame.*;
import ij.plugin.filter.*;
import ij.plugin.filter.Duplicater;
import ij.process.*;
import ij.process.StackConverter;
import ij.util.*;
import ij.util.Tools.*;




public class Manual_Tracking extends PlugInFrame implements ActionListener, ItemListener, MouseListener {
			
		double calxy=0.166;
		double calt=2;
		double dotsize=5;
		double linewidth=1;
		
		static Frame instance;
			
		Font bold = new Font("",3,12);
		
		Panel panel;
		TextField caltfield;
		Choice choicecalt;
		TextField calxyfield;
		Choice choicecalxy;
		TextField dotsizefield;
		TextField linewidthfield;
		
		String imgtitle;
		
		Button butAdd;
		Button butDlp;
		Button butEnd;
		Button butDel;
		Button butDelAll;
		Choice trackdel;
		Checkbox checkPath;
		Button butOvd;
		Button butOvl;
		Button butOvdl;
		Button butOverdots;
		Button butOverlines;
		Button butOverboth;
		
		int[] xRoi;
		int[] yRoi;
		Roi roi;
		int ox;
		int oy;
		int prevx;
		int prevy;
		double Distance;
		double Velocity;
		int PixVal;
		boolean islistening=false;
		
		ImagePlus img;
		ImageCanvas canvas;
		ImagePlus imgRGB;
					
		int Nbtrack=1;
		int i=1;
		ResultsTable rt;
		
		ImageStack stack;
		ImagePlus ov;
		
		Color[] col={Color.blue,Color.green,Color.red,Color.cyan,Color.magenta,Color.yellow,Color.white};

		
	public Manual_Tracking() {
				
	//Interface setup ----------------------------------------------------------------------------------------------------------------------------
		super ("Tracking");
		instance=this;
		panel = new Panel();
		panel.setLayout (new GridLayout (0,3));
		panel.setBackground (SystemColor.control);
				
		//1st title
		panel.add (new Label ());
		Label title=new Label ();
		title.setText ("Tracking Parameters:");
		title.setFont(bold);
		panel.add(title);
		panel.add (new Label ());
		
		//2nd row
		panel.add (new Label ("Time Interval :"));
		caltfield = new TextField (Double.toString(calt));
		panel.add(caltfield);
		choicecalt = new Choice();
		choicecalt.add("sec");
		choicecalt.add("min");
		choicecalt.add("unit");
		panel.add (choicecalt);
									
		//3rd row
		panel.add (new Label ("Distance calibration :"));
		calxyfield = new TextField (Double.toString(calxy));
		panel.add(calxyfield);
		choicecalxy = new Choice();
		choicecalxy.add("nm");
		choicecalxy.add("µm");
		choicecalxy.add("unit");
		choicecalxy.select("µm");
		panel.add (choicecalxy);
		
		//4th row
		butAdd = new Button("Add track");
		butAdd.addActionListener (this);
		panel.add(butAdd);
		
		butDlp = new Button("Delete last point");
		butDlp.addActionListener (this);
		panel.add(butDlp);
			
		butEnd = new Button("End track");
		butEnd.addActionListener (this);
		panel.add(butEnd);
				
		//5th row
		butDel = new Button("Delete track n°");
		butDel.addActionListener (this);
		panel.add(butDel);
		trackdel = new Choice();
		panel.add (trackdel);
		
		butDelAll = new Button("Delete all tracks");
		butDelAll.addActionListener (this);
		panel.add(butDelAll);
					
		//6th row
		
		panel.add (new Label ());
		checkPath=new Checkbox("Show path ?", true);
		checkPath.addItemListener(this);
		panel.add(checkPath);
		panel.add (new Label ());
		
		//Empty row
		panel.add (new Label ());
		panel.add (new Label ());
		panel.add (new Label ());
		
		//2nd title
		panel.add (new Label ());
		Label title2=new Label();
		title2.setText ("Drawing Parameters:");
		title2.setFont(bold);
		panel.add(title2);
		panel.add (new Label ());
		
		//9th row
		panel.add (new Label ("Dot Size :"));
		dotsizefield = new TextField (Double.toString(dotsize));
		panel.add(dotsizefield);
		panel.add (new Label ());
		
		//10th row
		panel.add (new Label ("Line width :"));
		linewidthfield = new TextField (Double.toString(linewidth));
		panel.add(linewidthfield);
		panel.add (new Label ());
				
		
		
		//11th row
		butOvd = new Button("Dots");
		butOvd.addActionListener (this);
		panel.add(butOvd);
		butOvl = new Button("Progressive Lines");
		butOvl.addActionListener (this);
		panel.add(butOvl);
		butOvdl = new Button("Dots & Lines");
		butOvdl.addActionListener (this);
		panel.add(butOvdl);
		
		//12th row
		butOverdots = new Button("Overlay Dots");
		butOverdots.addActionListener (this);
		panel.add (butOverdots);
		butOverlines = new Button("Overlay Lines");
		butOverlines.addActionListener (this);
		panel.add (butOverlines);
		butOverboth = new Button("Overlay Dots & Lines");
		butOverboth.addActionListener (this);
		panel.add (butOverboth);
		
		add(panel,BorderLayout.CENTER);
		pack();
		show();
		IJ.showProgress(0);
		rt=new ResultsTable();
		
	}
	
	// Show/Hide the current path-----------------------------------------------------------------------------------------------------------------
	
	public void itemStateChanged(ItemEvent e) {
	    if (e.getStateChange() == ItemEvent.SELECTED) {
	        img.setRoi(roi);
	    } else {
	        img.killRoi();
	    }
	}

	
	
	public void actionPerformed(ActionEvent e) {
		
		// Button Add Track pressed---------------------------------------------------------------------------------------------------------------
		if (e.getSource() == butAdd) {
			img=WindowManager.getCurrentImage();
			
			xRoi=new int[img.getStackSize()];
			yRoi=new int[img.getStackSize()];
		
			if (img==null){
				IJ.showMessage("Error", "Man,\n"+"You're in deep troubles:\n"+"no opened stack...");
				return;
			}
			imgtitle = img.getTitle();
			ImageWindow win = img.getWindow();
			canvas=win.getCanvas();
			img.setSlice(1);
			calt=Tools.parseDouble(caltfield.getText());
			calxy=Tools.parseDouble(calxyfield.getText());
			if (calt==0 || calxy==0) {
				IJ.showMessage("Error", "Calibration values\n"+"should not be equal to zero !!!");
				return;
			}
			i=1;
			IJ.showProgress(0);
			canvas.addMouseListener(this);
			islistening=true;
			return;
		}
		
		// Button Delete last point pressed---------------------------------------------------------------------------------------------------------------
		if (e.getSource() == butDlp) {
			GenericDialog gd = new GenericDialog("Delete last point");
			gd.addMessage ("Are you sure you want to \n" + "delete last point ?");
			gd.showDialog();
			if (gd.wasCanceled()) return;
			
			//Create a temporary ResultTable and copy only the non deleted data
			ResultsTable rtmp=new ResultsTable();
			for (int j=0; j<(rt.getCounter()); j++) {
				rtmp.incrementCounter();
				for (int k=0; k<7; k++) rtmp.addValue(k, rt.getValue(k,j));
				}
						
			rt.reset();
			
			//Copy data back to original table except last point
			
			String[] head={"Track n°","Slice n°","X","Y","Distance","Velocity","Pixel Value"};
			for (int j=0; j<head.length; j++) rt.setHeading(j,head[j]);
					
			for (int j=0; j<((rtmp.getCounter())-1); j++) {
				rt.incrementCounter();
				for (int k=0; k<7; k++) rt.addValue(k, rtmp.getValue(k,j));
			}
			rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
			
			//Manage case where the deleted point is the last of a serie
			if (islistening==false) {
				Nbtrack--;
				trackdel.remove(""+(int) rt.getValue(0,rt.getCounter()-1));		
				canvas.addMouseListener(this);
				islistening=true;
			}
			
			prevx=(int) rt.getValue(2, rt.getCounter()-1);
			prevy=(int) rt.getValue(3, rt.getCounter()-1);
			img.setSlice (((int) rt.getValue(1, rt.getCounter()-1))+1);
			IJ.showStatus("Last Point Deleted !");		
		}
		
		// Button End Track pressed---------------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butEnd) {
			trackdel.add (""+Nbtrack);
			Nbtrack++;
			canvas.removeMouseListener(this);
			islistening=false;
			IJ.showStatus("Tracking is over");
			return;
		}
						
		// Button Del Track pressed---------------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butDel) {
			canvas.removeMouseListener(this);
			islistening=false;
			int tracktodelete= (int) Tools.parseDouble(trackdel.getItem(trackdel.getSelectedIndex()));
			GenericDialog gd = new GenericDialog("Delete Track n°" + tracktodelete);
			gd.addMessage ("Do you want to \n" + "delete track n°" + tracktodelete + " ?");
			gd.showDialog();
			if (gd.wasCanceled()) return;
			
			//Create a temporary ResultTable and copy only the non deleted data
			ResultsTable rtmp=new ResultsTable();
			for (int j=0; j<(rt.getCounter()); j++) {
				int nbtrack=(int) rt.getValue(0,j);
				if(nbtrack!=tracktodelete){
					rtmp.incrementCounter();
					for (int k=0; k<7; k++) rtmp.addValue(k, rt.getValue(k,j));
				}
			}
			
			rt.reset();
			
			//Copy data back to original table
			
			String[] head={"Track n°","Slice n°","X","Y","Distance","Velocity","Pixel Value"};
			for (int j=0; j<head.length; j++) rt.setHeading(j,head[j]);
					
			for (int j=0; j<(rtmp.getCounter()); j++) {
				rt.incrementCounter();
				for (int k=0; k<7; k++) rt.addValue(k, rtmp.getValue(k,j));
			}
			
			rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
			trackdel.remove(trackdel.getSelectedIndex());		
			IJ.showStatus("Track n°"+tracktodelete +" Deleted !");
		}
			
		// Button Del All Tracks pressed----------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butDelAll) {
			canvas.removeMouseListener(this);
			islistening=false;
			IJ.showProgress(0);
			IJ.showStatus("Tracking is over");
			GenericDialog gd = new GenericDialog("Delete All Tracks");
			gd.addMessage ("Do you want to \n" + "delete all measurements ?");
			gd.showDialog();
			if (gd.wasCanceled()) return;
			rt.reset();
			rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
			trackdel.removeAll();		
			IJ.showStatus("All Tracks Deleted !");
			Nbtrack=1;
			return;
		}
		
		// Button Dots pressed----------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butOvd) {
			img.killRoi();
			canvas.removeMouseListener(this);
			islistening=false;
			ov=NewImage.createRGBImage("Dots "+imgtitle,img.getWidth(),img.getHeight(),img.getStackSize(),256);
			ov.show();
			stack=ov.getStack();
			Dots();
			IJ.showStatus("Dots "+imgtitle+" Created !");
			return;
		}
		
		// Button Progressive Lines pressed----------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butOvl) {
			img.killRoi();
			canvas.removeMouseListener(this);
			islistening=false;
			ov=NewImage.createRGBImage("Progressive Lines "+imgtitle,img.getWidth(),img.getHeight(),img.getStackSize(),256);
			ov.show();
			stack=ov.getStack();
			ProLines();
			IJ.showStatus("Progressive Lines "+imgtitle+" Created !");
			return;
		}
		
		// Button Lines & Dots pressed----------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butOvdl) {
			img.killRoi();
			canvas.removeMouseListener(this);
			islistening=false;
			ov=NewImage.createRGBImage("Dots & Lines "+imgtitle,img.getWidth(),img.getHeight(),img.getStackSize(),256);
			ov.show();
			stack=ov.getStack();
			ProLines();
			Dots();
			IJ.showStatus("Dots & Lines "+imgtitle+" Created !");
			return;
		}
		
		// Button Overlay Dots pressed----------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butOverdots) {
			img.killRoi();
			canvas.removeMouseListener(this);
			islistening=false;
			Duplicater dp= new Duplicater();
			imgRGB=dp.duplicateStack(img, "Overlay Dots "+imgtitle);
			imgRGB.show();
			StackConverter sc=new StackConverter(imgRGB);
			sc.convertToRGB();
			stack=imgRGB.getStack();
			Dots();
			IJ.showStatus("Overlay Dots "+imgtitle+" Created !");
			return;
		}
		
		// Button Overlay Lines pressed----------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butOverlines) {
			img.killRoi();
			canvas.removeMouseListener(this);
			islistening=false;
			Duplicater dp= new Duplicater();
			imgRGB=dp.duplicateStack(img, "Overlay Lines "+imgtitle);
			imgRGB.show();
			StackConverter sc=new StackConverter(imgRGB);
			sc.convertToRGB();
			stack=imgRGB.getStack();
			ProLines();
			IJ.showStatus("Overlay Lines "+imgtitle+" Created !");
			return;
		}
		
		// Button Overlay Dots & Lines pressed----------------------------------------------------------------------------------------------------------		
		if (e.getSource() == butOverboth) {
			img.killRoi();
			canvas.removeMouseListener(this);
			islistening=false;
			Duplicater dp= new Duplicater();
			imgRGB=dp.duplicateStack(img, "Overlay Dots & Lines "+imgtitle);
			imgRGB.show();
			StackConverter sc=new StackConverter(imgRGB);
			sc.convertToRGB();
			stack=imgRGB.getStack();
			ProLines();
			Dots();
			IJ.showStatus("Overlay Dots & Lines "+imgtitle+" Created !");
			return;
		}
	}
	
		// Click on image-------------------------------------------------------------------------------------------------------------------------
	public void mouseReleased(MouseEvent m) {
		
		IJ.showProgress((double)img.getCurrentSlice()/img.getStackSize());
		IJ.showStatus("Tracking slice "+(img.getCurrentSlice())+" of "+(img.getStackSize()));
		
		if (Nbtrack==1 && i==1){
			String[] head={"Track n°","Slice n°","X","Y","Distance","Velocity","Pixel Value"};
			for (int j=0; j<head.length; j++) rt.setHeading(j,head[j]);
		}
		
		img.killRoi();						
		int x=m.getX();
		int y=m.getY();
		ox=canvas.offScreenX(x);
		oy=canvas.offScreenY(y);
		xRoi[i-1]=ox;
		yRoi[i-1]=oy;
				
		if (i==1){
			Distance=-1;
			Velocity=-1;
			} else {
				Distance=calxy*Math.sqrt(Math.pow((ox-prevx),2)+Math.pow((oy-prevy),2));
				Velocity=Distance/calt;	
		}
		
		int [] tmp=img.getPixel(ox, oy);
		PixVal=tmp[0];
		
		rt.incrementCounter();
		double[] doub={Nbtrack,(img.getCurrentSlice()),ox,oy,Distance,Velocity,PixVal};
		for (int j=0; j<doub.length; j++) rt.addValue(j,doub[j]);
		rt.show("Results from "+imgtitle+" in "+choicecalxy.getItem(choicecalxy.getSelectedIndex())+" per "+choicecalt.getItem(choicecalt.getSelectedIndex()));
			
					
		if ((img.getCurrentSlice())<img.getStackSize()){
			i++;
			img.setSlice(img.getCurrentSlice()+1);
			prevx=ox;
			prevy=oy;
			roi=new PolygonRoi(xRoi,yRoi,i-1,Roi.POLYLINE);
			if(checkPath.getState()) img.setRoi(roi);
			} else {
				trackdel.add (""+Nbtrack);
				Nbtrack++;
				img.setRoi(roi);
				canvas.removeMouseListener(this);
				islistening=false;
				//IJ.showProgress(0);
				IJ.showStatus("Tracking is over");
				return;
	}
			
	}
	
	public void mousePressed(MouseEvent m) {}
	public void mouseExited(MouseEvent m) {}
	public void mouseClicked(MouseEvent m) {}
	public void mouseEntered(MouseEvent m) {}
	
	
	void Dots(){
		
		dotsize=Tools.parseDouble(dotsizefield.getText());
		int k=0;
		int nbtrackold=1;
		for (int j=0; j<(rt.getCounter()); j++) {
			int nbtrack=(int) rt.getValue(0,j);
			int nbslices=(int) rt.getValue(1,j);
			int cx=(int) rt.getValue(2,j);
			int cy=(int) rt.getValue(3,j);
			if ((nbtrack != nbtrackold)) k++;
			if (k>6) k=0;
			ImageProcessor ip= stack.getProcessor(nbslices);
			ip.setColor (col[k]);
			ip.setLineWidth((int) dotsize);
			ip.drawDot(cx, cy);
			nbtrackold=nbtrack;
		}	
		
	}	

	void ProLines(){
		
		linewidth=Tools.parseDouble(linewidthfield.getText());	
		int k=0;
		int l=1;
		int cxold=0;
		int cyold=0;
		int nbtrackold=1;
					
		for (int j=0; j<(rt.getCounter()); j++) {
			int nbtrack=(int) rt.getValue(0,j);
			int nbslices=(int) rt.getValue(1,j);
			int cx=(int) rt.getValue(2,j);
			int cy=(int) rt.getValue(3,j);
			int lim=img.getStackSize()+1;
			if ((nbtrack != nbtrackold)) {
				k++;
				l=1;
			}
			for (int n=nbtrack; n<(rt.getCounter());n++) {
				if ((int) (rt.getValue(0,n)) == nbtrack) lim=(int) rt.getValue(1,n);
			}
			
			if (k>6) k=0;
			for (int m=nbslices; m<lim+1;m++) {
				if (l==1){
					cxold=cx;
					cyold=cy;
				}
				
				ImageProcessor ip= stack.getProcessor(m);
				ip.setColor (col[k]);
				ip.setLineWidth((int) linewidth);
				ip.drawLine(cxold, cyold, cx, cy);
				nbtrackold=nbtrack;
				l++;
			}
			cxold=cx;
			cyold=cy;
		}
	}
}

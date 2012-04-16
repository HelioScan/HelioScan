/*
 * Colocalization_Finder.java
 *
 * Created on 20/02/2004 Copyright (C) 2003 IBMP
 * ImageJ plugin
 * Version  : 1.0
 * Authors  : C. Laummonerie & J. Mutterer
 *            written for the IBMP-CNRS Strasbourg(France)
 * Email    : jerome.mutterer at ibmp-ulp.u-strasbg.fr
 * Description :  This plugin displays a correlation diagram for two 
 * images (8bits, same size). Drawing a rectangular selection 
 * on this diagram allows you to highlight corresponding pixels on a
 * RGB overlap of the original images, and if selected, on a 3rd image.
 * Analysis can be restricted to pixels having values with a minimum ratio.
 * Selection settings are logged to a results window. Large parts of this 
 * code were taken from Wayne Rasband and Pierre Bourdoncle.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */
 
import ij.*;
import ij.plugin.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.filter.PlugInFilter;
import ij.io.*;
import java.io.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.awt.image.*;
import ij.process.ImageConverter.*;
import ij.process.ImageProcessor.*;

public class Colocalization_Finder implements PlugInFilter, MouseListener, MouseMotionListener, KeyListener {

    static String title = "Colocalization_Finder";
    double r;
    boolean ratio=false,useThirdImage=false;
    int[] wList;
    private String[] titles;
    int counter; 
    int i1Index;
    int i2Index,i3Index;
    ImageCanvas canvas;
    ImagePlus i1,i2,i3,imResu,img,iDic,iDicSource;
    private ImagePlus imp;
    ImagePlus[] image = new ImagePlus[3];
    
    public int setup(String arg, ImagePlus img) {
        if (IJ.versionLessThan("1.31i"))
            return DONE;
        this.img = img;
        IJ.register(Colocalization_Finder.class);
        return DOES_ALL+NO_CHANGES;
    }
    
    public void run(ImageProcessor ip) {
        wList = WindowManager.getIDList();
        if (wList==null || wList.length<2) {
            IJ.showMessage(title, "There must be at least two windows open");
            return;
        }
        
	titles = new String[wList.length];
        for (int i=0; i<wList.length; i++) {
            ImagePlus imp = WindowManager.getImage(wList[i]);
            if (imp!=null)
                titles[i] = imp.getTitle();
            else
                titles[i] = "";
        }

        if (!showDialog()) return; 
        
	correlate(i1,i2,i3);
	ip =i3.getProcessor();
	ImageWindow win = i3.getWindow();
	canvas = win.getCanvas();
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        canvas.addKeyListener(this);
	IJ.setColumnHeadings("min I1\t max I1\t min I2\t max I2\t % colocalization");
	comparaison();

    }
    
    public boolean showDialog() {
        GenericDialog gd = new GenericDialog(title);
        gd.addChoice("Image 1 (will be shown in red) :", titles, titles[0]);
	gd.addChoice("Image 2 (will be shown in green):", titles, titles[1]);
	gd.addMessage(" ");
	gd.addCheckbox("Also show selected pixels on a 3rd grayscale image",useThirdImage);
	gd.addChoice("Image 3 (selected pixels will be shown in green) :",titles,titles[0]);
	gd.addMessage(" ");
       	gd.addCheckbox("Restrain selection to pixels within a certain ratio", ratio);
	gd.addNumericField("Specify minimum ratio (0-100%):", r, 1);
        gd.showDialog();
        if (gd.wasCanceled())
            return false;
        int i1Index = gd.getNextChoiceIndex();
        int i2Index = gd.getNextChoiceIndex();
        useThirdImage = gd.getNextBoolean();
	int i3Index = gd.getNextChoiceIndex();
        r = gd.getNextNumber();
	if (r >= 100) r = 100;
	if (r <=0) r = 0.1;
        ratio = gd.getNextBoolean();
        i1 = WindowManager.getImage(wList[i1Index]);
        i2 = WindowManager.getImage(wList[i2Index]);
	int widthD,heightD;
	int width1 = widthD = i1.getWidth();
	int width2 = i2.getWidth();
	int height1 = heightD = i1.getHeight();
	int height2 = i2.getHeight();
	iDicSource = WindowManager.getImage(wList[i3Index]);
	ImageProcessor dicIp =iDicSource.getProcessor();
	iDic = new ImagePlus("DIC superposition in Green",dicIp);
	iDic.setProcessor(null, dicIp.convertToRGB());
        iDic.setCalibration(iDic.getCalibration());
	if (useThirdImage) {
		widthD = iDic.getWidth();
		heightD = iDic.getHeight();
	}
	if (width1 != width2 || height1 != height2 || widthD!=width1 || heightD !=height1) {
		IJ.showMessage(title, "image 1 and 2 (and maybe 3) must be at the same height and width");
		return false;
	}
	if (i1.getType()!=i1.GRAY8 || i2.getType()!=i1.GRAY8) {
            IJ.showMessage(title, "Both images must be 8-bit ");
            return false;
        }
	if (useThirdImage){
		if (iDicSource.getType()!=iDicSource.GRAY8) {
			IJ.showMessage(title, "3rd image must be 8-bit ");
			return false;
		}
        }
        ImageProcessor co = new FloatProcessor(256, 256);
	i3=new ImagePlus("Correlation Diagram",co);
	imResu =  NewImage.createRGBImage("Colocalizated points",width1,height1,1,NewImage.FILL_BLACK);
	
	return true;
    }

      
    public void correlate(ImagePlus i1, ImagePlus i2,ImagePlus i3) {

        int width = i1.getWidth();
        int height = i1.getHeight();
        
        ImageProcessor ip1, ip2, ip3;

        ip1 = i1.getProcessor();
        ip2 = i2.getProcessor();
	ip3 = i3.getProcessor();
        for (int y=0; y<height; y++) {
                for (int x=0; x<width; x++) {
                    int z1 = (int)ip1.getPixelValue(x,y);
                    int z2 = 255-(int)ip2.getPixelValue(x,y);
                    int count = (int)ip3.getPixelValue(z1, z2);
                count++;
                ip3.putPixelValue(z1, z2, count);
                }
            }
	    i3.show();
	    IJ.run("Enhance Contrast", "saturated=0.5");
	    IJ.run("Fire");
	    IJ.makeRectangle(50, 50, 150, 150);//make the first rectangle selection on this image
	    if (ratio){
		    ip3.setColor(java.awt.Color.white);
		    int xmax = (int)(255*(r/100));
		    int ymax = (int)(255*(r/100));
		    ip3.drawLine(0,255,xmax,0);
		    ip3.drawLine(0,255,255,255-ymax);
		    i3.updateAndDraw();
	    }

    }
    
    public void mouseReleased(MouseEvent e) {
	comparaison();
    }
    
    public void keyReleased(KeyEvent e) {
        comparaison();
    }

    void comparaison() {
        if (!isSelection())
             return;
        checkPlotWindow();

	Rectangle coord;
	Roi rect;
	ImageProcessor ip1,ip2,IR_ip,dicIp;
	ip1= i1.getProcessor();
	ip2 = i2.getProcessor();
	IR_ip =imResu.getProcessor();
	dicIp=iDic.getProcessor();
	int minI1, maxI1,maxI2,minI2,x,y,vi1,vi2,vDic,lon,lar,cid, dRed,dBlue,dGreen;
	lon = i1.getWidth();
	lar = i1.getHeight();
	rect = i3.getRoi();
	coord = rect.getBoundingRect();
	minI1=coord.x;
	minI2=255 - coord.y - coord.height;
	maxI1=coord.x + coord.width;
	maxI2=255 - coord.y ;
	counter=0;
	int[] pixels = (int[]) IR_ip.getPixels();
	int[] dicPixels = (int[]) dicIp.getPixels();
	for (y=0; y<lar; y++) {
                for (x=0; x<lon; x++) {
                    int pos = y*lon +x;
		    int c = pixels[pos];
		    int red =(c&0xff0000)>>16;
		    int green =(c&0x00ff00)>>8;
		    int blue =(c&0x0000ff);
		    cid =dicPixels[pos];
		    if (useThirdImage) {
			    dRed = (cid&0xff0000)>>16;
			    dGreen = (cid&0x00ff00)>>8;
			    dBlue = (cid&0x0000ff);
		    }
		    boolean condition = false;
		    vi1 = (int) ip1.getPixelValue(x,y);
                    vi2 = (int) ip2.getPixelValue(x,y);
		    vDic = (int) iDicSource.getProcessor().getPixelValue(x,y);
		    double rap;
		    if (vi1>=vi2) rap=(double) vi2/(vi1+0.0000001);
		    else rap=(double)vi1/(vi2+0.00000001);
		    if (vi1>=minI1 && vi1<=maxI1 && vi2>=minI2 && vi2<=maxI2 ){
			    if (ratio){
				    if (rap*100>=r)  condition=true;
			    }
			    else condition=true;
		    }
		    if (condition){
			    red=green=blue=255;
			    pixels[pos] = ((red & 0xff)<<16) + ((green & 0xff) <<8) + (blue & 0xff);
			    counter++;
			    if (useThirdImage) {
				    dRed = dBlue= 0;
				    dGreen = 255;
				    dicPixels[pos] = ((dRed & 0xff)<<16) + ((dGreen & 0xff) <<8) + (dBlue & 0xff);
			    }
		    }
		    else {
			    red=vi1;
			    green=vi2;
			    blue=0;
			    pixels[pos] = ((red & 0xff)<<16) + ((green & 0xff) <<8) + (blue & 0xff);
			    if (useThirdImage) {
				    dRed = dBlue = dGreen = vDic;
				    dicPixels[pos] = ((dRed & 0xff)<<16) + ((dGreen & 0xff) <<8) + (dBlue & 0xff);
			    }
		    }
		}
	 }
	imResu.show();
        imResu.setProcessor(null, IR_ip);
	if (useThirdImage){
		iDic.show();
	}
	iDic.setProcessor(null, dicIp);
	
	double nbPixel =i1.getWidth()*i1.getHeight();
	double rapport = (counter / nbPixel)*100;
	IJ.write(IJ.d2s(minI1)+"\t"+IJ.d2s(maxI1)+"\t"+IJ.d2s(minI2)+"\t"+IJ.d2s(maxI2)+"\t"+IJ.d2s(rapport));
    }

    // returns true if there is a rectangular selection
    boolean isSelection() {
        if (i3==null)
            return false;
        Roi roi = i3.getRoi();
        if (roi==null)
            return false;
        int roiType = roi.getType();
        if (roiType==Roi.RECTANGLE)
            return true;
       else
            return false;
    }

    // stop listening for mouse events if the plot window has been closed
    void checkPlotWindow() {
       if (imResu==null) 
            return;
       ImageWindow win = imResu.getWindow();
       if (win==null || win.isVisible()) 
           return;
       win = i3.getWindow();
       if (win==null)
           return;
       canvas = win.getCanvas();
       canvas.removeMouseListener(this);
       canvas.removeMouseMotionListener(this);
       canvas.removeKeyListener(this);
   }

    public void mousePressed(MouseEvent e) {}
    public void mouseDragged(MouseEvent e) {}
    public void keyPressed(KeyEvent e) {}
    public void keyTyped(KeyEvent e) {}
    public void mouseExited(MouseEvent e) {}
    public void mouseClicked(MouseEvent e) {}   
    public void mouseEntered(MouseEvent e) {}
    public void mouseMoved(MouseEvent e) {}


}


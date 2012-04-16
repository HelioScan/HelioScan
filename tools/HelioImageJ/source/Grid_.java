import ij.*;
import ij.process.*;
import ij.gui.*;
import java.awt.*;
import java.util.*;
import ij.plugin.filter.*;
import ij.measure.*;

public class Grid_ implements PlugInFilter {
	private static String[] types = {"Lines", "Crosses", "Points"};
	private static String type = types[0];
	private static double areaPerPoint;
	private static boolean randomOffset;
	private Random random = new Random(System.currentTimeMillis());	
	private double tileWidth, tileHeight;
	private int xstart, ystart;
	private int linesV, linesH;
	private int lineWidth;
	private double pixelWidth=1.0, pixelHeight=1.0;
	private String units = "pixels";

	public int setup(String arg, ImagePlus imp) {
		if (!showDialog(imp))
			return(DONE);
		else {
			IJ.register(Grid_.class);
			return IJ.setupDialog(imp, DOES_ALL);
		}
	}

	public void run(ImageProcessor ip) {
		drawGrid(ip);
	}
	
	void drawGrid(ImageProcessor ip) {
		ip.setLineWidth(lineWidth);
		ip.setColor(Toolbar.getForegroundColor());
		if (type.equals(types[0]))
			drawLines(ip);
		else if (type.equals(types[1]))
			drawCrosses(ip);
		else
			drawPoints(ip);
	}
	
	void drawPoints(ImageProcessor ip) {
		int one = lineWidth;
		int two = lineWidth*2;
		for(int h=0; h<linesV; h++) {
			for(int v=0; v<linesH; v++) {
				int x = (int)(xstart+h*tileWidth);
				int y = (int)(ystart+v*tileHeight);
				ip.moveTo(x-two, y-one); ip.lineTo(x-two, y+one);
				ip.moveTo(x+two, y-one); ip.lineTo(x+two, y+one);
				ip.moveTo(x-one, y-two); ip.lineTo(x+one, y-two);
				ip.moveTo(x-one, y+two); ip.lineTo(x+one, y+two);
			}
		}
	}

	void drawCrosses(ImageProcessor ip) {
		int arm  = 5*lineWidth;
		for(int h=0; h<linesV; h++) {
			for(int v=0; v<linesH; v++) {
				int x = (int)(xstart+h*tileWidth);
				int y = (int)(ystart+v*tileHeight);
				ip.moveTo(x-arm, y);
				ip.lineTo(x+arm, y);
				ip.moveTo(x, y-arm);
				ip.lineTo(x, y+arm);
			}
		}
	}

	void drawLines(ImageProcessor ip) {
		int width = ip.getWidth();
		int height = ip.getHeight();
		for(int i=0; i<linesV; i++) {
			int xoff = (int)(xstart+i*tileWidth);
			ip.moveTo(xoff,0);
			ip.lineTo(xoff, height-1);
		}
		for(int i=0; i<linesH; i++) {
			int yoff = (int)(ystart+i*tileHeight);
			ip.moveTo(0, yoff);
			ip.lineTo(width-1, yoff);
		}
	}

	boolean showDialog(ImagePlus imp) {
		if (imp==null)
			return true;
		int width = imp.getWidth();
		int height = imp.getHeight();
		Calibration cal = imp.getCalibration();
		int places;
		if (cal.scaled()) {
			pixelWidth = cal.pixelWidth;
			pixelHeight = cal.pixelHeight;
			units = cal.getUnits();
			places = 2;
		} else {
			pixelWidth = 1.0;
			pixelHeight = 1.0;
			units = "pixels";
			places = 0;
		}
		if (areaPerPoint==0.0)
			areaPerPoint = (width*cal.pixelWidth*height*cal.pixelHeight)/81.0; // default to 9x9 grid
		lineWidth = Line.getWidth();
		ImageWindow win = imp.getWindow();
		double mag = win!=null?win.getCanvas().getMagnification():1.0;
		int scale = (int)(1.0/mag);
		if (lineWidth<scale)
			lineWidth = scale;
		GenericDialog gd = new GenericDialog("Grid...");
		gd.addChoice("Grid Type:", types, type);
		gd.addNumericField("Area per Point:", areaPerPoint, places, 6, units+"^2");
		gd.addNumericField("Line width:", lineWidth, 0, 6, "pixels");
		gd.addCheckbox("Random Offset", randomOffset);
		gd.showDialog();
		if (gd.wasCanceled())
			return false;
		type = gd.getNextChoice();
		areaPerPoint = gd.getNextNumber();
		Line.setWidth((int)gd.getNextNumber());
		randomOffset = gd.getNextBoolean();
		
		lineWidth = Line.getWidth();
		double tileSize = Math.sqrt(areaPerPoint);
		tileWidth = tileSize/pixelWidth;
		tileHeight = tileSize/pixelHeight;
		if (randomOffset) {
			xstart = (int)(random.nextDouble()*tileWidth);
			ystart = (int)(random.nextDouble()*tileHeight);
		} else {
			xstart = (int)(tileWidth/2.0+0.5);
			ystart = (int)(tileHeight/2.0+0.5);
		}
		linesV = (int)((width-xstart)/tileWidth)+1; 
		linesH = (int)((height-ystart)/tileHeight)+1;
		return true;
	}

}

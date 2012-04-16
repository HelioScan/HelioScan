import java.awt.*;
import ij.*;
import ij.gui.*;
import ij.process.*;
import ij.plugin.PlugIn;
import ij.io.*;
import java.io.*;
import ij.plugin.*;

// G. Landini at bham.ac.uk
// 28 Dec 2003
// Makes an RGB stack of LUTs
public class List_LUTs implements PlugIn {

	public void run(String arg) {
		IJ.showStatus("Select a folder with LUTs...");

		int w = 256, h = 40, x, y, i, j;
		OpenDialog od = new OpenDialog("Select any file...", null);

		String dir = od.getDirectory();
		String name = od.getFileName();
		if (name==null)
			return;

		String[] list = new File(dir).list();

		java.util.Arrays.sort(list);

		for (i=0; i<list.length; i++) {

			if(list[i].endsWith(".lut") || list[i].endsWith(".map") ){
			//if (!f.isDirectory()) {

				//IJ.log(i+": "+list[i]);
				IJ.showStatus("Opening "+(i+1)+": "+list[i]);
				//--------------------
				ImagePlus imp = NewImage.createByteImage((i+1)+":"+list[i], w, h, 1, 0);
				ImageProcessor ip = imp.getProcessor();
				byte[] pixels = (byte[])ip.getPixels();
				imp.show();
				j=0;
				for (y = 0; y < h; y++) {
					for (x = 0; x < w; x++) {
						pixels[j++] = (byte) x;
					}
				}
				//--------------------
				imp.updateAndDraw();
				//imp.readLut(dir+list[i]);
				IJ.run("LUT... ", "path='"+dir+list[i]+"'");
				IJ.wait(200);
				IJ.run("RGB Color");
				ImagePlus imp2 = WindowManager.getCurrentImage();
				ImageProcessor ip2 = imp.getProcessor();
				for (y = 0; y < 16; y++) {
					for (x = 0; x < w; x++) {
						ip2.putPixel(x,y,(int)((255 & 0xff) <<16)+ ((255 & 0xff) << 8) + (255 & 0xff));
					}
				}
				ip2.setFont(new Font("Monospaced", Font.PLAIN, 12));
				ip2.setAntialiasedText(true);
				ip2.setColor(Color.black);
				ip2.moveTo(8,16);
				ip2.drawString(list[i]);
				imp2.updateAndDraw();
			}
		}
		IJ.showStatus("Creating stack...");
		IJ.run("Convert Images to Stack");

	 }

}

import ij.*; 
import ij.io.*; 
import ij.util.Tools; 
import ij.process.*; 
import ij.plugin.*; 
import java.io.*; 
import java.util.*; 
import ij.measure.*; 
import java.awt.image.*; 
import java.awt.*; 


public class MCID_Reader implements PlugIn { 
    private String directory; 
    private String fileName; 
    private BufferedInputStream f; 
    int itag; 
    int nextBlock; 
    Calibration cal=new Calibration(); 

    public void run(String arg) {
        if (IJ.versionLessThan("1.35j")) return; 
        OpenDialog od = new OpenDialog("Open MCID...", arg); 
        directory = od.getDirectory(); 
        fileName = od.getFileName(); 
        if (fileName==null) 
                return; 
        IJ.showStatus("Opening: " + directory + fileName); 
        FileInfo fi = null; 
        try {fi = getHeaderInfo();}   
        catch (Exception e) { 
             IJ.showStatus(""); 
             IJ.showMessage("MCID Reader", ""+e); 
             return; 
            } 
        if (fi!=null) { 
               FileOpener fo = new FileOpener(fi); 
               ImagePlus imp = fo.open(false); 
               if (imp==null)    return; 
               imp.setCalibration(cal); 
            imp.show(); 
            } 
        } 

    FileInfo getHeaderInfo() throws IOException { 
        int width = 0; 
        int height = 0; 
        int nImages = 1; 
        int bitdepth = 0; 
        openFile(); 
        int tag = 0; 

        while (tag!=4003)  tag = getShort(); 
        f.skip(4); 
        width = getShort();    

        while (tag!=4004)  tag = getShort(); 
        f.skip(4);       
        height = getShort();   

//guess for bitdepth tag 
        while (tag!=4011)  tag = getShort(); 
        f.skip(4);   
        int bitDepth= getShort();        

//get offset 
//image data appears to start after 4020 tag 

        openFile();
        int offset = 0; 
        while (tag!=4020)  {tag = getShort(); 
                offset++;} 
        offset = (offset*2)+6; 

        FileInfo fi = new FileInfo(); 
        fi.intelByteOrder = true; 
        fi.fileFormat = fi.RAW; 
        fi.fileName = fileName; 
        fi.directory = directory; 
        fi.width = width; 
        fi.height = height; 
        //fi.nImages = nImages; 
        fi.offset = offset; 
//IJ.showMessage ("offset  "+ offset); 

        //set via bitdepth code 
        if (bitDepth==2) fi.fileType = fi.GRAY16_UNSIGNED; 
        if (bitDepth==4) fi.fileType = fi.BARG; 
        f.close(); 
        return fi; 
        } 
   
        int getByte() throws IOException { 
        int b = f.read(); 
        if (b ==-1) throw new IOException("unexpected EOF"); 
        return b; 
            } 

        int getShort() throws IOException { 
        int b0 = getByte(); 
        int b1 = getByte(); 
        return ((b1<< 8) + b0); 
        } 

        int getInt() throws IOException { 
        int b0 = getShort(); 
        int b1 = getShort(); 
        return ((b1<<16) + b0); 
            } 

    void openFile() throws IOException { 
        f = new BufferedInputStream(new 
FileInputStream(directory+fileName)); 
            } 



}

import ij.plugin.frame.*;
import ij.*;
import ij.process.*;
import ij.io.*;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

import quicktime.qd.*;
import quicktime.*;
import quicktime.std.*;
import quicktime.io.*;
import quicktime.sound.*;
import quicktime.std.image.*;
import quicktime.std.movies.*;
import quicktime.std.movies.media.*;
import quicktime.util.*;
import quicktime.app.display.*;
import quicktime.app.image.*;
import quicktime.app.QTFactory;
import quicktime.std.qtcomponents.*;


/**
	This plugin uses QuickTime for Java to save the active stack as a QuickTime movie.
	It is based on the QT for Java  CreateMovie demo. The CompressionSettings class
	was contibuted by Charles Thomas.
*/
public class QT_Movie_Writer extends PlugInFrame implements StdQTConstants, Errors {

	private QTCanvas canv;
	private QTImageDrawer qid;
	private ImagePainter np;
	private int numFrames;
	private int kWidth;
	private int kHeight;
	private File soundFile;
	private String title;
	private ImageStack stack;

	//***** Compressor settings
	static int comp_type = kVideoCodecType;
	static int spatial_quality = codecNormalQuality;
	static float frame_rate = 8;
	static int key_frame_rate = 10;
						
	public QT_Movie_Writer () {
		super("Temp");
	}		

	public void run(String arg) {
		//IJ.write("v14");
		ImagePlus imp = WindowManager.getCurrentImage();
		if (imp!=null)
			saveAsMovie(imp);
		else
			IJ.noImage();
		IJ.register( QT_Movie_Writer.class); 
	 }

	public void saveAsMovie(ImagePlus imp) {
		stack = imp.getStack();
		numFrames = stack.getSize();
		title = imp.getTitle();
		try {
			QTSession.open();
			initialize(stack);
			makeMovie();
			canv.removeClient();
			QTSession.close();
			dispose();
		} catch (NoClassDefFoundError e) {
			IJ.error("QuickTime for Java required");
		} catch (Exception e) {
			QTSession.close();
		}
	}

	void initialize(ImageStack stack) throws Exception {
		kWidth = stack.getWidth();
		kHeight = stack.getHeight();
		canv = new QTCanvas (QTCanvas.kInitialSize, 0.5F, 0.5F);
		add ("Center", canv);
		np = new ImagePainter(stack);
		qid = new QTImageDrawer(np, new Dimension (kWidth, kHeight), Redrawable.kMultiFrame);
		qid.setRedrawing(true);
		canv.setClient (qid, true);
		pack();
	}		

	void makeMovie() {
		try {		
			SaveDialog sd = new SaveDialog("Save as QuickTime Movie", title, ".mov");
			String dir = sd.getDirectory();
			String name = sd.getFileName();
			if(name == null)
				throw new QTIOException (userCanceledErr, "");
			QTFile f = new QTFile(dir+name);
			Movie theMovie = Movie.createMovieFile (f,
								kMoviePlayer, 
								createMovieFileDeleteCurFile | createMovieFileDontCreateResFile);

			// add content
			addVideoTrack( theMovie );

			// save movie to file
			OpenMovieFile outStream = OpenMovieFile.asWrite (f); 
			theMovie.addResource( outStream, movieInDataForkResID, f.getName() );
			outStream.close();
		} catch (Exception e) {
			if (!e.getMessage().startsWith("-128"))
				printStackTrace(e); 
		}
	}
	
	private void addVideoTrack (Movie theMovie) throws Exception {
		int kNoVolume	= 0;
		int kVidTimeScale = 600;

		Track vidTrack = theMovie.addTrack (kWidth, kHeight, kNoVolume);
		VideoMedia vidMedia = new VideoMedia (vidTrack, kVidTimeScale);  
								
		vidMedia.beginEdits();
		addVideoSample (vidMedia);
		vidMedia.endEdits();
		
		int kTrackStart	= 0;
		int kMediaTime 	= 0;
		int kMediaRate	= 1;
		vidTrack.insertMedia (kTrackStart, kMediaTime, vidMedia.getDuration(), kMediaRate);
	}
	
	private void addVideoSample(VideoMedia vidMedia ) throws Exception {
		QDRect rect = new QDRect (kWidth, kHeight);
		QDGraphics gw = new QDGraphics (rect);
		
		getCodecSettings(null);
		int codecType = comp_type;
		int quality = spatial_quality;
		//IJ.write("quality, codecType: "+quality+", "+codecType+" "+frame_rate);
		
		int size = QTImage.getMaxCompressionSize (gw, 
						rect, 
						gw.getPixMap().getPixelSize(),
	                                        	quality, 
	                                        	codecType, 
	                                        	CodecComponent.anyCodec);
		QTHandle imageHandle = new QTHandle (size, true);
		imageHandle.lock();
		RawEncodedImage compressedImage = RawEncodedImage.fromQTHandle(imageHandle);
		CSequence seq = new CSequence (gw,
						rect, 
						gw.getPixMap().getPixelSize(),
						codecType, 
						CodecComponent.bestFidelityCodec,
						quality, 
						quality, 
						key_frame_rate,
						null, //cTab,
						0);
		ImageDescription desc = seq.getDescription();

 		//redraw first...
       		np.setCurrentFrame (1);
		qid.redraw(null);

		qid.setGWorld (gw);
		qid.setDisplayBounds (rect);
			
		for (int curSample = 1; curSample <= numFrames; curSample++) {
			IJ.showProgress((double)curSample/numFrames);
			IJ.showStatus(curSample+"/"+numFrames);
			System.gc();
	       		np.setCurrentFrame (curSample);
			qid.redraw(null);
			CompressedFrameInfo info = seq.compressFrame (gw, 
						rect, 
						codecFlagUpdatePrevious, 
						compressedImage);
 			boolean isKeyFrame = info.getSimilarity() == 0;
 			//System.out.println ("f#:" + curSample + ",kf=" + isKeyFrame + ",sim=" + info.getSimilarity());
 			if (frame_rate==0)
 				frame_rate = 1;
 			vidMedia.addSample (imageHandle, 
						0, // dataOffset,
						info.getDataSize(),
						(int)((1/frame_rate)*600), //  desired time per frame	
						desc,
						1, // one sample
						(isKeyFrame ? 0 : mediaSampleNotSync)); // no flags
      		 }
		IJ.showProgress(1.0);
		IJ.showStatus("");
		
 		//print out ImageDescription for the last video media data ->
 		//this has a sample count of 1 because we add each "frame" as an individual media sample
 		//System.out.println (desc);

 		//redraw after finishing...
		qid.setGWorld (canv.getPort());
       		np.setCurrentFrame (numFrames);
		qid.redraw(null);
	}
	
	void	getCodecSettings(PixMap test_pix_map) throws Exception {
	
		ImageCompressionDialog comp_dlog = null;
		SpatialSettings spatial_settings = null;
		TemporalSettings temporal_settings = null;
		DataRateSettings data_rate_settings = null;
		QDRect test_rect = null;
		int color_depth = 0; // let ICM choose depth
		CodecComponent comp = CodecComponent.anyCodec;
		int temporal_quality = codecNormalQuality;
		int data_rate = 1000;
		int frame_duration = 8;
		boolean cancelled = false;
		int test_flags = 0;
		int flags = 0;
		
		//***** Get the standard QT compression dialog
		comp_dlog = new ImageCompressionDialog();
			
		//***** Set the default image
		comp_dlog.setTestImagePixMap(test_pix_map, test_rect, test_flags);
                               		   
		//***** Set up the default settings
		spatial_settings = new SpatialSettings(comp_type, comp, color_depth, spatial_quality);
		comp_dlog.setInfoSpatialSettings(spatial_settings);
		temporal_settings = new TemporalSettings(temporal_quality, frame_rate, key_frame_rate);
		comp_dlog.setInfoTemporalSettings(temporal_settings);
		data_rate_settings = new DataRateSettings(data_rate, frame_duration, codecMinQuality,
codecMinQuality);
		comp_dlog.setInfoDataRateSettings(data_rate_settings);
		
		//***** Set the default such that BEST DEPTH is not an option
		flags &= ~scShowBestDepth;
		comp_dlog.setInfoPreferences(flags);

		//***** Display the dialog for the user
		try {
			comp_dlog.requestSequenceSettings();
		} catch (Error er) {
			throw new Exception("User cancelled");
		}
			
		if (!cancelled) {
			//***** Get the user settings
			spatial_settings = comp_dlog.getInfoSpatialSettings();
			comp_type = spatial_settings.getCodecType();
			spatial_quality = spatial_settings.getSpatialQuality();		
			temporal_settings = comp_dlog.getInfoTemporalSettings();
			frame_rate = temporal_settings.getFrameRate();
			key_frame_rate = temporal_settings.getKeyFrameRate();	
		}
	}
	
	void printStackTrace(Exception e) {
		CharArrayWriter caw = new CharArrayWriter();
		PrintWriter pw = new PrintWriter(caw);
		e.printStackTrace(pw);
		IJ.write(caw.toString());
	}

}
	
	
class ImagePainter implements Paintable {

	ImageStack stack;
	private int width, height;
	private int numFrames;
	private int loopslot = 1;
	private int topInset, leftInset;
	private Font theFont = new Font (new String("Courier"), Font.PLAIN, 48);
	private boolean firstTime;
	private Rectangle[] ret = new Rectangle[1];
	private Rectangle dirtyDrawRect;

	public ImagePainter (ImageStack stack) {
		this.stack = stack;
		numFrames = stack.getSize();;
	}
	
	/**
	 * Returns the number of images
	 */
	public int getNumberOfFrames () { return numFrames; }
	
	public void setCurrentFrame (int frame) {
		loopslot = frame;
		if (loopslot < 1) loopslot = 1;
		if (loopslot > numFrames) loopslot = numFrames;
	}
	
	/** 
	 * Sets the current frame - zero based
	 */
	public int getCurrentFrame () {
		return numFrames;
    }

	/**
	 * The Parent object of the Paintable tell the paintable object the size of its available
	 * drawing surface. Any drawing done outside of these bounds (originating at 0,0) will
	 * be clipped.
	 */
	public void newSizeNotified (QTImageDrawer drawer, Dimension d) {
		width = d.width;
		height = d.height;
		//dirtyDrawRect = new Rectangle(width/2 - 25, height/2 - 20, 64, 44);
		dirtyDrawRect = new Rectangle(0, 0, width , height);
		ret[0] = new Rectangle (width, height);
		firstTime = true;
	}
	
	/**
	 * Paint on the graphics. The supplied component is the component from which
	 * the graphics object was derived or related to and is also the component
	 * that is the object that paint was called upon that has called this method.
	 * The Graphics object is what you should paint on.
	 * This maybe an on or off screen graphics.
	 * You should not cache this graphics object as it can be different
	 * between different calls.
	 * @param comp the component from which the Graphics object was derived or 
	 * related too.
	 * @param g the graphics to paint on.
	 */
	public Rectangle[] paint (Graphics g) {
		ImageProcessor ip = stack.getProcessor(loopslot);
		Image img = ip.createImage();
		g.drawImage(img, 0, 0, null);
		
		//if (firstTime)
		//	firstTime = false;	//we have done the first time now don't do it again
		//else
		//	ret[0] = dirtyDrawRect;	
		return ret;
	}
}




/* THIS way captures the result of the draw method of the QTImageDrawer
	it then adds the raw pixel data to the movie, getting that raw data and description
	from the ImagePresenter of the QTImageDrawer
	
	the way we add data above is to compress the GWorld the QTImageDrawer draws to
	using the animation compressor -> which also gives us frame differencing and a smalller data size
	and better playback for the movie.
	
	this code is presented as an alternative which may be appropriate in certain instances
	-> like adding image data for a sprite where no fidelity on each image is lost.
	in this case the important point is NOT the QTImageDrawer but the addition
	of the raw EncodedImage and ImageDescription -> how we get that is up to the application.
	
	for (int curSample = 1; curSample <= numFrames; curSample++) {
	       	QTUtils.reclaimMemory();
	       	np.setCurrentFrame (curSample);
			qid.redraw(null);
			ImagePresenter ip = qid.toImagePresenter();
			ImageDescription desc = ip.getDescription();
			EncodedImage imageData = ip.getImage(1);			
			vidMedia.addSample (QTHandle.fromEncodedImage(imageData), 
									0, // dataOffset,
									desc.getDataSize(),
									60, // frameDuration, 60/600 = 1/10 of a second, desired time per frame	
									desc,
									1, // one sample
									0 ); // no flags
        }
*/				



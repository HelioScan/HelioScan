import ij.*;
import ij.plugin.*;
import ij.process.*;
import ij.gui.*;
import ij.io.*;
import ij.plugin.Animator;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

public class AVI_Reader implements PlugIn {

	private  long              startTime;
	private  RandomAccessFile  raFile;
	private  int               bytesPerPixel;

	private  File              file;
	private  int               bufferFactor;
	private  int               xDim, yDim, zDim, tDim;
	private  int               lutBufferRemapped[] = null;
	private  int               microSecPerFrame;
	private  int               xPad;
	private  byte[]          bufferWrite;
	private  int               bufferSize;
	private  int               indexA, indexB;
	private  float            opacityPrime;
	private  int               bufferAdr;
	private  byte[]          lutWrite = null;
	private  int[]             dcLength = null;

	private  String           type = "error";
	private  String           fcc = "error";
	private  int               size = -1;

	private  boolean        verbose = IJ.debugMode;
	private  boolean        showTimes = false;

	private  int               fileLength;
	private  int               i;

	//From AVI Header Chunk

	private  int               dwMicroSecPerFrame;
	private  int               dwMaxBytesPerSec;
	private  int               dwReserved1;
	private  int               dwFlags;
	private  int               dwTotalFrames;
	private  int               dwInitialFrames;
	private  int               dwStreams;
	private  int               dwSuggestedBufferSize;
	private  int               dwWidth;
	private  int               dwHeight;
	private  int               dwScale;
	private  int               dwRate;
	private  int               dwStart;
	private  int               dwLength;

	//From Stream Header Chunk

	private  String            fccStreamType;
	private  String            fccStreamHandler;
	private  int               dwStreamFlags;
	private  int               dwStreamReserved1;
	private  int               dwStreamInitialFrames;
	private  int               dwStreamScale;
	private  int               dwStreamRate;
	private  int               dwStreamStart;
	private  int               dwStreamLength;
	private  int               dwStreamSuggestedBufferSize;
	private  int               dwStreamQuality;
	private  int               dwStreamSampleSize;

	//From Stream Format Chunk
	//BMP header reader from BMP class

	// Actual contents (40 bytes):
	private  int               BMPsize;// size of this header in bytes
	private  short            BMPplanes;// no. of color planes: always 1
	private  int               BMPsizeOfBitmap;// size of bitmap in bytes (may be 0: if so, calculate)
	private  int               BMPhorzResolution;// horizontal resolution, pixels/meter (may be 0)
	private  int               BMPvertResolution;// vertical resolution, pixels/meter (may be 0)
	private  int               BMPcolorsUsed;// no. of colors in palette (if 0, calculate)
	private  int               BMPcolorsImportant;// no. of important colors (appear first in palette) (0 means all are important)
	private  boolean       BMPtopDown;
	private  int               BMPnoOfPixels;

	private  int               BMPwidth;
	private  int               BMPheight;
	private  short           BMPbitsPerPixel;
	private  int               BMPcompression;

	private  int               BMPactualSizeOfBitmap;
	private  int               BMPscanLineSize;
	private  int               BMPactualColorsUsed;

	private  int[]             intData;
	private  byte[]          byteData;
	private  byte[]          rawData;

	//palette
	private  byte[]            r;
	private  byte[]            g;
	private  byte[]            b;

	private  byte[]            videoData;

	private  ColorModel        cm;
	private  ImageProcessor    ip;

	private  ImageStack        stack = new ImageStack(0, 0);

	private  long              pos;

	private  long              bigChunkSize = 0;

	private  String            fileName;

	private  long              lastTime = 0;
	private  int                 lastLine = 0;


	public void run(String arg) {
		try {
			IJ.showProgress(.01);
			readAVI(arg);
		} catch (OutOfMemoryError e) {
			//IJ.outOfMemory(fileName);
			stack.trim();
			IJ.showMessage("AVI Reader", "Out of memory.  " + stack.getSize() + " of " + dwTotalFrames + " frames will be opened.");
			if (stack != null && stack.getSize() > 0) {
				new ImagePlus(fileName, stack).show();
			}
			//break; // break out of the frame reading loop
		} catch (Exception e) {
			String  msg  = e.getMessage();
			if (msg == null || msg.equals("")) {
				msg = "" + e;
			}

			IJ.showMessage("AVI Reader", "An error occurred reading the file.\n \n" + msg);
		} finally {
			try {
				showTime("Closing File");
				raFile.close();
				if (verbose) {
					IJ.log("File closed.");
				}
			} catch (Exception e) {

			}
			IJ.showProgress(1);
			IJ.showProgress(0);
			long  totalTime  = (System.currentTimeMillis() - startTime);
			if (totalTime==0)
				totalTime = 1;
			IJ.showStatus("Done in " + totalTime + " msec.  " + (stack.getWidth() * stack.getHeight() * stack.getSize() / totalTime * 1000) + " pixels/second");
		}
	}


	public void readAVI(String path) throws Exception, IOException {

		if (showTimes) {
			IJ.write("/--------------------------/");
		}

		//lutBufferRemapped = new int[1];

		OpenDialog  sd = new OpenDialog("Select AVI File", path);
		startTime = System.currentTimeMillis();
		showTime("File selected");
		fileName    = sd.getFileName();
		if (fileName == null)
			return;
		String      fileDir     = sd.getDirectory();
		file = new File(fileDir + fileName);
		raFile = new RandomAccessFile(file, "r");

		if (verbose)
			IJ.log("/--------------------------/");

		showTime("Reading File Header");
		readFileHeader();

		byte[]      list        = new byte[4];
		String      listString;

		//IJ.write("bigChunkSize="+bigChunkSize);

		while (raFile.read(list) == 4) {
			raFile.seek(raFile.getFilePointer() - 4);

			listString = new String(list);

			updateProgress();

			if (listString.equals("JUNK")) {
				showTime("Skipping JUNK");
				skipJUNK();
			} else if (listString.equals("LIST")) {
				readTypeAndSizeAndFcc();
				raFile.seek(raFile.getFilePointer() - 12);

				if (fcc.equals("hdrl")) {

					readAviHeader();

				} else if (fcc.equals("strl")) {

					showTime("Reading Video Stream");

					long  startPos    = raFile.getFilePointer();
					long  streamSize  = size;

					readVideoStream();

					raFile.seek(startPos + 8 + streamSize);

				} else if (fcc.equals("movi")) {

					showTime("Reading Movie Data");
					readMovieData();

					showTime("Creating ImagePlus to display stack");
					if (stack != null && stack.getSize() > 0) {
						new ImagePlus(fileName, stack).show();
					}

					//raFile.close();
					return;
				} else {

					showTime("Skipping unknown block");
					if (verbose) {
						IJ.log("**Unsupported LIST fcc '" + fcc + "' detected at " + raFile.getFilePointer());
					}
					if (verbose) {
						IJ.log("   size=" + size);
						IJ.log("   Skipping...");
					}
					raFile.seek(raFile.getFilePointer() + 8 + size);
					if (verbose) {
						IJ.log("   Now at " + raFile.getFilePointer());
					}
				}

			} else {

				showTime("Skipping unknown block");

				if (verbose) {
					IJ.log("**Unsupported type '" + listString + "' detected at " + raFile.getFilePointer());
				}
				readTypeAndSize();
				if (verbose) {
					IJ.log("   size=" + size);
					IJ.log("   Skipping...");
				}
				raFile.seek(raFile.getFilePointer() + size);
			}

			if (verbose) {
				IJ.log("");
			}
		}

		raFile.close();
		return;
		/*
		 *  readAviHeader();
		 *  readVideoStream();
		 *  skipJUNK();
		 *  checkList("movi");
		 *  readMovieData();
		 */

	}



	void readFileHeader() throws Exception, IOException {
		readTypeAndSizeAndFcc();

		if (type.equals("RIFF")) {
			if (verbose) {
				IJ.log("RIFF format detected...");
				IJ.log("   size=" + size);
				IJ.log("   fcc=" + fcc);
			}
			bigChunkSize = size;
			if (!fcc.equals("AVI ")) {
				if (verbose) {
					IJ.log("Unsupported format '" + fcc + "'");
					IJ.log("Expected format 'AVI '");
				}
				throw new Exception("Unsupported file type.  AVI RIFF form required.");
			}
		} else {
			if (verbose) {
				IJ.log("Unexpected '" + type + "'");
				IJ.log("Expected 'RIFF' not found");
			}
			throw new Exception("The file does not appear to be in AVI format.");
		}

		if (verbose) {
			IJ.log("");
		}
	}


	void readAviHeader() throws IOException {
		readTypeAndSizeAndFcc();

		if (type.equals("LIST")) {
			if (verbose) {
				IJ.log("AVI header detected...");
				IJ.log("   size=" + size);
				IJ.log("   fcc=" + fcc);
			}
			if (fcc.equals("hdrl")) {
				readTypeAndSize();
				if (type.equals("avih")) {
					if (verbose) {
						IJ.log("   AVI header chunk (avih) detected...");
					}

					pos = raFile.getFilePointer();

					dwMicroSecPerFrame = readInt();
					dwMaxBytesPerSec = readInt();
					dwReserved1 = readInt();
					dwFlags = readInt();
					dwTotalFrames = readInt();
					dwInitialFrames = readInt();
					dwStreams = readInt();
					dwSuggestedBufferSize = readInt();
					dwWidth = readInt();
					dwHeight = readInt();
					dwScale = readInt();
					dwRate = readInt();
					dwStart = readInt();
					dwLength = readInt();

					if (verbose) {
						IJ.log("      dwMicroSecPerFrame=" + dwMicroSecPerFrame);
						IJ.log("      dwMaxBytesPerSec=" + dwMaxBytesPerSec);
						IJ.log("      dwReserved1=" + dwReserved1);
						IJ.log("      dwFlags=" + dwFlags);
						IJ.log("      dwTotalFrames=" + dwTotalFrames);
						IJ.log("      dwInitialFrames=" + dwInitialFrames);
						IJ.log("      dwStreams=" + dwStreams);
						IJ.log("      dwSuggestedBufferSize=" + dwSuggestedBufferSize);
						IJ.log("      dwWidth=" + dwWidth);
						IJ.log("      dwHeight=" + dwHeight);
						IJ.log("      dwScale=" + dwScale);
						IJ.log("      dwRate=" + dwRate);
						IJ.log("      dwStart=" + dwStart);
						IJ.log("      dwLength=" + dwLength);
					}

					raFile.seek(pos + size);

				}
			} else {
				IJ.log("**Unsupported fcc '" + fcc + "'");
				IJ.log("**Expected fcc 'hdrl'");
				return;
			}

		} else {
			IJ.log("**Unexpected '" + type + "'");
			IJ.log("**Expected 'LIST' not found");
			return;
		}

		if (verbose) {
			IJ.log("");
		}
	}



	void readVideoStream() throws Exception, IOException {
		readTypeAndSizeAndFcc();

		if (type.equals("LIST")) {
			if (fcc.equals("strl")) {
				if (verbose) {
					IJ.log("Stream header (strl) detected...");
					IJ.log("   size=" + size);
					IJ.log("   fcc=" + fcc);
				}

				readTypeAndSize();
				if (type.equals("strh")) {
					if (verbose) {
						IJ.log("   Stream header chunk (strh) detected...");
					}

					pos = raFile.getFilePointer();

					String  fccStreamTypeOld;
					fccStreamTypeOld = fccStreamType;
					fccStreamType = readStringBytes();

					if (!fccStreamType.equals("vids")) {
						if (verbose) {
							IJ.log("      Not video stream (fcc '" + fccStreamType + "')");
						}

						fccStreamType = fccStreamTypeOld;

						return;
					}

					readStrh();

					raFile.seek(pos + size);
				} else {
					IJ.log("**Expected fcc 'strh', found fcc '" + fcc + "'");
					return;
				}

				readTypeAndSize();
				if (type.equals("strf")) {
					if (verbose) {
						IJ.log("   Stream format chunk (strf) detected...");
					}

					pos = raFile.getFilePointer();

					readStrf();

					raFile.seek(pos + size);
				} else {
					IJ.log("**Expected fcc 'strf', found fcc '" + fcc + "'");
					return;
				}

			} else {
				IJ.log("**Expected fcc 'strl', found fcc '" + fcc + "'");
				return;
			}

			readTypeAndSize();
			if (type.equals("strd")) {
				if (verbose) {
					IJ.log("   Stream 'strd' chunk detected and skipped");
				}
				raFile.seek(raFile.getFilePointer() + size);

			} else {
				if (verbose) {
					IJ.log("   Type '" + type + "' detected.  Backing up.");
				}
				raFile.seek(raFile.getFilePointer() - 8);
			}

			readTypeAndSize();
			if (type.equals("strn")) {
				if (verbose) {
					IJ.log("   Stream 'strn' chunk detected and skipped");
				}
				raFile.seek(raFile.getFilePointer() + size);
			} else {
				if (verbose) {
					IJ.log("   Type '" + type + "' detected.  Backing up.");
				}
				raFile.seek(raFile.getFilePointer() - 8);
			}

		} else {
			IJ.log("**Unexpected '" + type + "'");
			IJ.log("**Expected 'LIST' not found");
			return;
		}

		if (verbose) {
			IJ.log("");
		}
	}


	void readStrh() throws IOException {
		fccStreamHandler = readStringBytes();
		dwStreamFlags = readInt();
		dwStreamReserved1 = readInt();
		dwStreamInitialFrames = readInt();
		dwStreamScale = readInt();
		dwStreamRate = readInt();
		dwStreamStart = readInt();
		dwStreamLength = readInt();
		dwStreamSuggestedBufferSize = readInt();
		dwStreamQuality = readInt();
		dwStreamSampleSize = readInt();
		if (verbose) {
			IJ.log("      fccStreamType=" + fccStreamType);
			IJ.log("      fccStreamHandler=" + fccStreamHandler);
			IJ.log("      dwStreamFlags=" + dwStreamFlags);
			IJ.log("      dwStreamReserved1=" + dwStreamReserved1);
			IJ.log("      dwStreamInitialFrames=" + dwStreamInitialFrames);
			IJ.log("      dwStreamScale=" + dwStreamScale);
			IJ.log("      dwStreamRate=" + dwStreamRate);
			IJ.log("      dwStreamStart=" + dwStreamStart);
			IJ.log("      dwStreamLength=" + dwStreamLength);
			IJ.log("      dwStreamSuggestedBufferSize=" + dwStreamSuggestedBufferSize);
			IJ.log("      dwStreamQuality=" + dwStreamQuality);
			IJ.log("      dwStreamSampleSize=" + dwStreamSampleSize);
		}
	}


	void readStrf() throws Exception, IOException {
		//BMP header reader from BMP class

		BMPsize = readInt();
		BMPwidth = readInt();
		BMPheight = readInt();
		BMPplanes = readShort();
		BMPbitsPerPixel = readShort();
		BMPcompression = readInt();
		BMPsizeOfBitmap = readInt();
		BMPhorzResolution = readInt();
		BMPvertResolution = readInt();
		BMPcolorsUsed = readInt();
		BMPcolorsImportant = readInt();

		BMPtopDown = (BMPheight < 0);
		BMPnoOfPixels = BMPwidth * BMPheight;

		// Scan line is padded with zeroes to be a multiple of four bytes
		BMPscanLineSize = ((BMPwidth * BMPbitsPerPixel + 31) / 32) * 4;

		if (BMPsizeOfBitmap != 0) {
			BMPactualSizeOfBitmap = BMPsizeOfBitmap;
		} else {
			// a value of 0 doesn't mean zero - it means we have to calculate it
			BMPactualSizeOfBitmap = BMPscanLineSize * BMPheight;
		}

		if (BMPcolorsUsed != 0) {
			BMPactualColorsUsed = BMPcolorsUsed;
		} else
		// a value of 0 means we determine this based on the bits per pixel
				if (BMPbitsPerPixel < 16) {
			BMPactualColorsUsed = 1 << BMPbitsPerPixel;
		} else {
			BMPactualColorsUsed = 0;
		}            // no palette

		if (verbose) {
			IJ.log("      BMPsize=" + BMPsize);
			IJ.log("      BMPwidth=" + BMPwidth);
			IJ.log("      BMPheight=" + BMPheight);
			IJ.log("      BMPplanes=" + BMPplanes);
			IJ.log("      BMPbitsPerPixel=" + BMPbitsPerPixel);
			IJ.log("      BMPcompression=" + BMPcompression);
			IJ.log("      BMPsizeOfBitmap=" + BMPsizeOfBitmap);
			IJ.log("      BMPhorzResolution=" + BMPhorzResolution);
			IJ.log("      BMPvertResolution=" + BMPvertResolution);
			IJ.log("      BMPcolorsUsed=" + BMPcolorsUsed);
			IJ.log("      BMPcolorsImportant=" + BMPcolorsImportant);
			IJ.log("      >BMPnoOfPixels=" + BMPnoOfPixels);
			IJ.log("      >BMPscanLineSize=" + BMPscanLineSize);
			IJ.log("      >BMPactualSizeOfBitmap=" + BMPactualSizeOfBitmap);
			IJ.log("      >BMPactualColorsUsed=" + BMPscanLineSize);

			IJ.log("      Read up to " + raFile.getFilePointer());
			IJ.log("      Format ends at " + (pos + size));
		}

		if (BMPcompression != 0) {
			if (verbose) {
				IJ.log("**Unsupported compression");
			}
			//IJ.showMessage("AVI Reader", "Warning: AVI appears to be compressed.  Fatal error may follow.");
			//throw new Exception("Unsupported bits-per-pixel value");
			throw new Exception("AVI file must be uncompressed.");
		}

		if (BMPbitsPerPixel != 8 && BMPbitsPerPixel != 24) {
			throw new Exception("Unsupported bits-per-pixel value (8 or 24 bits required)");
		}

		if (BMPactualColorsUsed != 0) {
			if (verbose) {
				IJ.log("      Now reading palette...");
			}           // + (pos + size) );

			long    pos1  = raFile.getFilePointer();

			byte[]  pr    = new byte[BMPcolorsUsed];
			byte[]  pg    = new byte[BMPcolorsUsed];
			byte[]  pb    = new byte[BMPcolorsUsed];
			for (int i = 0; i < BMPcolorsUsed; i++) {
				pb[i] = raFile.readByte();
				pg[i] = raFile.readByte();
				pr[i] = raFile.readByte();
				raFile.readByte();
			}
			if (verbose) {
				IJ.log("      Palette was " + (raFile.getFilePointer() - pos1) + " bytes long");
				IJ.log("      Palette ended at " + (raFile.getFilePointer()) + " and was expected to end at " + (pos + size));
			}

			cm = new IndexColorModel(BMPbitsPerPixel, BMPcolorsUsed, pr, pg, pb);

			if (verbose) {
				IJ.log("      Creating stack with palette...");
			}
			stack = new ImageStack(dwWidth, BMPheight, cm);

		} else {
			if (verbose) {
				IJ.log("      Creating stack...");
			}
			stack = new ImageStack(dwWidth, BMPheight);
		}
	}



	void skipJUNK() throws IOException {
		readTypeAndSize();
		if (type.equals("JUNK")) {
			if (verbose) 
				IJ.log("JUNK stream detected and skipped");
			raFile.seek(raFile.getFilePointer() + size);
		}
	}


	void checkList(String thefcc) throws IOException {
		readTypeAndSizeAndFcc();
		if (!fcc.equals(thefcc)) {
			if (verbose) 
				IJ.log("Type '" + type + "' with fcc '" + fcc + "' detected and skipped");
			raFile.seek(raFile.getFilePointer() + size);
		} else 
			raFile.seek(raFile.getFilePointer() - 12);
		if (verbose)
			IJ.log("");
	}


	void readMovieData() throws Exception, IOException {
		readTypeAndSizeAndFcc();

		if (type.equals("LIST")) {
			if (verbose)
				IJ.log("Type 'LIST' detected of fcc '" + fcc + "'...");
			if (fcc.equals("movi")) {
				if (verbose)
					IJ.log("Movie data detected...");
				readTypeAndSizeAndFcc();
				if (type.equals("LIST") && fcc.equals("rec ")) {
					if (verbose)
						IJ.log("   Movie record detected and skipped");
				} else {
					if (verbose)
						IJ.log("   Type '" + type + "' and fcc '" + fcc + "' detected.  Backing up.");
					raFile.seek(raFile.getFilePointer() - 12);
				}
				readTypeAndSize();
				long  startPos  = raFile.getFilePointer();
				if (BMPbitsPerPixel <= 8)
					ip = new ByteProcessor(dwWidth, BMPheight);
				else
					ip = new ColorProcessor(dwWidth, BMPheight);

				showTime(">>Entering while-loop to read chunks");
				while (type.substring(2).equals("db") || type.substring(2).equals("dc") || type.substring(2).equals("wb")) {
					updateProgress();
					pos = raFile.getFilePointer();
					if (type.substring(2).equals("db") || type.substring(2).equals("dc")) {
						if (verbose) {
							IJ.log("   Video data chunk (" + type + ") detected...");
							IJ.log("      size=" + size);
						}
						readFrame();
						if (BMPbitsPerPixel <= 8)
							ip.setPixels(byteData);
						else
							ip.setPixels(intData);
						if (BMPactualColorsUsed != 0) {
							ip.setColorModel(cm);
							if (verbose)
								IJ.log("      Color model set");
						}
						stack.addSlice("", ip);

					} else {
						if (verbose) {
							IJ.log("   Unknown data chunk (" + type + ") detected.  Skipping...");
							IJ.log("      size=" + size);
						}
					}

					//raFile.seek(pos + size);// +1);  // <= quicktime seems to need this for rgb files
					//this is hackish, but I don't see a better way at the moment
					//if (raFile.readByte() == 48) {
					//	if (verbose)
					//		IJ.log("     Skipping last byte");
					//	raFile.seek(raFile.getFilePointer() - 1);
					//}
					readTypeAndSize();
					if (type.equals("JUNK")) {
 						raFile.seek(raFile.getFilePointer() + size);
						readTypeAndSize();
					}
				}

				showTime(">>Exiting while-loop to read chunks");
				if (verbose)
					IJ.log("End of video data reached with type '" + type + "'.  Backing up.");
				raFile.seek(raFile.getFilePointer() - 8);
			} else 
				IJ.log("**Expected fcc 'movi', but found fcc '" + fcc + "'");
		} else
			IJ.log("**Expected 'LIST', but found '" + type + "'");
		if (verbose) 
			IJ.log("");
	}

	//from BMP class
	void unpack(byte[] rawData, int rawOffset, int bpp,
			byte[] byteData, int byteOffset, int w) throws Exception {

		for (int i = 0; i < w; i++) {

			byteData[byteOffset + i] = rawData[rawOffset + i];

		}

	}

	//from BMP class
	void unpack(byte[] rawData, int rawOffset, int[] intData, int intOffset, int w) {
		int  j     = intOffset;
		int  k     = rawOffset;
		int  mask  = 0xff;
		for (int i = 0; i < w; i++) {
			int  b0  = (((int) (rawData[k++])) & mask);
			int  b1  = (((int) (rawData[k++])) & mask) << 8;
			int  b2  = (((int) (rawData[k++])) & mask) << 16;
			intData[j] = 0xff000000 | b0 | b1 | b2;
			j++;
		}
	}

	//from BMP class
	void readFrame() throws Exception, IOException {
		int  len = BMPscanLineSize;
		if (BMPbitsPerPixel > 8)
			intData = new int[dwWidth * BMPheight];
		else
			byteData = new byte[dwWidth * BMPheight];
		rawData = new byte[BMPactualSizeOfBitmap];
		int  rawOffset  = 0;
		int  offset     = (BMPheight - 1) * dwWidth;
		for (int i = BMPheight - 1; i >= 0; i--) {
			int  n  = raFile.read(rawData, rawOffset, len);
			if (n < len) 
				throw new Exception("Scan line ended prematurely after " + n + " bytes");
			if (BMPbitsPerPixel > 8)
				unpack(rawData, rawOffset, intData, offset, dwWidth);
			else
				unpack(rawData, rawOffset, BMPbitsPerPixel, byteData, offset, dwWidth);
			rawOffset += len;
			offset -= dwWidth;
		}
	}

	//from BMP class
	void getPalette() throws IOException {
		int  noOfEntries  = BMPactualColorsUsed;
		//IJ.log("noOfEntries: " + noOfEntries);
		if (noOfEntries > 0) {
			r = new byte[noOfEntries];
			g = new byte[noOfEntries];
			b = new byte[noOfEntries];

			int  reserved;
			for (int i = 0; i < noOfEntries; i++) {
				b[i] = (byte) raFile.read();
				g[i] = (byte) raFile.read();
				r[i] = (byte) raFile.read();
				reserved = raFile.read();
			}
		}
	}


	String readStringBytes() throws IOException {
		//reads next  4 bytes and returns them as a string
		byte[]  list;
		list = new byte[4];
		raFile.read(list);
		return new String(list);
	}



	int readInt() throws IOException {
		// 4 bytes
		// http://mindprod.com/endian.html
		int  accum    = 0;
		int  shiftBy;
		for (shiftBy = 0; shiftBy < 32; shiftBy += 8) {
			accum |= (raFile.readByte() & 0xff) << shiftBy;
		}
		return accum;
	}


	short readShort() throws IOException {
		// 2 bytes
		int  low   = raFile.readByte() & 0xff;
		int  high  = raFile.readByte() & 0xff;
		return (short) (high << 8 | low);
	}

	void readTypeAndSize() throws IOException {//(String listString, int fileLength) throws IOException{
		type = readStringBytes();
		size = readInt();
		//IJ.log("readTypeAndSize: "+type + " " + size+ " "+raFile.getFilePointer() );
	}


	void readTypeAndSizeAndFcc() throws IOException {// String listString, int fileLength, String fccString) throws IOException{
		//listString = readStringBytes();
		//fileLength = readInt();
		//fccString = readStringBytes();
		type = readStringBytes();
		size = readInt();
		fcc = readStringBytes();
	}


	void updateProgress() throws IOException {
		IJ.showProgress((double) raFile.getFilePointer() / bigChunkSize);
		//IJ.write("update");
	}


	void showTime(String w) {
		if (showTimes) {
			long  thisTime  = System.currentTimeMillis();
			if (lastTime > 0) {
				IJ.write("    " + (thisTime - lastTime) + " spent between");
			}
			IJ.write((thisTime - startTime) + ": " + w);
			lastTime = thisTime;
		}
	}

}

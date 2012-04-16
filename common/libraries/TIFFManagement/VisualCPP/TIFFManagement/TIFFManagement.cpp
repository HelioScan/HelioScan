// TIFFManagement.cpp : Defines the exported functions for the DLL application.
//


/* Refer to: 
- http://www.libtiff.org/libtiff.html
- http://www.cs.wisc.edu/graphics/Courses/638-f1999/libtiff_tutorial.htm
- http://www.remotesensing.org/libtiff/libtiff.html
- http://www.ibm.com/developerworks/linux/library/l-libtiff/
*/

#include "stdafx.h"

int findTIFFPage(TIFF *tif, int page);

__declspec(dllexport) char* getDescription(char* path, int page, char* description)
{
	TIFF *tif=TIFFOpen(path, "r");
	char* localDescription = NULL;
	int pageFound = findTIFFPage(tif, page);
	if (pageFound)
	{
		TIFFGetField(tif, TIFFTAG_IMAGEDESCRIPTION, &localDescription);     		
	}
	description = strcpy(description, localDescription);
	TIFFClose(tif); 
	return(description);
}

__declspec(dllexport) int getDescriptionLength(char* path, int page)
{
	TIFF *tif=TIFFOpen(path, "r");
	char* localDescription = NULL;
	int pageFound = findTIFFPage(tif, page);
	if (pageFound)
	{
		TIFFGetField(tif, TIFFTAG_IMAGEDESCRIPTION, &localDescription);     		
	}
	int descriptionLength = strlen(localDescription);
	TIFFClose(tif); 
	return(descriptionLength);
}

__declspec(dllexport) int getNumberOfPages(char* path)
{
	TIFF *tif=TIFFOpen(path, "r");
	int pageCounter = 0;
	do
	{
		pageCounter++;
	}
	while(TIFFReadDirectory(tif));
	TIFFClose(tif);
	return(pageCounter);
}


__declspec(dllexport) int getPageProperties(char* path, int page, uint32& width, uint32& height, uint32& nBytes, uint16& samplesPerPixel, uint16& bitsPerSample) 
{	
	TIFF *tif=TIFFOpen(path, "r");
	int pageFound = findTIFFPage(tif, page);
	if (pageFound)
	{
		TIFFGetField(tif, TIFFTAG_IMAGEWIDTH, &width);           // get width;
		TIFFGetField(tif, TIFFTAG_IMAGELENGTH, &height);        // get height;
		TIFFGetField(tif, TIFFTAG_SAMPLESPERPIXEL, &samplesPerPixel);	// get channels
		TIFFGetField(tif, TIFFTAG_BITSPERSAMPLE, &bitsPerSample);	// get bit size
		nBytes = width * height * samplesPerPixel * bitsPerSample / 8;
	}

	TIFFClose(tif);
	return (pageFound);
}


int findTIFFPage(TIFF *tif, int page) 
{
	int pageCounter = 0;
	int pageFound = 1;
	while(pageCounter < page && pageFound)
	{
		pageFound = TIFFReadDirectory(tif);
		pageCounter++;
	}
	return (pageFound);
}


__declspec(dllexport) int readPageGeneral(char* path, int page, void* buffer)
{
	TIFF *tif=TIFFOpen(path, "r");
	int pageFound = findTIFFPage(tif, page);
	if (pageFound)
	{		
		// Read in the possibly multiple strips
		tsize_t stripSize = TIFFStripSize (tif);
		tstrip_t numberOfStrips = TIFFNumberOfStrips (tif);
		uint16 bitsPerSample, samplesPerPixel;
		TIFFGetField(tif, TIFFTAG_BITSPERSAMPLE, &bitsPerSample);	// get bit size
		TIFFGetField(tif, TIFFTAG_SAMPLESPERPIXEL, &samplesPerPixel);	// get channels
		
		for (tstrip_t stripCount = 0; stripCount < numberOfStrips; stripCount++)
		{
			switch(samplesPerPixel)
			{
				case 1:
					switch(bitsPerSample)
					{
						case 8:
							TIFFReadEncodedStrip(tif, stripCount, &(((uint8*)buffer)[stripCount * stripSize]), stripSize);
							break;
						case 16:
							TIFFReadEncodedStrip(tif, stripCount, &(((uint16*)buffer)[stripCount * stripSize / 2]), stripSize);
							break;
						case 32:
							TIFFReadEncodedStrip(tif, stripCount, &(((uint32*)buffer)[stripCount * stripSize / 4]), stripSize);
							break;
						default:
							return (0);
					}
					break;
				case 3:
					TIFFReadEncodedStrip(tif, stripCount, &(((uint8*)buffer)[stripCount * stripSize]), stripSize);
					break;
				default:
					return (0);
			}
		}
	}

	TIFFClose(tif);
	return (pageFound);
}

__declspec(dllexport) int writeSinglePage(char* path, uint32 width, uint32 height, double* buffer, char* format, char* description)
{
	TIFF *tif = TIFFOpen(path, "w");

	uint16 samplesPerPixel = 1;
	uint16 bitsPerSample, bytesPerSample;
	uint8* bufferUInt8 = (uint8*)buffer;
	uint16* bufferUInt16 = (uint16*)buffer;
	int16* bufferInt16 = (int16*)buffer;
	uint32* bufferUInt32 = (uint32*)buffer;
	int32* bufferInt32 = (int32*)buffer;
	float* bufferFloat32 = (float*)buffer;
	int numberOfSamples = width * height;
	tsize_t stripSize;
	tstrip_t numberOfStrips;

	if (!strcmp(format, "uint8")) 
	{
		bitsPerSample = 8;
		bufferUInt8 = new uint8[numberOfSamples];
		for(int i = 0; i < numberOfSamples; i++)
			bufferUInt8[i] = (uint8)buffer[i];
	}
	else if (!strcmp(format, "uint16"))
	{
		bitsPerSample = 16;
		bufferUInt16 = new uint16[numberOfSamples];
		for(int i = 0; i < numberOfSamples; i++)
			bufferUInt16[i] = (uint16)buffer[i];
	}
	else if (!strcmp(format, "int16"))
	{
		bitsPerSample = 16;
		bufferInt16 = new int16[numberOfSamples];
		for(int i = 0; i < numberOfSamples; i++)
			bufferInt16[i] = (int16)buffer[i];
	}
	else if (!strcmp(format, "uint32") || !strcmp(format, "int32"))
	{
		bitsPerSample = 32;
		bufferUInt32 = new uint32[numberOfSamples];
		for(int i = 0; i < numberOfSamples; i++)
			bufferUInt32[i] = (uint32)buffer[i];
	}
	else if (!strcmp(format, "int32"))
	{
		bitsPerSample = 32;
		bufferInt32 = new int32[numberOfSamples];
		for(int i = 0; i < numberOfSamples; i++)
			bufferInt32[i] = (int32)buffer[i];
	}
	else if (!strcmp(format, "float32"))
	{
		bitsPerSample = 32;
		bufferFloat32 = new float[numberOfSamples];
		for(int i = 0; i < numberOfSamples; i++)
			bufferFloat32[i] = (float)buffer[i];
	}

	bytesPerSample = bitsPerSample / 8;

	TIFFSetField(tif, TIFFTAG_SUBFILETYPE, 0);
	TIFFSetField(tif, TIFFTAG_IMAGEWIDTH, width);  // set the width of the image
	TIFFSetField(tif, TIFFTAG_IMAGELENGTH, height);    // set the height of the image
	TIFFSetField(tif, TIFFTAG_PHOTOMETRIC, 1); // black is zero
	TIFFSetField(tif, TIFFTAG_SAMPLESPERPIXEL, samplesPerPixel);   // set number of channels per pixel
	TIFFSetField(tif, TIFFTAG_BITSPERSAMPLE, bitsPerSample);    // set the size of the channels
	TIFFSetField(tif, TIFFTAG_IMAGEDESCRIPTION, description); // set the image description


   // set the strip size of the file to be size of one row of pixels
	stripSize = TIFFDefaultStripSize(tif, width * samplesPerPixel * height);
    TIFFSetField(tif, TIFFTAG_ROWSPERSTRIP, width);
	numberOfStrips = width * height / stripSize;

	for (tstrip_t stripCount = 0; stripCount < numberOfStrips; stripCount++)
	{
		if (!strcmp(format, "uint8"))	
			TIFFWriteEncodedStrip(tif, stripCount, &bufferUInt8[stripCount * stripSize], stripSize * bytesPerSample);
		else if (!strcmp(format, "uint16"))			
			TIFFWriteEncodedStrip(tif, stripCount, &bufferUInt16[stripCount * stripSize], stripSize * bytesPerSample);
		else if (!strcmp(format, "int16"))
			TIFFWriteEncodedStrip(tif, stripCount, &bufferInt16[stripCount * stripSize], stripSize * bytesPerSample);
		else if (!strcmp(format, "uint32"))					
			TIFFWriteEncodedStrip(tif, stripCount, &bufferUInt32[stripCount * stripSize], stripSize * bytesPerSample);
		else if (!strcmp(format, "int32"))					
			TIFFWriteEncodedStrip(tif, stripCount, &bufferInt32[stripCount * stripSize], stripSize * bytesPerSample);
		else if (!strcmp(format, "float32"))
			TIFFWriteEncodedStrip(tif, stripCount, &bufferFloat32[stripCount * stripSize], stripSize * bytesPerSample);
		else
			return 0;
	}

	TIFFClose(tif);

	return 1;
}




__declspec(dllexport) int writeMultiPage(char* path, char* descriptionFilePath, char* description)
{
	char* filePath;
	uint32 width, height;
	fstream descriptionFile;
	fstream rawDataFile;
	descriptionFile.open (descriptionFilePath, fstream::in);
	char line[1000];
	char* buffer;
	char* format;
	int bufferSize;
	uint16 samplesPerPixel = 1;
	uint16 bitsPerSample;
	uint16 bytesPerSample;
	tsize_t stripSize;
	tstrip_t numberOfStrips;

	TIFF *tif = TIFFOpen(path, "w");

	while(descriptionFile.getline(line, 1000, '\n'))
	{
		// parse description file line for file properties
		char delims[] = "\t";	
		filePath = strtok(line, delims);
		width = atoi(strtok(NULL, delims));
		height = atoi(strtok(NULL, delims));
		format = strtok(NULL, delims);

		if (!strcmp(format, "uint8"))
			bitsPerSample = 8;
		else if (!strcmp(format, "uint16") || !strcmp(format, "int16"))
			bitsPerSample = 16;
		else if (!strcmp(format, "uint32") || !strcmp(format, "int32") || !strcmp(format, "float32"))
			bitsPerSample = 32;


		bytesPerSample = bitsPerSample / 8;

		// allocate raw data buffer
		bufferSize = width * height * bytesPerSample;
		buffer = new char[bufferSize];

		// read raw file
		rawDataFile.open(filePath, fstream::in | fstream::binary);
		rawDataFile.read((char*)buffer, bufferSize);
		rawDataFile.close();

		TIFFSetField(tif, TIFFTAG_IMAGEWIDTH, width);  // set the width of the image
		TIFFSetField(tif, TIFFTAG_IMAGELENGTH, height);    // set the height of the image
		TIFFSetField(tif, TIFFTAG_SAMPLESPERPIXEL, samplesPerPixel);   // set number of channels per pixel
		TIFFSetField(tif, TIFFTAG_BITSPERSAMPLE, bitsPerSample);    // set the size of the channels
		TIFFSetField(tif, TIFFTAG_IMAGEDESCRIPTION, description); // set the image description

	   // set the strip size of the file to be size of one row of pixels
		stripSize = TIFFDefaultStripSize(tif, width * samplesPerPixel);
		TIFFSetField(tif, TIFFTAG_ROWSPERSTRIP, stripSize);
		numberOfStrips = width * height / stripSize;
		
		uint8* bufferUInt8 = (uint8*)buffer;
		uint16* bufferUInt16 = (uint16*)buffer;
		int16* bufferInt16 = (int16*)buffer;
		uint32* bufferUInt32 = (uint32*)buffer;
		int32* bufferInt32 = (int32*)buffer;
		float* bufferFloat32 = (float*)buffer;

		for (tstrip_t stripCount = 0; stripCount < numberOfStrips; stripCount++)
		{
			if (!strcmp(format, "uint8"))	
				TIFFWriteEncodedStrip(tif, stripCount, &bufferUInt8[stripCount * stripSize], stripSize * bytesPerSample);
			else if (!strcmp(format, "uint16"))			
				TIFFWriteEncodedStrip(tif, stripCount, &bufferUInt16[stripCount * stripSize], stripSize * bytesPerSample);
			else if (!strcmp(format, "int16"))
				TIFFWriteEncodedStrip(tif, stripCount, &bufferInt16[stripCount * stripSize], stripSize * bytesPerSample);
			else if (!strcmp(format, "uint32"))					
				TIFFWriteEncodedStrip(tif, stripCount, &bufferUInt32[stripCount * stripSize], stripSize * bytesPerSample);
			else if (!strcmp(format, "int32"))					
				TIFFWriteEncodedStrip(tif, stripCount, &bufferInt32[stripCount * stripSize], stripSize * bytesPerSample);
			else if (!strcmp(format, "float32"))					
				TIFFWriteEncodedStrip(tif, stripCount, &bufferFloat32[stripCount * stripSize], stripSize * bytesPerSample);
			else
				return 0;
		}
		TIFFWriteDirectory(tif);
		// deallocate raw data buffer
		delete[] buffer;
		
	}
	descriptionFile.close();

	TIFFClose(tif);

	return 1;
}
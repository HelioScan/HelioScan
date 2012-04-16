// Fiberscope_support_dll.cpp : Defines the exported functions for the DLL application.
//

#include "stdafx.h"

/* Call Library source file */
#include <extcode.h>
#include <nivision.h>

/* lv_prolog.h and lv_epilog.h set up the correct alignment for LabVIEW data. */
#include <lv_prolog.h>

typedef struct {
	char* name;
	Image* address;
} IMAQ_Image;

typedef struct {
	int32_t dimSize;
	double elt[1];
	} TD1;
typedef TD1 **TD1Hdl;

typedef struct {
	int32_t dimSize;
	int32_t elt[1];
	} TD2;
typedef TD2 **TD2Hdl;

typedef struct {
	int32_t dimSizes[2];
	double elt[1];
	} TD3;
typedef TD3 **TD3Hdl;

#include <lv_epilog.h>

#include "Fiberscope_support_dll.h"


void FIBERSCOPE_SUPPORT_DLL_API WINAPI pixelmap(IMAQ_Image *LVImagePointer, TD1Hdl adRawdata, TD2Hdl aiLookupX, TD2Hdl aiLookupY, TD3Hdl aiPixelhits)
{
	ImageInfo infolvImage, infotmpImage;
	Image *lvImage, *tmpImage;
	
	PixelValue pixelValue;
	pixelValue.grayscale = 128;

	lvImage = LVImagePointer->address;

	imaqFillImage(lvImage, pixelValue, NULL);
	imaqGetImageInfo(lvImage, &infolvImage);

	pixelValue.grayscale = 254;
	tmpImage = imaqCreateImage(IMAQ_IMAGE_U8, 3);
	imaqSetImageSize(tmpImage, infolvImage.xRes, infolvImage.yRes);
	imaqFillImage(tmpImage, pixelValue, NULL);
	imaqGetImageInfo(tmpImage, &infotmpImage);
	
	uint32_t bufsize = infolvImage.pixelsPerLine * infolvImage.yRes;
	uint32_t y;
	for ( y = 0 ; y < 100; y++ ) {
		memcpy_s((char*)infolvImage.imageStart + y*infolvImage.pixelsPerLine, bufsize, (char*)infotmpImage.imageStart + y*infotmpImage.pixelsPerLine, infotmpImage.pixelsPerLine);
	}
	
	imaqDispose(tmpImage);

}


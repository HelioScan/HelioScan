// The following ifdef block is the standard way of creating macros which make exporting 
// from a DLL simpler. All files within this DLL are compiled with the FIBERSCOPE_SUPPORT_DLL_EXPORTS
// symbol defined on the command line. This symbol should not be defined on any project
// that uses this DLL. This way any other project whose source files include this file see 
// FIBERSCOPE_SUPPORT_DLL_API functions as being imported from a DLL, whereas this DLL sees symbols
// defined with this macro as being exported.
#ifdef FIBERSCOPE_SUPPORT_DLL_EXPORTS
#define FIBERSCOPE_SUPPORT_DLL_API __declspec(dllexport)
#else
#define FIBERSCOPE_SUPPORT_DLL_API __declspec(dllimport)
#endif

extern "C" {
	void FIBERSCOPE_SUPPORT_DLL_API WINAPI pixelmap(IMAQ_Image *LVImagePointer, TD1Hdl adRawdata, TD2Hdl aiLookupX, TD2Hdl aiLookupY, TD3Hdl aiPixelhits);
}
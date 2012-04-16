Threshold_Colour_readme.txt
Version 1.0
22/Feb/2004.

The plugin Threshold_Colour is a modification by G. Landini
<G.Landini@bham.ac.uk> of Bob Dougherty's <rpd@optinav.com> plugin
BandPass2. Some portions of the code are from ThresholdAdjuster
by Wayne Rasband.

The plugin has extended support for thresholding in the
Hue-Saturation-Brightness (HSB) and Red-Green-Blue (RGB) spaces.

ImageJ is available at: http://rsb.info.nih.gov/ij/

Bob Dougherty's BandPass2 plugin is aavailable at:
http://www.optinav.com/ImageJplugins/BandPass2.htm


Installation:
Copy the Threshold_Colour.jar file to the plugins folder and restart ImageJ.
The entry Threshold_Colour should appear under the Plugins>Filters hierarchy.


Description:
ImageJ plugin to isolate pixels in an RGB image or stack with certain
combinationsof ranges of hue, saturation and brightness or red, green and blue
components. Below is the description of the controls.

[Pass]: Band-pass filter (anything within range is displayed).

[Stop]: Band-reject filter (anything within range is NOT displayed).

[Original]: Shows the original image and updates the buffer when
switching to another image.

[Filtered]: Shows the filtered image.

[Stack]: Processes the rest of the slices in the stack (if any)
using the current settings.

[Threshold]: Shows the object/background in the foreground and
background colours selected in the ImageJ toolbar.

[Invert]: Swaps the fore/background colours.

[Sample]: (experimental) Sets the ranges of the filters based on the
pixel value componentd in a rectangular, user-defined, ROI.

[HSB] [RGB]: Selects HSB or RGB space and resets all the filters.

Note that the 'thresholded' image is RGB, not 8 bit grey
Just convert it to 8-bit if further binary thresholding is necessary.

---
History, in addition to BandPass2
17/Feb/2004.	Modified by G. Landini. The changes are seen as the
				sliders/checkboxes are adjusted. Added hue strip to
				histogram window, changed histogram scale factor

19/Feb/2004.	Modified by G. Landini. Added Saturation and Brightness histograms,
				Added Pass/Stop checkboxes for each HSB channel.
				Added threshold, added inversion of threshold
				Cleaned some variables. Changed name to Threshold_HSB

22/Feb/2004		Modified by G. Landini. Threshold in RGB or HSB space
				Changed name to Threshold_Colour, changed button names.
				Added thresholding by "sampling". Hue band sampled selection may not'
				always work (experimental, problems with 0 valued histograms).
				Thread now finishes properly.

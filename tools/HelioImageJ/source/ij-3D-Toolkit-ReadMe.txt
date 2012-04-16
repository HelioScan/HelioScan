------------------------
ij-3D-Toolkit - 2/8/2002
------------------------

3D Toolkit is a set of plugins for 3D and 2D operations on images for 
Image/J ( http://rsb.info.nih.gov/ij/). The toolkit has three groups of 
plugins:

    * 3D IO - reading and writing of images in VTK and MetaImage formats
    * 3D Toolkit - native Java operations on 2D and 3D images
    * VTK Filters - wrappers for native VTK filters

_____
3D IO
-----

3D IO is a set of plugins reading and writing of images in VTK (www.vtk.org) 
and MetaImage (www.itk.org) format. All plugins in this set are scriptable 
using ImageJ macros.

__________
3D Toolkit
----------

    * Connected threshold region growing segmentation 
    * Auto volume clipping to a region that contains non-zero pixels
    * Morphological dilation (max)
    * Morphological erosion (min)

__________
VTK Filter
----------

A set of plugins that allow direct access to VTK image filters from ImageJ. 
All those filter will work on a single slice image when an image has 
multiple slices it is assumed that they form a volume (a 3D image). All 
filters can handle 8 bit, 16 bit, 32 bit integer or float pixel types. 
Here are some of the available filters, links in the brackets indicate wrapped 
VTK filter:

    * Anisotropic diffusion (vtkImageAnisotropicDiffusion3D)
    * Morphological dilation (vtkImageContinuousDilate3D)
    * Morphological erosion (vtkImageContinuousErode3D)
    * Laplacian operator (vtkImageLaplacian)
    * Median filter (vtkImageMedian3D)

Use of vtk filter requires installed version of VTK with Java bindings. 
Filters were tested with VTK 4.1.1.

____________
INSTALLATION
------------

Following steps assume that you downloaded binary archive 
ij-3D-Toolkit_bin_0.3.zip or newer from http://ij-plugins.sf.net

   1. Follow the link and download latest ij-3D-Toolkit_bin_*.zip
   2. Locate your Image/J plugins folder.
   3. Remove from Image/J plugins folder sub directories named '3D IO' and
      '3D Toolkit', if their exist.
   4. Unzip content of ij-3D-Toolkit_bin_*.zip into Image/J plugins folder.
   5. Add ij-plugins-tookit.jar to Image/J class path. One of the ways to 
      do it is by extending classpath in Image/J startup script, for instance:
        java -cp ij.jar;plugins/ij-plugins-toolkit.jar ij.ImageJ
   6. To use VTK filters add path to VTK Java classes typically located in 
      vtk.jar. VTK binaries and source code are available from www.vtk.org.
   7. Restart Image/J.

______
SOURCE
------

The source code consist two components:

* Toolkit - library containing implementation of all classes responsible
	    for plugins functionality.

* Plugins - plugin loaders that interface the Toolkit to Image/J. Each
	    subproject within Plugins directory corresponds to a some
	    functional grouping of plugins. Each of those groupings is
	    intended to show up as a sub-menu in Image/J plugins menu.

Binaries can be build from the source code using the build file
(build.xml) for Ant build tool ( http://jakarta.apache.org/ant/). 
Building of complete release requires VTK Java classes. Classpath to VTK 
needs to be specified either at command prompt (property 'vtk.classpath') 
or by editing the main class path.   

___________________
CONTACT INFORMATION
-------------------

Author : Jarek Sacha 
E-mail : jsacha@users.sourceforge.net
Webpage: http://ij-plugins.sourceforge.net/
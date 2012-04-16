:: Batch file for 3D segmentation of neurons from an RGBfile
:: parameters:   %1-Filename (without .tif),  %2-IJ_Directory (without ending \)
::
:: Author: Fritjof Helmchen, 1.2.2008
::
::echo off

::set PATH=%PATH%;C:\Program Files\Java\jre1.5.0_06\bin

set tmppath=p:\data\Hifo\software\Labview\3DSegmentation
set cmd_3Dcontrast=%tmppath%\3DContrast
set cmd_FindCoordinates=%tmppath%\FindCoordinates
set resultsfile=%tmppath%\%1_res.txt


:: Prepare RGB-tiff stack for 3D segmentation:
java -jar ij.jar -batch %cmd_3DContrast% %1

:: perform 3D segmentation:
3dsegment.exe %1_todo

:: determine coordinates of center of masses 
java -jar ij.jar -batch %cmd_FindCoordinates% %1 > %resultsfile% 
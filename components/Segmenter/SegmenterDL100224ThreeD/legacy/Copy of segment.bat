:: Batch file for 3D segmentation of neurons from an RGBfile
:: parameters:   %1-Filename (without .tif),  %2-IJ_Directory (without ending \)
::
:: Author: Fritjof Helmchen, 1.2.2008
::
::echo off

::set PATH=%PATH%;C:\Program Files\Java\jre1.5.0_06\bin

set tmppath=P:\Data\Doktorarbeit\Data\2_Photon\Segmentation\MacroRunner_27Mar08
set cmd_3Dcontrast=%tmppath%\3DContrast
set cmd_FindCoordinates=%tmppath%\FindCoordinates
set resultsfile=%tmppath%\%1_res.txt

:: Prepare RGB-tiff stack for 3D segmentation:
java -jar ij.jar -batch %cmd_3DContrast% %1

:: perform 3D segmentation:
P:\Data\Doktorarbeit\Data\2_Photon\Segmentation\MacroRunner_27Mar08\3dsegment %1_todo

:: determine coordinates of center of masses 
java -jar ij.jar -batch %cmd_FindCoordinates% %1 > %resultsfile% 
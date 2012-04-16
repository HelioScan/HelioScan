"RunMacro.java" is an an example program that shows how to run an 
ImageJ macro from the command line with no GUI. ImageJ 1.37h 
and later can also run macros from the command line when you use
the -batch option. The ImageJ 1.37h ij.jar file is included
in this directory.

To compile:
  javac -classpath ij.jar RunMacro.java

To run the macro "analyze.txt", passing it the argument "blobs.tif":
  java -cp ij.jar:. RunMacro analyze.txt blobs.tif (Unix)
  java -cp ij.jar;. RunMacro analyze.txt blobs.tif (Windows)

You can also run this macro by running ImageJ with
the -batch command line option:

  java -jar ij.jar -batch analyze blobs.tif

Use I/O redirection to save macro output in a file:

  java -jar ij.jar -batch analyze blobs.tif>results.txt

Four example command line macros are in this folder:
  table.txt: Generates a sine/cosine table
  analyze.txt: Runs the particle analyzer on an image
  process.txt: Analyzes all the tiff images in a folder
  count.txt: Counts the number of lines in a text file
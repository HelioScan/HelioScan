import ij.*;

/** 
	This example shows how to write a command line 
	program that runs an ImageJ macro.
	
	To compile this program:
		javac -classpath ij.jar RunMacro.java
	
	To run the macro "macro.txt" from the command line: 
		java -cp ij.jar:. RunMacro macro.txt (Unix)
		java -cp ij.jar;. RunMacro macro.txt (Windows)
*/
public class RunMacro {

    public static void main(String args[]) {
        if (args.length<1)
            IJ.log("usage: RunMacro macro [argument]");
        else {
            //new ImageJ(); // open the ImageJ window to see images and results
            String macro = args[0];
            String arg = args.length==2?args[1]:null;
            if (!(macro.endsWith(".txt")||macro.endsWith(".TXT")))
            	macro += ".txt";	
            IJ.runMacroFile(macro, arg);
            System.exit(0);
        }
    }

}



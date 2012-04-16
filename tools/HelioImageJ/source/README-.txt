Plugins folder
---------------------
This folder is where user-written plugins go. It contains
two sample plugins. "RedAndBlue_.java" is an example
acquisition plugin for acquiring or generating images.
"Inverter_.java" is an example image filter. Many more are
available at "http://rsb.info.nih.gov/ij/plugins/". Plugins
with an underscore in their name are automatically installed
in the Plugins menu. Plugins in subfolders are installed in
submenus. Assign keyboard shortcuts using the Plugins/Shortcuts/
Create Shortcut command. Install plugins in other menus using
the Plugins/Shortcuts/Install Plugins command.

Compiling Plugins
----------------------------
To compile and run a plugin, use either the Plugins/Compile
and Run command or the Plugins/Edit command. Both commands
require that ImageJ be running on a Java Virtual Machine that
that includes the javac compiler, such as the JRE 1.1.8 distributed
with the Windows and Linux X86 versions of ImageJ. 

The javac compiler is also included with the Java Development
Kits from Sun but the tools.jar file must be in the classpath
before ImageJ can use it. This requires that you run ImageJ 
using a  command something like 

    java -cp ij.jar;C:\jdk1.3\lib\tools.jar ij.ImageJ

Alternately, put a copy of tools.jar in the Java extensions
folder, located at something like

    C:\Program Files\JavaSoft\JRE\1.3\lib\ext

and run ImageJ by double clicking on ij.jar.

To compile plugins in the Macintosh you must download and
install the MRJ Software Development Kit (SDK) from

    http://developer.apple.com/java/text/download.html#sdk

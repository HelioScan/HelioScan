import java.awt.*;
import java.awt.event.*;
import java.awt.datatransfer.*;
import ij.*;
import ij.process.*;
import ij.plugin.frame.*;
import ij.gui.*;
	
/** Copies and Pastes images to the system clipboard. Requires 1.4 or later. */
public class System_Clipboard extends PlugInFrame implements ActionListener, Transferable {
 	Button copyButton;
	Button pasteButton;
	Clipboard clipboard; 
	static Frame instance;

	public System_Clipboard() {
		super("Clipboard");
		//if (instance!=null) {
		//	instance.toFront();
		//	return;
		//}
		instance = this;
		WindowManager.addWindow(this);
		if (!IJ.isJava14()) {
			IJ.error ("This plugin requires Java 1.4 or later");
			return;
		}
		copyButton = new Button("Copy");
		pasteButton = new Button("Paste");
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		clipboard = toolkit.getSystemClipboard();
		copyButton.addActionListener(this);
		pasteButton.addActionListener(this);
		Panel panel = new Panel();
		panel.setLayout(new FlowLayout());
		panel.add(copyButton);
		panel.add(pasteButton);
		add(panel);
		pack();
		GUI.center(this);
		setVisible(true);
	}
	
	public void actionPerformed(ActionEvent e) {
		try {
  			if (e.getSource() == copyButton)
				clipboard.setContents(this, null);
			else {
				Transferable transferable = clipboard.getContents(null);
				if (transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
					Image img = (Image)transferable.getTransferData(DataFlavor.imageFlavor);
					new ImagePlus("Pasted Image", img).show();
				} else
					IJ.showMessage("System_Clipboard", "No image data in the system clipboard.");
			}
		} catch (Throwable t) {}
	}

	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { DataFlavor.imageFlavor };
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return DataFlavor.imageFlavor.equals(flavor);
	}

	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
		if (!isDataFlavorSupported(flavor))
			throw new UnsupportedFlavorException(flavor);
		ImagePlus imp = WindowManager.getCurrentImage();
		if ( imp != null) {
			ImageProcessor ip = imp.getProcessor();
			ip = ip.crop();
			int w = ip.getWidth();
			int h = ip.getHeight();
			IJ.showStatus(w+"x"+h+ " image copied to system clipboard");
			Image img = createImage(w, h);
			Graphics g = img.getGraphics();
			g.drawImage(ip.createImage(), 0, 0, null);
			g.dispose();
			return img;
		} else {
			//IJ.noImage();
			return null;
		}
	}

	public Insets getInsets() {
		Insets i= super.getInsets();
		return new Insets(i.top+5, i.left+40, i.bottom+5, i.right+40);
	}
}




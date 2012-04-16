/*
 * LSM_Reader.java
 *
 * Created on february 2002 Copyright (C) 2002-2003 Patrick Pirrotte
 *
 * ImageJ plugin
 * Version  : 3.2d
 * Authors   :      Patrick PIRROTTE, Yannick KREMPP (till v2.2), Jerome Mutterer.
 *                  written for the IBMP-CNRS Strasbourg(France)
 *
 * Emails    :      Project maintainer          : patrick.pirrotte@gmx.net
 *                                                jerome.mutterer@ibmp-ulp.u-strasbg.fr
 *                  Previous project maintainer : yannick.krempp@iphysiol.unil.ch
 *
 * Description :    This plugin reads *.lsm files produced by Zeiss LSM 510 confocal microscopes.
 *                  Each channel of an image plane, stack or time series is opened as a separate
 *                  image or stack window. The plugin also retrieves calibration infos from
 *                  LSM files. This plugin has been built using Zeiss' v3.0 fileformat
 *                  specifications. This software is compatible with files generated with AIM version 3.2.
 *                  Other versions of the lsm format should be readable more or less well.
 *                  A short manual is available from
 *                  ibmp.u-strasbg.fr/sg/microscopie/methods/lsmio/lsmio.htm
 *
 *
 * Release History :
 *
 * 2003/12/01 (v3.2d):  Adds compatibility with the "Handle Extra File Type " Plug-In. This plug-in must be edited with the following code : 
 *                      (just copy and paste this code into the Handle Extra File Type in the appropriate place, then recompile it.)
 *                          
                        //  Zeiss Confocal LSM 510 image file (.lsm) handler
                        //  http://rsb.info.nih.gov/ij/plugins/	
                        if (name.endsWith(".lsm")) {
			// Open SPEimage and display it
			IJ.runPlugIn("LSM_Reader", path);
			// Set flag so Opener.openImage() does not display error
			width = IMAGE_OPENED; 
			return null; 
                        }
 *
 *
 
 * 2003/12/01 (v3.2c):  Adds the "Batch Convert" method. This method allows the batch conversion of an entire directory of lsm files
 *				into various usual file format : tiff, 8-bit tiff, jpeg, raw, zip. This converter handles also stacks and lambda stacks by saving 
 *				one image for each slice of the stack. Credits for the base batch conversion method goes for Wayne Rasband,
 * 				wayne@codon.nih.gov, many thanks for his kind collaboration.
 *
 *
 *
 * 2003/05/16 (v3.2b):  Bug fix release. Fixed a bug that scrambled 12 bit images. Added option to dump stamps to text
                        file
 *
 * 2003/04/22 (v3.2):   Handles v3.0 LSM files respectively AIM v3.2 files, SCANINFO structure is read entirely
 *                      and shown in a JTree / property viewer. Filtered view (only viewing open channels) and
 *                      general view. Uknown tags are also shown. Linking with Lut_Panel plugin if present.
 *
  * 2003/04/22 (v3.0):  SCANINFO structure present in every LSM file, contains some very interesting information.
 *                      Dumping of that structure is now possible. Unknown tags are shown separately. Linking with
 *                      HyperVolume_Browser plugin if present. Added support for spectral images. Complete file reading
 *                      rehaul. As soon as Unisys's license expires, a new version with LZW-compressed TIFF support
 *                      should be available.
 *
 * 2003/01/23 (v2.2):   Handles 12 bits datasets and timelapse datasets. Single image info window.
 *
 * 2002/09/16 (v1.2):   Fixes a bug that prevented the opening of specific images containing textual
 *                      notes together with single channel fluorescence data.
 *
 * 2002/07/16 (v1.1):   First released version
 *
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation; either version 2 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with this program; if not, write to the Free Software
 *   Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *
 */

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.awt.image.*;
import java.awt.Color.*;
import java.awt.*;
import java.util.*;
import ij.*;
import ij.io.*;
import ij.process.*;
import ij.plugin.PlugIn;
import ij.plugin.filter.PlugInFilter;
import ij.gui.*;
import ij.gui.YesNoCancelDialog;
import ij.plugin.StackEditor;
import ij.text.*;
import ij.io.RandomAccessStream;
import ij.io.ImageReader;
import ij.measure.*;
import ij.ImagePlus.*;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import javax.swing.table.AbstractTableModel;
import javax.swing.JTable;
import java.awt.datatransfer.*;



/**********************************
 * class : LSM_Reader, main class *
 *********************************/

public class LSM_Reader implements PlugIn{
    
    public GUI theGUI;
    public static JFrame infoFrame = new JFrame("LSM Infos");
    public static JFrame detailsFrame = new JFrame("LSM Details");
    
    public void run(String args) {
        if (args != ""){
            File temp = new File(args);
            String arg2 = temp.getName();
            //IJ.log(arg2);
            String arg1 = "";
            arg1 = temp.getPath();
            //IJ.log("before : "+arg1);
            arg1 = arg1.substring(0, (arg1.length()-arg2.length()));
            //IJ.log("after : "+arg1);
            LSM_Reader_ read = new LSM_Reader.LSM_Reader_();
            read.OpenLSM(arg1, arg2);
        }
        else{
           theGUI = new GUI();
           theGUI.init();
        }
        
        
    }
    
    /********************************************************
     * class GUI : this class implements the LSM Reader GUI *
     *******************************************************/
    
    class GUI {
        public JFrame baseFrame;
        private JPanel pan;
        private GridLayout baseGridLayout = new GridLayout(11,1,20,1);
        private GUIButton butt1 = new GUIButton(" Open LSM ","Opens a 8-bit LSM image or image stack");
        private GUIButton butt2 = new GUIButton(" Close all Windows ","Closes all opened Image Windows");
        private GUIButton butt3 = new GUIButton(" Exit ","Exits the LSM Reader Plug-In");
        private GUIButton butt4 = new GUIButton(" Show Infos ","Brings the infos panel to front");
        private GUIButton butt5 = new GUIButton(" Apply t-stamp ","Apply timestamp to each image of a time series stack");
        private GUIButton butt6 = new GUIButton(" Browse ","Browse Hypervolume, needs HyperVolume_Browser");
        private GUIButton butt7 = new GUIButton(" Apply z-stamp ","Apply z-stamp to each image of a z series stack");
        private GUIButton butt8 = new GUIButton(" Apply l-stamp ","Apply lambda-stamp to each image of a spectral series");
        private GUIButton butt9 = new GUIButton(" Edit Palette ","Edit Palette, needs Lut_Panel");
        private GUIButton butt10 = new GUIButton(" Batch convert ","Converts LSM files to other file formats");
        private JButton SwitchButton = new JButton("Switch to filtered view");
        private JLabel label1 = new JLabel(" LSM Tools ", JLabel.CENTER);
        public String[] LSMinfoText = new String[22];
        public String[] infos = new String[22];
        public LSMFileInfo lsm_fi;
        public long timestamps_count;
        private Dimension ScreenDimension = Toolkit.getDefaultToolkit().getScreenSize();
        private int ScreenX = (int) ScreenDimension.getWidth();
        private int ScreenY = (int) ScreenDimension.getHeight();
        private int baseFrameXsize = (int)(ScreenX/8);
        private int baseFrameYsize = (int)(ScreenY/4);
        private int infoFrameXsize = (int)(ScreenX/5);
        private int infoFrameYsize = (int)(1.5*ScreenY/4);
        
        private int detailsFrameXsize = (int)(ScreenX/5);
        private int detailsFrameYsize = (int)(1.5*ScreenY/2);
        private int baseFrameXlocation = (int)((11*ScreenX/12) - baseFrameXsize);
        private int baseFrameYlocation = (int)((11.5*ScreenY/12) - baseFrameYsize);
        private int infoFrameXlocation = (int)((ScreenX/12));
        private int infoFrameYlocation = (int)((5*ScreenY/8) - infoFrameYsize);
        private int detailsFrameXlocation = (int)((ScreenX/10));
        private int detailsFrameYlocation = (int)((7*ScreenY/8) - detailsFrameYsize);
        
        private JLabel[] infolab = new JLabel[22];
        private JTextArea[] area = new JTextArea[22];
        private JTree tree;
        private JTable table;
        private DefaultMutableTreeNode rootnode;
        private DefaultTreeModel treemodel;
        private MyTableModel tablemodel;
        private Object[][] rowData = new Object[][] {{"N/A","N/A"}};
        private Object[][] noData = new Object[][] {{"N/A","N/A"}};
        private Object[][] undefinedData = new Object[][] {{"defined but empty","defined but empty"}};
        private boolean switcher = false;
        
        public void GUI() {
        }
        
        /*******************************************
         * method : init, initiates the GUI Object *
         ******************************************/
        
        public void init() {
            baseFrame = new JFrame();
            baseFrame.setTitle("LSM Reader 3.2d");
            baseFrame.setSize(baseFrameXsize,baseFrameYsize);
            baseFrame.setResizable(false);
            baseFrame.setLocation(baseFrameXlocation,baseFrameYlocation);
            infoFrame.setSize(infoFrameXsize, infoFrameYsize);
            infoFrame.setLocation(infoFrameXlocation,infoFrameYlocation);
            addExitListener(butt3, baseFrame);
            addShowHideInfolistener(butt4, baseFrame);
            addOpenListener(butt1, baseFrame);
            addCloseWinListener(butt2, baseFrame);
            addApplyZStampListener(butt7, baseFrame);
            addApplyTStampListener(butt5, baseFrame);
            addBrowseListener(butt6, baseFrame);
            addLUTListener(butt9, baseFrame);
            addApplyLambdaStampListener(butt8, baseFrame);
            addBatchConvertListener(butt10, baseFrame);
            pan = new JPanel();
            pan.setSize(130, 280);
            pan.setForeground(SystemColor.window);
            pan.setLayout(baseGridLayout);
            pan.add(label1);
            pan.add(butt1);
            pan.add(butt4);
            pan.add(butt7);
            pan.add(butt5);
            pan.add(butt8);
            pan.add(butt6);
            pan.add(butt9);
            pan.add(butt2);
            pan.add(butt10);
            pan.add(butt3);
            butt5.setEnabled(false);
            butt7.setEnabled(false);
            butt8.setEnabled(false);
            baseFrame.getContentPane().add(pan);
            baseFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    baseFrame.dispose();
                }
            }
            );
            baseFrame.setVisible(true);
            baseFrame.show();
            initInfoFrame();
        }
        
        /*******************************************
         * method ! closeFrames, closes all frames *
         ******************************************/
        
        private void closeFrames() {
            baseFrame.dispose();
            infoFrame.dispose();
            detailsFrame.dispose();
        }
        
        /************************************************
         * method : initinfoFrame, inits the info panel *
         ***********************************************/
        
        public void initInfoFrame() {
            infoFrame.getContentPane().setLayout(new BorderLayout());
            String[] infolabels = new String[19];
            infolabels[0] = "File Name";
            infolabels[1] = "User";
            infolabels[2] = "Image Width";
            infolabels[3] = "Image Height";
            infolabels[4] = "Number of channels";
            infolabels[5] = "Z Stack size";
            infolabels[6] = "Time Stack size";
            infolabels[7] = "Scan Type";
            infolabels[8] = "Voxel X size";
            infolabels[9] = "Voxel Y size";
            infolabels[10] = "Voxel Z size";
            infolabels[11] = "Objective";
            infolabels[12] = "X zoom factor";
            infolabels[13] = "Y zoom factor";
            infolabels[14] = "Z zoom factor";
            infolabels[15] = "Plane width";
            infolabels[16] = "Plane heigth";
            infolabels[17] = "Volume depth";
            infolabels[18] = "Plane spacing";
            JPanel infopanel = new JPanel(new GridLayout(20,2,3,3));
            Font dafont = new Font(null);
            float fontsize = 11;
            dafont = dafont.deriveFont(fontsize);
            Font dafontbold = dafont.deriveFont(Font.BOLD);
            
            for (int i=0; i<19; i++) {
                infolab[i] = new JLabel("  "+infolabels[i]);
                infolab[i].setFont(dafontbold);
                infopanel.add(infolab[i]);
                area[i] =  new JTextArea("");
                area[i].setEditable(false);
                area[i].setFont(dafont);
                infopanel.add(area[i]);
            }
            JButton details_button = new JButton("More details...");
            addDetailsListener(details_button, infoFrame);
            JButton notes_button = new JButton("Notes");
            addNotesButtonListener(notes_button, infoFrame);
            infoFrame.getContentPane().add(infopanel,BorderLayout.NORTH);
            infoFrame.getContentPane().add(notes_button,BorderLayout.EAST);
            infoFrame.getContentPane().add(details_button,BorderLayout.WEST);
            infoFrame.pack();
            initDetailsFrame();
        }
        
        /****************************************************
         * method : updateInfoFrame, updates the info panel *
         ***************************************************/
        
        public void updateInfoFrame(String[] str) {
            for (int i=0; i<19; i++) {
                area[i].setText(str[i]);
            }
        }
        
        /*****************************************************
         * method : initDetailsFrame, inits the details panel *
         *****************************************************/
        public void initDetailsFrame() {
            rootnode = new DefaultMutableTreeNode("LSM File Information");
            treemodel = new DefaultTreeModel(rootnode);
            detailsFrame.setSize(detailsFrameXsize, detailsFrameYsize);
            detailsFrame.setLocation(detailsFrameXlocation,detailsFrameYlocation);
            tree = new JTree(treemodel);
            tree.putClientProperty("JTree.lineStyle", "Angled");
            tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
            tree.setShowsRootHandles(true);
            MyTableModel myModel = new MyTableModel();
            tablemodel = new MyTableModel();
            table = new JTable(tablemodel);
            //table.setPreferredScrollableViewportSize(new Dimension(400, 70));
            JScrollPane treepane = new JScrollPane(tree);
            JScrollPane detailspane = new JScrollPane(table);
            JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,treepane,detailspane);
            splitPane.setOneTouchExpandable(true);
            splitPane.setDividerLocation(300);
            Dimension minimumSize = new Dimension((int)(0.75*infoFrameXlocation/2),(int)(infoFrameXlocation/2));
            minimumSize = new Dimension(detailsFrame.WIDTH, detailsFrame.HEIGHT-50);
            treepane.setMinimumSize(minimumSize);
            detailspane.setMinimumSize(minimumSize);
            JButton OKButton = new JButton("Ok");
            OKButton.setToolTipText("Close this window");
            JButton DumpButton = new JButton("Dump data");
            DumpButton.setToolTipText("Dump data to textwindow, saving to text file is possible");
            SwitchButton.setToolTipText("Switch view");
            JPanel buttonpanel = new JPanel();
            buttonpanel.add(OKButton);
            buttonpanel.add(DumpButton);
            buttonpanel.add(SwitchButton);
            Dimension dim = new Dimension(buttonpanel.WIDTH,OKButton.HEIGHT+10);
            JSplitPane splitPanesub = new JSplitPane(JSplitPane.VERTICAL_SPLIT,splitPane,buttonpanel);
            splitPanesub.setOneTouchExpandable(true);
            splitPanesub.setDividerLocation(-detailsFrame.HEIGHT+OKButton.HEIGHT-10);
            detailsFrame.getContentPane().add(splitPanesub);
            addSwitchListener(SwitchButton, detailsFrame);
            addDumpDataListener(DumpButton, detailsFrame);
            addDetailsListener(OKButton,detailsFrame);
            detailsFrame.pack();
        }
        
        /**************************************************
         * class : MyTableModel, tablemodel for the Jtree *
         *************************************************/
        
        class MyTableModel extends AbstractTableModel{
            String[] columnNames = {"Tag","Property"};
            public String getColumnName(int col) {
                return columnNames[col].toString();
            }
            public int getRowCount() { return rowData.length; }
            public int getColumnCount() { return 2; }
            public Object getValueAt(int row, int col) {
                Object Obj = new Object();
                Obj = rowData[rowData.length-row-1][col];
                return  (Obj);
            }
            public boolean isCellEditable(int row, int col){
                return false;
            }
            
            public void setValueAt(Object value, int row, int col) {
                rowData[row][col] = value;
                fireTableCellUpdated(row, col);
            }
        }
        /************************************************************************
         * method : updateNodes, updates the nodes of the Jtree with data from  *
         * Scanifno structure --> updating to show all the nodes (general view) *
         ***********************************************************************/
        
        private void updateNodes(DefaultMutableTreeNode rootnode) {
            DefaultMutableTreeNode recordings = null;
            DefaultMutableTreeNode lasers = null;
            DefaultMutableTreeNode tracks = null;
            DefaultMutableTreeNode markers = null;
            DefaultMutableTreeNode timers = null;
            DefaultMutableTreeNode det = null;
            DefaultMutableTreeNode mar = null;
            DefaultMutableTreeNode ill = null;
            DefaultMutableTreeNode bsp = null;
            DefaultMutableTreeNode dat = null;
            DefaultMutableTreeNode unk = null;
            // Updating the nodes
            rootnode.removeAllChildren();
            recordings = new DefaultMutableTreeNode(new InfoBlock("Recordings",1,0));
            unk = new DefaultMutableTreeNode(new InfoBlock("Unknown tags",11,0));
            lasers = new DefaultMutableTreeNode("Lasers");
            tracks = new DefaultMutableTreeNode("Tracks");
            markers = new DefaultMutableTreeNode("Markers");
            timers =  new DefaultMutableTreeNode("Timers");
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(recordings);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(lasers);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(tracks);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(markers);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(timers);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(unk);
            int det_inc = 0;
            int ill_inc = 0;
            int bsp_inc = 0;
            int dat_inc = 0;
            for (int i=0;i<lsm_fi.SCANINFO.tracks.size();i++){
                
                tracks.add(new DefaultMutableTreeNode("Track "+(i+1)));
                
                ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode(new InfoBlock("General Track Data",10,i)));
                
                if (((Integer)lsm_fi.SCANINFO.det_channels_count.elementAt(i)).intValue()==0){
                    ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode(new InfoBlock("Detection Channels ",9,0)));
                    
                } else{
                    
                    ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode("Detection Channels"));
                    for (int j=0;j<((Integer)lsm_fi.SCANINFO.det_channels_count.elementAt(i)).intValue();j++){
                        det = new DefaultMutableTreeNode(new InfoBlock("Detection Channel "+(j+1),2,det_inc++));
                        ((DefaultMutableTreeNode)tracks.getChildAt(i).getChildAt(1)).add(det);
                    }
                }
                
                if (((Integer)lsm_fi.SCANINFO.illum_channels_count.elementAt(i)).intValue()==0){
                    ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode(new InfoBlock("Illumination Channels ",9,0)));
                } else{
                    ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode("Illumination Chanels"));
                    for (int j=0;j<((Integer)lsm_fi.SCANINFO.illum_channels_count.elementAt(i)).intValue();j++){
                        ill=new DefaultMutableTreeNode(new InfoBlock("Illumination Channel "+(j+1),3,ill_inc++));
                        ((DefaultMutableTreeNode)tracks.getChildAt(i).getChildAt(2)).add(ill);
                    }
                }
                
                if (((Integer)lsm_fi.SCANINFO.bsplits_count.elementAt(i)).intValue()==0){
                    ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode(new InfoBlock("Beam Splitters ",9,0)));
                } else{
                    ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode("Beam Splitters"));
                    for (int j=0;j<((Integer)lsm_fi.SCANINFO.bsplits_count.elementAt(i)).intValue();j++){
                        bsp=new DefaultMutableTreeNode(new InfoBlock("Beam splitter "+(j+1),4,bsp_inc++));
                        ((DefaultMutableTreeNode)tracks.getChildAt(i).getChildAt(3)).add(bsp);
                    }
                }
                
                if (((Integer)lsm_fi.SCANINFO.data_channels_count.elementAt(i)).intValue()==0){
                    ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode(new InfoBlock("Data Channels ",9,0)));
                } else{
                    ((DefaultMutableTreeNode)tracks.getChildAt(i)).add(new DefaultMutableTreeNode("Data Channels"));
                    for (int j=0;j<((Integer)lsm_fi.SCANINFO.data_channels_count.elementAt(i)).intValue();j++){
                        dat=new DefaultMutableTreeNode(new InfoBlock("Data Channel "+(j+1),5,dat_inc++));
                        ((DefaultMutableTreeNode)tracks.getChildAt(i).getChildAt(4)).add(dat);
                    }
                }
            }
            
            for (int i=0;i<lsm_fi.SCANINFO.markers.size();i++){
                markers.add(new DefaultMutableTreeNode(new InfoBlock("Marker "+(i+1),6,i)));
            }
            for (int i=0;i<lsm_fi.SCANINFO.timers.size();i++){
                timers.add(new DefaultMutableTreeNode(new InfoBlock("Timer "+(i+1),7,i)));
            }
            for (int i=0;i<lsm_fi.SCANINFO.lasers.size();i++){
                lasers.add(new DefaultMutableTreeNode(new InfoBlock("Laser "+(i+1),8,i)));
            }
            // dispatching of the messages if one clicks on the different parts of the JTree
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    tree.getLastSelectedPathComponent();
                    if (node == null) return;
                    Object nodeInfo = node.getUserObject();
                    if (node.isLeaf()) {
                        InfoBlock info = (InfoBlock)nodeInfo;
                        if (info.datatype==1)
                            rowData = getObject(lsm_fi.SCANINFO.recordings);
                        if (info.datatype==2)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.det_channels.elementAt(info.index));
                        if (info.datatype==3)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.illum_channels.elementAt(info.index));
                        if (info.datatype==4)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.bsplits.elementAt(info.index));
                        if (info.datatype==5)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.data_channels.elementAt(info.index));
                        if (info.datatype==6)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.markers.elementAt(info.index));
                        if (info.datatype==7)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.timers.elementAt(info.index));
                        if (info.datatype==8)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.lasers.elementAt(info.index));
                        if (info.datatype==9)
                            rowData = undefinedData;
                        if (info.datatype==10)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.tracks.elementAt(info.index));
                        if (info.datatype==11)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.unknown);
                    } else
                        rowData = noData;
                    tablemodel.fireTableDataChanged();
                }
            }
            );
            treemodel.reload();
            expandEntireTree(rootnode);
            switcher = true;
        }
        
        /************************************************************************
         * method : updateNodes2, updates the nodes of the Jtree with data from *
         * Scanifno structure   --> update nodes to show only the channels used *
         ***********************************************************************/
        
        private void updateNodes2(DefaultMutableTreeNode rootnode) {
            DefaultMutableTreeNode recordings = null;
            DefaultMutableTreeNode lasers = null;
            DefaultMutableTreeNode tracks = null;
            DefaultMutableTreeNode markers = null;
            DefaultMutableTreeNode timers = null;
            DefaultMutableTreeNode det = null;
            DefaultMutableTreeNode mar = null;
            DefaultMutableTreeNode ill = null;
            DefaultMutableTreeNode bsp = null;
            DefaultMutableTreeNode dat = null;
            DefaultMutableTreeNode unk = null;
            // updating the nodes
            rootnode.removeAllChildren();
            recordings = new DefaultMutableTreeNode(new InfoBlock("Recordings",1,0));
            unk = new DefaultMutableTreeNode(new InfoBlock("Unknown tags",11,0));
            lasers = new DefaultMutableTreeNode("Lasers");
            tracks = new DefaultMutableTreeNode("Tracks");
            markers = new DefaultMutableTreeNode("Markers");
            timers =  new DefaultMutableTreeNode("Timers");
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(recordings);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(lasers);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(tracks);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(markers);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(timers);
            ((DefaultMutableTreeNode)treemodel.getRoot()).add(unk);
            
            int det_inc = 0;
            int ill_inc = 0;
            int bsp_inc = 0;
            int dat_inc = 0;
            int det_count = 0;

            for (int i=0;i<lsm_fi.SCANINFO.tracks.size();i++){
                if (((Integer)lsm_fi.SCANINFO.det_channels_count.elementAt(i)).intValue()!=0){
                    tracks.add(new DefaultMutableTreeNode("Track "+(i+1)));
                    ((DefaultMutableTreeNode)tracks.getLastChild()).add(new DefaultMutableTreeNode(new InfoBlock("General Track Data",10,i)));
                    ((DefaultMutableTreeNode)tracks.getLastChild()).add(new DefaultMutableTreeNode("Detection Channels"));
                    for (int j=0;j<((Integer)lsm_fi.SCANINFO.det_channels_count.elementAt(i)).intValue();j++){
                            if (check_on((ArrayList)lsm_fi.SCANINFO.det_channels.elementAt(det_inc),97)) {
                                det = new DefaultMutableTreeNode(new InfoBlock("Detection Channel "+(j+1),2,det_inc));
                                ((DefaultMutableTreeNode)tracks.getLastChild().getChildAt(1)).add(det);
                            }
                            det_inc++;
                    }
                    int ch=0;
                    int idx = 0;
                  
                    DefaultMutableTreeNode ill_main = new DefaultMutableTreeNode("Illumination Chanels");
                    ((DefaultMutableTreeNode)tracks.getLastChild()).add(ill_main);
                    idx = ((DefaultMutableTreeNode)ill_main.getParent()).getIndex(ill_main);
                    for (int j=0;j<((Integer)lsm_fi.SCANINFO.illum_channels_count.elementAt(i)).intValue();j++){
                            if (check_on((ArrayList)lsm_fi.SCANINFO.illum_channels.elementAt(ill_inc),119)) {
                                    ill=new DefaultMutableTreeNode(new InfoBlock("Illumination Channel "+(j+1),3,ill_inc));
                                    ((DefaultMutableTreeNode)tracks.getLastChild().getChildAt(2)).add(ill);
                                    ch++;
                            }
                            ill_inc++;
                    }
                    
                    DefaultMutableTreeNode tracksn = (DefaultMutableTreeNode)tracks.getLastChild();
                    DefaultMutableTreeNode tracksn2 = ((DefaultMutableTreeNode)tracks.getLastChild().getChildAt(2));
                    if (ch==0)  tracksn2.add(new DefaultMutableTreeNode(new InfoBlock("N/A",12,0)));
                    if (((Integer)lsm_fi.SCANINFO.bsplits_count.elementAt(i)).intValue()!=0){
                        ((DefaultMutableTreeNode)tracks.getLastChild()).add(new DefaultMutableTreeNode("Beam Splitters"));
                        for (int j=0;j<((Integer)lsm_fi.SCANINFO.bsplits_count.elementAt(i)).intValue();j++){
                            bsp=new DefaultMutableTreeNode(new InfoBlock("Beam splitter "+(j+1),4,bsp_inc++));
                            ((DefaultMutableTreeNode)tracks.getLastChild().getChildAt(3)).add(bsp);
                        }
                    }
                    if (((Integer)lsm_fi.SCANINFO.data_channels_count.elementAt(i)).intValue()!=0){
                        ((DefaultMutableTreeNode)tracks.getLastChild()).add(new DefaultMutableTreeNode("Data Channels"));
                        for (int j=0;j<((Integer)lsm_fi.SCANINFO.data_channels_count.elementAt(i)).intValue();j++){
                            dat=new DefaultMutableTreeNode(new InfoBlock("Data Channel "+(j+1),5,dat_inc++));
                            ((DefaultMutableTreeNode)tracks.getLastChild().getChildAt(4)).add(dat);
                        }
                    }
                }
                if (((Integer)lsm_fi.SCANINFO.det_channels_count.elementAt(i)).intValue()==0)  {
                    ill_inc = ill_inc + ((Integer)lsm_fi.SCANINFO.illum_channels_count.elementAt(i)).intValue() ;
                    
                }
           }
            
            for (int i=0;i<lsm_fi.SCANINFO.markers.size();i++){
                markers.add(new DefaultMutableTreeNode(new InfoBlock("Marker "+(i+1),6,i)));
            }
            for (int i=0;i<lsm_fi.SCANINFO.timers.size();i++){
                timers.add(new DefaultMutableTreeNode(new InfoBlock("Timer "+(i+1),7,i)));
            }
            for (int i=0;i<lsm_fi.SCANINFO.lasers.size();i++){
                if (check_on((ArrayList)lsm_fi.SCANINFO.lasers.elementAt(i),18))
                lasers.add(new DefaultMutableTreeNode(new InfoBlock("Laser "+(i+1),8,i)));
            }
            
            tree.addTreeSelectionListener(new TreeSelectionListener() {
                public void valueChanged(TreeSelectionEvent e) {
                    DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                    tree.getLastSelectedPathComponent();
                    if (node == null) return;
                    Object nodeInfo = node.getUserObject();
                    if (node.isLeaf()) {
                        InfoBlock info = (InfoBlock)nodeInfo;
                        if (info.datatype==1)
                            rowData = getObject(lsm_fi.SCANINFO.recordings);
                        if (info.datatype==2)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.det_channels.elementAt(info.index));
                        if (info.datatype==3)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.illum_channels.elementAt(info.index));
                        if (info.datatype==4)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.bsplits.elementAt(info.index));
                        if (info.datatype==5)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.data_channels.elementAt(info.index));
                        if (info.datatype==6)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.markers.elementAt(info.index));
                        if (info.datatype==7)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.timers.elementAt(info.index));
                        if (info.datatype==8)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.lasers.elementAt(info.index));
                        if (info.datatype==9)
                            rowData = undefinedData;
                        if (info.datatype==10)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.tracks.elementAt(info.index));
                        if (info.datatype==11)
                            rowData = getObject((ArrayList)lsm_fi.SCANINFO.unknown);
                        if (info.datatype==12)
                            rowData = noData;
                    } else
                        rowData = noData;
                    tablemodel.fireTableDataChanged();
                }
            }
            );
            treemodel.reload();
            expandEntireTree(rootnode);
            switcher = false;
        }
        
        /**********************************************************
         * method : getObject, gets an 2D Object from a hashtable *
         *********************************************************/
        
        public Object[][] getObject(ArrayList in){
            Object[][] rD = new Object[in.size()][2];
            for (int i=0;i<in.size();i++){
                Object[] obj = new Object[2];
                obj = ((Object[])in.get(i));
                int itmp = 0;
                if (obj[0] != null) itmp = ((Integer)obj[0]).intValue();
                if (obj[1] instanceof String){
                    String tmp =  (String)obj[1];
                    rD[i][1]=tmp;
                }
                if (obj[1] instanceof Double){
                    Double tmp =  (Double)obj[1];
                    rD[i][1]=tmp;
                }
                if (obj[1] instanceof Long){
                    Long tmp =  (Long)obj[1];
                    rD[i][1]=tmp;
                }
                
                if (obj[0] != null) rD[i][0]=lsm_fi.SCANINFO.table[itmp][3];
                else rD[i][0]="UNKNOWN TAG";
                
            }
            return rD;
        }
        
        /***********************************************************************************
         * method : check_on, check if an arraylist from a detection channel is on *
         **********************************************************************************/
        
        public boolean check_on(Object in,int val){
            boolean temp = false;
            if (in instanceof ArrayList)
                if ((ArrayList)in!=null)
                 for (int i=0;i<((ArrayList)in).size();i++){
                    Object[] obj = new Object[2];
                    obj = ((Object[])((ArrayList)in).get(i));
                    if (((Integer)obj[0]).intValue() == val)
                        if (((Long)obj[1]).intValue() == -1) temp = true;
                }
             if (in instanceof Vector)
             if ((Vector)in!=null)
             for (int i=0;i<((Vector)in).size();i++){
                    Object[] obj = new Object[2];
                    obj = ((Object[])((Vector)in).get(i));
                    if (((Integer)obj[0]).intValue() == val)
                        if (((Long)obj[1]).intValue() == -1) temp = true;
                }
            return temp;
        }
        
        /*********************************************************************************
         * method : translate, translates from indexed hashtable to tagged hashtable for *
         * property viewer                                                               *
         ********************************************************************************/
        
        public Hashtable translate(Hashtable hashin){
            Hashtable hashout = new Hashtable();
            
            for (int i=0;i<147;i++)
                if (hashin.containsKey(new Integer(i)))
                    hashout.put(lsm_fi.SCANINFO.table[i][3],hashin.get(new Integer(i)));
            return hashout;
            
        }
        
        /****************************************************************
         * method group : expandTree,expandEntireTree, self-explanatory *
         ***************************************************************/
        
        public void expandTree() {
            expandEntireTree((DefaultMutableTreeNode) treemodel.getRoot());
        }
        
        private void expandEntireTree(DefaultMutableTreeNode tNode) {
            TreePath tp=new TreePath(((DefaultMutableTreeNode)tNode).getPath());
            tree.expandPath(tp);
            
            for(int i = 0; i < tNode.getChildCount(); i++) {
                expandEntireTree((DefaultMutableTreeNode)tNode.getChildAt(i));
            }
        }
        
        /*****************************************
         * class : InfoBlock used with the Jtree *
         ****************************************/
        
        private class InfoBlock {
            public int datatype = 0;
            public int index = 0;
            String leafName;
            public InfoBlock(String name,int type,int num) {
                leafName = name;
                datatype = type;
                index = num;
            }
            public String toString() {
                return leafName;
            }
        }
        /** sets the current lsm info text */
        public void setLSMinfoText(String[] str) {
            LSMinfoText = str;
        }
        /** sets the current Stamps data*/
        public void setLSMFileInfo(LSMFileInfo l) {
            lsm_fi = l;
        }
        
        /*****************************************************************************
         * method printNFO : simply converts numbers into strings using ImageJ d2s() *
         * method and creates an ImageJ textwindow to display properly the infos.    *
         ****************************************************************************/
        
        public String[] printINFO(){
            String stacksize = IJ.d2s(lsm_fi.DIMENSION_Z, 0);
            String width = IJ.d2s(lsm_fi.TIF_IMAGEWIDTH, 0);
            String height = IJ.d2s(lsm_fi.TIF_IMAGELENGTH, 0);
            String channels = IJ.d2s(lsm_fi.NUMBER_OF_CHANNELS, 0);
            String scantype = "";
            int scan = (int) lsm_fi.SCANTYPE;
            switch (scan) {
                case 0:
                    scantype = "Normal X-Y-Z scan";
                    break;
                case 1:
                    scantype = "Z scan";
                    break;
                case 2:
                    scantype = "Line scan";
                    break;
                case 3:
                    scantype = "Time series X-Y";
                    break;
                case 4:
                    scantype = "Time series X-Z";
                    break;
                case 5:
                    scantype = "Time series - Means of ROIs";
                    break;
                case 6:
                    scantype = "Time series X-Y-Z";
                    break;
                default :
                    scantype = "UNKNOWN !";
                    break;
            }
            
            String voxelsize_x = IJ.d2s(lsm_fi.VOXELSIZE_X*1000000, 2) + " µm";
            String voxelsize_y = IJ.d2s(lsm_fi.VOXELSIZE_Y*1000000, 2) + " µm";
            String voxelsize_z = IJ.d2s(lsm_fi.VOXELSIZE_Z*1000000, 2) + " µm";
            String timestacksize = IJ.d2s(lsm_fi.TIMESTACKSIZE, 0);
            String plane_spacing = IJ.d2s(lsm_fi.PLANE_SPACING, 2) + " µm";
            String plane_width = IJ.d2s(lsm_fi.PLANE_WIDTH, 2) + " µm";
            String plane_height = IJ.d2s(lsm_fi.PLANE_HEIGHT, 2) + " µm";
            String volume_depth = IJ.d2s(lsm_fi.VOLUME_DEPTH, 2) + " µm";
            String channel_names = "";
            
            infos[0] = lsm_fi.FILENAME ;
            infos[1] = lsm_fi.User;
            infos[2] = width;
            infos[3] = height;
            infos[4] = channels;
            infos[5] = stacksize;
            infos[6] = timestacksize;
            infos[7] = scantype;
            infos[8] = voxelsize_x;
            infos[9] = voxelsize_y;
            infos[10] = voxelsize_z;
            infos[11] = lsm_fi.Objective;
            infos[12] = IJ.d2s(lsm_fi.ZOOM_X, 2);
            infos[13] = IJ.d2s(lsm_fi.ZOOM_Y, 2);
            infos[14] = IJ.d2s(lsm_fi.ZOOM_Z, 2);
            infos[15] = plane_width;
            infos[16] = plane_height;
            infos[17] = volume_depth;
            infos[18] = plane_spacing;
            return infos;
        }
        
        /***********************************************************************
         * method : dumpData dumps the SCANINFO STRUCTURE data in a textwindow *
         **********************************************************************/
        
        public void dumpData(){
            String header = new String("Offset\tLevel\tTag\tName\tSize (bytes)\tValue\t");
            TextWindow tw = new TextWindow("SCANINFO DUMP",header,lsm_fi.sb.toString(), 250, 400);
        }
        
        /**************************************************************************
         * method group : actionlisteners for the GUI, attach functions to button *
         * press                                                                  *
         *************************************************************************/
        
        /** Adds the dispose() function to a JButton in a JFrame */
        private void addExitListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ij.WindowManager.closeAllWindows();
                    closeFrames();
                }
            });
        }
        
        /** Adds the OpenLSLM() function to a JButton in a JFrame */
        private void addOpenListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LSM_Reader_ reader = new LSM_Reader_();
                    reader.OpenLSM("");
                    setLSMFileInfo(reader.lsm_fi);
                    setLSMinfoText(printINFO());
                    updateInfoFrame(LSMinfoText);
                }
            });
        }
        
        
         /** Adds the BatchConvert() function to a JButton in a JFrame */
        private void addBatchConvertListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Batch_Converter bat = new Batch_Converter();
                    bat.run("");
                    ij.WindowManager.closeAllWindows();
                }
            });
        }
        
        /** Adds the "close all image windows" function to a JButton in a JFrame */
        private void addCloseWinListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ij.WindowManager.closeAllWindows();
                    setLSMinfoText(new String[22]);
                    infoFrame.dispose();
                    detailsFrame.dispose();
                }
            });
        }
        
        /** Gathers all channels in a single Image Window or Expands them */
        private void addGatherExpandlistener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (button.getText() == " Gather ") {
                        button.setText(" Expand ");
                        button.setToolTipText("Split all the channels in separated windows");
                    }
                    else {
                        button.setText(" Gather ");
                        button.setToolTipText("Show all channels in the same window");
                    }
                }
            });
        }
        
        /** Adds the dispose() function to a JButton in a JFrame */
        private void addShowHideInfolistener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    updateInfoFrame(LSMinfoText);
                    if (infoFrame.isShowing()== false)
                        infoFrame.show();
                    else
                        infoFrame.hide();
                }
            });
        }
        
        /** Adds the "apply z-stamp" function to a JButton in a JFrame */
        private void addApplyZStampListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String[] choices = {"Dump to textfile","Apply to image"};
                    GenericDialog gd = new GenericDialog("Z-stamps");
                    gd.addChoice("Stamps destination : ", choices, "Apply to image");
                    gd.showDialog();
                    if (gd.wasCanceled()){
			return;
                    }				
                    String choice = gd.getNextChoice();
                    if (choice.equals("Dump to textfile")){
                        String twstr = new String("");
                        double ps=0;
                        for (int i=1; i<=lsm_fi.DIMENSION_Z; i++) {
                            String s = IJ.d2s(ps, 2) + " µm";
                            ps += lsm_fi.PLANE_SPACING; //moved from line -2
                            twstr = twstr+s+"\n";
                        }
                        TextWindow tw = new TextWindow("Z-stamps","Z-stamps",twstr,200,400);
                    
                    } else{
                        ImagePlus imp = WindowManager.getCurrentImage();
                        applyZSTAMP(imp,lsm_fi);
                    }
                }
            });
        }
        
        /** Adds the "apply t-stamp" function to a JButton in a JFrame */
        private void addApplyTStampListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String[] choices = {"Dump to textfile","Apply to image"};
                    GenericDialog gd = new GenericDialog("Timestamps");
                    gd.addChoice("Stamps destination : ", choices, "Apply to image");
                    gd.showDialog();
                    if (gd.wasCanceled()){
			return;
                    }				
                    String choice = gd.getNextChoice();
                    if (choice.equals("Dump to textfile")){
                        String twstr = new String("");
                        for (int i=0;i<lsm_fi.TS_COUNT;i++)
                            twstr = twstr+Double.toString(lsm_fi.TS_STAMPS[i])+"\n";
                            TextWindow tw = new TextWindow("Timestamps","Timestamps",twstr,200,400);
                    } else{
                        ImagePlus imp = WindowManager.getCurrentImage();
                        applyTSTAMP(imp,lsm_fi);
                    }
                }
            });
        }
        
        /** Adds the "apply lambda-stamp" function to a JButton in a JFrame */
        private void addApplyLambdaStampListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    ImagePlus imp = WindowManager.getCurrentImage();
                    if (lsm_fi.SPECTRALSCAN!=1){
                        IJ.error("Image not issued from spectral scan. Lambda stamp obsolete!");
                        return;
                    }
                    ////
                    String[] choices = {"Dump to textfile","Apply to image"};
                    GenericDialog gd = new GenericDialog("Lambdastamps");
                    gd.addChoice("Stamps destination : ", choices, "Apply to image");
                    gd.showDialog();
                    if (gd.wasCanceled()){
			return;
                    }				
                    String choice = gd.getNextChoice();
                    if (choice.equals("Dump to textfile")){
                        String twstr = new String("");
                        for (int i=0;i<lsm_fi.LS_COUNT;i++)
                            twstr = twstr+Double.toString(lsm_fi.LS_STAMPS[i])+"\n";
                        
                        TextWindow tw = new TextWindow("Lambdastamps","Lambdastamps",twstr,200,400);
                    
                    } else{
                        YesNoCancelDialog ync = new YesNoCancelDialog(baseFrame,"Lambda Stack creation and stamping",
                        "LSM_Reader will merge ALL open images into a stack and merge them.\n"+
                        "If images from different sources are open, do close them and restart this operation!\n"+
                        "If the images are hypervolumes, do not use lambda stamp!");
                        if (ync.yesPressed() == true) applyLSTAMP(imp,lsm_fi);
                    }
                }
            });
        }
        
        /** Adds the "Dump data" function to a JButton in a JFrame */
        private void addDumpDataListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dumpData();
                }
            });
        }
        
        /** Adds the "browse" function to a JButton in a JFrame */
        private void addBrowseListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    IJ.runPlugIn("HyperVolume_Browser","3D=z 3DV="+lsm_fi.DIMENSION_Z+" 4D=t");
                }
            });
        }
        
        /** Adds the "Lut panel" function to a JButton in a JFrame */
        private void addLUTListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    IJ.runPlugIn("Lut_Panel","");
                }
            });
        }
        
        /** Adds the "notes" function to a JButton in a JFrame */
        private void addNotesButtonListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    Notes notesFrame = new Notes(infoFrame);
                }
            });
        }
        
        /** Adds the "switch" function to a JButton in a JFrame */
        private void addSwitchListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (switcher == true) {
                        updateNodes2(rootnode);
                        SwitchButton.setText("Switch to general View");
                    }
                    else {
                        updateNodes(rootnode);
                        SwitchButton.setText("Switch to filtered View");
                    }
                }
            });
        }
        
        /** Adds the "More details..." function to a JButton in a JFrame */
        private void addDetailsListener(final JButton button, final JFrame parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    if (detailsFrame.isShowing()== false){
                        updateNodes2(rootnode);
                        detailsFrame.show();
                    }
                    else
                        detailsFrame.hide();
                }
            });
        }
    }
    
    /***************************************************************
     * class GUIButton : this class overrides the standard JButton *
     * Class to fit the GUI needs                                  *
     **************************************************************/
    
    class GUIButton extends JButton {
        
        public GUIButton(String buttonText, String tooltipText) {
            Font dafont = new Font(null);
            float fontsize = 11;
            dafont = dafont.deriveFont(fontsize);
            dafont = dafont.deriveFont(Font.BOLD);
            this.setFont(dafont);
            this.setText(buttonText);
            this.setForeground(SystemColor.windowText);
            this.setToolTipText(tooltipText);
        }
    }
    
    /***********************************************************
     * method : applyZSTAMP, applies depth(Z) stamps on stacks *
     **********************************************************/
    
    public void applyZSTAMP(ImagePlus imp,LSMFileInfo lfi){
        int x = 2;
        int y = 40;
        double ps = 0;
        ImageStack stack = imp.getStack();
        Font font = new Font("SansSerif", Font.PLAIN, 20);
        ImageProcessor ip = imp.getProcessor();
        Rectangle roi = ip.getRoi();
        if (roi.width<ip.getWidth() || roi.height<ip.getHeight()) {
            x = roi.x;
            y = roi.y+roi.height;
        }
        Color c= Toolbar.getForegroundColor();
        if (lfi.DIMENSION_Z!=1)
            if ((lfi.TIMESTACKSIZE==1)){
                for (int slice=1; slice<=lfi.DIMENSION_Z; slice++) {
                    IJ.showStatus("MinMax: "+slice+"/"+lfi.DIMENSION_Z);
                    //ps += lfi.PLANE_SPACING;
                    String s = IJ.d2s(ps, 2) + " µm";
                    ps += lfi.PLANE_SPACING; //moved from line -2
                    ip = stack.getProcessor(slice);
                    ip.setFont(font);
                    float[] hsb =Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
                    ip.setColor(c.getHSBColor(255,255,hsb[2]));
                    ip.moveTo(x, y);
                    ip.drawString(s);
                }
            } else
                if (lfi.TIMESTACKSIZE!=1){
                    for (int slicez=1; slicez<=lfi.DIMENSION_Z; slicez++) {
                        IJ.showStatus("MinMax: "+slicez+"/"+lfi.DIMENSION_Z);
                        //ps += lfi.PLANE_SPACING;
                        String s = IJ.d2s(ps, 2) + " µm";
                        ps += lfi.PLANE_SPACING; //moved from line -2
                        for (int slicet=1;slicet<=lfi.TIMESTACKSIZE;slicet++){
                            ip = stack.getProcessor((int)lfi.DIMENSION_Z*(slicet-1)+slicez);
                            ip.setFont(font);
                            float[] hsb =Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
                            ip.setColor(c.getHSBColor(255,255,hsb[2]));
                            ip.moveTo(x, y);
                            ip.drawString(s);
                        }
                    }
                }
        imp.updateAndRepaintWindow();
    }
    
    /**************************************************
     * method : applyTSTAMP, applies stamps on stacks *
     *************************************************/
    
    public void applyTSTAMP(ImagePlus imp,LSMFileInfo lfi){
        int x = 2;
        int y = 20;
        
        ImageStack stack = imp.getStack();
        Font font = new Font("SansSerif", Font.PLAIN, 20);
        ImageProcessor ip = imp.getProcessor();
        Rectangle roi = ip.getRoi();
        if (roi.width<ip.getWidth() || roi.height<ip.getHeight()) {
            x = roi.x;
            y = roi.y+roi.height;
        }
        Color c= Toolbar.getForegroundColor();
        if ((lfi.TIMESTACKSIZE==1) ^ (lfi.DIMENSION_Z==1)){
            int size = imp.getStackSize();
            /*if (size!=lfi.TS_COUNT) {
                IJ.error("Wrong stack size! File is probably corrupt!");
                IJ.error(Integer.toString(size)+ " " + Long.toString(lfi.TS_COUNT));
                return;
            }*/
            for (int slice=1; slice<=lfi.TS_COUNT; slice++) {
                IJ.showStatus("MinMax: "+slice+"/"+lfi.TS_COUNT);
                String s = IJ.d2s(lfi.TS_STAMPS[slice-1], 2) + " s";
                ip = stack.getProcessor(slice);
                ip.setFont(font);
                float[] hsb =Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
                ip.setColor(c.getHSBColor(255,255,hsb[2]));
                ip.moveTo(x, y);
                ip.drawString(s);
            }
        } else
            if ((lfi.TIMESTACKSIZE!=1) && (lfi.DIMENSION_Z!=1)){
                for (int slicet=1; slicet<=lfi.TIMESTACKSIZE; slicet++) {
                    IJ.showStatus("MinMax: "+slicet+"/"+lfi.TIMESTACKSIZE);
                    String s = IJ.d2s(lfi.TS_STAMPS[slicet-1], 2) + " s";
                    for (int slicez=1;slicez<=lfi.DIMENSION_Z;slicez++){
                        ip = stack.getProcessor((int)lfi.DIMENSION_Z*(slicet-1)+slicez);
                        ip.setFont(font);
                        float[] hsb =Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
                        ip.setColor(c.getHSBColor(255,255,hsb[2]));
                        ip.moveTo(x, y);
                        ip.drawString(s);
                    }
                }
            }
        imp.updateAndRepaintWindow();
    }
    
    /*********************************************************************
     * method : applyLSTAMP, applies wavelength(lambda) stamps on stacks *
     ********************************************************************/
    
    public void applyLSTAMP(ImagePlus imp,LSMFileInfo lfi){
        int x = 2;
        int y = 60;
        double ps = 0;
        ImageProcessor ip = imp.getProcessor();
        Rectangle roi = ip.getRoi();
        if (roi.width<ip.getWidth() || roi.height<ip.getHeight()) {
            x = roi.x;
            y = roi.y+roi.height;
        }
        StackEditor se = new StackEditor();
        se.convertImagesToStack();
        imp = WindowManager.getCurrentImage();
        ImageStack stack = imp.getStack();
        Font font = new Font("SansSerif", Font.PLAIN, 20);
        Color c= Toolbar.getForegroundColor();
        if (stack.getSize()!=1 && lfi.SPECTRALSCAN==1){
            for (int slice=1; slice<=lfi.LS_COUNT; slice++) {
                IJ.showStatus("MinMax: "+slice+"/"+lfi.LS_COUNT);
                String s = IJ.d2s(lfi.LS_STAMPS[slice-1]*1000000000, 2) + " nm";
                ip = stack.getProcessor(slice);
                ip.setFont(font);
                float[] hsb =Color.RGBtoHSB(c.getRed(),c.getGreen(),c.getBlue(),null);
                ip.setColor(c.getHSBColor(255,255,hsb[2]));
                ip.moveTo(x, y);
                ip.drawString(s);
            }
        }
        imp.updateAndRepaintWindow();
    }
    
    /******************************************************************
     * class MyFocusListener : defines a focus listener for an image  *
     * to refresh the info panel and the details panel with the       *
     * correct info                                                   *
     *****************************************************************/
    
    class MyFocusListener extends WindowAdapter {
        
        ImagePlus current_imp;
        LSMFileInfo lfi;
        
        public MyFocusListener(LSMFileInfo l, ImagePlus imp){
            current_imp = imp;
            lfi = new LSMFileInfo();
            lfi = l;
        }
        
        public void windowActivated(WindowEvent e) {
            // The component gains the focus
            theGUI.lsm_fi = lfi;
            if (theGUI.lsm_fi.TIMESTACKSIZE<=1) theGUI.butt5.setEnabled(false);else theGUI.butt5.setEnabled(true);
            if (theGUI.lsm_fi.DIMENSION_Z<=1) theGUI.butt7.setEnabled(false);else theGUI.butt7.setEnabled(true);
            if (theGUI.lsm_fi.SPECTRALSCAN==0)
                theGUI.butt8.setEnabled(false);else if (theGUI.lsm_fi.LS_COUNT>=1) theGUI.butt8.setEnabled(true);
            theGUI.updateInfoFrame(theGUI.printINFO());
            theGUI.updateNodes(theGUI.rootnode);
            theGUI.tree.clearSelection();
            theGUI.tree.collapseRow(1);
            if (theGUI.switcher == true) theGUI.SwitchButton.setText("Switch to general view");
            else theGUI.SwitchButton.setText("Switch to filtered view");
        }
        
        public void windowLostFocus(WindowEvent e){
            // The component lost the focus
            theGUI.lsm_fi = lfi;
            if (theGUI.lsm_fi.TIMESTACKSIZE==1) theGUI.butt5.setEnabled(false);else theGUI.butt5.setEnabled(true);
            if (theGUI.lsm_fi.DIMENSION_Z==1) theGUI.butt7.setEnabled(false);else theGUI.butt7.setEnabled(true);
            if (theGUI.lsm_fi.SPECTRALSCAN==1 && theGUI.lsm_fi.LS_COUNT>1) theGUI.butt8.setEnabled(false);else theGUI.butt8.setEnabled(true);
            theGUI.updateInfoFrame(theGUI.printINFO());
            theGUI.updateNodes(theGUI.rootnode);
            theGUI.tree.clearSelection();
            theGUI.tree.collapseRow(1);
            if (theGUI.switcher == true) theGUI.SwitchButton.setText("Switch to general view");
            else theGUI.SwitchButton.setText("Switch to filtered view");
        }
    }
    
    /************************************************
     * class : Notes,  notes panel                  *
     ***********************************************/
    
    public class Notes extends JDialog{
        public Notes(JFrame parent) {
            super(parent, "LSM Notes", true);
            Container c = this.getContentPane();
            c.setLayout(new BorderLayout());
            JLabel snotes = new JLabel("Short Notes :");
            JLabel dnotes = new JLabel("Detailed Notes :");
            JTextArea tsnotes = new JTextArea("");
            JTextArea tdnotes = new JTextArea("");
            tsnotes.setEditable(false);
            tsnotes.setEditable(false);
            tdnotes.setRows(4);
            tdnotes.setRows(4);
            tsnotes.setColumns(20);
            tdnotes.setColumns(20);
            JPanel datapanel = new JPanel();
            JPanel labelpanel = new JPanel();
            this.setSize(300,300);
            JPanel container = new JPanel();
            this.setLocation(300,300);
            container.setLayout(new GridLayout(2,2,0,2));
            container.add(snotes);
            container.add(tsnotes);
            container.add(dnotes);
            container.add(tdnotes);
            c.add(container,BorderLayout.NORTH);
            JButton okb = new JButton("Ok");
            addokbListener(okb, this);
            c.add(okb,BorderLayout.SOUTH);
            tsnotes.setText(theGUI.lsm_fi.ShortNotes);
            tdnotes.setText(theGUI.lsm_fi.DetailedNotes);
            pack();
            show();
        }
        
        /** Adds the "okbutton" function to a JButton in a JFrame */ //this okbutton is in the notes panel
        private void addokbListener(final JButton button, final JDialog parent) {
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    dispose();
                }
            });
        }
    }
    
    /**************************************************************************
     * class : LSM_Reader_, contains the actual LSM reading methods. This has *
     * been done in a frantic effort to separate file reading from GUI        *
     *************************************************************************/
    
    class LSM_Reader_ {
        
        /* header values */
        public String FILENAME = "";
        public String DIRECTORY = "";
        public File LSM;
        private RandomAccessFile file;
        private RandomAccessStream stream;
        public LSMFileInfo lsm_fi;
        
        public void LSM_Reader_() {
        }
        
        /*****************************************************************
         * method : OpenLSM, opens the LSM file and reads properties and *
         * image data                                                    *
         ****************************************************************/
        
        public void OpenLSM(String arg){
            
            /* Gets the filename and the directory via ImageJ methods */
            OpenDialog od = new OpenDialog("Open LSM image ... ", arg);
            FILENAME = od.getFileName();
            try{
                
                /* Clean exit of the program if no file selected */
                if (FILENAME == null){
                    IJ.error("no file selected");
                    return;
                }
                DIRECTORY = od.getDirectory();
                if (DIRECTORY == null){
                    IJ.error("no file selected");
                    return;
                }
                LSM = new File(DIRECTORY, FILENAME);
                file = new RandomAccessFile(LSM, "r");
                stream = new RandomAccessStream(file);
                
                /* Shows the "about" dialog if "about" is passed as argument */
                if (arg.equals("about")){
                    showAbout();
                }
                /* Tests if the selected file is a valid LSM 510 file */
                else{
                    lsm_fi = new LSMFileInfo();
                    lsm_fi.FILENAME=FILENAME;
                    lsm_fi.DIRECTORY=DIRECTORY;
                    boolean test = isLSMfile();
                    if (test){
                        
                        //////////////////////////////////////////////////////////////////////////////////////
                        long it = 0;
                        long nextoff=0;
                        int counter=0;
                        long pos=8;
                        byte[] tags;
                        do {
                            it = HowManyTAGs( (int)pos );
                            for (int k=0 ; k<it ; k++){
                                byte[] tags2 = readTAG(pos+2+12*k);
                                analyseTAG(tags2);
                            }
                            getSTRIPOFFSETS(lsm_fi.TIF_STRIPOFFSETS);
                            stream.seek( (int)pos + 2 + 12 * (int)it );
                            nextoff = swap( stream.readInt() );
                            pos = nextoff;
                            counter++;
                            if (lsm_fi.LENGTH2==1) lsm_fi.STRIPOFF.add( new Long( lsm_fi.TIF_STRIPOFFSETS  ) ); else
                                lsm_fi.STRIPOFF.add( new Long( lsm_fi.TIF_STRIPOFFSETS1  ) );
                            lsm_fi.IMAGETYPE.add( new Long( lsm_fi.TIF_NEWSUBFILETYPE ) );
                        } while (nextoff!=0);
                        
                        /* Searches for the number of tags in the first image directory */
                        long iterator1 = HowManyTAGs(8);
                        /* Analyses each tag found */
                        for (int k=0 ; k<iterator1 ; k++){
                            byte[] TAG1 = readTAG(10+12*k);
                            analyseTAG(TAG1);
                        }
                        if (lsm_fi.TIF_COMPRESSION==5) {
                            IJ.error("Image/J does not support compressed TIFF files");
                            //return;
                        }
                        /* Searches for the offsets to pixel data*/
                        getSTRIPOFFSETS(lsm_fi.TIF_STRIPOFFSETS);
                        /* Searches for the number of bytes used for a single pixel in each channel*/
                        getSTRIPBYTECOUNTS(lsm_fi.TIF_STRIPBYTECOUNTS);
                        /* Searches for infos located in the CZ-private TAG */
                        getCZ_LSMINFO(lsm_fi.TIF_CZ_LSMINFO);
                        /* Gets Channel names and colors */
                        getCHANNELNAMESANDCOLORS(lsm_fi.OFFSET_CHANNELSCOLORS);
                        /* Gets the LSM specific information located in the CZ-LSMinfo specific structure */
                        getSCANINFO(lsm_fi.OFFSET_SCANINFO);
                        /* Searches for infos located in the CZ-private TAG */
                        if (lsm_fi.OFFSET_TIMESTAMPS!=0) {
                            getTIMESTAMPS(lsm_fi.OFFSET_TIMESTAMPS);
                        }
                        if (lsm_fi.OFFSET_CHANNELWAVELENGTH!=0) {
                            getLAMBDASTAMPS(lsm_fi.OFFSET_CHANNELWAVELENGTH);
                        }
                        /* reads Input and output LUT. Those functions should work as far as I can see,
                         * but they don't. I think the specs from Zeiss are buggy on this. I commented
                         * these routines out because they are full of debug checks and not usable anyway
                         * as long as I don't have the propper specs
                         */
                        /* Searches for INPUT LUT */
                        /*if (lsm_fi.OFFSET_INPUTLUT!=0) {
                            getLUT(lsm_fi.OFFSET_INPUTLUT,lsm_fi.INPUTLUT);
                        }*/
                        /* Searches for OUTPUT LUT */
                        /*if (lsm_fi.OFFSET_OUTPUTLUT!=0) {
                            getLUT(lsm_fi.OFFSET_OUTPUTLUT,lsm_fi.OUTPUTLUT);
                        }*/
                        // Lut method call end
                        stream.close();
                        file.close();
                        
                        //int daOffset = (lsm_fi.DATATYPE == 2)?((int) (lsm_fi.TIF_IMAGEWIDTH*lsm_fi.TIF_IMAGELENGTH*2)):((int) (lsm_fi.TIF_IMAGEWIDTH*lsm_fi.TIF_IMAGELENGTH));
                        
                        for (int j = 0; j<(int)(lsm_fi.NUMBER_OF_CHANNELS); j++){
                            
                            ImagePlus imp = open(DIRECTORY, FILENAME,lsm_fi.STRIPOFF,j);
                            imp.setTitle(FILENAME + " Channel : " + lsm_fi.Channel[j]);
                            
                            if (imp != null ){
                                
                                Color[] gc= new Color[2];
                                gc[0] = new Color(0,0,0);
                                int r = (int)(lsm_fi.COLORS[j]&255);
                                int g = (int)((lsm_fi.COLORS[j]>>8)&255);
                                int b = (int)((lsm_fi.COLORS[j]>>16)&255);
                                gc[1] = new Color(r,g,b);
                                if (r==0 && g ==0 && b==0) gc[1]=Color.white;
                                apply_colors(imp,gc,2);
                                imp.getWindow().addWindowListener(new MyFocusListener(lsm_fi, imp)); 
                                if (lsm_fi.DATATYPE==2) {
                                    double min = imp.getProcessor().getMin();
                                    double max = imp.getProcessor().getMax();
                                    imp.getProcessor().setMinAndMax(min,max);
                                }
                                imp.show();
                                imp.updateAndDraw();
                            }
                            else {
                                IJ.showMessage("Open LSM...", "Failed.");
                            }
                        }
                    }
                    /****************************************************************
                     * Clean exit if selected file is not a valid LSM file          *
                     * NB : Here the term "valid" is given for any file matching    *
                     * the identifier regardless of the file version, even if       *
                     * this plug-in is written for LSM 510 version 2.8 to 3.2 only. *
                     ***************************************************************/
                    else {
                        IJ.error(" Selected image is not a valid LSM file ");
                        return;
                    }
                }
            }
            catch(IOException Open_LSM_exception){
                Open_LSM_exception.printStackTrace();
            }
        }

        /*****************************************************************
         * method : OpenLSM, opens the LSM file and reads properties and *
         * image data   ALTERNATE VERSION FOR USE TOGETHER WITH          *
         * "Handel_extra_file_type_plug-in"                              *
         ****************************************************************/
        
        
  public void OpenLSM(String arg1, String arg2){
            
            try{
                
                FILENAME = arg2;
                /* Clean exit of the program if no file selected */
                if (FILENAME == null){
                    IJ.error("no file selected");
                    return;
                }
                DIRECTORY = arg1;
                if (DIRECTORY == null){
                    IJ.error("no file selected");
                    return;
                }
                LSM = new File(DIRECTORY, FILENAME);
                file = new RandomAccessFile(LSM, "r");
                stream = new RandomAccessStream(file);
                
                /* Shows the "about" dialog if "about" is passed as argument */
                if (arg1.equals("about")){
                    showAbout();
                }
                /* Tests if the selected file is a valid LSM 510 file */
                else{
                    lsm_fi = new LSMFileInfo();
                    lsm_fi.FILENAME=FILENAME;
                    lsm_fi.DIRECTORY=DIRECTORY;
                    boolean test = isLSMfile();
                    if (test){
                        
                        //////////////////////////////////////////////////////////////////////////////////////
                        long it = 0;
                        long nextoff=0;
                        int counter=0;
                        long pos=8;
                        byte[] tags;
                        do {
                            it = HowManyTAGs( (int)pos );
                            for (int k=0 ; k<it ; k++){
                                byte[] tags2 = readTAG(pos+2+12*k);
                                analyseTAG(tags2);
                            }
                            getSTRIPOFFSETS(lsm_fi.TIF_STRIPOFFSETS);
                            stream.seek( (int)pos + 2 + 12 * (int)it );
                            nextoff = swap( stream.readInt() );
                            pos = nextoff;
                            counter++;
                            if (lsm_fi.LENGTH2==1) lsm_fi.STRIPOFF.add( new Long( lsm_fi.TIF_STRIPOFFSETS  ) ); else
                                lsm_fi.STRIPOFF.add( new Long( lsm_fi.TIF_STRIPOFFSETS1  ) );
                            lsm_fi.IMAGETYPE.add( new Long( lsm_fi.TIF_NEWSUBFILETYPE ) );
                        } while (nextoff!=0);
                        
                        /* Searches for the number of tags in the first image directory */
                        long iterator1 = HowManyTAGs(8);
                        /* Analyses each tag found */
                        for (int k=0 ; k<iterator1 ; k++){
                            byte[] TAG1 = readTAG(10+12*k);
                            analyseTAG(TAG1);
                        }
                        if (lsm_fi.TIF_COMPRESSION==5) {
                            IJ.error("Image/J does not support compressed TIFF files");
                            //return;
                        }
                        /* Searches for the offsets to pixel data*/
                        getSTRIPOFFSETS(lsm_fi.TIF_STRIPOFFSETS);
                        /* Searches for the number of bytes used for a single pixel in each channel*/
                        getSTRIPBYTECOUNTS(lsm_fi.TIF_STRIPBYTECOUNTS);
                        /* Searches for infos located in the CZ-private TAG */
                        getCZ_LSMINFO(lsm_fi.TIF_CZ_LSMINFO);
                        /* Gets Channel names and colors */
                        getCHANNELNAMESANDCOLORS(lsm_fi.OFFSET_CHANNELSCOLORS);
                        /* Gets the LSM specific information located in the CZ-LSMinfo specific structure */
                        getSCANINFO(lsm_fi.OFFSET_SCANINFO);
                        /* Searches for infos located in the CZ-private TAG */
                        if (lsm_fi.OFFSET_TIMESTAMPS!=0) {
                            getTIMESTAMPS(lsm_fi.OFFSET_TIMESTAMPS);
                        }
                        if (lsm_fi.OFFSET_CHANNELWAVELENGTH!=0) {
                            getLAMBDASTAMPS(lsm_fi.OFFSET_CHANNELWAVELENGTH);
                        }
                        /* reads Input and output LUT. Those functions should work as far as I can see,
                         * but they don't. I think the specs from Zeiss are buggy on this. I commented
                         * these routines out because they are full of debug checks and not usable anyway
                         * as long as I don't have the propper specs
                         */
                        /* Searches for INPUT LUT */
                        /*if (lsm_fi.OFFSET_INPUTLUT!=0) {
                            getLUT(lsm_fi.OFFSET_INPUTLUT,lsm_fi.INPUTLUT);
                        }*/
                        /* Searches for OUTPUT LUT */
                        /*if (lsm_fi.OFFSET_OUTPUTLUT!=0) {
                            getLUT(lsm_fi.OFFSET_OUTPUTLUT,lsm_fi.OUTPUTLUT);
                        }*/
                        // Lut method call end
                        stream.close();
                        file.close();
                        
                        //int daOffset = (lsm_fi.DATATYPE == 2)?((int) (lsm_fi.TIF_IMAGEWIDTH*lsm_fi.TIF_IMAGELENGTH*2)):((int) (lsm_fi.TIF_IMAGEWIDTH*lsm_fi.TIF_IMAGELENGTH));
                        
                        for (int j = 0; j<(int)(lsm_fi.NUMBER_OF_CHANNELS); j++){
                            
                            ImagePlus imp = open(DIRECTORY, FILENAME,lsm_fi.STRIPOFF,j);
                            imp.setTitle(FILENAME + " Channel : " + lsm_fi.Channel[j]);
                            
                            if (imp != null ){
                                
                                Color[] gc= new Color[2];
                                gc[0] = new Color(0,0,0);
                                int r = (int)(lsm_fi.COLORS[j]&255);
                                int g = (int)((lsm_fi.COLORS[j]>>8)&255);
                                int b = (int)((lsm_fi.COLORS[j]>>16)&255);
                                gc[1] = new Color(r,g,b);
                                if (r==0 && g ==0 && b==0) gc[1]=Color.white;
                                apply_colors(imp,gc,2);
                                imp.getWindow().addWindowListener(new MyFocusListener(lsm_fi, imp)); 
                                if (lsm_fi.DATATYPE==2) {
                                    double min = imp.getProcessor().getMin();
                                    double max = imp.getProcessor().getMax();
                                    imp.getProcessor().setMinAndMax(min,max);
                                }
                                imp.show();
                                imp.updateAndDraw();
                            }
                            else {
                                IJ.showMessage("Open LSM...", "Failed.");
                            }
                        }
                    }
                    /****************************************************************
                     * Clean exit if selected file is not a valid LSM file          *
                     * NB : Here the term "valid" is given for any file matching    *
                     * the identifier regardless of the file version, even if       *
                     * this plug-in is written for LSM 510 version 2.8 to 3.2 only. *
                     ***************************************************************/
                    else {
                        IJ.error(" Selected image is not a valid LSM file ");
                        return;
                    }
                }
            }
            catch(IOException Open_LSM_exception){
                Open_LSM_exception.printStackTrace();
            }
        }       
      
        
        
	  /*****************************************************************
         * method : OpenLSM2, opens the LSM file and reads properties and *
         * image data and send them to an array for batch conversion.     *
         ****************************************************************/
        
        
        public ImagePlus[] OpenLSM2(String arg1, String arg2){
            
            ArrayList tot = new ArrayList();
            
            /* Sets the filename and the directory */
            FILENAME = arg2;
            try{
                
                /* Clean exit of the program if no file selected */
                if (FILENAME == null){
                    IJ.error("no file selected");
                    return null;
                }
                DIRECTORY = arg1;
                if (DIRECTORY == null){
                    IJ.error("no file selected");
                    return null;
                }
                LSM = new File(DIRECTORY, FILENAME);
                file = new RandomAccessFile(LSM, "r");
                stream = new RandomAccessStream(file);
                
                /* Shows the "about" dialog if "about" is passed as argument */
                if (arg1.equals("about")){
                    showAbout();
                }
                /* Tests if the selected file is a valid LSM 510 file */
                else{
                    lsm_fi = new LSMFileInfo();
                    lsm_fi.FILENAME=FILENAME;
                    lsm_fi.DIRECTORY=DIRECTORY;
                    boolean test = isLSMfile();
                    if (test){
                        
                        //////////////////////////////////////////////////////////////////////////////////////
                        long it = 0;
                        long nextoff=0;
                        int counter=0;
                        long pos=8;
                        byte[] tags;
                        do {
                            it = HowManyTAGs( (int)pos );
                            for (int k=0 ; k<it ; k++){
                                byte[] tags2 = readTAG(pos+2+12*k);
                                analyseTAG(tags2);
                            }
                            getSTRIPOFFSETS(lsm_fi.TIF_STRIPOFFSETS);
                            stream.seek( (int)pos + 2 + 12 * (int)it );
                            nextoff = swap( stream.readInt() );
                            pos = nextoff;
                            counter++;
                            if (lsm_fi.LENGTH2==1) lsm_fi.STRIPOFF.add( new Long( lsm_fi.TIF_STRIPOFFSETS  ) ); else
                                lsm_fi.STRIPOFF.add( new Long( lsm_fi.TIF_STRIPOFFSETS1  ) );
                            lsm_fi.IMAGETYPE.add( new Long( lsm_fi.TIF_NEWSUBFILETYPE ) );
                        } while (nextoff!=0);
                        
                        /* Searches for the number of tags in the first image directory */
                        long iterator1 = HowManyTAGs(8);
                        /* Analyses each tag found */
                        for (int k=0 ; k<iterator1 ; k++){
                            byte[] TAG1 = readTAG(10+12*k);
                            analyseTAG(TAG1);
                        }
                        if (lsm_fi.TIF_COMPRESSION==5) {
                            //IJ.log("Image/J does not support compressed TIFF files");
                            //return;
                        }
                        /* Searches for the offsets to pixel data*/
                        getSTRIPOFFSETS(lsm_fi.TIF_STRIPOFFSETS);
                        /* Searches for the number of bytes used for a single pixel in each channel*/
                        getSTRIPBYTECOUNTS(lsm_fi.TIF_STRIPBYTECOUNTS);
                        /* Searches for infos located in the CZ-private TAG */
                        getCZ_LSMINFO(lsm_fi.TIF_CZ_LSMINFO);
                        /* Gets Channel names and colors */
                        getCHANNELNAMESANDCOLORS(lsm_fi.OFFSET_CHANNELSCOLORS);
                        /* Gets the LSM specific information located in the CZ-LSMinfo specific structure */
                        getSCANINFO(lsm_fi.OFFSET_SCANINFO);
                        /* Searches for infos located in the CZ-private TAG */
                        if (lsm_fi.OFFSET_TIMESTAMPS!=0) {
                            getTIMESTAMPS(lsm_fi.OFFSET_TIMESTAMPS);
                        }
                        if (lsm_fi.OFFSET_CHANNELWAVELENGTH!=0) {
                            getLAMBDASTAMPS(lsm_fi.OFFSET_CHANNELWAVELENGTH);
                        }
                        /* reads Input and output LUT. Those functions should work as far as I can see,
                         * but they don't. I think the specs from Zeiss are buggy on this. I commented
                         * these routines out because they are full of debug checks and not usable anyway
                         * as long as I don't have the propper specs
                         */
                        /* Searches for INPUT LUT */
                        /*if (lsm_fi.OFFSET_INPUTLUT!=0) {
                            getLUT(lsm_fi.OFFSET_INPUTLUT,lsm_fi.INPUTLUT);
                        }*/
                        /* Searches for OUTPUT LUT */
                        /*if (lsm_fi.OFFSET_OUTPUTLUT!=0) {
                            getLUT(lsm_fi.OFFSET_OUTPUTLUT,lsm_fi.OUTPUTLUT);
                        }*/
                        // Lut method call end
                        stream.close();
                        file.close();
                        
                        //int daOffset = (lsm_fi.DATATYPE == 2)?((int) (lsm_fi.TIF_IMAGEWIDTH*lsm_fi.TIF_IMAGELENGTH*2)):((int) (lsm_fi.TIF_IMAGEWIDTH*lsm_fi.TIF_IMAGELENGTH));
                        
                        for (int j = 0; j<(int)(lsm_fi.NUMBER_OF_CHANNELS); j++){
                            
                            ImagePlus imp = open(DIRECTORY, FILENAME,lsm_fi.STRIPOFF,j);
                            imp.setTitle(FILENAME + " Channel : " + lsm_fi.Channel[j]);
                            
                            if (imp != null ){
                                
                                Color[] gc= new Color[2];
                                gc[0] = new Color(0,0,0);
                                int r = (int)(lsm_fi.COLORS[j]&255);
                                int g = (int)((lsm_fi.COLORS[j]>>8)&255);
                                int b = (int)((lsm_fi.COLORS[j]>>16)&255);
                                gc[1] = new Color(r,g,b);
                                if (r==0 && g ==0 && b==0) gc[1]=Color.white;
                                apply_colors(imp,gc,2);
                                imp.getWindow().addWindowListener(new MyFocusListener(lsm_fi, imp)); 
                                if (lsm_fi.DATATYPE==2) {
                                    double min = imp.getProcessor().getMin();
                                    double max = imp.getProcessor().getMax();
                                    imp.getProcessor().setMinAndMax(min,max);
                                }
                                //imp.show();
                                imp.updateAndDraw();}
                            
                            tot.add(j,imp);
				    imp.hide();
                            
                        }
                    }
                    
                }
            }
            catch(IOException Open_LSM_exception){
                Open_LSM_exception.printStackTrace();
            }
            
            int l = tot.size();
            ImagePlus[] impTotal = new ImagePlus[l];
            for (int n=0; n<l; n++) impTotal[n] = (ImagePlus) tot.get(n);
            return impTotal;
        }
        
        /**********************************************************
         * method : isLSMfile, checks if file is a valid LSM file *
         *********************************************************/
        
        public  boolean isLSMfile() {
            boolean identifier = false;
            long ID = 0;
            try {
                stream.seek(2);//offset to the identifier
                /* Reads the identifier */
                ID = swap(stream.readShort());
                if (ID == 42) identifier = true;
            }
            catch (IOException isLSMfile_IOex) {
                isLSMfile_IOex.printStackTrace();
            }
            return identifier;
        }
        
        /****************************************************************
         * method : readTAG, reads some tags and puts the into an array *
         ***************************************************************/
        
        public byte[] readTAG(long position) {
            byte[] DIRENTRY = new byte[12];
            try {
                file.seek(position);
                for (int i=0; i<12; i++) {
                    DIRENTRY[i] = file.readByte();
                }
            }
            catch(IOException readTAG_exception) {
                readTAG_exception.printStackTrace();
            }
            return DIRENTRY;
        }
        
        /******************************************************
         * method : analyseTAG definition, analyses some tags *
         *****************************************************/
        
        public void analyseTAG(byte[] DIRENTRY) {
            /* This method works on 12 bytes arrays because java natively works on        */
            /* Big-Endian data but here the whole file is in Intel Byte Order which means */
            /* Little-Endian byte order. Thus, the byte swapping code trick on the array. */
            int TAGTYPE = 0;
            long LENGTH = 0;
            int MASK = 0x00ff;
            long MASK2 = 0x000000ff;
            
            TAGTYPE = ((DIRENTRY[1] & MASK) << 8) | ((DIRENTRY[0] & MASK ) <<0);
            LENGTH = ((DIRENTRY[7] & MASK2) << 24) | ((DIRENTRY[6] & MASK2) << 16) | ((DIRENTRY[5] & MASK2) << 8) | (DIRENTRY[4] & MASK2);
            
            switch (TAGTYPE) {
                case 254:
                    lsm_fi.TIF_NEWSUBFILETYPE = ((DIRENTRY[11] & MASK2) << 24) | ((DIRENTRY[10] & MASK2) << 16) | ((DIRENTRY[9] & MASK2) << 8) | (DIRENTRY[8] & MASK2);
                    break;
                case 256:
                    lsm_fi.TIF_IMAGEWIDTH = ((DIRENTRY[11] & MASK2) << 24) | ((DIRENTRY[10] & MASK2) << 16) | ((DIRENTRY[9] & MASK2) << 8) | (DIRENTRY[8] & MASK2);
                    break;
                case 257:
                    lsm_fi.TIF_IMAGELENGTH = ((DIRENTRY[11] & MASK2) << 24) | ((DIRENTRY[10] & MASK2) << 16) | ((DIRENTRY[9] & MASK2) << 8) | (DIRENTRY[8] & MASK2);
                    break;
                case 258:
                    lsm_fi.LENGTH1 = ((DIRENTRY[7] & MASK2) << 24) | ((DIRENTRY[6] & MASK2) << 16) | ((DIRENTRY[5] & MASK2) << 8) | (DIRENTRY[4] & MASK2);
                    lsm_fi.TIF_BITSPERSAMPLE_CHANNEL1 = ((DIRENTRY[8] & MASK2) << 0);
                    lsm_fi.TIF_BITSPERSAMPLE_CHANNEL2 = ((DIRENTRY[9] & MASK2) << 0);
                    lsm_fi.TIF_BITSPERSAMPLE_CHANNEL3 = ((DIRENTRY[10] & MASK2) << 0);
                    break;
                case 259:
                    lsm_fi.TIF_COMPRESSION = ((DIRENTRY[8] & MASK2) << 0);
                    break;
                case 262:
                    lsm_fi.TIF_PHOTOMETRICINTERPRETATION = ((DIRENTRY[8] & MASK2) << 0);
                    break;
                case 273:
                    lsm_fi.LENGTH2 = ((DIRENTRY[7] & MASK2) << 24) | ((DIRENTRY[6] & MASK2) << 16) | ((DIRENTRY[5] & MASK2) << 8) | (DIRENTRY[4] & MASK2);
                    lsm_fi.TIF_STRIPOFFSETS = ((DIRENTRY[11] & MASK2) << 24) | ((DIRENTRY[10] & MASK2) << 16) | ((DIRENTRY[9] & MASK2) << 8) | (DIRENTRY[8] & MASK2);
                    break;
                case 277:
                    lsm_fi.TIF_SAMPLESPERPIXEL = ((DIRENTRY[8] & MASK2) << 0);
                    break;
                case 279:
                    lsm_fi.TIF_STRIPBYTECOUNTS = ((DIRENTRY[11] & MASK2) << 24) | ((DIRENTRY[10] & MASK2) << 16) | ((DIRENTRY[9] & MASK2) << 8) | (DIRENTRY[8] & MASK2);
                    break;
                case 34412:
                    lsm_fi.TIF_CZ_LSMINFO = ((DIRENTRY[11] & MASK2) << 24) | ((DIRENTRY[10] & MASK2) << 16) | ((DIRENTRY[9] & MASK2) << 8) | (DIRENTRY[8] & MASK2);
                    break;
                default:
                    break;
            }
        }
        
        /***************************************************************
         * method : getSTRIPOFFSETS, gets the Offset to the image data *
         **************************************************************/
        
        public void getSTRIPOFFSETS(long position){
            try {
                stream.seek((int)position);
                lsm_fi.TIF_STRIPOFFSETS1 = swap(stream.readInt()); //reads first STRIPOFFSETS
                lsm_fi.TIF_STRIPOFFSETS2 = swap(stream.readInt()); //reads second STRIPOFFSETS
                lsm_fi.TIF_STRIPOFFSETS3 = swap(stream.readInt()); //reads third STRIPOFFSETS
            }
            catch(IOException getSTRIPOFFESTS_exception) {
                getSTRIPOFFESTS_exception.printStackTrace();
            }
        }
        
        /*******************************
         * method : getSTRIPBYTECOUNTS *
         ******************************/
        
        public void getSTRIPBYTECOUNTS(long position) {
            try {
                stream.seek((int)position);
                lsm_fi.TIF_STRIPBYTECOUNTS1 = swap(stream.readInt());
                lsm_fi.TIF_STRIPBYTECOUNTS2 = swap(stream.readInt());
                lsm_fi.TIF_STRIPBYTECOUNTS3 = swap(stream.readInt());
            }
            catch(IOException getSTRIPBYTECOUNTS_exception) {
                getSTRIPBYTECOUNTS_exception.printStackTrace();
            }
        }
        
        /*******************************************
         * method : HowManyTAGS, returns tag count *
         ******************************************/
        
        public long HowManyTAGs(long position) {
            long TAGs = 0;
            try {
                stream.seek((int)position);
                TAGs = swap(stream.readShort());
            }
            catch(IOException HowManyTAGs_exception) {
                HowManyTAGs_exception.printStackTrace();
            }
            return TAGs;
        }
        
        /*****************************************************************
         * method getCZ_LSMINFO, get Carl Zeiss LSM standard information *
         ****************************************************************/
        
        public void getCZ_LSMINFO(long position) {
            try {
                if (position == 0) return;
                stream.seek((int)position+8);
                
                lsm_fi.DIMENSION_X = swap(stream.readInt());
                lsm_fi.DIMENSION_Y = swap(stream.readInt());
                lsm_fi.DIMENSION_Z = swap(stream.readInt());
                
                lsm_fi.NUMBER_OF_CHANNELS = swap(stream.readInt());
                lsm_fi.TIMESTACKSIZE = swap(stream.readInt());
                
                lsm_fi.DATATYPE = swap(stream.readInt());
                
                stream.seek((int)position+88);
                lsm_fi.SCANTYPE = swap(stream.readShort());
                
                stream.seek((int)position+90);
                lsm_fi.SPECTRALSCAN = swap(stream.readShort());
                
                // second datatype , orignal scandata or calculated data or animation
                stream.seek((int)position+92);
                lsm_fi.DATATYPE2 = swap(stream.readInt());
                
                stream.seek((int)position+100);
                lsm_fi.OFFSET_INPUTLUT = swap(stream.readInt());
                
                stream.seek((int)position+104);
                lsm_fi.OFFSET_OUTPUTLUT = swap(stream.readInt());
                
                stream.seek((int)position + 40);
                lsm_fi.VOXELSIZE_X = swap(stream.readDouble());
                
                stream.seek((int)position + 48);
                lsm_fi.VOXELSIZE_Y = swap(stream.readDouble());
                
                stream.seek((int)position + 56);
                lsm_fi.VOXELSIZE_Z = swap(stream.readDouble());
                
                stream.seek((int)position + 108);
                lsm_fi.OFFSET_CHANNELSCOLORS = swap(stream.readInt());
                
                stream.seek((int)position + 120);
                lsm_fi.OFFSET_CHANNELDATATYPES = swap(stream.readInt());
                
                stream.seek((int)position+124);
                lsm_fi.OFFSET_SCANINFO = swap(stream.readInt());
                
                stream.seek((int)position+132);
                lsm_fi.OFFSET_TIMESTAMPS = swap(stream.readInt());
                
                stream.seek((int)position+204);
                lsm_fi.OFFSET_CHANNELWAVELENGTH = swap(stream.readInt());
            }
            catch(IOException getCZ_LSMINFO_exception) {
                getCZ_LSMINFO_exception.printStackTrace();
            }
        }
        
        /*****************************************************
         * method : getTIMESTAMPS, gets timestamps from file *
         ****************************************************/
        
        public void getTIMESTAMPS(long position) {
            try {
                stream.seek((int)position);
                lsm_fi.TS_BYTESIZE = swap(stream.readInt());
                lsm_fi.TS_COUNT = swap(stream.readInt());
                lsm_fi.TS_STAMPS = new double[(int)lsm_fi.TS_COUNT];
                
                for (int i = 0;i < lsm_fi.TS_COUNT;i++){
                    lsm_fi.TS_STAMPS[i] = swap(stream.readDouble());
                }
                for (int i=1; i<lsm_fi.TS_COUNT; i++) {
                    lsm_fi.TS_STAMPS[i]= lsm_fi.TS_STAMPS[i]-lsm_fi.TS_STAMPS[0];
                }
                lsm_fi.TS_STAMPS[0]=0;
            }
            catch(IOException getTIMESTAMPS_exception) {
                getTIMESTAMPS_exception.printStackTrace();
            }
        }
        
        /********************************************************
         * method : getLAMBDASTAMPS, gets lambdastamps from file *
         ********************************************************/
        
        public void getLAMBDASTAMPS(long position) {
            try {
                stream.seek((int)position);
                lsm_fi.LS_COUNT = swap(stream.readInt());
                lsm_fi.LS_STAMPS = new double[(int)lsm_fi.LS_COUNT];
                
                for (int i = 0;i < lsm_fi.LS_COUNT;i++){
                    double a = swap(stream.readDouble());
                    double b = swap(stream.readDouble());
                    lsm_fi.LS_STAMPS[i] = (a + b)/2;
                }
            }
            catch(IOException getLAMBDASTAMPS_exception) {
                getLAMBDASTAMPS_exception.printStackTrace();
            }
        }
        /*********************************************************************************************
         * method : getLUT, gets the LookUp Table, method is implemented but not used right now      *
         *          This function reads the LUT from the LSM file, but I have not thoroughly tested  *
         *          it. I hope that the implementation will be more clear to me once I've got a copy *
         *          of CZ new specs tables.                                                          *
         ********************************************************************************************/
        
        public void getLUT(long position,LSMFileInfo.LUT_INFO lut) {
            try {
                stream.seek((int)position);
                IJ.error("Offset: "+Long.toString(position));
                lut.LUT_SIZE = swap(stream.readInt());
                IJ.error("Size: "+Long.toString(lut.LUT_SIZE));
                lut.SUBBLOCKS_COUNT = swap(stream.readInt());
                IJ.error("SB COUNT: "+Long.toString(lut.SUBBLOCKS_COUNT));
                lut.CHANNELS_COUNT = swap(stream.readInt());
                IJ.error("CH COUNT: "+Long.toString(lut.CHANNELS_COUNT));
                lut.LUT_TYPE = swap(stream.readInt());
                lut.ADVANCED = swap(stream.readInt());
                lut.CURRENT_CHANNEL = swap(stream.readInt());
                stream.readInt();
                IJ.error("Subblocks : "+Long.toString(lut.SUBBLOCKS_COUNT));
                for (int i=0;i<lut.SUBBLOCKS_COUNT;i++){
                    long l = swap(stream.readInt());
                    IJ.error("Subblock type : "+Long.toString(l));
                    switch ((int)l) {
                        case 1 :    lut.G_SIZE= swap(stream.readInt());
                        IJ.error("Gamma present");
                        for (int j=0;i<lut.CHANNELS_COUNT;j++) lut.G_CHANNEL[j] = swap(stream.readDouble());
                        break;
                        case 2 :    lut.B_SIZE= swap(stream.readInt());
                        IJ.error("Brightness present");
                        for (int j=0;i<lut.CHANNELS_COUNT;j++) lut.B_CHANNEL[j] = swap(stream.readDouble());
                        break;
                        case 3 :    lut.C_SIZE= swap(stream.readInt());
                        IJ.error("Contrast present");
                        for (int j=0;i<lut.CHANNELS_COUNT;j++) lut.C_CHANNEL[j] = swap(stream.readDouble());
                        break;
                        
                        case 4 :    lut.R_SIZE= swap(stream.readInt());
                        IJ.error("Ramp present");
                        for (int j=0;i<lut.CHANNELS_COUNT;j++) {
                            lut.R_CHANNELSX[j] = swap(stream.readDouble());
                            lut.R_CHANNELSY[j] = swap(stream.readDouble());
                            lut.R_CHANNELEX[j] = swap(stream.readDouble());
                            lut.R_CHANNELEY[j] = swap(stream.readDouble());
                        }
                        break;
                        case 5 :    lut.K_SIZE= swap(stream.readInt());
                        IJ.error("Knots present");
                        lut.KNOTS = (lut.K_SIZE-4) / (lut.CHANNELS_COUNT*4);
                        for (int j=0;i<lut.CHANNELS_COUNT;j++){
                            for (int k=0;k<lut.KNOTS;k++){
                                lut.K_CHANNELX[(int)(j*lut.KNOTS+k)] = swap(stream.readDouble());
                                lut.K_CHANNELY[(int)(j*lut.KNOTS+k)] = swap(stream.readDouble());
                            }
                        }
                        break;
                        case 6 :    lut.P_SIZE= swap(stream.readInt());
                        
                        IJ.error("Palette present");
                        for (int j=0;i<lut.CHANNELS_COUNT;j++)
                            for (int k=0;k<4096;k++){
                                lut.P_CHANNEL[j*4096+k] = (byte)swap(stream.readShort());
                            }
                        break;
                        default :   break;
                    }
                }
            }
            catch(IOException getLUT_exception) {
                getLUT_exception.printStackTrace();
            }
        }
        
        /*************************************************************************
         * method : getCHANNELNAMESANDCOLORS gets the correct channel names, and *
         * their respective colors                                               *
         ************************************************************************/
        
        public void getCHANNELNAMESANDCOLORS(long position){
            try{
                stream.seek((int) position);
                int BlockSize = swap(stream.readInt());
                
                stream.seek((int) position+4);
                lsm_fi.NUMCOLORS = swap(stream.readInt());
                
                stream.seek((int) position+12);
                lsm_fi.OFFSET_COLORS = swap(stream.readInt());
                
                stream.seek((int) position+16);
                int NamesOffset = swap(stream.readInt());
                
                file.seek(NamesOffset + (int) position);
                int Namesize = BlockSize - NamesOffset;
                
                String AllNames = file.readLine();
                AllNames = AllNames.substring(0, Namesize);
                int k = 0;
                int begindex = 4;
                int endindex = 5;
                for (int j = 0; j<lsm_fi.NUMBER_OF_CHANNELS; j++) {
                    endindex = AllNames.indexOf(00, begindex);
                    lsm_fi.Channel[j] = AllNames.substring(begindex, endindex);
                    begindex = endindex+5;
                }
                
                stream.seek((int) lsm_fi.OFFSET_COLORS+ (int) position);
                lsm_fi.COLORS = new long[(int)(lsm_fi.NUMCOLORS)];
                
                for (int j = 0;j<(int)(lsm_fi.NUMCOLORS);j++){
                    lsm_fi.COLORS[j] = swap(stream.readInt());
                }
            }
            catch(IOException getCHANNELNAMESANDCOLORS_exception) {
                getCHANNELNAMESANDCOLORS_exception.printStackTrace();
            }
        }
        
        /************************************************************************************************
         * method : getSCANINFO, This structre is a redundant structure. Many tags can be found         *
         * in the LSMFileInfo class subclass SCANINFO. Some tags are missing, they do probably describe *
         * some additional data that has not yet been publicly specified by Zeiss.                      *
         ***********************************************************************************************/
        
        public void getSCANINFO(long position) {
            int level = 0 ;
            int i;
            boolean found;
            String s = new String("");
            level =0;
            String entry = new String("");
            String last = new String("");
            int count_1 = 1;
            int count_2 = 1;
            int count_3 = 1;
            int count_4 = 1;
            
            try {
                stream.seek((int)position);
                do{
                    if (entry.equals("060000000")){
                        lsm_fi.SCANINFO.det_channels_count.add(new Integer(0));
                        count_1 = 1;
                    }
                    if (entry.equals("080000000")){
                        lsm_fi.SCANINFO.illum_channels_count.add(new Integer(0));
                        count_2 = 1;
                    }
                    if (entry.equals("0A0000000")){
                        lsm_fi.SCANINFO.bsplits_count.add(new Integer(0));
                        count_3 = 1;
                    }
                    if (entry.equals("0C0000000")){
                        lsm_fi.SCANINFO.data_channels_count.add(new Integer(0));
                        count_4 = 1;
                    }
                    lsm_fi.SCANINFO.off = stream.getFilePointer();
                    
                    read_tags();
                    found = false;
                    String entry_1s =  Integer.toHexString(lsm_fi.SCANINFO.entry_1).toUpperCase();
                    String entry_2s =  Integer.toHexString(lsm_fi.SCANINFO.entry_2).toUpperCase();
                    int a = entry_1s.length();
                    int b = entry_2s.length();
                    if (a < 4)
                        for (int j=0;j<(4-a);j++) entry_1s="0"+entry_1s;
                    if (b < 4)
                        for (int j=0;j<(4-b);j++) entry_2s="0"+entry_2s;
                    entry = "0"+shifter(entry_1s+entry_2s);
                    
                    if (entry.equals("040000000")){
                        lsm_fi.SCANINFO.tracks.add(new ArrayList());
                    }
                    if (entry.equals("050000000")){
                        lsm_fi.SCANINFO.lasers.add(new ArrayList());
                    }
                    if (entry.equals("070000000")){
                        lsm_fi.SCANINFO.det_channels.add(new ArrayList());
                        if (lsm_fi.SCANINFO.det_channels_count.isEmpty()) lsm_fi.SCANINFO.det_channels_count.addElement(new Integer(count_1++)); else
                            lsm_fi.SCANINFO.det_channels_count.setElementAt(new Integer(count_1++),lsm_fi.SCANINFO.det_channels_count.size()-1);
                    }
                    if (entry.equals("090000000")){
                        lsm_fi.SCANINFO.illum_channels.add(new ArrayList());
                        if (lsm_fi.SCANINFO.illum_channels_count.isEmpty()) lsm_fi.SCANINFO.illum_channels_count.addElement(new Integer(count_2++)); else
                            lsm_fi.SCANINFO.illum_channels_count.setElementAt(new Integer(count_2++),lsm_fi.SCANINFO.illum_channels_count.size()-1);
                    }
                    if (entry.equals("0B0000000")){
                        lsm_fi.SCANINFO.bsplits.add(new ArrayList());
                        if (lsm_fi.SCANINFO.bsplits_count.isEmpty()) lsm_fi.SCANINFO.bsplits_count.addElement(new Integer(count_3++)); else
                            lsm_fi.SCANINFO.bsplits_count.setElementAt(new Integer(count_3++),lsm_fi.SCANINFO.bsplits_count.size()-1);
                    }
                    if (entry.equals("0D0000000")){
                        lsm_fi.SCANINFO.data_channels.add(new ArrayList());
                        if (lsm_fi.SCANINFO.data_channels_count.isEmpty()) lsm_fi.SCANINFO.data_channels_count.addElement(new Integer(count_4++)); else
                            lsm_fi.SCANINFO.data_channels_count.setElementAt(new Integer(count_4++),lsm_fi.SCANINFO.data_channels_count.size()-1);
                    }
                    if (entry.equals("012000000")){
                        lsm_fi.SCANINFO.timers.add(new ArrayList());
                    }
                    if (entry.equals("014000000")){
                        lsm_fi.SCANINFO.markers.add(new ArrayList());
                    }
                    for(i=0; i<147; i++){
                        String st = new String(lsm_fi.SCANINFO.table[i][0]);
                        st = st.substring(2);
                        if (entry.equals(st)){
                            found = true;
                            if (lsm_fi.SCANINFO.stype==2) {
                                s = read_ASCII(lsm_fi.SCANINFO.ssize);
                                lsm_fi.sb.append(Integer.toString(lsm_fi.SCANINFO.off)+"\t"+Integer.toString(level)+"\t"+lsm_fi.SCANINFO.table[i][0]+"\t"+lsm_fi.SCANINFO.table[i][3]+"\t"+lsm_fi.SCANINFO.ssize+"\t"+s+"\n");
                                if (i>=17 && i<=19 && !lsm_fi.SCANINFO.lasers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.lasers.lastElement()).add(new Object[]{new Integer(i),s});
                                if (i>=20 && i<=70) lsm_fi.SCANINFO.recordings.add(new Object[]{new Integer(i),s});
                                if (i>=71 && i<=92 && !lsm_fi.SCANINFO.tracks.isEmpty())((ArrayList)lsm_fi.SCANINFO.tracks.lastElement()).add(new Object[]{new Integer(i),s});
                                if (i>=93 && i<=115 && !lsm_fi.SCANINFO.det_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.det_channels.lastElement()).add(new Object[]{new Integer(i),s});
                                if (i>=116 && i<=122 && !lsm_fi.SCANINFO.illum_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.illum_channels.lastElement()).add(new Object[]{new Integer(i),s});
                                if (i>=123 && i<=125 && !lsm_fi.SCANINFO.bsplits.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.bsplits.lastElement()).add(new Object[]{new Integer(i),s});
                                if (i>=126 && i<=138 && !lsm_fi.SCANINFO.data_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.data_channels.lastElement()).add(new Object[]{new Integer(i),s});
                                if (i>=139 && i<=142 && !lsm_fi.SCANINFO.timers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.timers.lastElement()).add(new Object[]{new Integer(i),s});
                                if (i>=143 && i<=146 && !lsm_fi.SCANINFO.markers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.markers.lastElement()).add(new Object[]{new Integer(i),s});
                            }
                            if (lsm_fi.SCANINFO.stype==5) {
                                double d = swap(stream.readDouble());
                                lsm_fi.sb.append(Integer.toString(lsm_fi.SCANINFO.off)+"\t"+Integer.toString(level)+"\t"+lsm_fi.SCANINFO.table[i][0]+"\t"+lsm_fi.SCANINFO.table[i][3]+"\t"+lsm_fi.SCANINFO.ssize+"\t"+Double.toString(d)+"\n");
                                if (i>=17 && i<=19 && !lsm_fi.SCANINFO.lasers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.lasers.lastElement()).add(new Object[]{new Integer(i),new Double(d)});
                                if (i>=20 && i<=70) lsm_fi.SCANINFO.recordings.add(new Object[]{new Integer(i),new Double(d)});
                                if (i>=71 && i<=92 && !lsm_fi.SCANINFO.tracks.isEmpty())((ArrayList)lsm_fi.SCANINFO.tracks.lastElement()).add(new Object[]{new Integer(i),new Double(d)});
                                if (i>=93 && i<=115 && !lsm_fi.SCANINFO.det_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.det_channels.lastElement()).add(new Object[]{new Integer(i),new Double(d)});
                                if (i>=116 && i<=122 && !lsm_fi.SCANINFO.illum_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.illum_channels.lastElement()).add(new Object[]{new Integer(i),new Double(d)});
                                if (i>=123 && i<=125 && !lsm_fi.SCANINFO.bsplits.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.bsplits.lastElement()).add(new Object[]{new Integer(i),new Double(d)});
                                if (i>=126 && i<=138 && !lsm_fi.SCANINFO.data_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.data_channels.lastElement()).add(new Object[]{new Integer(i),new Double(d)});
                                if (i>=139 && i<=142 && !lsm_fi.SCANINFO.timers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.timers.lastElement()).add(new Object[]{new Integer(i),new Double(d)});
                                if (i>=143 && i<=146 && !lsm_fi.SCANINFO.markers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.markers.lastElement()).add(new Object[]{new Integer(i),new Double(d)});
                            }
                            if (lsm_fi.SCANINFO.stype==4) {
                                long n = swap(stream.readInt());
                                lsm_fi.sb.append(Integer.toString(lsm_fi.SCANINFO.off)+"\t"+Integer.toString(level)+"\t"+lsm_fi.SCANINFO.table[i][0]+"\t"+lsm_fi.SCANINFO.table[i][3]+"\t"+lsm_fi.SCANINFO.ssize+"\t"+Long.toString(n)+"\n");
                                if (i>=17 && i<=19 && !lsm_fi.SCANINFO.lasers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.lasers.lastElement()).add(new Object[]{new Integer(i),new Long(n)});
                                if (i>=20 && i<=70) lsm_fi.SCANINFO.recordings.add(new Object[]{new Integer(i),new Long(n)});
                                if (i>=71 && i<=92 && !lsm_fi.SCANINFO.tracks.isEmpty())((ArrayList)lsm_fi.SCANINFO.tracks.lastElement()).add(new Object[]{new Integer(i),new Long(n)});
                                if (i>=93 && i<=115 && !lsm_fi.SCANINFO.det_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.det_channels.lastElement()).add(new Object[]{new Integer(i),new Long(n)});
                                if (i>=116 && i<=122 && !lsm_fi.SCANINFO.illum_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.illum_channels.lastElement()).add(new Object[]{new Integer(i),new Long(n)});
                                if (i>=123 && i<=125 && !lsm_fi.SCANINFO.bsplits.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.bsplits.lastElement()).add(new Object[]{new Integer(i),new Long(n)});
                                if (i>=126 && i<=138 && !lsm_fi.SCANINFO.data_channels.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.data_channels.lastElement()).add(new Object[]{new Integer(i),new Long(n)});
                                if (i>=139 && i<=142 && !lsm_fi.SCANINFO.timers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.timers.lastElement()).add(new Object[]{new Integer(i),new Long(n)});
                                if (i>=143 && i<=146 && !lsm_fi.SCANINFO.markers.isEmpty()) ((ArrayList)lsm_fi.SCANINFO.markers.lastElement()).add(new Object[]{new Integer(i),new Long(n)});
                            }
                            if (lsm_fi.SCANINFO.stype==0) {
                                lsm_fi.sb.append(Integer.toString(lsm_fi.SCANINFO.off)+"\t"+Integer.toString(level)+"\t"+lsm_fi.SCANINFO.table[i][0]+"\t"+lsm_fi.SCANINFO.table[i][3]+"\t\t"+"\n");
                                level++;
                            }
                        }
                    }
                    if (entry.equals("0FFFFFFFF")) {
                        found = true;
                        lsm_fi.sb.append(Integer.toString(lsm_fi.SCANINFO.off)+"\t"+Integer.toString(level)+"\t0x0FFFFFFFF\tEND SUBBLOCK\t"+Long.toString(lsm_fi.SCANINFO.ssize)+"\n");
                        level--;
                    }
                    if (found==false){
                        if (lsm_fi.SCANINFO.stype==2) {
                            s = read_ASCII(lsm_fi.SCANINFO.ssize);
                            lsm_fi.sb.append(Integer.toString(lsm_fi.SCANINFO.off)+"\t"+Integer.toString(level)+"\t\tUNKNOWN TAG\t"+lsm_fi.SCANINFO.ssize+"\t"+s+"\n");
                            lsm_fi.SCANINFO.unknown.add(new Object[]{null,s});
                        }
                        if (lsm_fi.SCANINFO.stype==5) {
                            double d = stream.readDouble();
                            lsm_fi.sb.append(Integer.toString(lsm_fi.SCANINFO.off)+"\t"+Integer.toString(level)+"\t\tUNKNOWN TAG\t"+lsm_fi.SCANINFO.ssize+"\t"+Double.toString(d)+"\n");
                            lsm_fi.SCANINFO.unknown.add(new Object[]{null,new Double(d)});
                        }
                        if (lsm_fi.SCANINFO.stype==4) {
                            long n = swap(stream.readInt());
                            lsm_fi.sb.append(Integer.toString(lsm_fi.SCANINFO.off)+"\t"+Integer.toString(level)+"\t\tUNKNOWN TAG\t"+lsm_fi.SCANINFO.ssize+"\t"+Long.toString(n)+"\n");
                            lsm_fi.SCANINFO.unknown.add(new Object[]{null,new Long(n)});
                        }
                    }
                } while(level > 0);
            } catch(IOException Read_tags_exception){
                Read_tags_exception.printStackTrace();
            }
            
            lsm_fi.Objective = (String)scanObjectArray(lsm_fi.SCANINFO.recordings,23);
            lsm_fi.ShortNotes = (String)scanObjectArray(lsm_fi.SCANINFO.recordings,22);
            lsm_fi.DetailedNotes = (String)scanObjectArray(lsm_fi.SCANINFO.recordings,21);
            lsm_fi.User = (String)scanObjectArray(lsm_fi.SCANINFO.recordings,66);
            lsm_fi.ZOOM_X = ((Double)scanObjectArray(lsm_fi.SCANINFO.recordings,41)).doubleValue();
            lsm_fi.ZOOM_Y = ((Double)scanObjectArray(lsm_fi.SCANINFO.recordings,42)).doubleValue();
            lsm_fi.ZOOM_Z = ((Double)scanObjectArray(lsm_fi.SCANINFO.recordings,43)).doubleValue();
            lsm_fi.PLANE_SPACING = ((Double)scanObjectArray(lsm_fi.SCANINFO.recordings,49)).doubleValue();
            lsm_fi.PLANE_WIDTH = ((Double)scanObjectArray(lsm_fi.SCANINFO.recordings,50)).doubleValue();
            lsm_fi.PLANE_HEIGHT = ((Double)scanObjectArray(lsm_fi.SCANINFO.recordings,51)).doubleValue();
            lsm_fi.VOLUME_DEPTH = ((Double)scanObjectArray(lsm_fi.SCANINFO.recordings,52)).doubleValue();
        }
        
        /*****************************************************************************
         * method : scanObjectArrayList, scans an ArrayList containing an object and *
         * resolves data to its corresponding tag                                    *
         ****************************************************************************/
        
        public Object scanObjectArray(ArrayList in,int val){
            Object[] obj = new Object[2];
            Object ob = new Object();
            ob = new String("");
            for (int i=0;i<in.size();i++){
                obj = ((Object[])in.get(i));
                int itmp = ((Integer)obj[0]).intValue();
                if (val == ((Integer)obj[0]).intValue()){
                    ob = obj[1];
                }
            }
            return ob;
        }
        
        /******************************************************************************
         * method : ImagePlus.open() (method re-defintion)                            *
         * This method simply sets up the fileinfo class which is used by imageJ when *
         * opening an image. This version of open has one big advantage compared to   *
         * the original ImageJ version of the funtion. It adds support for variable   *
         * gaps between images. While in CZ LSM 3.0, the TIFF images had static gaps, *
         * in 3.2 images were intermixed with thumbnails f.i.                         *
         *****************************************************************************/
        
        public ImagePlus open(String directory, String file,Vector offsets ,int channel_iterator) {
            File f = new File(directory, file);
            try {
                FileInfo fi = new FileInfo();
                fi.url = "";
                fi.directory = directory;
                fi.fileFormat = fi.TIFF;
                fi.fileName = file ;
                int datatype = (int) lsm_fi.DATATYPE;
                switch (datatype) {
                    case 1:
                        fi.fileType = fi.GRAY8;
                        break;
                    case 2:
                        fi.fileType = fi.GRAY16_UNSIGNED;
                        break;
                    case 5:
                        fi.fileType = fi.GRAY32_FLOAT;
                        break;
                        
                    default :
                        fi.fileType = fi.GRAY8;
                        break;
                }
                
                fi.height = (int) lsm_fi.TIF_IMAGELENGTH;
                fi.intelByteOrder = true;
                int scantype = (int)(lsm_fi.SCANTYPE);
                int stacksize = 0;
                switch (scantype) {
                    case 3:
                        stacksize = (int) lsm_fi.TIMESTACKSIZE;
                        break;
                    case 4:
                        stacksize = (int) lsm_fi.TIMESTACKSIZE;
                        break;
                    case 6:
                        stacksize = ((int) lsm_fi.TIMESTACKSIZE) * ((int) lsm_fi.DIMENSION_Z);
                        break;
                    default:
                        stacksize = (int) lsm_fi.DIMENSION_Z;
                        break;
                }
                
                ImagePlus imp = new ImagePlus();
                fi.width = (int) lsm_fi.TIF_IMAGEWIDTH;
                fi.height = (int) lsm_fi.TIF_IMAGELENGTH;
                fi.pixelDepth = lsm_fi.VOXELSIZE_Z*1000000;
                fi.pixelHeight = lsm_fi.VOXELSIZE_Y*1000000;
                fi.pixelWidth = lsm_fi.VOXELSIZE_X*1000000;
                fi.unit = "µm";
                fi.valueUnit ="µm";
                fi.nImages = 1;
                ColorModel cm = createColorModel(fi);
                ImageStack stack = new ImageStack(fi.width,fi.height,cm);
                Vector v1 = new Vector();
                for (int k=0;k<lsm_fi.STRIPOFF.size();k++)
                    if (((Long)lsm_fi.IMAGETYPE.elementAt(k)).intValue()==0) v1.addElement((Long)lsm_fi.STRIPOFF.elementAt(k));
                ImageReader reader = new ImageReader(fi);
                FileOpener fop = new FileOpener(fi);
                InputStream is = fop.createInputStream(fi);
                int skip; 
                if (lsm_fi.DATATYPE == 2)  skip = ((Long)v1.firstElement()).intValue()+(channel_iterator)*fi.width*fi.height*2;
                else skip = ((Long)v1.firstElement()).intValue()+(channel_iterator)*fi.width*fi.height;
                //int skip = ((Long)v1.firstElement()).intValue()+(0)*128*128;
                Object pixels;
                int counter = 0;
                for (int k=0;k<v1.size();k++){
                    is.skip(skip);
                    pixels = reader.readPixels(is);
                    stack.addSlice("",pixels);
                    if (k<v1.size()-1){
                        if (lsm_fi.DATATYPE == 2) skip = ((Long)v1.elementAt(k+1)).intValue() - (((Long)v1.elementAt(k)).intValue())-fi.width*fi.height*2;
                        else skip = ((Long)v1.elementAt(k+1)).intValue() - (((Long)v1.elementAt(k)).intValue())-fi.width*fi.height;
                    } else skip = 0;
                    counter++;
                }
                is.close();
                IJ.showProgress(1.0);
                if (stack.getSize()==0) return null;
                imp = new ImagePlus(fi.fileName, stack);
                imp.setFileInfo(fi);
                setCalibration(imp,fi,fop);
                ImageProcessor ip = imp.getProcessor();
                if (ip.getMin()==ip.getMax())  // find stack min and max if first slice is blank
                    IJ.showProgress(1.0);
                imp.show();
                return imp;
                
            }
            catch (IOException open) {
                IJ.error("An error occured while reading the file.\n \n" + open);
                IJ.showStatus("");
                return null;
            }
        }
        
        /***************************************************************************
         * method group : used for calibration of images, methods come from ImageJ *
         * by Wayne Rasband.                                                       *
         **************************************************************************/
        
        void setCalibration(ImagePlus imp,FileInfo fi,FileOpener fop) {
            Calibration cal = imp.getCalibration();
            if (fi.fileType==FileInfo.GRAY16_SIGNED) {
                if (IJ.debugMode) IJ.log("16-bit signed");
                double[] coeff = new double[2];
                coeff[0] = -32768.0;
                coeff[1] = 1.0;
                cal.setFunction(Calibration.STRAIGHT_LINE, coeff, "gray value");
            }
            
            Properties props = fop.decodeDescriptionString();
            
            if (fi.pixelWidth>0.0 && fi.unit!=null) {
                cal.pixelWidth = fi.pixelWidth;
                cal.pixelHeight = fi.pixelHeight;
                cal.pixelDepth = fi.pixelDepth;
                cal.setUnit(fi.unit);
            }
            
            if (fi.valueUnit!=null) {
                int f = fi.calibrationFunction;
                if ((f>=Calibration.STRAIGHT_LINE && f<=Calibration.GAMMA_VARIATE && fi.coefficients!=null)
                ||f==Calibration.UNCALIBRATED_OD)
                    cal.setFunction(f, fi.coefficients, fi.valueUnit);
            }
            
            if (fi.frameInterval!=0.0)
                cal.frameInterval = fi.frameInterval;
            
            if (props==null)
                return;
            
            cal.xOrigin = getDouble(props,"xorigin");
            cal.yOrigin = getDouble(props,"yorigin");
            cal.zOrigin = getDouble(props,"zorigin");
            cal.info = props.getProperty("info");
            
            double displayMin = getDouble(props,"min");
            double displayMax = getDouble(props,"max");
            if (!(displayMin==0.0&&displayMax==0.0)) {
                int type = imp.getType();
                ImageProcessor ip = imp.getProcessor();
                if (type==ImagePlus.GRAY8 || type==ImagePlus.COLOR_256)
                    ip.setMinAndMax(displayMin, displayMax);
                else if (type==ImagePlus.GRAY16 || type==ImagePlus.GRAY32) {
                    if (ip.getMin()!=displayMin || ip.getMax()!=displayMax)
                        ip.setMinAndMax(displayMin, displayMax);
                }
            }
        }
        private Double getNumber(Properties props, String key) {
            String s = props.getProperty(key);
            if (s!=null) {
                try {
                    return Double.valueOf(s);
                } catch (NumberFormatException e) {}
            }
            return null;
        }
        
        private double getDouble(Properties props, String key) {
            Double n = getNumber(props, key);
            return n!=null?n.doubleValue():0.0;
        }
        
        /*****************************************************************************
         * method : createColorModel, by Wayne Rasband, returns an IndexColorModel   *
         * for the image specified by this FileInfo.                                 *
         ****************************************************************************/
        
        public ColorModel createColorModel(FileInfo fi) {
            if (fi.fileType==FileInfo.COLOR8 && fi.lutSize>0)
                return new IndexColorModel(8, fi.lutSize, fi.reds, fi.greens, fi.blues);
            else
                return LookUpTable.createGrayscaleColorModel(fi.whiteIsZero);
        }
        /**************************************************************************
         * method : Apply_colors, applies color gradient; function taken out from *
         * my Lut_Panel plugin                                                    *
         *************************************************************************/
        
        public void apply_colors(ImagePlus imp,Color[] gc, int i){
            FileInfo fi = new FileInfo();
            fi.reds = new byte[256];
            fi.greens = new byte[256];
            fi.blues = new byte[256];
            fi.lutSize = 256;
            float nColorsfl = 256;
            float interval = 256;
            float iR = gc[0].getRed();
            float iG = gc[0].getGreen();
            float iB = gc[0].getBlue();
            float idR = gc[1].getRed()-gc[0].getRed();
            float idG = gc[1].getGreen()-gc[0].getGreen();
            float idB = gc[1].getBlue()-gc[0].getBlue();
            idR = (idR / interval);
            idG = (idG / interval);
            idB = (idB / interval);
            int a=0;
            for (a=(int)(interval*0); a<(int)(interval*(0)+interval); a++,iR += idR,iG += idG,iB += idB) {
                fi.reds[a] = (byte)(iR);
                fi.greens[a] = (byte)(iG);
                fi.blues[a] = (byte)(iB);
            }
            int b=(int)(interval*0+interval)-1;
            fi.reds[b] = (byte)(gc[1].getRed());
            fi.greens[b] = (byte)(gc[1].getGreen());
            fi.blues[b] = (byte)(gc[1].getBlue());
            nColorsfl = 256;
            
            if (nColorsfl>0) {
                if (nColorsfl<256) interpolate(fi.reds, fi.greens, fi.blues, (int)nColorsfl);
                showLut(imp,fi,true);
                return;
            }
        }
        
        /*************************************************************************************
         * method : interpolate taken out from the orignal ImageJ project from Wayne Rasband *
         * Couldn't find a way to have straight access to this method so I had               *
         * to add it manually.                                                               *
         ************************************************************************************/
        
        void interpolate(byte[] reds, byte[] greens, byte[] blues, int nColors) {
            byte[] r = new byte[nColors];
            byte[] g = new byte[nColors];
            byte[] b = new byte[nColors];
            System.arraycopy(reds, 0, r, 0, nColors);
            System.arraycopy(greens, 0, g, 0, nColors);
            System.arraycopy(blues, 0, b, 0, nColors);
            double scale = nColors/256.0;
            int i1, i2;
            double fraction;
            for (int i=0; i<256; i++) {
                i1 = (int)(i*scale);
                i2 = i1+1;
                if (i2==nColors) i2 = nColors-1;
                fraction = i*scale - i1;
                reds[i] = (byte)((1.0-fraction)*(r[i1]&255) + fraction*(r[i2]&255));
                greens[i] = (byte)((1.0-fraction)*(g[i1]&255) + fraction*(g[i2]&255));
                blues[i] = (byte)((1.0-fraction)*(b[i1]&255) + fraction*(b[i2]&255));
            }
        }
        
        /*************************************************************
         * method : showLut, applies the new Lut on the actual image *
         ************************************************************/
        
        void showLut(ImagePlus imp,FileInfo fi, boolean showImage) {
            if (imp!=null) {
                if (imp.getType()==ImagePlus.COLOR_RGB)
                    IJ.error("Color tables cannot be assiged to RGB Images.");
                else {
                    ImageProcessor ip = imp.getProcessor();
                    ColorModel cm = new IndexColorModel(8, 256, fi.reds, fi.greens, fi.blues);
                    ip.setColorModel(cm);
                    if (imp.getStackSize()>1) imp.getStack().setColorModel(cm);
                    imp.updateAndDraw();
                }
            }
            /*else
            imp.createImage(fi, showImage);*/
        }
        
        /*************************************************
         * reads an ASCII String of size given by long s *
         ************************************************/
        
        String read_ASCII(long s){
            int offset=0;
            String tempstr = new String("");
            int in = 0;
            char ch;
            boolean addchar=true;
            try{
                while (offset<s){
                    in = stream.read();
                    if (in==-1) break;
                    ch = (char)in;
                    if (addchar== true){
                        String achar = new Character((char)ch).toString();
                        if (ch!=0x00) tempstr+=achar; else addchar=false;
                    }
                    offset++;
                }
            }
            
            catch(IOException Read_ASCII_exception){
                Read_ASCII_exception.printStackTrace();
            }
            return tempstr;
        }
        
        /*************************
         * reads a Scaninfo tags *
         ************************/
        
        void read_tags(){
            try{
                lsm_fi.SCANINFO.entry_1 = stream.readShort();
                lsm_fi.SCANINFO.entry_2 = stream.readShort();
                lsm_fi.SCANINFO.stype = swap(stream.readInt());
                lsm_fi.SCANINFO.ssize = swap(stream.readInt());
            }
            catch(IOException Read_tags_exception){
                Read_tags_exception.printStackTrace();
            }
        }
        
        /*******************************************************************************
         * method : shifter shifts a 32 bit string, used to circumvent lack of propper *
         * hex to double function in java. Similar to swap functions, but for strings  *
         ******************************************************************************/
        
        String shifter(String sh){
            String temp = new String();
            temp = sh.substring(6,8)+sh.substring(2,6)+sh.substring(0,2);
            return temp;
        }
        
        /***************************************************************************
         * method group : bit swapping methods                                      *
         * Those methods were taken from Java JRE 1.4 as ImageJ works only with 1.3 *
         * which doesn't have them                                                  *
         ***************************************************************************/
        
        short swap(short x) {
            return (short)((x << 8) |
            ((x >> 8) & 0xff));
        }
        
        char swap(char x) {
            return (char)((x << 8) |
            ((x >> 8) & 0xff));
        }
        
        int swap(int x) {
            return (int)((swap((short)x) << 16) |
            (swap((short)(x >> 16)) & 0xffff));
        }
        
        long swap(long x) {
            return (long)(((long)swap((int)(x)) << 32) |
            ((long)swap((int)(x >> 32)) & 0xffffffffL));
        }
        
        float swap(float x) {
            return Float.intBitsToFloat(swap(Float.floatToIntBits(x)));
        }
        
        double swap(double x) {
            return Double.longBitsToDouble(swap(Double.doubleToLongBits(x)));
        }
        
        /*****************************************
         * method : showAbout, The About message *
         ****************************************/
        
        void showAbout() {
            IJ.showMessage("About LSM Image Opener...",
            "Version 3.2b\n"
            +"Copyright (C) 2002-2003 Patrick Pirrotte"
            +"This software is subject to the GNU General Public License"
            +"Please read LICENSE or read source code information headers"
            +"Works on images generated by LSM 510 version 2.8 to 3.2\n"
            +"Not tested on other versions\n"
            +"Patrick PIRROTTE, Yannick KREMPP (till v2.2)\n"
            +"Jerome Mutterer for IBMP-CNRS Strasbourg, France\n"
            +"Project maintainer  : patrick.pirrotte@gmx.net"
            +"                      jerome.mutterer@ibmp-ulp.u-strasbg.fr"
            +"Previous maintainer : yannickkrempp@wanadoo.fr"
            );
        }
    }
    /*********************************************************************************
     * Batch Converter Class - Adapted from Wayne Rasband's Batch Converter plug-in. *
     ********************************************************************************/

    public class Batch_Converter{

	private String[] choices = {"Tiff", "8-bit Tiff", "Jpeg", "Zip", "Raw"};
	private String format = "Tiff";

	  /***************************************************************
         * method : run(), opens a dialog box for the batch conversion *
         **************************************************************/

	
	public void run(String arg) {
		OpenDialog od = new OpenDialog("Select a file in source folder...", "");
		if (od.getFileName()==null) return;
		String dir1 = od.getDirectory();

		GenericDialog gd = new GenericDialog("LSM Batch Converter", IJ.getInstance());
		gd.addChoice("Convert to: ", choices, format);
		gd.showDialog();
		if (gd.wasCanceled())
			return;
		format = gd.getNextChoice();
	
		SaveDialog sd = new SaveDialog("Open destination folder...", "dummy name (required)", "");
		if (sd.getFileName()==null) return;
		String dir2 = sd.getDirectory();

		convert(dir1, dir2, format);
	}

        /************************************************
         * method : convert, does the actual conversion *
         ***********************************************/


	public void convert(String dir1, String dir2, String format) {
		//IJ.log("");
		//IJ.log("Converting to "+format);
		if (!dir2.endsWith(File.separator))
			dir2 += File.separator;
		//IJ.log("dir1: "+dir1);
		//IJ.log("dir2: "+dir2);
		String[] list = new File(dir1).list();
		if (list==null) return;
                    for (int i=0; i<list.length; i++) {
			//IJ.log((i+list.length)+": "+list[i]);
			IJ.showStatus(i+"/"+list.length);
			File f = new File(dir1+list[i]);
			if (!f.isDirectory()) {
                            
                            LSM_Reader_ reader = new LSM_Reader_();
                            ImagePlus [] impTab = reader.OpenLSM2(dir1, list[i]);
                                
                            for (int k=0; k<impTab.length; k++){
                                                   
				ImagePlus img = impTab[k];
				if (img!=null) {
					img = process(img);
                                        if (img!=null){
                                                if (img.getStackSize() != 1) {
                                                    //IJ.log("stack detected");
                                                    for (int slice=0; slice<img.getStackSize(); slice++){
                                                        img.setSlice(slice);
                                                        ImageProcessor ip = img.getProcessor();
                                                        ImageProcessor newip = ip.createProcessor(ip.getWidth(),ip.getHeight());
                                                        newip.setPixels(ip.getPixelsCopy());
                                                        
                                                        String slicename = img.getTitle();
                                                        slicename +=("_slice");
                                                        String numb = IJ.d2s(slice,0);
                                                        slicename += "_"+numb;
                                                                                                                
                                                        ImagePlus img2 = new ImagePlus(slicename, newip);
                                                        //img2.show();
                                                        save(img2, dir2, format, k);
                                                        
                                                        slicename ="";
                                                        numb ="";
                                                        }
                                                    }
                                                else save(img, dir2, format, k);
                                                }
                                        }
				}
			}
		}
		IJ.showProgress(1.0);
		IJ.showStatus("");
		IJ.showMessage("Conversion done");
	}

        /************************************************************************************
         * method : process, optional method to add some image processing before conversion *
         ***********************************************************************************/


	/** This is the place to add code to process each image. The image 
		is not written if this method returns null. */
	public ImagePlus process(ImagePlus img) {
		//double xscale = 1.6;
		//double yscale = 1.1;
		//int width = img.getWidth();
		//int height = img.getHeight();
		//ImageProcessor ip = img.getProcessor();
		//ip.setInterpolate(true);
		//ip = ip.resize((int)(width*xscale), (int)(height*yscale));
		//img.setProcessor(null, ip);
		return img;
	}

	  /****************************************************************
         * method : save, saves the image with an appropriate file name *
         ****************************************************************/


	public void save(ImagePlus img, String dir, String format, int index) {
		String name = img.getTitle();
                int dotIndex = name.lastIndexOf(".");
                int doubledotIndex = name.lastIndexOf(":");
                String Chan = name.substring(doubledotIndex+1, name.length());
		if (dotIndex>=0)
			name = name.substring(0, dotIndex);
                        if (doubledotIndex >= 0) name = name.concat(Chan);
                        String path = dir + name;
		if (format.equals("Tiff"))
			new FileSaver(img).saveAsTiff(path+".tif");
		else if (format.equals("8-bit Tiff"))
			saveAs8bitTiff(img, path+".tif");
		else if (format.equals("Zip"))
			new FileSaver(img).saveAsZip(path+".zip");
		else if (format.equals("Raw"))
			new FileSaver(img).saveAsRaw(path+".raw");
		else if (format.equals("Jpeg"))
			new FileSaver(img).saveAsJpeg(path+".jpg");
	}

	  /*******************************************************************
         * method : saveAs8bitTiff, image processing for 8-bit Tiff saving *
         ******************************************************************/


	void saveAs8bitTiff(ImagePlus img, String path) {
		ImageProcessor ip = img.getProcessor();
		if (ip instanceof ColorProcessor)
			{ip = reduceColors(ip); img.setProcessor(null, ip);}
		else if ((ip instanceof ShortProcessor) || (ip instanceof FloatProcessor))
			{ip = ip.convertToByte(true); img.setProcessor(null, ip);}
		new FileSaver(img).saveAsTiff(path);
	}

	  /*****************************************************************************
         * method : reduceColors, reduces the color range for the appropriate format *
         ****************************************************************************/


	ImageProcessor reduceColors(ImageProcessor ip) {
		MedianCut mc = new MedianCut((int[])ip.getPixels(), ip.getWidth(), ip.getHeight());
		Image img = mc.convert(256);
		return(new ByteProcessor(img));
	}
}
    
    
    
    /******************************************************************************
     * LSM File info class contains many propertes deeply buried in the LSM file  *
     * structure. Sub class SCANINFO contains scaninfo structure data             *
     ******************************************************************************/
    
    class LSMFileInfo extends FileInfo{
        public String FILENAME = "";
        public String DIRECTORY = "";
        public double[] TS_STAMPS;
        public long TS_COUNT;
        public long TS_BYTESIZE;
        public double[] LS_STAMPS;
        public long LS_COUNT;
        public long STACKSIZE;
        public long TIMESTACKSIZE;
        public long TIF_NEWSUBFILETYPE = 0;
        public long TIF_IMAGEWIDTH = 0;
        public long TIF_IMAGELENGTH = 0;
        public long LENGTH1 = 0;
        public long LENGTH2 = 0;
        public long TIF_BITSPERSAMPLE_CHANNEL1 = 0;
        public long TIF_BITSPERSAMPLE_CHANNEL2 = 0;
        public long TIF_BITSPERSAMPLE_CHANNEL3 = 0;
        public long TIF_COMPRESSION = 0;
        public long TIF_PHOTOMETRICINTERPRETATION = 0;
        public long TIF_STRIPOFFSETS = 0;
        public long TIF_STRIPOFFSETS1 = 0;
        public long TIF_STRIPOFFSETS2 = 0;
        public long TIF_STRIPOFFSETS3 = 0;
        public long TIF_SAMPLESPERPIXEL = 0;
        public long TIF_STRIPBYTECOUNTS = 0;
        public long TIF_STRIPBYTECOUNTS1 = 0;
        public long TIF_STRIPBYTECOUNTS2 = 0;
        public long TIF_STRIPBYTECOUNTS3 = 0;
        public long TIF_CZ_LSMINFO = 0;
        public long DIMENSION_X = 0;
        public long DIMENSION_Y = 0;
        public long DIMENSION_Z = 0;
        public long NUMBER_OF_CHANNELS = 0;
        public long THUMB_X = 0;
        public long THUMB_Y = 0;
        public double VOXELSIZE_X = 0;
        public double VOXELSIZE_Y = 0;
        public double VOXELSIZE_Z = 0;
        public long DATATYPE = 1;
        public long SCANTYPE = 0;
        public int SPECTRALSCAN = 0;
        public int DATATYPE2 = 0; // calc data, orig data or anim
        public long OFFSET_INPUTLUT = 0;
        public long OFFSET_OUTPUTLUT = 0;
        public long OFFSET_SCANINFO = 0;
        public long OFFSET_CHANNELSCOLORS = 0;
        public long OFFSET_COLORS = 0;
        public long OFFSET_CHANNELDATATYPES = 0;
        public long OFFSET_TIMESTAMPS = 0;
        public long OFFSET_CHANNELWAVELENGTH = 0;
        public long NUMCOLORS = 0;
        public long[] COLORS;
        /* infos gathered in the CZ_LSMINFO structure */
        public String DetailedNotes = "";
        public String ShortNotes = "";
        public String Objective = "";
        public String Sampling_Mode = "";
        public String User = "NA";
        public String[] Channel = new String[32];
        public long SUBTYPE = 0;
        public long SUBSIZE = 0;
        public long ENTRY = 0;
        public double PLANE_SPACING = 0;
        public double PLANE_WIDTH = 0;
        public double PLANE_HEIGHT = 0;
        public double VOLUME_DEPTH = 0;
        public double VOXEL_X = 0;
        public double VOXEL_Y = 0;
        public double VOXEL_Z = 0;
        public double ZOOM_X = 0;
        public double ZOOM_Y = 0;
        public double ZOOM_Z = 0;
        public StringBuffer sb = new StringBuffer();
        public LUT_INFO INPUTLUT = new LUT_INFO();
        public LUT_INFO OUTPUTLUT = new LUT_INFO();
        public SCAN_INFO SCANINFO = new SCAN_INFO();
        public Vector STRIPOFF = new Vector();
        public Vector IMAGETYPE = new Vector();
        
        public class LUT_INFO{
            public long LUT_SIZE = 0;
            public long SUBBLOCKS_COUNT = 0;
            public long CHANNELS_COUNT = 0;
            public long LUT_TYPE = 0;
            public long ADVANCED = 0;
            public long CURRENT_CHANNEL = 0;
            
            //GAMMA
            public long G_TYPE = 0;
            public long G_SIZE = 0;
            public double[] G_CHANNEL;
            
            //BRIGHTNESS
            public long B_TYPE = 0;
            public long B_SIZE = 0;
            public double[] B_CHANNEL;
            
            //CONTRAST
            public long C_TYPE = 0;
            public long C_SIZE = 0;
            public double[] C_CHANNEL;
            
            //RAMP
            public long R_TYPE = 0;
            public long R_SIZE = 0;
            public double[] R_CHANNELSX;
            public double[] R_CHANNELSY;
            public double[] R_CHANNELEX;
            public double[] R_CHANNELEY;
            
            //KNOTS
            public long K_TYPE = 0;
            public long K_SIZE = 0;
            public long KNOTS = 0;
            public double[] K_CHANNELX;
            public double[] K_CHANNELY;
            
            //PALETTE
            public long P_TYPE = 0;
            public long P_SIZE = 0;
            public byte[] P_CHANNEL;
        }
        public class SCAN_INFO{
            private int entry_1 = 0;
            private int entry_2 = 0;
            private String entry = new String();
            private long stype = 0;
            private long ssize = 0;
            private int off = 0;
            private String[][] table=   {
                {"0x010000000","1","SUB","RECORDINGS"},     //SUB
                {"0x020000000","1","SUB","TRACKS"},
                {"0x030000000","1","SUB","LASERS"},
                {"0x040000000","1","SUB","TRACK"},
                {"0x050000000","1","SUB","LASER"},
                {"0x060000000","1","SUB","DETECTION_CHANNELS"},
                {"0x070000000","1","SUB","DETECTION_CHANNEL"},
                {"0x080000000","1","SUB","ILLUMINATION_CHANNELS"},
                {"0x090000000","1","SUB","ILLUMINATION_CHANNEL"},
                {"0x0A0000000","1","SUB","BEAM_SPLITTERS"},
                {"0x0B0000000","1","SUB","BEAM_SPLITTER"},
                {"0x0C0000000","1","SUB","DATA_CHANNELS"},
                {"0x0D0000000","1","SUB","DATA_CHANNEL"},
                {"0x011000000","1","SUB","TIMERS"},
                {"0x012000000","1","SUB","TIMER"},
                {"0x013000000","1","SUB","MARKERS"},
                {"0x014000000","1","SUB","MARKER"},
                {"0x050000001","0","A","LASER_NAME"},       //LASER
                {"0x050000002","0","L","LASER_ACQUIRE"},
                {"0x050000003","0","L","LASER_POWER"},
                {"0x010000001","0","A","ENTRY_NAME"},       //RECORDINGS
                {"0x010000002","0","A","ENTRY_DESCRIPTION"},
                {"0x010000003","0","A","ENTRY_NOTES"},
                {"0x010000004","0","A","ENTRY_OBJECTIVE"},
                {"0x010000005","0","A","PROCESSING_SUMMARY"},
                {"0x010000006","0","A","SPECIAL_SCAN"},
                {"0x010000007","0","A","SCAN_TYPE"},
                {"0x010000008","0","A","SCAN_MODE"},
                {"0x010000009","0","L","STACKS_COUNT"},
                {"0x01000000A","0","L","LINES_PER_PLANE"},
                {"0x01000000B","0","L","SAMPLES_PER_LINE"},
                {"0x01000000C","0","L","PLANES_PER_VOLUME"},
                {"0x01000000D","0","L","IMAGES_WIDTH"},
                {"0x01000000E","0","L","IMAGES_HEIGHT"},
                {"0x01000000F","0","L","NUMBER_OF_PLANES"},
                {"0x010000010","0","L","IMAGES_NUMBER_STACKS"},
                {"0x010000011","0","L","IMAGES_NUMBER_CHANNELS"},
                {"0x010000012","0","L","LINESCAN_XY"},
                {"0x010000013","0","L","SCAN_DIRECTION"},
                {"0x010000014","0","L","TIME_SERIES"},
                {"0x010000015","0","L","ORIGNAL_SCAN_DATA"},
                {"0x010000016","0","R","ZOOM_X"},
                {"0x010000017","0","R","ZOOM_Y"},
                {"0x010000018","0","R","ZOOM_Z"},
                {"0x010000019","0","R","SAMPLE_0X"},
                {"0x01000001A","0","R","SAMPLE_0Y"},
                {"0x01000001B","0","R","SAMPLE_0Z"},
                {"0x01000001C","0","R","SAMPLE_SPACING"},
                {"0x01000001D","0","R","LINE_SPACING"},
                {"0x01000001E","0","R","PLANE SPACING"},
                {"0x01000001F","0","R","PLANE_WIDTH"},
                {"0x010000020","0","R","PLANE_HEIGHT"},
                {"0x010000021","0","R","VOLUME_DEPTH"},
                {"0x010000034","0","R","ROTATION"},
                {"0x010000035","0","R","PRECESSION"},
                {"0x010000036","0","R","SAMPLE_0TIME"},
                {"0x010000037","0","A","START_SCAN_TRIGGER_IN"},
                {"0x010000038","0","A","START_SCAN_TRIGGER_OUT"},
                {"0x010000039","0","L","START_SCAN_EVENT"},
                {"0x010000040","0","R","START_SCAN_TIME"},
                {"0x010000041","0","A","STOP_SCAN_TRIGGER_IN"},
                {"0x010000042","0","A","STOP_SCAN_TRIGGER_OUT"},
                {"0x010000043","0","L","STOP_SCAN_EVENT"},
                {"0x010000044","0","R","START_SCAN_TIME2"},
                {"0x010000045","0","L","USE_ROIS"},
                {"0x010000046","0","R","USE_REDUCED_MEMORY_ROIS"},
                {"0x010000047","0","A","USER"},
                {"0x010000048","0","L","USE_BCCORECCTION"},
                {"0x010000049","0","R","POSITION_BCCORRECTION1"},
                {"0x010000050","0","R","POSITION_BCCORRECTION2"},
                {"0x010000051","0","L","INTERPOLATIONY"},
                {"0x040000001","0","L","MULTIPLEX_TYPE"},        //TRACK INFO
                {"0x040000002","0","L","MULTIPLEX_ORDER"},
                {"0x040000003","0","L","SAMPLING_MODE"},
                {"0x040000004","0","L","SAMPLING_METHOD"},
                {"0x040000005","0","L","SAMPLING_NUMBER"},
                {"0x040000006","0","L","ENTRY_ACQUIRE"},
                {"0x040000007","0","R","OBSERVATION_TIME"},
                {"0x04000000B","0","R","TIME_BETWEEN_STACKS"},
                {"0x04000000C","0","A","TRACK_NAME"},
                {"0x04000000D","0","A","COLLIMATOR1_NAME"},
                {"0x04000000E","0","L","COLLIMATOR1_POSITION"},
                {"0x04000000F","0","A","COLLIMATOR2_NAME"},
                {"0x040000010","0","A","COLLIMATOR2_POSITION"},
                {"0x040000011","0","L","BLEACH_TRACK"},
                {"0x040000012","0","L","BLEACH_AFTER_SCAN NUMBER"},
                {"0x040000013","0","L","BLEACH_SCAN_NUMBER"},
                {"0x040000014","0","A","TRIGGER_IN"},
                {"0x040000015","0","A","TRIGGER_OUT"},
                {"0x040000016","0","L","IS_RATIO_TRACK"},
                {"0x040000017","0","L","BLEACH_COUNT"},
                {"0x040000018","0","R","SPI_CENTER_WAVELENGTH"},
                {"0x040000019","0","R","PIXEL_TIME"},
                {"0x070000003","0","R","DETECTOR_GAIN"},       //DETECTION CHANNELS
                {"0x070000005","0","R","AMPLIFIER_GAIN"},
                {"0x070000007","0","R","AMPLIFIER_OFFSET"},
                {"0x070000009","0","R","PINHOLE_DIAMETER"},
                {"0x07000000B","0","L","ENTRY_ACQUIRE"},
                {"0x07000000C","0","A","DETECTOR_NAME"},
                {"0x07000000D","0","A","AMPLIFIER_NAME"},
                {"0x07000000E","0","A","PINHOLE_NAME"},
                {"0x07000000F","0","A","FILTER_SET_NAME"},
                {"0x070000010","0","A","FILTER_NAME"},
                {"0x070000013","0","A","INTEGRATOR_NAME"},
                {"0x070000014","0","A","DETECTION_CHANNEL_NAME"},
                {"0x070000015","0","R","DETECTOR_GAIN_BC1"},
                {"0x070000016","0","R","DETECTOR_GAIN_BC2"},
                {"0x070000017","0","R","AMPLIFIER_GAIN_BC1"},
                {"0x070000018","0","R","AMPLIFIER_GAIN_BC2"},
                {"0x070000019","0","R","AMPLIFIER_OFFSET_BC1"},
                {"0x070000020","0","R","AMPLIFIER_OFFSET_BC2"},
                {"0x070000021","0","L","SPECTRAL_SCAN_CHANNELS"},
                {"0x070000022","0","R","SPI_WAVE_LENGTH_START"},
                {"0x070000022","0","R","SPI_WAVELENGTH_END"},
                {"0x070000026","0","A","DYE_NAME"},
                {"0x070000027","0","A","DYE_FOLDER"},
                {"0x090000001","0","A","ILL_NAME",},             //ILLUMINATION CHANNEL
                {"0x090000002","0","R","POWER"},
                {"0x090000003","0","R","WAVELENGTH"},
                {"0x090000004","0","L","ACQUIRE"},
                {"0x090000005","0","A","DETCHANNEL_NAME"},
                {"0x090000006","0","R","POWER_BC1"},
                {"0x090000007","0","R","POWER_BC2"},
                {"0x0B0000001","0","A","FILTER_SET"},      //BEAM SPLITTER
                {"0x0B0000002","0","A","FILTER"},
                {"0x0B0000003","0","A","BS_NAME"},
                {"0x0D0000001","0","A","DATA_NAME"},           //DATA CHANNEL
                {"0x0D0000004","0","L","COLOR"},
                {"0x0D0000005","0","L","SAMPLETYPE"},
                {"0x0D0000006","0","L","BITS_PER_SAMPLE"},
                {"0x0D0000007","0","L","RATIO_TYPE"},
                {"0x0D0000008","0","L","RATIO_TRACK1"},
                {"0x0D0000009","0","L","RATIO_TRACK2"},
                {"0x0D000000A","0","A","RATIO_CHANNEL1"},
                {"0x0D000000B","0","A","RATIO_CHANNEL2"},
                {"0x0D000000C","0","R","RATIO_CONST1"},
                {"0x0D000000D","0","R","RATIO_CONST2"},
                {"0x0D000000E","0","R","RATIO_CONST3"},
                {"0x0D000000F","0","R","RATIO_CONST4"},
                {"0x012000001","0","A","TIMER_NAME"},           //TIMER
                {"0x012000003","0","R","INTERVAL"},
                {"0x012000004","0","A","TRIGGER_IN"},
                {"0x012000005","0","A","TRIGGER_OUT"},
                {"0x014000001","0","A","MARKER_NAME"},          //MARKER
                {"0x014000002","0","A","DESCRIPTION"},
                {"0x014000003","0","A","TRIGGER_IN"},
                {"0x014000004","0","A","TRIGGER_OUT"},
            };
            
            public ArrayList recordings= new ArrayList();
            public ArrayList unknown= new ArrayList();
            public Vector tracks = new Vector();
            public Vector lasers = new Vector();
            public Vector det_channels = new Vector();
            public Vector illum_channels = new Vector();
            public Vector bsplits = new Vector();
            public Vector data_channels = new Vector();
            public Vector timers = new Vector();
            public Vector markers = new Vector();
            public Vector det_channels_count = new Vector();
            public Vector illum_channels_count = new Vector();
            public Vector bsplits_count = new Vector();
            public Vector data_channels_count = new Vector();
        }
    }
}
/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.wcs.smart.datamodelmatcher.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Device;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tracker;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryAttributeLink;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;
import org.wcs.smart.internal.ca.datamodel.xml.generate.LanguageType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.TreeNodeType;


public class MatchSessionDialog extends Dialog {

	String[] codes; 
	
	private MatchSession ms;
	private TableViewer viewer;

	private Label totalMatched;
	  
	private Text mistText1;
	private Text mistText2;
	private Text mistText3;
	private Text mistText4;
	private Text mistText5;
	private Text mistText6;
	private Text mistText7;
	private Text mistText8;
	private Text mistText9;
	
	private Tree tree;
	private Combo langSelector;
	
	private List<CategoryType> rootCategory;
	private DataModel smartDM;
	
	private HashMap<String, String> smartAttributeHash; 
	private HashMap<String, AttributeType> smartAttributeDetailsHash;
	
	private TabFolder tabFolder; 
	
	private Composite tab1Composite;
	private Composite tab2Composite;
	private Composite tab3Composite;
	private Composite tab4Composite;
	private Composite tab5Composite;
	
	private TabItem tab1; 
	private TabItem tab2;
	private TabItem tab3;
	private TabItem tab4;
	private TabItem tab5;
	
	private AttributeSelection attribute1;
	private AttributeSelection attribute2;
	private AttributeSelection attribute3;
	private AttributeSelection attribute4;
	private AttributeSelection attribute5;
	
	public final Shell shell;
	
	private Button save;
	private Button next;
	private Button clear;
	private Button done;
	
	private Button autoMatch;
	
	public MatchSessionDialog(Shell parentShell, MatchSession ms) {
	    super(parentShell);
        shell = new Shell(parentShell, SWT.SHELL_TRIM );
        Point size = shell.computeSize(-1, -1);
        shell.setBounds(50, 50, size.x, size.y);    
        shell.setText("Match Session - " + ms.getSaveLocation());
	    this.ms = ms;
	  }
	
	
	  public int open() {

          Display display = shell.getDisplay();
          
          GridLayout layout = new GridLayout(1, false);
		  shell.setLayout(layout);
		    
		  GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		  shell.setLayoutData(gridData);
		  
		  shell.setMinimumSize(840, 640);
		  
		    
          // (widget creation, set result, etc).
        
          //main composite and layout
  		  final Composite main = new Composite(shell, SWT.None);
  		  GridLayout mlayout = new GridLayout(2, true);
  	      main.setLayout(mlayout);
  	      GridData mainGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
  	      main.setLayoutData(mainGridData);

  	      //main.setSize (840, 580);
  	    
  	      
  	      //left
  	      final Composite left = new Composite(main, SWT.None);
		  GridLayout llayout = new GridLayout(3, false);
	      left.setLayout(llayout);
	    
	      GridData leftGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
	      left.setLayoutData(leftGridData);
	      
  	    
	      //left - content
	      // define the TableViewer---------------------------------------------------------------------------------
	      viewer = new TableViewer(left, SWT.MULTI | SWT.H_SCROLL
	            | SWT.V_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);

	      // create the columns 
	      createColumns(left, viewer);

	      // make lines and header visible
	      final Table table = viewer.getTable();
	      table.setHeaderVisible(true);
	      table.setLinesVisible(true); 
	      
	      viewer.setContentProvider(new ArrayContentProvider());
	      viewer.setInput(ms.getRows());

	      
	      TableColumnSorter sorter = new TableColumnSorter(viewer);
	      sorter.setMs(ms);
	      viewer.setComparator(sorter);
	
	   // define layout for the viewer
	      GridData tgridData = new GridData(SWT.FILL,SWT.FILL, true, true,3,0);
	      //tgridData.widthHint = 510;
	      tgridData.heightHint = 350;
	      viewer.getControl().setLayoutData(tgridData);
	      
	      viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {		
				ViewerSelectionChanged();
			}
	      });
	      

	   // end Viewer---------------------------------------------------------------------------------
	      
	      
	      //Mid-Left composite - full listing of Mist names
	      final Composite midLeft = new Composite(left, SWT.BORDER);
		  GridLayout trlayout = new GridLayout(2, false);
	      midLeft.setLayout(trlayout);
	      
	      Device device = Display.getCurrent();
	      Color topColor = new Color (device, 255, 235, 235);
	      midLeft.setBackground(topColor);
	    
	      GridData midLeftGridData = new GridData(SWT.FILL,SWT.CENTER, true, false,3,0);
	      //midLeftGridData.widthHint = 510;
	      midLeft.setLayoutData(midLeftGridData);
	    
	      
	      
	      //---------------------------------------------------------------------
	      //Left save session buttons etc.
	      Composite bottomLeft = new Composite(left, SWT.None);
	      GridLayout btmleftLayout = new GridLayout(3, false);
	      bottomLeft.setLayout(btmleftLayout);
	    
	      GridData btmleftData = new GridData(SWT.FILL,SWT.BOTTOM, true, false,3,0);
	      bottomLeft.setLayoutData(btmleftData);
	      
	      totalMatched = new Label(bottomLeft, SWT.READ_ONLY);
	      totalMatched.setText("Matched: " + ms.getNumMatched().toString() + " of " + ms.getNumTotal().toString() );
	      GridData totalGridData = new GridData(SWT.FILL,SWT.FILL, false, false);
	      totalGridData.widthHint = 120;
	      totalMatched.setLayoutData(totalGridData);
	      
	      done = new Button(bottomLeft, SWT.NONE);
	      done.setText("Matching Complete - Exit");
	      GridData doneGridData = new GridData(SWT.CENTER,SWT.FILL, true, false);
	      done.setLayoutData(doneGridData);
	      done.setVisible(false);
	      done.addSelectionListener(new SelectionAdapter() {	
		    	@Override
		    	public void widgetSelected(SelectionEvent e) {
		    		ms.save();
		    		shell.dispose();
		    	}
		    });
	      
	      
	      save = new Button(bottomLeft, SWT.NONE);
	      save.setText("   Save Session   ");
	      GridData saveGridData = new GridData(SWT.RIGHT,SWT.FILL, false, false);
	      saveGridData.heightHint = 20;
	      save.setLayoutData(saveGridData);
	      
	      save.addSelectionListener(new SelectionAdapter() {	
		    	@Override
		    	public void widgetSelected(SelectionEvent e) {
		    		ms.save();
		    		shell.setText("Match Session - " + ms.getSaveLocation());
		    		save.setEnabled(false);
		    	}
		    });
	      
	      
	      //------------------------------------------------------------------------------------------
	      //Right side components
	      
	      //right composite
	      final Composite right = new Composite(main, SWT.NONE);
		  GridLayout rlayout = new GridLayout(1, false);
	      right.setLayout(rlayout);
	      GridData rightGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
	      right.setLayoutData(rightGridData);


	      
	      /*
	      topRight.addListener(SWT.MouseDown, new Listener() {

	          public void handleEvent(Event e) {

	            Tracker tracker = new Tracker(topRight.getParent(), SWT.RESIZE | SWT.DOWN|SWT.UP);
	            tracker.setStippled(true);
	            Rectangle rect = topRight.getBounds();
	            tracker.setRectangles(new Rectangle[] { rect });
	            if (tracker.open()) {
	              Rectangle after = tracker.getRectangles()[0];
	              topRight.setBounds(after);
	            }
	            tracker.dispose();
	            shell.pack();

	          }
	        });
	      */
	      
	      
	        
	      //Top Right elements---------------------------------------------
	      Label mistLabel = new Label(midLeft, SWT.NONE);
	      mistLabel.setBackground(topColor);
	      mistLabel.setText("MIST Observation Details:" );
	      FontData fontData = mistLabel.getFont().getFontData()[0];
	      Font font = new Font(getParent().getDisplay(), new FontData(fontData.getName(), fontData
	    		    .getHeight(), SWT.BOLD));
	      mistLabel.setFont(font);
	      GridData mistItemLabel = new GridData(SWT.FILL,SWT.CENTER, true, false,2,1);
	      mistLabel.setLayoutData(mistItemLabel);

 
	      
	      Label mistLabel1 = new Label(midLeft, SWT.NONE);
	      mistLabel1.setBackground(topColor);
	      mistLabel1.setText("Observation Group:" );
	      
	      mistText1 = new Text(midLeft, SWT.NONE);
	      mistText1.setBackground(topColor);
	      GridData m1 = new GridData(SWT.FILL,SWT.CENTER, true, false);
	      m1.widthHint = 250;
	      mistText1.setLayoutData(m1);
	      
	      mistText1.setEnabled(false);
	      
	      Label mistLabel2 = new Label(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistLabel2.setBackground(topColor);
	      mistLabel2.setText("Observation:" );
	      
	      mistText2 = new Text(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistText2.setBackground(topColor);
	      mistText2.setLayoutData(m1);
	      mistText2.setEnabled(false);
	      
	      Label mistLabel3 = new Label(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistLabel3.setBackground(topColor);
	      mistLabel3.setText("Observation Code:" );
	      
	      mistText3 = new Text(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistText3.setBackground(topColor);
	      mistText3.setLayoutData(m1);
	      mistText3.setEnabled(false);
	      
	      Label mistLabel4 = new Label(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistLabel4.setBackground(topColor);
	      mistLabel4.setText("Item:" );
	      
	      mistText4 = new Text(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistText4.setBackground(topColor);
	      mistText4.setLayoutData(m1);
	      mistText4.setEnabled(false);
	      
	      Label mistLabel5 = new Label(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistLabel5.setBackground(topColor);
	      mistLabel5.setText("Subcode 1:" );
	      
	      mistText5 = new Text(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistText5.setBackground(topColor);
	      mistText5.setLayoutData(m1);
	      mistText5.setEnabled(false);
	      
	      Label mistLabel6 = new Label(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistLabel6.setBackground(topColor);
	      mistLabel6.setText("Subcode 2:" );
	      
	      mistText6 = new Text(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistText6.setBackground(topColor);
	      mistText6.setLayoutData(m1);
	      mistText6.setEnabled(false);
	      
	      Label mistLabel7 = new Label(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistLabel7.setBackground(topColor);
	      mistLabel7.setText("Subcode 3:" );
	      
	      mistText7 = new Text(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistText7.setBackground(topColor);
	      mistText7.setLayoutData(m1);
	      mistText7.setEnabled(false);
	      
	      Label mistLabel8 = new Label(midLeft, SWT.NONE );
	      mistLabel8.setBackground(topColor);
	      mistLabel8.setText("Subcode 4:" );
	      
	      mistText8 = new Text(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistText8.setBackground(topColor);
	      mistText8.setLayoutData(m1);
	      mistText8.setEnabled(false);
	      
	      Label mistLabel9 = new Label(midLeft, SWT.NONE);
	      mistLabel9.setBackground(topColor);
	      mistLabel9.setText("Subcode 5:" );
	      
	      mistText9 = new Text(midLeft, SWT.NONE | SWT.READ_ONLY);
	      mistText9.setBackground(topColor);
	      mistText9.setLayoutData(m1);
	      mistText9.setEnabled(false);
	      //end of Top Right elements--------------------------------------
	      	      
	      
	      //bottom Right composite
	      
	      final Composite bottomRight = new Composite(right, SWT.BORDER);
		  GridLayout brlayout = new GridLayout(2, false);
		  bottomRight.setLayout(brlayout);

	    
	      GridData bottomRightGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
	      bottomRight.setLayoutData(bottomRightGridData);

	      //bottomRight.setSize (420, 380);
	      
	      /*
	      bottomRight.addListener(SWT.MouseDown, new Listener() {

	          public void handleEvent(Event e) {

	            Tracker tracker = new Tracker(bottomRight.getParent(), SWT.RESIZE | SWT.DOWN|SWT.UP);
	            tracker.setStippled(true);
	            Rectangle rect = bottomRight.getBounds();
	            tracker.setRectangles(new Rectangle[] { rect });
	            if (tracker.open()) {
	              Rectangle after = tracker.getRectangles()[0];
	              bottomRight.setBounds(after);
	            }
	            tracker.dispose();
	          }
	        });
*/
	      


	      //Bottom Right elements---------------------------------------------
	      //top line spacing, label and language drop down
	      Composite bottomRightFirstLine = new Composite(bottomRight, SWT.NONE);
		  GridLayout brfllayout = new GridLayout(3, false);
		  bottomRightFirstLine.setLayout(brfllayout);
		  GridData bottomRightFL = new GridData(SWT.FILL,SWT.TOP, true, false,2,1);
		  bottomRightFL.minimumHeight = 28;
		  bottomRightFirstLine.setLayoutData(bottomRightFL);

	      
	      Label smartLabel = new Label(bottomRightFirstLine, SWT.NONE);
	      smartLabel.setText("SMART Observation Definition" );
	      FontData sfontData = smartLabel.getFont().getFontData()[0];
	      Font sfont = new Font(getParent().getDisplay(), new FontData(sfontData.getName(), sfontData
	    		    .getHeight(), SWT.BOLD));
	      smartLabel.setFont(sfont);
	      GridData smartLabelData = new GridData(SWT.FILL,SWT.TOP, true, true,1,1);
	      smartLabel.setLayoutData(smartLabelData);

	      

	      
	      //Read the XML file
	      smartDM = ms.getXmlDataModel();
	      //make a hashmap of the attributes to use for lookups
	      smartAttributeHash = new HashMap<String, String>();
	      smartAttributeDetailsHash = new HashMap<String, AttributeType>();
	      
	      
	      //Get the language codes
	      List<LanguageType> langs = smartDM.getLanguages().getLanguages();
	      codes = new String[langs.size()];
	      for (int i=0; i < langs.size(); i++){
	    	  codes[i] = langs.get(i).getCode();
	      }
	      
	      buildAttributeHash(codes[0]);
	    	  
	      
	      //Make the language selector
	      Label languageLabel = new Label(bottomRightFirstLine, SWT.NONE);
	      languageLabel .setText("Language:" );
	      GridData langLabelData = new GridData(SWT.RIGHT,SWT.TOP, false, false);
	      languageLabel.setLayoutData(langLabelData);
	      
	      
	      langSelector =  new Combo (bottomRightFirstLine, SWT.READ_ONLY);
	      langSelector.setItems(codes);
	      langSelector.setLayoutData(new GridData(SWT.FILL,SWT.RIGHT, false, false));
	      langSelector.select(0);

	      langSelector.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent arg0) {
				int selection = langSelector.getSelectionIndex();
	  			if (selection < codes.length ) {
	  				String lang = (String) codes[selection];
					regenerateSmartTree(lang);
	  			}

			}
	      });

	      
	      Label smartLabel1 = new Label(bottomRight, SWT.NONE);
	      smartLabel1.setText("Category:" );
	      smartLabel1.setLayoutData(new GridData(SWT.LEFT,SWT.FILL, false, true));
	  	  
	      //Make the category Tree
	      rootCategory = smartDM.getCategories().getCategories();
	      
	      tree = new Tree (bottomRight, SWT.BORDER | SWT.SINGLE);
	      TreeItem iItem = new TreeItem (tree, 0);
		  iItem.setText("Data Model");
	      addCategoriesToTree(iItem, rootCategory, codes[0]);

	      GridData treeData = new GridData(SWT.FILL,SWT.FILL, true, true);
	      treeData.heightHint = 130;
	      treeData.minimumHeight = 100;
	      tree.setLayoutData(treeData);
	      tree.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				treeSelectionChanged();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// nothing to do here.
			}
	      });

	      
	      tabFolder = new TabFolder (bottomRight, SWT.NONE);
	      tabFolder.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true,2,1));
	      

	      tab1 = new TabItem (tabFolder, SWT.NONE);
	      tab1.setText ("Attribute #1");
	      tab2 = new TabItem (tabFolder, SWT.NONE);
	      tab2.setText ("Attribute #2");
	      tab3 = new TabItem (tabFolder, SWT.NONE);
	      tab3.setText ("Attribute #3");
	      tab4 = new TabItem (tabFolder, SWT.NONE);
	      tab4.setText ("Attribute #4");
	      tab5 = new TabItem (tabFolder, SWT.NONE);
	      tab5.setText ("Attribute #5");
	      
	      
	      tab1Composite = new Composite(tabFolder, SWT.NONE);
	      tab1Composite.setLayout(new GridLayout());
	      tab2Composite = new Composite(tabFolder, SWT.NONE);
	      tab2Composite.setLayout(new GridLayout());
	      tab3Composite = new Composite(tabFolder, SWT.NONE);
	      tab3Composite.setLayout(new GridLayout());
	      tab4Composite = new Composite(tabFolder, SWT.NONE);
	      tab4Composite.setLayout(new GridLayout());
	      tab5Composite = new Composite(tabFolder, SWT.NONE);
	      tab5Composite.setLayout(new GridLayout());
	      

	      
	      
	      //Make Attribute #1 selector
	      attribute1 = new AttributeSelection(shell);
	      attribute1.init(codes, langSelector);
	      attribute1.CreateAttribute(tab1Composite,1);
	      
	      attribute2 = new AttributeSelection(shell);
	      attribute2.init(codes, langSelector);
	      attribute2.CreateAttribute(tab2Composite,2);
	      
	      attribute3 = new AttributeSelection(shell);
	      attribute3.init(codes, langSelector);
	      attribute3.CreateAttribute(tab3Composite,3);
	      
	      attribute4 = new AttributeSelection(shell);
	      attribute4.init(codes, langSelector);
	      attribute4.CreateAttribute(tab4Composite,4);
	      
	      attribute5 = new AttributeSelection(shell);
	      attribute5.init(codes, langSelector);
	      attribute5.CreateAttribute(tab5Composite,5);
	      
	      
	      tab1.setControl(tab1Composite);
	      tab2.setControl(tab2Composite);
	      tab3.setControl(tab3Composite);
	      tab4.setControl(tab4Composite);
	      tab5.setControl(tab5Composite);
	      tabFolder.pack();

	      //end of bottom Right elements--------------------------------------
	      
	      
	  	  //Bottom right Buttons
	      Composite buttons = new Composite(right, SWT.NONE);
		  GridLayout buttonsLayout = new GridLayout(3, false);
		  buttons.setLayout(buttonsLayout);
	    
	      GridData buttonsData = new GridData(SWT.FILL,SWT.BOTTOM, true, false,2,0);
	      buttons.setLayoutData(buttonsData);
	      

	      clear = new Button(buttons, SWT.NONE);
	      clear.setText("Clear Match");
	      clear.addSelectionListener(new SelectionAdapter() {	
		    	@Override
		    	public void widgetSelected(SelectionEvent e) {
		    		clearCurrentMatch(e);
		    		totalMatched.setText("Matched: " + ms.getNumMatched().toString() + " of " + ms.getNumTotal().toString() );		
	    			done.setVisible(false);
		    	}
		    });
	      
	      clear.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));
	      clear.setEnabled(false);

	      
	      autoMatch = new Button (buttons, SWT.CHECK);
	      autoMatch.setText("Auto-update matching observations");
	      autoMatch.setSelection(true);
	      
	      autoMatch.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, true, false));

	      next = new Button(buttons, SWT.NONE);
	      next.setText("Accept and Load Next Match");
	      next.addSelectionListener(new SelectionAdapter() {	
		    	@Override
		    	public void widgetSelected(SelectionEvent e) {
		    		saveCurrentMatch();
		    	}
		    });
	      next.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
	      next.setEnabled(false);	      

	      saveCurrentMatch();
	      
	      
  	      shell.pack();
          shell.open();
   
          
	      
          while (!shell.isDisposed()) {
                  if (!display.readAndDispatch()) display.sleep();
          }

          return 1;
	  }

	  





	protected void clearCurrentMatch(SelectionEvent e) {
		ms.setDirty(true);
		ISelection selection = viewer.getSelection();
		if (!selection.isEmpty()) {
			IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
			MatchRow selected = (MatchRow) structuredSelection.getFirstElement();
			selected.getSmartItem().setConcatKey("");
			selected.getSmartItem().setSmartAttributes(new String[]{"","","","","","","","","","","","","","","","","","","","","","","","","",""} );
			selected.setMatched(false);
		}
		tree.setSelection(tree.getItem(0));

		attribute1.clearValues();
		attribute2.clearValues();
		attribute3.clearValues();
		attribute4.clearValues();
		attribute5.clearValues();
			
		Table t = viewer.getTable();
		int currentSelection = t.getSelectionIndex();
		if(currentSelection != -1){
			t.getItem(currentSelection).setChecked(false);
		}
		ViewerSelectionChanged();
		viewer.refresh();
		
	}


	private void buildAttributeHash(String languageCode) {
	      for(AttributeType a : smartDM.getAttributes().getAttributes()){
	    	  String name = a.getNames().get(0).getValue();
	    	  for(int x=0; x < a.getNames().size(); x++){
				  if(a.getNames().get(x).getLanguageCode().equals(languageCode)){
					  name = a.getNames().get(x).getValue() + " (" + a.getType() + ")";
					  break;
				  }
			  }
	    	  smartAttributeHash.put(a.getKey(), name);
	    	  smartAttributeDetailsHash.put(a.getKey(), a);
	      }
	}


	protected void regenerateSmartTree(String lang) {
		  //tree.clearAll(true);
		  tree.removeAll();
	      TreeItem iItem = new TreeItem (tree, 0);
		  iItem.setText("Data Model");
	      addCategoriesToTree(iItem, rootCategory, lang);
		
	      buildAttributeHash(lang);
	      
	      next.setEnabled(false);
	  }


	private void addCategoriesToTree(TreeItem tree, List<CategoryType> cat, String languageCode) {
		  int i=0;
		  while(i < cat.size()){
			  String name = cat.get(i).getNames().get(0).getValue();
			  //String key = cat.get(i).getKey();
			  TreeItem iItem = new TreeItem (tree, 0);
			  for(int x=0; x < cat.get(i).getNames().size(); x++){
				  if(cat.get(i).getNames().get(x).getLanguageCode().equals(languageCode)){
					  name = cat.get(i).getNames().get(x).getValue();
					 //key = cat.get(i).getKey();
					  break;
				  }
			  }
			  iItem.setText(name);
			  iItem.setData(cat.get(i));
			  addCategoriesToTree(iItem, cat.get(i).getCategories(), languageCode);
			  i++;
		  }
	}
	


	protected void ViewerSelectionChanged() {
		ISelection selection = viewer.getSelection();
  		if (!selection.isEmpty()) {
  			mistText1.setEnabled(true);
  			mistText2.setEnabled(true);
  			mistText3.setEnabled(true);
  			mistText4.setEnabled(true);
  			mistText5.setEnabled(true);
  			mistText6.setEnabled(true);
  			mistText7.setEnabled(true);
  			mistText8.setEnabled(true);
  			mistText9.setEnabled(true);
  			
  			clear.setEnabled(true);
  			
  			tree.setEnabled(true);

  			
  			
  		    IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
  		    MatchRow selected = (MatchRow) structuredSelection.getFirstElement();
  		   
  		    mistText1.setText(selected.getMistItem().getCat1());
  		    mistText2.setText(selected.getMistItem().getCat2());
  		    mistText3.setText(selected.getMistItem().getCat3());
  		    mistText4.setText(selected.getMistItem().getCat4());
  		    mistText5.setText(selected.getMistItem().getCat5());
  		    mistText6.setText(selected.getMistItem().getCat6());
  		    mistText7.setText(selected.getMistItem().getCat7());
  		    mistText8.setText(selected.getMistItem().getCat8());
  		    mistText9.setText(selected.getMistItem().getCat9());
  		    
  		    SmartItem smartItem = selected.getSmartItem();
  		    
  		    
  			if(!(smartItem.getCategoryKey() == null)){
  					updateTree(smartItem.getCategoryKey(), tree.getItem(0),"");
  					treeSelectionChanged();
  					if(smartItem.getCategoryKey() == null || smartItem.getCategoryKey().equals("")){
  						//tabFolder.setEnabled(false);
  					}else{
  						attribute1.update(smartItem.getAttr1key(), smartItem.getB1(), smartItem.getText1(), smartItem.getList1(), smartItem.getTree1());
  						attribute2.update(smartItem.getAttr2key(), smartItem.getB2(), smartItem.getText2(), smartItem.getList2(), smartItem.getTree2());
  						attribute3.update(smartItem.getAttr3key(), smartItem.getB3(), smartItem.getText3(), smartItem.getList3(), smartItem.getTree3());
  						attribute4.update(smartItem.getAttr4key(), smartItem.getB4(), smartItem.getText4(), smartItem.getList4(), smartItem.getTree4());
  						attribute5.update(smartItem.getAttr5key(), smartItem.getB5(), smartItem.getText5(), smartItem.getList5(), smartItem.getTree5());
  					}
  			}
  			
  			

  		    
  		}
	}

	private boolean updateTree(String catKey, TreeItem ti, String prefix) {
		for(int x=0; x < ti.getItemCount(); x++){
			CategoryType c = (CategoryType)ti.getItem(x).getData();
			if(c != null){ 
				if( (prefix + c.getKey()).equals(catKey)){
					tree.setSelection(ti.getItem(x));
					return true;
				}else{
					if(updateTree(catKey, ti.getItem(x), prefix + c.getKey() + ".")) return true;
				}
			}
		}
		return false;
	}


	public void saveCurrentMatch(){
	
		    ms.setDirty(true);
		    save.setEnabled(true);
		  	ISelection selection = viewer.getSelection();
  			if (!selection.isEmpty()) {
  				IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
  				MatchRow selected = (MatchRow) structuredSelection.getFirstElement();
  				
  				if(tree.getSelection().length <1 ){
  					return;
  				}
  				TreeItem selectedTreeItem = tree.getSelection()[0];
  				CategoryType cat = (CategoryType)selectedTreeItem.getData();
  				if(cat == null){
  					MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR);
			    	messageBox.setMessage("The selected cateogory is invalid, please select a different category.");
			    	messageBox.open();
			    	return;
  				}
  				

  				String catKey= cat.getKey();
  				String fullKey = catKey;
  				//Get Category
  				while(selectedTreeItem.getParentItem() != null){
  					selectedTreeItem = selectedTreeItem.getParentItem();
  					if(selectedTreeItem.getData() != null){
  						fullKey = ((CategoryType)selectedTreeItem.getData()).getKey() + "." + fullKey;
  						catKey = fullKey;
  					}else{
  						break;
  					}
  				}
  				
  				//test Attributes for valid values then update the smartItem
  				if(!hasValidValue(attribute1, 1)){
  					return;
  				}
  				if(!hasValidValue(attribute2, 2)){
  					return;
  				}
  				if(!hasValidValue(attribute3, 3)){
  					return;
  				}
  				if(!hasValidValue(attribute4, 5)){
  					return;
  				}
  				if(!hasValidValue(attribute5, 5)){
  					return;
  				}
  				
  				fullKey += getAttributeString(attribute1);
  				fullKey += getAttributeString(attribute2);
  				fullKey += getAttributeString(attribute3);
  				fullKey += getAttributeString(attribute4);
  				fullKey += getAttributeString(attribute5);
  				
  				selected.getSmartItem().setCategoryKey(catKey);
  				selected.getSmartItem().updateItem(getSelectedAttributeKey(attribute1), getSelectedAttributeKey(attribute2), getSelectedAttributeKey(attribute3), getSelectedAttributeKey(attribute4), getSelectedAttributeKey(attribute5),
  						getBooleanText(attribute1), getAttributeText(attribute1), getAttributeList(attribute1), getAttributeTree(attribute1),
  						getBooleanText(attribute2), getAttributeText(attribute2), getAttributeList(attribute2), getAttributeTree(attribute2),
  						getBooleanText(attribute3), getAttributeText(attribute3), getAttributeList(attribute3), getAttributeTree(attribute3),
  						getBooleanText(attribute4), getAttributeText(attribute4), getAttributeList(attribute4), getAttributeTree(attribute4),
  						getBooleanText(attribute5), getAttributeText(attribute5), getAttributeList(attribute5), getAttributeTree(attribute5));
  				
  				selected.setSmartItem(fullKey);
  				selected.setMatched(true);
  				
  				if(autoMatch.getSelection()){
  					PerformAutoMatch();//runs before the viewer is moved down 1
  				}
  			}
  			viewer.refresh();
  			
  			Table t = viewer.getTable();
    		int currentSelection = t.getSelectionIndex();
    		if(currentSelection != -1){
    			t.getItem(currentSelection).setChecked(true);
    		}
    		t.deselect(currentSelection);
    		t.select(currentSelection + 1);
    		
    		//viewer.reveal(viewer.getSelection());
    		ViewerSelectionChanged();
  			viewer.refresh(true, true);
  			
  			//update count
			totalMatched.setText("Matched: " + ms.getNumMatched().toString() + " of " + ms.getNumTotal().toString() );		
			if( ms.getNumMatched() == ms.getNumTotal()){
				done.setVisible(true);
			}

	  }


	private boolean hasValidValue(AttributeSelection attribute, int attributeNumber) {
		if(!getSelectedAttributeKey(attribute).equals("") && getAttributeString(attribute).equals("empty")){
			MessageBox messageBox = new MessageBox(shell, SWT.ICON_ERROR);
			messageBox.setMessage("Attribute " + attributeNumber + " is selected, but no valid value is provided. Please select or enter a value.");
			messageBox.open();
			return false;
		}
		return true;
	}
	  
	  
	  private void PerformAutoMatch() {
		Table t = viewer.getTable();
		for(int x=0; x < t.getItemCount() ; x++){
			MatchRow r = (MatchRow)viewer.getElementAt(x);
			if(r.getMatched()){
				continue;//don't automatch anything that has already been completed.
			}else{
				MistItem mi = r.getMistItem();
				String group = mi.getCat1();
				String observation = mi.getCat2();
				String obCode = mi.getCat3();
				String subcode1 = mi.getCat5();
				if(group.equals(mistText1.getText()) && observation.equals(mistText2.getText()) && obCode.equals(mistText3.getText()) && subcode1.equals(mistText5.getText())){
					SmartItem si = r.getSmartItem();
					
					si.updateItem(getSelectedAttributeKey(attribute1), getSelectedAttributeKey(attribute2), getSelectedAttributeKey(attribute3), getSelectedAttributeKey(attribute4), getSelectedAttributeKey(attribute5),
							false,"","","",
							false,"","","",
							false,"","","",
							false,"","","",
							false,"","","");//update everything excepts the values

					//update the concatenated String
					TreeItem selectedTreeItem = tree.getSelection()[0];
					CategoryType cat = (CategoryType)selectedTreeItem.getData();
					

	  				
	  				
	  				String catKey= cat.getKey();
	  				//Get Category
	  				while(selectedTreeItem.getParentItem() != null){
	  					selectedTreeItem = selectedTreeItem.getParentItem();
	  					if(selectedTreeItem.getData() != null){
	  						catKey = ((CategoryType)selectedTreeItem.getData()).getKey() + "." + catKey;
	  					}else{
	  						break;
	  					}
	  				}

	  				si.setCategoryKey(catKey);
	  				//get Attributes
	  				String fullKey = catKey;
	  				fullKey += getConcatStringNoValues(attribute1);
	  				fullKey += getConcatStringNoValues(attribute2);
	  				fullKey += getConcatStringNoValues(attribute3);
	  				fullKey += getConcatStringNoValues(attribute4);
	  				fullKey += getConcatStringNoValues(attribute5);

					si.setConcatKey(fullKey);
				}
				
			}
			
		}
		
	  }


	  public TableViewer getViewer() {
		  return viewer;
	  }
	  
	  // create the columns for the table
	  private void createColumns(final Composite parent, final TableViewer viewer) {
	    String[] titles = { "Complete", "MIST Item", "SMART Item"};
	    int[] bounds = { 70, 240, 240};

	    // first column is for the checkbox
	    TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
	    col.setLabelProvider(new ColumnLabelProvider() {
	      @Override
	      public String getText(Object element) {
	    	  MatchRow row = (MatchRow) element;
	    	  if(row.getMatched()){
	    		  return "Complete";
	    	  }else{
	    		  return "";
	    	  }
	      }
	    });

	    // second column is for the MIST item
	    col = createTableViewerColumn(titles[1], bounds[1], 1);
	    col.setLabelProvider(new ColumnLabelProvider() {
	      @Override
	      public String getText(Object element) {
	        MatchRow row = (MatchRow) element;
	        MistItem m = row.getMistItem();
	        return m.getText();
	      }
	    });

	    // third col is the Smart Item
	    col = createTableViewerColumn(titles[2], bounds[2], 2);
	    col.setLabelProvider(new ColumnLabelProvider() {
	      @Override
	      public String getText(Object element) {
	    	MatchRow row = (MatchRow) element;
	        SmartItem s = row.getSmartItem();
	        return s.getText();
	      }
	    });


	  }
	  
	  private TableViewerColumn createTableViewerColumn(String title, int bound, final int colNumber) {
		    final TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
		    final TableColumn column = viewerColumn.getColumn();
		    column.setText(title);
		    column.setWidth(bound);
		    column.setResizable(true);
		    column.setMoveable(true);
		    return viewerColumn;
		  }
	  
	  
	  public Point getCenterPoint() {
			Shell parentShell = getParent();
			Rectangle shellBounds = parentShell.getBounds();
			return new Point(shellBounds.x + shellBounds.width / 2, (shellBounds.y + shellBounds.height) / 2);
		}
	  
	  private String getAttributeString(AttributeSelection attributeSelection){
		  String fullKey = "";
		  ISelection selection = attributeSelection.getAttrComboViwer().getSelection();
			if (!selection.isEmpty()) {
  				IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
  				Attribute selectedAttr = (Attribute) structuredSelection.getFirstElement();
  				String type = selectedAttr.getAttributeType().getType();
  				if (type==null){
  					return "";//no attribute selection, do nothing
  				}
  				String attrKey = selectedAttr.getKey();
  				
  				fullKey = fullKey + "|" + attrKey;
  				
  				if( type.equals("NUMERIC") || type.equals("TEXT") ){
  					if(attributeSelection.getValueText().getText().equals("")){
  						return "empty";
  					}
  					if(attributeSelection.getValueText().getText().equals(AttributeSelection.useTotalObservations) ){
  						fullKey += "|" + "k_TOTAL_OBSERVED" + "|dValue|1"; //TOTAL_OBSERVED is a keyword for the FME process to use a value for that particular observation.
  					}else if(attributeSelection.getValueText().getText().equals(AttributeSelection.useTotalMaleObservations) ){
  						fullKey += "|" + "k_TOTAL_MALES" + "|dValue|1"; // a keyword for the FME process to use a value for that particular observation.
  					}else if(attributeSelection.getValueText().getText().equals(AttributeSelection.useTotalFemaleObservations) ){
  						fullKey += "|" + "k_TOTAL_FEMALES" + "|dValue|1"; // a keyword for the FME process to use a value for that particular observation.
  					}else if(attributeSelection.getValueText().getText().equals(AttributeSelection.useTotalYoungObservations) ){
  						fullKey += "|" + "k_TOTAL_YOUNG" + "|dValue|1"; // a keyword for the FME process to use a value for that particular observation.
  					}else{
  						fullKey += "|" + attributeSelection.getValueText().getText() + "|dValue|1";
  					}
  				}else if( type.equals("LIST") ){
  					ISelection listSelection = attributeSelection.getValueComboViewer().getSelection();
  					if (!listSelection.isEmpty()) {
  						IStructuredSelection sListSelection = (IStructuredSelection) listSelection;      
  						ListOption listItem = (ListOption) sListSelection.getFirstElement();
  						String listKey = listItem.getKey();
  						fullKey += "|" + listKey + "|itemKey|1" ; //1 is the value multiplier
  					}else{
  						return "empty"; 
  					}
  						
  				}else if( type.equals("TREE") ){
  					if(attributeSelection.getValueTree().getSelection().length >0){
  						TreeItem treeItem = attributeSelection.getValueTree().getSelection()[0];
  						TreeNodeType data = (TreeNodeType)treeItem.getData();
  						if(data == null){
  							return "empty";
  						}else{
  							String treeKey = data.getKey();
  						
  							while(treeItem.getParentItem() != null){
  								treeItem = treeItem.getParentItem();
  								if(treeItem.getData() != null){
  									treeKey = ((TreeNodeType)treeItem.getData()).getKey() + "." + treeKey;
  								}else{
  									break;
  								}
  							}
  							fullKey += "|" + treeKey + "|itemKey|1" ; //1 is the value multiplier
  						}
  					}else{
  						return "empty"; 
  					}
					
  				}else if( type.equals("BOOLEAN") ){
  					if(attributeSelection.getValueCombo().getText().equals("")){
  						return "empty";
  					}
  					fullKey += "|" + attributeSelection.getValueCombo().getText() + "|bValue|1";
  				}
  				
			}
			return fullKey;
	  }

	  private String getConcatStringNoValues(AttributeSelection attributeSelection){
		  String fullKey = "";
		  ISelection selection = attributeSelection.getAttrComboViwer().getSelection();
			if (!selection.isEmpty()) {
  				IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
  				Attribute selectedAttr = (Attribute) structuredSelection.getFirstElement();
  				String type = selectedAttr.getAttributeType().getType();
  				String attrKey = selectedAttr.getKey();
  				
  				fullKey = fullKey + "|" + attrKey;
  				if(type == null){
  					return "";
  				}
  				if( type.equals("NUMERIC") || type.equals("TEXT") ){
  					fullKey += "|" + "" + "|dValue|1";
  				}else if( type.equals("LIST") ){
  						fullKey += "|" + "" + "|itemKey|1" ; //1 is the value multiplier
  				}else if( type.equals("TREE") ){
		    			fullKey += "|" + "" + "|itemKey|1" ; //1 is the value multiplier
  				}else if( type.equals("BOOLEAN") ){
  					fullKey += "|" + "" + "|bValue|1";
  				}
			}
			return fullKey;
	  }

	  
	  
	  private String getSelectedAttributeKey(AttributeSelection attributeSelection){
		  ISelection selection = attributeSelection.getAttrComboViwer().getSelection();
			if (!selection.isEmpty()) {
  				IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
  				Attribute selectedAttr = (Attribute) structuredSelection.getFirstElement();
  				return selectedAttr.getKey();
			}
			return "";
	  }
	  
	  private String getAttributeText(AttributeSelection attributeSelection){
		  return attributeSelection.getValueText().getText();
	  }
	  
	  private boolean getBooleanText(AttributeSelection attributeSelection){
		  if(attributeSelection.getValueCombo().getText().equals("TRUE")){
			  return true;
		  }else if(attributeSelection.getValueCombo().getText().equals("FALSE")){
			  return false;
		  }else{
			  return false;
		  }
	  }
	  
	  private String getAttributeList(AttributeSelection attributeSelection){
		  ISelection listSelection = attributeSelection.getValueComboViewer().getSelection();
		  if (!listSelection.isEmpty()) {
			  IStructuredSelection sListSelection = (IStructuredSelection) listSelection;      
			  ListOption listItem = (ListOption) sListSelection.getFirstElement();
			  return listItem.getKey();
		  }else{
			  return "";
		  }
	  }
	  
	  private String getAttributeTree(AttributeSelection attributeSelection){
		  	if(attributeSelection.getValueTree().getSelection().length == 0) return "";
		  	TreeItem treeItem = attributeSelection.getValueTree().getSelection()[0];
  			TreeNodeType data = (TreeNodeType)treeItem.getData();
  			if(data == null){
  				return "";
  			}
  			String treeKey = data.getKey();

			while(treeItem.getParentItem() != null){
				treeItem = treeItem.getParentItem();
				if(treeItem.getData() != null){
					treeKey = ((TreeNodeType)treeItem.getData()).getKey() + "." + treeKey;
				}else{
					break;
				}
			}
			
			return treeKey;
	  }
	  
		private void treeSelectionChanged() {
			next.setEnabled(true);
		    
			ArrayList<Attribute> attributeArray = new ArrayList<Attribute>();
			Attribute blankAttribute = new Attribute("<None>" , "", new AttributeType());
			attributeArray.add(blankAttribute);
			
			if(tree.getSelectionCount() == 0)return;
			TreeItem selectedTreeItem = tree.getSelection()[0];
			while(selectedTreeItem != null){
				CategoryType cat = (CategoryType)selectedTreeItem.getData();
				if(cat != null){
					List<CategoryAttributeLink> attributes = cat.getAttributes();
					for(CategoryAttributeLink attr : attributes){
						String key = attr.getAttributekey(); 
						Attribute newAttribute = new Attribute(smartAttributeHash.get(key) , attr.getAttributekey(), smartAttributeDetailsHash.get(key));
						attributeArray.add(newAttribute);
					}

				}
				selectedTreeItem = selectedTreeItem.getParentItem();
			}
			attribute1.clearValues();
			attribute2.clearValues();
			attribute3.clearValues();
			attribute4.clearValues();
			attribute5.clearValues();
			
			attribute1.getAttrComboViwer().setInput(attributeArray);
			attribute2.getAttrComboViwer().setInput(attributeArray);
			attribute3.getAttrComboViwer().setInput(attributeArray);
			attribute4.getAttrComboViwer().setInput(attributeArray);
			attribute5.getAttrComboViwer().setInput(attributeArray);
			
			tabFolder.setEnabled(true);
		}

}

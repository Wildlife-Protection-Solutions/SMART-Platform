
package org.wcs.smart.datamodelmatcher.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Dialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.wcs.smart.internal.ca.datamodel.xml.generate.AttributeType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryAttributeLink;
import org.wcs.smart.internal.ca.datamodel.xml.generate.CategoryType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.DataModel;
import org.wcs.smart.internal.ca.datamodel.xml.generate.LanguageType;
import org.wcs.smart.internal.ca.datamodel.xml.generate.ListNode;

import sun.reflect.generics.tree.BottomSignature;


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
	
	private ComboViewer smartCombo2;
	private Composite value1;
	private Combo value1Combo;
	private ComboViewer value1ComboViewer;
	private Text value1Text;
    private Tree value1Tree;
	
	private ComboViewer smartCombo3;
	public final Shell shell;
	
	private Button save;
	private Button next;
	private Button done;
	
	public MatchSessionDialog(Shell parentShell, MatchSession ms) {
	    super(parentShell);
        shell = new Shell(parentShell, SWT.SHELL_TRIM);
        Point size = shell.computeSize(-1, -1);
        Rectangle screen = shell.getDisplay().getMonitors()[0].getBounds();
        shell.setBounds(50, 50, size.x, size.y);    
        shell.setText("Match Session - " + ms.getSaveLocation());
	    this.ms = ms;
	  }
	
	
	  public int open() {

          
          GridLayout layout = new GridLayout(1, false);
		  shell.setLayout(layout);
		    
		  GridData gridData = new GridData(SWT.FILL,SWT.FILL, true, true);
		  shell.setLayoutData(gridData);
		    
		    
          // (widget creation, set result, etc).
        
          //main composite and layout
  		  Composite main = new Composite(shell, SWT.None);
  		  GridLayout mlayout = new GridLayout(2, true);
  	      main.setLayout(mlayout);
  	      GridData mainGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
  	      main.setLayoutData(mainGridData);

  	      //left
  	      Composite left = new Composite(main, SWT.None);
		  GridLayout llayout = new GridLayout(3, false);
	      left.setLayout(llayout);
	    
	      GridData leftGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
	      left.setLayoutData(leftGridData);
	      	      
  	    
	      //left - content
	      // define the TableViewer---------------------------------------------------------------------------------
	      viewer = new TableViewer(left, SWT.CHECK | SWT.MULTI | SWT.H_SCROLL
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
	      viewer.setComparator(sorter);
	
	   // define layout for the viewer
	      GridData tgridData = new GridData(SWT.FILL,SWT.FILL, true, true,3,0);
	      //tgridData.widthHint = 510;
	      tgridData.heightHint = 500;
	      viewer.getControl().setLayoutData(tgridData);
	      
	      viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent arg0) {		
				ViewerSelectionChanged();
			}
	      });
	      

	   // end Viewer---------------------------------------------------------------------------------
	      

	      totalMatched = new Label(left, SWT.READ_ONLY);
	      totalMatched.setText("Matched: " + ms.getNumMatched().toString() + " of " + ms.getNumTotal().toString() );
	      GridData totalGridData = new GridData(SWT.FILL,SWT.FILL, false, false);
	      totalGridData.widthHint = 115;
	      totalMatched.setLayoutData(totalGridData);
	      
	      done = new Button(left, SWT.NONE);
	      done.setText("Matching Complete - Exit");
	      GridData doneGridData = new GridData(SWT.CENTER,SWT.FILL, false, false);
	      done.setLayoutData(doneGridData);
	      done.setVisible(false);
	      done.addSelectionListener(new SelectionAdapter() {	
		    	@Override
		    	public void widgetSelected(SelectionEvent e) {
		    		ms.save();
		    		shell.dispose();
		    	}
		    });
	      
	      
	      save = new Button(left, SWT.NONE);
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
  	      Composite right = new Composite(main, SWT.None);
		  GridLayout rlayout = new GridLayout(1, false);
	      right.setLayout(rlayout);
	    
	      GridData rightGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
	      right.setLayoutData(rightGridData);
	      
	      //Top Right composite
	      Composite topRight = new Composite(right, SWT.BORDER);
		  GridLayout trlayout = new GridLayout(2, false);
	      topRight.setLayout(trlayout);
	    
	      GridData topRightGridData = new GridData(SWT.FILL,SWT.CENTER, true, false);
	      topRight.setLayoutData(topRightGridData);
	      
	      //Top Right elements---------------------------------------------
	      Label mistLabel = new Label(topRight, SWT.NONE);
	      mistLabel.setText("MIST Observation:" );
	      FontData fontData = mistLabel.getFont().getFontData()[0];
	      Font font = new Font(getParent().getDisplay(), new FontData(fontData.getName(), fontData
	    		    .getHeight(), SWT.BOLD));
	      mistLabel.setFont(font);
	      GridData mistItemLabel = new GridData(SWT.FILL,SWT.CENTER, true, false,2,1);
	      mistLabel.setLayoutData(mistItemLabel);

 
	      
	      Label mistLabel1 = new Label(topRight, SWT.NONE);
	      mistLabel1.setText("Observation Group:" );
	      
	      mistText1 = new Text(topRight, SWT.NONE);
	      GridData m1 = new GridData(SWT.FILL,SWT.CENTER, true, false);
	      m1.widthHint = 250;
	      mistText1.setLayoutData(m1);
	      
	      mistText1.setEnabled(false);
	      
	      Label mistLabel2 = new Label(topRight, SWT.NONE);
	      mistLabel2.setText("Observation:" );
	      
	      mistText2 = new Text(topRight, SWT.NONE);
	      mistText2.setLayoutData(m1);
	      mistText2.setEnabled(false);
	      
	      Label mistLabel3 = new Label(topRight, SWT.NONE);
	      mistLabel3.setText("Observation Code:" );
	      
	      mistText3 = new Text(topRight, SWT.NONE);
	      mistText3.setLayoutData(m1);
	      mistText3.setEnabled(false);
	      
	      Label mistLabel4 = new Label(topRight, SWT.NONE);
	      mistLabel4.setText("Item:" );
	      
	      mistText4 = new Text(topRight, SWT.NONE);
	      mistText4.setLayoutData(m1);
	      mistText4.setEnabled(false);
	      
	      Label mistLabel5 = new Label(topRight, SWT.NONE);
	      mistLabel5.setText("Subcode 1:" );
	      
	      mistText5 = new Text(topRight, SWT.NONE);
	      mistText5.setLayoutData(m1);
	      mistText5.setEnabled(false);
	      
	      Label mistLabel6 = new Label(topRight, SWT.NONE);
	      mistLabel6.setText("Subcode 2:" );
	      
	      mistText6 = new Text(topRight, SWT.NONE);
	      mistText6.setLayoutData(m1);
	      mistText6.setEnabled(false);
	      
	      Label mistLabel7 = new Label(topRight, SWT.NONE);
	      mistLabel7.setText("Subcode 3:" );
	      
	      mistText7 = new Text(topRight, SWT.NONE);
	      mistText7.setLayoutData(m1);
	      mistText7.setEnabled(false);
	      
	      Label mistLabel8 = new Label(topRight, SWT.NONE);
	      mistLabel8.setText("Subcode 4:" );
	      
	      mistText8 = new Text(topRight, SWT.NONE);
	      mistText8.setLayoutData(m1);
	      mistText8.setEnabled(false);
	      
	      Label mistLabel9 = new Label(topRight, SWT.NONE);
	      mistLabel9.setText("Subcode 5:" );
	      
	      mistText9 = new Text(topRight, SWT.NONE);
	      mistText9.setLayoutData(m1);
	      mistText9.setEnabled(false);
	      //end of Top Right elements--------------------------------------
	      	      
	      
	      
	      //bottom Right composite
	      Composite bottomRight = new Composite(right, SWT.BORDER);
		  GridLayout brlayout = new GridLayout(2, false);
		  bottomRight.setLayout(brlayout);
	    
	      GridData bottomRightGridData = new GridData(SWT.FILL,SWT.FILL, true, true);
	      bottomRight.setLayoutData(bottomRightGridData);
	      

	      //Bottom Right elements---------------------------------------------
	      //top line spacing, label and language drop down
	      Composite bottomRightFirstLine = new Composite(bottomRight, SWT.NONE);
		  GridLayout brfllayout = new GridLayout(3, false);
		  bottomRightFirstLine.setLayout(brfllayout);
		  bottomRightFirstLine.setLayoutData(new GridData(SWT.FILL,SWT.TOP, true, true,2,1));
	      
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
	      
	      buildAttributeHash("");
	    	  
	      
	      //Make the language selector
	      Label languageLabel = new Label(bottomRightFirstLine, SWT.NONE);
	      languageLabel .setText("Language:" );
	      GridData langLabelData = new GridData(SWT.RIGHT,SWT.TOP, false, false);
	      languageLabel.setLayoutData(langLabelData);
	      
	      
	      List<LanguageType> langs = smartDM.getLanguages().getLanguages();
	      codes = new String[langs.size()];
	      for (int i=0; i < langs.size(); i++){
	    	  codes[i] = langs.get(i).getCode();
	      }
	      langSelector =  new Combo (bottomRightFirstLine, SWT.READ_ONLY);
	      langSelector.setItems(codes);
	      langSelector.setLayoutData(new GridData(SWT.FILL,SWT.RIGHT, false, false));

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
	      addCategoriesToTree(iItem, rootCategory, "en");

	      GridData treeData = new GridData(SWT.FILL,SWT.FILL, true, true);
	      treeData.heightHint = 120;
	      tree.setLayoutData(treeData);
	      tree.addSelectionListener(new SelectionListener() {
			
			@Override
			public void widgetSelected(SelectionEvent arg0) {
				ArrayList<Attribute> attributeArray = new ArrayList<Attribute>();
				
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
				smartCombo2.setInput(attributeArray);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent arg0) {
				// TODO Auto-generated method stub
				
			}
	      });


	      //Make an Attribute selector 
	      Label smartLabel2 = new Label(bottomRight, SWT.NONE);
	      smartLabel2.setText("Attribute #1:" );
	      smartLabel2.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
	      
	      smartCombo2 =  new ComboViewer (bottomRight, SWT.READ_ONLY);
	      smartCombo2.setContentProvider(ArrayContentProvider.getInstance());
	      smartCombo2.setLabelProvider(new LabelProvider() {
	    	  @Override
	    	  public String getText(Object element) {
	    	    return ((Attribute)element).getText();
	    	  }
	    	});
	      smartCombo2.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

	      smartCombo2.addSelectionChangedListener(new ISelectionChangedListener() {
			
	    	  @Override
	    	  public void selectionChanged(SelectionChangedEvent arg0) {
	    			ISelection selection = smartCombo2.getSelection();
	    	  		if (!selection.isEmpty()) {
	    	  		    IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
	    	  		    Attribute selected = (Attribute) structuredSelection.getFirstElement();
	    	  		    String type = selected.getAttributeType().getType();
	    	  		    Boolean isRequired = selected.getAttributeType().isIsrequired();
	    	  		    if( type.equals("NUMERIC") ){
	    	  		    	value1Text.setText("");
	    	  		    	((StackLayout)value1.getLayout()).topControl = value1Text;
	    	  		    }else if( type.equals("TEXT") ){
	    	  		    	value1Text.setText("");
	    	  		    	((StackLayout)value1.getLayout()).topControl = value1Text;
	    	  		    }else if( type.equals("LIST") ){
	    	  		    	String name;
	    	  		    	String lang;
	    	  		    	ArrayList<ListOption> options = new ArrayList<ListOption>();
	    	  		    	List<ListNode> list = selected.getAttributeType().getValues();
	    	  		    	int langIndex = langSelector.getSelectionIndex();

	    	  		    	if (langIndex != -1 && langIndex < codes.length && codes != null) {
	    		  				lang = (String) codes[langIndex];
	    	  		    	}else{
	    	  		    		lang = "";
	    	  		    	}
	    		  			for(ListNode i : list){
		    					name = i.getNames().get(0).getValue();
		    					for(int x=0; x < i.getNames().size(); x++){
		    						if(i.getNames().get(x).getLanguageCode().equals(lang)){
		    							name = i.getNames().get(x).getValue();
		    							break;
		    						}
		    					}
		    					options.add(new ListOption(name, i.getKey()));
		    				}
	    	  		    	value1ComboViewer.setInput(options);
	    	  		    	((StackLayout)value1.getLayout()).topControl = value1ComboViewer.getControl();
	    	  		    }else if( type.equals("TREE") ){
	    	  		    	((StackLayout)value1.getLayout()).topControl = value1Tree;
	    	  			}else if( type.equals("BOOLEAN") ){
	    	  				((StackLayout)value1.getLayout()).topControl = value1Combo;
	    	  		    }
	    	  		    value1.layout();
	    	  		}
	    	  }
	      });
	      
	  	  //smartCombo2.setItems (new String [] {});
	  	  //smartCombo2.setEnabled(false);
	  	  //smartCombo2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	      

	      
	      Label smartLabel3 = new Label(bottomRight, SWT.NONE);
	      smartLabel3.setText("Value #1:" );
	      smartLabel3.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));

	      value1 = new Composite(bottomRight, SWT.NONE);
	      value1.setLayout(new StackLayout());
	      value1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	      
	      value1Combo = new Combo(value1, SWT.DROP_DOWN | SWT.SIMPLE | SWT.READ_ONLY);
	      value1Combo.setItems(new String[]{"TRUE", "FALSE"});
	      value1Combo.setLayoutData(new GridData(SWT.LEFT, SWT.FILL, false, false));

	      value1ComboViewer =  new ComboViewer (value1, SWT.READ_ONLY);
	      value1ComboViewer.setContentProvider(ArrayContentProvider.getInstance());
	      value1ComboViewer.setLabelProvider(new LabelProvider() {
	    	  @Override
	    	  public String getText(Object element) {
	    	    return ((ListOption)element).getName();
	    	  }
	    	});
	      value1ComboViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
	      
	      
	      value1Text = new Text(value1, SWT.BORDER);
	      GridData gridtxt1Data = new GridData(SWT.FILL, SWT.TOP, false, false);
	      gridData.widthHint = 100;
	      value1Text.setLayoutData(gridtxt1Data);
	      
	      value1Tree = new Tree(value1, SWT.BORDER | SWT.SINGLE);
	      TreeItem vItem = new TreeItem (value1Tree, 0);
	      vItem.setText("<Select an Attribute to populate possible values>");
	      
	      GridData gridtree1Data = new GridData(SWT.FILL, SWT.FILL, true, true);
	      gridData.heightHint = 100;
	      value1Tree.setLayoutData(gridtree1Data);
	      
	      ((StackLayout)value1.getLayout()).topControl = value1Combo;
	      
	      
//	      smartCombo3 =  new Combo (bottomRight, SWT.READ_ONLY);
	//  	  smartCombo3.setItems (new String [] {});
	  //	  smartCombo3.setEnabled(false);
	      //end of bottom Right elements--------------------------------------
	      
	      
	  	  //Bottom right Buttons
	      Composite buttons = new Composite(right, SWT.NONE);
		  GridLayout buttonsLayout = new GridLayout(2, false);
		  buttons.setLayout(buttonsLayout);
	    
	      GridData buttonsData = new GridData(SWT.RIGHT,SWT.BOTTOM, true, false,2,0);
	      buttons.setLayoutData(buttonsData);


	      next = new Button(buttons, SWT.NONE);
	      next.setText("Accept and Load Next Match");
	      next.addSelectionListener(new SelectionAdapter() {	
		    	@Override
		    	public void widgetSelected(SelectionEvent e) {
		    		saveCurrentMatch(e);
		    		totalMatched.setText("Matched: " + ms.getNumMatched().toString() + " of " + ms.getNumTotal().toString() );		
		    		if( ms.getNumMatched() == ms.getNumTotal()){
		    			done.setVisible(true);
		    		}
		    	}
		    });
	      

	      
  	      shell.pack();
          shell.open();
          Display display = shell.getDisplay();
          
          
	      
          while (!shell.isDisposed()) {
                  if (!display.readAndDispatch()) display.sleep();
          }

          return 1;
	  }

	  


	private void buildAttributeHash(String languageCode) {
	      for(AttributeType a : smartDM.getAttributes().getAttributes()){
	    	  String name = a.getNames().get(0).getValue();
	    	  for(int x=0; x < a.getNames().size(); x++){
				  if(a.getNames().get(x).getLanguageCode().equals(languageCode)){
					  name = a.getNames().get(x).getValue();
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
  			
  			tree.setEnabled(true);
  			//TODO enable other attr/value fields
  			
  			
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
  		}
	}

	public void saveCurrentMatch(SelectionEvent e){
		    
		  	ISelection selection = viewer.getSelection();
  			if (!selection.isEmpty()) {
  				IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
  				MatchRow selected = (MatchRow) structuredSelection.getFirstElement();
  				
  				TreeItem selectedTreeItem = tree.getSelection()[0];
  				CategoryType cat = (CategoryType)selectedTreeItem.getData();
  				String fullKey = cat.getKey();
  				while(selectedTreeItem.getParentItem() != null){
  					selectedTreeItem = selectedTreeItem.getParentItem();
  					if(selectedTreeItem.getData() != null){
  						fullKey = ((CategoryType)selectedTreeItem.getData()).getKey() + "." + fullKey;
  					}else{
  						break;
  					}
  				}
  				selected.setSmartItem(fullKey);
  			}
  			viewer.refresh();
  			
  			Table t = viewer.getTable();
    		int currentSelection = t.getSelectionIndex();
    		if(currentSelection != -1){
    			t.getItem(currentSelection).setChecked(true);
    		}
    		t.deselect(currentSelection);
    		t.select(currentSelection + 1);
    		
    		
    		ViewerSelectionChanged();
  			viewer.refresh();
	  }
	  
	  
	  public TableViewer getViewer() {
		  return viewer;
	  }
	  
	// create the columns for the table
	  private void createColumns(final Composite parent, final TableViewer viewer) {
	    String[] titles = { "", "MIST Item", "SMART Item"};
	    int[] bounds = { 30, 240, 240};

	    // first column is for the checkbox
	    TableViewerColumn col = createTableViewerColumn(titles[0], bounds[0], 0);
	    col.setLabelProvider(new ColumnLabelProvider() {
	      @Override
	      public String getText(Object element) {
	    	  MatchRow row = (MatchRow) element;
	    	  //TODO check the boxes for ones that are matched when opening a file, I think it should be here somehow.
	    	  //also if we re-order by sorting hopefully this could be run again to reset the boxes?
	    	  if(row.getMatched()){
	    		  return "";
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
}

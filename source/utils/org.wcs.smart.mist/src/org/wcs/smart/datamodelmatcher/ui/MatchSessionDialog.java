
package org.wcs.smart.datamodelmatcher.ui;

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
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;



public class MatchSessionDialog extends Dialog {

	private MatchSession ms;
	private TableViewer viewer;
	// static fields to hold the images
	//private static final Image CHECKED = Activator.getImageDescriptor("icons/checked.gif").createImage(); 
	//private static final Image UNCHECKED = Activator.getImageDescriptor("icons/unchecked.gif").createImage();

	private Label totalMatched;
	  
	private Text mistText1;
	private Text mistText2;
	private Text mistText3;
	
	private Combo smartCombo1;
	private Combo smartCombo2;
	private Combo smartCombo3;
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
	      //viewer.setComparator(new TableColumnSorter(viewer));
	      
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
	      GridData saveGridData = new GridData(SWT.RIGHT,SWT.FILL, true, true);
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
	    
	      GridData rightGridData = new GridData(SWT.FILL,SWT.CENTER, true, false);
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
	      mistLabel3.setText("Remark:" );
	      
	      mistText3 = new Text(topRight, SWT.NONE);
	      mistText3.setLayoutData(m1);
	      mistText3.setEnabled(false);
	      //end of Top Right elements--------------------------------------
	      
	      
	      
	      //bottom Right composite
	      Composite bottomRight = new Composite(right, SWT.BORDER);
		  GridLayout brlayout = new GridLayout(2, false);
		  bottomRight.setLayout(brlayout);
	    
	      GridData bottomRightGridData = new GridData(SWT.FILL,SWT.CENTER, true, false);
	      bottomRight.setLayoutData(bottomRightGridData);
	      
	      //Bottom Right elements---------------------------------------------
	      Label smartLabel = new Label(bottomRight, SWT.NONE);
	      smartLabel.setText("MIST Observation:" );
	      FontData sfontData = smartLabel.getFont().getFontData()[0];
	      Font sfont = new Font(getParent().getDisplay(), new FontData(sfontData.getName(), sfontData
	    		    .getHeight(), SWT.BOLD));
	      smartLabel.setFont(sfont);
	      GridData smartLabelData = new GridData(SWT.FILL,SWT.CENTER, true, false,2,1);
	      smartLabel.setLayoutData(smartLabelData);
	      
	      Label smartLabel1 = new Label(bottomRight, SWT.NONE);
	      smartLabel1.setText("Category:" );
	      
	      smartCombo1 =  new Combo (bottomRight, SWT.READ_ONLY);
	  	  smartCombo1.setItems (new String [] {"Alpha", "Bravo", "Charlie"});
	  	  smartCombo1.setEnabled(false);
	  	  smartCombo1.addModifyListener(new ModifyListener() {
			
			@Override
			public void modifyText(ModifyEvent arg0) {
				save.setEnabled(true);
			}
	  	  });


	      
	      Label smartLabel2 = new Label(bottomRight, SWT.NONE);
	      smartLabel2.setText("Attribute1:" );
	      
	      smartCombo2 =  new Combo (bottomRight, SWT.READ_ONLY);
	  	  smartCombo2.setItems (new String [] {"Alpha", "Bravo", "Charlie"});
	  	smartCombo2.setEnabled(false);
	      
	      
	      
	      Label smartLabel3 = new Label(bottomRight, SWT.NONE);
	      smartLabel3.setText("Remark:" );
	      
	      smartCombo3 =  new Combo (bottomRight, SWT.READ_ONLY);
	  	  smartCombo3.setItems (new String [] {"Alpha", "Bravo", "Charlie"});
	  	smartCombo3.setEnabled(false);
	      //end of bottom Right elements--------------------------------------
	  	  
	  	  //Bottom right Buttons
	      Composite buttons = new Composite(right, SWT.NONE);
		  GridLayout buttonsLayout = new GridLayout(2, false);
		  buttons.setLayout(buttonsLayout);
	    
	      GridData buttonsData = new GridData(SWT.RIGHT,SWT.BOTTOM, true, false,2,0);
	      buttons.setLayoutData(buttonsData);


	      next = new Button(buttons, SWT.NONE);
	      next.setText("Next");
	      next.addSelectionListener(new SelectionAdapter() {	
		    	@Override
		    	public void widgetSelected(SelectionEvent e) {
		    		saveCurrentMatch();
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

	  protected void ViewerSelectionChanged() {
		ISelection selection = viewer.getSelection();
  		if (!selection.isEmpty()) {
  			mistText1.setEnabled(true);
  			mistText2.setEnabled(true);
  			mistText3.setEnabled(true);
  			smartCombo1.setEnabled(true);
  			smartCombo2.setEnabled(true);
  			smartCombo3.setEnabled(true);
  		    IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
  		    MatchRow selected = (MatchRow) structuredSelection.getFirstElement();
  		   
  		    mistText1.setText(selected.getMistItem().getText());
  		    mistText2.setText(selected.getMistItem().getText());
  		    mistText3.setText(selected.getMistItem().getText());
  		}
	}

	public void saveCurrentMatch(){
		    
		  	ISelection selection = viewer.getSelection();
  			if (!selection.isEmpty()) {
  				IStructuredSelection structuredSelection = (IStructuredSelection) selection;      
  				MatchRow selected = (MatchRow) structuredSelection.getFirstElement();
	    		selected.setSmartItem(smartCombo1.getText());
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
			Shell parentShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
			Rectangle shellBounds = parentShell.getBounds();
			return new Point(shellBounds.x + shellBounds.width / 2, (shellBounds.y + shellBounds.height) / 2);
		}
}

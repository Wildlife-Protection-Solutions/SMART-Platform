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
package org.wcs.smart.query.ui.summary;

import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.query.model.SummaryHeader;
import org.wcs.smart.query.model.SummaryQueryResult;

/**
 * Summary results table which display the results
 * of the summary query in a table.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryResultTable extends Composite {

	private static final int DEFAULT_COL_SIZE = 100;
	private static final int TABLE_HEADER_COLOR = SWT.COLOR_WIDGET_LIGHT_SHADOW;
	private static final int TABLE_HEADER_COLOR_2 = SWT.COLOR_WIDGET_LIGHT_SHADOW;
	
	private TableViewer topTable;
	private TableViewer leftTable;
	private TableViewer mainTable;
	
	private Cursor ewCursor = null;
	
	private SummaryQueryResult results;
	private Label lblSpacer;
	
	
	
	/**
	 * Creates a new summary results table
	 * 
	 * @param parent parent composite
	 * @param results results to display
	 * @param toolkit form tookit
	 */
	public SummaryResultTable(Composite parent, SummaryQueryResult results, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		
		this.results = results;
		createComposite(toolkit);
		
		if (toolkit != null){
			toolkit.adapt(this);
		}
		ewCursor = Display.getDefault().getSystemCursor(SWT.CURSOR_SIZEWE);
	}
	
	
	private void createComposite(FormToolkit toolkit){
		//setup layout
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = layout.horizontalSpacing = layout.verticalSpacing = 0;
		setLayout(layout);
		
		if (toolkit == null){
			lblSpacer = new Label(this, SWT.NONE);
		}else{
			lblSpacer = toolkit.createLabel(this, "");
		}
		lblSpacer.setVisible(false);
		lblSpacer.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		createTopTable();
		createLeftTable();
		createMainTable();

		// sync selection the same in both tables
		leftTable.getTable().addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				mainTable.getTable().setSelection(
						leftTable.getTable().getSelectionIndices());
			}
		});
		mainTable.getTable().addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				leftTable.getTable().setSelection(
						mainTable.getTable().getSelectionIndices());
			}
		});

		//sync position on all tables
		
		//top table  
		final ScrollBar hBar = mainTable.getTable().getHorizontalBar();
		Listener hlistener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				topTable.getTable().setLocation(-hBar.getSelection() , 0);
			}
		};
		
		ScrollBar vBarMain = mainTable.getTable().getVerticalBar();
		Listener vlistener = new Listener() {
			public void handleEvent(Event event) {
				leftTable.getTable().setTopIndex(mainTable.getTable().getTopIndex());
			}
		};
		hBar.addListener(SWT.Selection, hlistener);
		vBarMain.addListener(SWT.Selection, vlistener);
		mainTable.getTable().addListener(SWT.Selection, vlistener);
		mainTable.getTable().addListener(SWT.Resize, vlistener);
		mainTable.getTable().addListener(SWT.Resize, hlistener);
		//TODO fix this
		mainTable.getTable().addListener(SWT.Traverse, hlistener);
		
		mainTable.getTable().addListener(SWT.Resize, new Listener(){
			@Override
			public void handleEvent(Event event) {				
				Rectangle top = topTable.getTable().getBounds();
				Rectangle r = mainTable.getTable().getBounds();
				Point pnt = mainTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT);

				topTable.getTable().setBounds(top.x,top.y,Math.max(r.width,pnt.x),top.height);
				topTable.getTable().getParent().layout();
			}
			
		});
		
		topTable.setInput(results);
		leftTable.setInput(results);
		mainTable.setInput(results);
		
		//Size Top Table
		Point p = topTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Rectangle r = mainTable.getTable().getBounds();
		topTable.getTable().setBounds(0, 0, r.width, p.y);
		
		
	}
	
	private void createMainTable(){
		mainTable = new TableViewer(this, SWT.VIRTUAL | SWT.BORDER | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL );
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.heightHint = 200;
		mainTable.getTable().setLayoutData(gd);
		mainTable.getTable().setHeaderVisible(false);
		mainTable.getTable().setLinesVisible(true);
		for (int i = 0; i < results.getNumDataColumns(); i ++){
			TableViewerColumn tvc = new TableViewerColumn(mainTable, SWT.NONE);
			tvc.getColumn().setWidth(DEFAULT_COL_SIZE);
		}
		mainTable.setLabelProvider(new SummaryResultDataLabelProvider());
		mainTable.setContentProvider(new SummaryResultDataContentProvider());
	}
	
	
	private void createLeftTable(){
		leftTable = new TableViewer(this, SWT.VIRTUAL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION | SWT.NO_SCROLL);
		leftTable.getTable().setHeaderVisible(false);
		leftTable.getTable().setLinesVisible(true);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true);
		gd.heightHint = 200;
		leftTable.getTable().setLayoutData(gd);
		leftTable.getTable().setBackground(Display.getDefault().getSystemColor(TABLE_HEADER_COLOR));
		for (int i = 0; i < results.getRowHeaders().size(); i++){
			TableViewerColumn tvc = new TableViewerColumn(leftTable, SWT.NONE);
			tvc.getColumn().setWidth(DEFAULT_COL_SIZE);
		}
		
		leftTable.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return results.getRowHeaderValues();
			}
		});
		leftTable.setLabelProvider(new SummaryHeaderLabelProvider());
	}
	
	
	private void createTopTable(){
		final Composite topTableComp = new Composite(this, SWT.NONE);
		topTableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		topTable = new TableViewer(topTableComp, SWT.VIRTUAL | SWT.BORDER | SWT.NO_SCROLL   );
		topTable.getTable().setHeaderVisible(false);
		topTable.getTable().setLinesVisible(true);
		topTable.getTable().setLocation(0, 0);
		topTable.getTable().addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				topTable.getTable().deselectAll();
			}
			
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
			}
		});
		
		
		topTable.getTable().setBackground(Display.getDefault().getSystemColor(TABLE_HEADER_COLOR));
		
		for (int i = 0; i < results.getNumDataColumns(); i ++){
			TableViewerColumn tvc = new TableViewerColumn(topTable, SWT.NONE);
			tvc.getColumn().setWidth(DEFAULT_COL_SIZE);
		}
		
		topTable.setContentProvider(new IStructuredContentProvider() {
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
			
			@Override
			public void dispose() {
			}
			
			@Override
			public Object[] getElements(Object inputElement) {
				return results.getColumnHeaderValues();
			}
		});
		topTable.setLabelProvider(new SummaryHeaderLabelProvider());
		
		//add a listener so we can re-size the columns as we are not using the swt headers for resizing
		Listener thisListener = new Listener(){
			int moveIndex = -1;
			int lastX = 0;
			boolean mouseDown = false;
			
			@Override
			public void handleEvent(Event event) {
				
				if (event.type == SWT.MouseDown){
					//find column being re-sized
					int width = 0;
					for (int i = 0; i < topTable.getTable().getColumnCount(); i ++){
						width = width + topTable.getTable().getColumn(i).getWidth();
						if (width - 5 <= event.x && event.x <= width + 5){
							moveIndex = i;
						}
					}
					lastX = event.x;
					mouseDown = true;
					topTable.getTable().setCursor(ewCursor);
					topTable.getTable().setCapture(true);
					
				}else if (event.type == SWT.MouseUp){
					//reset
					mouseDown = false;
					moveIndex = -1;
					lastX = 0;
					topTable.getTable().setCapture(false);
					topTable.getTable().setCursor(null);
					
					//update table width
					int totalWidth = 0;
					for (int i = 0; i < mainTable.getTable().getColumnCount(); i ++){
						totalWidth += mainTable.getTable().getColumn(i).getWidth();
					}
					Rectangle top = topTable.getTable().getBounds();
					Rectangle r = mainTable.getTable().getBounds();
					
					int x = top.x;
					if (totalWidth < r.width){
						x = -mainTable.getTable().getHorizontalBar().getSelection();
					}
					topTable.getTable().setBounds(x,top.y,Math.max(totalWidth,r.width),top.height);
					//topTable.getTable().getParent().layout();
					topTable.getTable().layout();
					
					
				}else if (event.type == SWT.MouseMove){
					if (moveIndex < 0){
						topTable.getTable().setCursor(null);
						int width = 0;
						for (int i = 0; i < topTable.getTable().getColumnCount(); i ++){
							width = width + topTable.getTable().getColumn(i).getWidth();
							if (width - 5 <= event.x && event.x <= width + 5){
								//can move = true;
								topTable.getTable().setCursor(ewCursor);
							}
						}
					}else if (mouseDown && moveIndex >= 0){
						//resize colum in this table and the main table
						int prev = lastX - event.x;
						lastX = event.x;
						TableColumn col = topTable.getTable().getColumn(moveIndex);
						int newWidth = col.getWidth() - prev; 
						if (newWidth < 10){
							newWidth = 10;
						}
						col.setWidth(newWidth);
						mainTable.getTable().getColumn(moveIndex).setWidth(newWidth);
					}
				}

			}
		};
		
		topTable.getTable().addListener(SWT.MouseMove , thisListener);
		topTable.getTable().addListener(SWT.MouseDown, thisListener);
		topTable.getTable().addListener(SWT.MouseUp, thisListener);
	}
	

	private  class SummaryResultDataContentProvider implements IStructuredContentProvider{

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {

		}

		/**
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		@Override
		public Object[] getElements(Object inputElement) {
			return ((SummaryQueryResult)inputElement).getData();
		}
		
	}
	
	private class SummaryResultDataLabelProvider extends LabelProvider implements ITableLabelProvider{
		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object, int)
		 */
		@Override
		public String getColumnText(Object element, int columnIndex) {
			Double[] data = (Double[])element;
			Object value = data[columnIndex];
			if (value == null){
				return "";
			}else if (value instanceof Double){
				return  String.valueOf((Double)value);
			}else{
				return value.toString();
			}
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object, int)
		 */
		@Override
		public Image getColumnImage(Object element, int columnIndex) {
			return null;
		}
		
	}
	private class SummaryHeaderLabelProvider extends StyledCellLabelProvider{
		@Override
		public void update(ViewerCell cell){
			Object element = cell.getElement();
			if (element instanceof SummaryHeader[]){
				SummaryHeader header = ((SummaryHeader[])element)[cell.getColumnIndex()];
				cell.setText(header.getName());
			
				if (header.isValue()){
					cell.setBackground(Display.getDefault().getSystemColor(TABLE_HEADER_COLOR_2));
				}else{
					
				}
			}	
			super.update(cell);
		}	
	}
}




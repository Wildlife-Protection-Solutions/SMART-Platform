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

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
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
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.query.model.SummaryHeader;
import org.wcs.smart.query.model.SummaryQueryResult;
import org.wcs.smart.util.SmartUtils;

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
	}
	
	
	private void createComposite(FormToolkit toolkit){
		//setup layout
		GridLayout layout = new GridLayout(2, false);
		layout.marginWidth = layout.marginHeight = layout.horizontalSpacing = layout.verticalSpacing = 0;
		setLayout(layout);
		
		if (toolkit == null){
			lblSpacer = new Label(this, SWT.NONE);
		}else{
			lblSpacer = toolkit.createLabel(this, ""); //$NON-NLS-1$
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
			ColumnViewerToolTipSupport.enableFor(tvc.getViewer());
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
			ColumnViewerToolTipSupport.enableFor(tvc.getViewer());
			tvc.setLabelProvider(new SummaryHeaderLabelProvider(i));
		}
		if (results.getRowHeaders().size() == 0){
			leftTable.setLabelProvider(new CellLabelProvider() {
				@Override
				public void update(ViewerCell cell) {
					//leave it blank
				}
			});
		}
		
		//add a listener so we can re-size the columns as we are not using the swt headers for resizing
		TableResizeColumnListener leftListener = new TableResizeColumnListener(
				leftTable.getTable(),
				new Listener() {
					@Override
					public void handleEvent(Event event) {
						Point x = leftTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT);
						Rectangle left = leftTable.getTable().getBounds();
						leftTable.getTable().setBounds(left.x, left.y, x.x, left.height);
					}
				},
				null);
		
		leftTable.getTable().addListener(SWT.MouseMove, leftListener);
		leftTable.getTable().addListener(SWT.MouseUp, leftListener);
		leftTable.getTable().addListener(SWT.MouseDown, leftListener);
		
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
	}
	
	
	private void createTopTable(){
		final Composite topTableComp = new Composite(this, SWT.NONE);
		topTableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		topTable = new TableViewer(topTableComp, SWT.VIRTUAL | SWT.BORDER | SWT.NO_SCROLL | SWT.FULL_SELECTION   );
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
			ColumnViewerToolTipSupport.enableFor(tvc.getViewer());
			tvc.setLabelProvider(new SummaryHeaderLabelProvider(i));
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
		
		TableResizeColumnListener topListener = new TableResizeColumnListener(
				topTable.getTable(), new Listener() {
					@Override
					public void handleEvent(Event event) {
						// update table width
						int totalWidth = 0;
						for (int i = 0; i < mainTable.getTable()
								.getColumnCount(); i++) {
							totalWidth += mainTable.getTable().getColumn(i)
									.getWidth();
						}
						Rectangle top = topTable.getTable().getBounds();
						Rectangle r = mainTable.getTable().getBounds();

						int x = top.x;
						if (totalWidth < r.width) {
							x = -mainTable.getTable().getHorizontalBar()
									.getSelection();
						}
						topTable.getTable().setBounds(x, top.y,
								Math.max(totalWidth, r.width), top.height);
						// topTable.getTable().getParent().layout();
						topTable.getTable().layout();
					}
				}, new Listener() {

					@Override
					public void handleEvent(Event event) {
						int[] data = (int[]) event.data;
						mainTable.getTable().getColumn(data[0])
								.setWidth(data[1]);
					}
				});

		
		topTable.getTable().addListener(SWT.MouseMove , topListener);
		topTable.getTable().addListener(SWT.MouseDown, topListener);
		topTable.getTable().addListener(SWT.MouseUp, topListener);
	}
	

	private  class SummaryResultDataContentProvider implements IStructuredContentProvider{

		/**
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		@Override
		public void dispose() {
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
				return ""; //$NON-NLS-1$
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
		
		private int index;

		/**
		 * The data column index
		 * @param index
		 */
		public SummaryHeaderLabelProvider(int index) {
			super();
			this.index = index;
		}

		@Override
		public void update(ViewerCell cell) {
			Object element = cell.getElement();
			if (element instanceof SummaryHeader[]) {
				SummaryHeader[] array = (SummaryHeader[]) element;
				if (array.length > cell.getColumnIndex()) {
					SummaryHeader header = ((SummaryHeader[]) element)[cell
							.getColumnIndex()];
					cell.setText(header.getName());

					if (header.isValue()) {
						cell.setBackground(Display.getDefault().getSystemColor(
								TABLE_HEADER_COLOR_2));
					} else {

					}
				}
			}
			super.update(cell);
		}

		public Point getToolTipShift(Object object) {
			return new Point(5, 5);
		}

		public int getToolTipDisplayDelayTime(Object object) {
			return 2;
		}

		public int getToolTipTimeDisplayed(Object object) {
			return 5000;
		}

		@Override
		public String getToolTipText(Object element) {
			return SmartUtils.formatStringForLabel(((SummaryHeader[]) element)[index].getFullName());
		}
	}
}




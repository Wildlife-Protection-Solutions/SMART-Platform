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
package org.wcs.smart.query.common.ui;

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
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Slider;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.query.common.model.SummaryHeader;
import org.wcs.smart.query.common.model.SummaryQueryResult;
import org.wcs.smart.util.SmartUtils;

/**
 * Summary results table.
 * @author Emily
 *
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
	
	private Slider vSlider;
	private Slider hSlider;
	
	private int hSliderAbsoluteMax = 0;
	private Listener resizeListener;
	
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
		if (toolkit == null){
			toolkit = new FormToolkit(parent.getDisplay());
		}
		createComposite(toolkit);
		
		if (toolkit != null){
			toolkit.adapt(this);
		}
	}
	
	
	private void createComposite(FormToolkit toolkit){
		//setup layout
		GridLayout layout = new GridLayout(3, false);
		layout.marginWidth = layout.marginHeight = layout.horizontalSpacing = layout.verticalSpacing = 0;
		setLayout(layout);
		
		lblSpacer = toolkit.createLabel(this, ""); //$NON-NLS-1$
		lblSpacer.setVisible(false);
		lblSpacer.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		createTopTable();
		
		lblSpacer = toolkit.createLabel(this, ""); //$NON-NLS-1$
		lblSpacer.setVisible(false);
		lblSpacer.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
		
		createLeftTable();
		
		createMainTable();
		
		vSlider = new Slider(this, SWT.VERTICAL);
		vSlider.setMinimum(0);
		vSlider.setMaximum(100);
		vSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true));

		hSlider = new Slider(this, SWT.HORIZONTAL);
		hSlider.setMinimum(0);
		hSlider.setMaximum(100);
		hSlider.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		hSlider.addListener(SWT.Selection, new Listener() {
			@Override
			public void handleEvent(Event event) {
				updateHorizontalSlider();
			}
		});
		vSlider.addListener(SWT.Selection, new Listener() {
			
			@Override
			public void handleEvent(Event event) {
				updateVerticalSlider();
			}
		});
				
		// sync selection the same in both tables
		leftTable.getTable().addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				mainTable.getTable().setSelection(
						leftTable.getTable().getSelectionIndices());
				vSlider.setSelection(mainTable.getTable().getTopIndex());
			}
		});
		mainTable.getTable().addListener(SWT.Selection, new Listener() {
			public void handleEvent(Event event) {
				leftTable.getTable().setSelection(
						mainTable.getTable().getSelectionIndices());
				vSlider.setSelection(mainTable.getTable().getTopIndex());
			}
		});
		
		mainTable.setItemCount(results.getNumDataRows());
		topTable.setInput(results);
		leftTable.setInput(results);
		mainTable.setInput(results);
		
		//Table sizes		
		Point p = topTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		topTable.getTable().setBounds(0, 0, p.x, p.y);
		
		p = leftTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		leftTable.getTable().setBounds(0, 0, p.x, p.y + 50);
		
		p = mainTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT);
		mainTable.getTable().setBounds(0, 0, p.x, p.y+50);
		
		resizeListener = new Listener(){
			@Override
			public void handleEvent(Event event) {
				resize();
			}
		};
		super.addListener(SWT.Resize, resizeListener);
	}

	
	private void updateVerticalSlider(){
		leftTable.getTable().setTopIndex(vSlider.getSelection());
		mainTable.getTable().setTopIndex(vSlider.getSelection());
	}
	
	private void updateHorizontalSlider(){
		int position = hSlider.getSelection()  * 10;
		if (position > hSliderAbsoluteMax){
			position = hSliderAbsoluteMax;
		}
		
		Point p = topTable.getTable().getLocation();
		p.x = -position;
		topTable.getTable().setLocation(p);
		
		p = mainTable.getTable().getLocation();
		p.x = -position;
		mainTable.getTable().setLocation(p);
		
	}
	
	private void resize(){
		super.layout(true);		
		int displayHeight = mainTable.getTable().getParent().getBounds().height;
		int itemHeight = mainTable.getTable().getItemHeight();
		int numVisibleItems = (int) Math.ceil(displayHeight / (double)itemHeight);
		int numItems = results.getNumDataRows();
		if (numVisibleItems > numItems){
			vSlider.setEnabled(false);
			vSlider.setSelection(0);
		}else{
			vSlider.setEnabled(true);
			vSlider.setMaximum((numItems - numVisibleItems) +  vSlider.getThumb() + 1 );
		}
		
		int displayWidth = mainTable.getTable().getParent().getBounds().width;
		int width = mainTable.getTable().computeSize(SWT.DEFAULT, SWT.DEFAULT).x;
		
		if (displayWidth > width){
			hSlider.setEnabled(false);
			hSlider.setSelection(0);
		}else{
			hSlider.setEnabled(true);
			hSliderAbsoluteMax = (width - displayWidth);
			int maxValue = (int)Math.ceil((width - displayWidth) / 10.0) + hSlider.getThumb();
			hSlider.setMaximum(maxValue);
		}
		
		int tableWidth = Math.max(displayWidth, width);
		
		Rectangle rect = mainTable.getTable().getBounds();
		mainTable.getTable().setBounds(rect.x, rect.y, tableWidth, displayHeight);

		rect = leftTable.getTable().getBounds();
		leftTable.getTable().setBounds(rect.x, rect.y, rect.width, displayHeight);
		
		rect = topTable.getTable().getBounds();
		topTable.getTable().setBounds(rect.x, rect.y, tableWidth, rect.height);
		
		updateHorizontalSlider();
		updateVerticalSlider();
	}
	
	private void createMainTable(){
		Composite comp = new Composite(this, SWT.NONE);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
		gd.verticalIndent = 2;
		comp.setLayoutData(gd);
		
		
		mainTable = new TableViewer(comp, SWT.VIRTUAL | SWT.FULL_SELECTION | SWT.NO_SCROLL);
		mainTable.getTable().setLocation(0,0);
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
		Composite comp = new Composite(this, SWT.BORDER);
		
		GridData gd = new GridData(SWT.FILL, SWT.FILL, false, true, 1, 2);
		comp.setLayoutData(gd);
		comp.setBackground(comp.getDisplay().getSystemColor(TABLE_HEADER_COLOR));
		leftTable = new TableViewer(comp, SWT.VIRTUAL | SWT.SINGLE | SWT.FULL_SELECTION | SWT.NO_SCROLL );
		leftTable.getTable().setHeaderVisible(false);
		leftTable.getTable().setLinesVisible(true);
		leftTable.getTable().setLocation(0, 0);
		
		leftTable.getTable().setBackground(comp.getDisplay().getSystemColor(TABLE_HEADER_COLOR));
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
						resize();
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
		final Composite topTableComp = new Composite(this, SWT.BORDER);
		topTableComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		topTable = new TableViewer(topTableComp, SWT.VIRTUAL | SWT.NO_SCROLL | SWT.FULL_SELECTION   );
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
		
		topTable.getTable().setBackground(topTableComp.getDisplay().getSystemColor(TABLE_HEADER_COLOR));
		
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
							totalWidth += mainTable.getTable().getColumn(i).getWidth();
						}
						Rectangle top = topTable.getTable().getBounds();
						Rectangle r = mainTable.getTable().getBounds();

						topTable.getTable().setBounds(top.x, top.y, totalWidth, top.height);
						mainTable.getTable().setBounds(r.x, r.y, totalWidth, r.height);
						resize();
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
//			return new Double[61][15];
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
						cell.setBackground(mainTable.getControl().getDisplay().getSystemColor(TABLE_HEADER_COLOR_2));
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




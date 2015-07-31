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
package org.wcs.smart.query.ui.definition;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;


/**
 * Filter Panel for gridded query
 * 
 * @author Emily
 *
 */
public abstract class ValueRateFilterDeifnitionPanel implements IDefinitionPanel{

	public static final String ID ="org.wcs.smart.query.definition.RateValueFilter"; //$NON-NLS-1$
	public static final String PANEL_TITLE = Messages.ValueRateFilterDeifnitionPanel_PanelTitle;
	
	public enum PanelType{VALUE,RATE};
	
	protected BasicFilterDefintionPanel valueFilter = null;
	protected BasicFilterDefintionPanel rateFilter = null;
	
	@Inject protected IEclipseContext localContext;
	
	private QueryProxy currentQuery;
	private BasicFilterDefintionPanel currentTarget = null;
	
	private SashForm main ;
	private Composite right;
	private Composite left;
	private Font smallerFont = null;
	
	public ValueRateFilterDeifnitionPanel(){
	}
	
	public void dispose(){
		if (valueFilter != null){
			valueFilter.dispose();
		}
		if (rateFilter != null){
			rateFilter.dispose();
		}
		if (smallerFont != null){
			smallerFont.dispose();
		}
		main.dispose();
	}
	/**
	 * Creates the drop target composite
	 * @param parent
	 * @return
	 */
	@Override
	public Composite createComposite(Composite parent) {
		 
		main = new SashForm(parent, SWT.HORIZONTAL );

		/* left panel - value filter */
		left = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 2;
		gl.marginHeight = 2;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		left.setLayout(gl);
		left.addPaintListener(new PaintListener() {
			@Override
			public void paintControl(PaintEvent e) {
				if (currentTarget == valueFilter && right.isVisible()){
					e.gc.setLineWidth(2);
					e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
					e.gc.drawRectangle(1,1,left.getBounds().width - 6, left.getBounds().height - 6);
				}
			}
		});
		
		
		Composite leftInner = new Composite(left, SWT.NONE);
		gl = new GridLayout(2, false);
		leftInner.setLayout(gl);
		leftInner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblValueFilter = new Label(leftInner, SWT.NONE);
		lblValueFilter.setText(Messages.GriddedFilterPanel_ValueFilterLabel);
		lblValueFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		lblValueFilter.setToolTipText(Messages.GriddedFilterPanel_ValueFilterTooltip);
		lblValueFilter.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				setCurrent(valueFilter);
			}
			
		});
		
		Link lblClear = new Link(leftInner, SWT.NONE);
		lblClear.setText("<a>" + Messages.GriddedFilterPanel_ClearLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		FontData fd = lblClear.getFont().getFontData()[0];
		fd.setHeight(fd.getHeight() - 2);
		smallerFont = new Font(lblClear.getDisplay(), fd);
		lblClear.setFont(smallerFont);
		lblClear.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		lblClear.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				valueFilter.clear();
				fireQueryChangedListeners();
			}
		});
		
		Label lblSep2 = new Label(leftInner, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSep2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		/* right panel value filter */
		right = new Composite(main, SWT.BORDER);

		gl = new GridLayout(1, false);
		gl.marginWidth = 2;
		gl.marginHeight = 2;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		right.setLayout(gl);
		right.addPaintListener(new PaintListener() {			
			@Override
			public void paintControl(PaintEvent e) {
				if (currentTarget == rateFilter && right.isVisible()){
					e.gc.setLineWidth(2);
					e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_LIST_SELECTION));
					e.gc.drawRectangle(1,1,right.getBounds().width - 6, right.getBounds().height - 6);
				}
			}
		});

		Composite rightInner = new Composite(right, SWT.NONE);
		gl = new GridLayout(3, false);
		rightInner.setLayout(gl);
		rightInner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblRateFilter = new Label(rightInner, SWT.NONE);
		lblRateFilter.setText(Messages.GriddedFilterPanel_RateFilterLabel);
		lblRateFilter.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		lblRateFilter.setToolTipText(Messages.GriddedFilterPanel_RateFilterTooltip);
		lblRateFilter.addMouseListener(new MouseAdapter() {
			
			@Override
			public void mouseUp(MouseEvent e) {
				setCurrent(rateFilter);
			}
			
		});
		
		Link lblClear2 = new Link(rightInner, SWT.NONE);
		lblClear2.setText("<a>" + Messages.GriddedFilterPanel_ClearLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		lblClear2.setFont(smallerFont);
		lblClear2.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		lblClear2.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				rateFilter.clear();
				fireQueryChangedListeners();
			}
		});
		
		Link lblCopy = new Link(rightInner, SWT.NONE);
		lblCopy.setText("<a>" + Messages.GriddedFilterPanel_CopyLabel + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$ 
		lblCopy.setFont(smallerFont);
		lblCopy.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
		lblCopy.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				copyValueFilterToRateFilter();
				
			}
		});
		
		Label lblSep = new Label(rightInner, SWT.SEPARATOR | SWT.HORIZONTAL);
		lblSep.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));

		//filter panels
		valueFilter = createFilterPanel(PanelType.VALUE);	
		ContextInjectionFactory.inject(valueFilter, localContext);
		valueFilter.createComposite(left);
		
		rateFilter = createFilterPanel(PanelType.RATE);
		ContextInjectionFactory.inject(rateFilter, localContext);
		rateFilter.createComposite(right);
		
		valueFilter.addDropTargetPanel(rateFilter);
		rateFilter.addDropTargetPanel(valueFilter);
		
		setCurrent(valueFilter);
		
		updateFilterPanel(false);
		return main;
	}
	
	protected abstract BasicFilterDefintionPanel createFilterPanel( PanelType type );
	
	
	/**
	 * Re-draw the drop items
	 */
	public void redraw(){
		right.redraw();
		left.redraw();
	}
	
	public void updateFilterPanel(boolean needsRateFilter){	
		right.setData(needsRateFilter);
		right.setVisible(needsRateFilter);			
		if (!needsRateFilter && currentTarget == rateFilter){
			currentTarget = valueFilter;
		}
		main.layout();
		left.redraw();
		right.redraw();
		
	}
	protected abstract  void copyValueFilterToRateFilter();

	private void setCurrent(BasicFilterDefintionPanel target){
		this.currentTarget = target;
		left.redraw();
		right.redraw();
	}
	
	public void clear(){
		valueFilter.clear();
		rateFilter.clear();
		
	}
	
	public String getQueryPart(){
		StringBuilder queryText = new StringBuilder(valueFilter.getQueryPart());
		queryText.append("|"); //$NON-NLS-1$
		if ((Boolean)right.getData()){
			queryText.append(rateFilter.getQueryPart());
		}
		return queryText.toString();
	}
	
	public void addItem(DropItem item){
		if (currentTarget != null){
			currentTarget.addItem(item);
		}
	}
	public List<DropItem> getValueItems(){
		return valueFilter.getItems();
	}
	public List<DropItem> getRateItems(){
		return rateFilter.getItems();
	}

	@Override
	public void removeItem(DropItem item) {
		if (currentTarget != null){
			currentTarget.removeItem(item);
		}
	}

	@Override
	public String validate() {
		String error = valueFilter.validate();
		if (error == null){
			error = rateFilter.validate();
		}
		return error;
	}

	

	@Override
	public void saveItems(QueryProxy q) {
		valueFilter.saveItems(q);
		rateFilter.saveItems(q);
	}

	@Override
	public void initItems(QueryProxy q) throws Exception {
		this.currentQuery = q;
		valueFilter.initItems(q);
		rateFilter.initItems(q);
		
		if (q.getQuery() instanceof SummaryQuery){
			SummaryQuery query = (SummaryQuery)q.getQuery();
			if (query.getQueryDefinition() != null && query.getQueryDefinition().getValueFilter() != null){
				valueFilter.setFilterType(query.getQueryDefinition().getValueFilter().getFilterType());
			}else{
				valueFilter.setFilterType(FilterType.WAYPOINT);
			}
			if (query.getQueryDefinition() != null && query.getQueryDefinition().getRateFilter() != null){
				rateFilter.setFilterType(query.getQueryDefinition().getRateFilter().getFilterType());
			}else{
				rateFilter.setFilterType(FilterType.WAYPOINT);
			}
		}else if (q.getQuery() instanceof GriddedQuery){
			GriddedQuery query = (GriddedQuery)q.getQuery();
			if (query.getQueryDefinition() != null && query.getQueryDefinition().getValueFilter() != null){
				valueFilter.setFilterType(query.getQueryDefinition().getValueFilter().getFilterType());
			}else{
				valueFilter.setFilterType(FilterType.WAYPOINT);
			}
			if (query.getQueryDefinition() != null && query.getQueryDefinition().getRateFilter() != null){
				rateFilter.setFilterType(query.getQueryDefinition().getRateFilter().getFilterType());
			}else{
				rateFilter.setFilterType(FilterType.WAYPOINT);
			}
		}
		
	}


	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getGuiName() {
		return PANEL_TITLE;
	}

	@Override
	public void finishDrag(DropItem item) {
		if (currentTarget != null){
			currentTarget.finishDrag(item);
		}
	}

	@Override
	public void fireQueryChangedListeners() {
		QueryEventManager.getInstance().fireQueryDefinitionModified(currentQuery.getQuery());
	}

	@Override
	public Composite getDropTargetComposite() {
		return main;
	}


}

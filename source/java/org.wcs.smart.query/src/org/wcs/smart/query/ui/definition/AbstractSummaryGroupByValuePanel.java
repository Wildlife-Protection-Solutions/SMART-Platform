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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.IGroupByDropItem;
import org.wcs.smart.query.ui.model.IValueDropItem;

/**
 * Basic summary group by panel.  This consists of an row and column group by area as
 * well as a value list area.
 * 
 * @author Emily
 *
 */
public abstract class AbstractSummaryGroupByValuePanel implements IDefinitionPanel {

	/**
	 * Various drop item lists
	 * @author Emily
	 *
	 */
	public enum ListTargetType{ROW,COLUMN,VALUE};
	
	protected ListDefinitionPanel lstRowGroupBy;
	protected ListDefinitionPanel lstColumnGroupBy;
	protected ListDefinitionPanel lstValues;
	
	protected QueryProxy currentQuery;
	
	private Composite main;
	
	public AbstractSummaryGroupByValuePanel(){
	}
	
	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#getId()
	 */
	@Override
	public abstract String getId() ;

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#getGuiName()
	 */
	@Override
	public abstract String getGuiName();
	
	/**
	 * Creates a list drop target panel.  This will be
	 * one of a row, column or value list.
	 * 
	 * @param type list target type
	 * @return
	 */
	public abstract ListDefinitionPanel createListDropTargetPanel(ListTargetType type);

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#validate()
	 */
	@Override
	public abstract String validate();
	
	/**
	 * Adds the drop item to either the row group by list or the value list
	 * depending on the drop item type.
	 * 
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#addItem(org.wcs.smart.query.ui.model.DropItem)
	 */
	@Override
	public void addItem(DropItem item) {
		if (item instanceof IGroupByDropItem){
			lstRowGroupBy.addItem(item);
		}else if (item instanceof IValueDropItem){
			lstValues.addItem(item);
		}
	}

	/**
	 * Removes the drop item
	 * 
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#removeItem(org.wcs.smart.query.ui.model.DropItem)
	 */
	@Override
	public void removeItem(DropItem item) {
		lstRowGroupBy.removeItem(item);
		lstColumnGroupBy.removeItem(item);
		lstValues.removeItem(item);

	}

	/**
	 * Re-draw the drop items
	 */
	@Override
	public void redraw(){
		lstRowGroupBy.redraw();
		lstColumnGroupBy.redraw();
		lstValues.redraw();
	}
	
	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#createComposite(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public Composite createComposite(Composite parent) {
		main = new SashForm(parent, SWT.HORIZONTAL );
		
		Composite left = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		left.setLayout(gl);
		
		Composite right = new Composite(main, SWT.BORDER);
		gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		right.setLayout(gl);
		
		Composite rightInner = new Composite(right, SWT.NONE);
		gl = new GridLayout(2, false);
		rightInner.setLayout(gl);
		rightInner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		createValuesHeader(rightInner);

		Composite leftInner = new Composite(left, SWT.NONE);
		gl = new GridLayout(2, false);
		leftInner.setLayout(gl);
		Label lblGroupBys = new Label(leftInner, SWT.NONE);
		lblGroupBys.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.GROUPBY_ICON));
		
		lblGroupBys = new Label(leftInner, SWT.NONE);
		lblGroupBys.setText(Messages.SummaryValueGroupByPanel_GroupBySectionHeader);
		lblGroupBys.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblGroupBys.setToolTipText(Messages.SummaryValueGroupByPanel_GroupBySectionTooltip);
		createInnerGroupByComposite(left);
		
		lstValues = createListDropTargetPanel(ListTargetType.VALUE);
		Composite comp = lstValues.createComposite(right);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return main;
	}
	
	protected void createValuesHeader(Composite rightInner){
		Label lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_ICON));
		
		lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setText(Messages.SummaryValueGroupByPanel_ValuesSectionHeader);
		lblValues.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblValues.setToolTipText(Messages.SummaryValueGroupByPanel_ValuesSectionTooltip);
	}

	private void createInnerGroupByComposite(Composite parent){
		SashForm main = new SashForm(parent, SWT.HORIZONTAL );
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Composite left = new Composite(main, SWT.BORDER);
		GridLayout gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		left.setLayout(gl);
		
		Composite right = new Composite(main, SWT.BORDER);
		gl = new GridLayout(1, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		gl.verticalSpacing = 0;
		gl.horizontalSpacing = 0;
		right.setLayout(gl);
		
		
		Composite rightInner = new Composite(right, SWT.NONE);
		gl = new GridLayout(2, false);
		rightInner.setLayout(gl);
		
		Label lblImage = new Label(rightInner, SWT.NONE);
		lblImage.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.COLUMN_HEADER_ICON));
		
		Label lblColumnHeaders = new Label(rightInner, SWT.NONE);
		lblColumnHeaders.setText(Messages.SummaryValueGroupByPanel_ColumnHeadersHeader);
		lblColumnHeaders.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblColumnHeaders.setToolTipText(Messages.SummaryValueGroupByPanel_ColumnHeadersTooltip);
		
		Composite leftInner = new Composite(left, SWT.NONE);
		gl = new GridLayout(2, false);
		leftInner.setLayout(gl);
		
		lblImage = new Label(leftInner, SWT.NONE);
		lblImage.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.ROW_HEADER_ICON));
		
		Label lblRowHeaders = new Label(leftInner, SWT.NONE);
		lblRowHeaders.setText(Messages.SummaryValueGroupByPanel_RowHeadersHeader);
		lblRowHeaders.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblRowHeaders.setToolTipText(Messages.SummaryValueGroupByPanel_RowHeadersTooltip);
		
		lstRowGroupBy = createListDropTargetPanel(ListTargetType.ROW);
		Composite comp = lstRowGroupBy.createComposite(left);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstColumnGroupBy = createListDropTargetPanel(ListTargetType.COLUMN);
		comp = lstColumnGroupBy.createComposite(right);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstRowGroupBy.addTargetPanel(lstColumnGroupBy);
		lstColumnGroupBy.addTargetPanel(lstRowGroupBy);
		
	}
	
	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#dispose()
	 */
	@Override
	public void dispose() {
		lstRowGroupBy.dispose();
		lstColumnGroupBy.dispose();
		lstValues.dispose();
		
		main.dispose();

	}

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#getQueryPart()
	 */
	@Override
	public abstract String getQueryPart();

	/**
	 * Saves the items from the row, column and value panels
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#saveItems(org.wcs.smart.query.model.QueryProxy)
	 */
	@Override
	public void saveItems(QueryProxy q) {
		lstRowGroupBy.saveItems(q);
		lstColumnGroupBy.saveItems(q);
		lstValues.saveItems(q);
	}

	/**
	 * Inits the items in the row, column and value panels
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#initItems(org.wcs.smart.query.model.QueryProxy)
	 */
	@Override
	public void initItems(QueryProxy q) {
		this.currentQuery = q;
		lstRowGroupBy.initItems(q);
		lstColumnGroupBy.initItems(q);
		lstValues.initItems(q);
	}

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#clear()
	 */
	@Override
	public void clear() {
		lstRowGroupBy.clear();
		lstColumnGroupBy.clear();
		lstValues.clear();
	}

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#finishDrag(org.wcs.smart.query.ui.model.DropItem)
	 */
	@Override
	public void finishDrag(DropItem item) {
		lstRowGroupBy.finishDrag(item);
		lstColumnGroupBy.finishDrag(item);
		lstValues.finishDrag(item);
	}

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#fireQueryChangedListeners()
	 */
	@Override
	public void fireQueryChangedListeners() {
		QueryEventManager.getInstance().fireQueryDefinitionModified(currentQuery.getQuery());
	}

	/**
	 * @see org.wcs.smart.query.ui.model.IDefinitionPanel#getDropTargetComposite()
	 */
	@Override
	public Composite getDropTargetComposite() {
		return main;
	}

}

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

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.ui.formulaDnd.AbstractValueDropItem;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.IGroupByDropItem;
import org.wcs.smart.query.ui.formulaDnd.IValueDropItem;
import org.wcs.smart.query.ui.formulaDnd.ListDropTargetPanel;

/**
 * The value and group by panel for creating 
 * summary queries 
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryValueGroupByPanel {

	public static final String PANEL_TITLE = Messages.SummaryValueGroupByPanel_GroupByValuePanelTitle;
	
	private ListDropTargetPanel lstRowGroupBy;
	private ListDropTargetPanel lstColumnGroupBy;
	private ListDropTargetPanel lstValues;

	/**
	 * Creates the panel
	 * @param parent
	 * @param view
	 * @return
	 */
	public Composite createComposite(Composite parent, QueryDefView view){
		SashForm main = new SashForm(parent, SWT.HORIZONTAL );
		
		//Composite main = new Composite(parent, SWT.NONE);
		//main.setLayout(new GridLayout(2, false));
		
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
		
		Label lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_ICON));
		
		lblValues = new Label(rightInner, SWT.NONE);
		lblValues.setText(Messages.SummaryValueGroupByPanel_ValuesSectionHeader);
		lblValues.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblValues.setToolTipText(Messages.SummaryValueGroupByPanel_ValuesSectionTooltip);
		//Label lblSeparator = new Label(right, SWT.HORIZONTAL | SWT.SEPARATOR);
		//lblSeparator.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
		
		Composite leftInner = new Composite(left, SWT.NONE);
		gl = new GridLayout(2, false);
		leftInner.setLayout(gl);
		Label lblGroupBys = new Label(leftInner, SWT.NONE);
		lblGroupBys.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.GROUPBY_ICON));
		
		lblGroupBys = new Label(leftInner, SWT.NONE);
		lblGroupBys.setText(Messages.SummaryValueGroupByPanel_GroupBySectionHeader);
		lblGroupBys.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		lblGroupBys.setToolTipText(Messages.SummaryValueGroupByPanel_GroupBySectionTooltip);
		createInnerGroupByComposite(left, view);
		
		lstValues = new ListDropTargetPanel(view, false);
		Composite comp = lstValues.createComposite(right);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		return main;
	}
	
	/**
	 * 
	 * @return true if one of the values has an encounter rate
	 */
	public boolean hasRate(){
		for (DropItem it : lstValues.getItems()){
			if (it instanceof AbstractValueDropItem && ((AbstractValueDropItem)it).hasEncounterRatio()){
				return true;
			}
		}
		return false;
	}
	private void createInnerGroupByComposite(Composite parent, QueryDefView view){
		SashForm main = new SashForm(parent, SWT.HORIZONTAL );
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		//Composite main = new Composite(parent, SWT.NONE);
		//main.setLayout(new GridLayout(2, false));
		
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
		
		lstRowGroupBy = new ListDropTargetPanel(view, true);
		Composite comp = lstRowGroupBy.createComposite(left);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstColumnGroupBy = new ListDropTargetPanel(view, true);
		comp = lstColumnGroupBy.createComposite(right);
		comp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		lstRowGroupBy.addTargetPanel(lstColumnGroupBy);
		lstColumnGroupBy.addTargetPanel(lstRowGroupBy);
		
	}
	
	/**
	 * @return the query string
	 */
	public String getQueryString(){
		return  lstValues.getQueryString() + "|" + lstRowGroupBy.getQueryString() + "|" + lstColumnGroupBy.getQueryString() ; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	/**
	 * @param query initializes the panel based on the query values
	 */
	public void init(SummaryQuery query){
		lstValues.addElements(query.getValueDropItems());
		lstRowGroupBy.addElements(query.getRowGroupByDropItems());
		lstColumnGroupBy.addElements(query.getColumnGroupByDropItems());
		lstValues.validate();
	}
	
	/**
	 * Clears the components
	 */
	public void clear(){
		lstRowGroupBy.clear();
		lstColumnGroupBy.clear();
		lstValues.clear();
	}
	
	/**
	 * Saves current drop items to the query (for re-use)
	 * @param query
	 */
	public void saveDropItems(SummaryQuery query){
		ArrayList<DropItem> items = new ArrayList<DropItem>();
		items.addAll(lstRowGroupBy.getItems());
		query.setRowGroupByDropItems(items);
		
		items = new ArrayList<DropItem>();
		items.addAll(lstColumnGroupBy.getItems());
		query.setColumnGroupByDropItems(items);
		
		items = new ArrayList<DropItem>();
		items.addAll(lstValues.getItems());
		query.setValueDropItems(items);
	}
	
	/**
	 * Adds a drop item.
	 * @param item
	 */
	public void addItem(DropItem item){
		if (item instanceof IValueDropItem){
			lstValues.addElement(item);
		}else if (item instanceof IGroupByDropItem){
			lstRowGroupBy.addElement(item);
		}
	}
	
	
}

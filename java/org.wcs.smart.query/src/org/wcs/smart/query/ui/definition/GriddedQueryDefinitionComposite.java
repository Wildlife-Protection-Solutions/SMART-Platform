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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.GridQueryDefinition;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryDropType;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.FilterDropTargetPanel;


/**
 * Summary query definition area
 * @author egouge
 * @since 1.0.0
 */
public class GriddedQueryDefinitionComposite extends QueryDefinitionComposite {

	
	private SourceProvider provider;
	private QueryDefView parentView;
	private GriddedValuePanel panel;
	private FilterDropTargetPanel filterPanel;
	private TabFolder tabs;

	/**
	 * @param parent parent composite
	 * @param parentView parent view
	 */
	public GriddedQueryDefinitionComposite(Composite parent, 
			QueryDefView parentView) {
		super(parent, SWT.NONE);
		this.parentView = parentView;

		ISourceProviderService service = (ISourceProviderService)parentView.getSite().getService(ISourceProviderService.class);
		this.provider = (SourceProvider) service.getSourceProvider(SourceProvider.SELECTED_FILTERS);
		
		createComposite();
	}

	/**
	 * Creates the composite
	 */
	private void createComposite(){
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginHeight = 0;
		gl.marginWidth = 0;
		setLayout(gl);
		
		tabs = new TabFolder(this, SWT.TOP);
		tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		TabItem item1 = new TabItem(tabs, SWT.NONE);
		panel = new GriddedValuePanel();
		Composite pnl = panel.createComposite(tabs,parentView);
		item1.setControl(pnl);
		item1.setText("Grid and Value Definitions");	
		pnl.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));
		
		TabItem item2 = new TabItem(tabs, SWT.NONE);
		filterPanel = new FilterDropTargetPanel(parentView);
		item2.setControl( filterPanel.createComposite(tabs) );
		item2.setText("Data Filter");
	
		tabs.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISourceProviderService service = (ISourceProviderService)parentView.getSite().getService(ISourceProviderService.class);
				SourceProvider provider = (SourceProvider) service.getSourceProvider(SourceProvider.QUERY_DROP_TYPE);
				if (tabs.getSelectionIndex() == 0){
					provider.setQueryDefinitionType(QueryDropType.GRIDDED_ITEM);
				}else{
					provider.setQueryDefinitionType(QueryDropType.FILTER_ITEM);
				}
			}
		});
	}
	
	/**
	 * @see org.wcs.smart.query.ui.IQueryDefinitionComposite#clear()
	 */
	@Override
	public void clear() {
		panel.clear();
		filterPanel.clear();
	}

	/**
	 * @see org.wcs.smart.query.ui.IQueryDefinitionComposite#validate()
	 */
	@Override
	public String validate() {
		String query = panel.getQueryString() + "|" + panel.getGridSize() + "|" + filterPanel.getQueryString() ;
		boolean isvalid = true;
		GridQueryDefinition def = null;
		String error = null;
		if (query.length() == 0) {
			isvalid = false;
		} else {
			try {
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				def = parser.GridQuery();
				is.close();
			} catch (Throwable ex) {
				// failed to parse query
				isvalid = false;
				error = ex.getMessage();
			}
		}
		
		if (!isvalid || def.getValuePart() == null){
			isvalid = false;
			error = "Exactly one value must be selected.";
		}
		if (isvalid){
			if (panel.getGridSize() <= 0){
				isvalid = false;
				error = "Grid size must be greater than 0";
			}
			
//			String temp = GriddedQuery.validate();
//			if (temp != null){
//				isvalid = false;
//				error = temp;
//			}
		}
		
		provider.setQueryValue(isvalid, error);
		if (parentView.getQuery() != null){
			parentView.getQuery().setIsValid(isvalid);
			((GriddedQuery)parentView.getQuery()).setQuery(query, def);
		}
		return error;
	}

	/**
	 * @see org.wcs.smart.query.ui.QueryDefinitionComposite#init()
	 */
	@Override
	public void init() {
		GriddedQuery query = ((GriddedQuery)parentView.getQuery());
		filterPanel.addElements(query.getFilterDropItems());
		panel.init(query);
	}

	/**
	 * @see org.wcs.smart.query.ui.QueryDefinitionComposite#saveItems()
	 */
	@Override
	public void saveItems() {
		GriddedQuery query = ((GriddedQuery)parentView.getQuery());
		
		panel.saveDropItems(query);
		
		List<DropItem> items = new ArrayList<DropItem>();
		items.addAll(filterPanel.getItems());
		query.setFilterDropItems(items);
	}

	/**
	 * @see org.wcs.smart.query.ui.definition.QueryDefinitionComposite#addItem(org.wcs.smart.query.ui.formulaDnd.DropItem)
	 */
	@Override
	public void addItem(DropItem item) {
		if (item.isGroupByItem() || item.isValueItem()){
			panel.addItem(item);
		}else if (item.isFilterItem()){
			filterPanel.addElement(item);
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.definition.QueryDefinitionComposite#visible()
	 */
	public void visible(){
		tabs.setSelection(0);
		ISourceProviderService service = (ISourceProviderService)parentView.getSite().getService(ISourceProviderService.class);
		SourceProvider provider = (SourceProvider) service.getSourceProvider(SourceProvider.QUERY_DROP_TYPE);
		provider.setQueryDefinitionType(QueryDropType.GRIDDED_ITEM);
	}
	
}

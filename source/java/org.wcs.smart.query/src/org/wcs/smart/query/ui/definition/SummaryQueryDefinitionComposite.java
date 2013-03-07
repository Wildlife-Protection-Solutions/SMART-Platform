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
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.SummaryQuery;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.parser.internal.summary.SumQueryDefinition;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryPartPanelType;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.FilterDropTargetPanel;


/**
 * Summary query definition area
 * @author egouge
 * @since 1.0.0
 */
public class SummaryQueryDefinitionComposite extends QueryDefinitionComposite {

	
	private SourceProvider provider;
	private QueryDefView parentView;
	private SummaryValueGroupByPanel panel;
	private FilterDropTargetPanel filterPanel;
	private TabFolder tabs;
	private ConservationAreaFilterPanel caFilterPanel;

	/**
	 * @param parent parent composite
	 * @param parentView parent view
	 */
	public SummaryQueryDefinitionComposite(Composite parent, 
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
		panel = new SummaryValueGroupByPanel();
		Composite pnl = panel.createComposite(tabs,parentView);
		item1.setControl(pnl);
		item1.setText(SummaryValueGroupByPanel.PANEL_TITLE);	
		pnl.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));
		
		TabItem item2 = new TabItem(tabs, SWT.NONE);
		filterPanel = new FilterDropTargetPanel(parentView);
		item2.setControl( filterPanel.createComposite(tabs) );
		item2.setText(FilterDropTargetPanel.PANEL_TITLE);
		
		tabs.addSelectionListener(new SelectionAdapter() {			
			@Override
			public void widgetSelected(SelectionEvent e) {
				ISourceProviderService service = (ISourceProviderService)parentView.getSite().getService(ISourceProviderService.class);
				SourceProvider provider = (SourceProvider) service.getSourceProvider(SourceProvider.QUERY_DROP_TYPE);
				if (tabs.getSelectionIndex() == 0){
					provider.setQueryDefinitionType(QueryPartPanelType.SUMMARY_ITEM);
				}else{
					provider.setQueryDefinitionType(QueryPartPanelType.FILTER_ITEM);
				}
			}
		});
		
		if (SmartDB.isMultipleAnalysis()){
			TabItem item3 = new TabItem(tabs, SWT.NONE);
			caFilterPanel = new ConservationAreaFilterPanel(tabs);
			item3.setControl(caFilterPanel);
			item3.setText(ConservationAreaFilterPanel.PANEL_TITLE);	
			caFilterPanel.setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, false));
			caFilterPanel.setSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					validate();
				}
			});
		}
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
		String query = panel.getQueryString() + "|" + filterPanel.getQueryString(); //$NON-NLS-1$
		SumQueryDefinition def = null;
		String error = null;
		if (query.length() == 0) {
			error = Messages.SummaryQueryDefinitionComposite_Error_EmptyQuery;
		} else {
			try {
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				def = parser.SumQuery();
				is.close();
			} catch (Throwable ex) {
				// failed to parse query
				error = ex.getLocalizedMessage();
			}
		}
		
		if (error == null && def.getValuePart().getValueItems().size() == 0){
			error = Messages.SummaryQueryDefinitionComposite_NoValueError;
		}
		if (error == null){
			String temp = SummaryQuery.validateQueryParts(def);
			if (temp != null){
				error = temp;
			}
		}
		if (error == null & caFilterPanel != null){
			error = caFilterPanel.isValid();
		}
		provider.setQueryValue(error == null, error);
		if (parentView.getQuery() != null){
			parentView.getQuery().setIsValid(error == null);
			((SummaryQuery)parentView.getQuery()).setQuery(query, def);
			if (caFilterPanel != null){
				ConservationAreaFilter filter = caFilterPanel.getCaFilter();
				if (filter != null){
					parentView.getQuery().setConservationAreaFilter(filter);
				}
			}
		}
		return error;
	}

	/**
	 * @see org.wcs.smart.query.ui.QueryDefinitionComposite#init()
	 */
	@Override
	public void init() {
		SummaryQuery query = ((SummaryQuery)parentView.getQuery());
		filterPanel.addElements(query.getFilterDropItems());
		panel.init(query);
		if (caFilterPanel != null){
			caFilterPanel.initQuery(query);
		}
	}

	/**
	 * @see org.wcs.smart.query.ui.QueryDefinitionComposite#saveItems()
	 */
	@Override
	public void saveItems() {
		SummaryQuery query = ((SummaryQuery)parentView.getQuery());
		
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
		provider.setQueryDefinitionType(QueryPartPanelType.SUMMARY_ITEM);
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	@Override
	public void dispose(){
		filterPanel.dispose();
	}

}

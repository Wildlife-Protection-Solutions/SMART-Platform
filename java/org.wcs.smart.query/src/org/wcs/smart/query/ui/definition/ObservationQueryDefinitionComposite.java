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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.parser.filter.ConservationAreaFilter;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryPartPanelType;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.FilterDropTargetPanel;
import org.wcs.smart.query.ui.formulaDnd.IFilterDropItem;

/**
 * Observation query definition panel
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ObservationQueryDefinitionComposite extends QueryDefinitionComposite {

	
	private FilterDropTargetPanel dropTarget;
	private ConservationAreaFilterPanel caFilter;
	private TabItem caTabItem;
	
	private QueryDefView view;
	private boolean isInitializing = false;

	
	/**
	 * 
	 */
	public ObservationQueryDefinitionComposite(Composite parent, QueryDefView view) {
		super(parent, SWT.NONE);
		this.view = view;
		createComposite();
	}
	
	private void createComposite(){
		
		GridLayout layout = new GridLayout(1, false);
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		
		setLayout(layout);
		setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));


		
		dropTarget = new FilterDropTargetPanel(view);
		
		if (SmartDB.isMultipleAnalysis()){
			//need tabs with both a ca filter and a query filter
			TabFolder tabs = new TabFolder(this, SWT.TOP);
			tabs.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			TabItem item2 = new TabItem(tabs, SWT.NONE);
			Composite control = dropTarget.createComposite(tabs);
			item2.setControl(control);
			item2.setText(FilterDropTargetPanel.PANEL_TITLE);	
			dropTarget.getComposite().setLayoutData(new GridData(SWT.FILL,SWT.FILL, true, true));
			
			caTabItem = new TabItem(tabs, SWT.NONE);
			caFilter = new ConservationAreaFilterPanel(tabs);
			caTabItem.setControl(caFilter);
			caTabItem.setText(ConservationAreaFilterPanel.PANEL_TITLE);

			caFilter.setSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					validate();
				}
			});
		}else{
			// create drop area
			dropTarget.createComposite(this);
		}
	}
	
	/**
	 * @see org.wcs.smart.query.ui.definition.QueryDefinitionComposite#addItem(org.wcs.smart.query.ui.formulaDnd.DropItem)
	 */
	public void addItem(DropItem item){
		if (item instanceof IFilterDropItem){
			dropTarget.addElement(item);
		}
	}
	


	/**
	 * @see org.wcs.smart.query.ui.QueryDefinitionComposite#clear()
	 */
	@Override
	public void clear() {
		dropTarget.clear();
	}
	
	/**
	 * @see org.eclipse.swt.widgets.Widget#dispose()
	 */
	public void dispose(){
		super.dispose();
		if (dropTarget != null){
			dropTarget.dispose();
		}
	}
	
	
	/**
	 * @see org.wcs.smart.query.ui.definition.QueryDefinitionComposite#validate()
	 */
	public String validate(){
		if (isInitializing) return null; //still initializing ; do not validate
		
		String error =null;
		String query = dropTarget.getQueryString().trim();
		if (query.length() != 0) {
			try {
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				parser.QueryFilter();
				is.close();
			} catch (Throwable ex) {
				// failed to parse query
				error = ex.getLocalizedMessage();
			}
		}
		
		if (error == null && caFilter != null){
			error = caFilter.isValid();
		}

		SourceProvider provider = (SourceProvider) ((ISourceProviderService)view.getSite().getService(ISourceProviderService.class)).getSourceProvider(SourceProvider.QUERY_VALID);
		provider.setQueryValue(error == null, error);
		if(view.getQuery() != null){
			view.getQuery().setIsValid(error == null);
			if (error == null){
				((SimpleQuery)view.getQuery()).setQueryFilter(query);
			}
			if (caFilter != null){
				ConservationAreaFilter newFilter = caFilter.getCaFilter();
				if (newFilter != null){
					view.getQuery().setConservationAreaFilter(newFilter);
				}
				
				if (newFilter == null || (newFilter.getMissingCas() != null && newFilter.getMissingCas().size() > 0)){
					caTabItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.WARN_ICON));
					caTabItem.setToolTipText(Messages.ObservationQueryDefinitionComposite_FilterWarningTooltip);
				}else{
					caTabItem.setImage(null);
					caTabItem.setToolTipText(Messages.ObservationQueryDefinitionComposite_CaTooltip);
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
		SimpleQuery query = (SimpleQuery)view.getQuery();
		isInitializing = true;
		dropTarget.addElements(query.getDropItems());
		dropTarget.setFilterType(query.getFilterType());
		if (caFilter != null){
			caFilter.initQuery(view.getQuery());
		}
		
		isInitializing = false;
	}

	/**
	 * @see org.wcs.smart.query.ui.QueryDefinitionComposite#saveItems()
	 */
	@Override
	public void saveItems() {
		ArrayList<DropItem> items = new ArrayList<DropItem>();
		items.addAll(dropTarget.getItems());
		((SimpleQuery)view.getQuery()).setDropItems(items);
		
	}
	
	/**
	 * 
	 * @see org.wcs.smart.query.ui.definition.QueryDefinitionComposite#visible()
	 */
	public void visible(){
		ISourceProviderService service = (ISourceProviderService)view.getSite().getService(ISourceProviderService.class);
		SourceProvider provider = (SourceProvider) service.getSourceProvider(SourceProvider.QUERY_DROP_TYPE);
		provider.setQueryDefinitionType(QueryPartPanelType.FILTER_ITEM);
	}

}

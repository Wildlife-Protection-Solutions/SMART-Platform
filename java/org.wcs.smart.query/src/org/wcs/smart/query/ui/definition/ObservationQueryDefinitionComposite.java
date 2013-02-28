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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.services.ISourceProviderService;
import org.wcs.smart.query.model.SimpleQuery;
import org.wcs.smart.query.parser.internal.parser.Parser;
import org.wcs.smart.query.ui.SourceProvider;
import org.wcs.smart.query.ui.SourceProvider.QueryPartPanelType;
import org.wcs.smart.query.ui.formulaDnd.DropItem;
import org.wcs.smart.query.ui.formulaDnd.FilterDropTargetPanel;

/**
 * Observation query definition panel
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ObservationQueryDefinitionComposite extends QueryDefinitionComposite {

	
	private FilterDropTargetPanel dropTarget;
	
	private QueryDefView view;
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

		// create drop area
		createDragAndDropArea(this);
	}
	
	/**
	 * @see org.wcs.smart.query.ui.definition.QueryDefinitionComposite#addItem(org.wcs.smart.query.ui.formulaDnd.DropItem)
	 */
	public void addItem(DropItem item){
		if (item.isFilterItem()){
			dropTarget.addElement(item);
		}
	}
	
	/**
	 * Creates the drag and drop are panel
	 * @param parent
	 * 
	 * @return
	 */
	private void createDragAndDropArea(Composite parent){		
		dropTarget = new FilterDropTargetPanel(view);
		dropTarget.createComposite(parent);
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
		String error =null;
		String query = dropTarget.getQueryString().trim();
		boolean isvalid = true;
		if (query.length() == 0) {
			isvalid = true;
		} else {
			try {
				InputStream is = new ByteArrayInputStream(query.getBytes());
				Parser parser = new Parser(is);
				parser.QueryFilter();
				is.close();
			} catch (Throwable ex) {
				// failed to parse query
				isvalid = false;
				error = ex.getLocalizedMessage();
			}
		}
		SourceProvider provider = (SourceProvider) ((ISourceProviderService)view.getSite().getService(ISourceProviderService.class)).getSourceProvider(SourceProvider.QUERY_VALID);
		provider.setQueryValue(isvalid, error);
		if(view.getQuery() != null){
			view.getQuery().setIsValid(isvalid);
			((SimpleQuery)view.getQuery()).setQueryFilter(query);
		}
		return error;
	}

	/**
	 * @see org.wcs.smart.query.ui.QueryDefinitionComposite#init()
	 */
	@Override
	public void init() {
		dropTarget.addElements(((SimpleQuery)view.getQuery()).getDropItems());
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

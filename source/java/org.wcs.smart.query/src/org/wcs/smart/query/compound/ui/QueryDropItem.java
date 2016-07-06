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
package org.wcs.smart.query.compound.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.e4.ui.model.application.ui.menu.impl.ToolBarImpl;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.CompoundMapQueryLayer;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.util.UuidUtils;

/**
 * Query drop item that includes a date filter.
 * 
 * @author Emily
 *
 */
public class QueryDropItem extends DropItem {

	private UUID query;
	private IQueryType queryType;
	private String label;
	
	private QueryDateFilterComposite dateFilter;
	private CompoundMapQueryLayer mapLayer;
	
	
	public QueryDropItem(Query query){
		this.query = query.getUuid();
		this.queryType = QueryTypeManager.INSTANCE.findQueryType(query.getTypeKey());
		this.label = query.getName() + " [" + query.getId() + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public QueryDropItem(UUID query, IQueryType queryType, String label){
		this.query = query;
		this.queryType = queryType;
		this.label = label;
	}
	
	/**
	 * validates the date filter
	 */
	public void validate(){
		dateFilter.validate();
	}
	
	/**
	 * The query type
	 * @return
	 */
	public IQueryType getQueryType(){
		return this.queryType;
	}
	
	/**
	 * the query name label
	 * @return
	 */
	public String getLabel(){
		return this.label;
	}
	
	/**
	 * the query uuid
	 * @return
	 */
	public UUID getQueryUuid(){
		return this.query;
	}
	
	/**
	 * the date filter
	 * @return
	 */
	public DateFilter getDateFilter(){
		return dateFilter.getDateFilter();
	}
	
	public void setDateFilter(DateFilter df){
		this.dateFilter.setDateFilter(df);
	}
	
	@Override
	public String getText() {
		StringBuilder sb = new StringBuilder();
		sb.append(queryType.getKey());
		sb.append(","); //$NON-NLS-1$
		sb.append(UuidUtils.uuidToString(query));
		sb.append(","); //$NON-NLS-1$
		sb.append(mapLayer.getDateFilter());
		return sb.toString();
	}

	@Override
	public String asQueryPart() {
		return getText();
	}

	public CompoundMapQueryLayer getLayer(){
		return this.mapLayer;
	}
	
	/**
	 * Accepts a compound map query layer
	 * 
	 */
	@Override
	public void initializeData(Object data) {
		if (data instanceof CompoundMapQueryLayer){
			this.mapLayer = (CompoundMapQueryLayer) data;
		}
	}

	@Override
	protected void createComposite(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(3, false);
		gl.marginWidth = gl.marginHeight = 0;
		main.setLayout(gl);
		
		Label lblName = new Label(main, SWT.NONE);
		lblName.setText(label);
		
		dateFilter = new QueryDateFilterComposite(main, queryType.getDateFilterOptions(),
				IDateFilter.DATE_FILTERS, false);
		dateFilter.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (mapLayer != null && mapLayer.getDateFilter() != null){
			dateFilter.setDateFilter(mapLayer.getDateFilterAsFilter());
		}
		dateFilter.addChangeListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				queryChanged();
				getWidget().pack(true);
				getTargetPanel().getDropTargetComposite().pack(true);
			}
		});
	
		ToolBar temp = new ToolBar(main, SWT.FLAT);
		
		ToolItem applyAll = new ToolItem(temp, SWT.PUSH  | SWT.FLAT );
		applyAll.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.DATE_APPLY_ALL));
		applyAll.setToolTipText("apply date filter to all layers");
		applyAll.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				for (DropItem di : ((CompoundDefinitionPanel)getTargetPanel()).getItems()){
					if (di instanceof QueryDropItem){
						((QueryDropItem) di).setDateFilter(getDateFilter());
					}
				}
				queryChanged();
			}
		});
		
		ToolItem clearStyle = new ToolItem(temp, SWT.PUSH  | SWT.FLAT );
		clearStyle.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.CLEAR_STYLE));
		clearStyle.setToolTipText("clear style");
		clearStyle.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				mapLayer.setQueryStyle(null);
				queryChanged();
			}
		});
		
		List<Control> kids = new ArrayList<Control>();
		kids.add(dateFilter);
		while(!kids.isEmpty()){
			Control kid = kids.remove(0);
			initDrag(kid);
			if (kid instanceof Composite){
				for (Control c : ((Composite)kid).getChildren()){
					kids.add(c);
				}
			}
		}
		initDrag(lblName);
		initDrag(main);
	}
}

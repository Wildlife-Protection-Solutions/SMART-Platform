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
package org.wcs.smart.r.ui.editor.script;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.QueryTypeManager;
import org.wcs.smart.query.common.model.CompoundMapQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.importexport.QueryExportEngine;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.IDateFilter;
import org.wcs.smart.query.ui.QueryDateFilterComposite;
import org.wcs.smart.query.ui.importexport.QueryExportLabelProvider;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.engine.QueryConfiguration;
import org.wcs.smart.r.internal.Messages;

/**
 * Composite for listing queries and collection date and output format 
 * 
 * @author Emily
 *
 */
public class QueryListComposite extends Composite{

//	private List<Query> queries;
	private List<QueryItem> uiElements;
	
	private Composite list;
	private FormToolkit toolkit;
	private ScrolledForm form;
	
	public QueryListComposite(Composite parent) {
		super(parent, SWT.NONE);
		
		uiElements = new ArrayList<>();
		
		toolkit = new FormToolkit(Display.getDefault());
		addListener(SWT.Dispose, e->toolkit.dispose());
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		form = toolkit.createScrolledForm(this);
		form.getBody().setLayout(new GridLayout());
		((GridLayout)form.getBody().getLayout()).marginWidth = 0;
		((GridLayout)form.getBody().getLayout()).marginHeight = 0;
		
		list = toolkit.createComposite(form.getBody(), SWT.NONE);
		list.setLayout(new GridLayout(5, false));
		form.setContent(list);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)list.getLayout()).marginWidth = 0;
		((GridLayout)list.getLayout()).marginHeight = 0;
		
		updateList();
	}
	
	protected boolean canAdd(Query query) {
		return !query.getTypeKey().equalsIgnoreCase(CompoundMapQuery.TYPE_KEY);
	}
	
	/**
	 * Adds the given query does not fire any listeners
	 * @param query
	 * @param dFilter
	 * @param exportId
	 */
	public void addQuery(Query query, DateFilter dFilter, String exportId) {
		if (!canAdd(query)) return;
		
		QueryItem qi = new QueryItem(query, null, null);
		qi.lastDate = dFilter;
		qi.lastFormat = exportId;
		uiElements.add(qi);
	}
	
	/**
	 * Adds the queries, fires change listeners once added
	 * @param queries
	 */
	public void addQueries(List<Query> queries) {
		for (Query q : queries) {
			if (!canAdd(q)) continue;
			QueryItem qi = new QueryItem(q, null, null);
			uiElements.add(qi);
		}
		updateList();
		fireListeners();
	}

	private void removeQuery(QueryItem query) {
		uiElements.remove(query);
		updateList();
		fireListeners();
	}
	
	private void fireListeners() {
		Event modified = new Event();
		for(Listener l : getListeners(SWT.Selection)) {
			l.handleEvent(modified);
		}
	}
	
	
	private void adapt(Control c) {
		toolkit.adapt(c,false, false);
		if (c instanceof Composite) {
			for (Control kid : ((Composite) c).getChildren()) {
				adapt(kid);
			}
		}
	}
	
	void updateList() {
		for (Control c : list.getChildren()) c.dispose();
		
		Label l = toolkit.createLabel(list, Messages.QueryListComposite_NameLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		toolkit.createLabel(list, Messages.QueryListComposite_DataFilterLabel);
		toolkit.createLabel(list, Messages.QueryListComposite_ExportLabel);
		toolkit.createLabel(list, ""); //$NON-NLS-1$
		
		l = toolkit.createLabel(list, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		
		for (QueryItem q : uiElements) {
			IQueryType qType = QueryTypeManager.INSTANCE.findQueryType(q.query.getTypeKey());
			
			//query icon
			l = toolkit.createLabel(list, ""); //$NON-NLS-1$
			l.setImage(qType.getImage());
			
			//query name
			toolkit.createLabel(list, q.query.getName());
			
			//date filter
			QueryDateFilterComposite dateComposite = null;
			if (qType.getDateFilterOptions().length > 0) {
				dateComposite = new QueryDateFilterComposite(list, qType.getDateFilterOptions(), IDateFilter.DATE_FILTERS, false);
				dateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
				adapt(dateComposite);
				dateComposite.addChangeListener(evt->{
					list.layout(true);
					form.reflow(true);
				});
				if (q.lastDate != null) {
					dateComposite.setDateFilter((DateFilter)q.lastDate);
				}
				dateComposite.addChangeListener(new ISelectionChangedListener() {
					@Override
					public void selectionChanged(SelectionChangedEvent event) {
						fireListeners();
					}
				});
			}else {
				toolkit.createLabel(list, ""); //$NON-NLS-1$
			}
			
			//format
			ComboViewer cmbFormat = new ComboViewer(list, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbFormat.setLabelProvider(new QueryExportLabelProvider());
			cmbFormat.setContentProvider(ArrayContentProvider.getInstance());
			List<IQueryExporter> exports = QueryExportEngine.getQueryExports(q.query);
			Collections.sort(exports, new Comparator<IQueryExporter>() {
				@Override
				public int compare(IQueryExporter o1, IQueryExporter o2) {
					return Collator.getInstance().compare(o1.getName(), o2.getName());
				}
			});
			cmbFormat.setInput(exports);
			
			if (q.lastFormat != null) {
				String id = (String) q.lastFormat;
				IQueryExporter selection = exports.get(0);
				for (IQueryExporter i : exports) {
					if (i.getId().equalsIgnoreCase(id)) {
						selection = i;
						break;
					}
				}
				cmbFormat.setSelection(new StructuredSelection(selection));
			}else {
				IQueryExporter selection = exports.get(0);
				for (IQueryExporter i : exports) {
					if (i.getDefaultExtension() != null && i.getDefaultExtension().equalsIgnoreCase("csv")) { //$NON-NLS-1$
						selection = i;
						break;
					}
				}
				cmbFormat.setSelection(new StructuredSelection(selection));
			}
			cmbFormat.addSelectionChangedListener(new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent event) {
					fireListeners();
				}
			});
			
			ToolBar btn = new ToolBar(list, SWT.FLAT);
			adapt(btn);
			ToolItem moveUp = new ToolItem(btn, SWT.PUSH);
			moveUp.setImage(RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_UP));
			moveUp.addListener(SWT.Selection, e->{
				int index = uiElements.indexOf(q);
				index --;
				if (index < 0) index = 0;
				uiElements.remove(q);
				uiElements.add(index, q);
				updateList();
				fireListeners();
			});
			
			ToolItem moveDown = new ToolItem(btn, SWT.PUSH );
			moveDown.setImage(RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_DOWN));
			moveDown.addListener(SWT.Selection, e->{
				int index = uiElements.indexOf(q);
				uiElements.remove(q);
				index++;
				if (index > uiElements.size()) index = uiElements.size();
				uiElements.add(index, q);
				updateList();
				fireListeners();
			});
			
			ToolItem miDelete = new ToolItem(btn, SWT.PUSH);
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.addListener(SWT.Selection, e->removeQuery(q));
			
			l = toolkit.createLabel(list, "", SWT.SEPARATOR | SWT.HORIZONTAL); //$NON-NLS-1$
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
			
			q.setUiItems(dateComposite, cmbFormat);
		}
		
		l = toolkit.createLabel(list, Messages.QueryListComposite_Message1);
		l.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 5, 1));
		((GridData)l.getLayoutData()).verticalIndent = 10;
		l.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		
		FontData fd = l.getFont().getFontData()[0];
		fd.setStyle(SWT.ITALIC);
		Font newFont = new Font(l.getDisplay(), fd);
		l.addListener(SWT.Dispose, e->newFont.dispose());
		l.setFont(newFont);
		
		l = toolkit.createLabel(list, Messages.QueryListComposite_Message2);
		l.setLayoutData(new GridData(SWT.CENTER, SWT.FILL, true, false, 5, 1));
		l.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY));
		l.setFont(newFont);
		
		
		list.layout();
		form.reflow(true);
	}
	
	public List<QueryConfiguration> getQueries(){
		List<QueryConfiguration> configs = new ArrayList<>();
		for (QueryItem i : uiElements) {
			configs.add(i.asQueryConfiguration());
		}
		return configs;
	}
	
	private class QueryItem{
		Query query;
		ComboViewer formatSelector;
		QueryDateFilterComposite dateCom;
		
		Object lastFormat = null;
		Object lastDate = null;
		
		public QueryItem(Query query, QueryDateFilterComposite dateCom, ComboViewer formatSelector) {
			this.query = query;
			setUiItems(dateCom, formatSelector);
		}
		
		public void setUiItems( QueryDateFilterComposite dateCom, ComboViewer formatSelector) {
			this.formatSelector = formatSelector;
			this.dateCom = dateCom;
			
			if (formatSelector != null)
				formatSelector.addSelectionChangedListener(e->lastFormat = ((IQueryExporter)formatSelector.getStructuredSelection().getFirstElement()).getId());
			if (dateCom != null)
				dateCom.addChangeListener(e->lastDate = dateCom.getDateFilter());
		}
		private QueryConfiguration asQueryConfiguration() {
			IQueryExporter exporter = (IQueryExporter) formatSelector.getStructuredSelection().getFirstElement();
			return new QueryConfiguration(query, exporter, dateCom == null ? null : dateCom.getDateFilter());
		}
	}
}

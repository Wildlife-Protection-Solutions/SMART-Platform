package org.wcs.smart.r.ui.editor.script;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.QueryTypeManager;
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

public class QueryListComposite extends Composite{

	private List<Query> queries;
	private List<QueryItem> uiElements;
	
	private Composite list;
	private FormToolkit toolkit;
	private ScrolledForm form;
	
	public QueryListComposite(Composite parent) {
		super(parent, SWT.BORDER);
		
		queries = new ArrayList<>();
		
		toolkit = new FormToolkit(Display.getDefault());
		addListener(SWT.Dispose, e->toolkit.dispose());
		
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		form = toolkit.createScrolledForm(this);
		form.getBody().setLayout(new GridLayout());
		list = toolkit.createComposite(form.getBody(), SWT.NONE);
		list.setLayout(new GridLayout(5, false));
		form.setContent(list);
		form.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		list.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)list.getLayout()).marginWidth = 0;
		((GridLayout)list.getLayout()).marginHeight = 0;
	}
	
	
	
	public void addQuery(Query query) {
		queries.add(query);
		updateList();
	}
	public void addQueries(List<Query> queries) {
		this.queries.addAll(queries);
		updateList();
	}

	public void removeQuery(Query query) {
		queries.remove(query);
		updateList();
	}
	
	private void adapt(Control c) {
		toolkit.adapt(c,false, false);
		if (c instanceof Composite) {
			for (Control kid : ((Composite) c).getChildren()) {
				adapt(kid);
			}
		}
		
	}
	private void updateList() {
		for (Control c : list.getChildren()) c.dispose();
		
		
		Label l = toolkit.createLabel(list, "Query Name");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		toolkit.createLabel(list, "Date Filter");
		toolkit.createLabel(list, "Export Format");
		toolkit.createLabel(list, "");
		
		l = toolkit.createLabel(list, "", SWT.SEPARATOR | SWT.HORIZONTAL);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		
		uiElements = new ArrayList<>();
		for (Query q : queries) {
			IQueryType qType = QueryTypeManager.INSTANCE.findQueryType(q.getTypeKey());
			
			//query icon
			l = toolkit.createLabel(list, "");
			l.setImage(qType.getImage());
			
			//query name
			toolkit.createLabel(list, q.getName());
			
			//date filter
			QueryDateFilterComposite dateComposite = new QueryDateFilterComposite(list, qType.getDateFilterOptions(), IDateFilter.DATE_FILTERS, false);
			dateComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			adapt(dateComposite);
			dateComposite.addChangeListener(evt->{
				list.layout(true);
				form.reflow(true);
			});
			
			//format
			ComboViewer cmbFormat = new ComboViewer(list, SWT.DROP_DOWN | SWT.READ_ONLY);
			cmbFormat.setLabelProvider(new QueryExportLabelProvider());
			cmbFormat.setContentProvider(ArrayContentProvider.getInstance());
			List<IQueryExporter> exports = QueryExportEngine.getQueryExports(q);
			Collections.sort(exports, new Comparator<IQueryExporter>() {
				@Override
				public int compare(IQueryExporter o1, IQueryExporter o2) {
					return Collator.getInstance().compare(o1.getName(), o2.getName());
				}
			});
			cmbFormat.setInput(exports);
			IQueryExporter selection = exports.get(0);
			for (IQueryExporter i : exports) {
				if (i.getDefaultExtension().equalsIgnoreCase("csv")) {
					selection = i;
					break;
				}
			}
			cmbFormat.setSelection(new StructuredSelection(selection));
			
			ToolBar btn = new ToolBar(list, SWT.FLAT);
			adapt(btn);
			ToolItem moveUp = new ToolItem(btn, SWT.PUSH);
			moveUp.setImage(RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_UP));
			moveUp.addListener(SWT.Selection, e->{
				int index = queries.indexOf(q);
				index --;
				if (index < 0) index = 0;
				queries.remove(q);
				queries.add(index, q);
				updateList();
			});
			
			ToolItem moveDown = new ToolItem(btn, SWT.PUSH );
			moveDown.setImage(RPlugIn.getDefault().getImageRegistry().get(RPlugIn.ICON_DOWN));
			moveDown.addListener(SWT.Selection, e->{
				int index = queries.indexOf(q);
				queries.remove(q);
				index++;
				if (index > queries.size()) index = queries.size();
				queries.add(index, q);
				updateList();
			});
			
			ToolItem miDelete = new ToolItem(btn, SWT.PUSH);
			miDelete.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DELETE_ICON));
			miDelete.addListener(SWT.Selection, e->removeQuery(q));
			
			l = toolkit.createLabel(list, "", SWT.SEPARATOR | SWT.HORIZONTAL);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
			
			uiElements.add(new QueryItem(q, dateComposite, cmbFormat));
		}
		
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
		
		public QueryItem(Query query, QueryDateFilterComposite dateCom, ComboViewer formatSelector) {
			this.query = query;
			this.formatSelector = formatSelector;
			this.dateCom = dateCom;
		}
		
		private QueryConfiguration asQueryConfiguration() {
			IQueryExporter exporter = (IQueryExporter) formatSelector.getStructuredSelection().getFirstElement();
			DateFilter dFilter = dateCom.getDateFilter();
			return new QueryConfiguration(query, exporter, dFilter);
		}
	}
}

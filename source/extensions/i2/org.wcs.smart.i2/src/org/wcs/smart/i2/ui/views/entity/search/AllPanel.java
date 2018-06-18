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
package org.wcs.smart.i2.ui.views.entity.search;

import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import javax.inject.Inject;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelAttribute;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.search.AllEntitySearch;
import org.wcs.smart.i2.ui.handler.OpenEntityHandler;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.entity.search.AllEntityContentProvider.EntityTableData;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * All entity table panel for entity search view
 * 
 * @author Emily
 *
 */
public class AllPanel extends Composite {

	@Inject
	private IEclipseContext context;
	private EntitySearchView view;
	
	//ui components
	private FormToolkit toolkit;
	private Composite tableComposite;
	private TableViewer entityTable;
	private EntitySearchPanel searchPanel;

	//search object
	private AllEntitySearch entitySearch;
	
	public AllPanel(Composite parent, EntitySearchView view, FormToolkit toolkit) {
		super(parent, SWT.NONE);
		this.view = view;
		this.toolkit = toolkit;
		createContents();
		addDisposeListener(e->toolkit.dispose());
		
		entitySearch = new AllEntitySearch(null);
	}

	private void createContents() {
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		SashForm main = new SashForm(this, SWT.VERTICAL);
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		searchPanel = new EntitySearchPanel(main) {
			@Override
			public void saveSearch() {
				entitySearch.setFilterString(getQueryString());
				view.saveSearch(entitySearch);
			}
			
			@Override
			public void doSearch() {
				
				if (entityTable == null || entityTable.getControl().isDisposed()) {
					refresh();
				}else {
					((AllEntityContentProvider)entityTable.getContentProvider()).setFilter(getQueryString());
				}
			}
		};
		searchPanel.addQueryModifiedListener(e->{
			//show if hidden; otherwise leave alone 
			double ratio = main.getWeights()[0] / (double)main.getWeights()[1];
			if (ratio <= 1/20.0) {
				main.setWeights(new int[] {1,4});
			}
		});
		
		ToolItem refreshItem = new ToolItem(searchPanel.getToolbar(), SWT.PUSH);
		refreshItem.addListener(SWT.Selection, e->refresh());
		refreshItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_REFRESH));
		refreshItem.setToolTipText("reload entity results");
		
		ToolItem configureItem = new ToolItem(searchPanel.getToolbar(), SWT.PUSH);
		configureItem.addListener(SWT.Selection, e->configureTable());
		configureItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CONFIGURE));
		configureItem.setToolTipText("configure table columns");
		
		ToolItem hideFilters = new ToolItem(searchPanel.getToolbar(), SWT.PUSH);
		hideFilters.addListener(SWT.Selection, e->{
			double ratio = main.getWeights()[0] / (double)main.getWeights()[1];
			if (ratio <= 1/20.0) {
				main.setWeights(new int[] {1,4});
			}else {
				main.setWeights(new int[] {1,20});
			}
		});
		hideFilters.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_FILTERS));
		hideFilters.setToolTipText("show/hide filters");
		
		tableComposite = toolkit.createComposite(main);
		
		tableComposite.setLayout(new GridLayout());
		((GridLayout)tableComposite.getLayout()).marginWidth = 0;
		((GridLayout)tableComposite.getLayout()).marginHeight = 0;
	
		//only do search when this panel is viewed
		Listener firstSearch = new Listener() {
			@Override
			public void handleEvent(Event event) {
				tableComposite.removeListener(SWT.Paint, this);
				refresh();
			}};
		tableComposite.addListener(SWT.Paint, firstSearch);
			
		
		
		main.setWeights(new int[] {1,20});
	}
	
	/**
	 * Initialize the panel with the contents from the saved search
	 * @param search
	 */
	public void initPanel(AllEntitySearch search) {
		this.entitySearch = search;
		searchPanel.initPanel(search.getFilterString());
		createTable();
		searchPanel.doSearch();
	}
	
	/**
	 * Clears the table and reruns the search
	 */
	public void refresh() {
		for (Control c : tableComposite.getChildren()) c.dispose();
		toolkit.createLabel(tableComposite, "Loading....");
		tableComposite.layout();
		refreshJob.schedule();
	}

	/*
	 * displays dialog for configuring table columns
	 */
	private void configureTable() {
		ColumnSelectorDialog dialog = new ColumnSelectorDialog(getShell(), entitySearch.getQueryColumns());
		if (dialog.open() != Window.OK) return;
		
		entitySearch.setQueryColumns(dialog.getVisibleColumns());
		createTable();
	}
	
	/*
	 * recreates the table with the existing data/content provider
	 */
	private void createTable() {
		if (entityTable == null || entityTable.getControl().isDisposed()) return;
		EntityTableData data = (EntityTableData) entityTable.getInput();
		AllEntityContentProvider provider = (AllEntityContentProvider) entityTable.getContentProvider();
		createTable(data, provider);	
	}
	
	/*
	 * recreates the table with the provided data/content provider
	 */
	//must be called from display thread
	private void createTable(EntityTableData data, AllEntityContentProvider provider) {
		for (Control c : tableComposite.getChildren()) c.dispose();

		entityTable = new TableViewer(tableComposite, SWT.BORDER | SWT.VIRTUAL | SWT.FULL_SELECTION);
		entityTable.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		entityTable.setContentProvider(provider);
		entityTable.getTable().setHeaderVisible(true);
		entityTable.getTable().setLinesVisible(true);
		entityTable.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				
				Object item = entityTable.getStructuredSelection().getFirstElement();
				if (item == null) return;
				if (!(item instanceof EntityTableRowItem)) return;
				
				UUID entityUuid = ((EntityTableRowItem)item).getEntityUuid();
				IntelEntity temp = null;
				try(Session session = HibernateManager.openSession()){
					temp = session.get(IntelEntity.class, entityUuid);
					if (temp != null) {
						temp.getIdAttributeAsText();
						temp.getEntityType();
					}
				}
				if (temp == null) {
					MessageDialog.openInformation(context.get(Shell.class), "Not Found", "Entity Not Found.");
					return;
				}
				(new OpenEntityHandler()).openEntity(temp, context);
				
			}
		});
		
		TableViewerColumn idColumn = new TableViewerColumn(entityTable, SWT.NONE);
		idColumn.getColumn().setText("Entity ID");
		idColumn.getColumn().setWidth(150);
		
		idColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == null) return "...";
				String keyId = ((EntityTableRowItem)element).getId();
				
				Object value =  ((EntityTableRowItem)element).getAttributeValue(keyId);
				IntelAttribute attribute = null;
				for (IntelAttribute aa : data.getAttributes()) {
					if (aa.getKeyId().equals(keyId)) {
						attribute = aa;
						break;
					}
				}
				if (value == null || attribute == null) return "";
				switch(attribute.getType()) {
				case BOOLEAN:
					if (((Double)value) < 0.5) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
					return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
				case DATE:
					return DateFormat.getDateInstance().format(java.sql.Date.valueOf((String)value));
				case EMPLOYEE:
				case LIST:
				case NUMERIC:
				case POSITION:
				case TEXT:
					return value.toString();
				}
				
				return ((EntityTableRowItem)element).getId();
			}
		});
		
		TableViewerColumn typeColumn = new TableViewerColumn(entityTable, SWT.NONE);
		typeColumn.getColumn().setText("Entity Type");
		typeColumn.getColumn().setWidth(150);
		typeColumn.getColumn().addListener(SWT.Selection, e->{
			provider.setSortColumn(AllEntityContentProvider.COL_ENTITY_TYPE_NAME);
			entityTable.getTable().setSortColumn(typeColumn.getColumn());
			entityTable.getTable().setSortDirection(provider.getSortDirection());
		});
		typeColumn.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				if (element == null) return "...";
				return ((EntityTableRowItem)element).getType();
			}
		});
		
		for (IntelAttribute attribute : data.getAttributes()) {
			if (entitySearch.getQueryColumns() != null && !entitySearch.getQueryColumns().contains(attribute.getKeyId())) continue;
			
			TableViewerColumn aColumn = new TableViewerColumn(entityTable, SWT.NONE);
			aColumn.getColumn().setText(attribute.getName());
			aColumn.getColumn().setWidth(150);
			aColumn.getColumn().addListener(SWT.Selection, e->{
				provider.setSortColumn(attribute.getKeyId());
				entityTable.getTable().setSortColumn(aColumn.getColumn());
				entityTable.getTable().setSortDirection(provider.getSortDirection());
			});
			
			aColumn.setLabelProvider(new ColumnLabelProvider() {
				@Override
				public String getText(Object element) {
					if (element == null) return "...";
					Object value =  ((EntityTableRowItem)element).getAttributeValue(attribute.getKeyId());
					if (value == null) return "";
					switch(attribute.getType()) {
					case BOOLEAN:
						if (((Double)value) < 0.5) return SmartLabelProvider.BOOLEAN_FALSE_LABEL;
						return SmartLabelProvider.BOOLEAN_TRUE_LABEL;
					case DATE:
						return DateFormat.getDateInstance().format((Date)value);
					case EMPLOYEE:
					case LIST:
					case NUMERIC:
					case POSITION:
					case TEXT:
						return value.toString();
					}
					return element.toString();
				}
			});
		}
		
		tableComposite.layout(true);
		
		entityTable.setInput(data);
	}

	private String getFilterString() {
		return searchPanel.getQueryString();
	}
	
	private Job refreshJob = new Job("refresh entity table") {

		private String filterString = null;
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			AllEntityContentProvider provider = new AllEntityContentProvider();
			EntityTableData data = provider.generateData();
			
			Display.getDefault().syncExec(()->{filterString = getFilterString();});
			
			if (filterString != null && !filterString.isEmpty()) {
				provider.setFilter(filterString);
			}
			
			if (data == null) {
				Display.getDefault().syncExec(()->{
					toolkit.createLabel(tableComposite, "ERROR");
					tableComposite.layout(true);
				});
				return Status.OK_STATUS;

			}
			Display.getDefault().syncExec(()->{
				createTable(data, provider);
				
			});
			
			return Status.OK_STATUS;
		}
		
	};


}
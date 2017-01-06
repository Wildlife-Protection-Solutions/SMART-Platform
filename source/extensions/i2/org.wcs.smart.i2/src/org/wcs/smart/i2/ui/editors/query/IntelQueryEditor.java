/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.query;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.query.observation.filter.ParsedObservationQuery;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter.FilterType;
import org.wcs.smart.i2.query.observation.parser.ParseException;
import org.wcs.smart.i2.query.observation.parser.Parser;
import org.wcs.smart.i2.ui.SmartSection;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItemFactory;
import org.wcs.smart.i2.ui.views.query.dropitem.ErrorDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.FilterDefinitionPanel;

/**
 * Intelligence query editor
 * 
 * @author Emily
 *
 */
public class IntelQueryEditor extends EditorPart{

	public static final String ID = "org.wcs.smart.i2.editor.query";

	private boolean isDirty = false;
	
	private IntelRecordQuery query;
	
	private IntelQueryNameLabel header;
	private DateFilterDropDownComposite datePart;
	private FilterDefinitionPanel panel;
	private IEclipseContext context;
	private IEventBroker eventBroker;
	
	private ToolItem runItem;
	
	private boolean isInitializing = false;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		String queryString = validateQuery();
		if (queryString == null){
			MessageDialog.openError(getSite().getShell(), "ERROR", "Cannot save an invalid query.");
			return;
		}
		query.setQueryString(queryString);
		
		boolean isNew = query.getUuid() == null;
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
		
			s.saveOrUpdate(query);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			Intelligence2PlugIn.displayLog("Error saving query: " + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		if (isNew){
			eventBroker.post(IntelEvents.QUERY_NEW, query);
			((QueryEditorInput)getEditorInput()).setUuid(query.getUuid());
		}else{
			eventBroker.post(IntelEvents.QUERY_MODIFIED, query);
		}
		
		
		setPartName(query.getName());
		setDirty(false);
		
	}

	@Override
	public void doSaveAs() {
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setInput(input);
		super.setSite(site);	
		setPartName(((QueryEditorInput)input).getName());
	}

	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	private void closeEditor(){
		getSite().getPage().closeEditor(IntelQueryEditor.this, false);
	}

	@Override
	public void createPartControl(Composite parent) {
		//TODO: add tags so it works in both perspectives
		context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		eventBroker = context.get(IEventBroker.class);
		
		eventBroker.subscribe(IntelEvents.QUERY_DELETED, new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				Object data = event.getProperty(IEventBroker.DATA);
				if (data instanceof IntelRecordQuery){
					if (data.equals(query)){
						closeEditor();
						return;
					}
				}else if (data instanceof List){
					List dd = (List)data;
					for (Object d: dd){
						if (d.equals(query)){
							closeEditor();
							return;
						}
					}
				}
				
			}
		});
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Form pageForm = toolkit.createForm(parent);
		pageForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite main = pageForm.getBody();
		main.setLayout(new GridLayout());
		
		header = new IntelQueryNameLabel(main, toolkit, pageForm.getFont(), pageForm.getForeground());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		header.addListener(SWT.Selection, e-> {
			if (query == null){
				e.doit = false;
				return;
			}
			query.setName(e.text);
			query.updateName(SmartDB.getCurrentLanguage(), e.text);
			setDirty(true);
		});
		
		createDatePart(main, toolkit);
		
		SashForm core = new SashForm(main, SWT.VERTICAL);
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartSection resultsSection = new SmartSection(core, toolkit, "Results");
		Composite c = toolkit.createComposite(resultsSection);
		c.setLayout(new GridLayout());
		toolkit.createLabel(c, "results");
		
		SmartSection definitionSection = new SmartSection(core, toolkit, "Definition");
		c = toolkit.createComposite(definitionSection);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		panel = new FilterDefinitionPanel();
		Composite definitionPanel = panel.createComposite(c);
		definitionPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		panel.addQueryChangedListener(()->{
			setDirty(true);
			validateQuery();
		});
		
		loadQueryJob.schedule();
	}
	

	
	private String validateQuery(){
		if (isInitializing) return null; //do not valid while initializing
		String queryString = panel.getQueryPart();
		try{
			IntelRecordQuery.parseQuery(queryString);
			panel.setErrorMessage(null, null);
			runItem.setEnabled(true);
			return queryString;
		}catch (Exception ex){
			runItem.setEnabled(false);
			panel.setErrorMessage("Query is invalid", ex);
			return null;
		}
	}
	public void addDropItems(DropItem[] item){
		for (DropItem i : item){
			panel.addItem(i);
		}
	}

	@Override
	public void setFocus() {
		header.setFocus();
	}
	
	private void createDatePart(Composite parent, FormToolkit toolkit){
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		((GridLayout)main.getLayout()).horizontalSpacing = 0;
		
		DateFilterComposite.DateFilter[] defaultFilters = new DateFilter[]{
				DateFilter.CURRENT_MONTH,
				DateFilter.LAST_30_DAYS,
				DateFilter.LAST_60_DAYS,
				DateFilter.CURRENT_YEAR,
				DateFilter.LAST_YEAR,
				DateFilter.LAST_5_YEARS,
				DateFilter.ALL,
				DateFilter.CUSTOM
		};
		
		datePart = new DateFilterDropDownComposite(main, defaultFilters, DateFilter.ALL, true);
		datePart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		datePart.addChangeListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				main.layout(true);
			}
		}); 
		datePart.adapt(toolkit);
		
		ToolBar headerToolbar = new ToolBar(main, SWT.FLAT);
		runItem = new ToolItem(headerToolbar, SWT.PUSH);
		runItem.setToolTipText("run query");
		runItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RUN));
		runItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				runQuery();
			}
		});
		headerToolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		main.getParent().layout(true, true);
		headerToolbar.redraw();
		headerToolbar.layout(true, true);
	}
	
	
	private void runQuery(){
		System.out.println("run query");
	}
	
	private void initUiField(){
		setPartName(query.getName());
		header.setText(query.getName());
		
		if (query.getUuid() == null) setDirty(true);
	}
	
	
	private Job loadQueryJob = new Job("loading query"){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			query = null;
			UUID uuid = ((QueryEditorInput)getEditorInput()).getUuid();
			
			List<DropItem> generatedDropItems = new ArrayList<>();
			IQueryFilter.FilterType filterType = IQueryFilter.FilterType.OBSERVATION;
			
			if (((QueryEditorInput)getEditorInput()).isNew()){
				uuid = null;
				IntelRecordQuery temp = new IntelRecordQuery();
				temp.setName("<New Query>");
				temp.updateName(SmartDB.getCurrentLanguage(), temp.getName());
				temp.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), temp.getName());
				temp.setConservationArea(SmartDB.getCurrentConservationArea());
				
				query = temp;
			}else{
				
				Session s = HibernateManager.openSession();
				try{
					IntelRecordQuery temp = (IntelRecordQuery)s.get(IntelRecordQuery.class, uuid);
					if (temp == null){
						Intelligence2PlugIn.displayLog("Query not found.", null);
						closeEditor();
						return Status.OK_STATUS;
					}
					temp.getNames().size();
					query = temp;
					
					try{
						ParsedObservationQuery parsedQuery = IntelRecordQuery.parseQuery(query.getQueryString());
						generatedDropItems = DropItemFactory.generateDropItems(parsedQuery.getFilter(), s);
						filterType = parsedQuery.getFilterType();
					}catch(Exception ex){
						DropItem di = new ErrorDropItem("Unable to parse query: " + ex.getMessage());
						generatedDropItems.add(di);
					}
					
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog("Error loading query from database: " + ex.getMessage(), ex);
					getSite().getPage().closeEditor(IntelQueryEditor.this, false);
					return Status.OK_STATUS;
				}finally{
					s.close();
				}
			}
			final List<DropItem> fGeneratedDropItems = generatedDropItems;
			final IQueryFilter.FilterType fType = filterType;
			Display.getDefault().syncExec(()->{
				isInitializing = true;
				try{
					initUiField();
					panel.setFilterType(fType);
					panel.addItems(fGeneratedDropItems);
				}finally{
					isInitializing = false;
				}
				validateQuery();
				setDirty(false);
					
			});
			return Status.OK_STATUS;
		}
		
	};
}

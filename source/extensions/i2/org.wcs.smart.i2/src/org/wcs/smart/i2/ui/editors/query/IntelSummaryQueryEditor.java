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

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelRecordSummaryQuery;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.RunQueryJob;
import org.wcs.smart.i2.query.SummaryQueryResult;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.i2.query.observation.filter.ValuePart;
import org.wcs.smart.i2.query.observation.filter.ValuePart.ValueOption;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntityPerspective;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.SmartSection;
import org.wcs.smart.i2.ui.dialogs.query.ExportQueryWizard;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItemFactory;
import org.wcs.smart.i2.ui.views.query.dropitem.ErrorDropItem;
import org.wcs.smart.ui.SmartStyledInputDialog;
import org.wcs.smart.ui.SmartWizardDialog;

/**
 * Intelligence query editor for record observation query
 * 
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class IntelSummaryQueryEditor extends EditorPart implements IQueryEditor{

	public static final String ID = "org.wcs.smart.i2.editor.query.entitysummary"; //$NON-NLS-1$

	private boolean isDirty = false;
	
	//injects
	private IEclipseContext context;
	private IEventBroker eventBroker;

	private DateFilterDropDownComposite datePart;

	//query
	private AbstractIntelQuery query;
	private enum Type {ENTITY, RECORD};
	private Type type;
	
	//header & date part
	private IntelQueryNameLabel header;
	
	//filter panel
	private SummaryDefinitionPanel summaryPanel;
	private ToolItem[] runItem = new ToolItem[2];
	private ToolItem saveItem;

	//results area
	private Composite stackPanel;
	private Composite resultsArea;
	private ProgressPanel progressPanel;
	private ErrorPanel errorPanel;
	
	private boolean isInitializing = false;
	private RunQueryJob runJob;
	private List<EventHandler> eventHandles;
	
	private FormToolkit toolkit;
	
	private SummaryQueryResult cachedResults;
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		String queryString = validateQuery();
		if (queryString == null){
			MessageDialog.openError(getSite().getShell(), Messages.IntelQueryEditor_ErrorDialogTitle, Messages.IntelQueryEditor_InvalidQuery);
			return;
		}
		query.setQueryString(queryString);
		query.setProfileFilter(summaryPanel.getProfileFilter());
		boolean isNew = query.getUuid() == null;
		try(Session s = HibernateManager.openSession()){
			s.beginTransaction();
			try {
				s.saveOrUpdate(query);
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(Messages.IntelQueryEditor_ErrorSavingQuery + ex.getMessage(), ex);
				return;
			}
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
	public void dispose(){
		super.dispose();
		toolkit.dispose();
		//remove all event subscriptions
		if (eventHandles != null) eventHandles.forEach((h)->eventBroker.unsubscribe(h));
	}
	
	@Override
	public void doSaveAs() {
		String queryString = validateQuery();
		if (queryString == null){
			MessageDialog.openError(getSite().getShell(), Messages.IntelQueryEditor_ErrorDialogTitle, Messages.IntelQueryEditor_InvalidQuery);
			return;
		}
		query.setQueryString(queryString);
		
		InputDialog newName = new SmartStyledInputDialog(getSite().getShell(), Messages.IntelQueryEditor_SaveAsTitle, Messages.IntelQueryEditor_SaveAsMessage, MessageFormat.format(Messages.IntelQueryEditor_DefaultQueryName, query.getName()), new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText == null || newText.trim().length() == 0) return Messages.IntelQueryEditor_NameRequireError;
				return null;
			}
		});
		if (newName.open() != Window.OK){
			return;
		}
		
		AbstractIntelQuery clone = null;
		if (type == Type.ENTITY) {
			clone = new IntelEntitySummaryQuery();
		}else if (type == Type.RECORD) {
			clone = new IntelRecordSummaryQuery();
		}
		clone.setConservationArea(SmartDB.getCurrentConservationArea());
		clone.setQueryString(query.getQueryString());
		clone.setProfileFilter(summaryPanel.getProfileFilter());
		clone.setName(newName.getValue());
		clone.updateName(SmartDB.getCurrentLanguage(), clone.getName());
		clone.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), clone.getName());
		
		try(Session s = HibernateManager.openSession()){
		
			s.beginTransaction();
			try {
				s.save(clone);
				s.getTransaction().commit();
			}catch (Exception ex){
				s.getTransaction().rollback();
				Intelligence2PlugIn.displayLog(Messages.IntelQueryEditor_CloneError + ex.getMessage(), ex);
				return;
			}
		}
		
		eventBroker.post(IntelEvents.QUERY_NEW, query);
		setInput(new QueryEditorInput(clone));
		
		this.query = clone;
		initUiField();
		setDirty(false);
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.setInput(input);
		super.setSite(site);	
		setPartName(((QueryEditorInput)input).getName());
		
		if (((QueryEditorInput)input).getTypeKey().equals(IntelEntitySummaryQuery.KEY)){
			type = Type.ENTITY;
		}else if (((QueryEditorInput)input).getTypeKey().equals(IntelRecordSummaryQuery.KEY)){
			type = Type.RECORD;
		}
	}

	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		if (saveItem != null) saveItem.setEnabled(isDirty);
		summaryPanel.setQueryState(isDirty);
		firePropertyChange(IEditorPart.PROP_DIRTY);
	}
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return true;
	}
	
	private void closeEditor(boolean promptSave){
		getSite().getPage().closeEditor(IntelSummaryQueryEditor.this, promptSave);
	}

	@Override
	public void createPartControl(Composite parent) {
		context = (IEclipseContext) getSite().getService(IEclipseContext.class);
		MPart part = context.get(MPart.class);
		if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
		if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);
		if (!part.getTags().contains(EntityPerspective.ID)) part.getTags().add(EntityPerspective.ID);
		
		eventBroker = context.get(IEventBroker.class);
		
		eventHandles = new ArrayList<>();
		EventHandler handler = new EventHandler() {
			@SuppressWarnings("rawtypes")
			@Override
			public void handleEvent(Event event) {
				Object data = event.getProperty(IEventBroker.DATA);
				if (data instanceof IntelEntitySummaryQuery){
					if (data.equals(query)){
						closeEditor(false);
						return;
					}
				}else if (data instanceof List){
					List dd = (List)data;
					for (Object d: dd){
						if (d.equals(query)){
							closeEditor(false);
							return;
						}
					}
				}
				
			}
		};
		eventBroker.subscribe(IntelEvents.QUERY_DELETED, handler);
		eventHandles.add(handler);

		//profiles modified
		handler = event->{
			if (!query.queriesProfile(ProfilesManager.INSTANCE.getActiveProfileKeys())) closeEditor(true);
		};
		eventHandles.add(handler);
		eventBroker.subscribe(IntelEvents.PROFILES_ALL, handler);
				
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth = 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		
		toolkit = new FormToolkit(parent.getDisplay());
		
		Form pageForm = toolkit.createForm(parent);
		pageForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		Composite main = pageForm.getBody();
		main.setLayout(new GridLayout());
		
		Composite headerComp = toolkit.createComposite(main);
		headerComp.setLayout(new GridLayout(2, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		WidgetElement.setCSSClass(headerComp, "SMARTFormHeader");  //$NON-NLS-1$
		
		header = new IntelQueryNameLabel(headerComp, toolkit, pageForm.getFont(), pageForm.getForeground());
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
		
		ToolBar headerToolbar = new ToolBar(headerComp, SWT.FLAT);
		headerToolbar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));

		if (IntelSecurityManager.INSTANCE.canEditQuery()) {
			saveItem = new ToolItem(headerToolbar, SWT.PUSH);
			saveItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVE_ICON));
			saveItem.addListener(SWT.Selection, (event)->IntelSummaryQueryEditor.this.getSite().getPage().saveEditor(IntelSummaryQueryEditor.this, false));
			saveItem.setToolTipText(Messages.IntelQueryEditor_saveTooltip);
			
			ToolItem saveAsItem = new ToolItem(headerToolbar, SWT.PUSH);
			saveAsItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVEAS_ICON));
			saveAsItem.addListener(SWT.Selection, (event)->doSaveAs());
			saveAsItem.setToolTipText(Messages.IntelQueryEditor_saveAsTooltip);
		}
		
		ToolItem exportItem = new ToolItem(headerToolbar, SWT.PUSH);
		exportItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EXPORT_QUERY));
		exportItem.addListener(SWT.Selection, (event)->exportQuery());
		exportItem.setToolTipText(Messages.IntelQueryEditor_ExportTooltip);
		
		ToolItem runItem2 = new ToolItem(headerToolbar, SWT.PUSH);
		runItem2.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RUN));
		runItem2.addListener(SWT.Selection, (event)->runQuery());
		runItem2.setToolTipText(Messages.IntelQueryEditor_RunTooltip);
		runItem[0] = runItem2;
		
		if (type == Type.RECORD) createDatePart(main, toolkit);
		
		SashForm core = new SashForm(main, SWT.VERTICAL);
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		SmartSection resultsSection = new SmartSection(core, toolkit, Messages.IntelQueryEditor_ResultsSectionLabel);

			
		Composite c = toolkit.createComposite(resultsSection);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		createResultSection(c, toolkit);
		
		SmartSection definitionSection = new SmartSection(core, toolkit, Messages.IntelQueryEditor_DefinitionSelctionLabel);
		((GridLayout)definitionSection.getLayout()).verticalSpacing = 0;
		
		summaryPanel = new SummaryDefinitionPanel(this);
		Composite temp = summaryPanel.createComposite(definitionSection);
		temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		summaryPanel.addQueryChangedListener(()->{
			setDirty(true);
			validateQuery();
		});
		
		loadQueryJob.schedule();
	}

	private void createResultSection(Composite parent, FormToolkit toolkit){
		stackPanel = new Composite(parent, SWT.NONE);
		stackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		stackPanel.setLayout(new StackLayout());
		
		Composite runQueryComp = toolkit.createComposite(stackPanel);
		runQueryComp.setLayout(new GridLayout());
		Hyperlink l = toolkit.createHyperlink(runQueryComp, Messages.IntelQueryEditor_RunQueryLink, SWT.NONE);
		l.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				runQuery();
			}
		});
		
		progressPanel = new ProgressPanel(stackPanel);
		resultsArea = toolkit.createComposite(stackPanel, SWT.NONE);
		resultsArea.setLayout(new GridLayout());
		((GridLayout)resultsArea.getLayout()).marginWidth = 0;
		((GridLayout)resultsArea.getLayout()).marginHeight = 0;
		errorPanel = new ErrorPanel(stackPanel);
		((StackLayout)stackPanel.getLayout()).topControl = runQueryComp;		
	}
	
	private void exportQuery(){
		SummaryQueryResult results = cachedResults;
		if (results == null){
			MessageDialog.openInformation(getSite().getShell(), Messages.IntelQueryEditor_ExportTitle, Messages.IntelQueryEditor_ExportMsg);
			return;
		}
		ExportQueryWizard wizard = new ExportQueryWizard(query, results);
		WizardDialog wd = new SmartWizardDialog(getSite().getShell(), wizard);
		wd.open();
	}
	
	public void runQuery(){
		
		if (type == Type.RECORD) {
			Date[] dateFilter = null;
			if (datePart.getDateFilter() == DateFilter.CUSTOM){
				dateFilter = new Date[]{datePart.getCustomStartDate(), datePart.getCustomEndDate()};
			}else if (datePart.getDateFilter() == DateFilter.ALL){
				dateFilter = new Date[]{null, null};
			}else{
				dateFilter = new Date[]{datePart.getDateFilter().getStartDate(), datePart.getDateFilter().getEndDate()};
			}
			runJob.setDateFilter(dateFilter);
		}
		
		cachedResults = null;
		for (Control c : resultsArea.getChildren()) c.dispose();
		((StackLayout)stackPanel.getLayout()).topControl = progressPanel;
		stackPanel.layout(true);
		String queryString = summaryPanel.getQueryPart();
		query.setQueryString(queryString);
		query.setProfileFilter(summaryPanel.getProfileFilter());
		runJob.setQuery(query);
		runJob.schedule();
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
		runItem[1] = new ToolItem(headerToolbar, SWT.PUSH);
		runItem[1].setToolTipText(Messages.IntelQueryEditor_runTooltip);
		runItem[1].setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RUN));
		runItem[1].addSelectionListener(new SelectionAdapter() {
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
	public String validateQuery(){
		if (isInitializing) return null; //do not valid while initializing
		
		try{
			String err = summaryPanel.validate();
			if (err != null) throw new Exception(err);
			
			String queryString = summaryPanel.getQueryPart();
			if (type == Type.ENTITY) IntelEntitySummaryQuery.parseQuery(queryString);
			if (type == Type.RECORD) IntelRecordSummaryQuery.parseQuery(queryString);
			summaryPanel.setErrorMessage(null, null);
			for (ToolItem ri : runItem) if (ri != null) ri.setEnabled(true);
			
			return queryString;
		}catch (Exception ex){
			for (ToolItem ri : runItem) if (ri != null) ri.setEnabled(false);
			summaryPanel.setErrorMessage(Messages.IntelQueryEditor_InvalidQueryError, ex);
			return null;
		}
	}
	
	
	@Override
	public void addDropItems(DropItem[] item){
		for (DropItem i : item){
			summaryPanel.addItem(i);
		}
	}

	@Override
	public void setFocus() {
		header.setFocus();
	}
	
	private void initUiField(){
		setPartName(query.getName());
		header.setText(query.getName(), InternalQueryManager.INSTANCE.getName(query.getTypeKey()));
		if (query.getUuid() == null) setDirty(true);
	}
	
	@Override
	public AbstractIntelQuery getQuery(){
		return this.query;
	}
	
	private Job loadQueryJob = new Job(Messages.IntelQueryEditor_loadQueryJobname){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			query = null;
			UUID uuid = ((QueryEditorInput)getEditorInput()).getUuid();
			
			final List<DropItem> filterDropItems = new ArrayList<>();
			final List<DropItem> rowGbDropItems = new ArrayList<>();
			final List<DropItem> colGbDropItems = new ArrayList<>();
			final List<DropItem> valueGbDropItems = new ArrayList<>();
			

			if (((QueryEditorInput)getEditorInput()).isNew()){
				uuid = null;
				
				AbstractIntelQuery temp = null;
				ValuePart vp = null;
				switch(type) {
				case ENTITY:
					temp = new IntelEntitySummaryQuery();
					vp = new ValuePart(ValueOption.NUMBER_ENTITIES);
					break;
				case RECORD:
					temp = new IntelRecordSummaryQuery();
					vp = new ValuePart(ValueOption.NUMBER_RECORDS);
					break;
				};
				temp.setName(Messages.IntelQueryEditor_defaultQueryName);
				temp.updateName(SmartDB.getCurrentLanguage(), temp.getName());
				temp.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), temp.getName());
				temp.setConservationArea(SmartDB.getCurrentConservationArea());
				
				temp.setProfileFilter(AbstractIntelQuery.convertToProfileFilter(ProfilesManager.INSTANCE.getActiveProfiles()
						.stream().filter(e->IntelSecurityManager.INSTANCE.canViewQuery(e)).collect(Collectors.toSet())));
				
				StringBuilder sb = new StringBuilder();
				sb.append(IntelEntitySummaryQuery.PART_SEPERATOR);
				sb.append(IntelEntitySummaryQuery.PART_SEPERATOR);
				sb.append(vp.asString());
				sb.append(IntelEntitySummaryQuery.PART_SEPERATOR);
				temp.setQueryString(sb.toString());
				
				valueGbDropItems.addAll( DropItemFactory.generateDropItems(vp, null) );

				query = temp;
			}else{
				
				try(Session s = HibernateManager.openSession()){
					AbstractIntelQuery temp = null;
					if (type == Type.ENTITY) {
						temp = (IntelEntitySummaryQuery)s.get(IntelEntitySummaryQuery.class, uuid);
					}else if (type == Type.RECORD) {
						temp = (IntelRecordSummaryQuery)s.get(IntelRecordSummaryQuery.class, uuid);
					}
					if (temp == null){
						Intelligence2PlugIn.displayLog(Messages.IntelQueryEditor_QueryNotfoundError, null);
						closeEditor(false);
						return Status.OK_STATUS;
					}
					temp.getNames().size();
					query = temp;
					
					try{
						SumQueryDefinition parsedQuery = null;
						if (type == Type.ENTITY) {
							parsedQuery = IntelEntitySummaryQuery.parseQuery(query.getQueryString());
						}else if (type == Type.RECORD) {
							parsedQuery = IntelRecordSummaryQuery.parseQuery(query.getQueryString());
						}
						filterDropItems.addAll(  DropItemFactory.generateDropItems(parsedQuery.getFilter(), s) );
						rowGbDropItems.addAll( DropItemFactory.generateDropItems(parsedQuery.getRowGroupByPart(), s));
						colGbDropItems.addAll( DropItemFactory.generateDropItems(parsedQuery.getColumnGroupByPart(), s));
						valueGbDropItems.addAll( DropItemFactory.generateDropItems(parsedQuery.getValuePart(), s));
					}catch(Exception ex){
						DropItem di = new ErrorDropItem(Messages.IntelQueryEditor_QueryParseError + ex.getMessage());
						valueGbDropItems.add(di);
					}
					
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(Messages.IntelQueryEditor_LoadError + ex.getMessage(), ex);
					getSite().getPage().closeEditor(IntelSummaryQueryEditor.this, false);
					return Status.OK_STATUS;
				}
			}
			
			//configure run query job
			runJob = new RunQueryJob(query) {
				@Override
				protected void onError(Exception ex) {
					cachedResults = null;
					Display.getDefault().syncExec(()->{
						((StackLayout)stackPanel.getLayout()).topControl = errorPanel;
						errorPanel.setError(Messages.IntelQueryEditor_RunError + ex.getMessage());
						stackPanel.layout(true);
					});
				}
				
				@Override
				protected void onComplete(IQueryResult results) {
					cachedResults = (SummaryQueryResult) results;
					Display.getDefault().syncExec(()->{
						SummaryResultTable summaryTable = new SummaryResultTable(resultsArea, (SummaryQueryResult) results, toolkit);
						summaryTable.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
						((StackLayout)stackPanel.getLayout()).topControl = resultsArea;
						stackPanel.layout(true);
						resultsArea.layout(true);
					});
				}
				
				@Override
				protected void onCancel(){
					cachedResults = null;
					Display.getDefault().syncExec(()->{
						((StackLayout)stackPanel.getLayout()).topControl = errorPanel;
						errorPanel.setError(Messages.IntelQueryEditor_CancelledError);
						stackPanel.layout(true);
					});
				}
			};
			runJob.setProgresPanel(progressPanel);
			
			Display.getDefault().syncExec(()->{
				isInitializing = true;
				try{
					initUiField();
					
					summaryPanel.initGroupByItems(rowGbDropItems, true);
					summaryPanel.initGroupByItems(colGbDropItems, false);
					summaryPanel.initValueItems(valueGbDropItems);
					summaryPanel.initFilterItems(filterDropItems);
					summaryPanel.initProfileFilter(query.getProfileFilter());
					
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

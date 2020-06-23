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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
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
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.RunQueryJob;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntityPerspective;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.SectionTabHeader;
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
public class IntelRecordQueryEditor extends EditorPart implements IQueryEditor{

	public static final String ID = "org.wcs.smart.i2.editor.query.record"; //$NON-NLS-1$

	private boolean isDirty = false;
	
	//injects
	private IEclipseContext context;
	private IEventBroker eventBroker;
	
	//query
	private IntelRecordQuery query;
	
	//header & date part
	private IntelQueryNameLabel header;
	private DateFilterDropDownComposite datePart;
	
	//filter panel
	private FilterDefinitionPanel dpanel;
	private ProfilesDefinitionPanel ppanel;
	private ToolItem[] runItem = new ToolItem[2];
	private ToolItem saveItem;
	private Label infoLabel;
	
	//results area
	private Composite stackPanel;
	private QueryLazyResultsTable resultsTable;
	private ProgressPanel progressPanel;
	private ErrorPanel errorPanel;
	
	private boolean isInitializing = false;
	private RunQueryJob runJob;
	private List<EventHandler> eventHandles;
	
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		String queryString = validateQuery();
		if (queryString == null){
			MessageDialog.openError(getSite().getShell(), Messages.IntelQueryEditor_ErrorDialogTitle, Messages.IntelQueryEditor_InvalidQuery);
			return;
		}
		query.setQueryString(queryString);
		query.setProfileFilter(ppanel.getQueryPart());
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
		
		IntelRecordQuery clone = new IntelRecordQuery();
		clone.setConservationArea(SmartDB.getCurrentConservationArea());
		clone.setQueryString(queryString);
		clone.setName(newName.getValue());
		clone.updateName(SmartDB.getCurrentLanguage(), clone.getName());
		clone.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), clone.getName());
		clone.setProfileFilter(ppanel.getQueryPart());
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
	}

	public void setDirty(boolean isDirty){
		this.isDirty = isDirty;
		if (saveItem != null) saveItem.setEnabled(isDirty);
		dpanel.setQueryState(isDirty);
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
		getSite().getPage().closeEditor(IntelRecordQueryEditor.this, promptSave);
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
				if (data instanceof IntelRecordQuery){
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
		
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
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
			saveItem.addListener(SWT.Selection, (event)->IntelRecordQueryEditor.this.getSite().getPage().saveEditor(IntelRecordQueryEditor.this, false));
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
		
		runItem[0] = new ToolItem(headerToolbar, SWT.PUSH);
		runItem[0].setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RUN));
		runItem[0].addListener(SWT.Selection, (event)->runQuery());
		runItem[0].setToolTipText(Messages.IntelQueryEditor_RunTooltip);
		
		createDatePart(main, toolkit);
		
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
		c = toolkit.createComposite(definitionSection);
		c.setLayout(new GridLayout());
		c.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)c.getLayout()).marginWidth = 0;
		((GridLayout)c.getLayout()).marginHeight = 0;
		((GridLayout)c.getLayout()).verticalSpacing = 0;

		Composite headerPart = toolkit.createComposite(c, SWT.NONE);
		headerPart.setLayout(new GridLayout(3, false));
		((GridLayout)headerPart.getLayout()).marginWidth = 0;
		((GridLayout)headerPart.getLayout()).marginHeight = 0;
		((GridLayout)headerPart.getLayout()).horizontalSpacing = 0;
		((GridLayout)headerPart.getLayout()).verticalSpacing = 0;
		headerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		SectionTabHeader header1 = new SectionTabHeader(new String[] {Messages.IntelRecordQueryEditor_DefinitionSection, Messages.IntelRecordQueryEditor_ProfileFilterSection}, headerPart, toolkit);
		header1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)header1.getLayout()).marginWidth = 0;
		((GridLayout)header1.getLayout()).numColumns = ((GridLayout)header1.getLayout()).numColumns + 2; 
		
		createToolbar(header1);
		
		Composite definitionStack = toolkit.createComposite(c, SWT.NONE);
		definitionStack.setLayout(new StackLayout());
		definitionStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((StackLayout)definitionStack.getLayout()).marginWidth = 0;
		((StackLayout)definitionStack.getLayout()).marginHeight = 0;
		
		Composite defComp = toolkit.createComposite(definitionStack, SWT.NONE);
		defComp.setLayout(new GridLayout());
		((GridLayout)defComp.getLayout()).marginWidth = 0;
		((GridLayout)defComp.getLayout()).marginHeight = 0;
		
		Composite pComp = toolkit.createComposite(definitionStack, SWT.NONE);
		pComp.setLayout(new GridLayout());
		((GridLayout)pComp.getLayout()).marginWidth = 0;
		((GridLayout)pComp.getLayout()).marginHeight = 0;
		
		header1.setContent(new Composite[] {defComp, pComp}, definitionStack);
		header1.selectTab(0);
		
		dpanel = new FilterDefinitionPanel(false, false){
			public void runQuery(){
				runQuery();
			}
			public void saveQuery(){
				getSite().getPage().saveEditor(IntelRecordQueryEditor.this, false);
			}
		};
		Composite definitionPanel = dpanel.createComposite(defComp);
		definitionPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		dpanel.addQueryChangedListener(()->{
			setDirty(true);
			validateQuery();
		});
		
		ppanel = new ProfilesDefinitionPanel();
		Composite profilesPanel = ppanel.createComposite(pComp);
		profilesPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	
		ppanel.addQueryChangedListener(()->{
			setDirty(true);
			validateQuery();
		});
	
		
		loadQueryJob.schedule();
	}

	private void createResultSection(Composite parent, FormToolkit toolkit){
		Composite outerStackPanel = new Composite(parent, SWT.NONE);
		outerStackPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outerStackPanel.setLayout(new StackLayout());
		
		stackPanel = new Composite(outerStackPanel, SWT.NONE);
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
		resultsTable = new QueryLazyResultsTable(stackPanel);
		errorPanel = new ErrorPanel(stackPanel);
		((StackLayout)stackPanel.getLayout()).topControl = runQueryComp;
		
		((StackLayout)outerStackPanel.getLayout()).topControl = stackPanel;
	}
	
	private void exportQuery(){
		if (resultsTable.getCurrentResults() == null){
			MessageDialog.openInformation(getSite().getShell(), Messages.IntelQueryEditor_ExportTitle, Messages.IntelQueryEditor_ExportMsg);
			return;
		}
		
		ExportQueryWizard wizard = new ExportQueryWizard(query, resultsTable.getCurrentResults());
		WizardDialog wd = new SmartWizardDialog(getSite().getShell(), wizard);
		wd.open();
	}
	
	private void runQuery(){
		
		resultsTable.setInput(null);
		((StackLayout)stackPanel.getLayout()).topControl = progressPanel;
		stackPanel.layout(true);
		
		Date[] dateFilter = null;
		if (datePart.getDateFilter() == DateFilter.CUSTOM){
			dateFilter = new Date[]{datePart.getCustomStartDate(), datePart.getCustomEndDate()};
		}else if (datePart.getDateFilter() == DateFilter.ALL){
			dateFilter = new Date[]{null, null};
		}else{
			dateFilter = new Date[]{datePart.getDateFilter().getStartDate(), datePart.getDateFilter().getEndDate()};
		}
		
		final Date[] fdateFilter = dateFilter;
		
		String queryString = dpanel.getQueryPart();
		query.setQueryString(queryString);
		query.setProfileFilter(ppanel.getQueryPart());
		resultsTable.setQuery(query);
		
		runJob.setQuery(query);
		runJob.setDateFilter(fdateFilter);
		runJob.schedule();
	}
	
	
	private String validateQuery(){
		if (isInitializing) return null; //do not valid while initializing
		try{
			String x = ppanel.validate();
			if (x != null) throw new Exception(x);
			
			String queryString = dpanel.getQueryPart();
			IntelRecordQuery.parseQuery(queryString);
			setErrorMessage(null, null);
			for(ToolItem i : runItem) i.setEnabled(true);
			
			return queryString;
		}catch (Exception ex){
			for(ToolItem i : runItem) i.setEnabled(false);
			setErrorMessage(Messages.IntelQueryEditor_InvalidQueryError, ex);
			return null;
		}
	}
	
	public void setErrorMessage(String message, Exception fullMessage){
		if (message == null) {
			infoLabel.setToolTipText(""); //$NON-NLS-1$
			infoLabel.setText(""); //$NON-NLS-1$
			infoLabel.getParent().setVisible(false);
		}else {
			infoLabel.setToolTipText(fullMessage.getMessage());
			infoLabel.setText(message);
			infoLabel.getParent().setVisible(true);
		}
	}
	@Override
	public void addDropItems(DropItem[] item){
		for (DropItem i : item){
			dpanel.addItem(i);
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
	private void createToolbar(Composite parent) {
		Composite infoPanel = new Composite(parent, SWT.NONE);
		infoPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)infoPanel.getLayoutData()).heightHint = 10;
		infoPanel.setLayout(new GridLayout(2, false));
		
		Label l = new Label(infoPanel, SWT.NONE);
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		infoLabel = new Label(infoPanel, SWT.NONE);
		infoLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		if (IntelSecurityManager.INSTANCE.canEditQuery()) {
			saveItem = new ToolItem(toolbar, SWT.PUSH);
			saveItem.setToolTipText(Messages.FilterDefinitionPanel_savetooltip);
			saveItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVE_ICON));
			saveItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					getSite().getPage().saveEditor(IntelRecordQueryEditor.this, false);
				}
			});
			
			ToolItem clear = new ToolItem(toolbar, SWT.PUSH);
			clear.setToolTipText(Messages.FilterDefinitionPanel_clearTooltip);
			clear.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CLEAR));
			clear.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					dpanel.clear();
					ppanel.clear();
					setDirty(true);
					validateQuery();
				}
			});
		}
		
		ToolItem runItem = new ToolItem(toolbar, SWT.PUSH);
		runItem.setToolTipText(Messages.FilterDefinitionPanel_runtooltip);
		runItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RUN));
		runItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				runQuery();
			}
		});
		this.runItem[1] = runItem;
		
	}
	
	private void initUiField(){
		setPartName(query.getName());
		header.setText(query.getName());
		if (query.getUuid() == null) setDirty(true);
	}
	
	@Override
	public IntelRecordQuery getQuery(){
		return this.query;
	}
	
	private Job loadQueryJob = new Job(Messages.IntelQueryEditor_loadQueryJobname){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			query = null;
			UUID uuid = ((QueryEditorInput)getEditorInput()).getUuid();
			
			List<DropItem> generatedDropItems = new ArrayList<>();
			
			if (((QueryEditorInput)getEditorInput()).isNew()){
				uuid = null;
				IntelRecordQuery temp = new IntelRecordQuery();
				temp.setName(Messages.IntelQueryEditor_defaultQueryName);
				temp.updateName(SmartDB.getCurrentLanguage(), temp.getName());
				temp.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), temp.getName());
				temp.setConservationArea(SmartDB.getCurrentConservationArea());
				
				temp.setProfileFilter(AbstractIntelQuery.convertToProfileFilter(ProfilesManager.INSTANCE.getActiveProfiles()
						.stream().filter(e->IntelSecurityManager.INSTANCE.canViewQuery(e)).collect(Collectors.toSet())));
				
				query = temp;
			}else{
				
				try(Session s = HibernateManager.openSession()){
					IntelRecordQuery temp = s.get(IntelRecordQuery.class, uuid);
					if (temp == null){
						Intelligence2PlugIn.displayLog(Messages.IntelQueryEditor_QueryNotfoundError, null);
						closeEditor(false);
						return Status.OK_STATUS;
					}
					temp.getNames().size();
					query = temp;
					
					try{
						IQueryFilter filter = IntelRecordQuery.parseQuery(query.getQueryString());
						generatedDropItems = DropItemFactory.generateDropItems(filter, s);
					}catch(Exception ex){
						ex.printStackTrace();
						DropItem di = new ErrorDropItem(Messages.IntelQueryEditor_QueryParseError + ex.getMessage());
						generatedDropItems.add(di);
					}
					
				}catch (Exception ex){
					Intelligence2PlugIn.displayLog(Messages.IntelQueryEditor_LoadError + ex.getMessage(), ex);
					getSite().getPage().closeEditor(IntelRecordQueryEditor.this, false);
					return Status.OK_STATUS;
				}
			}
			final List<DropItem> fGeneratedDropItems = generatedDropItems;
			
			//configure run query job
			runJob = new RunQueryJob(query) {
				@Override
				protected void onError(Exception ex) {
					Display.getDefault().syncExec(()->{
						if (stackPanel.isDisposed()) return;
						resultsTable.setInput(null);
						((StackLayout)stackPanel.getLayout()).topControl = errorPanel;
						errorPanel.setError(Messages.IntelQueryEditor_RunError + ex.getMessage());
						stackPanel.layout(true);
					});
				}
				
				@Override
				protected void onComplete(IQueryResult results) {
					Display.getDefault().syncExec(()->{
						if (stackPanel.isDisposed()) return;
						resultsTable.setInput((IPagedQueryResultSet) results);
						((StackLayout)stackPanel.getLayout()).topControl = resultsTable;
						stackPanel.layout(true);
					});
				}
				
				@Override
				protected void onCancel(){
					Display.getDefault().syncExec(()->{
						if (stackPanel.isDisposed()) return;
						resultsTable.setInput(null);
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
					dpanel.addItems(fGeneratedDropItems);
					ppanel.setProfileFilter(query.getProfileFilter());
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

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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
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
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.query.IQueryResult;
import org.wcs.smart.i2.query.RunQueryJob;
import org.wcs.smart.i2.query.SummaryQueryResult;
import org.wcs.smart.i2.query.observation.filter.SumQueryDefinition;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.EntityPerspective;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.SmartSection;
import org.wcs.smart.i2.ui.dialogs.query.ExportQueryWizard;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItemFactory;
import org.wcs.smart.i2.ui.views.query.dropitem.ErrorDropItem;

/**
 * Intelligence query editor for record observation query
 * 
 * @author Emily
 *
 */
public class IntelEntitySummaryQueryEditor extends EditorPart implements IQueryEditor{

	public static final String ID = "org.wcs.smart.i2.editor.query.entitysummary"; //$NON-NLS-1$

	private boolean isDirty = false;
	
	//injects
	private IEclipseContext context;
	private IEventBroker eventBroker;
	
	//query
	private IntelEntitySummaryQuery query;
	
	//header & date part
	private IntelQueryNameLabel header;
	
	//filter panel
	private SummaryDefinitionPanel summaryPanel;
	private ToolItem runItem;
	private ToolItem saveItem;
	private ToolItem wsetItem;

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
		
		InputDialog newName = new InputDialog(getSite().getShell(), Messages.IntelQueryEditor_SaveAsTitle, Messages.IntelQueryEditor_SaveAsMessage, MessageFormat.format(Messages.IntelQueryEditor_DefaultQueryName, query.getName()), new IInputValidator() {
			@Override
			public String isValid(String newText) {
				if (newText == null || newText.trim().length() == 0) return Messages.IntelQueryEditor_NameRequireError;
				return null;
			}
		});
		if (newName.open() != Window.OK){
			return;
		}
		
		IntelEntitySummaryQuery clone = new IntelEntitySummaryQuery();
		clone.setConservationArea(SmartDB.getCurrentConservationArea());
		clone.setQueryString(query.getQueryString());
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
	
	private void closeEditor(){
		getSite().getPage().closeEditor(IntelEntitySummaryQueryEditor.this, false);
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
		};
		eventBroker.subscribe(IntelEvents.QUERY_DELETED, handler);
		eventHandles.add(handler);
		
		handler = (e) -> {
			if (wsetItem != null) wsetItem.setEnabled(WorkingSetManager.INSTANCE.isSet());
		};
		
		eventHandles.add(handler);
		eventBroker.subscribe(IntelEvents.ACTIVE_WS_SET, handler);
		
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
		((GridLayout)headerComp.getLayout()).marginWidth = 0;
		((GridLayout)headerComp.getLayout()).marginHeight = 0;
		((GridData)headerComp.getLayoutData()).heightHint = 24;
		
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
			saveItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
			saveItem.addListener(SWT.Selection, (event)->IntelEntitySummaryQueryEditor.this.getSite().getPage().saveEditor(IntelEntitySummaryQueryEditor.this, false));
			saveItem.setToolTipText(Messages.IntelQueryEditor_saveTooltip);
			
			ToolItem saveAsItem = new ToolItem(headerToolbar, SWT.PUSH);
			saveAsItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVEAS_EDIT));
			saveAsItem.addListener(SWT.Selection, (event)->doSaveAs());
			saveAsItem.setToolTipText(Messages.IntelQueryEditor_saveAsTooltip);
		
			wsetItem = new ToolItem(headerToolbar, SWT.PUSH);
			wsetItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_WORKINGSET_NEW));
			wsetItem.addListener(SWT.Selection, (event)->WorkingSetManager.INSTANCE.addQueryToActiveWorkingSet(Collections.singleton(getQuery()), context));
			wsetItem.setToolTipText(Messages.IntelQueryEditor_AddWsTooltip);
			wsetItem.setEnabled(WorkingSetManager.INSTANCE.isSet());
		}
		
		ToolItem exportItem = new ToolItem(headerToolbar, SWT.PUSH);
		exportItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_EXPORT_QUERY));
		exportItem.addListener(SWT.Selection, (event)->exportQuery());
		exportItem.setToolTipText(Messages.IntelQueryEditor_ExportTooltip);
		
		runItem = new ToolItem(headerToolbar, SWT.PUSH);
		runItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RUN));
		runItem.addListener(SWT.Selection, (event)->runQuery());
		runItem.setToolTipText(Messages.IntelQueryEditor_RunTooltip);
				
		SashForm core = new SashForm(main, SWT.VERTICAL);
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite c = toolkit.createComposite(core);
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
		WizardDialog wd = new WizardDialog(getSite().getShell(), wizard);
		wd.open();
	}
	
	public void runQuery(){
		cachedResults = null;
		for (Control c : resultsArea.getChildren()) c.dispose();
		((StackLayout)stackPanel.getLayout()).topControl = progressPanel;
		stackPanel.layout(true);
		String queryString = summaryPanel.getQueryPart();
		query.setQueryString(queryString);
		runJob.schedule();
	}
	
	
	public String validateQuery(){
		if (isInitializing) return null; //do not valid while initializing
		String queryString = summaryPanel.getQueryPart();
		try{
			IntelEntitySummaryQuery.parseQuery(queryString);
			summaryPanel.setErrorMessage(null, null);
			runItem.setEnabled(true);
			
			return queryString;
		}catch (Exception ex){
			runItem.setEnabled(false);
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
		header.setText(query.getName());
		if (query.getUuid() == null) setDirty(true);
	}
	
	@Override
	public IntelEntitySummaryQuery getQuery(){
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
				IntelEntitySummaryQuery temp = new IntelEntitySummaryQuery();
				temp.setName(Messages.IntelQueryEditor_defaultQueryName);
				temp.updateName(SmartDB.getCurrentLanguage(), temp.getName());
				temp.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), temp.getName());
				temp.setConservationArea(SmartDB.getCurrentConservationArea());
				
				query = temp;
			}else{
				
				try(Session s = HibernateManager.openSession()){
					IntelEntitySummaryQuery temp = (IntelEntitySummaryQuery)s.get(IntelEntitySummaryQuery.class, uuid);
					if (temp == null){
						Intelligence2PlugIn.displayLog(Messages.IntelQueryEditor_QueryNotfoundError, null);
						closeEditor();
						return Status.OK_STATUS;
					}
					temp.getNames().size();
					query = temp;
					
					try{
						SumQueryDefinition parsedQuery = IntelEntitySummaryQuery.parseQuery(query.getQueryString());
						
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
					getSite().getPage().closeEditor(IntelEntitySummaryQueryEditor.this, false);
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

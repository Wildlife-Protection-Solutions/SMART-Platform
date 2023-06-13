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
package org.wcs.smart.asset.ui.views.stationlocation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.tool.IToolManager;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetCoreLabelProvider;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.internal.Messages;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetDeployment;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.IdFieldHeader;
import org.wcs.smart.asset.ui.handler.OpenStationHandler;
import org.wcs.smart.asset.ui.views.station.StationEditorInput;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.SectionHeader;

/**
 * Station location editor.
 * 
 * @author Emily
 *
 */
public class StationLocationEditor extends EditorPart implements MapPart {

	public static final String ID = "org.wcs.smart.asset.ui.views.stationlocation"; //$NON-NLS-1$
	
	private AssetStationLocation stationlocation;
	
	private Composite sectionBody;
	
	private Composite currentPanel;
	private Composite detailsPanel;
	private Composite historyPanel;
	private Composite dataPanel;
	
	private StationLocationCurrentPage currentPage;
	private StationLocationDetailsPage detailsPage;
	private StationLocationEventPage historyPage;
	private StationLocationDataPage dataPage;
	
	private boolean isDirty;
	
	private IEclipseContext parentContext;
	private List<EventHandler> handlers = null;
	
	private FormToolkit toolkit;
	private Form pageForm;

	private Label lblStatus;
	private Label lblStatusImage;
	
	private IdFieldHeader lblId;
	
	CoordinateReferenceSystem viewCrs;
	
	private IPartListener2 partlistener = new IPartListener2(){
        public void partActivated( IWorkbenchPartReference partRef ) {
            if (partRef.getPart(false) == StationLocationEditor.this && getMap() != ApplicationGIS.NO_MAP) {
            	if (!currentPage.getMapViewer().getControl().isDisposed()) {
            		IToolManager toolManager = ApplicationGIS.getToolManager();
            		toolManager.setCurrentEditor( StationLocationEditor.this );
            	}
            }
        }
        public void partBroughtToTop( IWorkbenchPartReference partRef ) { }
        public void partClosed( IWorkbenchPartReference partRef ) { }
        public void partDeactivated( IWorkbenchPartReference partRef ) { }
        public void partOpened( IWorkbenchPartReference partRef ) { }
        public void partHidden( IWorkbenchPartReference partRef ) { }
        public void partVisible( IWorkbenchPartReference partRef ) { }
        public void partInputChanged( IWorkbenchPartReference partRef ) { }

    };
    
	@Override
	public void doSave(IProgressMonitor monitor) {
		
		String error = detailsPage.getErrorMessage();
		if (error != null) {
			MessageDialog.openError(getSite().getShell(), Messages.StationLocationEditor_ErrorTitle, 
					MessageFormat.format(Messages.StationLocationEditor_ErrorsExist, "\n\n", error)); //$NON-NLS-1$
			return;
		}
		
		boolean isNew = stationlocation.getUuid() == null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			try {
				String query =  "SELECT count(*) FROM AssetStationLocation l where LOWER(l.id) = LOWER(:id) AND l.station.conservationArea = :ca "; //$NON-NLS-1$
				if (!isNew) {
					query += " AND uuid != :uuid"; //$NON-NLS-1$
				}
				Query<Long> q = s.createQuery(query, Long.class)
				.setParameter("id", stationlocation.getId()) //$NON-NLS-1$
				.setParameter("ca", stationlocation.getStation().getConservationArea()); //$NON-NLS-1$
				if (!isNew) {
					q.setParameter("uuid", stationlocation.getUuid()); //$NON-NLS-1$
				}
				Long cnt = q.uniqueResult();
				if (cnt > 0) {
					MessageDialog.openError(getSite().getShell(), Messages.StationLocationEditor_SaveDialogTtiel, 
						MessageFormat.format(Messages.StationLocationEditor_DuplicateIdError, stationlocation.getId())
							);
					return;
				}
				
				s.beginTransaction();
				HibernateManager.saveOrMerge(s, stationlocation);
				s.getTransaction().commit();
				
				((StationLocationEditorInput)getEditorInput()).setStationLocationUuid(stationlocation.getUuid());
				setDirty(false);
				
			}catch (Exception ex) {
				s.getTransaction().rollback();
				AssetPlugIn.displayLog(
						MessageFormat.format(Messages.StationLocationEditor_SaveError, stationlocation.getId(), ex.getMessage()), ex);
				return;
			}
		}
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put(UIEvents.EventTags.ELEMENT, parentContext.get(MPart.class));
		data.put(IEventBroker.DATA, Collections.singletonList(stationlocation));
		parentContext.get(IEventBroker.class).post(isNew ? AssetEvents.ASSETSTATIONLOCATION_NEW : AssetEvents.ASSETSTATIONLOCATION_MODIFIED, data);	
		
	}

	void openStation(AssetStation station) {
		IEclipseContext ctx = parentContext.createChild();
		ctx.set(OpenStationHandler.STATION_PARAM, new StationEditorInput(station.getUuid(), station.getId()));
		ContextInjectionFactory.invoke(new OpenStationHandler(), Execute.class, ctx);
	}
	
	@Override
	public void doSaveAs() {
		
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		super.setInput(input);
		super.setSite(site);
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
	}
	
	private void subscribeToEvent(String eventTopic, EventHandler handler){
		parentContext.get(IEventBroker.class).subscribe(eventTopic, handler);
		handlers.add(handler);
	}
	
	private void refreshStatus() {
		lblStatus.setText(stationlocation.getCachedStatus().getGuiName(Locale.getDefault()));
		lblStatusImage.setImage(AssetCoreLabelProvider.getStatusImage(stationlocation.getCachedStatus()));
		lblStatus.getParent().layout(true);
	}
	
	private void initData() {
		if (isDirty()) {
			if (!MessageDialog.openQuestion(getSite().getShell(), Messages.StationLocationEditor_RefreshDialogTitle, Messages.StationLocationEditor_RefreshConfirmMsg)) return;
		}
		refreshJob.setSystem(true);
		refreshJob.schedule();
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	private void initializeDataPage(AssetStationLocation location) {
		if (dataPage != null) dataPage.initializePanel();
	}
	
	private void initializeCurrentPage(AssetStationLocation location) {
		if (currentPage != null) currentPage.initializePanel(location);
	}
	
	private void initializeDetailsPage(AssetStationLocation location) {
		if (detailsPage != null) detailsPage.initializeAttributes(location);
	}
	
	private void initializeHistoryPage(AssetStationLocation location) {
		if (historyPage != null) historyPage.initialize(location);
	}
	
	public void setDirty(boolean isDirty) {
		boolean fire = isDirty != this.isDirty;
		this.isDirty = isDirty;
		if (fire) firePropertyChange(IEditorPart.PROP_DIRTY);
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	private void forceCloseEditor(){
		getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(StationLocationEditor.this, false);
	}
	
	@SuppressWarnings("unchecked")
	private void createEventHandlers() {
		//on delete close editor
		handlers = new ArrayList<>();
		
		subscribeToEvent(AssetEvents.ASSETDEPLOYMENT_ALL, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				boolean refresh = false;
				Collection<AssetDeployment> items = (Collection<AssetDeployment>)data;
				for (AssetDeployment loc : items){
					if (loc.getStationLocation().equals(stationlocation)) {
						refresh = true;
						break;
					}
				}
				if (refresh) validateAndRefresh();
			}
		});
		
		subscribeToEvent(AssetEvents.ASSETSTATIONLOCATION_DELETE, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				Collection<AssetStationLocation> items = (Collection<AssetStationLocation>)data;
				for (AssetStationLocation loc : items){
					if (loc.equals(stationlocation)) forceCloseEditor();
				}
			}
		});
		
		subscribeToEvent(AssetEvents.ASSETSTATION_DELETE, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				Collection<AssetStation> items = (Collection<AssetStation>)data;
				for (AssetStation loc : items){
					if (loc.equals(stationlocation.getStation())) {
						forceCloseEditor();
						return;
					}
				}
			}
		});
		
		subscribeToEvent(AssetEvents.ASSETSTATIONLOCATION_MODIFIED, (event)->{
			if (parentContext.get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)) return;

			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				boolean validate = false;
				Collection<AssetStationLocation> items = (Collection<AssetStationLocation>)data;
				for (AssetStationLocation stn : items){
					if (stn.equals(stationlocation)) {
						validate = true;
						break;
					}
				}
				if (validate) validateAndRefresh();
			}
		});
		
		subscribeToEvent(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, e->{
			initData();
		});
		
		getSite().getWorkbenchWindow().getPartService().addPartListener(partlistener);
	}
	
	private void validateAndRefresh() {
		if (isDirty) {
			if (!MessageDialog.openQuestion(getSite().getShell(), Messages.StationLocationEditor_StationModifiedDialogTitle, 
					Messages.StationLocationEditor_StationModifiedConfirm )) {
				return;
			}
		}
		//reload
		initData();
	}
	/**
	 * Gets the asset station; may return null if the station not yet loaded
	 * @return
	 */
	public AssetStationLocation getAssetStationLocation() {
		return this.stationlocation;
	}
		
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		viewCrs = HibernateManager.getCurrentViewCRS();
		
		parent.setLayout(new GridLayout());
		((GridLayout)parent.getLayout()).marginWidth= 0;
		((GridLayout)parent.getLayout()).marginHeight = 0;
		((GridLayout)parent.getLayout()).verticalSpacing= 0;
		
		pageForm = toolkit.createForm(parent);
		pageForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite body = pageForm.getBody();
		body.setLayout(new GridLayout());
		body.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite headerComp = toolkit.createComposite(body);
		SmartUiUtils.setCSSClass(headerComp, SmartUiUtils.FORM_HEADER_CLASS);
		headerComp.setLayout(new GridLayout(5, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label icon = toolkit.createLabel(headerComp, ""); //$NON-NLS-1$
		icon.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATION_LOCATION));
		
		lblId = new IdFieldHeader(headerComp, toolkit, pageForm.getFont(), pageForm.getForeground());
		lblId.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		FontData fd = lblId.getFont().getFontData()[0];
		fd.setStyle(SWT.BOLD);
		fd.setHeight(fd.getHeight() + 1);
		Font headerFont = new Font(parent.getShell().getDisplay(), fd);
		lblId.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				headerFont.dispose();	
			}
		});
		lblId.setFont(headerFont);
		lblId.addListener(SWT.Selection, e->{
			String text = e.text.trim();
			if (text.isEmpty() || text.length() > Asset.ID_MAX_LENGTH) {
				MessageDialog.openWarning(getSite().getShell(), Messages.StationLocationEditor_InvalidLocationTitle, MessageFormat.format(Messages.StationLocationEditor_InvalidLocationMsg, 1, Asset.ID_MAX_LENGTH));
				lblId.setText(stationlocation.getId());
				return;
			}
			stationlocation.setId(e.text);
			setPartName(e.text);
			setDirty(true);
		});

		lblStatusImage = toolkit.createLabel(headerComp, ""); //$NON-NLS-1$
		lblStatus = toolkit.createLabel(headerComp, ""); //$NON-NLS-1$
		
		ToolBar tbRefresh = new ToolBar(headerComp, SWT.FLAT);
		new ToolItem(tbRefresh, SWT.SEPARATOR);
		ToolItem refreshItem = new ToolItem(tbRefresh,SWT.PUSH);
		refreshItem.setToolTipText(Messages.StationLocationEditor_refreshTooltip);
		refreshItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		refreshItem.addListener(SWT.Selection, e->initData());
		
		ToolItem saveItem = new ToolItem(tbRefresh, SWT.PUSH);
		saveItem.setToolTipText(Messages.StationLocationEditor_saveTooltip);
		saveItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.SAVE_ICON));
		saveItem.setEnabled(false);
		saveItem.addListener(SWT.Selection, e->getSite().getPage().saveEditor(this, false));
		addPropertyListener(new IPropertyListener() {
			
			@Override
			public void propertyChanged(Object source, int propId) {
				if (propId == IEditorPart.PROP_DIRTY) {
					saveItem.setEnabled(isDirty);
				}
			}
		});
		
		
		String headers[] = new String[] {Messages.StationLocationEditor_StatusSectionHeader, Messages.StationLocationEditor_DataSectionHeader, Messages.StationLocationEditor_PropertiesSectionHeader, Messages.StationLocationEditor_HistorySectionHeader};
		Listener[] actions = new Listener[] {
			event->{
				if (currentPanel == null) currentPanel = createCurrentSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = currentPanel;
				sectionBody.layout(true);},
			event->{
				if (dataPanel == null) dataPanel = createDataSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = dataPanel;
				sectionBody.layout(true);},
			event->{
				if (detailsPanel == null) detailsPanel = createDetailsSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = detailsPanel;
				sectionBody.layout(true);},
			event->{
				if (historyPanel == null) historyPanel = createHistorySection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = historyPanel;
				sectionBody.layout(true);},
				
		};
		
		SectionHeader headerSection = new SectionHeader(body, SWT.NONE, headers, actions);
		headerSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sectionBody = toolkit.createComposite(body);
		sectionBody.setLayout(new StackLayout());
		sectionBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//create initial panel
		if (((StationLocationEditorInput)getEditorInput()).showProperties()) {
			headerSection.selectPanel(2);
		} else {
			headerSection.selectPanel(0);
		}
		
		createEventHandlers();
		initData();
	}
	
	private Composite createCurrentSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		currentPage = new StationLocationCurrentPage(this);
		ContextInjectionFactory.inject(currentPage, parentContext);
		currentPage.createControl(panel, toolkit);
		
		return panel;
	}

	private Composite createDetailsSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		detailsPage = new StationLocationDetailsPage(this);
		ContextInjectionFactory.inject(detailsPage, parentContext);
		detailsPage.createControl(panel, toolkit);
		
		detailsPage.initializeAttributes(stationlocation);
		return panel;
	}
	
	private Composite createHistorySection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		historyPage = new StationLocationEventPage(this);
		ContextInjectionFactory.inject(historyPage, parentContext);
		historyPage.createControl(panel, toolkit);
		
		initializeHistoryPage(stationlocation);

		return panel;
	}
	
	private Composite createDataSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		
		dataPage = new StationLocationDataPage(this);
		ContextInjectionFactory.inject(dataPage, parentContext);
		dataPage.createDataSection(panel, toolkit);
		
		return panel;
	}
	
	@Override
	public void setFocus() {
		pageForm.setFocus();
	}
	
	@Override
	public void dispose(){
		IEventBroker event = parentContext.get(IEventBroker.class);
		if (handlers != null){
			handlers.forEach((h)->event.unsubscribe(h));
		}
		this.handlers = null;
		getSite().getWorkbenchWindow().getPartService().removePartListener(partlistener);
		super.dispose();
		
		if (this.currentPage != null && this.currentPage.getMapViewer() != null) this.currentPage.getMapViewer().dispose();
		
		ApplicationGIS.getToolManager().setCurrentEditor(null);
		
		this.currentPage = null;
		this.dataPage = null;
		this.detailsPage = null;
		this.historyPage = null;
	}


	@Override
	public org.locationtech.udig.project.internal.Map getMap() {
		if (currentPage == null || currentPage.getMapViewer() == null) return ApplicationGIS.NO_MAP;
		return currentPage.getMapViewer().getMap();
	}

	@Override
	public void openContextMenu() {
		//do nothing
		return;
	}

	@Override
	public void setFont(Control textArea) {
		//do nothing
	}

	@Override
	public void setSelectionProvider(IMapEditorSelectionProvider selectionProvider) {
		//do nothing
			
	}

	@Override
	public IStatusLineManager getStatusLineManager() {
		return ((IEditorSite)getSite()).getActionBars().getStatusLineManager();
	}

	
	Job refreshJob = new Job(Messages.StationLocationEditor_refreshJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			StationLocationEditorInput in = ((StationLocationEditorInput)StationLocationEditor.this.getEditorInput());
			try(Session session = HibernateManager.openSession()){
				//load station location data from database
				if (in.getStationLocationUuid() == null) {
					//this should never happen
					throw new IllegalStateException(Messages.StationLocationEditor_StationLocationNotFound);
				}else {
					stationlocation = session.get(AssetStationLocation.class, in.getStationLocationUuid());
					if (stationlocation != null) stationlocation.getUuid().equals(null);
				}
				
				if (stationlocation == null) {
					throw new Exception(Messages.StationLocationEditor_StationLocationNotFound);
				}
				if (stationlocation.getAttributeValues() == null) stationlocation.setAttributeValues(new ArrayList<>());
				
				stationlocation.getAttributeValues().forEach(a->{
					if (a.getAttributeListItem() != null) a.getAttributeListItem().getName();
				});
				stationlocation.getStation().getId();
				stationlocation.getStation().getUuid().equals(null);
				stationlocation.getStation().computeStatus(session);
				stationlocation.computeStatus(session);
				
				Display.getDefault().syncExec(()->{
					//update uid
					lblId.setText(stationlocation.getId());
					setPartName(stationlocation.getId());

					initializeCurrentPage(stationlocation);
					initializeDetailsPage(stationlocation);
					initializeHistoryPage(stationlocation);
					initializeDataPage(stationlocation);
					
					refreshStatus();
					setDirty(false);
				});
			}catch (Exception ex) {
				AssetPlugIn.displayLog(ex.getMessage(), ex);
			}
			return Status.OK_STATUS;
		}
	};
}

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
package org.wcs.smart.asset.ui.views.station;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
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
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.IdFieldHeader;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.SectionHeader;

/**
 * Station editor 
 * @author Emily
 *
 */
public class StationEditor extends EditorPart implements MapPart {

	public static final String ID = "org.wcs.smart.asset.ui.views.station"; //$NON-NLS-1$
	
	private AssetStation station;
	
	private SectionHeader headerSection ;
	private Composite sectionBody;
	
	private Composite currentPanel;
	private Composite locationsPanel;
	private Composite detailsPanel;
	private Composite historyPanel;
	private Composite dataPanel;
	
	private StationCurrentPage currentPage;
	private StationLocationPage locationsPage;
	private StationDetailsPage detailsPage;
	private StationHistoryPage historyPage;
	private StationDataPage dataPage;
	
	private Object lastMapPage;
	
	private boolean isDirty;
	
	private IEclipseContext parentContext;
	private List<EventHandler> handlers = null;
	
	private FormToolkit toolkit;
	private Form pageForm;
	
	private IdFieldHeader lblId;
	
	private Label lblStatus;
	private Label lblStatusImage;
	
	private EventHandler promptToReset;
	
	CoordinateReferenceSystem viewCrs;
	
	private IPartListener2 partlistener = new IPartListener2(){
        public void partActivated( IWorkbenchPartReference partRef ) {
            if (partRef.getPart(false) == StationEditor.this && getMap() != ApplicationGIS.NO_MAP) {
                IToolManager toolManager = ApplicationGIS.getToolManager();
                toolManager.setCurrentEditor( StationEditor.this );
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
		boolean isNew = station.getUuid() == null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			try {
				String query =  "SELECT count(*) FROM AssetStation where LOWER(id) = LOWER(:id) AND conservationArea = :ca "; //$NON-NLS-1$
				if (!isNew) {
					query += " AND uuid != :uuid"; //$NON-NLS-1$
				}
				Query<?> q = s.createQuery(query)
				.setParameter("id", station.getId()) //$NON-NLS-1$
				.setParameter("ca", station.getConservationArea()); //$NON-NLS-1$
				if (!isNew) {
					q.setParameter("uuid", station.getUuid()); //$NON-NLS-1$
				}
				Long cnt = (Long) q.uniqueResult();
				if (cnt > 0) {
					MessageDialog.openError(getSite().getShell(), Messages.StationEditor_SaveTitle, 
						MessageFormat.format(Messages.StationEditor_DupIdError, station.getId())
							);
					return;
				}
				
				s.beginTransaction();
				s.saveOrUpdate(station);
				s.getTransaction().commit();
				
				((StationEditorInput)getEditorInput()).setStationUuid(station.getUuid());
				setDirty(false);
				
			}catch (Exception ex) {
				s.getTransaction().rollback();
				AssetPlugIn.displayLog(
						MessageFormat.format(Messages.StationEditor_SaveError, station.getId(), ex.getMessage()), ex);
				return;
			}
		}
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put(UIEvents.EventTags.ELEMENT, parentContext.get(MPart.class));
		data.put(IEventBroker.DATA, Collections.singletonList(station));
		parentContext.get(IEventBroker.class).post(isNew ? AssetEvents.ASSETSTATION_NEW : AssetEvents.ASSETSTATION_MODIFIED, data);	
	}

	@Override
	public void doSaveAs() {
		
	}
	
	private void refreshStatus() {
		lblStatus.setText(station.getCachedStatus().getGuiName(Locale.getDefault()));
		lblStatusImage.setImage(AssetCoreLabelProvider.getStatusImage(station.getCachedStatus()));
		lblStatus.getParent().layout(true);
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
	
	private void initData() {
		if (isDirty()) {
			if (!MessageDialog.openQuestion(getSite().getShell(), Messages.StationEditor_RefreshTitle, Messages.StationEditor_RefreshMsg)) return;
		}
		
		refreshJob.setSystem(true);
		refreshJob.schedule();
	}

	public void findAndShow(UUID waypointUuid) {
		if (waypointUuid == null) return;
		headerSection.selectPanel(1);
		dataPage.scrollTo(waypointUuid);
	}
	
	
	@Override
	public boolean isDirty() {
		return isDirty;
	}
	
	private void initializeDataPage(AssetStation station) {
		if (dataPage != null) dataPage.initializePanel();
	}
	
	private void initializeCurrentPage(AssetStation station) {
		if (currentPage != null) currentPage.initializePanel(station);
	}
	
	private void initializeDetailsPage(AssetStation station) {
		if (detailsPage != null) detailsPage.initializeAttributes(station);
	}
	
	private void initializeHistoryPage(AssetStation station) {
		if (historyPage != null) historyPage.initialize(station);
	}
	
	private void initializeLocationsPage(AssetStation station) {
		if (locationsPage != null) locationsPage.initialize(station);
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
		getEditorSite().getWorkbenchWindow().getActivePage().closeEditor(StationEditor.this, false);
	}
	
	@SuppressWarnings("unchecked")
	private void createEventHandlers() {
		//on delete close editor
		handlers = new ArrayList<>();
		
		subscribeToEvent(AssetEvents.ASSETSTATIONLOCATION_ALL, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				boolean validate = false;
				Collection<AssetStationLocation> items = (Collection<AssetStationLocation>)data;
				for (AssetStationLocation stn : items){
					if (stn.getStation().equals(station)) {
						validate = true;
						break;
					}
				}
				if (validate) validateAndRefresh();
			}
		});
		
		
		subscribeToEvent(AssetEvents.ASSETSTATION_DELETE, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				Collection<AssetStation> items = (Collection<AssetStation>)data;
				for (AssetStation stn : items){
					if (stn.equals(station)) forceCloseEditor();
				}
			}
		});
		
		subscribeToEvent(AssetEvents.ASSETSTATION_MODIFIED, (event)->{
			if (parentContext.get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)) return;

			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				boolean validate = false;
				Collection<AssetStation> items = (Collection<AssetStation>)data;
				for (AssetStation stn : items){
					if (stn.equals(station)) {
						validate = true;
						break;
					}
				}
				
				if (validate) validateAndRefresh();
			}
		});
		
		subscribeToEvent(AssetEvents.ASSETDATA, (event)->{
			if (parentContext.get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)) return;
			//refresh data page
			initData();
		});
		
		promptToReset = new EventHandler(){
			@Override
			public void handleEvent(org.osgi.service.event.Event event) {
				if (parentContext.get(MPart.class) == event.getProperty(UIEvents.EventTags.ELEMENT)){
					parentContext.get(IEventBroker.class).unsubscribe(this);
					
					if (MessageDialog.openQuestion(getSite().getShell(), Messages.StationEditor_StnModifiedTitle, 
							MessageFormat.format(Messages.StationEditor_StnModifiedMsg, station.getId()) )) {
						initData();
					}
				}
			}
		};
		handlers.add(promptToReset);
		
		subscribeToEvent(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, e->{
			if (isDirty) {
				parentContext.get(IEventBroker.class).subscribe(UIEvents.UILifeCycle.BRINGTOTOP, promptToReset);
			}else {
				initData();	
			}
			
		});
		
		getSite().getWorkbenchWindow().getPartService().addPartListener(partlistener);
	}
	
	private void validateAndRefresh() {
		if (isDirty) {
			if (parentContext.get(MPart.class).isOnTop()){
				if (MessageDialog.openQuestion(getSite().getShell(), Messages.StationEditor_StnModifiedTitle, 
					MessageFormat.format(Messages.StationEditor_StnModifiedMsg, station.getId()) )) {
					initData();
				}
			}else {
				parentContext.get(IEventBroker.class).subscribe(UIEvents.UILifeCycle.BRINGTOTOP, promptToReset);
			}
		}else {
			//reload
			initData();
		}
	}
	/**
	 * Gets the asset station; may return null if the station not yet loaded
	 * @return
	 */
	public AssetStation getAssetStation() {
		return this.station;
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
		WidgetElement.setCSSClass(headerComp, "SMARTFormHeader");  //$NON-NLS-1$
		headerComp.setLayout(new GridLayout(5, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label icon = toolkit.createLabel(headerComp, ""); //$NON-NLS-1$
		icon.setImage(AssetPlugIn.getDefault().getImageRegistry().get(AssetPlugIn.ICON_STATION));
		
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
				MessageDialog.openWarning(getSite().getShell(), Messages.StationEditor_IdDialogTitle, MessageFormat.format(Messages.StationEditor_IdErrorMsg, 1, Asset.ID_MAX_LENGTH));
				lblId.setText(station.getId());
				return;
			}
			station.setId(e.text);
			setPartName(e.text);
			setDirty(true);
		});

		lblStatusImage = toolkit.createLabel(headerComp, ""); //$NON-NLS-1$
		lblStatus = toolkit.createLabel(headerComp, ""); //$NON-NLS-1$
		
		ToolBar tbRefresh = new ToolBar(headerComp, SWT.FLAT);
		new ToolItem(tbRefresh, SWT.SEPARATOR);
		
		ToolItem refreshItem = new ToolItem(tbRefresh,SWT.PUSH);
		refreshItem.setToolTipText(Messages.StationEditor_refreshTooltip);
		refreshItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		refreshItem.addListener(SWT.Selection, e->initData());
		
		ToolItem saveItem = new ToolItem(tbRefresh, SWT.PUSH);
		saveItem.setToolTipText(Messages.StationEditor_saveTooltip);
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
		
		String headers[] = new String[] {Messages.StationEditor_CurrentStatusSection, Messages.StationEditor_DataSection, Messages.StationEditor_LocationsSection, Messages.StationEditor_PropertiesSection, Messages.StationEditor_AssetsSection};
		Listener[] actions = new Listener[] {
			event->{
				if (currentPanel == null) currentPanel = createCurrentSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = currentPanel;
				lastMapPage = currentPanel;
				if (currentPage.getMapViewer() == null) {
					ApplicationGIS.getToolManager().setCurrentEditor(null);
				}else {
					//force refresh of map editor so tools work
					ApplicationGIS.getToolManager().setCurrentEditor(null);
					ApplicationGIS.getToolManager().setCurrentEditor(this);
				}
				sectionBody.layout(true);},
			event->{
				if (dataPanel == null) dataPanel = createDataSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = dataPanel;
				sectionBody.layout(true);},
			event->{
				if (locationsPanel == null) locationsPanel = createLocationsSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = locationsPanel;
				lastMapPage = locationsPanel;
				//force refresh of map editor so tools work
				ApplicationGIS.getToolManager().setCurrentEditor(null);
				ApplicationGIS.getToolManager().setCurrentEditor(this);
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
		
		headerSection = new SectionHeader(body, SWT.NONE, headers, actions);
		headerSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sectionBody = toolkit.createComposite(body);
		sectionBody.setLayout(new StackLayout());
		sectionBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//create initial panel
		if ( ((StationEditorInput)getEditorInput()).showProperties() ) {
			headerSection.selectPanel(3);
		}else {
			headerSection.selectPanel(0);
		}
		
		createEventHandlers();
		initData();
	}
	
	private Composite createLocationsSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		locationsPage = new StationLocationPage(this);
		ContextInjectionFactory.inject(locationsPage, parentContext);
		locationsPage.createControl(panel,  toolkit);
		
		initializeLocationsPage(station);
		return panel;
		
	}
	private Composite createCurrentSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		currentPage = new StationCurrentPage(this);
		ContextInjectionFactory.inject(currentPage, parentContext);
		currentPage.createControl(panel, toolkit);
		
		return panel;
	}

	private Composite createDataSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		
		dataPage = new StationDataPage(this);
		ContextInjectionFactory.inject(dataPage, parentContext);
		dataPage.createDataSection(panel, toolkit);
		
		return panel;
	}
	
	private Composite createDetailsSection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		detailsPage = new StationDetailsPage(this);
		ContextInjectionFactory.inject(detailsPage, parentContext);
		detailsPage.createControl(panel, toolkit);
		
		detailsPage.initializeAttributes(station);
		return panel;
	}
	
	private Composite createHistorySection(Composite parent) {
		Composite panel = toolkit.createComposite(parent);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((GridLayout)panel.getLayout()).marginWidth = 0;
		((GridLayout)panel.getLayout()).marginHeight = 0;
		
		historyPage = new StationHistoryPage(this);
		ContextInjectionFactory.inject(historyPage, parentContext);
		historyPage.createControl(panel, toolkit);
		
		initializeHistoryPage(station);

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
		if (lastMapPage == locationsPanel) {
			if (locationsPage == null || locationsPage.getMapViewer() == null) return ApplicationGIS.NO_MAP;
			return locationsPage.getMapViewer().getMap();
		}else if (lastMapPage == currentPanel) {
			if (currentPage == null || currentPage.getMapViewer() == null) return ApplicationGIS.NO_MAP;
			return currentPage.getMapViewer().getMap();
		}
		return ApplicationGIS.NO_MAP;
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

	private Job refreshJob = new Job(Messages.StationEditor_refreshJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			StationEditorInput in = (StationEditorInput) StationEditor.this.getEditorInput();
			
			try(Session session = HibernateManager.openSession()){
				//load data
				if (in.getStationUuid() == null) {
					throw new IllegalStateException(Messages.StationEditor_StationNotFound);
				}
				station = session.get(AssetStation.class, in.getStationUuid());
				if (station == null) {
					throw new Exception(Messages.StationEditor_StationNotFound);
				}
				//lazy load uuid
				station.getUuid().equals(null);
				station.computeStatus(session);
				if (station.getAttributeValues() == null) {
					station.setAttributeValues(new ArrayList<>());
				}else {
					station.getAttributeValues().forEach(a->{
						if (a.getAttributeListItem() != null) a.getAttributeListItem().getName();
					});
				}
				
				if (station.getLocations() == null) station.setLocations(new ArrayList<>());
				station.getLocations().forEach(loc->{
					loc.getId();
					if (loc.getAttributeValues() != null) loc.getAttributeValues().forEach(att->{
						att.getAttribute().getName();
						if (att.getAttributeListItem() != null) att.getAttributeListItem().getName();
					});
				});
				
				//update ui
				Display.getDefault().syncExec(()->{
					if (!lblId.isDisposed()) lblId.setText(station.getId());	
					
					setPartName(station.getId());
					
					initializeCurrentPage(station);
					initializeDetailsPage(station);
					initializeHistoryPage(station);
					initializeLocationsPage(station);
					initializeDataPage(station);
					
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


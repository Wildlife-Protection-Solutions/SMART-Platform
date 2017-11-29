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

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
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
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.hibernate.query.Query;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.asset.AssetCoreLabelProvider;
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.IdFieldHeader;
import org.wcs.smart.asset.ui.SectionHeader;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Station editor 
 * @author Emily
 *
 */
public class StationEditor extends EditorPart implements MapPart {

	public static final String ID = "org.wcs.smart.asset.ui.views.station"; //$NON-NLS-1$
	
	private AssetStation station;
	
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
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		boolean isNew = station.getUuid() == null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			try {
				String query =  "SELECT count(*) FROM AssetStation where LOWER(id) = :id AND conservationArea = :ca ";
				if (!isNew) {
					query += " AND uuid != :uuid";
				}
				Query<?> q = s.createQuery(query)
				.setParameter("id", station.getId().toLowerCase())
				.setParameter("ca", station.getConservationArea());
				if (!isNew) {
					q.setParameter("uuid", station.getUuid());
				}
				Long cnt = (Long) q.uniqueResult();
				if (cnt > 0) {
					MessageDialog.openError(getSite().getShell(), "Save Station", 
						MessageFormat.format("The id ''{0}'' is already used by another station in the system. You cannot duplicate Station IDs.  Change the station id and try again.", station.getId())
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
						MessageFormat.format("Unable to save changes to station: {0}. {1}", station.getId(), ex.getMessage()), ex);
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
		lblStatus.setText(station.getStatus().getGuiName(Locale.getDefault()));
		lblStatusImage.setImage(AssetCoreLabelProvider.getStatusImage(station.getStatus()));
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
			if (!MessageDialog.openQuestion(getSite().getShell(), "Refresh", "This station has unsaved changes.  By refreshing this page, these changes will be lost.  Are you sure you want to continue?")) return;
		}
		
		refreshJob.setSystem(true);
		refreshJob.schedule();
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
	}
	
	private void validateAndRefresh() {
		if (isDirty) {
			if (!MessageDialog.openQuestion(getSite().getShell(), "Station Modified", 
					"This station was modified by another part of the system.  Do you want to reload the page and loose any local changes?  By not reloading your risk overwriting other changes made outside this page." )) {
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
	public AssetStation getAssetStation() {
		return this.station;
	}
		
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
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
		headerComp.setLayout(new GridLayout(5, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerComp.getLayout()).marginWidth = 0;
		((GridLayout)headerComp.getLayout()).marginHeight = 0;
		
		Label icon = toolkit.createLabel(headerComp, "");
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
				MessageDialog.openWarning(getSite().getShell(), "Station ID", MessageFormat.format("Invalid station id.  ID must be between {0} and {1} charaters", 1, Asset.ID_MAX_LENGTH));
				lblId.setText(station.getId());
				return;
			}
			station.setId(e.text);
			setPartName(e.text);
			setDirty(true);
		});

		lblStatusImage = toolkit.createLabel(headerComp, "");
		lblStatus = toolkit.createLabel(headerComp, "");
		
		ToolBar tbRefresh = new ToolBar(headerComp, SWT.FLAT);
		new ToolItem(tbRefresh, SWT.SEPARATOR);
		
		ToolItem refreshItem = new ToolItem(tbRefresh,SWT.PUSH);
		refreshItem.setToolTipText("Refresh station location");
		refreshItem.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.REFRESH_ICON));
		refreshItem.addListener(SWT.Selection, e->initData());
		
		ToolItem saveItem = new ToolItem(tbRefresh, SWT.PUSH);
		saveItem.setToolTipText("Save changes");
		saveItem.setImage(getSite().getWorkbenchWindow().getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
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
		
		String headers[] = new String[] {"Current Status", "Data", "Station Locations", "Properties", "History"};
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
		
		SectionHeader headerSection = new SectionHeader(body, SWT.NONE, headers, actions, toolkit);
		headerSection.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		sectionBody = toolkit.createComposite(body);
		sectionBody.setLayout(new StackLayout());
		sectionBody.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		//create initial panel
		headerSection.selectPanel(0);
		
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
		super.dispose();
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

	private Job refreshJob = new Job("refresh station details") {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			StationEditorInput in = (StationEditorInput) StationEditor.this.getEditorInput();
			
			try(Session session = HibernateManager.openSession()){
				//load data
				if (in.getStationUuid() == null) {
					new IllegalStateException("Station does not exist.");
				}else {
					station = session.get(AssetStation.class, in.getStationUuid());
					station.equals(null);
				}
				
				if (station == null) {
					throw new Exception("Station not found; could not initialize element controls");
				}
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
					lblId.setText(station.getId());
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


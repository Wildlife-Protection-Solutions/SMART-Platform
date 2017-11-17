package org.wcs.smart.asset.ui.views.stationlocation;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
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
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
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
import org.wcs.smart.asset.AssetEvents;
import org.wcs.smart.asset.AssetPlugIn;
import org.wcs.smart.asset.model.Asset;
import org.wcs.smart.asset.model.AssetStation;
import org.wcs.smart.asset.model.AssetStationLocation;
import org.wcs.smart.asset.ui.IdFieldHeader;
import org.wcs.smart.asset.ui.SectionHeader;
import org.wcs.smart.common.attachment.AttachmentInterceptor;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;

public class StationLocationEditor extends EditorPart implements MapPart {

	public static final String ID = "org.wcs.smart.asset.ui.views.stationlocation"; //$NON-NLS-1$
	
	private AssetStationLocation stationlocation;
	
	private Composite sectionBody;
	
	private Composite currentPanel;
	private Composite detailsPanel;
	private Composite historyPanel;
	
	private StationLocationCurrentPage currentPage;
	private StationLocationDetailsPage detailsPage;
	private StationLocationEventPage historyPage;
		
	private boolean isDirty;
	
	private IEclipseContext parentContext;
	private List<EventHandler> handlers = null;
	
	private FormToolkit toolkit;
	private Form pageForm;

	private Label lblStatus;
	private Label lblStatusImage;
	
	private IdFieldHeader lblId;
	
	
	@Override
	public void doSave(IProgressMonitor monitor) {
		boolean isNew = stationlocation.getUuid() == null;
		try(Session s = HibernateManager.openSession(new AttachmentInterceptor())){
			try {
				String query =  "SELECT count(*) FROM AssetStationLocation l where LOWER(l.id) = :id AND l.station.conservationArea = :ca ";
				if (!isNew) {
					query += " AND uuid != :uuid";
				}
				Query<?> q = s.createQuery(query)
				.setParameter("id", stationlocation.getId().toLowerCase())
				.setParameter("ca", stationlocation.getStation().getConservationArea());
				if (!isNew) {
					q.setParameter("uuid", stationlocation.getUuid());
				}
				Long cnt = (Long) q.uniqueResult();
				if (cnt > 0) {
					MessageDialog.openError(getSite().getShell(), "Save Station Location", 
						MessageFormat.format("The id ''{0}'' is already used by another station location in the system. You cannot duplicate Station Location IDs.  Change the station location id and try again.", stationlocation.getId())
							);
					return;
				}
				
				s.beginTransaction();
				
				if (historyPage != null)historyPage.doSave(s);
				
				s.saveOrUpdate(stationlocation);
				s.getTransaction().commit();
				
				if (historyPage != null)historyPage.afterSaveComplete();
				((StationLocationEditorInput)getEditorInput()).setStationLocationUuid(stationlocation.getUuid());
				setDirty(false);
				
			}catch (Exception ex) {
				s.getTransaction().rollback();
				AssetPlugIn.displayLog(
						MessageFormat.format("Unable to save changes to station location: {0}. {1}", stationlocation.getId(), ex.getMessage()), ex);
				return;
			}
		}
		HashMap<String, Object> data = new HashMap<String, Object>();
		data.put(UIEvents.EventTags.ELEMENT, parentContext.get(MPart.class));
		data.put(IEventBroker.DATA, Collections.singletonList(stationlocation));
		parentContext.get(IEventBroker.class).post(isNew ? AssetEvents.ASSETSTATIONLOCATION_NEW : AssetEvents.ASSETSTATIONLOCATION_MODIFIED, data);	
		
//		if (currentPage != null) currentPage.refresh();
//		if (detailsPage != null) detailsPage.refresh();
//		if (historyPage != null) historyPage.refresh();
//		if (detailsPage != null) detailsPage.refresh();
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
	
	private void initData() {
		StationLocationEditorInput in = (StationLocationEditorInput) super.getEditorInput();
		
		try(Session session = HibernateManager.openSession()){
			if (in.getStationLocationUuid() == null) {
				//this should probably never happy
				if (in.getStationLocationUuid() != null) {
					stationlocation = new AssetStationLocation();
					//TODO: find station
//					stationlocation.setConservationArea(SmartDB.getCurrentConservationArea());
					
					stationlocation.setAttributeValues(new ArrayList<>());
					stationlocation.setId("Station Location ID");
					
					setDirty(true);
				}
			}else {
				stationlocation = session.get(AssetStationLocation.class, in.getStationLocationUuid());
				if (stationlocation != null) stationlocation.equals(null);
			}
			
			if (stationlocation == null) {
				throw new Exception("Station Location not found; could not initialize element controls");
			}
			if (stationlocation.getAttributeValues() == null) {
				stationlocation.setAttributeValues(new ArrayList<>());
			}else {
				stationlocation.getAttributeValues().forEach(a->{
					if (a.getAttributeListItem() != null) a.getAttributeListItem().getName();
				});
			}
			
			stationlocation.getStation().getId();
			stationlocation.getStation().getUuid().equals(null);
			
			lblId.setText(stationlocation.getId());
			setPartName(stationlocation.getId());
			//setTitleImage(img);
			
			initializeCurrentPage(stationlocation);
			initializeDetailsPage(stationlocation);
			initializeHistoryPage(stationlocation);
			
				
		}catch (Exception ex) {
			AssetPlugIn.displayLog(ex.getMessage(), ex);
		}
		
	}

	@Override
	public boolean isDirty() {
		return isDirty;
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
	
	private void createEventHandlers() {
		//on delete close editor
		handlers = new ArrayList<>();
		
		subscribeToEvent(AssetEvents.ASSETSTATIONLOCATION_DELETE, (event)->{
			Object data = event.getProperty(IEventBroker.DATA);
			if (data != null){
				Collection<AssetStationLocation> items = (Collection<AssetStationLocation>)data;
				for (AssetStationLocation loc : items){
					if (loc.equals(stationlocation)) forceCloseEditor();
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
				
				if (validate) {
					if (isDirty) {
						if (!MessageDialog.openQuestion(getSite().getShell(), "Station Location Modified", 
								"This station location was modified by another part of the system.  Do you want to reload the page and loose any local changes?  By not reloading your risk overwriting other changes made outside this page." )) {
							return;
						}
					}
					//reload
					initData();
				}
			}
		});
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
		headerComp.setLayout(new GridLayout(1, false));
		headerComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)headerComp.getLayout()).marginWidth = 0;
		((GridLayout)headerComp.getLayout()).marginHeight = 0;
		
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
				MessageDialog.openWarning(getSite().getShell(), "Station Location ID", MessageFormat.format("Invalid station location id.  ID must be between {0} and {1} charaters", 1, Asset.ID_MAX_LENGTH));
				lblId.setText(stationlocation.getId());
				return;
			}
			stationlocation.setId(e.text);
			setPartName(e.text);
			setDirty(true);
		});

		String headers[] = new String[] {"Current Status", "Properties", "History"};
		Listener[] actions = new Listener[] {
			event->{
				if (currentPanel == null) currentPanel = createCurrentSection(sectionBody);
				((StackLayout)sectionBody.getLayout()).topControl = currentPanel;
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

}

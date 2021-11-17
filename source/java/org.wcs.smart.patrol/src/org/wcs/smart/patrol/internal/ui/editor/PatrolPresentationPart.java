/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.patrol.internal.ui.editor;

import java.io.IOException;
import java.text.Collator;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.IStyleBlackboard;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.ProjectPackage;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.render.SelectionStyleContent;
import org.locationtech.udig.project.internal.render.impl.RenderManagerImpl;
import org.locationtech.udig.project.render.IViewportModel;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.ui.WaypointInfoShellProvider;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.geotools.PatrolDataSource;
import org.wcs.smart.patrol.geotools.PatrolFeatureSource;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.udig.catalog.PatrolGeoResource;
import org.wcs.smart.patrol.udig.catalog.PatrolService;
import org.wcs.smart.patrol.udig.catalog.StyleUtils;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;
import org.wcs.smart.patrol.ui.IPatrolPresentationContribution;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.ui.map.tool.IInfoToolShellProvider;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.UuidUtils;

import com.ibm.icu.text.MessageFormat;

/**
 * Page for the editor for displaying a map
 * of the waypoints and tracks.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolPresentationPart extends SmartMapEditorPart {
	
	public static final String ID = "org.wcs.smart.patrol.ui.PatrolPresentationPart"; //$NON-NLS-1$
	
	private PatrolEditor parentEditor; 
	
	private PatrolService patrolService = null;
	private LoadDefaultLayersJob loadDefaultLayers;
		
	private Composite patrolSummary;
	private Composite patrolData;
	private List<IPatrolPresentationContribution> contributions;
	
	private Composite rightStack;
	private PresentationHeader header1 ;
	private PatrolPresentationImageViewer imageViewer;
	
	private TableViewer tblData;
	private HashMap<LocalDate, List<Object>> pdata;
	private Projection viewProjection;
	
	private Layer waypointLayer = null;
	
	private Job addLayerJob = new Job(Messages.PatrolMapPageEditor_AddLayersJobName) {
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			patrolService = new PatrolService(parentEditor.getPatrol());
	    	try {
	    		List<IGeoResource> layers = (List<IGeoResource>) patrolService.resources(monitor);
	    		
	    		List<IGeoResource> sortedLayers = new ArrayList<>();
	    		for (IGeoResource l : layers) if (((PatrolGeoResource)l).getType().equals(PatrolDataSource.TRACK_PART_TYPE)) sortedLayers.add(l);
	    		for (IGeoResource l : layers) if (((PatrolGeoResource)l).getType().equals(PatrolDataSource.WAYPOINT_PRJ_TYPE)) sortedLayers.add(l);
	    		for (IGeoResource l : layers) if (((PatrolGeoResource)l).getType().equals(PatrolDataSource.WAYPOINT_TYPE)) sortedLayers.add(l);
	    		
	    		AddLayersCommand command = new AddLayersCommand(sortedLayers, getMap().getLayersInternal().size()) {
	    			public void run( IProgressMonitor monitor ) throws Exception {
	    				
	    				((RenderManagerImpl)getMap().getRenderManagerInternal()).disableRendering();
	    				
	    				super.run(monitor);
	    				
	    				for (Layer l : getLayers()) {
	    					
	    					Style s = l.getGeoResource().resolve(Style.class, monitor);
	    					if (s != null) l.getStyleBlackboard().put(SLDContent.ID, s);			
	    					PatrolFeatureSource fs = l.getGeoResource().resolve(PatrolFeatureSource.class, monitor);
	    					if (fs != null) {
	    						l.setName(fs.getLayerName());
	    						l.setVisible(fs.getDefaultVisibility());
	    						l.eNotify(new ENotificationImpl(
	    								(InternalEObject) l, Notification.SET,
	    								ProjectPackage.LAYER__VISIBLE, false, l.isVisible()));	
	    					}
	    					
	    					if (l.getGeoResource().canResolve(PatrolGeoResource.class)) {
	    						if (((PatrolGeoResource)l.getGeoResource().resolve(PatrolGeoResource.class, new NullProgressMonitor())).getType().equals(PatrolDataSource.TRACK_PART_TYPE)) {
	    							IStyleBlackboard bb = StyleUtils.INSTANCE.getPatrolTrackStyle(parentEditor.getPatrol());
	    							l.setStyleBlackboard((StyleBlackboard) bb);
	    						}else if (((PatrolGeoResource)l.getGeoResource().resolve(PatrolGeoResource.class, new NullProgressMonitor())).getType().equals(PatrolDataSource.WAYPOINT_TYPE)) {
	    							waypointLayer = l;
	    							waypointLayer.getStyleBlackboard().put(SelectionStyleContent.ID, 
	    									StyleUtils.INSTANCE.getPointSelectionStyle(waypointLayer.getSchema()));
	    						}
	    					 }
	    				}
	    				
	    				((RenderManagerImpl)getMap().getRenderManagerInternal()).enableRendering();
	    				getMap().getRenderManager().refresh(null);
	    			}
					
	    		};
	    		getMap().sendCommandASync(command);
    		
	    		addInitialZoomFunction();
				
			} catch (IOException e) {
				return new Status(IStatus.ERROR, Messages.PatrolMapPageEditor_UnknownError, IStatus.ERROR, Messages.PatrolMapPageEditor_Error_LoadingMapPage, e);
			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job(Messages.PatrolMapPageEditor_RefreshPatrolLayers_Job){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (patrolService != null){
				try {
					patrolService.refresh(parentEditor.getPatrol(), null);
				} catch (IOException e) {
					SmartPatrolPlugIn.log(Messages.PatrolMapPageEditor_Error_RefreshingLayers, e);
				}
			}
			//clear selection
			mapViewer.getRenderManager().refresh(null);
			return Status.OK_STATUS;
		}
    };
    
    /** 
     * Listener for patrol events
     * 
     */
    private IPatrolEventListener patrolUpdatedListeners = new IPatrolEventListener() {
		@Override
		public void eventFired(int attributeChanged, Object source) {
			Patrol p = null;
			if (source instanceof Patrol){
				p = (Patrol) p;
			}else if (source instanceof PatrolLegDay){
				p = ((PatrolLegDay)source).getPatrolLeg().getPatrol();
			}
			if (p != null && p.equals(parentEditor.getPatrol()) && (
					attributeChanged == PatrolEventManager.PATROL_DATES_LEG ||
					attributeChanged == PatrolEventManager.PATROL_TRACKS ||
					attributeChanged == PatrolEventManager.PATROL_WAYPOINTS)){
				refresh();
			}
		}
	};
	    
	public PatrolPresentationPart(PatrolEditor parent){
		this.parentEditor = parent;
		
		List<String> tools = new ArrayList<String>();
		for (String tool : MapToolComposite.DEFAULT_MAP_TOOLS){
			tools.add(tool);
		}
		this.mapTools = tools.toArray(new String[tools.size()]);
	}
	
	public MultiPageEditorPart getParentEditor() {
		return this.parentEditor;
	}

	/**
	 * refresh the map and track layers
	 */
	public void refresh() {
    	if (refreshJob != null) {
        	refreshJob.cancel();
        	refreshJob.schedule();
    	}
	}

	private void addLayers(){
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap()) {
			protected IStatus run(IProgressMonitor monitor) {
				IStatus r = super.run(monitor);
				if (addLayerJob != null) addLayerJob.schedule();
				return r;
			}
		};
		loadDefaultLayers.schedule();
	}
	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof PatrolEditorInput)){
			throw new RuntimeException("Invalid editor input."); //$NON-NLS-1$
		}
		super.init(site, input);
		
		
	}

	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		
		contributions = findContributions();
		
		SashForm sashLeftRight = new SashForm(parent, SWT.HORIZONTAL);
		
		Composite left = new Composite(sashLeftRight, SWT.BORDER);
		left.setLayout(new GridLayout());
		((GridLayout)left.getLayout()).marginWidth = 0;
		((GridLayout)left.getLayout()).marginHeight = 0;
		
		super.createPartControl(left);
        
		Composite right = new Composite(sashLeftRight, SWT.BORDER);
		right.setLayout(new GridLayout());
		((GridLayout)right.getLayout()).marginWidth = 0;
		((GridLayout)right.getLayout()).marginHeight = 0;
		((GridLayout)right.getLayout()).verticalSpacing = 0;
		
		Composite rightHeader = new Composite(right, SWT.NONE);
		rightHeader.setLayout(new GridLayout());
		rightHeader.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)rightHeader.getLayout()).marginWidth = 0;
		((GridLayout)rightHeader.getLayout()).marginHeight = 0;
		
		createHeader(rightHeader);
		
		rightStack = new Composite(right, SWT.NONE);
		rightStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		rightStack.setLayout(new StackLayout());
		
		patrolSummary = new Composite(rightStack, SWT.NONE);
		patrolSummary.setLayout(new GridLayout());

		patrolData = new Composite(rightStack, SWT.NONE);
		patrolData.setLayout(new GridLayout());
		((GridLayout)patrolData.getLayout()).marginWidth = 0;
		((GridLayout)patrolData.getLayout()).marginHeight = 0;
		createPatrolData(patrolData);
		
		((StackLayout)rightStack.getLayout()).topControl = patrolSummary;
		
		//sashLeftRight.setWeights(new int[] {1,1});
		
		sashLeftRight.setWeights(50, 50);
		
		addLayers();
        PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, patrolUpdatedListeners);
       
        
        getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, getMapInfoProvider());
        getMap().getBlackboard().put(IInfoToolShellProvider.BLACKBOARD_KEY, getInfoShellProvider());
        
        getMap().setName("PRESENTATION MAP");
        initData();
        
	}
	

	private void createPatrolData(Composite parent) {
		
		SashForm sashUpDown = new SashForm(parent, SWT.VERTICAL);
		sashUpDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite dataSection = new Composite(sashUpDown, SWT.NONE);
		dataSection.setLayout(new GridLayout());
		((GridLayout)dataSection.getLayout()).marginWidth = 0;
		((GridLayout)dataSection.getLayout()).marginHeight = 0;
		
		tblData = new TableViewer(dataSection, SWT.FULL_SELECTION);
		tblData.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblData.setContentProvider(ArrayContentProvider.getInstance());
		tblData.getTable().setHeaderVisible(true);
		tblData.addSelectionChangedListener(e->{
			Object x = tblData.getStructuredSelection().getFirstElement();
			if (x instanceof Waypoint) {
				imageViewer.setSource(x);
			}else if (x instanceof WaypointObservation) {
				imageViewer.setSource(x);
			}else {
				imageViewer.setSource(null);
			}
		});
		
		for (Column c : Column.values()) {
			TableViewerColumn wpIdColumn = new TableViewerColumn(tblData, SWT.NONE);
			wpIdColumn.getColumn().setText(c.getName());
			wpIdColumn.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					return c.getValue(element, viewProjection);
				}
			});
			wpIdColumn.getColumn().setWidth( c.getSize() );
		}
		
		int catcnt = 0;
		try(Session session = HibernateManager.openSession()){
			Number v = (Number)session.createNativeQuery("SELECT max(smart.hkeylength(c.hkey)) FROM smart.dm_category c WHERE c.ca_uuid = :ca")
					.setParameter("ca", parentEditor.getPatrol().getConservationArea())
					.uniqueResult();
			catcnt = v.intValue();
		}catch (Exception ex) {
			ex.printStackTrace();
		}
		
		for (int i = 0; i < catcnt+1; i ++) {
			TableViewerColumn catColumn = new TableViewerColumn(tblData, SWT.NONE);
			catColumn.getColumn().setText(MessageFormat.format("Category {0}", i+1));
			catColumn.getColumn().setWidth( 100 );

			final int index = i;
			catColumn.setLabelProvider(new ColumnLabelProvider() {
				public String getText(Object element) {
					if (element instanceof WaypointObservation) {
						Category c = ((WaypointObservation)element).getCategory();
						
						int size = Category.hkeyLength(c.getHkey());
						if (size >= index) {
							while(size != index) {
							c = c.getParent();
								size --;
							}
							return c.getName();	
						}
					}
					return "";

				}
			});
		}
		
		tblData.addSelectionChangedListener(e->{
			Object x = tblData.getStructuredSelection().getFirstElement();
			UUID wpuuid = null;
			if (x != null && x instanceof Waypoint) {
				wpuuid = ((Waypoint)x).getUuid();
			}else if (x != null && x instanceof WaypointObservation) {
				wpuuid = ((WaypointObservation)x).getWaypoint().getUuid();
			}

			if (wpuuid == null) {
				waypointLayer.setFilter(Filter.EXCLUDE);
			}else {
				FilterFactory ff = CommonFactoryFinder.getFilterFactory();
				Filter filter = ff.equal(ff.property("wp_uuid"), ff.literal(UuidUtils.uuidToString(wpuuid)), false);
				waypointLayer.setFilter(filter);
			}
			
		});
		
		Menu wpMenu = new Menu(tblData.getControl());
		tblData.getControl().setMenu(wpMenu);
		
		MenuItem miZoom = new MenuItem(wpMenu,SWT.PUSH);
		miZoom.setText("Zoom To");
		miZoom.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ZOOM_IMAGE));
		miZoom.addListener(SWT.Selection,e->{
			Object x = tblData.getStructuredSelection().getFirstElement();
			Waypoint wp = null;
			if (x instanceof Waypoint) {
				wp = (Waypoint)x;
			}else if ( x instanceof WaypointObservation ) {
				wp = ((WaypointObservation)x).getWaypoint();
			}
			if (wp == null) return;
			
			Coordinate c = new Coordinate(wp.getX(), wp.getY());
			double offset = 0.001;
			ReferencedEnvelope re = new ReferencedEnvelope(c.x-offset, c.x+offset, c.y-offset, c.y+offset, GeometryUtils.SMART_CRS);
			SetViewportBBoxCommand cmd = new SetViewportBBoxCommand(re, true);
			getMap().sendCommandASync(cmd);	
		});
		
		imageViewer = new PatrolPresentationImageViewer(sashUpDown, SWT.NONE);
		imageViewer.setSource(null);
	}
	
    @Override
    public void dispose() {
    	JobUtil.stopJobs(loadDefaultLayers, addLayerJob, refreshJob);
    	this.loadDefaultLayers = null;
        this.refreshJob = null;
        this.addLayerJob = null;
        super.dispose();
        
        PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, patrolUpdatedListeners);
        
        //dispose of patrol service
        CatalogPlugin.getDefault().getLocalCatalog().remove(patrolService);
        patrolService.dispose(null);
        patrolService = null;
        parentEditor = null;
        patrolUpdatedListeners = null;
    }


   
    
    private IInfoToolShellProvider getInfoShellProvider() {
    	return new WaypointInfoShellProvider(getSite().getShell(), super.mapViewer.getControl());
    }
    
    private IInfoToolProvider getMapInfoProvider(){
		return new IInfoToolProvider(){
			@Override
			public InfoPoint findFeature(int x, int y, IViewportModel vm) {
				try{
					int xll = x - 5;
					int yll = y - 5;
					int xur = x + 5;
					int yur = y + 5;
					
					Coordinate worldll = vm.pixelToWorld(xll, yll);
					Coordinate worldur = vm.pixelToWorld(xur, yur);
					
					Coordinate dbll = ReprojectUtils.reproject(worldll.x, worldll.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					Coordinate dbur = ReprojectUtils.reproject(worldur.x, worldur.y, vm.getCRS(), SmartDB.DATABASE_CRS);
					
					Envelope env = new Envelope(dbll,  dbur);

					//find all waypoints in bounding box
					List<Waypoint> waypoints = new ArrayList<>();
					for(PatrolLeg pl : parentEditor.getPatrol().getLegs()) {
						for (PatrolLegDay pld : pl.getPatrolLegDays()) {
							for (PatrolWaypoint pw : pld.getWaypoints()) {
								if (env.contains(pw.getWaypoint().getX(), pw.getWaypoint().getY())) {
									waypoints.add(pw.getWaypoint());
								}
							}
						}
					}
					
					if (waypoints.isEmpty()) return null;
					Coordinate px = ReprojectUtils.reproject(waypoints.get(0).getX(), waypoints.get(0).getY(), SmartDB.DATABASE_CRS, vm.getCRS());
					return new InfoPoint(vm.worldToPixel(px), waypoints, null);	
				}catch (Exception ex) {
					SmartPatrolPlugIn.log(ex.getMessage(), ex);					
				}
				return null;
			}
			
		};	
	}
    
    
    private void createHeader(Composite parent) {

    	header1 = new PresentationHeader(parent, SWT.NONE, e->{
    		if (header1.getCurrentDate() == null) {
    			((StackLayout)rightStack.getLayout()).topControl = patrolSummary;
    		}else {
    			((StackLayout)rightStack.getLayout()).topControl = patrolData;
    			
    			tblData.setInput(pdata.get(header1.getCurrentDate()));
    		}
    		rightStack.layout();
    	});
    	header1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    }
    
    private void initData() {
    	
    	for (Control kid : patrolSummary.getChildren()) kid.dispose();
    	
    	patrolSummary.setLayout(new GridLayout());
    	((GridLayout)patrolSummary.getLayout()).marginWidth = 0;
    	((GridLayout)patrolSummary.getLayout()).marginHeight = 0;
    	
    	FormToolkit toolkit = new FormToolkit(patrolSummary.getDisplay());
    	
    	ScrolledForm scroll = toolkit.createScrolledForm(patrolSummary);
    	scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	Composite scrollComposite = scroll.getBody();
    	scrollComposite.setLayout(new GridLayout());
    	((GridLayout)scrollComposite.getLayout()).marginWidth = 0;
    	((GridLayout)scrollComposite.getLayout()).marginHeight = 0;

    	//scroll.setContent(scrollComposite);
    	
    	Section summaryArea = toolkit.createSection(scrollComposite, Section.TITLE_BAR);
    	summaryArea.setText("Patrol Summary");
    	summaryArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    	Composite sarea = toolkit.createComposite(summaryArea);
    	sarea.setLayout(new GridLayout(2, true));
    	((GridLayout)sarea.getLayout()).marginWidth = 0;
    	((GridLayout)sarea.getLayout()).marginHeight = 0;
    	summaryArea.setClient(sarea);
    	summaryArea.setLayout(new GridLayout());
//    	sarea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	Composite left =  toolkit.createComposite(sarea);
    	left.setLayout(new GridLayout(2, false));
    	((GridLayout)left.getLayout()).marginWidth = 0;
    	((GridLayout)left.getLayout()).marginHeight = 0;
    	left.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
    	
    	Composite right =  toolkit.createComposite(sarea);
    	right.setLayout(new GridLayout());
    	((GridLayout)right.getLayout()).marginWidth = 0;
    	((GridLayout)right.getLayout()).marginHeight = 0;
    	right.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	
    	
    	try(Session session = HibernateManager.openSession()){
			viewProjection = HibernateManager.getCurrentViewProjection(session);
			if (viewProjection == null) {
				viewProjection = new Projection();
				viewProjection.setParsedCoordinateReferenceSystem(SmartDB.DATABASE_CRS);
				viewProjection.setName(SmartDB.DATABASE_CRS.getName().toString());
			}
			if (viewProjection.getParsedCoordinateReferenceSystem() == null){
				try {
					viewProjection.setParsedCoordinateReferenceSystem( ReprojectUtils.stringToCrs(viewProjection.getDefinition()) );
				}catch (Exception ex) {
					SmartPatrolPlugIn.log(ex.getMessage(), ex);
				}
			}

			Patrol patrol = session.get(Patrol.class, parentEditor.getPatrol().getUuid());

			toolkit.createLabel(left, "Start:");
			toolkit.createLabel(left, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(patrol.getStartDate()));
			
			toolkit.createLabel(left, "End:");
			toolkit.createLabel(left, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(patrol.getEndDate()));
			
			new Label(left, SWT.NONE);
			new Label(left, SWT.NONE);
			
			
			toolkit.createLabel(left, "Station:");
			toolkit.createLabel(left, patrol.getStation() == null ? "" : patrol.getStation().getName());
			
			toolkit.createLabel(left, "Mandate:");
			
			Set<PatrolMandate> mandates = new HashSet<>();
			mandates.add(patrol.getLegs().get(0).getMandate());
			
			toolkit.createLabel(left, patrol.getLegs().get(0).getMandate().getName());
			
			for (int i = 1; i < patrol.getLegs().size(); i ++) {
				if (mandates.contains(patrol.getLegs().get(i).getMandate())) continue;
				new Label(left, SWT.NONE);
				toolkit.createLabel(left, patrol.getLegs().get(i).getMandate().getName());
				mandates.add(patrol.getLegs().get(1).getMandate());
			}
			
			new Label(left, SWT.NONE);
			new Label(left, SWT.NONE);
			
			float distance = 0;
			double totalTime = 0;
			double activeTime = 0;
			
			HashMap<PatrolTransportType, Double> tdistance = new HashMap<>();
			
			for (PatrolLeg leg : patrol.getLegs()) {
				for (PatrolLegDay pld : leg.getPatrolLegDays()) {
					Track track = pld.getTrack();
					float value = 0;
					if (track != null && track.getDistance() != null) {
						value = track.getDistance();
						distance += value;
					}
					Double d = tdistance.get(pld.getPatrolLeg().getType());
					if (d == null) {
						d = Double.valueOf(value);
					}else {
						d = d + value;
					}
					tdistance.put(pld.getPatrolLeg().getType(), d);
					
					totalTime += pld.getPatrolHoursWorked();
					activeTime += pld.getFieldHoursWorked();
				}
			}
			
			toolkit.createLabel(left, "Total Time:");
			toolkit.createLabel(left, PatrolEditor.formatTimeRange(totalTime));
			
			toolkit.createLabel(left, "Active Time:");
			toolkit.createLabel(left, PatrolEditor.formatTimeRange(activeTime));
			
			toolkit.createLabel(left, "Total Distance:");
			toolkit.createLabel(left, PatrolEditor.DISTANCE_FORMATTER.format(distance) + " km");
			
			tdistance.keySet().stream().sorted((a,b)->Collator.getInstance().compare(a.getName(),  b.getName())).forEach(pt->{
				Label ll = toolkit.createLabel(left, pt.getName() + ":");
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				
				toolkit.createLabel(left, PatrolEditor.DISTANCE_FORMATTER.format(tdistance.get(pt)) + " km");
			});
			
			Set<Employee> members = new HashSet<>();
			Set<Employee> leaders = new HashSet<>();
			for (PatrolLeg pl : patrol.getLegs()) {
				pl.getMembers().forEach(pm -> members.add(pm.getMember()));
				leaders.add(pl.getLeader().getMember());
			}
			members.removeAll(leaders);
			
			toolkit.createLabel(right, "Leader(s):");
			
			TableViewer tblLeaders = new TableViewer(right, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			tblLeaders.setContentProvider(ArrayContentProvider.getInstance());
			tblLeaders.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					return SmartLabelProvider.getShortLabel((Employee)element);
				}
			});
			tblLeaders.setInput(leaders.stream()
					.sorted((a,b)-> Collator.getInstance().compare(SmartLabelProvider.getShortLabel(a), SmartLabelProvider.getShortLabel(b)))  
					.collect(Collectors.toList()));
			tblLeaders.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			if (leaders.size() > 3) 
				((GridData)tblLeaders.getControl().getLayoutData()).heightHint = 60;
			((GridData)tblLeaders.getControl().getLayoutData()).widthHint = 100;
			
			toolkit.createLabel(right,"Members(s):");
			
			TableViewer tblMembers = new TableViewer(right, SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
			tblMembers.setContentProvider(ArrayContentProvider.getInstance());
			tblMembers.setLabelProvider(new LabelProvider() {
				public String getText(Object element) {
					return SmartLabelProvider.getShortLabel((Employee)element);
				}
			});
			tblMembers.setInput(members.stream()
					.sorted((a,b)-> Collator.getInstance().compare(SmartLabelProvider.getShortLabel(a), SmartLabelProvider.getShortLabel(b)))  
					.collect(Collectors.toList()));
			tblMembers.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			if (members.size() > 10) 
				((GridData)tblMembers.getControl().getLayoutData()).heightHint = 150;
			((GridData)tblMembers.getControl().getLayoutData()).widthHint = 100;
			
			if (patrol.getObjective() != null && !patrol.getObjective().isEmpty()) {
				
				Label l = toolkit.createLabel(sarea, "Objective:");
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
				
				Text txtObj = toolkit.createText(sarea, patrol.getObjective() == null ? "" : patrol.getObjective(), SWT.MULTI | SWT.V_SCROLL | SWT.WRAP);
				txtObj.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				((GridData)txtObj.getLayoutData()).heightHint = 60;
			}
			
			
			for(IPatrolPresentationContribution contribution : contributions) {
				Composite part = contribution.createSummaryWidget(scrollComposite, patrol, session, toolkit);
				if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			}
			
			Set<LocalDate> dates = new HashSet<>();
			
			pdata = new HashMap<>();
			
			for (PatrolLeg leg : patrol.getLegs()) {
				for (PatrolLegDay legday : leg.getPatrolLegDays()) {
					dates.add(legday.getDate());
					
					List<Object> pdaydata = pdata.get(legday.getDate());
					if (pdaydata == null) {
						pdaydata = new ArrayList<>();
						pdata.put(legday.getDate(), pdaydata);
					}
					
					final List<Object> fpdaydata = pdaydata;
					
					if (!legday.getWaypoints().isEmpty() && patrol.getLegs().size() > 1) {
						pdaydata.add(leg);
					}
					
					legday.getWaypoints()
						.stream()
						.sorted((a,b)->a.getWaypoint().getDateTime().compareTo(b.getWaypoint().getDateTime()))
						.forEach(pw->{
							if (pw.getWaypoint().getAllObservations().isEmpty()) {
								fpdaydata.add(pw.getWaypoint());
							}else {
								pw.getWaypoint().getAllObservations().forEach(wp->wp.getCategory().getFullCategoryName());								
								fpdaydata.addAll(pw.getWaypoint().getAllObservations());
							}							
						});

				}
			}
			
			List<LocalDate> sortedDates = dates.stream().sorted((a,b)->a.compareTo(b)).collect(Collectors.toList());
			header1.setDateRange(sortedDates);
			header1.setCurrentDate(null);
    	}
    	
    	patrolSummary.layout(true);
    }
    
    
    
	private List<IPatrolPresentationContribution> findContributions(){
		List<IPatrolPresentationContribution> items = new ArrayList<>();
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IPatrolEditorContribution.EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equals(IPatrolPresentationContribution.NAME)){ 
					IPatrolPresentationContribution page = (IPatrolPresentationContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
					ContextInjectionFactory.inject(page, parentEditor.getContext());
					items.add(page);
				}
			}
		}catch (Exception ex){
			SmartPatrolPlugIn.displayLog(Messages.CreatePatrolWizard_ErrorCreatingWizardPages, ex);
			return null;
		}
		return items;
	}
	
	private enum Column{
		ID("ID"),
		X("X"),
		Y("Y"),
		TIME("Time");
		
		private String name;
		
		private Column(String name) {
			this.name = name;
		}
		public String getName() {
			return this.name;
		}
		public int getSize() {
			if (this == ID) return 50;
			return 100;
		}
		
		public String getValue(Object value, Projection viewProjection) {
			if (value == null) return "";
			
			WaypointObservation wo = null;
			Waypoint wp = null;
			PatrolLeg pl = null;
			if (value instanceof WaypointObservation) {
				wo = (WaypointObservation) value;
			}else if (value instanceof Waypoint) {
				wp = (Waypoint)value;
			}else if (value instanceof PatrolLeg) {
				pl = (PatrolLeg)value;
			}
			
			Double x = null;
			Double y = null;
			
			switch(this) {
			case ID:
				if (pl != null) return pl.getId();
				if (wo != null) return wo.getWaypoint().getId();
				if (wp != null) return wp.getId();
				return "";
			case TIME:
				if (wo != null) return DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format( wo.getWaypoint().getDateTime().toLocalTime() );
				if (wp != null) return DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format( wp.getDateTime().toLocalTime() );
				return "";
			case X:
				
				if (wo != null) {
					y = wo.getWaypoint().getY();
					x = wo.getWaypoint().getX();
				}else if (wp != null) {
					y = wp.getY();
					x = wp.getX();
				}
				if (x != null && y != null) {
					return String.valueOf(ReprojectUtils.transform(x, y, viewProjection.getParsedCoordinateReferenceSystem()).getX());
				}
				return "";
			case Y:
				if (wo != null) {
					y = wo.getWaypoint().getY();
					x = wo.getWaypoint().getX();
				}else if (wp != null) {
					y = wp.getY();
					x = wp.getX();
				}
				if (x != null && y != null) {
					return String.valueOf(ReprojectUtils.transform(x, y, viewProjection.getParsedCoordinateReferenceSystem()).getX());
				}
				return "";
			}
			return "";
		}
		
	}
}


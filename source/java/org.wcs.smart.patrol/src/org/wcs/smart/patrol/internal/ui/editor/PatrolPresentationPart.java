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
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IAdaptable;
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
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MenuListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.IFormColors;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.forms.widgets.Section;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hibernate.Session;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.IStyleBlackboard;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.ProjectPackage;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.locationtech.udig.project.internal.command.navigation.SetViewportBBoxCommand;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.impl.StyleBlackboardImpl;
import org.locationtech.udig.project.internal.render.SelectionStyleContent;
import org.locationtech.udig.project.internal.render.impl.RenderManagerImpl;
import org.locationtech.udig.project.render.IViewportModel;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Projection;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.map.ShowStyleDialogHandler;
import org.wcs.smart.observation.events.IWaypointEventListener;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationAttribute;
import org.wcs.smart.observation.ui.WaypointInfoShellProvider;
import org.wcs.smart.observation.ui.input.ObservationWizard;
import org.wcs.smart.observation.ui.input.ObservationWizardDialog;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.geotools.PatrolDataSource;
import org.wcs.smart.patrol.geotools.PatrolFeatureSource;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.map.style.PatrolReviewTrackDefaultStyle;
import org.wcs.smart.patrol.map.style.PatrolReviewWaypointDefaultStyle;
import org.wcs.smart.patrol.map.style.PatrolReviewWaypointRawDefaultStyle;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLeg;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.model.PatrolLegMember;
import org.wcs.smart.patrol.model.PatrolMandate;
import org.wcs.smart.patrol.model.PatrolTransportType;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.model.PatrolWaypointSource;
import org.wcs.smart.patrol.model.Track;
import org.wcs.smart.patrol.udig.catalog.PatrolGeoResource;
import org.wcs.smart.patrol.udig.catalog.PatrolService;
import org.wcs.smart.patrol.udig.catalog.StyleUtils;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;
import org.wcs.smart.patrol.ui.IPatrolPresentationContribution;
import org.wcs.smart.patrol.ui.PatrolEditor;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.SmartLabelProvider;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.IInfoToolProvider;
import org.wcs.smart.ui.map.tool.IInfoToolShellProvider;
import org.wcs.smart.ui.properties.DialogConstants;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.SmartUtils;
import org.wcs.smart.util.UuidUtils;


/**
 * Page for the editor for displaying a map
 * of the waypoints and tracks.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolPresentationPart extends SmartMapEditorPart {
	
	public static final String ID = "org.wcs.smart.patrol.ui.PatrolPresentationPart"; //$NON-NLS-1$
	
	public static final String TAB_NAME = Messages.PatrolPresentationPart_tabname;
	
	private PatrolEditor parentEditor; 
	
	private Section summaryArea;
	private PatrolService patrolService = null;
	private LoadDefaultLayersJob loadDefaultLayers;
		
	private Composite patrolSummary;
	private Composite patrolData;
	private List<IPatrolPresentationContribution> contributions;
	
	private Composite rightStack;
	private PresentationHeader header1 ;
	private PatrolPresentationImageViewer imageViewer;
	private FormToolkit toolkit;
	
	private TableViewer tblData;
	private HashMap<LocalDate, List<Object>> pdata;
	private Projection viewProjection;
	
	private Layer waypointLayer = null;
	private Layer trackLayer = null;

	
	private Object currentSelection = null;
	
	private boolean autoZoom = true;
		
	private Job addLayerJob = new Job(Messages.PatrolMapPageEditor_AddLayersJobName) {
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			patrolService = new PatrolService(parentEditor.getPatrol());
	    	try {
	    		Map<String, IGeoResource> toAdd = new HashMap<>();
	    		List<IGeoResource> layers = (List<IGeoResource>) patrolService.resources(monitor);
	    		List<IGeoResource> sortedLayers = new ArrayList<>();

	    		for (IGeoResource l : layers) toAdd.put(  ((PatrolGeoResource)l).getType(), l );
    
	    		String[] orderedLayers = new String[] {
	    				PatrolDataSource.TRACK_PART_TYPE, 
	    				PatrolDataSource.WAYPOINT_PRJ_TYPE, 
	    				PatrolDataSource.WAYPOINT_TYPE,
	    		};
	    		for (String name : orderedLayers) {
	    			sortedLayers.add(toAdd.get(name));
	    			toAdd.remove(name);
	    		}
	    		List<IGeoResource> othersorted = new ArrayList<>();
	    		othersorted.addAll(toAdd.values());
	    		othersorted.sort((a,b)->-Collator.getInstance().compare(a.getTitle(), b.getTitle()));
	    		sortedLayers.addAll(0,othersorted);
    		
	    		
	    		AddLayersCommand command = new AddLayersCommand(sortedLayers, getMap().getLayersInternal().size()) {
	    			public void run( IProgressMonitor monitor ) throws Exception {
	    				
	    				((RenderManagerImpl)getMap().getRenderManagerInternal()).disableRendering();
	    				
	    				super.run(monitor);
	    				
	    				Map<String,String> geoIdToStyle = new HashMap<>();
	    				geoIdToStyle.put(PatrolDataSource.TRACK_PART_TYPE,  PatrolReviewTrackDefaultStyle.KEY);
	    				geoIdToStyle.put(PatrolDataSource.WAYPOINT_PRJ_TYPE,  PatrolReviewWaypointRawDefaultStyle.KEY);
	    				geoIdToStyle.put(PatrolDataSource.WAYPOINT_TYPE,  PatrolReviewWaypointDefaultStyle.KEY);

	    				//if a default style is not specified we'll use this style instead
	    				Map<String, Consumer<Layer>> defaultStyles = new HashMap<>();
	    				defaultStyles.put(PatrolReviewTrackDefaultStyle.KEY, (l)->{
	    					try {
								IStyleBlackboard bb = StyleUtils.INSTANCE.getPatrolTrackStyle(parentEditor.getPatrol());
								if (bb != null) l.setStyleBlackboard((StyleBlackboard) bb);
							} catch (Exception e) {
								SmartPlugIn.log(e.getMessage(), e);
							}
	    				});
	    				try(Session session = HibernateManager.openSession()){
		    				for (Layer l : getLayers()) {
		    					
		    					StyleManager.INSTANCE.applyDefaultStyleToMapLayer(SmartDB.getCurrentConservationArea(), l, geoIdToStyle, defaultStyles, session, monitor);
		    					
		    					PatrolFeatureSource fs = l.getGeoResource().resolve(PatrolFeatureSource.class, monitor);
		    					if (fs != null) {
		    						l.setVisible(fs.getDefaultVisibility());
		    						l.eNotify(new ENotificationImpl(
		    								(InternalEObject) l, Notification.SET,
		    								ProjectPackage.LAYER__VISIBLE, false, l.isVisible()));	
		    					}
		    					
		    					if (l.getGeoResource().canResolve(PatrolGeoResource.class)) {
		    						
		    						
		    						String type = l.getGeoResource().resolve(PatrolGeoResource.class, new NullProgressMonitor()).getType();
		    						if (type.equals(PatrolDataSource.TRACK_PART_TYPE)) {
		    							trackLayer = l;
		    						}else if (type.equals(PatrolDataSource.WAYPOINT_TYPE)) {
		    							waypointLayer = l;
		    							waypointLayer.setStyleBlackboard(new WaypointLayerStyleBlackboard(waypointLayer.getStyleBlackboard()));
		    							waypointLayer.getStyleBlackboard().put(SelectionStyleContent.ID, 
		    									StyleUtils.INSTANCE.getPointSelectionStyle(waypointLayer.getSchema()));
//		    						}else if (type.equals(PatrolDataSource.OBS_ATTRIBUTE_LINESTRING)) {
//		    							attributeLineStringLayer = l;
//		    							//attributeLineStringLayer.setFilter(Filter.EXCLUDE);
//		    						}else if (type.equals(PatrolDataSource.OBS_ATTRIBUTE_POLYGON)) {
//		    							attributePolygonLayer = l;
//		    							
//		    							//DataStore src = attributePolygonLayer.getGeoResource().resolve(PatrolGeoResource.class, null).resolve(DataStore.class, null);
//		    							//attributePolygonLayer.setFilter(Filter.EXCLUDE);
		    						}
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
				p = (Patrol) source;
			}else if (source instanceof PatrolLegDay){
				p = ((PatrolLegDay)source).getPatrolLeg().getPatrol();
			}
			if (p != null && p.getUuid().equals(parentEditor.getPatrolUuid())){
				if (attributeChanged == PatrolEventManager.PATROL_WAYPOINTS) refresh();
			}
		}
	};
	    
	IPatrolEventListener waypointDeleteListener = new IPatrolEventListener() {

		@Override
		public void eventFired(int attributeChanged, Object source) {
			if (!(source instanceof PatrolWaypoint)) return;
			PatrolWaypoint pw = (PatrolWaypoint)source;
			
			//we don't know what patrol this is from; only date update date if we match it
			if (pdata.containsKey(pw.getWaypoint().getDateTime().toLocalDate())){
				refreshWaypointData();
			}
			
		}
		
	};

	IWaypointEventListener waypointModifiedListener = new IWaypointEventListener() {
		
		@Override
		public void handleEvent(Waypoint wp) {
			if (!wp.getSourceId().equals(PatrolWaypointSource.PATROL_WP_SOURCE_ID)) return;
			PatrolWaypoint pw = null;
			try(Session session = HibernateManager.openSession()){
				pw = QueryFactory.buildQuery(session, 
						PatrolWaypoint.class,
						new Object[] {"id.waypoint", wp}).uniqueResult(); //$NON-NLS-1$
			
				if (pw == null || !pw.getPatrolLegDay().getPatrolLeg().getPatrol().getUuid().equals(parentEditor.getPatrolUuid())) {
					return;
				}
				refreshWaypointData();
			}
			getMap().getRenderManager().refresh(null);
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
    	getSite().getShell().getDisplay().asyncExec(()->{
    		initData();
    	});
	}

    public void renderMap() {
    	super.getMap().getRenderManager().refresh(null);
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

	@Override
	protected MapToolComposite createMapToolsComposite() {
		this.mapTools = new String[MapToolComposite.DEFAULT_MAP_TOOLS.length + 4];
		
		for (int i = 0; i < MapToolComposite.DEFAULT_MAP_TOOLS.length; i ++) {
			this.mapTools[i] = MapToolComposite.DEFAULT_MAP_TOOLS[i];
		}
		
		int index = mapTools.length - 4;
		String autoZoomId = "org.wcs.smart.patrol.presentation.autozoomoption"; //$NON-NLS-1$
		String waypointStyleId = "org.wcs.smart.patrol.presentation.waypointstyle"; //$NON-NLS-1$
		String trackStyleId = "org.wcs.smart.patrol.presentation.trackstyle"; //$NON-NLS-1$
		
		this.mapTools[index] = MapToolComposite.SEPERATOR_TOOL_ID;
		this.mapTools[index+1] = autoZoomId;
		this.mapTools[index+2] = waypointStyleId;
		this.mapTools[index+3] = trackStyleId;
		
		MapToolComposite mapTools = super.createMapToolsComposite();

		mapTools.addCustomToolItem(autoZoomId, Messages.PresentationHeader_autozoomtooltip, 
				SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ICON_AUTOZOOM), l->{
					autoZoom = (((ToolItem)l.widget).getSelection());
					}, SWT.CHECK, Boolean.TRUE);
		
		mapTools.addCustomToolItem(waypointStyleId, Messages.PatrolPresentationPart_changewpstyletooltip, 
				SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.STYLEWAYPOINT_ICON), l->{
					(new ShowStyleDialogHandler()).showDialog(getSite().getShell(), waypointLayer);
					}, SWT.PUSH, Boolean.TRUE);
		
		mapTools.addCustomToolItem(trackStyleId, Messages.PatrolPresentationPart_changetrackstyletooltip, 
				SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.STYLETRACK_ICON), l->{
					(new ShowStyleDialogHandler()).showDialog(getSite().getShell(), trackLayer);
					}, SWT.PUSH, Boolean.TRUE);
		
		return mapTools;
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
       
        WaypointEventManager.getInstance().addListener(WaypointEventManager.EventType.WAYPOINT_MODIFIED, waypointModifiedListener);
        PatrolEventManager.getInstance().addListener(PatrolEventManager.EventType.WAYPOINT_DELETED, waypointDeleteListener);
        
        
        getMap().getBlackboard().put(IInfoToolProvider.BLACKBOARD_KEY, getMapInfoProvider());
        getMap().getBlackboard().put(IInfoToolShellProvider.BLACKBOARD_KEY, getInfoShellProvider());
        
        getMap().setName("PRESENTATION MAP"); //$NON-NLS-1$
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
//		tblData.getTable().setLinesVisible(true);
		tblData.getTable().setHeaderVisible(true);
		tblData.addDoubleClickListener(e->{
			editWaypoint();
		});
		
		tblData.addSelectionChangedListener(e->{
			Object x = tblData.getStructuredSelection().getFirstElement();
			
			if (x == currentSelection) return;
			Object lastSelection = currentSelection;
			currentSelection = x;
			
			Waypoint wp = findWaypoint(x);
			if (x instanceof FirstWaypointObservation) {
				x = ((FirstWaypointObservation)x).wo;
			}
			if (x instanceof Waypoint) {
				imageViewer.setSource(x);
			}else if (x instanceof WaypointObservation) {
				imageViewer.setSource(x);
			}else {
				imageViewer.setSource(null);
			}
			
			
			if (wp == null) {
				waypointLayer.setFilter(Filter.EXCLUDE);
			}else {
				if (!wp.equals(findWaypoint(lastSelection))) {
					FilterFactory ff = CommonFactoryFinder.getFilterFactory();
					Filter filter = ff.equal(ff.property("wp_uuid"), ff.literal(UuidUtils.uuidToString(wp.getUuid())), false); //$NON-NLS-1$
					waypointLayer.setFilter(filter);
					if(autoZoom) zoomTo(wp);
				}
			}
		});

	
		Listener paintListener = event -> {
			ViewerCell cell = tblData.getCell(new Point(event.x, event.y));
			if (cell == null) return;
			Object element = cell.getElement();
			if (!(element instanceof PatrolLeg)) return;
			
			switch(event.type) {
				case SWT.PaintItem: {
					PatrolLeg pl = (PatrolLeg)element;
					GC gc = event.gc;
					String text = MessageFormat.format(Messages.PatrolPresentationPart_PatrolLegHeader, 
							pl.getId(), pl.getType().getName(), 
							pl.getMandate() == null ? "" : pl.getMandate().getName());  //$NON-NLS-1$
					final Point extent = gc.stringExtent(text);

					int width = 0;
					for (TableColumn tc : tblData.getTable().getColumns()){
						width += tc.getWidth();
					}
					int height = cell.getBounds().height;
					event.gc.setBackground(toolkit.getColors().getColor(IFormColors.TB_BG));
					event.gc.fillRectangle(0,event.y,width,height-1);

					event.gc.setBackground(tblData.getControl().getDisplay().getSystemColor(SWT.COLOR_BLACK));
					event.gc.drawRectangle(0,event.y, width-1, height-1);

					event.gc.setForeground(summaryArea.getForeground());
					
					event.gc.setFont(summaryArea.getFont());
					int y = event.y + (event.height - extent.y)/2;
					event.gc.drawString(text, 0+5, y, true);
					break;
				}
			}
		};
		tblData.getTable().addListener(SWT.PaintItem, paintListener);
		
		parentEditor.getSelectionProvider().addSelectionProvider(tblData);
		ColumnViewerToolTipSupport.enableFor(tblData);
		
		for (Column c : new Column[] {Column.ID, Column.TIME, Column.CATEGORY}) createTableColumn(c);
		
//		int catcnt = 0;
//		try(Session session = HibernateManager.openSession()){
//			Number v = (Number)session.createNativeQuery("SELECT max(smart.hkeylength(c.hkey)) FROM smart.dm_category c WHERE c.ca_uuid = :ca")
//					.setParameter("ca", parentEditor.getPatrol().getConservationArea())
//					.uniqueResult();
//			catcnt = v.intValue();
//		}catch (Exception ex) {
//			ex.printStackTrace();
//		}
//		
//		for (int i = catcnt; i >=0; i --) createTableColumn(i);
		
		for (Column c : new Column[] {Column.X, Column.Y, Column.ATTACHMENTS}) createTableColumn(c);
					
		Menu wpMenu = new Menu(tblData.getControl());
		tblData.getControl().setMenu(wpMenu);
		
		MenuItem miZoom = new MenuItem(wpMenu,SWT.PUSH);
		miZoom.setText(Messages.PatrolPresentationPart_ZoomToMenu);
		miZoom.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ZOOM_IMAGE));
		miZoom.addListener(SWT.Selection,e->{
			Object x = tblData.getStructuredSelection().getFirstElement();
			zoomTo(findWaypoint(x));
		});
		
		MenuItem miEdit = new MenuItem(wpMenu,SWT.PUSH);
		miEdit.setText(DialogConstants.EDIT_BUTTON_TEXT);
		miEdit.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.EDIT_ICON));
		miEdit.addListener(SWT.Selection,e->{
			editWaypoint();
		});
		
		new MenuItem(wpMenu, SWT.SEPARATOR);
		
		MenuItem miGoto = new MenuItem(wpMenu,SWT.PUSH);
		miGoto.setText(Messages.PatrolPresentationPart_GotoMenuItme);
		miGoto.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON));
		miGoto.addListener(SWT.Selection,e->{
			Object x = tblData.getStructuredSelection().getFirstElement();
			Waypoint wp = findWaypoint(x);
			if (wp == null) return;
			parentEditor.findAndShow(wp.getUuid());
		});
		
		wpMenu.addMenuListener(new MenuListener() {
			
			@Override
			public void menuShown(MenuEvent e) {
				Object x = tblData.getStructuredSelection().getFirstElement();
				boolean iswp = x != null && x instanceof Waypoint || x instanceof WaypointObservation || x instanceof FirstWaypointObservation;
				
				miZoom.setEnabled(iswp);
				miGoto.setEnabled(iswp);
				miEdit.setEnabled(iswp);
			}
			
			@Override
			public void menuHidden(MenuEvent e) {}
		});
		imageViewer = new PatrolPresentationImageViewer(sashUpDown, SWT.NONE);
		imageViewer.setSource(null);
		
		imageViewer.addListener(SWT.Paint, e->{
			e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_GRAY));
			e.gc.drawLine(dataSection.getLocation().x, dataSection.getLocation().y, dataSection.getSize().x, dataSection.getLocation().y);
			
		});
	}
	
	private void editWaypoint() {
		Object x = tblData.getStructuredSelection().getFirstElement();
		Waypoint wp = findWaypoint(x);
		WaypointObservation wo = findObservation(x);
		
		if (wp == null) return;
		
		PatrolLegDay legDay = null;
		
		for (PatrolLeg pl : parentEditor.getPatrol().getLegs()) {
			for (PatrolLegDay pld : pl.getPatrolLegDays()) {
				for (PatrolWaypoint pd : pld.getWaypoints()) {
					if (pd.getWaypoint().equals(wp)) {
						legDay = pld;
						break;
					}
				}
				if (legDay != null) break;
			}
			if (legDay != null) break;
		}
		
		List<Employee> employees = new ArrayList<>();
		for (PatrolLegMember m : legDay.getPatrolLeg().getMembers()){
			if (!employees.contains(m.getMember())) employees.add(m.getMember());
		}			
				
		if(wp.getObservationGroups() == null)wp.setObservationGroups(new ArrayList<>());
		
		final ObservationWizard wizard = new ObservationWizard(wp, employees);
		ObservationWizardDialog dialog = new ObservationWizardDialog(getSite().getShell(), wizard);
		wizard.setWizardDialog(dialog);
		wizard.selectObservation(wo);
		if (dialog.open() == Window.CANCEL) {
			return;
		}
		wp.getObservationGroups().clear();
		wp.getObservationGroups().addAll(wizard.getWaypoint().getObservationGroups());

		//fire change to ensure day pages are updated
		PatrolEventManager.getInstance().patrolChanged(PatrolEventManager.PATROL_WAYPOINTS, legDay);
	}
	
	
//	private void createTableColumn(int index) {
//		TableViewerColumn catColumn = new TableViewerColumn(tblData, SWT.NONE);
//		catColumn.getColumn().setText(MessageFormat.format("Category {0}", index+1));
//		catColumn.getColumn().setWidth( 100 );
//
//		catColumn.setLabelProvider(new ColumnLabelProvider() {
//			public String getText(Object element) {
//				if (element instanceof FirstWaypointObservation) {
//					element = ((FirstWaypointObservation)element).wo;
//				}
//				if (element instanceof WaypointObservation) {
//					Category c = ((WaypointObservation)element).getCategory();
//					
//					int size = Category.hkeyLength(c.getHkey());
//					if (size >= index) {
//						while(size != index) {
//						c = c.getParent();
//							size --;
//						}
//						return c.getName();	
//					}
//				}
//				return ""; //$NON-NLS-1$
//
//			}
//			public Color getBackground(Object element) {
//				if (element instanceof PatrolLeg) return parentEditor.getSite().getShell().getDisplay().getSystemColor(SWT.COLOR_GRAY);
//				return null;
//			}
//		});
//	}
	
	private void createTableColumn(Column c) {
		TableViewerColumn tblColumn = new TableViewerColumn(tblData, SWT.NONE);
		tblColumn.getColumn().setText(c.getName());
		tblColumn.setLabelProvider(new ColumnLabelProvider() {
			
			public String getText(Object element) {
				return c.getValue(element, viewProjection);
			}
			
			@Override
			public String getToolTipText(Object element) {
				return c.getTooltip(element);
			}
		});
		tblColumn.getColumn().setWidth( c.getSize() );
		
	}
	private void zoomTo(Waypoint wp) {
		if (wp == null) return;
		Coordinate c = new Coordinate(wp.getX(), wp.getY());
		double offset = 0.01;
		ReferencedEnvelope re = new ReferencedEnvelope(c.x-offset, c.x+offset, c.y-offset, c.y+offset, GeometryUtils.SMART_CRS);
		SetViewportBBoxCommand cmd = new SetViewportBBoxCommand(re, true);
		getMap().sendCommandASync(cmd);	
	}
	
    @Override
    public void dispose() {
    	JobUtil.stopJobs(loadDefaultLayers, addLayerJob, refreshJob);
    	this.loadDefaultLayers = null;
        this.refreshJob = null;
        this.addLayerJob = null;
        toolkit.dispose();
        super.dispose();
        
        PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, patrolUpdatedListeners);
        WaypointEventManager.getInstance().removeListener(WaypointEventManager.EventType.WAYPOINT_MODIFIED, waypointModifiedListener);
        PatrolEventManager.getInstance().removeListener(PatrolEventManager.EventType.WAYPOINT_DELETED, waypointDeleteListener);
        
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
    			currentSelection = null;
    		}
    		rightStack.layout();
    	});
    	header1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
    	
    }
    
    private void initData() {
    	
    	if (patrolSummary.isDisposed()) return;
    	for (Control kid : patrolSummary.getChildren()) kid.dispose();
    	
    	patrolSummary.setLayout(new GridLayout());
    	((GridLayout)patrolSummary.getLayout()).marginWidth = 0;
    	((GridLayout)patrolSummary.getLayout()).marginHeight = 0;
    	
    	toolkit = new FormToolkit(patrolSummary.getDisplay());
    	
    	ScrolledForm scroll = toolkit.createScrolledForm(patrolSummary);
    	scroll.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    	
    	Composite scrollComposite = scroll.getBody();
    	scrollComposite.setLayout(new GridLayout());
    	((GridLayout)scrollComposite.getLayout()).marginWidth = 0;
    	((GridLayout)scrollComposite.getLayout()).marginHeight = 0;

    	//scroll.setContent(scrollComposite);
    	summaryArea = toolkit.createSection(scrollComposite, Section.TITLE_BAR);
    	summaryArea.setText(Messages.PatrolPresentationPart_SummaryAreaLabel);
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

			Patrol patrol = parentEditor.getPatrol();

			toolkit.createLabel(left, Messages.PatrolPresentationPart_StartDate);
			toolkit.createLabel(left, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(patrol.getStartDate()));
			
			toolkit.createLabel(left, Messages.PatrolPresentationPart_EndDate);
			toolkit.createLabel(left, DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(patrol.getEndDate()));
			
			toolkit.createLabel(left, Messages.PatrolPresentationPart_TotalDays);

			long days = ChronoUnit.DAYS.between(patrol.getStartDate(), patrol.getEndDate()) + 1;
			String unit = Messages.PatrolPresentationPart_daylabel;
			if (days > 1) unit = Messages.PatrolPresentationPart_dayslabel;
			String msg = MessageFormat.format("{0} {1}", days, unit ); //$NON-NLS-1$
			
			if (patrol.getLegs().size() > 1) {
				msg = MessageFormat.format(Messages.PatrolPresentationPart_DaysAndLegs, days, unit, patrol.getLegs().size());	
			}
			toolkit.createLabel(left, msg);
			
			new Label(left, SWT.NONE);
			new Label(left, SWT.NONE);
			
			
			toolkit.createLabel(left, Messages.PatrolPresentationPart_Station);
			toolkit.createLabel(left, patrol.getStation() == null ? "" : patrol.getStation().getName()); //$NON-NLS-1$
			
			
			long mandatecnt = patrol.getLegs().stream().map(leg->leg.getMandate()).distinct().count();
			if (mandatecnt > 1) {
				toolkit.createLabel(left, Messages.PatrolPresentationPart_Mandates);
			}else {
				toolkit.createLabel(left, Messages.PatrolPresentationPart_Mandate);
			}
			
			//sort legs so mandates appear in order
			List<PatrolLeg> legs = new ArrayList<>();
			legs.addAll(patrol.getLegs());
			legs.sort((a,b)->{
				if (!a.getStartDate().equals(b.getStartDate())) return a.getStartDate().compareTo(b.getStartDate());
				PatrolLegDay da = null;
				for (PatrolLegDay d : a.getPatrolLegDays()) {
					if (d.getDate().equals(a.getStartDate())) {
						da = d;
						break;
					}
				}
				PatrolLegDay db = null;
				for (PatrolLegDay d : b.getPatrolLegDays()) {
					if (d.getDate().equals(b.getStartDate())) {
						db = d;
						break;
					}
				}
				if(da != null && db!=null) return da.getStartTime().compareTo(db.getStartTime());
				return 0;
			});
			Set<PatrolMandate> mandates = new HashSet<>();

			mandates.add(legs.get(0).getMandate());
			toolkit.createLabel(left, legs.get(0).getMandate() == null ? "" : legs.get(0).getMandate().getName()); //$NON-NLS-1$
			
			for (int i = 1; i < legs.size(); i ++) {
				if (mandates.contains(legs.get(i).getMandate())) continue;
				if (legs.get(i).getMandate() == null) continue;
				new Label(left, SWT.NONE);
				toolkit.createLabel(left, legs.get(i).getMandate().getName());
				mandates.add(legs.get(1).getMandate());
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
			
			toolkit.createLabel(left, Messages.PatrolPresentationPart_TotalTime);
			toolkit.createLabel(left, PatrolEditor.formatTimeRange(totalTime));
			
			toolkit.createLabel(left, Messages.PatrolPresentationPart_ActiveTime);
			toolkit.createLabel(left, PatrolEditor.formatTimeRange(activeTime));
			
			toolkit.createLabel(left, Messages.PatrolPresentationPart_TotalDistance);
			toolkit.createLabel(left, PatrolEditor.DISTANCE_FORMATTER.format(distance) + Messages.PatrolPresentationPart_km);
			
			tdistance.keySet().stream().sorted((a,b)->Collator.getInstance().compare(a.getName(),  b.getName())).forEach(pt->{
				Label ll = toolkit.createLabel(left, pt.getName() + ":"); //$NON-NLS-1$
				ll.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
				
				toolkit.createLabel(left, PatrolEditor.DISTANCE_FORMATTER.format(tdistance.get(pt)) + Messages.PatrolPresentationPart_km);
			});
			
			Set<Employee> members = new HashSet<>();
			Set<Employee> leaders = new HashSet<>();
			for (PatrolLeg pl : patrol.getLegs()) {
				pl.getMembers().forEach(pm -> members.add(pm.getMember()));
				if (pl.getLeader() != null) leaders.add(pl.getLeader().getMember());
			}
			members.removeAll(leaders);
			
			toolkit.createLabel(right, Messages.PatrolPresentationPart_Leaders);
			
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
			
			toolkit.createLabel(right,Messages.PatrolPresentationPart_Members);
			
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
				
				Label l = toolkit.createLabel(sarea, Messages.PatrolPresentationPart_Objective);
				l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 2, 1));
				
				Text txtObj = toolkit.createText(sarea, patrol.getObjective() == null ? "" : patrol.getObjective(), SWT.MULTI | SWT.V_SCROLL | SWT.WRAP); //$NON-NLS-1$
				txtObj.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
				((GridData)txtObj.getLayoutData()).heightHint = 60;
			}
			
			
			for(IPatrolPresentationContribution contribution : contributions) {
				Composite part = contribution.createSummaryWidget(scrollComposite, patrol, session, toolkit);
				if (part != null) part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			}
			
			Set<LocalDate> dates = new HashSet<>();
			
			for (PatrolLeg leg : patrol.getLegs()) {
				for (PatrolLegDay legday : leg.getPatrolLegDays()) {
					dates.add(legday.getDate());
				}
			}
			List<LocalDate> sortedDates = dates.stream().sorted((a,b)->a.compareTo(b)).collect(Collectors.toList());
			header1.setDateRange(sortedDates);
			header1.setCurrentDate(null);
			refreshWaypointData();
    	}
    	
    	patrolSummary.layout(true);
    }
    	
    private void refreshWaypointData() {
    	
    	getSite().getShell().getDisplay().syncExec(()->tblData.setInput(null));
    	
    	
		Job j = new Job(Messages.PatrolPresentationPart_refreshjobname) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				pdata = new HashMap<>();
				
				try(Session session = HibernateManager.openSession()){
					Patrol patrol = session.get(Patrol.class, parentEditor.getPatrol().getUuid());
	
					HashMap<LocalDate, List<PatrolLegDay>> legsbyday = new HashMap<>();
	
					for (PatrolLeg leg : patrol.getLegs()) {
						// order leg days by time of first waypoint
						leg.getType().getName();
						if (leg.getMandate() != null)
							leg.getMandate().getName();
						for (PatrolLegDay legday : leg.getPatrolLegDays()) {
							List<PatrolLegDay> temp = legsbyday.get(legday.getDate());
							if (temp == null) {
								temp = new ArrayList<>();
								legsbyday.put(legday.getDate(), temp);
							}
							temp.add(legday);
						}
					}
					for (Entry<LocalDate, List<PatrolLegDay>> key : legsbyday.entrySet()) {
	
						List<Object> pdaydata = pdata.get(key.getKey());
						if (pdaydata == null) {
							pdaydata = new ArrayList<>();
							pdata.put(key.getKey(), pdaydata);
						}
	
						final List<Object> fpdaydata = pdaydata;
	
						List<PatrolLegDay> legs = key.getValue();
						legs.sort((a, b) -> a.getStartTime().compareTo(b.getStartTime()));
	
						for (PatrolLegDay legday : legs) {
							if (patrol.getLegs().size() > 1) {
								pdaydata.add(legday.getPatrolLeg());
							}
	
							legday.getWaypoints().stream().sorted(
									(a, b) -> a.getWaypoint().getDateTime().compareTo(b.getWaypoint().getDateTime()))
									.forEach(pw -> {
										pw.getWaypoint().getAttachments().size();
										if (pw.getWaypoint().getAllObservations().isEmpty()) {
											fpdaydata.add(pw.getWaypoint());
										} else {
											pw.getWaypoint().getAllObservations()
													.forEach(wp -> {
														wp.getCategory().getFullCategoryName();
														wp.getCategory().getAllAttribute(new ArrayList<>(), null);
													});
											List<WaypointObservation> obs = pw.getWaypoint().getAllObservations();
											obs.forEach(oo -> oo.getAttributes().forEach(a -> {
												a.getAttribute().getName();
												a.getAttributeValueAsString(Locale.getDefault());
											}));
											obs.forEach(oo -> oo.getAttachments().size());
											FirstWaypointObservation first = new FirstWaypointObservation(obs.get(0));
											fpdaydata.add(first);
											for (int i = 1; i < obs.size(); i++)
												fpdaydata.add(obs.get(i));
	
										}
									});
						}
					}
				}
				
				getSite().getShell().getDisplay().syncExec(()->tblData.setInput(pdata.get(header1.getCurrentDate())));
				currentSelection = null;
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();

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
	
	private static Waypoint findWaypoint(Object x) {
		if (x instanceof Waypoint) {
			return (Waypoint)x;
		}else if (x instanceof WaypointObservation) {
			return ((WaypointObservation)x).getWaypoint();
		}else if (x instanceof FirstWaypointObservation) {
			return ((FirstWaypointObservation)x).wo.getWaypoint();
		}
		return null;
	}
	
	private static WaypointObservation findObservation(Object value) {
		if (value instanceof WaypointObservation) {
			return (WaypointObservation) value;
		}else if (value instanceof FirstWaypointObservation) {
			return ((FirstWaypointObservation)value).wo;
		}
		return null;
	}
	
	private enum Column{
		ID(Messages.PatrolPresentationPart_IdColumn),
		X(Messages.PatrolPresentationPart_xcolumn),
		Y(Messages.PatrolPresentationPart_ycolumn),
		TIME(Messages.PatrolPresentationPart_timecolumn),
		CATEGORY(Messages.PatrolPresentationPart_categorycolumn),
		ATTACHMENTS(Messages.PatrolPresentationPart_attachmentscolumn);
		
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
		public String getTooltip(Object value) {
			if (this == Column.CATEGORY) {
				WaypointObservation wo = findObservation(value);
				if (wo == null) return null;
				StringBuilder sb = new StringBuilder();
				
				sb.append(SmartUtils.formatStringForLabel(wo.getCategory().getFullCategoryName()));
				
				if (!wo.getAttributes().isEmpty()) {
					List<WaypointObservationAttribute> sortedAtts = wo.getAttributesSorted();
					
					sb.append("\n\n"); //$NON-NLS-1$
					for (WaypointObservationAttribute a : sortedAtts) {
						sb.append(MessageFormat.format("{0}: {1}", a.getAttribute().getName(), a.getAttributeValueAsString(Locale.getDefault()))); //$NON-NLS-1$
						sb.append("\n"); //$NON-NLS-1$
					}
					sb.deleteCharAt(sb.length() - 1);
				}			
				
				if (!wo.getAttributes().isEmpty()) sb.deleteCharAt(sb.length() - 1);
				return sb.toString();
			}
			
			return null;
		}
		
		
		public String getValue(Object value, Projection viewProjection) {
			if (value == null) return ""; //$NON-NLS-1$
			
			WaypointObservation wo = findObservation(value);
			Waypoint wp = null;
			if (value instanceof Waypoint) {
				wp = (Waypoint)value;
			}
			
			Double x = null;
			Double y = null;
			
			switch(this) {
			case ID:
				if (value instanceof FirstWaypointObservation) return wo.getWaypoint().getId();
				if (wp != null) return wp.getId();
				return ""; //$NON-NLS-1$
			case TIME:
				if (value instanceof FirstWaypointObservation) return DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format( wo.getWaypoint().getDateTime().toLocalTime() );
				if (wp != null) return DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM).format( wp.getDateTime().toLocalTime() );
				return ""; //$NON-NLS-1$
			case X:
				
				if (value instanceof FirstWaypointObservation) {
					y = wo.getWaypoint().getY();
					x = wo.getWaypoint().getX();
				}else if (wp != null) {
					y = wp.getY();
					x = wp.getX();
				}
				if (x != null && y != null) {
					return String.valueOf(ReprojectUtils.transform(x, y, viewProjection.getParsedCoordinateReferenceSystem()).getX());
				}
				return ""; //$NON-NLS-1$
			case Y:
				if (value instanceof FirstWaypointObservation) {
					y = wo.getWaypoint().getY();
					x = wo.getWaypoint().getX();
				}else if (wp != null) {
					y = wp.getY();
					x = wp.getX();
				}
				if (x != null && y != null) {
					return String.valueOf(ReprojectUtils.transform(x, y, viewProjection.getParsedCoordinateReferenceSystem()).getY());
				}
				return ""; //$NON-NLS-1$
			case CATEGORY:
				if (wo != null) return wo.getCategory().getName();
				return ""; //$NON-NLS-1$
			case ATTACHMENTS:
				int cnt = 0;
				Waypoint tmp = findWaypoint(value);
				if (tmp != null) cnt += tmp.getAttachments().size();
				if (wo != null) cnt += wo.getAttachments().size();
				if (cnt > 1) {
					return MessageFormat.format(Messages.PatrolPresentationPart_attachmentslabel, cnt);
				}else if (cnt == 1) {
					return MessageFormat.format(Messages.PatrolPresentationPart_singleattachmentslabel, cnt);
				}
				return ""; //$NON-NLS-1$
			}
			return ""; //$NON-NLS-1$
		}
		
	}
	
	class FirstWaypointObservation implements IAdaptable{
		WaypointObservation wo;
		
		public FirstWaypointObservation(WaypointObservation wo) {
			this.wo = wo;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter.equals(Waypoint.class)) return (T) wo.getWaypoint();
			return null;
		}
	}
	
	class WaypointLayerStyleBlackboard extends StyleBlackboardImpl {

		public WaypointLayerStyleBlackboard(StyleBlackboard current) {
			super();
			for (StyleEntry i : current.getContent() ) {
				super.put(i.getID(), i.getStyle());
			}
		}
		
		@Override
		public Object lookup(Class<?> theClass) {
			Object x = get(SelectionStyleContent.ID);
			super.remove(SelectionStyleContent.ID);
			Object value = super.lookup(theClass);
			if (x != null) super.put(SelectionStyleContent.ID, x);
			return value;
		}
		
	}

}


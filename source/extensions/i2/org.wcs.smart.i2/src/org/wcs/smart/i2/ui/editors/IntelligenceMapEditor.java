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
package org.wcs.smart.i2.ui.editors;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.internal.ui.IDropTargetProvider;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.LayerEvent;
import org.locationtech.udig.project.LayerEvent.EventType;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.internal.render.RenderPackage;
import org.locationtech.udig.project.internal.render.ViewportModel;
import org.locationtech.udig.project.ui.AnimationUpdater;
import org.locationtech.udig.project.ui.ApplicationGIS;
import org.locationtech.udig.project.ui.commands.IDrawCommand;
import org.locationtech.udig.project.ui.internal.FeatureAnimation;
import org.locationtech.udig.project.ui.internal.MapPart;
import org.locationtech.udig.project.ui.internal.commands.draw.DrawFeatureCommand;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseEvent;
import org.locationtech.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import org.locationtech.udig.project.ui.tool.IMapEditorSelectionProvider;
import org.locationtech.udig.project.ui.tool.IToolManager;
import org.locationtech.udig.project.ui.viewers.MapViewer;
import org.locationtech.udig.ui.IBlockingSelection;
import org.locationtech.udig.ui.UDIGDragDropUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.Filter;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.udig.ContentFilterLayerImpl;
import org.wcs.smart.i2.udig.IWorkingSetResource;
import org.wcs.smart.i2.udig.entity.IntelEntityDataSource;
import org.wcs.smart.i2.udig.query.QueryService;
import org.wcs.smart.i2.udig.query.QueryService.State;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.views.LayerVisibleEvent;
import org.wcs.smart.i2.ui.views.WorkingSetView;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.ProjectionDialog;
import org.wcs.smart.ui.map.ScaleRatioComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Intelligence working set map.  Implementing as an editor but we only
 * ever want one of these.
 * 
 * 
 */
public class IntelligenceMapEditor extends EditorPart implements MapPart, IDropTargetProvider {

	public static final String ID = "org.wcs.smart.i2.editor.map"; //$NON-NLS-1$
	
	private IEclipseContext parentContext;
	
	protected MapViewer mapViewer;
	private Label lblCoordinates;
	private Button lblSRID;
	protected MapToolComposite tools;
	private FlashFeatureListener selectFeatureListener = new FlashFeatureListener();
    private boolean flashFeatureRegistered = false;
    private List<EventHandler> handlers = null;
    private WorkingSetMapLayersJob configureLayersJob = null;
    private WorkingSetQueryLayersJob queryLayersJob = null;
    
    private boolean handlingLayerVisibility = false;
    
    private Date[] refreshMapJobDates = null;;
    private Job refreshMapJob = new Job(Messages.IntelligenceMapEditor_refreshmapjob){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Date[] newDates = refreshMapJobDates;
			if (newDates != null){
				Filter udigDateFilter = IntelEntityDataSource.createDateFilter(newDates[0], newDates[1]);
				boolean refresh = false;
				for (Layer l : getMap().getLayersInternal()){
					Boolean x = (Boolean) l.getBlackboard().get(WorkingSetMapLayersJob.WS_MAP_LAYER_KEY);
					if (x == null || !x) continue;
					if (!l.getGeoResource().canResolve(IWorkingSetResource.class)) continue;
					
					if (l instanceof ContentFilterLayerImpl){
						((ContentFilterLayerImpl) l).setContentFilter(udigDateFilter);
						refresh = true;
					}
					if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				}
				if (monitor.isCanceled()) return Status.CANCEL_STATUS;
				rerunQueryLayers();
				if (refresh) getMap().getRenderManager().refresh(null);
			}
			return Status.OK_STATUS;
		}
    	
    };
		
    private ILayerListener visibilityListener = new ILayerListener() {

		@Override
		public void refresh(LayerEvent event) {
			if (event.getType() != EventType.VISIBILITY) return;
			if(handlingLayerVisibility) return;
			try{
				handlingLayerVisibility = true;
				
				ILayer layer = event.getSource();
				if (layer == null) return;
				
				Object x = layer.getBlackboard().get(WorkingSetMapLayersJob.WS_MAP_LAYER_KEY);
				if (x == null) return; //not a working set layer
				if (!((Boolean)x)) return; //not a working set layer
				if (!layer.getGeoResource().canResolve(IWorkingSetResource.class)) return;
				
				IWorkingSetResource resource = null;
				try {
					resource = layer.getGeoResource().resolve(IWorkingSetResource.class, null);
				} catch (IOException e) {
					Intelligence2PlugIn.log(e.getMessage(), e);
				}
				if (resource == null) return;
				boolean visibility = layer.isVisible();
				
				boolean allVisible = true;
				LayerVisibleEvent newevent = new  LayerVisibleEvent();
				
				List<IGeoResource> allItems = null;
				try{
					allItems = (List<IGeoResource>) layer.getGeoResource().resolve(IService.class, null).resources(null);
				}catch (Exception ex){
					Intelligence2PlugIn.log(ex.getMessage(), ex);
				}
				if (allItems != null){
					for (IGeoResource rr : allItems){
						for (Layer l : getMap().getLayersInternal()){
							if (l.getGeoResource().getID().equals(rr.getID())){
								if (l.isVisible() != visibility){
									allVisible = false;
								}
								break;
							}
						}
						if (!allVisible) break;
					}
				}
				if (allVisible){
					if (!visibility){
						newevent.notVisible.add(resource.getResourceId());
					}else{
						newevent.allVisible.add(resource.getResourceId());
					}
				}else{
					newevent.partVisible.add(resource.getResourceId());
				}
				parentContext.get(IEventBroker.class).send(IntelEvents.ACTIVE_WS_LAYER_VISIBILITY, newevent);
			}finally{
				handlingLayerVisibility = false;
			}
			
		}
    	
    };
	public static IEditorInput MAPINPUT = new IEditorInput() {
		
		@Override
		public Object getAdapter(Class adapter) {
			return null;
		}
		
		@Override
		public String getToolTipText() {
			return null;
		}
		
		@Override
		public IPersistableElement getPersistable() {
			return null;
		}
		
		@Override
		public String getName() {
			return Messages.IntelligenceMapEditor_MapName;
		}
		
		@Override
		public ImageDescriptor getImageDescriptor() {
			return SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.MAP_ICON);
		}
		
		@Override
		public boolean exists() {
			return false;
		}
	}; 
	

	
	IPartListener2 partlistener = new IPartListener2(){
	        public void partActivated( IWorkbenchPartReference partRef ) {
	            if (partRef.getPart(false) == IntelligenceMapEditor.this) {
	            	
	                IToolManager toolManager = ApplicationGIS.getToolManager();
	                toolManager.setCurrentEditor( IntelligenceMapEditor.this.mapViewer );
	                IntelligenceMapEditor.this.tools.selectLastTool();
	                
	                //make sure the table does not display the close button
	                //I tried this with css styles but it didn't work properly; the 
	                //active editor ended up with the wrong styling
	                MPart part = parentContext.get(MPart.class);
	        		CTabFolder folder = (CTabFolder)part.getParent().getWidget();
	        		for (CTabItem item : folder.getItems()){
	        			if (item.getControl() == part.getWidget()){
	        				item.setShowClose(false);
	        				break;
	        			}			
	        		}
	            }
	        }

	        public void partBroughtToTop( IWorkbenchPartReference partRef ) {
	        }

	        public void partClosed( IWorkbenchPartReference partRef ) {
	        }

	        public void partDeactivated( IWorkbenchPartReference partRef ) {
	        }

	        public void partOpened( IWorkbenchPartReference partRef ) {
	        }

	        public void partHidden( IWorkbenchPartReference partRef ) {
	        	if (partRef.getPart(false) == IntelligenceMapEditor.this) {
	        		deregisterFeatureFlasher();
	        	}
	        }

	        public void partVisible( IWorkbenchPartReference partRef ) {
	        	if (partRef.getPart(false) == IntelligenceMapEditor.this) {
	        		registerFeatureFlasher();
	        	}
	        }

	        public void partInputChanged( IWorkbenchPartReference partRef ) {
	        }

	    };
	    
	  

    /**
     * registers a listener with the current page that flashes a feature each time the current
     * selected feature changes.
     */
    protected synchronized void registerFeatureFlasher() {
        if (!flashFeatureRegistered) {
            flashFeatureRegistered = true;
            IWorkbenchPage page = getSite().getPage();
            page.addPostSelectionListener(selectFeatureListener);
        }
    }

    protected synchronized void deregisterFeatureFlasher() {
        flashFeatureRegistered = false;
        //AnimationUpdater.cancel(getMap().getRenderManager().getMapDisplay());
        getSite().getPage().removePostSelectionListener(selectFeatureListener);
    }
    
	
	/**
	 * Does nothing; there is nothing to save.
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	public void doSave(IProgressMonitor monitor) {
		
	}


	/** Does nothing; there is nothing to save.
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	@Override
	public void doSaveAs() {
	}

	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		setSite(site);
		setInput(input);
	}

	/**
	 * Nothing to save; always not dirty.
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 * @return <code>false</code>
	 */
	@Override
	public boolean isDirty() {
		return false;
	}

	/**
	 * Nothing to save.
	 * 
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 * @return <code>false</code>
	 */
	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		handlers = new ArrayList<EventHandler>();
		
		
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
		MPart part = parentContext.get(MPart.class);
		//disable close button on map editor
		part.setCloseable(false);
		part.getTags().add(E3Utils.DO_NOT_CLOSE_TAG);

		//configure tags so editors show in both perspectives
		if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
		if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);
		//part.get
		EventHandler handler = new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				List<ILayer> toDelete = new ArrayList<>();
				for (ILayer l : getMap().getMapLayers()){
					Object x = l.getBlackboard().get(WorkingSetMapLayersJob.WS_MAP_LAYER_KEY) ;
					if (x != null && ((Boolean)x)) toDelete.add(l);
				}
				WorkingSetMapLayersJob.removeLayers(getMap(), toDelete);
				setWorkingSet();
			}
		};
		parentContext.get(IEventBroker.class).subscribe(IntelEvents.ACTIVE_WS_SET, handler);
		handlers.add(handler);
		
		handler = new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				IntelWorkingSet set = (IntelWorkingSet) event.getProperty(IEventBroker.DATA);
				if (set != null && set.getUuid().equals(WorkingSetManager.INSTANCE.getActiveWorkingSet())){
					setWorkingSet();
				}
			}
		};
		parentContext.get(IEventBroker.class).subscribe(IntelEvents.WS_MODIFIED, handler);
		handlers.add(handler);
		
		
		handler = new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				Object data = (Object) event.getProperty(IEventBroker.DATA);
				List<IntelRecordObservationQuery> queries = Collections.emptyList();
				if (data instanceof IntelRecordObservationQuery){
					queries = Collections.singletonList((IntelRecordObservationQuery)data);
				}else if (data instanceof List){
					queries = (List<IntelRecordObservationQuery>) data;
				}
				//rerun query
				if (queryLayersJob != null){
					WorkingSetQueryLayersJob refreshJob = queryLayersJob.clone();
					refreshJob.setQueriesToUpdate(queries);
					refreshJob.schedule();
				}
			}
		};
		parentContext.get(IEventBroker.class).subscribe(IntelEvents.QUERY_MODIFIED, handler);
		handlers.add(handler);
		
		
		handler = new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				//refresh map
				//only if the record is part of the working set
				if (!WorkingSetManager.INSTANCE.isSet()) return;
				boolean refresh = false;
				Session s = HibernateManager.openSession();
				try{
					IntelWorkingSet set = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
					Object data = (Object) event.getProperty(IEventBroker.DATA);
					Collection<?> items= null;
					if (data instanceof IntelRecord){
						items = Collections.singletonList(data);
					}else if(data instanceof IntelEntity){
						items = Collections.singletonList(data);
					}else if (data instanceof Collection){
						items = (Collection<?>) data;
					}else{
						return;
					}
					for (IntelWorkingSetRecord r : set.getRecords()){
						if (items.contains(r.getRecord())){
							refresh = true;
							break;
						}
					}
					if (!refresh){
						for (IntelWorkingSetEntity r : set.getEntities()){
							if (items.contains(r.getEntity())){
								refresh = true;
								break;
							}
						}	
					}
					
				}finally{
					s.close();
				}
				if (refresh){
					mapViewer.getRenderManager().refresh(null);
				}
				
			}
		};
		parentContext.get(IEventBroker.class).subscribe(IntelEvents.RECORD_MODIFIED, handler);
		parentContext.get(IEventBroker.class).subscribe(IntelEvents.ENTITY_MODIFIED, handler);
		handlers.add(handler);
		
		handler = new EventHandler() {
			
			@Override
			public void handleEvent(Event event) {
				if (handlingLayerVisibility) return;
				handlingLayerVisibility = true;
				try{
					LayerVisibleEvent visibleLayers = (LayerVisibleEvent) event.getProperty(IEventBroker.DATA);
					getMap().getRenderManagerInternal().disableRendering();
					try{
						for (Layer l : getMap().getLayersInternal()){
							Boolean x = (Boolean) l.getBlackboard().get(WorkingSetMapLayersJob.WS_MAP_LAYER_KEY);
							if (x == null || !x) continue;
							if (!l.getGeoResource().canResolve(IWorkingSetResource.class)) continue;
							try {
								UUID uuid = (l.getGeoResource().resolve(IWorkingSetResource.class, null)).getResourceId();
								if (visibleLayers.allVisible.contains(uuid)){
									l.setVisible(true);
								}
								if (visibleLayers.notVisible.contains(uuid)){
									l.setVisible(false);
								}
								
							} catch (IOException e) {
								e.printStackTrace();
							}
						}	
					}finally{
						getMap().getRenderManagerInternal().enableRendering();
					}
				}finally{
					handlingLayerVisibility = false;
				}
			}
		};
		parentContext.get(IEventBroker.class).subscribe(IntelEvents.ACTIVE_WS_LAYER_VISIBILITY, handler);
		handlers.add(handler);
		
		
		handler = new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				refreshMapJobDates = (Date[]) event.getProperty(IEventBroker.DATA);
				refreshMapJob.cancel();
				refreshMapJob.schedule(200);
			}
		};
		parentContext.get(IEventBroker.class).subscribe(IntelEvents.ACTIVE_WS_LAYER_DATEFILTER, handler);
		handlers.add(handler);
		
		GridLayout layout = new GridLayout(1,false);
    	layout.marginBottom=0;
    	layout.marginHeight = 0;
    	layout.marginLeft = 0;
    	layout.marginRight = 0;
    	layout.marginTop = 0;
    	layout.marginWidth = 0;
        parent.setLayout(layout);
        
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		layout = new GridLayout(2,false);
    	layout.marginBottom=0;
    	layout.marginHeight = 0;
    	layout.marginLeft = 0;
    	layout.marginRight = 0;
    	layout.marginTop = 0;
    	layout.marginWidth = 0;
    	layout.horizontalSpacing = 0;
    	layout.verticalSpacing = 2;
        parent.setLayout(layout);
		composite.setLayout(layout);

        mapViewer = new MapViewer(composite,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
        mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Map map = (Map) ProjectFactory.eINSTANCE.createMap();
        map.setName(getEditorInput().getName());
        mapViewer.setMap(map);
        //set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);
	      
        ApplicationGIS.getToolManager().setCurrentEditor(this);
        
		tools = new MapToolComposite();
		tools.createComposite(composite);
		tools.selectTool("org.locationtech.udig.tools.Pan"); //$NON-NLS-1$
		
        Composite infoArea = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(5, false);
        gl.marginTop = gl.marginBottom = gl.marginHeight= 0;
        gl.marginRight = 0;
        gl.marginWidth = 0;
        infoArea.setLayout(gl);
        infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,2, 1));
        lblCoordinates = new Label(infoArea, SWT.NONE);
        lblCoordinates.setText(SmartMapEditorPart.COORDINATE_LABEL);
        lblCoordinates.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        lblCoordinates.setAlignment(SWT.RIGHT);
        
        Label lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
        GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        lblSeparator.setLayoutData(gd);
        
        ScaleRatioComposite scale = new ScaleRatioComposite(infoArea, getMap());
        scale.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, false, false));
        
        
        lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
        gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        lblSeparator.setLayoutData(gd);
        
        lblSRID = new Button(infoArea, SWT.NONE);
        lblSRID.setText(map.getViewportModel().getCRS().getName().getCode());
        lblSRID.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
        lblSRID.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				ProjectionDialog pd = new ProjectionDialog(getSite().getShell(), mapViewer.getMap().getViewportModel().getCRS());
				if (pd.open() == IDialogConstants.OK_ID){
					try{
						ChangeCRSCommand command = new ChangeCRSCommand(
								ReprojectUtils.stringToCrs(pd.getSelection().getDefinition()));
						getMap().sendCommandASync(command);
					}catch (Exception ex){
						SmartPlugIn.displayLog(SmartMapEditorPart.ERROR_SETTING_MAP_PROJECTION + ex.getLocalizedMessage(), ex);
					}	
				}
			}
		});
        
        final Map thisMap = map;
        map.getViewportModelInternal().eAdapters().add(new AdapterImpl(){
        	public void notifyChanged(Notification notification) {
        		if (notification.getEventType() == Notification.SET &&
        				notification.getFeatureID(thisMap.getViewportModelInternal().getClass()) == RenderPackage.VIEWPORT_MODEL__CRS){
        			updateLabel();
        		}
        	}
        });
        
        mapViewer.getViewport().addMouseMotionListener(new MapMouseMotionListener() {
			@Override
			public void mouseMoved(MapMouseEvent event) {
				event.getPoint();
				Coordinate c = mapViewer.getMap().getViewportModelInternal().pixelToWorld(event.x, event.y);
				lblCoordinates.setText(format(c.x) + SmartMapEditorPart.COORDINATE_XYSEPARATOR + format(c.y));
			}
			
			private String format(double d){
				 DecimalFormat format = (DecimalFormat) NumberFormat.getNumberInstance();
		         format.setMaximumFractionDigits(4);
		         format.setMinimumIntegerDigits(1);
		         format.setGroupingUsed(false);
		         String string = format.format(d);
		         return string;
			}
			@Override
			public void mouseHovered(MapMouseEvent event) {
			}
			
			@Override
			public void mouseDragged(MapMouseEvent event) {
			}
		});   
        mapViewer.init(this);
 
        
        getSite().getWorkbenchWindow().getPartService().addPartListener(partlistener);
        registerFeatureFlasher();

        UDIGDragDropUtilities.addDropSupport(mapViewer.getViewport().getControl(), this);
        
        (new LoadDefaultLayersJob(getMap())).schedule();
        
		//initialize from preference store
		String uuid = Intelligence2PlugIn.getDefault().getPreferenceStore().getString(WorkingSetView.LAST_WS_PREFERENCE + UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid()));
		if (uuid != null && !uuid.isEmpty()){
		
			Job loadWs = new Job("loading working set"){ //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					UUID wset = UuidUtils.stringToUuid(uuid);
					IntelWorkingSet ws = null;
					Session s = HibernateManager.openSession();
					try{
						ws = (IntelWorkingSet)s.createCriteria(IntelWorkingSet.class)
								.add(Restrictions.eq("uuid", wset)) //$NON-NLS-1$
								.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
								.uniqueResult();
					}finally{
						s.close();
					}
					try {
						WorkingSetManager.INSTANCE.setActiveWorkingSet(ws, parentContext);
					} catch (Exception e) {
						Intelligence2PlugIn.log(e.getMessage(), e);
					}
					return Status.OK_STATUS;
				}
					
			};
			loadWs.setSystem(true);
			loadWs.schedule();
			
		}
        
	}

	@Override
	public Object getTarget(DropTargetEvent event) {
		return this;
	}
	
	private synchronized void setWorkingSet(){
		if (configureLayersJob == null){
			configureLayersJob = new WorkingSetMapLayersJob(getMap(), parentContext, visibilityListener);
			configureLayersJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					getWorkingSetQueryLayersJob().schedule();			
				}
			});
		}
		configureLayersJob.schedule();
		
	}
	
	private void rerunQueryLayers(){
		//reset the state of the layers
		//then refresh
		for (ILayer l : getMap().getMapLayers()){
			if (l.getGeoResource().canResolve(QueryService.class)){
				try{
					QueryService currentService = l.getGeoResource().resolve(QueryService.class, new NullProgressMonitor());
					currentService.setState(State.NO_RESULTS);						
				}catch (Exception ex){
					ex.printStackTrace();
				}
				
			}
		}
		WorkingSetQueryLayersJob j = getWorkingSetQueryLayersJob();
		j.cancel();
		getWorkingSetQueryLayersJob().schedule(100);
	}
	
	private synchronized WorkingSetQueryLayersJob getWorkingSetQueryLayersJob(){
		if (queryLayersJob == null){
			queryLayersJob = new WorkingSetQueryLayersJob(getMap(), parentContext, visibilityListener);
		}
		return queryLayersJob;
	}
	
	private void updateLabel() {
		getSite().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (lblSRID == null || lblSRID.isDisposed()) return;
				lblSRID.setText(getMap().getViewportModel().getCRS().getName()
						.getCode());
				lblSRID.getParent().layout();
			}
		});

	}
        
    public Map getMap() {
        return mapViewer.getMap();
    }
	
    @Override
    public void dispose() {
        super.dispose();
        deregisterFeatureFlasher();
        getSite().getWorkbenchWindow().getPartService().removePartListener(partlistener);
        
        this.partlistener = null;
        this.selectFeatureListener = null;
        
        if (mapViewer != null && mapViewer.getViewport() != null && getMap() != null) {
        	mapViewer.getViewport().removePaneListener(getMap().getViewportModelInternal());
        }
        if (getMap() != null){
                getMap().getViewportModelInternal().setInitialized(false);
        }
        if (mapViewer != null){
        	if ( mapViewer.getRenderManager() != null){
        		mapViewer.getRenderManager().disableRendering();
        		mapViewer.getRenderManager().stopRendering();
        		mapViewer.getRenderManager().dispose();
        	}
        	mapViewer.dispose();
        }
        
        IEventBroker events = parentContext.get(IEventBroker.class);
        if (handlers != null) handlers.forEach(h -> events.unsubscribe(h));
    }

    public void openContextMenu() {
    	mapViewer.openContextMenu();
    }

    public void setFont( Control control ) {
    	mapViewer.setFont(control);
    }

    public void setSelectionProvider( IMapEditorSelectionProvider selectionProvider ) {
    	if (selectionProvider == null) {
            throw new NullPointerException("selection provider must not be null!"); //$NON-NLS-1$
        }
    	selectionProvider.setActiveMap(mapViewer.getMap(), mapViewer);
    	mapViewer.setSelectionProvider(selectionProvider);
        
    }

	

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	@Override
	public void setFocus() {
		mapViewer.getViewport().getControl().setFocus();
	}

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return ((IEditorSite)getSite()).getActionBars().getStatusLineManager();		
	}
	
	
	private class FlashFeatureListener implements ISelectionListener {

		
        public void selectionChanged( IWorkbenchPart part, final ISelection selection ) {
            if (part == IntelligenceMapEditor.this || getSite().getPage().getActivePart() != part
                    || selection instanceof IBlockingSelection)
                return;
            
            ISafeRunnable sendAnimation = new ISafeRunnable(){
                public void run() {
                    if (selection instanceof IStructuredSelection) {
                        IStructuredSelection s = (IStructuredSelection) selection;
                        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
                        for( Iterator<?> iter = s.iterator(); iter.hasNext(); ) {
                            Object element = iter.next();

                            if (element instanceof SimpleFeature) {
                                SimpleFeature feature = (SimpleFeature) element;
                                features.add(feature);
                            }
                        }
                        if (features.size() == 0)
                            return;
                        if (!mapViewer.getRenderManager().isDisposed()) {
                        	FeatureAnimation anim = createAnimation(features);
                            if (anim != null){
                                AnimationUpdater.runTimer(getMap().getRenderManager().getMapDisplay(), anim);
                            }
                            
                        }
                    }
                }
                public void handleException( Throwable exception ) {
                	SmartPlugIn.log("Exception preparing animation", exception); //$NON-NLS-1$
                }
            };

            try {
                sendAnimation.run();
            } catch (Exception e) {
            	SmartPlugIn.log("", e); //$NON-NLS-1$
            }
        }

        private FeatureAnimation createAnimation( List<SimpleFeature> current ) {
            final List<IDrawCommand> commands = new ArrayList<IDrawCommand>();
            for( SimpleFeature feature : current ) {
                if (feature == null || feature.getFeatureType().getGeometryDescriptor() == null)
                    continue;
                DrawFeatureCommand command = null;
                if (feature instanceof IAdaptable) {
                    Layer layer = (Layer) ((IAdaptable) feature).getAdapter(Layer.class);
                    if (layer != null)
                        try {
                            command = new DrawFeatureCommand(feature, layer);
                        } catch (IOException e) {
                            // do nothing... thats life
                        	e.printStackTrace();
                        }
                }
                if (command == null) {
                    command = new DrawFeatureCommand(feature);
                }
                command.setMap(getMap());
                commands.add(command);
            }
            Rectangle2D rect = new Rectangle();
            
            final Rectangle validArea = (Rectangle) rect;
            FeatureAnimation anim = new FeatureAnimation(commands, validArea);
            return anim;
        }
    }
	

}
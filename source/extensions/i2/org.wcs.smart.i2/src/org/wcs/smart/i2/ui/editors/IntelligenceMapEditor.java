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
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
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
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.internal.ui.IDropTargetProvider;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
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
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.WorkingSetManager;
import org.wcs.smart.i2.event.IntelEvents;
import org.wcs.smart.i2.model.IntelWorkingSet;
import org.wcs.smart.i2.model.IntelWorkingSetEntity;
import org.wcs.smart.i2.model.IntelWorkingSetRecord;
import org.wcs.smart.i2.udig.AddContentFilterLayersCommand;
import org.wcs.smart.i2.udig.entity.IntelEntityService;
import org.wcs.smart.i2.udig.entity.IntelEntityServiceExtension;
import org.wcs.smart.i2.udig.record.IntelRecordService;
import org.wcs.smart.i2.udig.record.IntelRecordServiceExtension;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.ProjectionDialog;
import org.wcs.smart.ui.map.ScaleRatioComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

/*
 * implementing as an editor; but we only ever want one 
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
			return "Map";
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
		
		//configure tags so editors show in both perspectives
		parentContext = (IEclipseContext) getSite().getService(IEclipseContext.class);
		MPart part = parentContext.get(MPart.class); 
		if (!part.getTags().contains(IntelDataAssessmentPerspective.ID)) part.getTags().add(IntelDataAssessmentPerspective.ID);
		if (!part.getTags().contains(IntelDataAnalysisPerspective.ID)) part.getTags().add(IntelDataAnalysisPerspective.ID);
		
		EventHandler handler = new EventHandler() {
			@Override
			public void handleEvent(Event event) {
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
	}

	@Override
	public Object getTarget(DropTargetEvent event) {
		return this;
	}
	
	private void setWorkingSet(){
		Job setWorkingset  = new Job("set working set"){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				IntelWorkingSet workingset = null;
				Session s = HibernateManager.openSession();
				try{
					workingset = (IntelWorkingSet) s.get(IntelWorkingSet.class, WorkingSetManager.INSTANCE.getActiveWorkingSet());
					
					if (workingset.getEntities() != null){
						workingset.getEntities().forEach(e->e.getEntity().getIdAttributeAsText());
					}
					if (workingset.getRecords() != null){
						workingset.getRecords().forEach(e->e.getRecord().getTitle());
					}
					if (workingset.getQueries() != null){
						workingset.getQueries().forEach(e->e.getQuery().getName());
					}
				}finally{
					s.close();
				}
				
				//remove all layers from map
				List<ILayer> layersToRemove = new ArrayList<ILayer>();
				for (ILayer l : getMap().getLayersInternal()){
					Object x = l.getBlackboard().get("org.wcs.smart.i2.ws.map.wslayer") ;
					if (x != null && ((Boolean)x)){
						layersToRemove.add(l);
					}
				}
				if (!layersToRemove.isEmpty()){
					DeleteLayersCommand cmd = new DeleteLayersCommand(layersToRemove.toArray(new ILayer[layersToRemove.size()]));
					getMap().sendCommandASync(cmd);
				}
				
				List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
				for (IntelWorkingSetEntity entity: workingset.getEntities()){
					HashMap<String, Serializable> params = new HashMap<String,Serializable>();
					params.put(IntelEntityServiceExtension.ENTITY_UUID_KEY, UuidUtils.uuidToString(entity.getEntity().getUuid()));
					IntelEntityService service = new IntelEntityService(params);
					try {
						toAdd.addAll(service.resources(monitor));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				AddContentFilterLayersCommand addCmd = new AddContentFilterLayersCommand(toAdd, 1){
					 public void run( IProgressMonitor monitor ) throws Exception {
							 super.run(monitor);
							 for (Layer layer : getLayers()){
								 layer.getBlackboard().put("org.wcs.smart.i2.ws.map.wslayer", Boolean.TRUE);
							 }
						 }
					};
				getMap().sendCommandASync(addCmd);
				
				
				toAdd = new ArrayList<IGeoResource>();
				for (IntelWorkingSetRecord record: workingset.getRecords()){
					HashMap<String, Serializable> params = new HashMap<String,Serializable>();
					params.put(IntelRecordServiceExtension.RECORD_UUID_KEY, UuidUtils.uuidToString(record.getRecord().getUuid()));
					IntelRecordService service = new IntelRecordService(params);
					try {
						toAdd.addAll(service.resources(monitor));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
				
				AddLayersCommand addCmd2 = new AddLayersCommand(toAdd, 1){
					 public void run( IProgressMonitor monitor ) throws Exception {
							 super.run(monitor);
							 for (Layer layer : getLayers()){
								 layer.getBlackboard().put("org.wcs.smart.i2.ws.map.wslayer", Boolean.TRUE);
							 }
						 }
					};
				getMap().sendCommandASync(addCmd2);
				return Status.OK_STATUS;
			}
		};
		setWorkingset.schedule();
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
        
        getMap().getViewportModelInternal().setInitialized(false);
        mapViewer.getRenderManager().disableRendering();
        mapViewer.getRenderManager().stopRendering();
       	mapViewer.getRenderManager().dispose();

        mapViewer.dispose();
        
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
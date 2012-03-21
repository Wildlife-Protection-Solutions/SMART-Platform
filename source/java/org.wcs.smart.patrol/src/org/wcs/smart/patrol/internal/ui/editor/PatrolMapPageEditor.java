/*
 * Copyright (C) 2012 Wildlife Conservation Society
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

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.command.navigation.ZoomExtentCommand;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.selection.NoSelectCommand;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.render.IViewportModelListener;
import net.refractions.udig.project.render.ViewportModelEvent;
import net.refractions.udig.project.ui.AnimationUpdater;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.IAnimation;
import net.refractions.udig.project.ui.commands.IDrawCommand;
import net.refractions.udig.project.ui.internal.FeatureAnimation;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.internal.commands.draw.DrawFeatureCommand;
import net.refractions.udig.project.ui.internal.tool.ToolContext;
import net.refractions.udig.project.ui.internal.tool.impl.ToolContextImpl;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.tool.IToolManager;
import net.refractions.udig.project.ui.tool.ModalTool;
import net.refractions.udig.project.ui.tool.Tool;
import net.refractions.udig.project.ui.viewers.MapViewer;
import net.refractions.udig.ui.IBlockingSelection;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.EditorPart;
import org.opengis.feature.simple.SimpleFeature;
import org.wcs.smart.ca.Area;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolEventManager.EventType;
import org.wcs.smart.patrol.PatrolEventManager.IPatrolEventListener;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolLegDay;
import org.wcs.smart.patrol.udig.catalog.PatrolService;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Page for the editor for displaying a map
 * of the waypoints and tracks.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PatrolMapPageEditor extends EditorPart implements MapPart {
	public static final String ID = "org.wcs.smart.patrol.ui.PatrolMapEditor"; //$NON-NLS-1$
	
	private MapViewer mapViewer;

	private Label lblCoordinates;
	private ModalTool activeTool;
	private ToolContext toolcontext;
	
	private PatrolEditor parentEditor;
	private IViewportModelListener initListener = null; 
	
	private PatrolService patrolService = null;
	private Job addLayerJob = new Job("Add Layers Job") {
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			patrolService = new PatrolService(parentEditor.getPatrol());
	    	try {
	    		List<IGeoResource> layers = (List<IGeoResource>) patrolService.resources(monitor);
	    		
	    		AddLayersCommand command = new AddLayersCommand(layers, 0);
	    		getMap().sendCommandASync(command);
    		
	    		initListener = new IViewportModelListener() {
					@Override
					public void changed(ViewportModelEvent event) {
						getMap().getViewportModel().removeViewportModelListener(initListener);
						getMap().sendCommandASync(new ZoomExtentCommand());
						
					}
				};
	    		getMap().getViewportModel().addViewportModelListener(initListener);
				
			} catch (IOException e) {
				return new Status(IStatus.ERROR, "unknown", IStatus.ERROR, "Error loading pages", e);
			}
			return Status.OK_STATUS;
		}
	};
	
	 IPartListener2 partlistener = new IPartListener2(){
	        public void partActivated( IWorkbenchPartReference partRef ) {
	            if (partRef.getPart(false) == parentEditor) {
	                IToolManager tools = ApplicationGIS.getToolManager();
	                tools.setCurrentEditor( (PatrolMapPageEditor.this).mapViewer );
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
	        }

	        public void partVisible( IWorkbenchPartReference partRef ) {
	        }

	        public void partInputChanged( IWorkbenchPartReference partRef ) {
	        }

	    };
	    
    private FlashFeatureListener selectFeatureListener = new FlashFeatureListener();
    private boolean flashFeatureRegistered = false;
    
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job("Patrol Refresh Job"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (patrolService != null){
				try {
					patrolService.refresh(null);
				} catch (IOException e) {
					SmartPatrolPlugIn.log("Error refreshing patrol service.", e);
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
				refreshJob.schedule();
			}
		}
	};
	    
	public PatrolMapPageEditor(PatrolEditor parent){
		this.parentEditor = parent;
	}
	

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
        getSite().getPage().removePostSelectionListener(selectFeatureListener);
    }
    
	private void addLayers(){
		addLayerJob.schedule();
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
		if (!(input instanceof PatrolEditorInput)){
			throw new RuntimeException("Invalid editor input.");
		}
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
    	layout.verticalSpacing = 0;
        parent.setLayout(layout);
		composite.setLayout(layout);

        mapViewer = new MapViewer(composite,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
        mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Map map = (Map) ProjectFactory.eINSTANCE.createMap();
        mapViewer.setMap(map);
        //set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(Area.AREA_CRS);
		
		MapToolComposite tools = new MapToolComposite();
		tools.createComposite(composite);
		
		
        Composite infoArea = new Composite(parent, SWT.BORDER);
        infoArea.setLayout(new GridLayout(3, false));
        infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,2, 1));
        lblCoordinates = new Label(infoArea, SWT.NONE);
        lblCoordinates.setText("Coordinates");
        lblCoordinates.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        lblCoordinates.setAlignment(SWT.RIGHT);
        
        final Label lblSeparator = new Label(infoArea, SWT.SEPARATOR | SWT.VERTICAL);
        GridData gd = new GridData(SWT.CENTER, SWT.CENTER, false, false);
        gd.heightHint = lblCoordinates.computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
        lblSeparator.setLayoutData(gd);
        
        final Label lblSRID = new Label(infoArea, SWT.NONE);
        lblSRID.setText(map.getViewportModel().getCRS().getName().getCode());
        lblSRID.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
        map.getViewportModel().addViewportModelListener(new IViewportModelListener() {
			@Override
			public void changed(ViewportModelEvent event) {
				if(event.getType() == ViewportModelEvent.EventType.CRS){
					Display display = PlatformUI.getWorkbench().getDisplay();
			        if (display == null){
			        	display = Display.getDefault();
			        }
			        display.asyncExec(new Runnable(){
						@Override
						public void run() {
							lblSRID.setText(getMap().getViewportModel().getCRS().getName().getCode());
							lblSRID.getParent().layout();
						}});
				}
				
			}
		});
      
        
        mapViewer.getViewport().addMouseMotionListener(new MapMouseMotionListener() {
			@Override
			public void mouseMoved(MapMouseEvent event) {
				event.getPoint();
				Coordinate c = mapViewer.getMap().getViewportModelInternal().pixelToWorld(event.x, event.y);
				lblCoordinates.setText(format(c.x) + ", " + format(c.y));
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
        addLayers();
        registerFeatureFlasher();
        PatrolEventManager.getInstance().addListener(EventType.PATROL_MODIFIED, patrolUpdatedListeners);
	}

    public void setModalTool( String toolId ) {
    	   if (activeTool != null) {
               // ask the current tool to stop listening etc...
    		   activeTool.setActive(false);
    		   //activeTool.setEnabled(false);
               activeTool = null;
           }
    	   
    	   Tool tool = ApplicationGIS.getToolManager().findTool(toolId);
   		
           if( tool == null || !(tool instanceof ModalTool)){
               return;
           }
           activeTool = (ModalTool)tool;
           activeTool.setContext(getToolContext());
           activeTool.setActive(true);
           
           Cursor toolCursor = ApplicationGIS.getToolManager().findToolCursor(activeTool.getCursorID());
           if (toolCursor != null){
        	   activeTool.getContext().getViewportPane().setCursor(toolCursor);
           }
    }
    
   
    /**
     * @return tool context (used to teach tools about our MapViewer facilities.
     */
    protected synchronized ToolContext getToolContext(){
        if( toolcontext == null ){
            toolcontext = new ToolContextImpl();
            toolcontext.setMapInternal(mapViewer.getMap());        
            toolcontext.setRenderManagerInternal(mapViewer.getMap().getRenderManagerInternal());            
        }
        return toolcontext;
    }


    public Map getMap() {
        return mapViewer.getMap();
    }

    @Override
    public void dispose() {
        super.dispose();

        if (mapViewer != null && mapViewer.getViewport() != null && getMap() != null) {
        	mapViewer.getViewport().removePaneListener(getMap().getViewportModelInternal());
        }
        //getSite().getWorkbenchWindow().getPartService().removePartListener(partListener);
		
        getMap().getViewportModelInternal().setInitialized(false);
//        getSite().removeSelectionProvider(replaceableSelectionProvider);
        mapViewer.dispose();
        getSite().getWorkbenchWindow().getPartService().removePartListener(partlistener);
        
        partlistener = null;
        deregisterFeatureFlasher();
        this.selectFeatureListener = null;
        PatrolEventManager.getInstance().removeListener(EventType.PATROL_MODIFIED, patrolUpdatedListeners);
        
        //dispose of patrol service
        CatalogPlugin.getDefault().getLocalCatalog().remove(patrolService);
        patrolService.dispose(null);
        patrolService = null;
        
        
        refreshJob.cancel();
        refreshJob = null;
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
	 * @see net.refractions.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return ((IEditorSite)getSite()).getActionBars().getStatusLineManager();		
	}
	
	
	private class FlashFeatureListener implements ISelectionListener {

        public void selectionChanged( IWorkbenchPart part, final ISelection selection ) {
            if (part == PatrolMapPageEditor.this.parentEditor || getSite().getPage().getActivePart() != part
                    || selection instanceof IBlockingSelection)
                return;
            
            ISafeRunnable sendAnimation = new ISafeRunnable(){
                public void run() {
                    if (selection instanceof IStructuredSelection) {
                        IStructuredSelection s = (IStructuredSelection) selection;
                        List<SimpleFeature> features = new ArrayList<SimpleFeature>();
                        for( Iterator iter = s.iterator(); iter.hasNext(); ) {
                            Object element = iter.next();

                            if (element instanceof SimpleFeature) {
                                SimpleFeature feature = (SimpleFeature) element;
                                features.add(feature);
                            }
                        }
                        if (features.size() == 0)
                            return;
                        if (!mapViewer.getRenderManager().isDisposed()) {
                            IAnimation anim = createAnimation(features);
                            //System.out.println("run animation");
                            if (anim != null){
                                AnimationUpdater.runTimer(getMap().getRenderManager().getMapDisplay(), anim);
                            }
                        }
                    }
                }
                public void handleException( Throwable exception ) {
                	SmartPatrolPlugIn.log("Exception preparing animation", exception); //$NON-NLS-1$
                }
            };

            try {
                sendAnimation.run();
            } catch (Exception e) {
                SmartPatrolPlugIn.log("", e); //$NON-NLS-1$
            }
            // PlatformGIS.run(sendAnimation);
        }

        private IAnimation createAnimation( List<SimpleFeature> current ) {
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
                        }
                }
                if (command == null) {
                    command = new DrawFeatureCommand(feature);
                }
                command.setMap(getMap());
//                command.preRender();
                commands.add(command);
            }
            Rectangle2D rect = new Rectangle();
            // for( IDrawCommand command : commands ) {
            // rect=rect.createUnion(command.getValidArea());
            // }
            final Rectangle validArea = (Rectangle) rect;
            FeatureAnimation anim = new FeatureAnimation(commands, validArea);
            return anim;
        }
    }

}


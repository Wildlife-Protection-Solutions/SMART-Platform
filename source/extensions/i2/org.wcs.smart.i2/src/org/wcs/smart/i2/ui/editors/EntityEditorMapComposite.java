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
package org.wcs.smart.i2.ui.editors;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.Serializable;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
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
import org.opengis.feature.simple.SimpleFeature;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.filter.DateFilterComposite;
import org.wcs.smart.common.filter.DateFilterDropDownComposite;
import org.wcs.smart.common.filter.DateFilterComposite.DateFilter;
import org.wcs.smart.i2.udig.entity.IntelEntityGeoResource;
import org.wcs.smart.i2.udig.entity.IntelEntityService;
import org.wcs.smart.i2.udig.entity.IntelEntityServiceExtension;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.ProjectionDialog;
import org.wcs.smart.ui.map.ScaleRatioComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.GeometryUtils;
import org.wcs.smart.util.ReprojectUtils;
import org.wcs.smart.util.UuidUtils;

import com.vividsolutions.jts.geom.Coordinate;

public class EntityEditorMapComposite extends Composite implements MapPart{

	private EntityEditor editor;

	// map components
	private Label lblCoordinates;
	private Button lblSRID;
	protected MapViewer mapViewer;
	protected String[] mapTools = null;
	protected MapToolComposite tools;
	
	private DateFilterDropDownComposite dateComp;
	
	IPartListener2 partlistener = new IPartListener2(){
	        public void partActivated( IWorkbenchPartReference partRef ) {
	            if (partRef.getPart(false) == editor) {
	                IToolManager toolManager = ApplicationGIS.getToolManager();
	                toolManager.setCurrentEditor( mapViewer );
                	tools.selectLastTool();
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
	        	if (partRef.getPart(false) == editor) {
	        		deregisterFeatureFlasher();
	        	}
	        }

	        public void partVisible( IWorkbenchPartReference partRef ) {
	        	if (partRef.getPart(false) == editor) {
	        		registerFeatureFlasher();
	        	}
	        }

	        public void partInputChanged( IWorkbenchPartReference partRef ) {
	        }

	    };
	    
    private FlashFeatureListener selectFeatureListener = new FlashFeatureListener();
    private boolean flashFeatureRegistered = false;
	
	public EntityEditorMapComposite(Composite parent, EntityEditor parentEditor) {
		super(parent, SWT.NONE);
		this.editor = parentEditor;
		
		createPartControl();
		addLayers();
	}

	private IntelEntityService service = null;
	private void addLayers(){
		LoadDefaultLayersJob loadDefaultLayers = new LoadDefaultLayersJob(getMap()){
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = super.run(monitor);
				
				if (service == null){
					//TODO: only run once entity is loaded
					HashMap<String, Serializable> params = new HashMap<String,Serializable>();
					params.put(IntelEntityServiceExtension.ENTITY_UUID_KEY, UuidUtils.uuidToString(editor.getEntity().getUuid()));
					service = new IntelEntityService(params);
					
					Display.getDefault().syncExec(new Runnable(){

						@Override
						public void run() {
							if (dateComp != null){
								Date[] filter = null;
								if (dateComp.getDateFilter() == DateFilter.CUSTOM){
									filter = new Date[]{dateComp.getCustomStartDate(), dateComp.getCustomEndDate()};
								}else{
									filter = new Date[]{dateComp.getDateFilter().getStartDate(),dateComp.getDateFilter().getEndDate()};
								}
								try {
									service.setDateFilter(filter);
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
						
					});
					try {
						AddLayersCommand cmd = new AddLayersCommand(service.resources(monitor), 1);
						getMap().sendCommandASync(cmd);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
				}
				
				
				return status;
			}
		};
		loadDefaultLayers.schedule();
	}
	 /**
     * registers a listener with the current page that flashes a feature each time the current
     * selected feature changes.
     */
    protected synchronized void registerFeatureFlasher() {
        if (!flashFeatureRegistered) {
            flashFeatureRegistered = true;
            IWorkbenchPage page = editor.getSite().getPage();
            page.addPostSelectionListener(selectFeatureListener);
        }
    }

    protected synchronized void deregisterFeatureFlasher() {
        flashFeatureRegistered = false;
        //AnimationUpdater.cancel(getMap().getRenderManager().getMapDisplay());
        editor.getSite().getPage().removePostSelectionListener(selectFeatureListener);
    }
    
	
	
	
	/**
	 *  Creates the map
	 * 
	 */
	public void createPartControl() {
		Composite parent = this;
    

		DateFilterComposite.DateFilter[] defaultFilters = new DateFilter[]{
					DateFilter.LAST_30_DAYS,
					DateFilter.LAST_60_DAYS,
					DateFilter.LAST_YEAR,
					DateFilter.LAST_5_YEARS,
					DateFilter.ALL,
					DateFilter.CUSTOM
		};
		dateComp = new DateFilterDropDownComposite(this, defaultFilters, DateFilter.LAST_YEAR);
        dateComp.setBounds(0, 0, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
        dateComp.addChangeListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				dateComp.setBounds(0, 0, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).x, dateComp.computeSize(SWT.DEFAULT, SWT.DEFAULT).y);
				
				if (service != null){
					try {
						Date[] filter = null;
						if (dateComp.getDateFilter() == DateFilter.CUSTOM){
							filter = new Date[]{dateComp.getCustomStartDate(), dateComp.getCustomEndDate()};
						}else{
							filter = new Date[]{dateComp.getDateFilter().getStartDate(),dateComp.getDateFilter().getEndDate()};
						}
						service.setDateFilter(filter);
						getMap().getRenderManager().refresh(null);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		});
		Composite composite = new Composite(parent, SWT.NONE);
		addListener(SWT.Resize, new Listener(){
			@Override
			public void handleEvent(Event event) {
				composite.setBounds(0,0,getBounds().width-5,getBounds().height-5);		
			}
        });
		GridLayout layout = new GridLayout(2,false);
    	layout.marginBottom=0;
    	layout.marginHeight = 0;
    	layout.marginLeft = 0;
    	layout.marginRight = 0;
    	layout.marginTop = 0;
    	layout.marginWidth = 0;
    	layout.horizontalSpacing = 0;
    	layout.verticalSpacing = 2;
		composite.setLayout(layout);

        mapViewer = new MapViewer(composite,  SWT.SINGLE | SWT.DOUBLE_BUFFERED);
        mapViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        Map map = (Map) ProjectFactory.eINSTANCE.createMap();
        map.setName(editor.getEditorInput().getName());
        mapViewer.setMap(map);
        //set default crs
		mapViewer.getMap().getViewportModelInternal().setCRS(ViewportModel.BAD_DEFAULT);
		mapViewer.getMap().getViewportModelInternal().setCRS(GeometryUtils.SMART_CRS);
	    
        if (mapTools == null || mapTools.length == 0){
        	tools = new MapToolComposite();
        }else{
        	tools = new MapToolComposite(mapTools);
        }
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
				ProjectionDialog pd = new ProjectionDialog(editor.getSite().getShell(), mapViewer.getMap().getViewportModel().getCRS());
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
        mapViewer.init(editor);
 
        
        editor.getSite().getWorkbenchWindow().getPartService().addPartListener(partlistener);
        registerFeatureFlasher();
	}

	
	private void updateLabel() {
		editor.getSite().getShell().getDisplay().asyncExec(new Runnable() {
			@Override
			public void run() {
				if (lblSRID == null || lblSRID.isDisposed()) return;
				lblSRID.setText(getMap().getViewportModel().getCRS().getName()
						.getCode());
				lblSRID.getParent().layout();
			}
		});

	}
        
	@Override
    public Map getMap() {
        return mapViewer.getMap();
    }
	
    @Override
    public void dispose() {
        super.dispose();
        deregisterFeatureFlasher();
        editor.getSite().getWorkbenchWindow().getPartService().removePartListener(partlistener);
        
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

	

	/* (non-Javadoc)
	 * @see org.locationtech.udig.project.ui.internal.MapPart#getStatusLineManager()
	 */
	@Override
	public IStatusLineManager getStatusLineManager() {
		return ((IEditorSite)editor.getSite()).getActionBars().getStatusLineManager();		
	}
	
	
	private class FlashFeatureListener implements ISelectionListener {

		
        public void selectionChanged( IWorkbenchPart part, final ISelection selection ) {
            if (part == editor || editor.getSite().getPage().getActivePart() != part
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
	
	/* initial zoom function */
	protected ReferencedEnvelope initialZoom = null;
	
	public void setInitialZoom(ReferencedEnvelope zoom){
		this.initialZoom = zoom;
	}
//	
//	protected IPageChangedListener initialZoomListener = new IPageChangedListener() {
//		@Override
//		public void pageChanged(PageChangedEvent event) {
//			if (event.getSelectedPage() == editor){
//					getMap().getViewportModel().addViewportModelListener(new IViewportModelListener() {
//					
//					@Override
//					public void changed(ViewportModelEvent event) {
//						getMap().getViewportModel().removeViewportModelListener(this);
//						if (initialZoom == null){
//							getMap().sendCommandSync(new ZoomExtentCommand());
//						}else{
//							getMap().sendCommandSync(new SetViewportBBoxCommand(initialZoom));
//						}
//					}
//				});
//			}
//			
//		}
//	};
//	
//	protected void addInitialZoomFunction(){
//		getParentEditor().addPageChangedListener(initialZoomListener);
//	}

}

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
package org.wcs.smart.ui.map;

import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import net.refractions.udig.internal.ui.IDropTargetProvider;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.commands.ChangeCRSCommand;
import net.refractions.udig.project.internal.render.RenderPackage;
import net.refractions.udig.project.internal.render.ViewportModel;
import net.refractions.udig.project.ui.AnimationUpdater;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.project.ui.commands.IDrawCommand;
import net.refractions.udig.project.ui.internal.FeatureAnimation;
import net.refractions.udig.project.ui.internal.MapPart;
import net.refractions.udig.project.ui.internal.commands.draw.DrawFeatureCommand;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseEvent;
import net.refractions.udig.project.ui.render.displayAdapter.MapMouseMotionListener;
import net.refractions.udig.project.ui.tool.IMapEditorSelectionProvider;
import net.refractions.udig.project.ui.tool.IToolManager;
import net.refractions.udig.project.ui.viewers.MapViewer;
import net.refractions.udig.ui.IBlockingSelection;
import net.refractions.udig.ui.UDIGDragDropUtilities;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.PageChangedEvent;
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
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.opengis.feature.simple.SimpleFeature;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * A base class for smart map editors.
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class SmartMapEditorPart extends EditorPart implements MapPart, IDropTargetProvider {

	/**
	 * Coordinate text x,y separator
	 */
	public static final String COORDINATE_XYSEPARATOR = Messages.SmartMapEditorPart_MapCoordinate_XYSeparator;

	/**
	 * Coordinate widget label
	 */
	public static final String COORDINATE_LABEL = Messages.SmartMapEditorPart_MapCoordinate_Label;

	/**
	 * Projection error meesage
	 */
	public static final String ERROR_SETTING_MAP_PROJECTION = Messages.SmartMapEditorPart_Error_SettingMapProjection;

	protected MapViewer mapViewer;

	private Label lblCoordinates;
	private Button lblSRID;
	
	private MapToolComposite tools;
	
	IPartListener2 partlistener = new IPartListener2(){
	        public void partActivated( IWorkbenchPartReference partRef ) {
	            if (partRef.getPart(false) == getParentEditor()) {
	                IToolManager toolManager = ApplicationGIS.getToolManager();
	                toolManager.setCurrentEditor( SmartMapEditorPart.this.mapViewer );
                	SmartMapEditorPart.this.tools.selectLastTool();
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
	        	if (partRef.getPart(false) == getParentEditor()) {
	        		deregisterFeatureFlasher();
	        	}
	        }

	        public void partVisible( IWorkbenchPartReference partRef ) {
	        	if (partRef.getPart(false) == getParentEditor()) {
	        		registerFeatureFlasher();
	        	}
	        }

	        public void partInputChanged( IWorkbenchPartReference partRef ) {
	        }

	    };
	    
    private FlashFeatureListener selectFeatureListener = new FlashFeatureListener();
    private boolean flashFeatureRegistered = false;
  
    public abstract MultiPageEditorPart getParentEditor();
	

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
		mapViewer.getMap().getViewportModelInternal().setCRS(SmartDB.DATABASE_CRS);
	      
        ApplicationGIS.getToolManager().setCurrentEditor(this);
        
		tools = new MapToolComposite();
		tools.createComposite(composite);
		tools.selectTool("net.refractions.udig.tools.Pan"); //$NON-NLS-1$
		
        Composite infoArea = new Composite(parent, SWT.NONE);
        GridLayout gl = new GridLayout(5, false);
        gl.marginTop = gl.marginBottom = gl.marginHeight= 0;
        gl.marginRight = 0;
        gl.marginWidth = 0;
        infoArea.setLayout(gl);
        infoArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false,2, 1));
        lblCoordinates = new Label(infoArea, SWT.NONE);
        lblCoordinates.setText(COORDINATE_LABEL);
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
						ChangeCRSCommand command = new ChangeCRSCommand(pd.getSelection().getCrs());
						getMap().sendCommandASync(command);
					}catch (Exception ex){
						SmartPlugIn.displayLog(getSite().getShell(), ERROR_SETTING_MAP_PROJECTION + ex.getLocalizedMessage(), ex);
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
				lblCoordinates.setText(format(c.x) + COORDINATE_XYSEPARATOR + format(c.y));
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
        
        getParentEditor().addPageChangedListener(new IPageChangedListener() {			
			@Override
			public void pageChanged(PageChangedEvent event) {
				if (event.getSelectedPage().equals(SmartMapEditorPart.this)){
					tools.selectLastTool();
				}
			}
		});

	}

	@Override
	public Object getTarget(DropTargetEvent event) {
		return this;
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
        
        if (mapViewer != null && mapViewer.getViewport() != null && getMap() != null) {
        	mapViewer.getViewport().removePaneListener(getMap().getViewportModelInternal());
        }
        
        getMap().getViewportModelInternal().setInitialized(false);
        mapViewer.dispose();
        getSite().getWorkbenchWindow().getPartService().removePartListener(partlistener);
        partlistener = null;
        this.selectFeatureListener = null;
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
            if (part == SmartMapEditorPart.this.getParentEditor() || getSite().getPage().getActivePart() != part
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
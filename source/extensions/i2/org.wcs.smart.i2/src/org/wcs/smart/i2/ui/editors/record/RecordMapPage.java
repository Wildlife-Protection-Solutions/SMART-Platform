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
package org.wcs.smart.i2.ui.editors.record;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.factory.CommonFactoryFinder;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.render.impl.RenderManagerImpl;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.map.style.RecordPointObservationDefaultStyle;
import org.wcs.smart.i2.map.style.RecordPolygonObservationDefaultStyle;
import org.wcs.smart.i2.map.style.RecordPositionAttributeDefaultStyle;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.i2.udig.record.IntelRecordAttributeGeoResource;
import org.wcs.smart.i2.udig.record.IntelRecordFeatureSource;
import org.wcs.smart.i2.udig.record.IntelRecordGeoResource;
import org.wcs.smart.i2.udig.record.IntelRecordService;
import org.wcs.smart.i2.ui.editors.LocationAttributeMapLayer;
import org.wcs.smart.udig.style.StyleManager;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
import org.wcs.smart.ui.map.tool.DrawPolygonTool;
import org.wcs.smart.ui.map.tool.DrawPolygonTool.INewPolygonEvent;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.UuidUtils;

/**
 * Map page for intelligence record.
 * 
 * @author Emily
 *
 */
public class RecordMapPage extends SmartMapEditorPart {
	
	private RecordEditor recordEditor; 
	
	private LoadDefaultLayersJob loadDefaultLayers;
	private FormToolkit toolkit = null;
	
	private ILayer pointLayer;
	private ILayer polygonLayer;
	private IntelRecordService recordService ;
	
	private LocationAttributeMapLayer attributeLayer;
	private LocationListComposite locationPanel;
	private ToolItem importItem ;
	
	private static HashMap<String,String> defaultStyles = new HashMap<>();
	static {
		defaultStyles.put(LocationLayerType.POINT.name(), RecordPointObservationDefaultStyle.KEY);
		defaultStyles.put(LocationLayerType.POLYGON.name(), RecordPolygonObservationDefaultStyle.KEY);		
	}
	
	private Job localMapLayerJob = new Job(Messages.RecordMapPage_jobname) { 
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			//wait for job to finish
			if (getMap() == null) return Status.OK_STATUS;
			if (loadDefaultLayers == null) return Status.OK_STATUS;
			try {
				loadDefaultLayers.join();
			} catch (InterruptedException e1) {
				Intelligence2PlugIn.log(e1.getMessage(), e1);
			}
			if (getMap() == null) return Status.OK_STATUS;
			
			try {
				
				recordService = new IntelRecordService(recordEditor.getRecord());

	    		List<IGeoResource> layers = (List<IGeoResource>) recordService.resources(monitor);
				
				List<IGeoResource> attributeLayers = new ArrayList<>();
				List<IGeoResource> recordLayers = new ArrayList<>();
				
				for (IGeoResource l : layers) {
					if (l.canResolve(IntelRecordGeoResource.class)) {
						if (l.resolve(IntelRecordGeoResource.class, null).getType() == LocationLayerType.POLYGON) {
							recordLayers.add(0, l);	
						}else {
							recordLayers.add(l);	
						}
					}else if (l.canResolve(IntelRecordAttributeGeoResource.class)) {
						attributeLayers.add(l);
											
					}					
				}
				
				attributeLayers.sort((a,b)->-Collator.getInstance().compare(a.getTitle(), b.getTitle()));
				recordLayers.addAll(0,attributeLayers);
				
				AddLayersCommand command = new AddLayersCommand(recordLayers, getMap().getLayersInternal().size()) {
	    			public void run( IProgressMonitor monitor ) throws Exception {
	    				
	    				((RenderManagerImpl)getMap().getRenderManagerInternal()).disableRendering();
	    				
	    				super.run(monitor);
	    				
	    				try(Session session = HibernateManager.openSession()){
		    				for (Layer l : getLayers()) {
		    					
		    					if (l.getGeoResource().canResolve(IntelRecordAttributeGeoResource.class)) {
		    						l.getStyleBlackboard().put(SLDContent.ID, 
		    								l.getGeoResource().resolve(IntelRecordAttributeGeoResource.class, null).getAttribute().getAttributeGeometryStyle().toStyle());
		    						
		    						l.setName(MessageFormat.format("Data Model: {0}", l.getName()));
		    					}else {
		    						
		    						if (l.getGeoResource().canResolve(IntelRecordGeoResource.class)) {
		    							LocationLayerType type = l.getGeoResource().resolve(IntelRecordGeoResource.class, null).getType();
		    							if (type == LocationLayerType.POINT) {
		    								pointLayer = l;
		    							}else if (type == LocationLayerType.POLYGON) {
		    								polygonLayer = l;
		    							}
		    						}
		    						
		    						StyleManager.INSTANCE.applyDefaultStyleToMapLayer(SmartDB.getCurrentConservationArea(),  l, defaultStyles, session, monitor);
		    					}
		    				}
	    				}
	    				
	    				((RenderManagerImpl)getMap().getRenderManagerInternal()).enableRendering();
	    				getMap().getRenderManager().refresh(null);
	    			}
					
	    		};
	    		getMap().sendCommandASync(command);
				
				addInitialZoomFunction();
				
			} catch (Exception e) {
				Intelligence2PlugIn.log(e.getMessage(), e);
			}
			return Status.OK_STATUS;
		}
	};
	
    
   
	    
	public RecordMapPage(RecordEditor parent){
		this.recordEditor = parent;
	}
	
	public MultiPageEditorPart getParentEditor() {
		return this.recordEditor;
	}

	/**
	 * refresh the map and track layers
	 */
	public void refresh() {

    	locationPanel.refreshTable();
    	if (recordEditor.getRecord().getAttributes() != null){
    		for (IntelRecordAttributeValue v : recordEditor.getRecord().getAttributes()){
    			attributeLayer.refreshLayerRecord(v);
    		}
    	}
    	
    	mapViewer.getRenderManager().refresh(null);
	}

	public void setEditMode(boolean editMode){
		super.tools.getTool(DrawPolygonTool.ID).setEnabled(editMode);
        super.tools.getTool(DrawPointTool.ID).setEnabled(editMode);
        importItem.setEnabled(editMode);
	}
	
	
	public void updateLocationAttribute(IntelRecordAttributeValue value){
		attributeLayer.refreshLayerRecord(value);
	}
	
	public synchronized void initPage(){
		locationPanel.init();
		localMapLayerJob.schedule();
		
		if (attributeLayer != null){
			attributeLayer.dispose();
		}
		UUID id = recordEditor.getRecord().getUuid();
		if (id == null) id = UUID.randomUUID();
		
		attributeLayer = new LocationAttributeMapLayer(getMap(), Messages.RecordMapPage_PositionMapLayerName1, UuidUtils.uuidToString(id), RecordPositionAttributeDefaultStyle.KEY);
		attributeLayer.createLayersRecord(recordEditor.getRecord().getAttributes());
	}
	
	private void addLayers(){
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap());
		loadDefaultLayers.schedule();
	}
	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		if (!(input instanceof RecordEditorInput)){
			throw new RuntimeException("Invalid editor input."); //$NON-NLS-1$
		}
		super.init(site, input);
	}

	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		
		SashForm sash = new SashForm(parent,  SWT.VERTICAL);
		sash.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		String[] tools = Arrays.copyOf(MapToolComposite.DEFAULT_MAP_TOOLS, MapToolComposite.DEFAULT_MAP_TOOLS.length + 3);
		tools[tools.length - 3] = ClearSelectionTool.ID;
		tools[tools.length - 2] = DrawPolygonTool.ID;
		tools[tools.length - 1] = DrawPointTool.ID;
		mapTools = tools;
		
		Composite mapArea = toolkit.createComposite(sash);
		mapArea.setLayout(new GridLayout());
		
		super.createPartControl(mapArea);
		
		locationPanel = new LocationListComposite(sash, toolkit, recordEditor);
		locationPanel.addSelectionListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				for (Iterator<?> iterator = ((IStructuredSelection)event.getSelection()).iterator(); iterator.hasNext();) {
					Object next = (Object)iterator.next();
					if (next instanceof IntelLocation){
						highlightFeature((IntelLocation) next);
						return;
					}	
				}
			}
		});
		
		ToolBar bar = super.tools.getToolbar();
		
		importItem = new ToolItem(bar, SWT.DROP_DOWN);
		importItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_LOCATION_IMPORT));
		importItem.setToolTipText(Messages.RecordMapPage_ImportMenuItem);
		
		if (!recordEditor.getEditMode()) {
			importItem.setEnabled(false);
		}
		Menu dd = new Menu(bar);
		
		MenuItem miManual = new MenuItem(dd, SWT.PUSH);
		miManual.setText(Messages.RecordMapPage_EnterValuesLabel);
		miManual.addListener(SWT.Selection, e->locationPanel.manuallyAddLocation());
		
		MenuItem fromFile = new MenuItem(dd, SWT.PUSH);
		fromFile.setText(Messages.RecordMapPage_ImportFileMenuItem);
		fromFile.addListener(SWT.Selection, e->locationPanel.importLocationsFromFile());
		
		MenuItem fromGps = new MenuItem(dd, SWT.PUSH);
		fromGps.setText(Messages.RecordMapPage_ImportGpsMenuItem);
		fromGps.addListener(SWT.Selection, e->locationPanel.importLocationsFromGps());
		
		bar.addDisposeListener(e->dd.dispose());
				
		importItem.addSelectionListener(new SelectionAdapter(){
			@Override
			public void widgetSelected(SelectionEvent event){
				 if (event.detail == SWT.ARROW) {
			          Rectangle rect = importItem.getBounds();
			          Point pt = new Point(rect.x, rect.y + rect.height);
			          pt = bar.toDisplay(pt);
			          dd.setLocation(pt.x, pt.y);
			          dd.setVisible(true);
			    }
			}	
		});
		
		sash.setWeights(new int[]{8,2});
        getMap().getBlackboard().put(RecordEditor.class.getName(), recordEditor);
        addLayers();
        
        super.tools.getTool(DrawPolygonTool.ID).setEnabled(recordEditor.getEditMode());
        super.tools.getTool(DrawPointTool.ID).setEnabled(recordEditor.getEditMode());
        

		INewPolygonEvent event = polygon -> recordEditor.addNewLocation(polygon, null);
        getMap().getBlackboard().put(DrawPolygonTool.INewPolygonEvent.class.getName(),event);
	}

	//udig does not support selection from multiple layers 
	private void highlightFeature(IntelLocation location){
		FilterFactory ff = CommonFactoryFinder.getFilterFactory();
		if (pointLayer == null || polygonLayer == null) return;
		if (location == null){
			((Layer)pointLayer).setFilter(Filter.EXCLUDE);
			((Layer)polygonLayer).setFilter(Filter.EXCLUDE);
		}else if (location.isPoint()){
			((Layer)polygonLayer).setFilter(Filter.EXCLUDE);
			((Layer)pointLayer).setFilter(ff.equals(ff.property(IntelRecordFeatureSource.FID_FIELD), ff.literal(location.getFeatureId())));
		}else if (location.isPolygon()){
			((Layer)pointLayer).setFilter(Filter.EXCLUDE);
			((Layer)polygonLayer).setFilter(ff.equals(ff.property(IntelRecordFeatureSource.FID_FIELD), ff.literal(location.getFeatureId())));
		}
	}
    
    @Override
    public void dispose() {
    	JobUtil.stopJobs(loadDefaultLayers, localMapLayerJob);
    	loadDefaultLayers = null;
    	localMapLayerJob = null;

        if (toolkit != null){
        	toolkit.dispose();
        	toolkit = null;
        }
        
     
        this.locationPanel = null;
        this.recordEditor = null;
        CatalogPlugin.getDefault().getLocalCatalog().remove(recordService);
        this.recordService.dispose(new NullProgressMonitor());
        
        this.recordService = null;
        
        super.dispose();
        
    }

    public void selectLocation(IntelLocation location) {
    	locationPanel.setSelection(location);
    }
    
    public void refreshLocationTable() {
    	locationPanel.refreshTable();
    }

}
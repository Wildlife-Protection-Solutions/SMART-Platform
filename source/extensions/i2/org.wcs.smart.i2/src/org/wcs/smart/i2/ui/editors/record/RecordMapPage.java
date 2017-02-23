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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
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
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.core.internal.FeatureUtils;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.IntelLocation;
import org.wcs.smart.i2.model.IntelRecordAttributeValue;
import org.wcs.smart.i2.udig.LocationLayerType;
import org.wcs.smart.i2.udig.record.IntelRecordDataSource;
import org.wcs.smart.i2.udig.record.IntelRecordFeatureReader;
import org.wcs.smart.i2.udig.record.IntelRecordFeatureSource;
import org.wcs.smart.i2.ui.editors.LocationAttributeMapLayer;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.ui.map.tool.ClearSelectionTool;
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
	private IGeoResource pointResource;
	private IGeoResource polygonResource;
	private SimpleFeatureType polygonFeatureType = null;
	private SimpleFeatureType pointFeatureType = null;
	private LocationAttributeMapLayer attributeLayer;
	private LocationListComposite locationPanel;
	
	private Job localMapLayerJob = new Job("configuring locations layer") {
		
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
			boolean added = false;
			try {
				if (polygonFeatureType == null || polygonResource == null){
					String formatString = IntelRecordFeatureSource.getFeatureSchemaString(LocationLayerType.POLYGON);
					Name name = IntelRecordDataSource.generateName(LocationLayerType.POLYGON, recordEditor.getRecord().getUuid());
					polygonFeatureType = DataUtilities.createType(name.getNamespaceURI(), name.getLocalPart(), formatString);
					synchronized (CatalogPlugin.getDefault().getLocalCatalog()) {
						polygonResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(polygonFeatureType);
					}
					
					AddLayersCommand command = new AddLayersCommand(Collections.singletonList(polygonResource), getMap().getLayersInternal().size()){
						 public void run( IProgressMonitor monitor ) throws Exception {
							 super.run(monitor);
							 if (getLayers() != null &&  !getLayers().isEmpty()){
								 polygonLayer = getLayers().get(0);
								 polygonLayer.getStyleBlackboard().put("org.locationtech.udig.style.sld", LocationLayerType.POLYGON.getDefaultLayerStyle());
							 }
						 }
					};
					getMap().sendCommandASync(command);
					added = true;
				}
				if (pointFeatureType == null || pointResource == null){
					String formatString = IntelRecordFeatureSource.getFeatureSchemaString(LocationLayerType.POINT);
					Name name = IntelRecordDataSource.generateName(LocationLayerType.POINT, recordEditor.getRecord().getUuid());
					pointFeatureType = DataUtilities.createType(name.getNamespaceURI(), name.getLocalPart(),formatString);
					synchronized (CatalogPlugin.getDefault().getLocalCatalog()) {
						pointResource = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(pointFeatureType);
					}
					AddLayersCommand command = new AddLayersCommand(Collections.singletonList(pointResource), getMap().getLayersInternal().size()){
						 public void run( IProgressMonitor monitor ) throws Exception {
							 super.run(monitor);
							 if (getLayers() != null &&  !getLayers().isEmpty()){
								 pointLayer = getLayers().get(0);
								 pointLayer.getStyleBlackboard().put("org.locationtech.udig.style.sld", LocationLayerType.POINT.getDefaultLayerStyle());
							 }
						 }
					};
					getMap().sendCommandASync(command);
					added = true;
				}
				
				IGeoResource toUpdate[] = new IGeoResource[]{polygonResource, pointResource};
				SimpleFeatureType typesToUpdate[] = new SimpleFeatureType[]{polygonFeatureType, pointFeatureType};
				for (int i = 0; i < toUpdate.length; i ++){
					List<SimpleFeature> features = new ArrayList<SimpleFeature>();
					try(IntelRecordFeatureReader featureReader = new IntelRecordFeatureReader(recordEditor.getRecord(), typesToUpdate[i])){
						while(featureReader.hasNext()){
							features.add(featureReader.next());
						}
					}
					
					try{
						toUpdate[i].resolve(FeatureStore.class, monitor).removeFeatures(Filter.INCLUDE);
						toUpdate[i].resolve(FeatureStore.class, monitor).addFeatures(FeatureUtils.toFeatureCollection(features, typesToUpdate[i]));
					}catch (ConcurrentModificationException ex){
						//try again - geotools bug?
						toUpdate[i].resolve(FeatureStore.class, monitor).removeFeatures(Filter.INCLUDE);
						toUpdate[i].resolve(FeatureStore.class, monitor).addFeatures(FeatureUtils.toFeatureCollection(features, typesToUpdate[i]));
					}
				}
				if (added){
					addInitialZoomFunction();
				}else{
					mapViewer.getRenderManager().refresh(null);
				}
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
    	if (localMapLayerJob != null) {
    		localMapLayerJob.cancel();
    		localMapLayerJob.schedule();
    	}

    	locationPanel.refreshTable();
    	for (IntelRecordAttributeValue v : recordEditor.getRecord().getAttributes()){
    		attributeLayer.refreshLayerRecord(v);
    	}
	}

	public void setEditMode(boolean editMode){
		tools.getTool(DrawPolygonTool.ID).setEnabled(editMode);
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
		
		attributeLayer = new LocationAttributeMapLayer(getMap(), "Position Attributes", UuidUtils.uuidToString(id));
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
		
		ToolItem importItem = new ToolItem(bar, SWT.DROP_DOWN);
		importItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_LOCATION_IMPORT));
		importItem.setToolTipText("import locations from file or GPS device");
		
		Menu dd = new Menu(bar);
		
		MenuItem fromFile = new MenuItem(dd, SWT.PUSH);
		fromFile.setText("Import From File...");
		fromFile.addListener(SWT.Selection, e->locationPanel.importLocationsFromFile());
		
		MenuItem fromGps = new MenuItem(dd, SWT.PUSH);
		fromGps.setText("Import From GPS Device...");
		fromGps.addListener(SWT.Selection, e->locationPanel.importLocationsFromGps());
		
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
        super.dispose();
        
        //dispose of services
        try {
			if (pointResource != null) CatalogPlugin.getDefault().getLocalCatalog().remove(pointResource.service(null));
			if (polygonResource != null) CatalogPlugin.getDefault().getLocalCatalog().remove(polygonResource.service(null));
		} catch (Exception e) {
			Intelligence2PlugIn.log(e.getMessage(), e);
		}
    }

    

}
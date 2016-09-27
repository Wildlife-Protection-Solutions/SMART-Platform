package org.wcs.smart.i2.ui.editors.record;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.feature.SchemaException;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.core.internal.FeatureUtils;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.wcs.smart.i2.udig.IntelRecordDataSource;
import org.wcs.smart.i2.udig.IntelRecordFeatureReader;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.MapToolComposite;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.JobUtil;

public class RecordMapPage extends SmartMapEditorPart {
	
	//TODO: refresh events??
	private RecordEditor recordEditor; 
	
	private LoadDefaultLayersJob loadDefaultLayers;
	private FormToolkit toolkit = null;
	
	private IGeoResource locationLayer;
	private SimpleFeatureType featureType = null;
	
	private LocationListComposite locationPanel ;
	
	private Job addLayerJob = new Job("adding map layers") {
		
		@SuppressWarnings("unchecked")
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			String formatString = "the_geom:Geometry:srid=4326,fid:String,id:String,date:Date,time:Date,comment:String";
			
			try {
				featureType = DataUtilities.createType(IntelRecordDataSource.RECORD_TYPE, formatString);
				locationLayer = CatalogPlugin.getDefault().getLocalCatalog().createTemporaryResource(featureType);
			
				IntelRecordFeatureReader featureReader = new IntelRecordFeatureReader(recordEditor.getRecord(), featureType);
				List<SimpleFeature> features = new ArrayList<SimpleFeature>();
				while(featureReader.hasNext()){
					features.add(featureReader.next());
				}
				locationLayer.resolve(FeatureStore.class, monitor).addFeatures(FeatureUtils.toFeatureCollection(features, featureType));
		
				AddLayersCommand command = new AddLayersCommand(Collections.singletonList(locationLayer), 0);
				getMap().sendCommandASync(command);
		    	addInitialZoomFunction();
			} catch (Exception e) {
				//TODO:
				e.printStackTrace();
			}
			
//			patrolService = new PatrolService(parentEditor.getPatrol());
//	    	try {
//	    		List<IGeoResource> layers = (List<IGeoResource>) patrolService.resources(monitor);
//	    		
//	    		AddLayersCommand command = new AddLayersCommand(layers, 0);
//	    		getMap().sendCommandASync(command);
//    		
//	    		addInitialZoomFunction();
//				
//			} catch (IOException e) {
//				return new Status(IStatus.ERROR, Messages.PatrolMapPageEditor_UnknownError, IStatus.ERROR, Messages.PatrolMapPageEditor_Error_LoadingMapPage, e);
//			}
			return Status.OK_STATUS;
		}
	};
	
	  
    /**
     * Job to refresh the service and map.
     */
    private Job refreshJob = new Job("refresh map layers"){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			try{
				IntelRecordFeatureReader featureReader = new IntelRecordFeatureReader(recordEditor.getRecord(), featureType);
				List<SimpleFeature> features = new ArrayList<SimpleFeature>();
				while(featureReader.hasNext()){
					features.add(featureReader.next());
				}
				try{
					locationLayer.resolve(FeatureStore.class, monitor).removeFeatures(Filter.INCLUDE);
					locationLayer.resolve(FeatureStore.class, monitor).addFeatures(FeatureUtils.toFeatureCollection(features, featureType));
				}catch (ConcurrentModificationException ex){
					locationLayer.resolve(FeatureStore.class, monitor).removeFeatures(Filter.INCLUDE);
					locationLayer.resolve(FeatureStore.class, monitor).addFeatures(FeatureUtils.toFeatureCollection(features, featureType));
				}
			}catch(Exception ex){
				ex.printStackTrace();
			}
			
//			if (patrolService != null){
//				try {
//					patrolService.refresh(parentEditor.getPatrol(), null);
//				} catch (IOException e) {
//					SmartPatrolPlugIn.log(Messages.PatrolMapPageEditor_Error_RefreshingLayers, e);
//				}
//			}
//			//clear selection
			mapViewer.getRenderManager().refresh(null);
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
    	if (refreshJob != null) {
        	refreshJob.cancel();
        	refreshJob.schedule();
    	}

    	locationPanel.refreshTable();
	}

	public void setEditMode(boolean editMode){
		//TODO:
	}
	
	public void initPage(){
		locationPanel.init();

        addLayers();
	}
	
	private void addLayers(){
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap()){
			protected IStatus run(IProgressMonitor monitor) {
				IStatus status = super.run(monitor);
				addLayerJob.schedule();
				return status;
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
		
		String[] tools = Arrays.copyOf(MapToolComposite.DEFAULT_MAP_TOOLS, MapToolComposite.DEFAULT_MAP_TOOLS.length + 1);
		tools[tools.length - 1] = "org.wcs.smart.i2.record.polygon.draw";
		mapTools = tools;
		
		Composite mapArea = toolkit.createComposite(sash);
		mapArea.setLayout(new GridLayout());
		
		super.createPartControl(mapArea);
		
		locationPanel = new LocationListComposite(sash, toolkit, recordEditor);
		sash.setWeights(new int[]{8,2});
		
        
        getMap().getBlackboard().put(RecordEditor.class.getName(), recordEditor);
	}

    
    @Override
    public void dispose() {
    	JobUtil.stopJobs(loadDefaultLayers, addLayerJob, refreshJob);
    	loadDefaultLayers = null;
        refreshJob = null;

        if (toolkit != null){
        	toolkit.dispose();
        	toolkit = null;
        }
        super.dispose();
        
        //dispose of patrol service
        //TODO:
//        CatalogPlugin.getDefault().getLocalCatalog().remove(patrolService);
//        patrolService.dispose(null);
//        patrolService = null;
    }

    

}
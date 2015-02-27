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
package org.wcs.smart.plan.ui.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.hibernate.Session;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.project.command.AbstractCommand;
import org.locationtech.udig.project.command.UndoableMapCommand;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.filter.FilterFactory;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.query.map.udig.QueryServiceFactory;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolQueryFactory;
import org.wcs.smart.patrol.query.model.PatrolStartDateField;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.map.udig.PlanTargetService;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.query.model.filter.DateFilter;
import org.wcs.smart.query.model.filter.date.AllDatesFilter;
import org.wcs.smart.ui.map.LoadDefaultLayersJob;
import org.wcs.smart.ui.map.SmartMapEditorPart;
import org.wcs.smart.util.JobUtil;
import org.wcs.smart.util.SmartUtils;

/**
 * Map page for Plan editor.  Displays spatial targets on a map
 * for given plan and children plans.
 * @author Emily
 *
 */
public class MapPlanEditorPage extends SmartMapEditorPart {

	private PlanEditor parentEditor;

	private PlanTargetService planTargetService = null;
	private PlanTargetService subPlanTargetService = null;
	private LoadDefaultLayersJob loadDefaultLayers;

	//plan object with all subplans loaded
	//for supporting the subplan spatial targets
	//layer
	private Plan subPlanLayer = null;
	
	//layer for patrol query which
	//returns all tracklogs for patrols related
	//to the query
	private IGeoResource patrolLayer = null;

	private final Object lockObj = new Object();	
	private boolean isDisposing = false;
	
	/*
	 * Job for adding or refreshing the
	 * subplan spatial target layer.  It makes
	 * use of the subPlanLayer Plan object which will have
	 * all children plans loaded from the database.
	 * It waits for both the defaultlayers job  and the
	 * addlayers job to finish. 
	 */
	private Job refreshSubPlanLayer = new Job(Messages.MapPlanEditorPage_SubplanJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			Plan lPlan = subPlanLayer;
			if (lPlan == null){
				return Status.OK_STATUS;
			}

			if (subPlanTargetService == null) {
				try {
					//wait until plan target layer is added first
					if (isDisposing) return Status.CANCEL_STATUS;
					loadDefaultLayers.join();
					if (isDisposing) return Status.CANCEL_STATUS;
					addLayerJob.join();
					if (isDisposing) return Status.CANCEL_STATUS;
				} catch (Exception e) {					
					SmartPlanPlugIn.displayLog(Messages.MapPlanEditorPage_SubPlanError + e.getLocalizedMessage(), e);
				}
			}
			
			synchronized (lockObj) {
				try {
					if (subPlanTargetService != null){
						subPlanTargetService.refresh(lPlan, monitor);
						mapViewer.getRenderManager().refresh(null);
					}else{
						subPlanTargetService = new PlanTargetService(lPlan, true);
		    			@SuppressWarnings("unchecked")
						List<IGeoResource> layers = (List<IGeoResource>) subPlanTargetService.resources(monitor);
		    			AddLayersCommand command = new AddLayersCommand(layers, getMap().getLayersInternal().size());
		    			getMap().sendCommandASync(command);
					}
				} catch (Exception e) {					
					SmartPlanPlugIn.displayLog(Messages.MapPlanEditorPage_SubPlanError + e.getLocalizedMessage(), e);
				}
			}
			
			return Status.OK_STATUS;
		}
		
	};
	
	/*
	 * Job for adding plan spatial target layer, patrol query layer
	 *  and zooming the map.  This job waits until
	 * the load default layers job is finished to add
	 * the plan target layer to the map.
	 */
	private Job addLayerJob = new Job(Messages.MapPlanEditorPage_PlanJobName) {
		
		@Override
		protected IStatus run(IProgressMonitor monitor) {
	    	try {
	    		loadDefaultLayers.join();
	    		if (isDisposing) return Status.CANCEL_STATUS;

	    		synchronized (lockObj) {
	    			planTargetService = new PlanTargetService(parentEditor.getPlan(), false);
					
		    		//target layer
		    		final List<IGeoResource> layers = new ArrayList<IGeoResource>(planTargetService.resources(monitor));
		    		IGeoResource tracks = createTrackPoints(monitor);
		    		if (tracks != null){
		    			layers.add(tracks);
		    		}
		    		AddPlanningLayers command = new AddPlanningLayers(layers);
		    		getMap().sendCommandASync(command);
				}
				
			} catch (Exception e) {
				return new Status(IStatus.ERROR, 
						Messages.MapPlanEditorPage_UnknownError, 
						IStatus.ERROR, 
						Messages.MapPlanEditorPage_ErrorInitializePlanTargets, e);
			}
			return Status.OK_STATUS;
		}
		
		private IGeoResource createTrackPoints(IProgressMonitor monitor) throws IOException{
			PatrolQuery pq = PatrolQueryFactory.createPatrolQuery();
			pq.updateName(SmartDB.getCurrentLanguage(), Messages.MapPlanEditorPage_QueryName);
			pq.setName(Messages.MapPlanEditorPage_QueryName);
			pq.setDateFilter(new DateFilter(PatrolStartDateField.INSTANCE, AllDatesFilter.INSTANCE));
			pq.setQueryFilter(generateQueryString());
			
			//add pq to map
			IService service = QueryServiceFactory.generateQueryService(pq);
			@SuppressWarnings("unchecked")
			List<IGeoResource> layers = (List<IGeoResource>) service.resources(monitor);
			if (layers.size() > 0){
				patrolLayer = layers.get(0);
				return patrolLayer;
			}
			return null;
		}
	};
	
	
    /*
     * Job to refresh the plan target service and map layer 
     */
    private Job refreshJob = new Job(Messages.MapPlanEditorPage_RefreshMapJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			synchronized (lockObj) {
				if (planTargetService != null){
					try {
						planTargetService.refresh(parentEditor.getPlan(), null);
					} catch (IOException e) {
						SmartPlanPlugIn.log(Messages.MapPlanEditorPage_RefreshError, e);
					}
				}
				//clear selection
				mapViewer.getRenderManager().refresh(null);
			}
			return Status.OK_STATUS;
		}
    };
        
    /*
     * Job to refresh the plan target service and map layer 
     */
    private Job refreshPatrolsJob = new Job(Messages.MapPlanEditorPage_RefreshMapJobName){
		@Override
		protected IStatus run(IProgressMonitor monitor) {
			synchronized (lockObj) {
				if (patrolLayer != null){
					try{
						PatrolQuery pq = patrolLayer.resolve(PatrolQuery.class, monitor);
						pq.clearCachedResults(); //clear cached results
						if (pq != null){
							pq.setQueryFilter(generateQueryString()); //update filter
						}
					}catch (IOException e){
						SmartPlanPlugIn.log("Error refreshing patrols layers." + e.getMessage(), e); //$NON-NLS-1$
					}
					mapViewer.getRenderManager().refresh(null);
				}
			}
			return Status.OK_STATUS;
		}
    };
    /**
     * Creates a new map page
     * @param editor
     */
	public MapPlanEditorPage(PlanEditor editor){
		super();
		this.parentEditor = editor;
		
	}
	
	/**
	 * Updates the sub plan targets.
	 * 
	 * @param loadedPlan plan with all subplans loaded from database
	 */
	public void updateSubplanTargetLayer(Plan loadedPlan){
		this.subPlanLayer = loadedPlan;
		refreshSubPlanLayer.schedule();
	}
	
	public MultiPageEditorPart getParentEditor() {
		return this.parentEditor;
	}

	/**
	 * refresh the map and track layers
	 */
	public void refreshPlanTargets(){
		refreshJob.cancel();
		refreshJob.schedule();
	}
	
	/**
	 * refreshes the patrol tracks layer
	 */
	public void refreshPatrols(){
		refreshPatrolsJob.cancel();
		refreshPatrolsJob.schedule();
	}

	private void addLayers(){
		if (loadDefaultLayers != null){
			loadDefaultLayers.cancel();			
		}
		loadDefaultLayers = new LoadDefaultLayersJob(getMap(), false);
		loadDefaultLayers.schedule();	
		
		addLayerJob.schedule();
		
		addInitialZoomFunction();
	}
	
	/**
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
	}

	
	/** Creates the map
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createPartControl(Composite parent) {
		super.createPartControl(parent);
        addLayers();
	}

	@Override
	public void dispose() {
		isDisposing = true;
		JobUtil.stopJobs(refreshSubPlanLayer, loadDefaultLayers, addLayerJob, refreshJob, refreshPatrolsJob);
		
		super.dispose();

		synchronized (lockObj) {
			//dispose of patrol service
			if (planTargetService != null) {
				CatalogPlugin.getDefault().getLocalCatalog().remove(planTargetService);
				planTargetService.dispose(null);
			}

			if (subPlanTargetService != null) {
				CatalogPlugin.getDefault().getLocalCatalog().remove(subPlanTargetService);
				subPlanTargetService.dispose(null);
				subPlanTargetService = null;
			}

			if (patrolLayer != null){
				try{
					IService service = patrolLayer.resolve(IService.class, null);
					CatalogPlugin.getDefault().getLocalCatalog().remove(service);
					service.dispose(null);
					patrolLayer.dispose(null);
					patrolLayer = null;
				}catch(IOException ex){
					SmartPlanPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
	}

    private String generateQueryString(){
    	final Set<PatrolEditorInput> childPatrols = new HashSet<PatrolEditorInput>(); 
		final List<PatrolEditorInput> myPatrols;
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			myPatrols = PlanHibernateManager.getPatrols(parentEditor.getPlan(), s);
			Plan thisPlan = (Plan) s.get(Plan.class, parentEditor.getPlan().getUuid());	//load a copy so we don't have problems with trying to have plan open in multiple sessions
			parentEditor.getChildPlanPatrols(thisPlan, childPatrols, s);
			s.getTransaction().rollback();
		}finally{
			s.close();
		}

		myPatrols.addAll(childPatrols);
		
		StringBuilder query = new StringBuilder();
		
		if (myPatrols.size() == 0){
			return "observation|patrol:uuid equals \"\""; //$NON-NLS-1$
		}
		for (PatrolEditorInput i : myPatrols ){
			if (query.length() > 0){
				query.append(" OR "); //$NON-NLS-1$
			}
			query.append("patrol:uuid equals\"" + SmartUtils.encodeHex(i.getUuid()) + "\""); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "observation|" + query.toString(); //$NON-NLS-1$
    }
    
    
    /*
     * Creates the layer default style
     */
    private Style createDefaultTrackStyle(){
    	StyleFactory sf = CommonFactoryFinder.getStyleFactory();
    	FilterFactory ff = CommonFactoryFinder.getFilterFactory();
    	LineSymbolizer ls = sf.createLineSymbolizer();
    	ls.setStroke(sf.createStroke(ff.literal("#0000FF"), ff.literal(1))); //$NON-NLS-1$
    	
    	FeatureTypeStyle fts = sf.createFeatureTypeStyle();
    	
    	Style style = sf.createStyle();
    	style.featureTypeStyles().add(fts);
    	
    	Rule r= sf.createRule();
    	fts.rules().add(r);
    	r.symbolizers().add(ls);
    
    	return style;
    }
    
    
    private class AddPlanningLayers extends AbstractCommand implements UndoableMapCommand{
		
    	private List<IGeoResource> layers = null;
    	private AddLayersCommand command = null;
    	
    	private AddPlanningLayers(List<IGeoResource> layers){
    		this.layers = layers;
    	}
		@Override
		public void run(IProgressMonitor monitor) throws Exception {
			command = new AddLayersCommand(layers, getMap().getLayersInternal().size());
			command.setMap(getMap());
			command.run(monitor);
			
			Layer trackLayer = command.getLayers().get(layers.size() - 1);
			trackLayer.getStyleBlackboard().put(SLDContent.ID, createDefaultTrackStyle());
			trackLayer.refresh(null);
		}
		
		@Override
		public String getName() {
			return Messages.MapPlanEditorPage_CommandName;
		}
		
		@Override
		public void rollback(IProgressMonitor monitor) throws Exception {
			if (command != null){
				command.rollback(monitor);
			}
		}
    }
}


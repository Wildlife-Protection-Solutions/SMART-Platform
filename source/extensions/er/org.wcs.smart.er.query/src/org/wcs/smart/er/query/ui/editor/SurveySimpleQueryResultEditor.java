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
package org.wcs.smart.er.query.ui.editor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.DeleteLayersCommand;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.wcs.smart.er.EcologicalRecordsPlugIn;
import org.wcs.smart.er.map.samplingunit.SamplingUnitService;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.er.query.map.udig.QueryService;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionQueryType;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyObservationQueryType;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQueryType;
import org.wcs.smart.er.query.ui.columns.SurveyQueryColumnManager;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Editor for displaying survey query results.  The editor includes two pages
 * a tabular results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveySimpleQueryResultEditor extends QueryResultsEditor{

	public static final String ID = "org.wcs.smart.er.query.ui.SimpleQueryResultsEditor";  //$NON-NLS-1$

	private SurveyQueryEventManager.SurveyDesignChangeListener updateTable = new SurveyQueryEventManager.SurveyDesignChangeListener(){
		@Override
		public void surveyDesignChange(ISurveyQuery query) {
			if (!getQuery().equals(query)) return;
			
			getQueryResultsTable().clearColumns();
			getQueryResultsTable().initQuery(getQueryInternal());
		
			if (!(getQuery() instanceof MissionQuery)){
				addSuLayer.schedule();
			}
		}
	};
	
	private SamplingUnitService service = null;
	
	/**
	 * Creates a new results editor
	 */
	public SurveySimpleQueryResultEditor(){
		super();
		SurveyQueryEventManager.getInstance().addSurveyDesignChangeListener(updateTable);	
	}
	
	
	/**
	 * Disposes editor
	 */
	@Override
	public void dispose(){
		super.dispose();
		SurveyQueryEventManager.getInstance().removeSurveyDesignChangeListener(updateTable);
		
		if (service != null){
			CatalogPlugin.getDefault().getLocalCatalog().remove(service);
			service.dispose(null);
			service = null;
		}
	}
	
	
	/**
	 * Creates a new query of the given type
	 * @param type
	 * @return
	 */
	public Query createNewQuery(IQueryType type){
		return SurveyQueryFactory.createQuery(type);
	}
	
	@Override
	protected IDateFieldFilter[] getDateFilterOptions(){
		if (getQueryInternal() instanceof SurveyObservationQuery){
			return SurveyObservationQueryType.validDateFields();
		}else if (getQueryInternal() instanceof MissionQuery){
			return MissionQueryType.validDateFields();
		}else if (getQueryInternal() instanceof SurveyWaypointQuery){
			return SurveyWaypointQueryType.validDateFields();
		}
		return null;
	}
	
	@Override
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column){
		return SurveyQueryColumnManager.getLabelProvider(column);
	}

	@Override
	public IQueryService createQueryService() {
		//return null;
		return new QueryService(getQuery());
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		
		IQueryType type = ((QueryEditorInput)input).getType();
		if (!type.getKey().equals(MissionQueryType.KEY)){
			addSuLayer.schedule();
		}
	}
	
	private Job addSuLayer = new Job(Messages.SurveySimpleQueryResultEditor_LoadSuJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			if (getQuery() instanceof ISurveyQuery){
				ISurveyQuery qq = (ISurveyQuery) getQuery();
				
				SurveyDesign sd = qq.getSurveyDesignAsObject();
				if(sd != null){
					if (service != null &&
							service.getSurveyDesign().equals(sd)){
						//we don't have to do anything
						return Status.OK_STATUS;
					}
					disposeService(monitor);
					service = new SamplingUnitService(sd);
					try{
				    	@SuppressWarnings("unchecked")
						List<IGeoResource> layers = (List<IGeoResource>) service.resources(null);
				    	AddLayersCommand command = new AddLayersCommand(layers);
				    	if (getMap() == null) return Status.CANCEL_STATUS;
			    		getMap().sendCommandASync(command);
					}catch (Exception ex){
						EcologicalRecordsPlugIn.log("Error adding survey design sampling unit layers.", ex); //$NON-NLS-1$
					}
					return Status.OK_STATUS;
				}
			}
			//dispose service
			disposeService(monitor);
			return Status.OK_STATUS;
		}
		
		private void disposeService(IProgressMonitor monitor){
			if (service == null) return;
			try {
				List<? extends IGeoResource> resources = service.resources(monitor);
				List<ILayer> toDelete = new ArrayList<ILayer>();
				for (IGeoResource r : resources){
					for( ILayer layer : getMap().getLayersInternal() ) {
	                	if(layer.getID().equals(r.getIdentifier())){
	                		toDelete.add(layer);
	                		break;
	                	}
	                }
				}
				if (toDelete.size() > 0){
					DeleteLayersCommand cmd = new DeleteLayersCommand(toDelete.toArray(new ILayer[toDelete.size()]));
					getMap().sendCommandASync(cmd);
				}
			} catch (IOException e) {
				EcologicalRecordsPlugIn.log("Error disposing survey design sampling unit layers.", e); //$NON-NLS-1$
			}
			CatalogPlugin.getDefault().getLocalCatalog().remove(service);
			service.dispose(monitor);
			service = null;
		}
		
	};
}

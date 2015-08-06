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

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.locationtech.udig.project.internal.Map;
import org.wcs.smart.er.ISurveyEventListener;
import org.wcs.smart.er.SurveyEventHandler;
import org.wcs.smart.er.SurveyEventHandler.EventType;
import org.wcs.smart.er.model.SurveyDesign;
import org.wcs.smart.er.query.map.udig.QueryService;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.MissionQuery;
import org.wcs.smart.er.query.model.MissionQueryType;
import org.wcs.smart.er.query.model.MissionTrackQuery;
import org.wcs.smart.er.query.model.MissionTrackQueryType;
import org.wcs.smart.er.query.model.SurveyObservationQuery;
import org.wcs.smart.er.query.model.SurveyObservationQueryType;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
import org.wcs.smart.er.query.model.SurveyWaypointQuery;
import org.wcs.smart.er.query.model.SurveyWaypointQueryType;
import org.wcs.smart.er.query.ui.columns.SurveyQueryColumnManager;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.ISummaryInfo;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

/**
 * Editor for displaying survey query results.  The editor includes two pages
 * a tabular results page and a map results page.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class SurveySimpleQueryResultEditor extends QueryResultsEditor{

	public static final String ID = "org.wcs.smart.er.query.ui.SimpleQueryResultsEditor";  //$NON-NLS-1$

	private SurveyQueryEventManager.QuerySurveyDesignChangeListener updateTable = new SurveyQueryEventManager.QuerySurveyDesignChangeListener(){
		@Override
		public void surveyDesignChange(ISurveyQuery query, SurveyDesign newDesign) {
			if (!getQuery().equals(query)) return;
			
			//clear current results
			page1.clearTable();
			
			//update table columns
			getQueryResultsTable().clearColumns();
			getQueryResultsTable().initQuery(getQueryInternal());
			
			//update layer
			page2.reset(false);
			addSuLayer.schedule();
			
			
		}
	};
	
	private ISurveyEventListener surveyDesignListener = new ISurveyEventListener() {
		@Override
		public void event(Object o) {
			if (o instanceof SurveyDesign){
				String qd = ((ISurveyQuery)getQuery()).getSurveyDesign();
				if (qd != null && qd.equals(((SurveyDesign) o).getKeyId())){
					reparseQuery();		
				}
			}
				
		}
	};
	
	private AddSamplingUnitLayersJob addSuLayer = null;
	
	/**
	 * Creates a new results editor
	 */
	public SurveySimpleQueryResultEditor(){
		super();
		SurveyQueryEventManager.getInstance().addSurveyDesignChangeListener(updateTable);
		SurveyEventHandler.getInstance().addListener(EventType.SURVEY_DESIGN_MODIFIED, surveyDesignListener);
	}
	
	
	/**
	 * Disposes editor
	 */
	@Override
	public void dispose(){
		super.dispose();
		SurveyQueryEventManager.getInstance().removeSurveyDesignChangeListener(updateTable);
		SurveyEventHandler.getInstance().removeListener(EventType.SURVEY_DESIGN_MODIFIED, surveyDesignListener);
		addSuLayer.dispose();	
	}
	
	
	/**
	 * Creates a new query of the given type
	 * @param type
	 * @return
	 */
	public Query createNewQuery(IQueryType type){
		return SurveyQueryFactory.createQuery(type);
	}
	
	private boolean isQueryType(String typeKey){
		return (getQueryInternal().getTypeKey().equals(typeKey));
	}
	
	@Override
	protected IDateFieldFilter[] getDateFilterOptions(){
		if (isQueryType(SurveyObservationQuery.KEY)){
			return SurveyObservationQueryType.validDateFields();
		}else if (isQueryType(MissionQuery.KEY)){
			return MissionQueryType.validDateFields();
		}else if (isQueryType(SurveyWaypointQuery.KEY)){
			return SurveyWaypointQueryType.validDateFields();
		}else if (isQueryType(MissionTrackQuery.KEY)){
			return MissionTrackQueryType.validDateFields();
		}
		return null;
	}
	
	@Override
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column){
		return SurveyQueryColumnManager.getLabelProvider(column);
	}

	@Override
	public IQueryService createQueryService() {
		return new QueryService(getQuery());
	}
	
	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {
		super.init(site, input);
		
		addSuLayer = new AddSamplingUnitLayersJob(){
			@Override
			public Query getQuery() {
				return SurveySimpleQueryResultEditor.this.getQuery();
			}
			@Override
			public Map getMap() {
				return SurveySimpleQueryResultEditor.this.getMap();
			}};
		addSuLayer.schedule();
	}

	@Override
	protected ISummaryInfo createInfoSection(){
		if (isQueryType(MissionTrackQuery.KEY)){
			return new MissionTrackInfoSection();
		}else if (isQueryType(MissionQuery.KEY)){
			
			return new MissionInfoSection();
		}else{
			return super.createInfoSection();
		}
	}
}

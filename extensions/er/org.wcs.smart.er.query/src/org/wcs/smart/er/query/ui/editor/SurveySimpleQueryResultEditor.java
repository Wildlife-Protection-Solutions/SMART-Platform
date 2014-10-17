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

import net.refractions.udig.project.internal.Map;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.wcs.smart.er.query.map.udig.QueryService;
import org.wcs.smart.er.query.model.ISurveyQuery;
import org.wcs.smart.er.query.model.MissionQueryType;
import org.wcs.smart.er.query.model.MissionTrackQueryType;
import org.wcs.smart.er.query.model.SurveyObservationQueryType;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
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

	private SurveyQueryEventManager.SurveyDesignChangeListener updateTable = new SurveyQueryEventManager.SurveyDesignChangeListener(){
		@Override
		public void surveyDesignChange(ISurveyQuery query) {
			if (!getQuery().equals(query)) return;
			
			getQueryResultsTable().clearColumns();
			getQueryResultsTable().initQuery(getQueryInternal());
		
			addSuLayer.schedule();
		}
	};
	
	private AddSamplingUnitLayersJob addSuLayer = null;
	
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
		return (getQueryInternal().getType().getKey().equals(typeKey));
	}
	
	@Override
	protected IDateFieldFilter[] getDateFilterOptions(){
		if (isQueryType(SurveyObservationQueryType.KEY)){
			return SurveyObservationQueryType.validDateFields();
		}else if (isQueryType(MissionQueryType.KEY)){
			return MissionQueryType.validDateFields();
		}else if (isQueryType(SurveyWaypointQueryType.KEY)){
			return SurveyWaypointQueryType.validDateFields();
		}else if (isQueryType(MissionTrackQueryType.KEY)){
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
		if (isQueryType(MissionTrackQueryType.KEY)){
			return new MissionTrackInfoSection();
		}else if (isQueryType(MissionQueryType.KEY)){
			return new MissionInfoSection();
		}else{
			return super.createInfoSection();
		}
	}
}

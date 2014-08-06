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
import org.wcs.smart.er.query.model.SurveyObservationQueryType;
import org.wcs.smart.er.query.model.SurveyQueryFactory;
import org.wcs.smart.er.query.ui.columns.SurveyQueryColumnManager;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.event.IQueryListener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.event.QueryListenerAdapter;
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

	IQueryListener updateTable = new QueryListenerAdapter(){
		@Override
		public void queryModified(int eventType, Object object) {
			if (eventType == IQueryListener.QUERY_DEFINITION_MODIFIED &&
					object.equals(query.getQuery())){
				//TODO: figure out how to only do this if the survey design associated
				//with the query has changed.
				getQueryResultsTable().clearColumns();
				getQueryResultsTable().initQuery(getQueryInternal());
				
			}
		}
	};
	public SurveySimpleQueryResultEditor(){
		super();
		
		QueryEventManager.getInstance().addListener(updateTable);
	}
	
	
	@Override
	public void dispose(){
		super.dispose();
		QueryEventManager.getInstance().removeListener(updateTable);
	}
	
	
	/**
	 * Creates a new query of the given type
	 * @param type
	 * @return
	 */
	public Query createNewQuery(IQueryType type){
		return SurveyQueryFactory.createQuery(type);
	}
	
	protected IDateFieldFilter[] getDateFilterOptions(){
		return SurveyObservationQueryType.validDateFields();
	}
	
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column){
		return SurveyQueryColumnManager.getLabelProvider(column);
	}

	@Override
	public IQueryService createQueryService() {
//		return new QueryService(getQuery());
		return null;
	}
}

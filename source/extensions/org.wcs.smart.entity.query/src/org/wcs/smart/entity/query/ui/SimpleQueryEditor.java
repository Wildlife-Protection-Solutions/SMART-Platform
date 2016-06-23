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
package org.wcs.smart.entity.query.ui;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.entity.query.EntityQueryPlugIn;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.entity.query.map.udig.QueryService;
import org.wcs.smart.entity.query.model.EntityObservationQuery;
import org.wcs.smart.entity.query.model.EntityQueryFactory;
import org.wcs.smart.entity.query.model.EntityWaypointQuery;
import org.wcs.smart.entity.query.model.columns.EntityAttributeQueryColumn;
import org.wcs.smart.entity.query.model.columns.EtAttributeQueryColumn;
import org.wcs.smart.entity.query.model.columns.EtCategoryQueryColumn;
import org.wcs.smart.entity.query.model.columns.FixedQueryColumn;
import org.wcs.smart.entity.query.model.type.EntityObservationQueryType;
import org.wcs.smart.entity.query.model.type.EntityWaypointQueryType;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.QueryColumnLabelProvider;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
/**
 * Query editor for simple observation queries
 * @author Emily
 *
 */
public class SimpleQueryEditor extends QueryResultsEditor {
	
	public static final String ID = "org.wcs.smart.entity.editor.simple"; //$NON-NLS-1$
	
	@Override
	public Query createNewQuery(IQueryType type) {
		return EntityQueryFactory.createQuery(type);
	}

	@Override
	public IQueryService createQueryService() {
		return new QueryService(query.getQuery());
	}

	@Override
	protected IDateFieldFilter[] getDateFilterOptions() {
		if (getInputInternal().getType().getKey().equals(EntityObservationQuery.KEY)){
			return EntityObservationQueryType.validDateFields();
		}else if (getInputInternal().getType().getKey().equals(EntityWaypointQuery.KEY)){
			return EntityWaypointQueryType.validDateFields();
		}
		return null;
	}

	@Override
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column, List<QueryColumn> allColumns) {
		if (column instanceof FixedQueryColumn){
			return new QueryColumnLabelProvider(column);
		}else if (column instanceof EtAttributeQueryColumn){
			return new AttributeColumnLabelProvider(column);
		}else if (column instanceof EtCategoryQueryColumn){
			return new CategoryColumnLabelProvider(column);
		}else if (column instanceof GridQueryColumn){
			return new QueryColumnLabelProvider(column);
		}else if (column instanceof EntityAttributeQueryColumn){
			return new AttributeColumnLabelProvider(column);
		}
		return null;

	}
	
	
	/**
	 * Re-run the query and refresh the results.
	 */
	@Override
	public void refreshQuery(){
		
		//update query columns
		if (getQuery() instanceof EntityObservationQuery){
			updateQueryColumnJob.setSystem(true);
			updateQueryColumnJob.schedule();
			try{
				updateQueryColumnJob.join();
			}catch (Exception ex){
				EntityQueryPlugIn.log(ex.getMessage(), ex);
			}
		}
		super.refreshQuery();
		
	}
	
	
	private Job updateQueryColumnJob = new Job(Messages.SimpleQueryEditor_JobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			final SimpleQuery q = (SimpleQuery) getQuery();
			
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					//need to clear and rebuild the query columns as filter may results it addition of entity attribute columns
					page1.getQueryResultsTable().clearColumns();
					page1.getQueryResultsTable().initQuery(q, SimpleQueryEditor.this);
				}});	
			
			return Status.OK_STATUS;
		}
		
	};

}

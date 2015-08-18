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
package org.wcs.smart.observation.query.ui;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.wcs.smart.observation.query.map.udig.QueryService;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationQueryFactory;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.observation.query.model.columns.ObservationAttributeQueryColumn;
import org.wcs.smart.observation.query.model.columns.ObservationCategoryQueryColumn;
import org.wcs.smart.observation.query.model.types.ObservationQueryType;
import org.wcs.smart.observation.query.model.types.ObservationWaypointQueryType;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.GridColumnLabelProvider;
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
	
	public static final String ID = "org.wcs.smart.observation.editor.simple"; //$NON-NLS-1$
	
	@Override
	public Query createNewQuery(IQueryType type) {
		return ObservationQueryFactory.createQuery(type);
	}

	@Override
	public IQueryService createQueryService() {
		return new QueryService(query.getQuery());
	}

	@Override
	protected IDateFieldFilter[] getDateFilterOptions() {
		if (getInputInternal().getType().getKey().equals(ObsObservationQuery.KEY)){
			return ObservationQueryType.validDateFields();
		}else if (getInputInternal().getType().getKey().equals(ObservationWaypointQuery.KEY)){
			return ObservationWaypointQueryType.validDateFields();
		}
		return null;
	}

	@Override
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column) {
		if (column instanceof FixedQueryColumn){
			return new FixedColumnLabelProvider(column);
		}else if (column instanceof ObservationAttributeQueryColumn){
			return new AttributeColumnLabelProvider(column);
		}else if (column instanceof ObservationCategoryQueryColumn){
			return new CategoryColumnLabelProvider(column);
		}else if (column instanceof GridQueryColumn){
			return new GridColumnLabelProvider(column);
		}
		return null;

	}

}

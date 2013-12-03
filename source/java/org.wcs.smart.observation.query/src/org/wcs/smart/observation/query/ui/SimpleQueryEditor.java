package org.wcs.smart.observation.query.ui;

import org.eclipse.jface.viewers.CellLabelProvider;
import org.wcs.smart.observation.query.map.udig.QueryService;
import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationQueryFactory;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.observation.query.model.columns.AttributeQueryColumn;
import org.wcs.smart.observation.query.model.columns.CategoryQueryColumn;
import org.wcs.smart.observation.query.model.columns.FixedQueryColumn;
import org.wcs.smart.observation.query.model.columns.GridQueryColumn;
import org.wcs.smart.observation.query.model.types.ObservationQueryType;
import org.wcs.smart.observation.query.model.types.ObservationWaypointQueryType;
import org.wcs.smart.query.common.model.udig.IQueryService;
import org.wcs.smart.query.common.ui.GridColumnLabelProvider;
import org.wcs.smart.query.common.ui.QueryResultsEditor;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryColumn;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;

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
		if (query.getQuery() instanceof ObsObservationQuery){
			return ObservationQueryType.validDateFields();
		}else if (query.getQuery() instanceof ObservationWaypointQuery){
			return ObservationWaypointQueryType.validDateFields();
		}
		return null;
	}

	@Override
	protected CellLabelProvider getColumnLabelProvider(QueryColumn column) {
		if (column instanceof FixedQueryColumn){
			return new FixedColumnLabelProvider(column);
		}else if (column instanceof AttributeQueryColumn){
			return new AttributeColumnLabelProvider(column);
		}else if (column instanceof CategoryQueryColumn){
			return new CategoryColumnLabelProvider(column);
		}else if (column instanceof GridQueryColumn){
			return new GridColumnLabelProvider(column);
		}
		return null;

	}

}

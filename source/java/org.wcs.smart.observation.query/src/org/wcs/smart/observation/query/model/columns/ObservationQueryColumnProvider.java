package org.wcs.smart.observation.query.model.columns;

import org.wcs.smart.observation.query.model.ObsObservationQuery;
import org.wcs.smart.observation.query.model.ObservationGriddedQuery;
import org.wcs.smart.observation.query.model.ObservationWaypointQuery;
import org.wcs.smart.query.model.QueryColumn;

public class ObservationQueryColumnProvider implements IObservationQueryColumnProvider {

	@Override
	public QueryColumn[] getQueryColumns(String queryTypeKey) {
		if (queryTypeKey.equals(ObsObservationQuery.KEY)){
			return ObservationQueryColumnCache.getInstance().getObservationQueryColumns();
		}else if (queryTypeKey.equals(ObservationWaypointQuery.KEY)){
			return ObservationQueryColumnCache.getInstance().getWaypointQueryColumns();
		}else if (queryTypeKey.equals(ObservationGriddedQuery.KEY)){
			return ObservationQueryColumnCache.getInstance().getGridColumns();
		}
		return null;
	}

}

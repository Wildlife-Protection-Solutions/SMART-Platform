package org.wcs.smart.patrol.query.model.observation;

import org.wcs.smart.patrol.query.model.IPatrolQueryColumnProvider;
import org.wcs.smart.patrol.query.model.PatrolGriddedQuery;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.patrol.query.model.PatrolQuery;
import org.wcs.smart.patrol.query.model.PatrolWaypointQuery;
import org.wcs.smart.query.model.QueryColumn;

public class PatrolQueryColumnProvider implements IPatrolQueryColumnProvider {

	@Override
	public QueryColumn[] getQueryColumns(String queryTypeKey) {
		if (queryTypeKey.equals(PatrolObservationQuery.KEY)){
			return PatrolQueryColumnCache.getInstance().getObservationQueryColumns();
		}
		if (queryTypeKey.equals(PatrolWaypointQuery.KEY)){
			return PatrolQueryColumnCache.getInstance().getWaypointQueryColumns();
		}
		if (queryTypeKey.equals(PatrolQuery.KEY)){
			return PatrolQueryColumnCache.getInstance().getPatrolQueryColumns();
		}
		if (queryTypeKey.equals(PatrolGriddedQuery.KEY)){
			return PatrolQueryColumnCache.getInstance().getGridColumns();
		}
		return null;
	}

}

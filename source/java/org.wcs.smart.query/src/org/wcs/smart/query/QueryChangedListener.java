package org.wcs.smart.query;

import org.wcs.smart.query.model.WaypointQuery;

public interface QueryChangedListener {

	public void queryChanged(WaypointQuery query);
}

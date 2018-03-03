package org.wcs.smart.i2.query.engine;

import org.wcs.smart.i2.IIntelQueryEngine;
import org.wcs.smart.i2.IQueryEngineFactory;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;

public class QueryEngineFactory implements IQueryEngineFactory {

	@Override
	public IIntelQueryEngine findQueryEngine(String queryType) {
		if (queryType.equals(IntelEntitySummaryQuery.KEY)) return new IntelEntitySummaryQueryEngine();
		if (queryType.equals(IntelRecordObservationQuery.KEY)) return new IntelObservationQueryEngine();

		return null;
	}

}

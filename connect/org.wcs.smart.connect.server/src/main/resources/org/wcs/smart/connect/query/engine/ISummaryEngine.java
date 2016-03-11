package org.wcs.smart.connect.query.engine;

import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.observation.query.model.ObservationSummaryQuery;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.common.model.SummaryQueryResult;

public interface ISummaryEngine extends IQueryEngine{

	public void getHeaderInfo(SummaryQuery query, 
			SummaryQueryResult results,
			Locale l,
			Session session) throws Exception;
}

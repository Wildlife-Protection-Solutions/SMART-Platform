package org.wcs.smart.intelligence.query.model;

import org.wcs.smart.intelligence.query.IIntelligenceQueryColumnProvider;
import org.wcs.smart.intelligence.query.internal.IntelligenceQueryColumnCache;
import org.wcs.smart.query.model.QueryColumn;

public class IntelligenceQueryColumnProvider implements
		IIntelligenceQueryColumnProvider {

	@Override
	public QueryColumn[] getQueryColumns(String queryTypeKey) {
		if (queryTypeKey.equals(IntelligenceRecordQuery.KEY)){
			return IntelligenceQueryColumnCache.getInstance().getIntelligenceRecordQueryColumns();
		}
		
		return null;
	}

}

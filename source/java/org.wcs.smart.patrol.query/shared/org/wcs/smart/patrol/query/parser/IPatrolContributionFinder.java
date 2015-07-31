package org.wcs.smart.patrol.query.parser;

import java.util.List;

public interface IPatrolContributionFinder {
	
	public List<IQueryFilterPatrolContribution> getFilterContributions();
	
	public List<IGroupByPatrolContribution> getGroupByContributions();
	
}

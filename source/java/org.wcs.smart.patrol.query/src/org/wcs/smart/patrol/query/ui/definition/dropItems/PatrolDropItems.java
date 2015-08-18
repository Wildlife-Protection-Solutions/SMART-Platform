package org.wcs.smart.patrol.query.ui.definition.dropItems;

import org.wcs.smart.patrol.query.model.PatrolDropItemFactory;
import org.wcs.smart.patrol.query.model.PatrolQueryOptions;
import org.wcs.smart.patrol.query.model.PatrolValueOption;
import org.wcs.smart.query.ui.model.IValueDropItem;

public class PatrolDropItems {

	public final static IValueDropItem[] SUMMARY_ENCOUNTER_RATE_DROP_OPTIONS;
	static{
		IValueDropItem[] ditems = new IValueDropItem[PatrolQueryOptions.SUMMARY_ENCOUNTER_RATE_OPTIONS.length];
		int i = 0;
		for (PatrolValueOption op : PatrolQueryOptions.SUMMARY_ENCOUNTER_RATE_OPTIONS){
			PatrolValueDropItem item = (PatrolValueDropItem) PatrolDropItemFactory.INSTANCE.createPatrolValueDropItem(op);
			ditems[i++] = item; 
		}	
		SUMMARY_ENCOUNTER_RATE_DROP_OPTIONS = ditems;
	}
	
	public final static IValueDropItem[] GRID_ENCOUNTER_RATE_DROP_OPTIONS;
	static{
		IValueDropItem[] ditems = new IValueDropItem[PatrolQueryOptions.GRID_ENCOUNTER_RATE_OPTIONS.length];
		int i = 0;
		for (PatrolValueOption op : PatrolQueryOptions.GRID_ENCOUNTER_RATE_OPTIONS){
			ditems[i++] = (IValueDropItem) PatrolDropItemFactory.INSTANCE.createPatrolValueDropItem(op);
		}	
		GRID_ENCOUNTER_RATE_DROP_OPTIONS = ditems;
	}
}

package org.wcs.smart.entity.query;

import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.query.model.filter.ConservationAreaFilter;
import org.wcs.smart.query.model.filter.DateFilter;

public class EntityQuery {

	
	private DateFilter dateFilter;
	private EntityType entityType;
	private EntityFilter entityFilter;
	private ConservationAreaFilter caFilter;
	
	public EntityQuery(EntityType entityType, 
			DateFilter dateFilter, 
			EntityFilter entityFilter){
		
		this.entityType = entityType;
		this.dateFilter = dateFilter;
		this.entityFilter = entityFilter;
		
		this.caFilter = new ConservationAreaFilter(true);
	}
	
	public DateFilter getDateFilter(){
		return this.dateFilter;
	}
	
	public ConservationAreaFilter getConservationAreaFilter(){
		return this.caFilter;
	}
	
	public EntityFilter getEntityFilter(){
		return this.entityFilter;
	}
	public EntityType getEntityType(){
		return this.entityType;
	}
}

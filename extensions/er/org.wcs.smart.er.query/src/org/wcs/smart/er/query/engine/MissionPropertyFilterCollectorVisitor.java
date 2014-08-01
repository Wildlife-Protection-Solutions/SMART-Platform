package org.wcs.smart.er.query.engine;

import java.util.HashSet;
import java.util.Set;

import org.wcs.smart.er.query.filter.MissionPropertyFilter;
import org.wcs.smart.query.model.filter.AttributeInfo;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;

public class MissionPropertyFilterCollectorVisitor implements IFilterVisitor{

	private HashSet<AttributeInfo> filters = new HashSet<AttributeInfo>();

	@Override
	public void visit(IFilter filter) {
		if (filter instanceof MissionPropertyFilter){
			MissionPropertyFilter f = (MissionPropertyFilter) filter;
			AttributeInfo in = new AttributeInfo(f.getAttributeKey(),f.getAttributeType());
			if (!filters.contains(in)){
				filters.add(in);
			}
			
		}
	}
	
	/**
	 * 
	 * @return list of attribute filters found
	 */
	public Set<AttributeInfo> getAttributeInfo(){
		return this.filters;
	}
}


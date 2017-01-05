package org.wcs.smart.i2.query.observation.filter;

public class NotFilter implements IQueryFilter {
	
	
	public static NotFilter create(IQueryFilter filter){
		return new NotFilter(filter);
	}
	
	private IQueryFilter filter;
	
	public NotFilter(IQueryFilter filter){
		this.filter = filter;
	}
	
	public IQueryFilter getFilter(){
		return this.filter;
	}
}

package org.wcs.smart.i2.query.observation.filter;

public class BracketFilter implements IQueryFilter {

	public static BracketFilter create(IQueryFilter filter){
		return new BracketFilter(filter);
	}
	
	private IQueryFilter filter;
	
	public BracketFilter(IQueryFilter filter){
		this.filter = filter;
	}
	
	public IQueryFilter getFilter(){
		return this.filter;
	}
}

package org.wcs.smart.i2.query.observation.filter;

public class ParsedObservationQuery {

	public static ParsedObservationQuery create(IQueryFilter.FilterType type, IQueryFilter filter){
		return new ParsedObservationQuery(type, filter);
	}
	private IQueryFilter.FilterType type;
	private IQueryFilter filter;
	
	public ParsedObservationQuery(IQueryFilter.FilterType type, IQueryFilter filter){
		this.type = type;
		this.filter = filter;
	}
}

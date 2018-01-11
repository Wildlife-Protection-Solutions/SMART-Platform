package org.wcs.smart.asset.map.engine;

public class BracketFilter implements IFilter{
	public static BracketFilter parse(IFilter exp) {
		return new BracketFilter(exp);
	}
	
	
	
	IFilter filter1;
	
	public BracketFilter(IFilter exp) {
		this.filter1 = exp;
		
	}
	

	@Override
	public String toString() {
		return " ( " + filter1.toString() + " ) ";
	
	}
}

package org.wcs.smart.asset.map.engine;

public class ColumnFilter implements IFilter{

	public static ColumnFilter parse (String key) {
		return new ColumnFilter(key);
	}

	private String key;
	
	public ColumnFilter(String key) {
		this.key = key;
	}
	
	public String getColumnKey() { 
		return key;
	}
	
	@Override
	public String toString() {
		return key;
	}
	
	@Override
	public void accept(IFilterVisitor visitor) {
		visitor.visit(this);
	}
}

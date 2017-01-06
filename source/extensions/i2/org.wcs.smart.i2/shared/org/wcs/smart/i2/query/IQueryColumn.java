package org.wcs.smart.i2.query;

import java.util.Locale;

public interface IQueryColumn {

	public String getColumnName();
	
	public String getKey();
	
	public boolean isVisible();
	
	public abstract String getValue(IResultItem item, Locale l);
	
	default public String getTooltip(){
		return null;
	}
}

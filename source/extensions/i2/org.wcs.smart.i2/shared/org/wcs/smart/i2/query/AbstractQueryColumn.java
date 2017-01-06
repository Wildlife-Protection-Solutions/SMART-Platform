package org.wcs.smart.i2.query;

import java.util.Locale;

public abstract class AbstractQueryColumn implements IQueryColumn{

	private String name;
	private String key;
	
	private boolean isVisible = true;
	
	public AbstractQueryColumn(String columnName, String key){
		this.name = columnName;
		this.key = key;
	}
	
	@Override
	public String getColumnName(){
		return this.name;
	}
	
	@Override
	public String getKey(){
		return this.key;
	}
	
	@Override
	public boolean isVisible(){
		return this.isVisible;
	}
	
	public void setVisible(boolean isVisible){
		this.isVisible = isVisible;
	}
	
	public abstract String getValue(IResultItem item, Locale l);
}

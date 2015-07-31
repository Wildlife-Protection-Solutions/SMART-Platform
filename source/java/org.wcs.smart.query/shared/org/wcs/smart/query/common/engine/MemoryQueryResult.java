package org.wcs.smart.query.common.engine;

import java.util.List;

public class  MemoryQueryResult<T extends IResultItem> implements IQueryResult {

	private List<T> data;
	
	public MemoryQueryResult(List<T> data){
		this.data = data;
	}
	
	public List<T> getData(){
		return this.data;
	}
}

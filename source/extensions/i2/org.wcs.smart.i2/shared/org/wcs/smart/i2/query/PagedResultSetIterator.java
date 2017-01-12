package org.wcs.smart.i2.query;

import java.util.List;

public class PagedResultSetIterator {

	private IPagedQueryResultSet results;
	
	private int pageCount = -1;
	private int index = 0;
	
	List<? extends IResultItem> currentData = null;
	
	public PagedResultSetIterator(IPagedQueryResultSet results){
		this.results = results;
		
		results.getItemCount();
	}
	
	public boolean hasNext(){
		return (pageCount * IPagedQueryResultSet.MAP_PAGE_SIZE) + index < results.getItemCount();
	}
	
	public IResultItem next(){
		if (currentData == null || index >= currentData.size()){
			pageCount++;
			index = 0;
			currentData = results.getData(pageCount*IPagedQueryResultSet.MAP_PAGE_SIZE, IPagedQueryResultSet.MAP_PAGE_SIZE);
		}
		
		return currentData.get(index++);
	}
	
}

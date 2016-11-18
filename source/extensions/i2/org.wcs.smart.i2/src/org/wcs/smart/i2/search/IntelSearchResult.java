package org.wcs.smart.i2.search;

import java.util.List;

public class IntelSearchResult {

	private long totalMatched;
	private List<IntelEntitySearchResult> results;
	
	public IntelSearchResult(long totalCnt, List<IntelEntitySearchResult> results){
		this.totalMatched = totalCnt;
		this.results = results;
	}
	
	public long getTotalMatched(){
		return this.totalMatched;
	}
	
	public List<IntelEntitySearchResult> getResults(){
		return this.results;
	}
	
}

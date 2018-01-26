package org.wcs.smart.asset.query.engine.visitors;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.asset.query.parser.internal.filter.AssetFilter;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilterVisitor;


public class AssetFilterCollector implements IFilterVisitor{

	private List<AssetFilter> assetFilters = new ArrayList<>();
	
	@Override
	public void visit(IFilter filter) {
		if (filter instanceof AssetFilter){
			assetFilters.add((AssetFilter) filter);
		}
	}
	
	public List<AssetFilter> getFilters(){ 
		return this.assetFilters; 
	}
}
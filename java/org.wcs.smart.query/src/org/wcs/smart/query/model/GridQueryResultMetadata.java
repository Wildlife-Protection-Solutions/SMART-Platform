package org.wcs.smart.query.model;

import java.util.Iterator;
import java.util.List;

public class GridQueryResultMetadata {

	private double minResultValue = Double.NEGATIVE_INFINITY;
	
	private double maxResultValue = Double.POSITIVE_INFINITY;
	
	private long minXTile = 0;
	
	private long maxXTile = 0;
	
	private long minYTile = 0;
	
	private long maxYTile = 0;

	public GridQueryResultMetadata(double minResultValue,
			double maxResultValue, long minXTile, long maxXTile, long minYTile,
			long maxYTile) {
		super();
		this.minResultValue = minResultValue;
		this.maxResultValue = maxResultValue;
		this.minXTile = minXTile;
		this.maxXTile = maxXTile;
		this.minYTile = minYTile;
		this.maxYTile = maxYTile;
	}

	public double getMinResultValue() {
		return minResultValue;
	}

	public double getMaxResultValue() {
		return maxResultValue;
	}

	public long getMinXTile() {
		return minXTile;
	}

	public long getMaxXTile() {
		return maxXTile;
	}

	public long getMinYTile() {
		return minYTile;
	}

	public long getMaxYTile() {
		return maxYTile;
	}
	
	public static final GridQueryResultMetadata computeMetadata(List<GridResultItem> items){
		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		
		long[] values = new long[]{Long.MAX_VALUE,Long.MIN_VALUE, Long.MAX_VALUE,Long.MIN_VALUE};
		
		for (Iterator<GridResultItem> iterator = items.iterator(); iterator.hasNext();) {
			GridResultItem type = (GridResultItem) iterator.next();
			
			max = Math.max(max, type.getValue());
			min = Math.min(min, type.getValue());
			values[0] = Math.min(values[0], type.getTileX());
			values[1] = Math.max(values[1], type.getTileX());
			values[2] = Math.min(values[2], type.getTileY());
			values[3] = Math.max(values[3], type.getTileY());
			
		}
		
		GridQueryResultMetadata metadata = new GridQueryResultMetadata(
				min, max,values[0], values[1], values[2], values[3]);
		
		return metadata;
	}
}

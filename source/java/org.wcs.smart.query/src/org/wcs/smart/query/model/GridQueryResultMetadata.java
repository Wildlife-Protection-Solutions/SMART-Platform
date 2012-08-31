package org.wcs.smart.query.model;

import java.util.Iterator;
import java.util.List;

public class GridQueryResultMetadata {

	private double minResultValue = Double.NEGATIVE_INFINITY;
	
	private double maxResultValue = Double.POSITIVE_INFINITY;
	
	private int minXTile = 0;
	
	private int maxXTile = 0;
	
	private int minYTile = 0;
	
	private int maxYTile = 0;

	public GridQueryResultMetadata(double minResultValue,
			double maxResultValue, int minXTile, int maxXTile, int minYTile,
			int maxYTile) {
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

	public int getMinXTile() {
		return minXTile;
	}

	public int getMaxXTile() {
		return maxXTile;
	}

	public int getMinYTile() {
		return minYTile;
	}

	public int getMaxYTile() {
		return maxYTile;
	}
	
	public static final GridQueryResultMetadata computeMetadata(List<GridResultItem> items){
		
		double min = Double.POSITIVE_INFINITY;
		double max = Double.NEGATIVE_INFINITY;
		
		int[] values = new int[]{Integer.MAX_VALUE,Integer.MAX_VALUE, Integer.MIN_VALUE,Integer.MIN_VALUE};
		
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

/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.data.oda.smart.query.common;

import java.awt.Point;

import org.eclipse.datatools.connectivity.oda.OdaException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.data.oda.smart.impl.SmartConnection;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.model.GridQueryColumn;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Metadata for gridded query results.
 * 
 * @author Emily
 *
 */
public class GriddedQueryResultSetMetadata extends SimpleQueryResultSetMetadata {

	private CoordinateReferenceSystem crs;
	private Point origin;
	private double cellSize;
	
	private String xColumnName;
	private String yColumnName;
	private String valueColumn;
	
	public GriddedQueryResultSetMetadata(GriddedQuery query, SmartConnection connection) throws OdaException{
		super(query, connection);
		this.geometryColumns = null;
		try{
			this.crs = query.getCoordinateReferenceSystem();
		}catch (Exception ex){
			throw new OdaException(ex);
		}
		this.origin = query.getGridOrigin();
		this.cellSize = query.getGridSize();
		
		for (QueryColumn qc : super.queryColumns){
			if (qc.getKey().equals(GridQueryColumn.GridColumns.TILE_Y.getKey())){
				yColumnName = qc.getName();
			}else if (qc.getKey().equals(GridQueryColumn.GridColumns.TILE_X.getKey())){
				xColumnName = qc.getName();
			}else if (qc.getKey().equals(GridQueryColumn.GridColumns.VALUE.getKey())){
				valueColumn = qc.getName();
			}
		}
	}
	
	/**
	 * 
	 * @return the grid origin
	 */
	public Point getOrigin(){
		return this.origin;
	}
	
	/**
	 * 
	 * @return the grid cell size
	 */
	public double getCellSize(){
		return this.cellSize;
	}
	
	/**
	 * 
	 * @return the grid crs
	 */
	public CoordinateReferenceSystem getCoordinateReferenceSystem(){
		return this.crs;
	}
	
	/**
	 * 
	 * @return the grid result set x column
	 */
	public String getXColumn(){
		return this.xColumnName;
	}
	
	
	/**
	 * 
	 * @return the grid result set y column
	 */
	public String getYColumn(){
		return this.yColumnName;
	}
	
	
	/**
	 * 
	 * @return the grid result set value column
	 */
	public String getValueColumn(){
		return this.valueColumn;
	}
}

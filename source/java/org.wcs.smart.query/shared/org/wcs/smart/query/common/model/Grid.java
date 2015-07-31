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
package org.wcs.smart.query.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartContext;
import org.wcs.smart.query.model.IGridQueryColumnLabelProvider;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geomgraph.Edge;


/**
 * Grid definition class for gridded queries
 * @author egouge
 *
 */
public class Grid {

	public static final int MAX_GRID_CELLS = 1000*1000;
	public static final Exception GRID_TO_BIG_EXCEPTION = 
			new Exception(
					SmartContext.INSTANCE.getClass(IGridQueryColumnLabelProvider.class).getLabel(IGridQueryColumnLabelProvider.GRID_TO_BIG_KEY, Locale.getDefault()));
					
	
	private double EPSILON = 0.0000000001;
	
	private double originX;
	private double originY;
	private double cellSize;
	private CoordinateReferenceSystem crs;
	
	/**
	 * Creates a new grid definition
	 * @param originX x origin value
	 * @param originY y origin value
	 * @param cellSize cell size
	 * @param crs coordinate reference system of the grid
	 */
	public Grid(double originX, double originY, double cellSize, CoordinateReferenceSystem crs){
		this.originX = originX;
		this.originY = originY;
		this.cellSize = cellSize;
		this.crs = crs;
	}
	
	/**
	 * 
	 * @return the grid x origin
	 */
	public double getOriginX(){
		return this.originX;
	}
	
	/**
	 * 
	 * @return the grid y origin
	 */
	public double getOriginY(){
		return this.originY;
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
	 * @return the grid coordinate reference system
	 */
	public CoordinateReferenceSystem getCrs(){
		return this.crs;
	}
	
	/**
	 * Creates a set of vertical and horizontal
	 * edges that represent the grid.
	 * 
	 * @param txmin minimum x tile id
	 * @param tymin minimum y tile id
	 * @param width width of grid (number of cells)
	 * @param height height of grid (number of cells)
	 * @throws Exception if the grid size is too large
	 * @return
	 */
	public List<Edge> computeEdges(long txmin, long tymin, 
			long width, long height) throws Exception{
		
		if (width * height > MAX_GRID_CELLS){
			throw GRID_TO_BIG_EXCEPTION;
		}
		ArrayList<Edge> graphEdges = new ArrayList<Edge>();
		
		double ymin = ( tymin ) * cellSize + originY;
		double ymax = ( tymin + height ) * cellSize + originY;
		
		double xmin = ( txmin ) * cellSize + originX;
		double xmax = ( txmin + width ) * cellSize + originX;
		
		//vertical edges
		for (double i = 0; i <= width; i ++){
			double x = (i + txmin) * cellSize + originX;
			Coordinate c1 = new Coordinate(x, ymin);
			Coordinate c2 = new Coordinate(x, ymax);
			graphEdges.add(new Edge(new Coordinate[]{c1, c2}));
		}
		
		//horizontal edges
		for (double i = 0; i <= height; i ++){
			double y = (i + tymin) * cellSize + originY;
			Coordinate c1 = new Coordinate(xmin, y);
			Coordinate c2 = new Coordinate(xmax, y);
			graphEdges.add(new Edge(new Coordinate[]{c1, c2}));
		}
		
//		System.out.println("-- edges --");
//		System.out.println(txmin + ":" + tymin + ":" + width  + ":" + height);
//		for (Edge e : graphEdges){
//			System.out.println("LINESTRING(" + e.getCoordinate(0).x + " " + e.getCoordinate(0).y + "," + e.getCoordinate(1).x + " " + e.getCoordinate(1).y + ")");
//		}
//		System.out.println("-- edges --");
		return graphEdges;
	}
	
	
	/**
	 * Finds all tiles that touch the given point or
	 * contain the given point
	 * 
	 * @param x x coordinate the crs of grid
	 * @param y y coordinate in crs of grid
	 * 
	 * @return list of tiles that touch or contain the point
	 */
	public List<Tile> findTiles(double x, double y){
		
		int txmin = (int)Math.floor( (x - originX) / cellSize);
		int txmax = txmin;
		if (  Math.abs(( (x-originX) / cellSize) - txmin) < EPSILON ){
			txmin--;
		}
		
		int tymin = (int)Math.floor( (y - originY) / cellSize);
		int tymax = tymin;
		if (  Math.abs(( (y - originY) / cellSize) - tymin) < EPSILON ){
			tymin--;
		}
		
		List<Tile> ts = new ArrayList<Tile>();
		for (int ix = txmin; ix <= txmax; ix++){
			for (int iy = tymin; iy <= tymax; iy++){
				ts.add(new Tile(ix, iy));
			}
		}
		return ts;
	}
	
	/**
	 * Prints the grids for debugging purposes
	 * @param width
	 * @param height
	 */
	public void printGrid(int width, int height){
		for (double i = originX; i <= width*cellSize + originX; i += cellSize){
			System.out.println("LINESTRING(" + i + " " + originY + "," + i + " " + (originY + height * cellSize) + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
		
		for (double i = originY; i <= height * cellSize + originY; i += cellSize){
			System.out.println("LINESTRING(" + originX + " " + i + "," + (originX + width * cellSize) + " " + i + ")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$
		}
	}
}

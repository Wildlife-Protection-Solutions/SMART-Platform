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
package org.wcs.smart.query.common.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.wcs.smart.query.common.model.Grid;
import org.wcs.smart.query.common.model.Tile;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geomgraph.Edge;
import com.vividsolutions.jts.geomgraph.index.SegmentIntersector;
import com.vividsolutions.jts.geomgraph.index.SimpleMCSweepLineIntersector;

/**
 * Performs grid analysis on geometries
 * @author egouge
 *
 * @param <T> the values stored in the cells
 */
public class GridAnalysisEngine<T> {

	private Grid gridDef;
	private ICellMerger<T> cellMerger;
	private IValueComputer<T> valueCal;
	private HashMap<Tile, T> data;
	private MathTransform transform = null;
	/**
	 * Creates a new grid analysis engine
	 * @param gridDef the grid definition
	 * @param cellMerger how cell values computed for different linestrings are merged
	 * @param valueCal computes the cell value for a given linestring
	 * @throws FactoryException 
	 */
	public GridAnalysisEngine(Grid gridDef, ICellMerger<T> cellMerger,
			IValueComputer<T> valueCal) throws FactoryException {
		this.gridDef = gridDef;
		this.cellMerger = cellMerger;
		this.valueCal = valueCal;

		data = new HashMap<Tile, T>();
		
		//reproject ls to gridDef crs
		if (!CRS.equalsIgnoreMetadata(gridDef.getCrs(), GeometryUtils.SMART_CRS)){
			transform = CRS.findMathTransform(GeometryUtils.SMART_CRS, gridDef.getCrs());
		}
	}

	/**
	 * @return rasterized data by tile
	 */
	public HashMap<Tile, Double> getData() {
		HashMap<Tile, Double> results = new HashMap<Tile, Double>();
		for (Iterator<Entry<Tile, T>> iterator = data.entrySet().iterator(); iterator
				.hasNext();) {
			Entry<Tile, T> item = (Entry<Tile, T>) iterator.next();
			results.put(item.getKey(),
					cellMerger.getFinalValue(item.getValue()));

		}
		return results;
	}

	/**
	 * Rasterizes the given linestring merging
	 * the results to any previous rasterized linestrings
	 * using the provided cell merger.
	 * 
	 * @param ls rasterizes the linestring 
	 * @throws TransformException 
	 * @throws MismatchedDimensionException 
	 */
	/*
	 * The way rasterization is performed:
	 * 1. find the bbox of the linestring
	 * 2. create a set of grid lines that cover the bbox of the linestring
	 * 3. clean the linestring (remove duplicate coordinates);
	 * 4. find all intersection points between the linestring and the grid lines
	 * 5. find all tiles that touch all the intersection points
	 * 6. compute the value for those tiles
	 * 7. if there are no intersection points than the line lies wholly within
	 * a single cell -> find the cell these line within and process
	 * 
	 */
	public void rasterizeLinestring(LineString ls) throws Exception {
		LineString orig = ls;
		if (transform != null){
			ls = (LineString)JTS.transform(ls, transform);
		}
		if (ls == null || ls.getCoordinates() == null){
			throw new Exception("Error occurred rasterizing linestring. Linestring or coordinates is null after transform. (" + orig.toText() + ")", null); //$NON-NLS-1$ //$NON-NLS-2$
//			//something happened during the transform; we cannot do anything
//			QueryPlugIn.log("Error occurred rasterizing linestring. Linestring or coordinates is null after transform. (" + orig.toText() + ")", null); //$NON-NLS-1$ //$NON-NLS-2$
//			return;
		}
		
		//find ls envelope
		Envelope env = ls.getEnvelopeInternal();

		//create grid lines 
		long txmin = (long) Math.floor((env.getMinX() - gridDef.getOriginX())
				/ gridDef.getCellSize());
		long txmax = (long) Math.ceil((env.getMaxX() - gridDef.getOriginX())
				/ gridDef.getCellSize());

		long tymin = (long) Math.floor((env.getMinY() - gridDef.getOriginY())
				/ gridDef.getCellSize());
		long tymax = (long) Math.ceil((env.getMaxY() - gridDef.getOriginY())
				/ gridDef.getCellSize());

		List<Edge> edges = gridDef.computeEdges(txmin, tymin,
				txmax - txmin + 1, tymax - tymin + 1);


		//process linestring 
		//if there are duplicate points they cause problems for
		//intersector so we will remove them

		List<Coordinate> datapnts = new ArrayList<Coordinate>();
		// remove duplicates
		Coordinate cdata[] = ls.getCoordinates();
		datapnts.add(cdata[0]);
		for (int i = 1; i < cdata.length; i++) {
			if (cdata[i - 1].x != cdata[i].x || cdata[i - 1].y != cdata[i].y) {
				datapnts.add(cdata[i]);
			}
		}
		HashMap<Tile, T> ldata = null;	//rasterized data
		
		GriddedLineIntersector<T> intersector = new GriddedLineIntersector<T>(valueCal, gridDef, ls);
		if (datapnts.size() > 1){
			//compute intersection points and process tiles
			List<Edge> lsEdges = new ArrayList<Edge>();
			lsEdges.add(new Edge(datapnts.toArray(new Coordinate[datapnts.size()])));
			
			//intersector computer
			SimpleMCSweepLineIntersector processor = new SimpleMCSweepLineIntersector();			
			SegmentIntersector si = new SegmentIntersector(intersector, false, false);
			processor.computeIntersections(edges, lsEdges, si);
			if (intersector.getException() != null){
				throw intersector.getException();
			}
			ldata = intersector.getData();
		}
		
		if (datapnts.size() == 0 || intersector.getIntersectionCount() == 0){
			//here either the linestring consisted of one point
			//or there were no intersections with grid so linestring must
			//be wholly contained within a single grid cell.
			Coordinate c = datapnts.get(0);
			List<Tile> tiles = gridDef.findTiles(c.x, c.y);
			ldata = new HashMap<Tile, T>();
			for (Tile t : tiles) {
				ldata.put(t, valueCal.computeValue(null, t, gridDef, ls));
			}
		} 

		// merge results of the linestring processing
		//with existing results
		for (Iterator<Entry<Tile, T>> iterator = ldata.entrySet().iterator(); iterator
				.hasNext();) {
			Entry<Tile, T> mydata = (Entry<Tile, T>) iterator.next();
			T x = data.get(mydata.getKey());
			data.put(mydata.getKey(),cellMerger.mergeCell(x, mydata.getValue()));
			if (data.size() > Grid.MAX_GRID_CELLS){
				throw Grid.GRID_TO_BIG_EXCEPTION;
			}
		}
	}
}

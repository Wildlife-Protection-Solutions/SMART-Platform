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
package org.wcs.smart.report.birt.map.execute.raster;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.data.engine.api.IQueryResults;
import org.eclipse.birt.data.engine.api.IResultIterator;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.map.raster.GridMetadata;
import org.wcs.smart.map.raster.GridResultItem;
import org.wcs.smart.map.raster.RasterBuilder;

/**
 * Builds raster file from BIRT query results
 *  
 * @author Emily
 *
 */
public class BirtRasterBuilder {

	private File rasterTempFile;
	private List<File> generatedFiles;
	
	private CoordinateReferenceSystem crs;
	private double originX;
	private double originY;
	private double cellSize;
	
	private String xColumn;
	private String yColumn;
	private String valueColumn;
	
	double minValue = Double.POSITIVE_INFINITY;
	double maxValue = Double.NEGATIVE_INFINITY;
	
	public BirtRasterBuilder(CoordinateReferenceSystem crs, Point origin, double size,
			String xColumn, String yColumn, String valueColumn){
		this.crs = crs;
		this.originX = origin.getX();
		this.originY = origin.getY();
		this.cellSize = size;
		this.xColumn = xColumn;
		this.yColumn = yColumn;
		this.valueColumn = valueColumn;
		
	}
	
	/**
	 * 
	 * @return the raster image file
	 */
	public File getFileImage(){
		return this.rasterTempFile;
	}
	
	/**
	 * List of all files (include the raster image file) that support
	 * the raster layer
	 * @return
	 */
	public List<File> getAllFiles(){
		return this.generatedFiles;
	}
	
	/**
	 * 
	 * @return The minimum value in the raster.
	 */
	public double getMinValue(){
		return this.minValue;
	}
	
	/**
	 * 
	 * @return The maximum value in the raster.
	 */
	public double getMaxValue(){
		return this.maxValue;
	}
	
	/**
	 * Builds the raster
	 * @param results
	 * @throws Exception
	 */
	public void buildRaster(IQueryResults results) throws Exception{
		generatedFiles = new ArrayList<File>();
		
		
		IResultIterator data = results.getPreparedQuery().execute(null).getResultIterator();
	
		List<GridResultItem> gridresults = new ArrayList<GridResultItem>();
		long minX = Integer.MAX_VALUE;
		long maxX = Integer.MIN_VALUE;
		long minY = Integer.MAX_VALUE;
		long maxY = Integer.MIN_VALUE;

		while(data.next()){
			GridResultItem i = new GridResultItem();
			
			i.setTileX(data.getInteger(xColumn));
			i.setTileY(data.getInteger(yColumn));
			i.setValue(data.getDouble(valueColumn));
			
			if (i.getTileX() < minX) minX = i.getTileX();
			if (i.getTileX() > maxX) maxX = i.getTileX();
			if (i.getTileY() < minY) minY = i.getTileY();
			if (i.getTileY() > maxY) maxY = i.getTileY();
			if (i.getValue() < minValue) minValue = i.getValue();
			if (i.getValue() > maxValue) maxValue = i.getValue();
			
			gridresults.add(i);
		}
		if (gridresults.isEmpty()) return;
		
		GridMetadata md = new GridMetadata(minValue, maxValue, minX, maxX, minY, maxY);
		
		long rasterHeight = maxY - minY + 1;
		long rasterWidth = maxX - minX + 1;
				
		Envelope2D env = new Envelope2D(
				crs, 
				(minX-1)* cellSize + 0.5* cellSize + originX, 
				(minY-1) * cellSize - 0.5*cellSize + originY, 
				rasterWidth * cellSize , rasterHeight * cellSize);
		
		rasterTempFile = File.createTempFile("smart" + System.nanoTime(), ".tiff"); //$NON-NLS-1$ //$NON-NLS-2$
		RasterBuilder builder = new RasterBuilder();
		builder.setEnvelope(env);
		builder.setFileName(rasterTempFile.getAbsolutePath());
		builder.setGridCellSize(cellSize);
		builder.setRasterDimensions((int)rasterWidth, (int)rasterHeight);
		builder.setTable(gridresults, md);
		
		builder.build();
		
		generatedFiles.addAll(builder.getAllFiles());
	}
	
	
}

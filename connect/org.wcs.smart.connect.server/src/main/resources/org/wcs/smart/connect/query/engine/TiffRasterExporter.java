/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine;

import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Response.Status;

import org.geotools.geometry.Envelope2D;
import org.hibernate.Session;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.query.common.model.GridQueryResultMetadata;
import org.wcs.smart.query.common.model.GriddedQuery;

/**
 * Exporter for exporting grid queries to raster file. 
 * 
 * @author Emily
 *
 */
public class TiffRasterExporter {

	public static final String FORMAT_KEY = "tif"; //$NON-NLS-1$
	
	public static final int MAX_GRID_CELLS = 1000*1000;
	
	public static final String getName(Locale l){
		return Messages.getString("TiffRasterExporter.TiffFormat", l);  //$NON-NLS-1$
	}
	
	private final Logger logger = Logger.getLogger(TiffRasterExporter.class.getName());
	
	private Path rasterFile;
	private Locale l;
	
	public TiffRasterExporter(Path rasterFile, Locale l){
		this.rasterFile = rasterFile;
		this.l = l;

	}
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 */
	public void exportResults(GriddedQuery query, GridQueryResults results, Session session){
		try{
			RasterBuilder rb = new RasterBuilder();
			rb.setFileName(rasterFile);
			
			GridQueryResultMetadata pnts = results.getTileBounds();
			
			long width = pnts.getMaxXTile() - pnts.getMinXTile() + 1;
			long height = pnts.getMaxYTile() - pnts.getMinYTile()+ 1;
			
			if (width > Integer.MAX_VALUE 
					|| height > Integer.MAX_VALUE 
					|| width * height > MAX_GRID_CELLS){
				throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("TiffRasterExporter.GridTooBigError", l)); //$NON-NLS-1$
			}
			rb.setRasterDimensions((int)width, (int)height);
				
			// sets the envelope based in the map bound
			double gridCellSize = query.getGridSize();
			rb.setEnvelope(
					new Envelope2D(
						query.getCoordinateReferenceSystem(), 
						(pnts.getMinXTile()-1)* gridCellSize + 0.5* gridCellSize + query.getGridOrigin().x, 
						(pnts.getMinYTile()-1) * gridCellSize - 0.5*gridCellSize + query.getGridOrigin().y, 
						width * gridCellSize , height*gridCellSize)); 
			
			rb.setTable(results,pnts); 
			rb.build();
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Error writing grid results to tif file", ex); //$NON-NLS-1$
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, Messages.getString("TiffRasterExporter.WriteError", l)); //$NON-NLS-1$
		}
	}
}

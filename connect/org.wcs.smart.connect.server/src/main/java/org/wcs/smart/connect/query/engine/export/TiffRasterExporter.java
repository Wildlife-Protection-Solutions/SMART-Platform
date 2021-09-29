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
package org.wcs.smart.connect.query.engine.export;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response.Status;

import org.geotools.geometry.Envelope2D;
import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.query.engine.GridQueryResults;
import org.wcs.smart.map.raster.GridMetadata;
import org.wcs.smart.map.raster.RasterBuilder;
import org.wcs.smart.query.common.model.GriddedQuery;

/**
 * Exporter for exporting grid queries to raster file. 
 * 
 * @author Emily
 *
 */
public class TiffRasterExporter extends AbstractQueryExporter{

	public static final String FORMAT_KEY = "tif"; //$NON-NLS-1$
	
	public static final int MAX_GRID_CELLS = 1000*1000;
	
	public static final String getName(Locale l){
		return Messages.getString("TiffRasterExporter.TiffFormat", l);  //$NON-NLS-1$
	}
	
	private final Logger logger = Logger.getLogger(TiffRasterExporter.class.getName());
	
	private String filename;
	private GriddedQuery query;
	private GridQueryResults results;
	
	protected TiffRasterExporter(IProjectionProvider prjProvider, Locale l, ServletContext context){
		super(prjProvider, l, context);
	}
	
	public TiffRasterExporter(GriddedQuery query, GridQueryResults results, IProjectionProvider prjProvider, Locale l, ServletContext context){
		this(prjProvider, l, context);
		this.query = query;
		this.results = results;
		filename = SmartUtils.cleanFileName(query.getName() + "_"+ query.getId()) + ".tiff"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	public String getFileName() {
		return filename;
	}
	
	@Override
	public void write(OutputStream output) throws IOException, WebApplicationException {
		Path tifFile = null;
		try(Session session = HibernateManager.openNewSession(context, locale)){
			session.beginTransaction();
			try {
				tifFile = createTiff(session);
			}finally {
				session.getTransaction().commit();
			}
		}finally {
			try(Session session = HibernateManager.openNewSession(context, locale)){
				session.beginTransaction();
				try {
					dispose(results, session);
					session.getTransaction().commit();
				}catch (Exception ex) {
					if (session.getTransaction().isActive()) session.getTransaction().rollback();
					logger.log(Level.SEVERE, ex.getMessage(),ex);
					throw new IOException(ex);
				}
			}
		}
		try {
			Files.copy(tifFile, output);
		}finally {
			try {
				Files.delete(tifFile);
			}catch (IOException ex) {
				logger.log(Level.WARNING, ex.getMessage(), ex);
			}
		}
	}
	
	/**
	 * Exports simple queries whose results and represented by a database table.
	 * 
	 * @param query
	 * @param results
	 * @param session
	 */
	public Path createTiff(Session session){
		try{
			java.nio.file.Path rasterFile  = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(filename); 
			
			if ( results.getData().size() == 0){
				//create an empty file
				Files.createFile(rasterFile);
				return rasterFile;
			}
			
			RasterBuilder rb = new RasterBuilder();
			rb.setFileName(rasterFile.toString());
			
			GridMetadata pnts = results.getTileBounds();
			
			long width = pnts.getMaxXTile() - pnts.getMinXTile() + 1;
			long height = pnts.getMaxYTile() - pnts.getMinYTile()+ 1;
			
			if (width > Integer.MAX_VALUE 
					|| height > Integer.MAX_VALUE 
					|| width * height > MAX_GRID_CELLS){
				throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("TiffRasterExporter.GridTooBigError", locale)); //$NON-NLS-1$
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
			
			rb.setTable(results.getData(), pnts);
			rb.build();
			
			return rasterFile;
		}catch (Exception ex){
			logger.log(Level.SEVERE, "Error writing grid results to tif file", ex); //$NON-NLS-1$
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, Messages.getString("TiffRasterExporter.WriteError", locale)); //$NON-NLS-1$
		}
	}

}

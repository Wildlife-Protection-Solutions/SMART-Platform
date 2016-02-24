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
package org.wcs.smart.connect.query.engine;

import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Iterator;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.connect.query.engine.GridQueryResults.GridMetadata;
import org.wcs.smart.query.common.model.GridResultItem;
import org.wcs.smart.util.SharedUtils;


/**
 * Builds a raster file for one band.
 * 
 * <pre>
 * Coordinates in pixel values
 * (0,0) ----------- >(0,Max X)
 *   |
 *   |
 *   |
 *   |
 *   V
 * (0,Max Y) 
 * </pre>
 * 
 * 
 * 
 * @author Mauricio Pazos
 * @author Emily
 *
 */
final class RasterBuilder {

	private static final int BAND_0 = 0;
	
	private static final float NO_DATA = -9999;	//no data value
	
	/** a table where x,y values are the position in the raster grid. (0,0) is the bottom left tile and (360 180) is the top right tile */
	private GridQueryResults table; 
	private GridMetadata metadata;
	
	private Path file;
	private Envelope2D envelope = null;

	/** raster dimensions (pixel values) */
	private int width;
	private int height;

	/** grid cell size */
	private double gridCellSize;
	
	/**
	 * Sets the file name to write the raster to
	 * @param fileName
	 */
	public void setFileName(Path file) {
		this.file = file;
	}
	
	/**
	 * 
	 * @param width 
	 * @param height 
	 */
	public void setRasterDimensions( final int width, final int height){
		assert width > 0 ;
		assert height > 0;
		
		this.width = width;
		this.height = height;
	}

	/**
	 * The envelop for the raster to generate.
	 * 
	 * @param envelop an envelope sets as EPSG:4326 is expected
	 */
	public void setEnvelope(Envelope2D envelop){
		this.envelope = envelop;
	}
	
	/**
	 * Sets the table used to build the raster
	 * 
	 * @param table query result data
	 * @param metadata query result metadata
	 */
	public void setTable(GridQueryResults data, GridMetadata metadata ) {
		this.table = data;
		this.metadata = metadata;
	}
	
	/**
	 * Sets the grid cell size.
	 * 
	 * @param cellSize
	 */
	public void setGridCellSize(double cellSize){
		this.gridCellSize = cellSize;
	}

	/**
	 * Creates the raster file using the values present in the table.
	 * @throws Exception 
	 * 
	 * @throws NumberFormatException 
	 */
	public void build() throws Exception {
		
		assert this.table != null;
		assert this.envelope != null;

		TiledImage raster = null;
		try {
			raster = createRaster();

			RenderedOp op = JAI.create("filestore",raster,file.toFile().getCanonicalPath(),"TIFF"); //$NON-NLS-1$ //$NON-NLS-2$
			op.dispose();

			String baseFile = file.toFile().getCanonicalPath().substring(0,  file.toFile().getCanonicalPath().lastIndexOf('.'));
			createProjectionFile(baseFile, envelope.getCoordinateReferenceSystem());
			createWorldFile(baseFile, gridCellSize, envelope.getMinX(), envelope.getMaxY());
			
			
		}catch (Exception ex){
			throw ex;
		} finally {
			if (raster != null){
				raster.dispose();
			}
		}
	}

	/**
	 * Writes required world file
	 * @param baseFile
	 * @param gridSize
	 * @param xmin
	 * @param ymax
	 * @throws IOException
	 */
	private void createWorldFile(final String baseFile,
			double gridSize, double xmin, double ymax) throws IOException {
		final File prjFile = new File(new StringBuffer(baseFile).append(".tfw") //$NON-NLS-1$
				.toString());
		try(BufferedWriter out = new BufferedWriter(new FileWriter(prjFile))){
			out.write(String.valueOf(gridSize));
			out.write(SharedUtils.LINE_SEPARATOR);
			out.write("0"); //$NON-NLS-1$
			out.write(SharedUtils.LINE_SEPARATOR);
			out.write("0"); //$NON-NLS-1$
			out.write(SharedUtils.LINE_SEPARATOR);
			out.write(String.valueOf(-gridSize));
			out.write(SharedUtils.LINE_SEPARATOR);
			out.write(String.valueOf(xmin));
			out.write(SharedUtils.LINE_SEPARATOR);
			out.write(String.valueOf(ymax));
			out.write(SharedUtils.LINE_SEPARATOR);
		}

	}
	
	/**
	 * Writes projection file
	 * @param baseFile
	 * @param coordinateReferenceSystem
	 * @throws IOException
	 */
	private void createProjectionFile(final String baseFile,
			final CoordinateReferenceSystem coordinateReferenceSystem)
	
			throws IOException {
		final File prjFile = new File(new StringBuffer(baseFile).append(".prj") //$NON-NLS-1$
				.toString());
		try(BufferedWriter out = new BufferedWriter(new FileWriter(prjFile))){
			out.write(coordinateReferenceSystem.toWKT());
		}
	}

	/**
	 * Creates a raster based on the list of values for band 0 
	 * @return {@link WritableRaster}
	 * @throws Exception 
	 */
	private TiledImage createRaster() throws Exception {
		SampleModel sampleModel = RasterFactory.createBandedSampleModel(
				DataBuffer.TYPE_FLOAT, this.width, this.height, 1);
		ColorModel colorModel = PlanarImage.createColorModel(sampleModel);
		WritableRaster raster = RasterFactory.createWritableRaster(sampleModel,
				new Point(0, 0));
		
		//initialize data to no data
		for (int x = 0; x < raster.getWidth(); x++) {
			for (int y = 0; y < raster.getHeight(); y++) {
				raster.setSample(x, y, BAND_0, NO_DATA);
			}
		}
		
		//add data points
		for (Iterator<GridResultItem> iterator = table.getIterator(); iterator.hasNext();) {
			GridResultItem item = (GridResultItem) iterator.next();
		
			// computes the raster x,y coord based on the top left bounds' coordenates (MinX, MaxY)
			if (item.getTileX() >= metadata.xmin && item.getTileX() <= metadata.xmax 
					&& item.getTileY() >= metadata.ymin
					&& item.getTileY() <= metadata.ymax){
				int x = (int)(item.getTileX() - metadata.xmin);
				int y = (int)(height - (item.getTileY() - metadata.ymin +1));
				raster.setSample(x, y,BAND_0, item.getValue());
			}
		}			
		
		TiledImage tiledImage = new TiledImage(0, 0, width, height, 0, 0,
				sampleModel, colorModel);
		tiledImage.setData(raster);
		return tiledImage;

	}
}

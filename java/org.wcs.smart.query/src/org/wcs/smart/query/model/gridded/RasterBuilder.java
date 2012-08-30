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
package org.wcs.smart.query.model.gridded;


import java.awt.Point;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.image.WorldImageFormat;
import org.geotools.gce.image.WorldImageWriter;
import org.geotools.geometry.Envelope2D;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.util.SmartUtils;


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
 * <pre>
 * Usage
 * TODO 
 * </pre>
 * 
 * 
 * @author Mauricio Pazos
 *
 */
final class RasterBuilder {

	private static final int BAND_0 = 0;
	private static final float NO_DATA = -9999;	//no data value
	
	/** a table where x,y values are the position in the raster grid. (0,0) is the bottom left tile and (360 180) is the top right tile */
	private List<Map<String, Object>> table; 
	
	private File file;
	private String fileName;
	private Envelope2D envelope = null;

	/** raster dimensions (pixel values) */
	private int width;
	private int height;

	public void setFileName(String fileName) {
		
		this.fileName = fileName;
	}
	
	/**
	 * 
	 * @param width a value between 0 and 360
	 * @param height a value between 0 and 180
	 */
	public void setRasterDimensions( final int width, final int height){
		
		assert width >= 0 && width <= 360;
		assert width >= 0 && width <= 180;
	
		this.width = width;
		this.height = height;
	}

	/**
	 * The envelop for the raster to generate.
	 * 
	 * @param envelop an envelope sets as EPSG:4326 is expected
	 */
	public void setEnvelope(Envelope2D envelop){
		
		assert envelop.getCoordinateReferenceSystem().getName().equals("EPSG:4326");
		
		this.envelope = envelop;
	}
	
	/**
	 * Sets the table used to build the raster
	 * 
	 * @param table List of < x, y, value >
	 */
	public void setTable(List<Map<String, Object>> table) {

		this.table = table;
	}

	/**
	 * Creates the raster file using the values present in the table.
	 * @throws Exception 
	 * 
	 * @throws NumberFormatException 
	 */
	public void build(double gridSize) throws RasterServiceException {
		
		assert this.table != null;
		assert this.envelope != null;

//		GridCoverage2D gc = null;
		try {
			TiledImage raster = createRaster();

			File out = new File(fileName);
			JAI.create("filestore",raster,out.getCanonicalPath(),"TIFF");

			String baseFile = out.getCanonicalPath().substring(0,  out.getCanonicalPath().lastIndexOf('.'));
			createProjectionFile(baseFile, envelope.getCoordinateReferenceSystem());
			createWorldFile(baseFile, gridSize, envelope.getMinX(), envelope.getMaxY());
			this.file = out;
//
//			this.file = saveInFile(gc, this.fileName);
//			
		}catch (Exception ex){
			//TODO:
			ex.printStackTrace();
		} finally {
			
//			if(gc != null) gc.dispose(true);
		}
	}
	
	private void createWorldFile(final String baseFile,
			double gridSize, double xmin, double ymax) throws IOException {
		final File prjFile = new File(new StringBuffer(baseFile).append(".tfw")
				.toString());
		BufferedWriter out = new BufferedWriter(new FileWriter(prjFile));
		out.write(String.valueOf(gridSize));
		out.write(SmartUtils.LINE_SEPARATOR);
		out.write("0");
		out.write(SmartUtils.LINE_SEPARATOR);
		out.write("0");
		out.write(SmartUtils.LINE_SEPARATOR);
		out.write(String.valueOf(-gridSize));
		out.write(SmartUtils.LINE_SEPARATOR);
		out.write(String.valueOf(xmin));
		out.write(SmartUtils.LINE_SEPARATOR);
		out.write(String.valueOf(ymax));
		out.write(SmartUtils.LINE_SEPARATOR);
		out.close();

	}
	
	private void createProjectionFile(final String baseFile,
			final CoordinateReferenceSystem coordinateReferenceSystem)
	
			throws IOException {
		final File prjFile = new File(new StringBuffer(baseFile).append(".prj")
				.toString());
		BufferedWriter out = new BufferedWriter(new FileWriter(prjFile));
		out.write(coordinateReferenceSystem.toWKT());
		out.close();

	}


	/**
	 * Saves the coverate in the file
	 * 
	 * @param gc
	 * @param fileName
	 * @return {@link File}
	 * 
	 * @throws RasterServiceException
	 */
	private File saveInFile(final GridCoverage2D gc, final String fileName) throws RasterServiceException{

		WorldImageWriter w = null;
		File rasterFile = new File(fileName);
		try {
		    w = new WorldImageWriter(rasterFile);
			Format format = w.getFormat();
			final ParameterValueGroup params = format.getWriteParameters();
			params.parameter(WorldImageFormat.FORMAT.getName().toString()).setValue("tiff");
			final GeneralParameterValue[] gpv = { params.parameter(WorldImageFormat.FORMAT.getName().toString()) };
			// writing
			w.write(gc,gpv);
	        
		} catch (Exception e) {
    		final String message = "Fail saving raster "+ fileName +". " +e.getMessage();
			throw new RasterServiceException(message, e);
		} finally {
			if(w != null) w.dispose();
		}		
		
		return rasterFile;
		
	}

	/**
	 * Creates a raster based on the list of values for band 0 
	 * @return {@link WritableRaster}
	 * @throws Exception 
	 */
	private TiledImage createRaster() throws RasterServiceException{

		try{
			SampleModel sampleModel = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_FLOAT, this.width, this.height, 1);
			ColorModel colorModel = PlanarImage.createColorModel(sampleModel);			
	        WritableRaster raster = RasterFactory.createWritableRaster(sampleModel, new Point(0,0));
	        for (int x = 0; x < raster.getWidth(); x ++){
	        	for (int y = 0; y < raster.getHeight(); y ++){
	        		raster.setSample(x, y, BAND_0, NO_DATA);
	        	}
	        }
	        for (Map<String, Object> row: this.table) {
	        	int x =(Integer) row.get("x");
	        	int y = (Integer) row.get("y");
	        	double value = Double.parseDouble(row.get("value").toString());
				assert value >= 0;
	        	raster.setSample(x, y, BAND_0, value); 
			}
	        
	        TiledImage tiledImage = new TiledImage(0,0,width,height,0,0,sampleModel,colorModel);
	        tiledImage.setData(raster);
	        
	        
//	        JAI.create("filestore",tiledImage,new File(fileName),"TIFF");
	        
	        return tiledImage;
			
		} catch (Exception e){
			String message = "the raster could not be created. " + e.getMessage();
			throw  new RasterServiceException(message, e);
		}
	}

	public File getResult() {

		assert this.file != null;
		
		
		return this.file;
	}
	}

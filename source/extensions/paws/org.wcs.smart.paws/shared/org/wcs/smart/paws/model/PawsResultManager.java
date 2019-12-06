/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.model;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.Envelope2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import it.geosolutions.jaiext.range.NoDataContainer;

/**
 * Tools for managing PAWS results
 * 
 * @author Emily
 *
 */
public class PawsResultManager {

	public static final String RUN_DIR = "run";
	public static final String PAWS_DIR = "paws";
	
	private static int NO_DATA = -9999;
	
	private PawsRun run;
	private Integer numRows = null;
	private String headers[] = null;
	private Path resultsFile = null;
	
	public PawsResultManager(PawsRun run) {
		this.run = run;
		resultsFile = getRunDirectory(run);
	}
	
	public static Path getRunDirectory(PawsRun run) {
		 return Paths.get(run.getConservationArea().getFileDataStoreLocation())
			.resolve(PAWS_DIR)
			.resolve(RUN_DIR)
			.resolve(UuidUtils.uuidToString(run.getUuid()));
	}
	public Path getResultsFile() {
		return this.resultsFile;
	}

	public PawsRun getRun() {
		return this.run;
	}
	
	/**
	 * populate the data array with size elements starting at start index
	 * @param startIndex
	 * @param size
	 * @param data
	 * @return
	 * @throws Exception
	 */
	public void getData(int startIndex, int size, List<String[]> data) throws Exception{		
		try(BufferedReader breader = Files.newBufferedReader(resultsFile)){
			try(CSVReader reader = new CSVReader(breader, CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, startIndex)){
				int cnt = 0;
				while(cnt < size) {
					cnt++;
					String[] item = reader.readNext();
					if (item != null) {
						data.add(item);
					}else {
						break;
					}
					
				}
			}
		}
	}
	
	
	public synchronized String[] getHeaders() throws Exception{
		if (headers == null) loadSummary();
		return headers;
	}
	
	public synchronized int getNumRows() throws Exception{
		if (numRows == null) loadSummary();
		return numRows;
	}
	
	private synchronized void loadSummary() throws Exception {
		try(BufferedReader breader = Files.newBufferedReader(resultsFile)){
			try(CSVReader reader = new CSVReader(breader)){
				int size = 0;
				headers = reader.readNext();
				while(reader.readNext() != null) size++;
				
				this.numRows = size;
			}
		}
	}
	
	public List<Path> getRasterFiles() throws Exception{
		List<Path> items = new ArrayList<>();
		for (String x : getHeaders()) {
			if (!x.startsWith("threshold")) continue;
			String fname = x.replaceAll("=","_").replaceAll("\\.", "_");
			items.add(getRunDirectory(run).resolve(fname + ".tif"));
		}
		return items;
	}
	
	public void createOutput(Path resultLayer, CoordinateReferenceSystem crs) throws Exception{
		if (Files.exists(resultLayer)) return; //created
		
		if (crs == null) throw new Exception("Projection of results could not be determined");

		//create a raster files out of this layer
		String headername = resultLayer.getFileName().toString();
		headername = SharedUtils.getFilenameWithoutExtension(headername);
		String columnname = headername.replaceAll("threshold_", "threshold=").replaceAll("_", ".");
		
		String[] headers = getHeaders();
		int index = -1;
		int xindex = -1;
		int yindex = -1;
		for (int i = 0; i < headers.length; i ++ ) {
			if(headers[i].equalsIgnoreCase(columnname)) {
				index = i;
			}else if (headers[i].equalsIgnoreCase("x")) {
				xindex = i;
			}else if (headers[i].equalsIgnoreCase("y")) {
				yindex = i;
			}
		}
		if (index < 0) throw new Exception(MessageFormat.format("No results layer with name {0} found", columnname));
		if (xindex < 0) throw new Exception("X coordinate column not found");
		if (yindex < 0) throw new Exception("Y coordinate column not found");
		
		double minx = Double.MAX_VALUE;
		double maxx = Double.MIN_NORMAL;
		double miny = Double.MAX_VALUE;
		double maxy = Double.MIN_NORMAL;
		double gridSize = Double.valueOf( run.getConfiguration().findParameter(PawsParameter.FixedParameter.GRID_SIZE.name()).getValue() );
				
		//detemine min/max values
		try(BufferedReader breader = Files.newBufferedReader(resultsFile)){
			try(CSVReader reader = new CSVReader(breader)){
				reader.readNext();	//skip header
				
				String[] data;
				while ((data = reader.readNext()) != null) {
					double x = Double.valueOf(data[xindex]);
					double y = Double.valueOf(data[yindex]);
					
					minx = Double.min(minx,  x);
					maxx = Double.max(maxx,  x);
					miny = Double.min(miny,  y);
					maxy = Double.max(maxy,  y);
				}
				
			}
		}
		
		//create raster
		int width = (int)((maxx - minx) / gridSize) + 1;
		int height = (int)((maxy - miny) / gridSize) + 1;
		SampleModel sampleModel = new ComponentSampleModel(DataBuffer.TYPE_FLOAT, width, height, 1, width, new int[] {0});
		
		WritableRaster outfile = Raster.createWritableRaster(sampleModel, null);

		for (int i = 0; i < width; i ++) {
			for (int j = 0; j < height; j ++) {
				outfile.setPixel(i, j, new double[] {NO_DATA});
			}
		}
		
		try(BufferedReader breader = Files.newBufferedReader(resultsFile)){
			try(CSVReader reader = new CSVReader(breader)){
				reader.readNext();	//skip header
				String[] data;
				while ((data = reader.readNext()) != null) {
					double x = Double.valueOf(data[xindex]);
					double y = Double.valueOf(data[yindex]);
					double value = Double.valueOf(data[index]);
					
					int xi = (int)((x - minx) / gridSize);
					int yi = (int)((maxy - y) / gridSize);
					outfile.setPixel(xi, yi, new double[] {value});
				}
			}
		}
		
		
		//write raster
		minx = minx - (gridSize / 2.0);
		miny = miny - (gridSize / 2.0);
		

		ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
		ColorModel cm = new ComponentColorModel(cs, false, true, Transparency.OPAQUE, DataBuffer.TYPE_FLOAT);
		final RenderedImage image = new BufferedImage(cm, outfile, false, null);
		Envelope2D env = new Envelope2D(crs, minx, miny, width * gridSize, height * gridSize);
	    
	    NoDataContainer nodata = new NoDataContainer(NO_DATA);
	    HashMap<Object, Object> props = new HashMap<>();
	    props.put(NoDataContainer.GC_NODATA, nodata);
	    GridCoverage2D gc = (new GridCoverageFactory()).create(headername, image, env, null, null, props);
		
		GeoTiffWriter writer = new GeoTiffWriter(resultLayer.toFile());
		writer.write(gc, null);

	}

}

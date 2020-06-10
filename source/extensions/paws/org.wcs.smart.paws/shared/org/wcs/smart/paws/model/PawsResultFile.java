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

import au.com.bytecode.opencsv.CSVParser;
import au.com.bytecode.opencsv.CSVReader;
import it.geosolutions.jaiext.range.NoDataContainer;


/**
 * This represents a single csv file in the multiple files provided 
 * by a PAWS Run.  This csv file has multiple columns of data.
 * 
 * @author Emily
 *
 */
public class PawsResultFile {
	private static int NO_DATA = -9999;
	
	private PawsRun run;
	
	private Integer numRows = null;
	private String headers[] = null;
	
	private Path resultsFile = null;
	private Path imageDir = null;
	
	private List<Path> rasterFiles = null;
	
	public PawsResultFile(PawsRun run, Path file) {
		this.run = run;
		this.resultsFile = file;
		
		String rootName = resultsFile.getFileName().toString();
		//remove any extensions and create a folder
		rootName = SharedUtils.getFilenameWithoutExtension(rootName);
		
		imageDir = resultsFile.getParent().resolve(rootName);
		if (!Files.exists(imageDir)) {
			try {
				Files.createDirectories(imageDir);
			}catch (Exception ex) {
				ex.printStackTrace();
			}
		}
		
	}
	
	public Path getResultsFile() {
		return this.resultsFile;
	}

	public String getTimeFrameString() {
		String fname = this.resultsFile.getFileName().toString();
		if (fname.length() == "predictions_XXXX.csv".length()) {
			//year only
			fname = fname.substring("predictions_".length(), fname.length() - ".csv".length()); //$NON-NLS-1$ //$NON-NLS-2$ 
		}else {
			fname = fname.substring("predictions_".length(), fname.length() - "_XXXX.csv".length()); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return fname;
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
	
	/**
	 * One raster file per output data column
	 * @return list of raster files ordered by threshold value; lower value first
	 * @throws Exception
	 */
	public List<Path> getRasterFiles() throws Exception{
		if (rasterFiles != null) return rasterFiles;
		
		List<Path> items = new ArrayList<>();	
		HashMap<Path, Double> threshold = new HashMap<>();
		
		for (String x : getHeaders()) {
			if (!x.startsWith("threshold")) continue; //$NON-NLS-1$
			
			double t = Double.parseDouble( x.substring(x.indexOf('=') + 1) );
			String fname = x.replaceAll("=","_").replaceAll("\\.", "_"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
			Path p = imageDir.resolve(fname + ".tif");  //$NON-NLS-1$
			
			items.add(p);
			threshold.put(p, t);
		}
		
		rasterFiles = items;
		rasterFiles.sort((a,b)->threshold.get(a).compareTo(threshold.get(b)));
		
		return rasterFiles;
	}
	
	public void createOutputImages(CoordinateReferenceSystem crs) throws Exception{
		for (Path p : getRasterFiles()) createOutputImage(p, crs);
	}
	private void createOutputImage(Path resultFile, CoordinateReferenceSystem crs) throws Exception{
		if (Files.exists(resultFile)) return; //created
		if (crs == null) throw new Exception("Projection of results could not be determined"); //$NON-NLS-1$

		//create a raster files out of this layer
		String headername = resultFile.getFileName().toString();
		headername = SharedUtils.getFilenameWithoutExtension(headername);
		String columnname = headername.replaceAll("threshold_", "threshold=").replaceAll("_", "."); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
		
		String[] headers = getHeaders();
		int index = -1;
		int xindex = -1;
		int yindex = -1;
		for (int i = 0; i < headers.length; i ++ ) {
			if(headers[i].equalsIgnoreCase(columnname)) {
				index = i;
			}else if (headers[i].equalsIgnoreCase("x")) { //$NON-NLS-1$
				xindex = i;
			}else if (headers[i].equalsIgnoreCase("y")) { //$NON-NLS-1$
				yindex = i;
			}
		}
		if (index < 0) throw new Exception(MessageFormat.format("No results layer with name {0} found", columnname)); //$NON-NLS-1$
		if (xindex < 0) throw new Exception("X coordinate column not found"); //$NON-NLS-1$
		if (yindex < 0) throw new Exception("Y coordinate column not found"); //$NON-NLS-1$
		
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
					//if (data[xindex].trim().isEmpty()) continue;
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
					if (data[xindex].trim().isEmpty()) continue;
					if (data[index].trim().isEmpty()) continue;
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
		
		GeoTiffWriter writer = new GeoTiffWriter(resultFile.toFile());
		writer.write(gc, null);

	}
}

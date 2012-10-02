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
package org.wcs.smart.query.export;

import java.io.File;

import org.eclipse.core.runtime.IProgressMonitor;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.opengis.coverage.grid.GridCoverage;
import org.wcs.smart.query.model.GriddedQuery;
import org.wcs.smart.query.model.Query;

/**
 * Export gridded query results to arc ascii image
 * @author Emily
 *
 */
public class GridArcImageExporter implements IQueryExporter {

	public GridArcImageExporter() {
	}

	@Override
	public String getId() {
		return "org.wcs.smart.query.export.GridArcImageExporter";
	}

	@Override
	public String getName() {
		return "Arc Ascii Grid Exporter";
	}

	@Override
	public String getDefaultExtension() {
		return "asc";
	}

	@Override
	public boolean canExport(Query query) {
		return (GriddedQuery.class.isAssignableFrom(query.getClass()));
	}

	@Override
	public void export(Query query, File file, IProgressMonitor monitor)
			throws Exception {
		
		File sourceFile = ((GriddedQuery)query).getLastRasterFile();
		if (sourceFile == null || !sourceFile.exists()){
			throw new Exception("Query could not be exported.  Please run the query and try again.", new Exception("File: " + (sourceFile == null ? "NULL" : sourceFile.toString()) + " not found."));
		}

		
	    GeoTiffFormat frmt = new GeoTiffFormat();
	    AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) frmt.getReader(sourceFile);
	    GridCoverage gridCoverage = reader.read(null);
	        
		ArcGridWriter writer = new ArcGridWriter(file);
		writer.write(gridCoverage, null);
		writer.dispose();

	}

}

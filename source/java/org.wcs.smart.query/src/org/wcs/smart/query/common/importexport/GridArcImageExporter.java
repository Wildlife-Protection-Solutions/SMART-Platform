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
package org.wcs.smart.query.common.importexport;

import java.io.File;
import java.text.MessageFormat;
import java.util.HashMap;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.opengis.coverage.grid.GridCoverage;
import org.wcs.smart.common.filter.ISmartProgressMonitor;
import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.model.GridQueryResult;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.importexport.IQueryExporter;
import org.wcs.smart.query.internal.Messages;
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
		return "org.wcs.smart.query.export.GridArcImageExporter"; //$NON-NLS-1$
	}

	@Override
	public String getName() {
		return Messages.GridArcImageExporter_ExporterName;
	}

	@Override
	public String getDefaultExtension() {
		return "asc"; //$NON-NLS-1$
	}

	@Override
	public boolean canExport(Query query) {
		return (GriddedQuery.class.isAssignableFrom(query.getClass()));
	}

	@Override
	public void export(Query query, IQueryResult result, File file,
			HashMap<String, Object> parameters, ISmartProgressMonitor monitor)
			throws Exception {
		
		File sourceFile = ((GridQueryResult)result).getRasterFile();
		if (sourceFile == null || !sourceFile.exists()){
			throw new Exception(Messages.GridArcImageExporter_ExportError, 
					new Exception(
							MessageFormat.format(Messages.GridArcImageExporter_FileNotFound, new Object[]{(sourceFile == null ? "NULL" : sourceFile.toString()) }))); //$NON-NLS-1$
		}

		
	    GeoTiffFormat frmt = new GeoTiffFormat();
	    AbstractGridCoverage2DReader reader = (AbstractGridCoverage2DReader) frmt.getReader(sourceFile);
	    GridCoverage gridCoverage = reader.read(null);
	        
		ArcGridWriter writer = new ArcGridWriter(file);
		writer.write(gridCoverage, null);
		writer.dispose();

	}

}

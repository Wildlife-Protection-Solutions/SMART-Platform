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
package org.wcs.smart.report.birt.map;

import java.io.File;
import java.util.List;

import org.eclipse.birt.report.engine.extension.IBaseResultSet;
import org.eclipse.birt.report.engine.extension.IExecutorContext;

/**
 * creates a raster from the query results
 * @author Emily
 *
 */
public interface IRasterCreator {

	public boolean canProcess(IExecutorContext context, String datasetId, String queryText) throws Exception;
	/**
	 * Creates a raster and returns the file that contains the raster
	 * @return
	 */
	public File createRaster(IExecutorContext context, String datasetId, IBaseResultSet results) throws Exception;

	/**
	 * Files to remove after raster rendered
	 * @return
	 */
	public List<File> getFilesToCleanUp();
	
	/**
	 * 
	 * @return the minimum value in the raster; should only be called after createRaster is called
	 */
	public double getRasterMinValue();
	
	/**
	 * 
	 * @return the maximum value in the raster; should only be called after createRaster is called
	 */
	public double getRasterMaxValue();
}

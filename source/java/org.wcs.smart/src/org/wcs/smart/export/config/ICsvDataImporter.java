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
package org.wcs.smart.export.config;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;

/**
 * Class that is able to perform import from csv file operation.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public interface ICsvDataImporter {

	/**
	 * Imports a delimited file
	 * @param file the file to import
	 * @param delimiter the field delimiter
	 * @param headers if the first line contains headers
	 * @param monitor progress monitor 
	 * @param session db session
	 * @return
	 * @throws Exception
	 */
	public boolean importCsvFile(File file, char delimiter, boolean headers, Charset cs, IProgressMonitor monitor, Session session) throws Exception;
	
	/**
	 * 
	 * @return a list of warning generated during the import process.  Can be
	 * <code>null</code> or an empty list if no warnings generated
	 */
	public List<String> getWarnings();
}

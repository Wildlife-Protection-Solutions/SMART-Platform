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
package org.wcs.smart.query.importexport;

import java.util.ArrayList;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.query.model.IQueryType;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.xml.model.QueryType;

/**
 * Interface for query importer.  Different query
 * types should implement a query importer for importing 
 * the xml definition if applicable.
 * <p>Currently you must also update the QueryImporter.java
 * class to find the query importer to use if adding
 * addition imports.
 * </p>
 * 
 * @author Emily
 *
 */
public interface IQueryImporter {

	/**
	 * Determines if the importer can import the given query type.
	 * 
	 * @param qt
	 * @return
	 */
	public boolean canImport(IQueryType qt);
	
	/**
	 * Imports the given xml query.  This function is responsible
	 * for converting to the internal model format.  It is not responsible
	 * for saving the query to the database.  That is done in a separate
	 * process.
	 * 
	 * @param qt the xml query type to import.
	 * @param the conservation area 
	 * @return the imported query definition
	 * @throws Exception if the query cannot be imported
	 */
	public Query importQuery(QueryType xmlQuery, ConservationArea ca) throws Exception;
	
	/**
	 * Warnings are displayed to the user and the user is allowed to choose
	 * if they want to proceed with the query import.
	 * <p>An example of a warning may be that an employee with the same name but different
	 * uuid is found.</p>
	 * <p>If an error occurs that does not allow the query to be imported
	 * the importQuery function should throw an Exception. Warnings should
	 * only be populated if the Query can still be imported.
	 * </p>
	 * @return warnings generated during import process
	 */
	public ArrayList<String> getWarnings();
}

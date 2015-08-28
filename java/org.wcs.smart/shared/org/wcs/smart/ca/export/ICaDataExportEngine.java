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
package org.wcs.smart.ca.export;

import java.io.File;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;

/**
 * Engine for exporting conservation area data
 * @author egouge
 * @since 1.0.0
 */
public interface ICaDataExportEngine {

	/**
	 * The name of the conservation area data file in the exported
	 * data.
	 */
	public static final String CA_INFO_FILENAME = "conservationarea.dat"; //$NON-NLS-1$
	/**
	 * The name of the directory where the database data is stored
	 */
	public static final String DATABASE_DIR = "database"; //$NON-NLS-1$
	/**
	 * The name of the directory where the filestore data is 
	 * stored
	 */
	public static final String FILESTORE_DIR = "filestore"; //$NON-NLS-1$


	/**
	 * @return the conservation area being exported
	 */
	public ConservationArea getConservationArea();
	
	/**
	 * @return the current hibernate session
	 */
	public Session getSession();
	
	/**
	 * @return the location to export data to
	 */
	public File getExportLocation();
	
	/**
	 * Finds all the columns in a given table
	 * @param tableName schema qualified table name
	 * @return list of columns in the table
	 * @throws Exception
	 */
	String[] getTableColumns(String tableName) throws Exception;
	
	/**
	 * Writes the required table definition file.
	 * 
	 * @param tableName the table name
	 * @param hibernateClass the hibernate class represented by the tablename
	 * @param columns the columns in the table
	 * @throws Exception
	 */
	void writeTableDefinitionFile(String tableName, String hibernateClass, String[] columns) throws Exception;
	
	/**
	 * Exports all data in the given table.
	 * 
	 * @param tableName the table name
	 * @param hibernateClass the hibernate class represented by the tablename
	 * @param columns all columns in the table
	 * @param conservationAreaProperty the conservation area property column name
	 * @throws Exception
	 */
	void exportTableData(String tableName, String hibernateClass, String[] columns, String conservationAreaProperty) throws Exception;
	
	/**
	 * Exports all the data from a table that 
	 * does not have a conservationAreaProperty.  Instead the 
	 * conservation area property is represented through an hql
	 * query.
	 * 
	 * @param tableName the schema qualified database table name to export
	 * @param hibernateClass the hibernate class representing the database table
	 * @param columns the columns in the database table
	 * @param caPropertyQuery the hql query that links the table with 
	 * a conservationarea property (ie for AttributeTreeNode .attribute.conservationArea)
	 * @throws Exception
	 */
	void writeHibernateQuery(String tableName,
			String hibernateClass,
			String[] columns,
			String caPropertyQuery) throws Exception;
	
	
	/**
	 * Writes data from a table.  The query is an SQL query
	 * that identifies
	 * which rows from the table that will be exported.
	 * The query should return the table columns in the order
	 * provided by the results of the 
	 * getTableColumns(tableName) function.
	 * 
	 * @param tableName the table name
	 * @param query the query to extract the rows
	 * @throws Exception
	 */
	void writeQuery(String fileName,
			String query) throws Exception;
	
}

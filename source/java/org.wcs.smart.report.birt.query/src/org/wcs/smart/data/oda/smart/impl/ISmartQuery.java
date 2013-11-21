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
package org.wcs.smart.data.oda.smart.impl;

import org.eclipse.datatools.connectivity.oda.IResultSet;
import org.eclipse.datatools.connectivity.oda.IResultSetMetaData;
import org.eclipse.datatools.connectivity.oda.OdaException;

/**
 * Query dataset handler for allowing queries
 * to be available in a BIRT report.
 * 
 * @author Emily
 *
 */
public interface ISmartQuery {

	/**
	 * Report Smart Query Extension Point
	 */
	public static final String SMART_QUERY_EXTENSION_ID = "org.wcs.smart.report.birt.query.queryDataset"; //$NON-NLS-1$
	
	/**
	 * Prepares the query but loading and ensure it exists.
	 * @param smartQuery
	 * @throws OdaException
	 */
	public void prepare(SmartQuery smartQuery) throws OdaException;
	
	/**
	 * Executes the queries and returns the result set
	 * @param smartQuery
	 * @return
	 * @throws OdaException
	 */
	public IResultSet executeQuery(SmartQuery smartQuery) throws OdaException;
	
	/**
	 * Return result set metadata
	 * @param smartQuery
	 * @return
	 * @throws OdaException
	 */
	public IResultSetMetaData getMetaData(SmartQuery smartQuery) throws OdaException;
	
	
}
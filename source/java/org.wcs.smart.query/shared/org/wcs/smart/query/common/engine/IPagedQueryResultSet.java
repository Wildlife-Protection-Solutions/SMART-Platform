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
package org.wcs.smart.query.common.engine;

import java.util.Iterator;
import java.util.List;

import org.wcs.smart.query.model.QueryColumn;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Interface for paging results.  This is currently used
 * for observation queries but can be used for other large
 * query types as well.
 * 
 * @author Emily
 *
 */
public interface IPagedQueryResultSet extends IQueryResult{
	/**
	 * Reasonable page size for ui tables
	 */
	public static final int TABLE_DEFAULT_PAGE_SIZE = 30;
	
	/**
	 * A larger page size for iterating over all features
	 * (for example in a map). 
	 */
	public static final int MAP_PAGE_SIZE = 1000;
	
	/**
	 * Close and destroy the paged results set
	 */
	public void destroy();
	
	/**
	 * Gets all data starting from the given offset
	 * for the given page size.
	 * 
	 * @param offset
	 * @param pageSize
	 * @return
	 */
	public List<? extends IResultItem> getData(int offset, int pageSize);
	
	/**
	 * This is only applicable to result sets that
	 * return spatial data.  Returns the extends of the spatial
	 * data in the results set.  Projection
	 * is assumed to be 4326 (default database projection)
	 * 
	 * @return
	 */
	public Envelope getEnvelope();
	
	/**
	 * Creates an iterator that will iterator over all 
	 * elements in the result set loading the given pagesize
	 * each time.
	 * 
	 * @param pageSize
	 * @return
	 */
	public Iterator<? extends IResultItem> iterator(int pageSize);

	/**
	 * Sets the current sorting column.
	 * @param sortColumn
	 * @param direction SWT.UP or SWT.DOWN
	 */
	public void setSorting(QueryColumn sortColumn, int direction) ;
		
	/**
	 * 
	 * @return the total item count
	 */
	public int getItemCount() ;

}
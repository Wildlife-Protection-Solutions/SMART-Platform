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

import java.sql.SQLException;
import java.util.List;

import org.hibernate.Session;

/**
 * Interface for queries that also return a set of attachments associated
 * with the results.
 * 
 * @author Emily
 *
 */
public interface IPagedImageResultSet {

	/**
	 * Gets all attachments starting from the given offset
	 * for the given page size.
	 * 
	 * This is used by the ui and the result item doesn't
	 * need to include all the observation information.
	 * 
	 * @param offset
	 * @param pageSize
	 * @return
	 */
	public List<IAttachmentResultItem> getImageData(int offset, int pageSize);

	
	/**
	 * Gets an iterator that iterates over all the
	 * attachments in a result set.
	 * 
	 * This is used by the reports the the associated
	 * resultitems should generally include all the
	 * observation details including the attachment.
	 * 
	 * @param session
	 * @return
	 * @throws SQLException
	 */
	public default IQueryResultSetIterator<? extends IAttachmentResultItem> getImageIterator(Session session) throws SQLException{
		return null;
	}

	/**
	 * 
	 * @return the total number of items with images
	 */
	public int getImageCount();

}

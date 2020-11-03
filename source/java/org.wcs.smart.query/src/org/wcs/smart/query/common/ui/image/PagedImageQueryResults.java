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
package org.wcs.smart.query.common.ui.image;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jdbc.ReturningWork;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.test.IDesktopWOEngine;

/**
 * Abstract class for iterating over a list of attachments represented by 
 * a table in the database.
 * 
 * @author Emily
 *
 */
public abstract class PagedImageQueryResults {

	private String imageTempTable = null;
	private int imageDataCnt = -1;
	
	private IDesktopWOEngine<?> engine;
	
	public PagedImageQueryResults(IDesktopWOEngine<?> engine) {
		this.engine = engine;
	}

	/**
	 * The data table representing the attachments
	 * @return
	 */
	public String getResultsTable() {
		return imageTempTable;
	}

	/**
	 * The number of attachments in the result set
	 * @return
	 */
	public int getImageCount() {
		if (imageTempTable == null) {
			initImageData();
		}
		return imageDataCnt;
	}
	
	/**
	 * Sets the table containing the attachments and the data count.
	 * The table must have at least two columns.  The first column must
	 * represent the attachment uuid the second (seq_order) the order the attachments are 
	 * to be loaded 
	 * 
	 * @param imageTempTable
	 * @param imageDataCnt
	 */
	public void setResults(String imageTempTable, int imageDataCnt) {
		this.imageTempTable = imageTempTable;
		this.imageDataCnt = imageDataCnt;
	}
	
	public List<IAttachmentResultItem> getImageData(int offset, int pageSize) {
		if (imageTempTable == null) {
			initImageData();
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT r.*, b.attach_uuid as attach_uuid FROM " ); //$NON-NLS-1$
		sb.append(engine.getQueryDataTable() + " r "); //$NON-NLS-1$
		sb.append(" join " + imageTempTable + " b "); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("on r.wp_uuid = b.wp_uuid order by b.seq_order "); //$NON-NLS-1$
		
		List<IAttachmentResultItem>  imageData = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
		
			ResultSet imageResultSet = session.doReturningWork(new ReturningWork<ResultSet>() {
				@Override
				public ResultSet execute(Connection c) throws SQLException {
					return c.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)
							.executeQuery(sb.toString());	
				}
			});
			
			imageResultSet.absolute(offset);
			int to = offset + pageSize;
			if (to >= imageDataCnt) {
				to = imageDataCnt;
			}
			for(int x = offset; x < to; x++) {
				imageResultSet.next();
				imageData.add(engine.asQueryAttachmentResultItem(imageResultSet, session));
			}
		}catch (SQLException ex) {
			QueryPlugIn.log("Error loading image data: " + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
		return imageData;
	}

	/**
	 * Initializes the image database table that supports the results set.
	 * The table must have at least two columns.  The first column must
	 * represent the attachment uuid the second (seq_order) the order the attachments are 
	 * to be loaded 
	 */
	protected /* synchronized */ abstract void initImageData();
	
}

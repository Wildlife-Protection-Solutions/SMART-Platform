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
package org.wcs.smart.patrol.query.ext;

import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.query.common.engine.IQueryEngine;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.ui.model.DropItem;

/**
 * Maps an IExtensionFilter to the associated UI
 * elements. 
 *
 * @author elitvin
 * @since 1.0.0
 */
public interface IExtensionFilterViewer {

	/**
	 * Name of the option as it appears on the gui
	 * @return
	 */
	public String getName();

	/**
	 * Creates a new drop item for the filter.
	 * @return
	 */
	public DropItem asDropItem();
	
	/**
	 * The image associated with the filter
	 * @return
	 */
	public Image getImage();
	
	/**
	 * 
	 * @return the filter class
	 */
	public Class<? extends IExtensionFilter> getFilterClass();
	
	/**
	 * Converts a filter to associated drop items 
	 * @param filter
	 * @param session
	 * @return
	 */
	public DropItem[] getDropItems(IFilter filter, Session session);
	
	/**
	 * Converts the given filter to sql 
	 * @param tableMapping mapping of database tables to query table prefix
	 * @param filter filter to process
	 * @return null if filter is not one of the filters produced by createFilter; otherwise 
	 * the sql string representing the query filter
	 */
	public String asSql(IQueryEngine engine, Session s, IFilter filter );
}

/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.query;

import java.util.Collections;
import java.util.List;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.query.IResultItem;

/**
 * An interface to implement for performing an action when the user
 * selects a row from a query results table.
 * 
 * @author Emily
 *
 */
public interface IQuerySourceFinder {

	/**
	 * Runs some action based on a result item record.
	 * 
	 * @param item
	 */
	public void runAction(IResultItem item);
	
	/**
	 * Optional; can return null;
	 * @return
	 */
	public Image getImage();
	
	/**
	 * required.  returns the name of the finder
	 * @return
	 */
	public String getName();
	
	/**
	 * Finds all finders for a given query type.
	 * @param query
	 * @return
	 */
	public static List<IQuerySourceFinder> getQuerySources(IntelRecordObservationQuery query){
		if (query.getClass().equals(IntelRecordObservationQuery.class)){
			return Collections.singletonList(ObservationQuerySourceFinder.INSTANCE);
		}
		return Collections.emptyList();
	}
}

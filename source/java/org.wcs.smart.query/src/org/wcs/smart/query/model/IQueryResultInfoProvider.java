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
package org.wcs.smart.query.model;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.internal.Messages;

/**
 * A info provider that can be attached to query results.  These
 * are added a right click menus to the query result tables and
 * can do extra work provided based on the result item provided.
 * 
 * @author Emily
 *
 */
public interface IQueryResultInfoProvider {

	/**
	 * GOTO String
	 */
	public static final String GOTO_SOURCE_STR = Messages.IQueryResultInfoProvider_GoToLabel;
	/**
	 * Error String
	 */
	public static final String ERROR_STR = Messages.IQueryResultInfoProvider_ErrorLabel;
	/**
	 * Operation not supported string
	 */
	public static final String OP_NOT_SUPPORTED_STR = Messages.IQueryResultInfoProvider_OpNotSupported;
	
	/**
	 * Get the name of the info provider.  This is displayed to the user.
	 * 
	 * @return
	 */
	public String getName();
	
	/**
	 * Get an image associated with the provider.  Can be null.
	 * @return
	 */
	public Image getImage();
	
	/**
	 * Do the action.
	 * 
	 * @param resultItem
	 */
	public void doWork(IResultItem resultItem);
	
	/**
	 * 
	 * @return true if works with ccaa query results
	 */
	public boolean supportsCcaa();
	
	
	/**
	 * 
	 * @return true if works on map page 
	 */
	public default boolean supportsMap(){
		return false;
	}

	/**
	 * 
	 * @return true if works on table page 
	 */
	public default boolean supportsTable(){
		return true;
	}
}

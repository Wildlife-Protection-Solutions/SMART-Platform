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

import org.wcs.smart.query.common.engine.IQueryResult;
import org.wcs.smart.query.common.engine.IResultItem;

/**
 * interface for adding menu items to a results table 
 * @author Emily
 *
 */
public abstract class IQueryEditCommand implements IQueryResultInfoProvider{

	/**
	 * Do the action.
	 * 
	 * @Return true if result set updated, false if no changes
	 * @param resultItem
	 */
	public abstract boolean doWork(IResultItem resultItem, IQueryResult results);

	@Override
	public void doWork(IResultItem resultItem){
		throw new UnsupportedOperationException("doWork(resultItem) not support for editing item; call doWork(resultItem, reuslts, table) instead"); //$NON-NLS-1$
	}
	
}

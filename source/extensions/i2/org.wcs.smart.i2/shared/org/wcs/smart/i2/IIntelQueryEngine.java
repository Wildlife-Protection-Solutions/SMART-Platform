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
package org.wcs.smart.i2;

import java.util.HashMap;

import org.wcs.smart.SmartContext;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.query.IQueryResult;

/**
 * Intelligence Observation query engine.
 * 
 * @author Emily
 *
 */
public interface IIntelQueryEngine {

	public static IIntelQueryEngine createEngine( String queryType ) {
		return SmartContext.INSTANCE.getClass(IQueryEngineFactory.class).findQueryEngine(queryType);
	}
	
	/**
	 * Execute a query and return the results
	 * 
	 * @param query
	 * @param parameters
	 * @return
	 * @throws Exception
	 */
	public IQueryResult executeQuery(AbstractIntelQuery query,  HashMap<String, Object> parameters) throws Exception;

}

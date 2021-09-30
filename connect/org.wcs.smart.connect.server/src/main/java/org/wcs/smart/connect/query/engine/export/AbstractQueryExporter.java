/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.connect.query.engine.export;

import java.sql.SQLException;
import java.util.Locale;

import javax.servlet.ServletContext;
import javax.ws.rs.core.StreamingOutput;

import org.hibernate.Session;
import org.wcs.smart.IProjectionProvider;
import org.wcs.smart.query.common.engine.IQueryResult;

/**
 * Abstract class for query streaming exporter
 * @author Emily
 *
 */
public abstract class AbstractQueryExporter implements StreamingOutput {

	protected Locale locale;
	protected ServletContext context;
	protected IProjectionProvider prjProvider;
	
	public AbstractQueryExporter(IProjectionProvider prjProvider, Locale locale, ServletContext context) {
		this.locale = locale;
		this.context = context;
		this.prjProvider = prjProvider;
	}
	
	/**
	 * Dispose query results
	 * 
	 * @param result
	 * @param session
	 * @throws SQLException
	 */
	protected void dispose(IQueryResult result, Session session) throws SQLException {
		result.dispose(session);
	}

	/**
	 * Dispose query results
	 * @param result
	 * @param session
	 * @throws SQLException
	 */
	protected void dispose(org.wcs.smart.i2.query.IQueryResult result, Session session) throws SQLException {
		result.dispose(session);
	}

	
}

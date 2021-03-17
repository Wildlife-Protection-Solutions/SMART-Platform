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
package org.wcs.smart.connect.report;

import java.util.Locale;

import javax.servlet.ServletContext;

import org.hibernate.Session;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;

/**
 * Connection provider 
 * @author Emily
 *
 */
public class ConnectConnectionProvider implements IDatabaseConnectionProvider {

	private static final long serialVersionUID = 1L;

	private ServletContext context;
	private Locale l;
	
	public ConnectConnectionProvider(ServletContext context, Locale l){
		this.context = context;
		this.l = l;
	}
	
	@Override
	public Session openSession() {
		return HibernateManager.openNewSession(context,l);
	}
	
	@Override
	public Locale getLocale() {
		return l;
	}

	@Override
	public void finishSession(Session session) {
		session.close();
	}

}

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
package org.wcs.smart.startup;

import org.hibernate.Session;

/**
 * Executes some database code as the database starts up
 * the first time the application is run.
 * 
 * @author Emily
 *
 */
public interface IDatabaseStartupRunner {
	
	public static final String EXTENSION_ID = "org.wcs.smart.startup.db"; //$NON-NLS-1$
	
	public static final String CLASS_ATTRIBUTE_NAME = "StartUpRunner"; //$NON-NLS-1$
	
	/**
	 * Runs the first time the application is started
	 * and the database is starting up.  Does not run
	 * when the database is shutdown and restarted
	 * unless the application specifically restarted 
	 * 
	 * @param session
	 * @throws Exception
	 */
	public void run(Session session) throws Exception;
}

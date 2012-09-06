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
package org.wcs.smart.report.in;

import java.util.HashMap;

import org.eclipse.birt.report.model.api.ReportDesignHandle;

/**
 * Report import handler that is
 * called when a report is imported.  
 * <p>
 * Implementations can make changes to the
 * report as required.  These changes will
 * be saved after all handlers are
 * run.
 * </p>
 * @author egouge
 *
 */
public interface IReportImportHandler {

	/**
	 * Fired when report imported.
	 * 
	 * @param handle the report design handler; can be modified
	 * @param oldToNewQuery a link of old query ids to the new query ids
	 * @throws Exception
	 */
	public void reportImported(ReportDesignHandle handle, HashMap<String,String> oldToNewQuery) throws Exception;
}

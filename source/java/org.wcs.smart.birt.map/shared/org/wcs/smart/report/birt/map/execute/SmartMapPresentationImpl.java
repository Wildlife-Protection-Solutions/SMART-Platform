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
package org.wcs.smart.report.birt.map.execute;

import java.io.ByteArrayInputStream;

import org.eclipse.birt.core.exception.BirtException;
import org.eclipse.birt.report.engine.content.IForeignContent;
import org.eclipse.birt.report.engine.extension.IRowSet;
import org.eclipse.birt.report.engine.extension.ReportItemPresentationBase;

/**
 * For rendering map in a report.
 * 
 * @author Emily
 *
 */
public class SmartMapPresentationImpl extends ReportItemPresentationBase {

	public static final int MIN_DPI = 72;
	public static final int DEFAULT_DPI = 96;
	public static final int MAX_DPI = 300;
	
	/**
	 * @see org.eclipse.birt.report.engine.extension.ReportItemPresentationBase#getOutputType()
	 */
	public int getOutputType() {
		return OUTPUT_AS_IMAGE;
	}

	/**
	 * create query for non-listing report item
	 * 
	 * @param item
	 *            report item
	 * @return a report query
	 */

	/**
	 * @see org.eclipse.birt.report.engine.extension.ReportItemPresentationBase#onRowSets(org.eclipse.birt.report.engine.extension.IRowSet[])
	 */
	public Object onRowSets(IRowSet[] rowSets) throws BirtException {
		byte[] mapimage = (byte[])((IForeignContent) content).getRawValue();
		return new ByteArrayInputStream(mapimage);
	}
}
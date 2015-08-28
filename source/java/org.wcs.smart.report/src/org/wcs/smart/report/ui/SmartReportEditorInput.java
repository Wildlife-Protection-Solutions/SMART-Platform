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
package org.wcs.smart.report.ui;

import org.eclipse.birt.report.designer.internal.ui.editors.ReportEditorInput;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.model.Report;

/**
 * Wrapper around report editor input to include report object
 * @author egouge
 * @since 1.0.0
 */
public class SmartReportEditorInput extends ReportEditorInput {

	private Report report;
	
	/**
	 * @param report the report object
	 */
	public SmartReportEditorInput(Report report) {
		super(ReportPlugIn.getDefault().getReportFile(report));
		this.report = report;
	}
	
	/**
	 * @return the report object
	 */
	public Report getReport(){
		return this.report;
	}
	
	/**
	 * Based on the report object
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj != null && obj.getClass() == getClass())
			return this.report.equals(((SmartReportEditorInput) obj).report);
		return false;
	}
	
	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return report.hashCode();
	}

}

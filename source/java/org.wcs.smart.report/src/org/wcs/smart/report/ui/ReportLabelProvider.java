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

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.model.Report;
import org.wcs.smart.report.model.ReportFolder;
import org.wcs.smart.report.model.RootReportFolder;

/**
 * Label provider for report items
 * @author egouge
 * @since 1.0.0
 */
public class ReportLabelProvider extends LabelProvider {

	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns <code>null</code>.
	 * Subclasses may override.
	 */
	public Image getImage(Object element) {
		if (element instanceof RootReportFolder || element instanceof ReportFolder){
			return JFaceResources.getImage(QueryPlugIn.FOLDER_ICON);
		}else if (element instanceof Report){
			return JFaceResources.getImage(ReportPlugIn.REPORT_ICON);
		}
		return null;
	}

	/**
	 * The <code>LabelProvider</code> implementation of this
	 * <code>ILabelProvider</code> method returns the element's
	 * <code>toString</code> string. Subclasses may override.
	 */
	public String getText(Object element) {
		if (element instanceof RootReportFolder){
			return ((RootReportFolder) element).getName();
		}else if (element instanceof Report){
			return ((Report) element).getName() + " [" + ((Report)element).getId() + "]";
		}else if (element instanceof ReportFolder){
			return ((ReportFolder)element).getName();
		}
		return element == null ? "" : element.toString();//$NON-NLS-1$
	}
}

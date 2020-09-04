/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.report;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.report.internal.ui.viewer.ReportView;
import org.wcs.smart.report.internal.ui.viewer.ReportView.ReportViewWrapper;

/**
 * "Save Output As..." property tester
 * @author Emily
 * @since 7.0.0
 *
 */
public class ExportablePropertyTester extends PropertyTester {

	public ExportablePropertyTester() {
	}

	@Override
	@SuppressWarnings("restriction")
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (PlatformUI.getWorkbench() == null) return false;
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow() == null) return false;
		if (PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage() == null) return false;
		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActivePart();
		if (part == null) return false;
		if (!part.getSite().getId().equalsIgnoreCase(ReportView.ID)) return false;
		ReportView view = ((ReportViewWrapper)part).getComponent();
		if (view.getRenderFile() == null) return false;
		return true;
	}

}

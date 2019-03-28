/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.common.control;

import java.util.Locale;

import org.eclipse.nebula.jface.tablecomboviewer.TableComboViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;

/**
 * UI Utilities for SMART
 * 
 * @author Emily
 *
 */
public class SmartUiUtils {

	public static void configure(TableComboViewer viewer) {
		//the height for these does not configure nicely on mac so for mac only we adjust the height
		//assumption is viewer layout data is griddata if not we don't do anything
		if (!(viewer.getControl().getLayoutData() instanceof GridData)) return;		
		if (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac")) { //$NON-NLS-1$ //$NON-NLS-2$
			GridData gd = (GridData) viewer.getControl().getLayoutData();
			gd.heightHint = viewer.getControl().computeSize(SWT.DEFAULT,SWT.DEFAULT).y + 6;
		}
	}
}

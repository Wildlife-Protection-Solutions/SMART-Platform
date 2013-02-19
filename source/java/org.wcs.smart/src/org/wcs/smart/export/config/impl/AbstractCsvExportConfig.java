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
package org.wcs.smart.export.config.impl;

import org.eclipse.swt.SWT;
import org.wcs.smart.export.config.ICsvExportDialogConfig;
import org.wcs.smart.export.dialog.CsvExportDialog;
import org.wcs.smart.internal.Messages;

/**
 * Basic export configuration for {@link CsvExportDialog}
 * 
 * @author elitvin
 * @since 1.0.0
 */
public abstract class AbstractCsvExportConfig implements ICsvExportDialogConfig {

	@Override
	public boolean includeHasHeader() {
		return false;
	}

	@Override
	public String getHasHeaderText() {
		return ""; //$NON-NLS-1$
	}

	@Override
	public String getActionButtonText() {
		return Messages.CsvConfig_Action_Export;
	}

	@Override
	public int getFileDialogStyle() {
		return SWT.SAVE;
	}
	
}

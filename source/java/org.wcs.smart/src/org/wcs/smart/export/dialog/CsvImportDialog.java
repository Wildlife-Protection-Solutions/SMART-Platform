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
package org.wcs.smart.export.dialog;

import java.io.File;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.export.config.ICsvImportDialogConfig;

/**
 * Dialog for importing from csv file
 *
 * @author elitvin
 * @since 1.0.0
 */
public class CsvImportDialog extends AbstractCsvDialog {

	private ICsvImportDialogConfig config;

	/**
	 * @param parentShell
	 * @param config
	 */
	public CsvImportDialog(Shell parentShell, ICsvImportDialogConfig config) {
		super(parentShell, config);
		this.config = config;
	}

	@Override
	protected boolean performAction(File file, char delimiter, boolean headers, IProgressMonitor monitor, Session session) throws Exception {
		return config.getImporter().importCsvFile(file, delimiter, headers, monitor, session);
	}
	
	@Override
	protected List<String> getWarnings(){
		return config.getImporter().getWarnings();
	}
	
	@Override
	public Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		super.createFileComposite(comp, true);
		return comp;
	}

}

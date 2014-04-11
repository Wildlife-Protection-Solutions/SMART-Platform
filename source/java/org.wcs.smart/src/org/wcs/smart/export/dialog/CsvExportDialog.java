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
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.export.config.ICsvExportDialogConfig;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.SmartUtils;

/**
 * Dialog for exporting into csv file
 *
 * @author elitvin
 * @since 1.0.0
 */
public class CsvExportDialog extends AbstractCsvDialog {

	private ICsvExportDialogConfig config;
	
	/**
	 * @param parentShell
	 * @param config
	 */
	public CsvExportDialog(Shell parentShell, ICsvExportDialogConfig config) {
		super(parentShell, config);
		this.config = config;
	}

	@Override
	protected boolean performAction(File file, char delimiter, boolean headers, IProgressMonitor monitor, Session session) throws Exception {
		return config.getExporter().exportCsvFile(file, delimiter, SmartDB.getCurrentConservationArea(), headers, monitor, session);
	}

	@Override
	public Control createDialogArea(Composite parent) {
		Composite comp = (Composite) super.createDialogArea(parent);
		super.createFileComposite(comp, true);
		return comp;
	}
	
	/**
	 * ensure the export location exists and can be overwritten
	 * @param fileName
	 * @return
	 */
	@Override
	protected boolean validateFilename(String fileName){
		File f = new File(fileName);
		if (f.exists()){
			boolean ok = MessageDialog.openQuestion(getShell(), Messages.CsvExportDialog_DialogTitle, MessageFormat.format(Messages.CsvExportDialog_OverwriteFile, new Object[]{f.toString()}));
			if (!ok){
				return false;
			}
		}
		if (!f.getParentFile().exists()){
			boolean ok = MessageDialog.openQuestion(getShell(), Messages.CsvExportDialog_DialogTitle, MessageFormat.format(Messages.CsvExportDialog_CreateDirectory, new Object[]{f.getParent()}));
			if (ok){
				if (!SmartUtils.createDirectory(f.getParentFile())){
					return false;
				}
			}else{
				return false;
			}
		}
		return true;
	}
	
	@Override
	protected List<String> getWarnings(){
		return null;
	}
}

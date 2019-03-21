/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.cybertracker.ctpackage.ui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.util.UuidUtils;

/**
 * Exports cybertracker package to local filestore/device.
 * 
 * @author Emily
 *
 */
public class ExportPackageToFileAction implements ICtExportAction {

	
	@Override
	public void doAction(List<ICtPackage> ctpackages, Shell shell) {
		exportLocal(ctpackages, shell);
	}

	@Override
	public String getName() {
		return "Export to File/Device";
	}

	@Override
	public Image getIcon() {
		return null;
	}

	private void exportLocal(List<ICtPackage> towrite, Shell shell) {
		
		// export all the packages
		if (towrite.size() == 1) {
			// single package let the user pick the file name
			FileDialog fd = new FileDialog(shell, SWT.SAVE);
			fd.setText("Export CyberTracker Package");
			fd.setFilterExtensions(new String[] { "*.zip", "*.*" });
			fd.setFilterNames(new String[] { "Cybertracker Package(*.zip)", "All Files (*.*)" });
			// fd.setFilterPath(string);
			// fd.setfilename

			String output = fd.open();
			if (output == null)
				return;

			Path exportFile = Paths.get(output);
			if (Files.exists(exportFile)) {
				if (!MessageDialog.openQuestion(shell, "Overwrite",
						MessageFormat.format("File file {0} exists and will be overwritten.  Do you want to continue?",
								exportFile.toString()))) {
					return;
				}
				boolean ok = true;
				try {
					ok = Files.deleteIfExists(exportFile);
				} catch (Exception ex) {
					CyberTrackerPlugIn.log(ex.getMessage(), ex);
					ok = false;
				}
				if (!ok) {
					MessageDialog.openError(shell, "Error",
							MessageFormat.format("Unable to remove file {0}.", exportFile.toString()));
					return;
				}
			}

			try {
				Files.copy(towrite.get(0).getLocalFile(), exportFile);
				MessageDialog.openInformation(shell, "Export Cybertracker Package",
						MessageFormat.format("Package exported to {0}", exportFile.toString()));

			} catch (IOException e) {
				CyberTrackerPlugIn.displayError("Error", "Error exporting cybertracker package: " + e.getMessage(), e);
			}
		} else {
			// multiple packages get a directory from the user and use the package name/uuid
			// as the
			// file name
			DirectoryDialog dd = new DirectoryDialog(shell, SWT.NONE);
			dd.setText("Export CyberTracker Packages");
			String output = dd.open();
			if (output == null)
				return;
			Path exportPath = Paths.get(output);

			int cnt = 0;
			for (ICtPackage w : towrite) {
				try {
					String name = w.getName().replaceAll("[^a-zA-Z0-9]", "");
					Path export = exportPath.resolve(name + "." + UuidUtils.uuidToString(w.getUuid()) + ".zip");
					Files.copy(towrite.get(0).getLocalFile(), export);
					cnt++;

				} catch (IOException e) {
					CyberTrackerPlugIn.displayError("Error", "Error exporting cybertracker package: " + e.getMessage(),
							e);
				}
			}
			MessageDialog.openInformation(shell, "Export Cybertracker Package", MessageFormat
					.format("Exported {0} or {1} packages to {2}", cnt, towrite.size(), exportPath.toString()));
		}
	}

}

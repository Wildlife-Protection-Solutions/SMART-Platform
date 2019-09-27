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
package org.wcs.smart.cybertracker.navigation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.NavigationLayer;

/**
 * Exports cybertracker package to local filestore/device.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class ExportNavigationToFileAction implements INavigationExportAction {

	
	@Override
	public void doAction(List<NavigationLayer> layers, IEclipseContext context) {
		exportLocal(layers, context.get(Shell.class));
	}

	@Override
	public String getName() {
		return Messages.ExportPackageToFileAction_OptionNAme;
	}

	@Override
	public Image getIcon() {
		return CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_FILE32);
	}

	private void exportLocal(List<NavigationLayer> towrite, Shell shell) {
		
		// export all the packages
		if (towrite.size() == 1) {
			// single package let the user pick the file name
			FileDialog fd = new FileDialog(shell, SWT.SAVE);
			fd.setText(Messages.ExportNavigationToFileAction_FileDialogText);
			fd.setFilterExtensions(new String[] { "*.zip", "*.*" }); //$NON-NLS-1$ //$NON-NLS-2$
			fd.setFilterNames(new String[] { Messages.ExportNavigationToFileAction_ZipFile, Messages.ExportNavigationToFileAction_AllFiles });
			fd.setFileName(URLUtils.cleanFilename(towrite.get(0).getName()) + ".zip"); //$NON-NLS-1$

			String output = fd.open();
			if (output == null)
				return;

			Path exportFile = Paths.get(output);
			if (Files.exists(exportFile)) {
				if (!MessageDialog.openQuestion(shell, Messages.ExportNavigationToFileAction_OverwriteTitle,
						MessageFormat.format(Messages.ExportNavigationToFileAction_OverwriteMsg,
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
					MessageDialog.openError(shell, Messages.ExportNavigationToFileAction_ErrorTitle,
							MessageFormat.format(Messages.ExportNavigationToFileAction_DeleteFileError, exportFile.toString()));
					return;
				}
			}

			try {
				ExportNavigationManager.INSTANCE.exportNavigationLayer(towrite.get(0), exportFile);
				MessageDialog.openInformation(shell, Messages.ExportNavigationToFileAction_ExportTitle,
						MessageFormat.format(Messages.ExportNavigationToFileAction_ExportMessage, exportFile.toString()));

			} catch (IOException e) {
				CyberTrackerPlugIn.displayError(Messages.ExportNavigationToFileAction_ErrorTitle, 
						Messages.ExportNavigationToFileAction_ExportErrorMsg + e.getMessage(), e);
			}
		} else {
			// multiple packages get a directory from the user and use the package name/uuid
			// as the
			// file name
			DirectoryDialog dd = new DirectoryDialog(shell, SWT.NONE);
			dd.setText(Messages.ExportNavigationToFileAction_DirectorySelectorTitle);
			String output = dd.open();
			if (output == null)
				return;
			Path exportPath = Paths.get(output);

			int cnt = ExportNavigationManager.INSTANCE.exportNavigationLayers(towrite, exportPath).size();
			
			MessageDialog.openInformation(shell, Messages.ExportPackageToFileAction_ExportCompleteTitle, MessageFormat
					.format(Messages.ExportPackageToFileAction_ExportCompleteMultiMessage, cnt, towrite.size(), exportPath.toString()));
		}
	}

}

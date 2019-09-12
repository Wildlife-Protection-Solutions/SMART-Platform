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

import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.MobileDeviceUtils;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.NavigationLayer;
import org.wcs.smart.cybertracker.navigation.ExportNavigationManager;

/**
 * Exports cybertracker package to local filestore/device.
 * 
 * @author Emily
 *
 */
public class ExportPackageToDeviceAction implements ICtExportAction {

	
	@Override
	public void doAction(List<ICtPackage> ctpackages, List<NavigationLayer> navlayers, IEclipseContext context) {
		exportLocal(ctpackages, navlayers, context.get(Shell.class));
	}

	@Override
	public String getName() {
		return Messages.ExportPackageToDeviceAction_OptionName;
	}

	@Override
	public Image getIcon() {
		return CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.ICON_DEVICE32);
	}

	private void exportLocal(List<ICtPackage> towrite,  List<NavigationLayer> navlayers, Shell shell) {
		// export all the packages
		int cnt = 0;
		String date = DateTimeFormatter.ofPattern("ddMMYYYY").format(LocalDate.now()); //$NON-NLS-1$
		for (ICtPackage w : towrite) {
			try {
				MobileDeviceUtils.exportAppToDevice(w.getLocalFile(), URLUtils.cleanFilename(w.getName()) + "." + date + ".zip"); //$NON-NLS-1$ //$NON-NLS-2$
				cnt++;
			} catch (Exception e) {
				CyberTrackerPlugIn.displayError(Messages.ExportPackageToDeviceAction_ErrorTitle, 
						MessageFormat.format(Messages.ExportPackageToDeviceAction_ErrorMsg + "\n\n{1}", w.getName(), e.getMessage()), e); //$NON-NLS-1$
			}
		}
		for (NavigationLayer nl : navlayers) {
			String fname = ExportNavigationManager.INSTANCE.getExportFileName(nl);
			try {
				
				Path temp = Files.createTempFile("smart", ""); //$NON-NLS-1$ //$NON-NLS-2$
				try {
					ExportNavigationManager.INSTANCE.exportNavigationLayer(nl, temp);
					MobileDeviceUtils.exportAppToDevice(temp, fname);
					cnt++;
				}finally {
					Files.deleteIfExists(temp);
				}
			} catch (Exception e) {
				CyberTrackerPlugIn.displayError(Messages.ExportPackageToDeviceAction_ErrorTitle, 
						MessageFormat.format(Messages.ExportPackageToDeviceAction_NavLayerExportError + "\n\n{1}", nl.getName(), e.getMessage()), e); //$NON-NLS-1$
			}
		}
		MessageDialog.openInformation(shell, Messages.ExportPackageToDeviceAction_ExportMsgTitle, MessageFormat
				.format(Messages.ExportPackageToDeviceAction_ExportMsg, cnt, towrite.size()));

	}

}

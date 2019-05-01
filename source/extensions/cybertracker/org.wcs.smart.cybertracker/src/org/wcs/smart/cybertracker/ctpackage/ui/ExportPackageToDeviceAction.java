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

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.MobileDeviceUtils;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.model.ICtPackage;

/**
 * Exports cybertracker package to local filestore/device.
 * 
 * @author Emily
 *
 */
public class ExportPackageToDeviceAction implements ICtExportAction {

	
	@Override
	public void doAction(List<ICtPackage> ctpackages, IEclipseContext context) {
		exportLocal(ctpackages, context.get(Shell.class));
	}

	@Override
	public String getName() {
		return Messages.ExportPackageToDeviceAction_OptionName;
	}

	@Override
	public Image getIcon() {
		return null;
	}

	private void exportLocal(List<ICtPackage> towrite, Shell shell) {
		
		// export all the packages
		int cnt = 0;
		for (ICtPackage w : towrite) {
			try {
				MobileDeviceUtils.exportAppToDevice(w.getLocalFile());
				cnt++;
			} catch (Exception e) {
				CyberTrackerPlugIn.displayError(Messages.ExportPackageToDeviceAction_ErrorTitle, 
						MessageFormat.format(Messages.ExportPackageToDeviceAction_ErrorMsg + "\n\n{1}", w.getName(), e.getMessage()), e); //$NON-NLS-1$
			}
		}
		MessageDialog.openInformation(shell, Messages.ExportPackageToDeviceAction_ExportMsgTitle, MessageFormat
				.format(Messages.ExportPackageToDeviceAction_ExportMsg, cnt, towrite.size()));

	}

}

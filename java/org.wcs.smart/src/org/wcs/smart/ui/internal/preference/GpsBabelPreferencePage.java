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
package org.wcs.smart.ui.internal.preference;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.internal.Messages;

/**
 * Customized Smart preference page for modifying the gps babel install
 * location.
 * 
 * @author egouge
 * 
 */
public class GpsBabelPreferencePage extends PreferencePage implements
		IWorkbenchPreferencePage {

	public static final String ID = "org.wcs.smart.preference.GpsBabel"; //$NON-NLS-1$

	private FileFieldEditor gpsLoc;

	public GpsBabelPreferencePage() {
		super();
	}

	public GpsBabelPreferencePage(String title) {
		super(title);
	}

	public GpsBabelPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		String propValue = SmartProperties.getInstance().getProperty(
				SmartProperties.PROP_GPS_BABEL);
		if (propValue.equals(gpsLoc.getStringValue())) {
			return true;
		}

		File f = new File(gpsLoc.getStringValue());
		if (!f.exists()) {
			if (!MessageDialog.openConfirm(getShell(),
					Messages.GpsBabelPreferencePage_WarningDialogTitle,
					MessageFormat.format(
							Messages.GpsBabelPreferencePage_FileNotFound,
							new Object[] { f.toString() }))) {
				return false;
			}
		}
		// try to keep relative location of gps babel
		String newLocation = f.toString();
		try {
			newLocation = f.getCanonicalPath();
			String appLoc = new File(".").getCanonicalPath(); //$NON-NLS-1$
			if (newLocation.startsWith(appLoc)) {
				newLocation = "." + newLocation.substring(appLoc.length()); //$NON-NLS-1$
			}
			gpsLoc.setStringValue(newLocation);
		} catch (Exception ex) {
		}

		try {
			SmartProperties.getInstance().setKey(
					SmartProperties.PROP_GPS_BABEL, newLocation);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(
					Messages.GpsBabelPreferencePage_CouldNotUpdate
							+ ex.getMessage(), ex);
			return false;
		}
		return true;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		gpsLoc.setStringValue(SmartProperties.getInstance().getSystemDefaultValue(SmartProperties.PROP_GPS_BABEL));
		performApply();
		
	
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		gpsLoc = new FileFieldEditor(
				"", Messages.GpsBabelPreferencePage_ProgramLocLabel, main); //$NON-NLS-1$

		gpsLoc.setStringValue(SmartProperties.getInstance().getProperty(
				SmartProperties.PROP_GPS_BABEL));

		return main;
	}

}

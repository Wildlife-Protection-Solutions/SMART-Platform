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
package org.wcs.smart.r.ui;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.FileFieldEditor;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.osgi.service.prefs.BackingStoreException;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.attachment.AttachmentUtil;
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.internal.Messages;

/**
 * Preference page for specifying the location of the R install
 * @author Emily
 *
 */
public class RPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	public static final String ID = "org.wcs.smart.preference.r"; //$NON-NLS-1$
	
	public static final String R_PROGRAM = "RPROGRAM"; //$NON-NLS-1$

	private FileFieldEditor rLocation;

	public RPreferencePage() {
		super();
		super.setImageDescriptor(RPlugIn.getDefault().getImageRegistry().getDescriptor(RPlugIn.ICON_R));
	}

	public RPreferencePage(String title) {
		super(title, RPlugIn.getDefault().getImageRegistry().getDescriptor(RPlugIn.ICON_R));
	}


	@Override
	public void init(IWorkbench workbench) {
	}

	@Override
	public boolean performOk() {
		String propValue = getRSystemProperty();
		if (propValue.equals(rLocation.getStringValue())) {
			return true;
		}
		if(rLocation.getStringValue() == null || rLocation.getStringValue().isEmpty() ){
			try {
				setSystemProperty(Messages.RPreferencePage_0);
			} catch (BackingStoreException e) {
				RPlugIn.log(e.getMessage(), e);
			}
			return true;
		}
		
		File f = new File(rLocation.getStringValue());
		if (!f.exists()) {
			if (!MessageDialog.openConfirm(getShell(),
					Messages.RPreferencePage_WaringTitle, MessageFormat.format(Messages.RPreferencePage_NotFoundWarning,new Object[] { f.toString() }))) {
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
			rLocation.setStringValue(newLocation);
		} catch (Exception ex) {
		}

		try {
			setSystemProperty(newLocation);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(
					Messages.RPreferencePage_UpdateError +ex.getMessage(), ex);
			return false;
		}
		return true;
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		rLocation.setStringValue(getRSystemProperty());
		performApply();
	}

	@Override
	protected Control createContents(Composite parent) {
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));
		rLocation = new FileFieldEditor("", Messages.RPreferencePage_RLabel + "*", main); //$NON-NLS-1$ //$NON-NLS-2$
		rLocation.setStringValue(getRSystemProperty());
		
		Label l = new Label(main, SWT.WRAP);
		l.setText("*" + Messages.RPreferencePage_RInfo); //$NON-NLS-1$
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		((GridData)l.getLayoutData()).verticalIndent = 5;
		((GridData)l.getLayoutData()).widthHint = 150;
		
		Link lnk = new Link(main, SWT.NONE);
		lnk.setText(MessageFormat.format("{0} <a>{1}</a>", Messages.RPreferencePage_RInstall, "http://www.r-project.org")); //$NON-NLS-1$ //$NON-NLS-2$ 
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		((GridData)lnk.getLayoutData()).verticalIndent = 20;
		lnk.addListener(SWT.Selection, e->AttachmentUtil.launch("http://www.r-project.org")); //$NON-NLS-1$
		
		lnk = new Link(main, SWT.WRAP);
		lnk.setText(MessageFormat.format("{0} <a>{1}</a>{2}",Messages.RPreferencePage_Licence, "CC-BY-SA 4.0", Messages.RPreferencePage_NoMods)); //$NON-NLS-1$ //$NON-NLS-2$ 
		lnk.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		((GridData)lnk.getLayoutData()).widthHint = 150;
		((GridData)lnk.getLayoutData()).verticalIndent = 20;
		lnk.addListener(SWT.Selection, e->AttachmentUtil.launch("http://creativecommons.org/licenses/by-sa/4.0/")); //$NON-NLS-1$

		return main;
	}

	public static String getRSystemProperty(){
		return ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).get(R_PROGRAM, DefaultScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).get(R_PROGRAM, "")); //$NON-NLS-1$
	}
	
	protected void setSystemProperty(String value) throws BackingStoreException {
		ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).put(R_PROGRAM, value);
		ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).flush();
	}

}

package org.wcs.smart.r.ui;

import java.io.File;
import java.text.MessageFormat;

import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
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
import org.osgi.service.prefs.BackingStoreException;
import org.wcs.smart.SmartPlugIn;

public class RPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {
	
	public static final String ID = "org.wcs.smart.preference.r"; //$NON-NLS-1$
	
	public static final String R_PROGRAM = "RPROGRAM";

	private FileFieldEditor rLocation;

	public RPreferencePage() {
		super();
	}

	public RPreferencePage(String title) {
		super(title);
	}

	public RPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
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

		File f = new File(rLocation.getStringValue());
		if (!f.exists()) {
			if (!MessageDialog.openConfirm(getShell(),
					"Warning", MessageFormat.format("R Software not found at {0}",new Object[] { f.toString() }))) {
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
					"Could not update R Software location:" +ex.getMessage(), ex);
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
		rLocation = new FileFieldEditor("", "R Program:", main); //$NON-NLS-1$
		rLocation.setStringValue(getRSystemProperty());
		return main;
	}

	public static String getRSystemProperty(){
		return ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).get(R_PROGRAM, DefaultScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).get(R_PROGRAM, ""));
	}
	
	protected void setSystemProperty(String value) throws BackingStoreException {
		ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).put(R_PROGRAM, value);
		ConfigurationScope.INSTANCE.getNode(SmartPlugIn.PLUGIN_ID).flush();
	}

}

package org.wcs.smart.connect.report.udig;

import org.eclipse.jface.preference.IPreferenceStore;
import org.locationtech.udig.internal.ui.UiPlugin;

public class UiPluginWrapper extends UiPlugin {
	
	private IPreferenceStore localPreference;
	
	@Override
    public IPreferenceStore getPreferenceStore() {
		if (localPreference == null){
			localPreference = new UdigPreferenceStore();
		}
		return localPreference;
	}
}

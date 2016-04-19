package org.wcs.smart.connect.report.udig;

import org.eclipse.ui.preferences.ScopedPreferenceStore;
import org.locationtech.udig.project.internal.ProjectPlugin;
import org.locationtech.udig.project.preferences.PreferenceConstants;

public class ProjectPluginWrapper extends ProjectPlugin.Implementation {

	private UdigPreferenceStore localPreference;
	
	@Override
    public ScopedPreferenceStore getPreferenceStore() {
		if (localPreference == null){
			localPreference = new UdigPreferenceStore();
			localPreference.setValue(PreferenceConstants.P_TRANSPARENCY, Boolean.TRUE);
			localPreference.setValue(PreferenceConstants.P_ANTI_ALIASING, Boolean.TRUE);
		}
		return localPreference;
	}
}

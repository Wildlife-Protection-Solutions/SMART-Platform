package org.wcs.smart.connect.report.udig;

import org.eclipse.jface.preference.IPreferenceStore;
import org.locationtech.udig.catalog.internal.shp.ShpPlugin;

public class ShpPluginWrapper extends ShpPlugin{
	
	private IPreferenceStore localPreference;
	
	@Override
    public IPreferenceStore getPreferenceStore() {
		if (localPreference == null){
			localPreference = new UdigPreferenceStore();
		}
		return localPreference;
	}
}

package org.wcs.smart;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.backup.IBackupContributor;

public enum IdGeneratorManager {

	INSTANCE;
	
	public static final String EXTENSION_ID = "org.wcs.smart.idgenerator"; //$NON-NLS-1$
	
	public List<IdGeneratorContribution> getContributions(){
		
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IdGeneratorContribution> items = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				items.add((IdGeneratorContribution)e.createExecutableExtension("uicontribution")); //$NON-NLS-1$
			}
		}catch (Exception ex){
			SmartPlugIn.log("Error getting id generator extensions", ex); //$NON-NLS-1$
		}
		return items;
	}
}

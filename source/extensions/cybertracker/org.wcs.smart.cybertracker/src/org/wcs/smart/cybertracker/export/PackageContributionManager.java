package org.wcs.smart.cybertracker.export;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;

public enum PackageContributionManager {
	
	INSTANCE;
	
	public static final String EXT_ID = "org.wcs.smart.cybertracker.export.package.contribution";
	
	public List<IPackageContribution> getContributionItems(){
		List<IPackageContribution>  items = new ArrayList<>();
		
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXT_ID);
			try {
				for (IConfigurationElement e : config) {
					IPackageContribution ext = (IPackageContribution) e.createExecutableExtension("contribution"); //$NON-NLS-1$
					items.add(ext);
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog("Error loading export package contributions", ex);
			}
		}
		return items;
	}

}

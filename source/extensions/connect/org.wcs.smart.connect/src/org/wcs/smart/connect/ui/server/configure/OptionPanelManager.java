package org.wcs.smart.connect.ui.server.configure;

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.connect.ConnectPlugIn;

public class OptionPanelManager {

	public synchronized static IServerOptionsPanel[] createOptionPanels(){
		if (Platform.getExtensionRegistry() == null) return new IServerOptionsPanel[0];
		ArrayList<IServerOptionsPanel> items = new ArrayList<IServerOptionsPanel>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IServerOptionsPanel.EXTENSION_ID);
		for (IConfigurationElement e : config) {
			try{
				items.add((IServerOptionsPanel)e.createExecutableExtension("class")); //$NON-NLS-1$
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
		}
		return items.toArray(new IServerOptionsPanel[items.size()]);
	}
	
}

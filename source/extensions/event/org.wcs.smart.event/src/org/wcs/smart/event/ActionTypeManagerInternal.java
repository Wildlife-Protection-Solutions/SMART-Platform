package org.wcs.smart.event;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.event.ui.model.IActionParameterCollector;

public enum ActionTypeManagerInternal {
	
	INSTANCE;
	
	private volatile Map<String, String> collectors = null;
	
	public IActionParameterCollector createParameterCollector(IActionType type) {
		initActionParameterCollectors();
		if (collectors == null) return null;
		String className = collectors.get(type.getKey());
		if (className == null) return null;
		try {
			return (IActionParameterCollector) Class.forName(className).newInstance();
		}catch (Exception ex) {
			EventPlugIn.log(ex.getMessage(), ex);
		}
		return null;
		
	}
	
	private void initActionParameterCollectors() {
		if (collectors != null) return ;
		synchronized (INSTANCE) {
			if (collectors != null) return ;

			Map<String, String> localTypes = new HashMap<>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IActionType.EXTENSION_ID);
			for (IConfigurationElement e : config) {	
				try {
					IActionType type = (IActionType) e.createExecutableExtension("class"); //$NON-NLS-1$
					String className = e.getAttribute("parameter_collector"); //$NON-NLS-1$
					localTypes.put(type.getKey(), className);
				}catch (Exception ex) {
					EventPlugIn.log(ex.getMessage(), ex);
				}
			}
			this.collectors = localTypes;
		}
	}
}

package org.wcs.smart.event;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.event.model.IActionType;

public enum ActionTypeManager {
	
	INSTANCE;
	
	private volatile List<IActionType> types = null;
	
	public List<IActionType> getActionTypes(){
		return getActionTypesInternal();
	}
	
	public IActionType getActionType(String key) {
		for(IActionType t : getActionTypesInternal()) {
			if (t.getKey().equals(key)) return t;
		}
		return null;
	}
	
	private List<IActionType> getActionTypesInternal() {
		if (types != null) return types;
		synchronized (INSTANCE) {
			if (types != null) return types;

			List<IActionType> localTypes = new ArrayList<>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IActionType.EXTENSION_ID);
			for (IConfigurationElement e : config) {	
				try {
					IActionType type = (IActionType) e.createExecutableExtension("class"); //$NON-NLS-1$
					localTypes.add(type);
				}catch (Exception ex) {
					EventPlugIn.log(ex.getMessage(), ex);
				}
			}
			this.types = localTypes;
			return this.types;
		}
	}
}

/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.event.ui.model.IActionParameterCollector;

/**
 * Desktop specific action type manager
 * 
 * @author Emily
 *
 */
public enum ActionTypeManagerInternal {
	
	INSTANCE;
	
	private volatile Map<String, IConfigurationElement> collectors = null;
	private volatile List<IActionType> types = null;

	public IActionParameterCollector createParameterCollector(IActionType type) {
		initActionParameterCollectors();
		if (collectors == null) return null;
		IConfigurationElement config = collectors.get(type.getKey());
		if (config == null) return null;
		try {
			return (IActionParameterCollector) config.createExecutableExtension("parameter_collector"); //$NON-NLS-1$
		}catch (Exception ex) {
			EventPlugIn.log(ex.getMessage(), ex);
		}
		return null;
		
	}
	
	private void initActionParameterCollectors() {
		if (collectors != null) return ;
		synchronized (INSTANCE) {
			if (collectors != null) return ;

			Map<String, IConfigurationElement> localTypes = new HashMap<>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IActionType.EXTENSION_ID);
			for (IConfigurationElement e : config) {	
				try {
					IActionType type = (IActionType) e.createExecutableExtension("actionType"); //$NON-NLS-1$
					localTypes.put(type.getKey(), e);
				}catch (Exception ex) {
					EventPlugIn.log(ex.getMessage(), ex);
				}
			}
			this.collectors = localTypes;
		}
	}
	
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
			
			
			IExtensionRegistry registry = Platform.getExtensionRegistry();
	        IExtensionPoint extensionPoint = registry.getExtensionPoint(IActionType.EXTENSION_ID);
	        if (extensionPoint == null) return Collections.emptyList();
	            
	        IExtension[] extensions = extensionPoint.getExtensions();
			
			for (IExtension extension : extensions) {
	            IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement e : elements) {	
					try {
						IActionType type = (IActionType) e.createExecutableExtension("actionType"); //$NON-NLS-1$
						localTypes.add(type);
					}catch (Exception ex) {
						EventPlugIn.log(ex.getMessage(), ex);
					}
				}
			}
			this.types = localTypes;
			return this.types;
		}
	}
}

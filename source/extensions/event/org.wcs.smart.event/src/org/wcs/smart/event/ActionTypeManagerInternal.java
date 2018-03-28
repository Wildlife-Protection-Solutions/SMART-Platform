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

import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
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
					IActionType type = (IActionType) e.createExecutableExtension("class"); //$NON-NLS-1$
					localTypes.put(type.getKey(), e);
				}catch (Exception ex) {
					EventPlugIn.log(ex.getMessage(), ex);
				}
			}
			this.collectors = localTypes;
		}
	}
}

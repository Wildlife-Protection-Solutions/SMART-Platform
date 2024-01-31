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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.event.model.IActionType;
import org.wcs.smart.event.model.IActionTypeExecutor;

/**
 * Action type manager
 * 
 * @author Emily
 *
 */
public enum ActionExecutorManager {

	INSTANCE;
	
	private static Logger logger = Logger.getLogger( ActionExecutorManager.class.getCanonicalName());
	
	private volatile List<IActionTypeExecutor> executors = null;
	
	public List<IActionTypeExecutor> getActionTypes(){
		return getActionTypesInternal();
	}
	
	public IActionTypeExecutor getActionType(String key) {
		for(IActionTypeExecutor t : getActionTypesInternal()) {
			if (t.getKey().equals(key)) return t;
		}
		return null;
	}
	
	private List<IActionTypeExecutor> getActionTypesInternal() {
		if (executors != null) return executors;
		synchronized (INSTANCE) {
			if (executors != null) return executors;

			List<IActionTypeExecutor> localTypes = new ArrayList<>();
			
			
			IExtensionRegistry registry = Platform.getExtensionRegistry();
	        IExtensionPoint extensionPoint = registry.getExtensionPoint(IActionType.EXTENSION_ID);
	        if (extensionPoint == null) return Collections.emptyList();
	            
	        IExtension[] extensions = extensionPoint.getExtensions();
			
			for (IExtension extension : extensions) {
	            IConfigurationElement[] elements = extension.getConfigurationElements();
				for (IConfigurationElement e : elements) {	
					try {
						IActionTypeExecutor type = (IActionTypeExecutor) e.createExecutableExtension("actionExecutor"); //$NON-NLS-1$
						localTypes.add(type);
					}catch (Exception ex) {
						logger.log(Level.WARNING,ex.getMessage(),ex);
					}
				}
			}
			this.executors = localTypes;
			return this.executors;
		}
	}
}

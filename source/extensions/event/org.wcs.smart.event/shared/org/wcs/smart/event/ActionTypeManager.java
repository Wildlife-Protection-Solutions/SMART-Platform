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
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.event.model.IActionType;

/**
 * Action type manager
 * 
 * @author Emily
 *
 */
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

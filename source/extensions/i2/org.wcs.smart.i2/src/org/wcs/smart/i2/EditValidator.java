/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.i2;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;

/**
 * Implementation for extension point that allows for validation of profile, entity type
 * and record source configuration before saving.
 * 
 * @author Emily
 *
 */
public enum EditValidator {
	
	INSTANCE;
	
	private List<IEditValidator> items = null;
	
	public static final String EXTENSION_ID = "org.wcs.smart.i2.profile.edit.validation"; //$NON-NLS-1$
	
	/**
	 * 
	 * @param object can be IntelProfile, IntelRecordSource, or IntelEntityType
	 * @param session
	 * @return error message if configuration is not valid, otherwise returns null
	 */
	public String isValid(Object object, Session session) {
		loadValidators();
		for (IEditValidator item : items) {
			String x = item.isValid(object, session);
			if (x != null) return x;
		}
		return null;
	}

	private synchronized void loadValidators() {
		if (items != null) return;
		
		items = new ArrayList<>();
		if (Platform.getExtensionRegistry() != null){
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
			
			for (IConfigurationElement e : config) {
				if (e.getName().equalsIgnoreCase("validator")) { //$NON-NLS-1$
					try {
						items.add((IEditValidator)e.createExecutableExtension("class")); //$NON-NLS-1$
					}catch(Exception ex) {
						Intelligence2PlugIn.log(ex.getMessage(),  ex);
					}
				}
			}
		}
	}
}

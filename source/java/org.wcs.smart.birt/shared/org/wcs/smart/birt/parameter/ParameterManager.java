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
package org.wcs.smart.birt.parameter;

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
import org.locationtech.udig.core.internal.ExtensionPointProcessor;
import org.locationtech.udig.core.internal.ExtensionPointUtil;

/**
 * Manager for custom SMART Birt Parameter Extension point.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public enum ParameterManager {

	INSTANCE;

	public static final String EXTENSION_ID = "org.wcs.smart.birt.parameter"; //$NON-NLS-1$

	public static final String PARAMETER_ELEMENT = "parameter"; //$NON-NLS-1$
	public static final String CLASS_ATTRIBUTE = "class"; //$NON-NLS-1$
	public static final String IMAGE_ATTRIBUTE = "image"; //$NON-NLS-1$

	private List<ISmartBirtParameter> parameters = null;
	private Map<String, IConfigurationElement> configs = new HashMap<>();

	public synchronized List<ISmartBirtParameter> getParameters() {
		if (parameters != null) return parameters;
		configs.clear();
		try {
			List<ISmartBirtParameter> temp = new ArrayList<>();
			
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_ID);
			if (extensionPoint == null) return Collections.emptyList();
		            
			IExtension[] extensions = extensionPoint.getExtensions();

		        // For each extension ...
		    for( int i = 0; i < extensions.length; i++ ) {
		    	IExtension extension = extensions[i];
		        IConfigurationElement[] elements = extension.getConfigurationElements();

		        // For each member of the extension ...
		        for( int j = 0; j < elements.length; j++ ) {
		        	IConfigurationElement element = elements[j];
		        	if (element.getName().equalsIgnoreCase(PARAMETER_ELEMENT)) {
						ISmartBirtParameter p = (ISmartBirtParameter) element.createExecutableExtension(CLASS_ATTRIBUTE);
						temp.add(p);
						configs.put(p.getKey(), element);
					
					}	
		        }
		    }
		                    
		                    
		                    
//			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
//			for (IConfigurationElement e : config) {	
//				if (e.getName().equalsIgnoreCase(PARAMETER_ELEMENT)) {
//					ISmartBirtParameter p = (ISmartBirtParameter) e.createExecutableExtension(CLASS_ATTRIBUTE);
//					temp.add(p);
//					configs.put(p.getKey(), e);
//				
//				}
//			}
			parameters = temp;
			return parameters;
		}catch (Exception ex) {
			ex.printStackTrace();
			//TODO:
		}
		return Collections.emptyList();
	}

	public IConfigurationElement getConfigElement(String key) {
		if (parameters == null)
			getParameters();
		return configs.get(key);
	}

	public ISmartBirtParameter findParameter(String key) {
		for (ISmartBirtParameter i : getParameters()) {
			if (i.getKey().equalsIgnoreCase(key))
				return i;
		}
		return null;
	}
}

/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.ca;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Constants;
import org.wcs.smart.SmartPlugIn;

/**
 * 
 * @author Emily
 * @since 8.0.0
 */
public enum DataStoreManager {

	INSTANCE;
	
	public static final String FILESTORE_DIR_EXTENSION_ID = "org.wcs.smart.caFilestoreDir"; //$NON-NLS-1$
	
	
	public Map<String, String> getFilestoreDirectoriesToPlugin(){
		if (Platform.getExtensionRegistry() == null) return Collections.emptyMap();
		Map<String, String> items = new HashMap<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(FILESTORE_DIR_EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				String dir = e.getAttribute("directory"); //$NON-NLS-1$
				if (dir != null && dir.length() > 0){
					items.put(dir, Platform.getBundle(e.getContributor().getName()).getHeaders().get(Constants.BUNDLE_NAME));
				}
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}
	
	/**
	 * @return set of directory names that contain data associated with the ConservationArea that
	 * sounds be included in imports/exports
	 */
	public Set<String> getFilestoreDirectories(){
		if (Platform.getExtensionRegistry() == null) return Collections.emptySet();
		Set<String> items = new HashSet<String>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(FILESTORE_DIR_EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				String dir = e.getAttribute("directory"); //$NON-NLS-1$
				if (dir != null && dir.length() > 0){
					items.add(dir);
				}
			}
		}catch (Exception ex){
			SmartPlugIn.log(ex.getMessage(), ex);
		}
		return items;
	}
	
}

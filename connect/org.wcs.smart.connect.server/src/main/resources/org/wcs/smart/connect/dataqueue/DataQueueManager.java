/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.dataqueue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.birt.core.framework.FrameworkException;
import org.eclipse.birt.core.framework.PlatformConfig;
import org.eclipse.birt.core.framework.jar.ServiceLauncher;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;

/**
 * Manages the data types for the data queue.
 * @author Emily
 *
 */
public enum DataQueueManager {
	
	INSTANCE;
	
	private final Logger logger = Logger.getLogger(DataQueueManager.class.getName());
	
	private List<String> datatypes;
	
	
	public List<String> getDataTypes(){
		if (datatypes != null) return datatypes;
		synchronized (INSTANCE) {
			if (datatypes != null) return datatypes;
						IExtensionRegistry registry = RegistryFactory.getRegistry();
			if (registry == null) {
				//try configuring the registery
				try {
					(new ServiceLauncher( )).startup(new PlatformConfig());
				} catch (FrameworkException e1) {
					logger.log(Level.WARNING, e1.getMessage(), e1);
				}
				registry = RegistryFactory.getRegistry();
			}
			if (registry == null) {
				return Collections.emptyList();
			}

			List<String> types = new ArrayList<>();
			IExtensionPoint pnt = registry.getExtensionPoint("org.wcs.smart.connect.dataqueue.processor"); //$NON-NLS-1$
			IConfigurationElement[] config = pnt.getConfigurationElements();
			for (IConfigurationElement e : config) {
				if (e.getName().equals("datatype")) { //$NON-NLS-1$
					types.add(e.getAttribute("key").toUpperCase()); //$NON-NLS-1$
				}
			}
			this.datatypes = types;
			return datatypes;
		}
	}
}

/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.dataentry.model.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.wcs.smart.dataentry.model.xml.external.ICmXmlExtraDataExporter;

/**
 * Factory for providing all contributed information added via extension point
 * for Export/Import extra-data for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class XmlCmExtraDataContributionFactory {

	private static List<ICmXmlExtraDataExporter> contributions = null;
	
	private static volatile Object lock = new Object();
	
	public static List<ICmXmlExtraDataExporter> getExporters() throws Exception {
		if (contributions != null) return contributions;
		synchronized (lock) {
			if (contributions != null) return contributions;
			IExtensionRegistry registry = RegistryFactory.getRegistry();
			if (registry == null) {
				return Collections.emptyList();
			}

			List<ICmXmlExtraDataExporter> items = new ArrayList<ICmXmlExtraDataExporter>();
			IExtensionPoint pnt = registry.getExtensionPoint(ICmXmlExtraDataExporter.EXTENSION_ID);
			IConfigurationElement[] config = pnt.getConfigurationElements();
			for (IConfigurationElement e : config) {
				if (e.getName().equals("exporter")) { //$NON-NLS-1$
					ICmXmlExtraDataExporter contribution = (ICmXmlExtraDataExporter)e.createExecutableExtension("class"); //$NON-NLS-1$
					items.add(contribution);
				}
			}
			contributions = items;
			return contributions;
		}
	}
	
}

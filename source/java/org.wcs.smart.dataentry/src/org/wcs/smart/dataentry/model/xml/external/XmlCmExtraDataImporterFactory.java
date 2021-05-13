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
package org.wcs.smart.dataentry.model.xml.external;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

/**
 * Factory for providing all contributed information added via extension point
 * for Export/Import extra-data for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class XmlCmExtraDataImporterFactory {

	private static List<ICmXmlExtraDataImporter> contributions = null;
	
	public static List<ICmXmlExtraDataImporter> getImporters() throws Exception {
		if (contributions == null) {
			if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
			List<ICmXmlExtraDataImporter> items = new ArrayList<ICmXmlExtraDataImporter>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(ICmXmlExtraDataImporter.EXTENSION_ID);
			for (IConfigurationElement e : config) {
				if (e.getName().equals("importer")) { //$NON-NLS-1$
					ICmXmlExtraDataImporter contribution = (ICmXmlExtraDataImporter)e.createExecutableExtension("class"); //$NON-NLS-1$
					items.add(contribution);
				}
			}
			contributions = items;
			
		}
		return contributions;
	}
	
}

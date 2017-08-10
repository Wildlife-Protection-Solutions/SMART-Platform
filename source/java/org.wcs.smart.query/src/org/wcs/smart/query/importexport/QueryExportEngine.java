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
package org.wcs.smart.query.importexport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.query.model.Query;

/**
 * Support functions for exporting queries
 * @author egouge
 * @since 1.0.0
 */
public class QueryExportEngine {
	
	public static final String MAPPING_ID = "org.wcs.smart.query.exportFormat"; //$NON-NLS-1$
	
	/**
	 * Returns all valid query exporters for the given query.
	 * @param query
	 * @return
	 */
	public static final List<IQueryExporter>  getQueryExports(Query query){
		List<IQueryExporter> items = new ArrayList<IQueryExporter>();
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(MAPPING_ID);
		try {
			for (IConfigurationElement e : config) {
				IQueryExporter exporter = (IQueryExporter) e.createExecutableExtension("class"); //$NON-NLS-1$
				if (exporter.canExport(query)) {
					items.add(exporter);
				}
			}
		}catch (Exception ex){
			ex.printStackTrace();
		}
		return items;
	}
}

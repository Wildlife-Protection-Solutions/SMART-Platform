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
package org.wcs.smart.report.birt.map;

import org.hibernate.Session;
import org.locationtech.udig.project.internal.StyleBlackboard;

/**
 * Extension point for providing styles for BIRT Map Layers
 * @author Emily
 *
 */
public interface IBirtLayerStyleProvider {

	public static final String EXTENSION_ID = "org.wcs.smart.birt.map.styleprovider"; //$NON-NLS-1$
	
	/**
	 * Gets the style for a given birt layer.
	 * @param extensionId the layer dataset extension point id
	 * @param queryText the query text
	 * @param s the current session
	 * @return
	 */
	public StyleBlackboard getStyle(String extensionId, String queryText, Session s);
}

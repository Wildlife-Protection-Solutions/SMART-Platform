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

import org.locationtech.udig.project.IMap;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.impl.MapImpl;

/**
 * Map Factory for creating BIRT Maps
 * @author Emily
 *
 */
public class BirtMapFactory {

	/**
	 * Creates a new BIRT Map and initializes
	 * various map components.
	 * 
	 * @return
	 */
	public static Map createMap(){
		IMap renderedMap = new BirtMap();
		((Map)renderedMap).setLayerFactory(new BirtMapLayerFactory());
		((Map)renderedMap).setViewportModelInternal(new BirtMapViewportModelImpl());
		((Map)renderedMap).setBlackBoardInternal(ProjectFactory.eINSTANCE.createBlackboard());
		((MapImpl) renderedMap).setContextModel(new BirtMapContextModel());
		return (Map) renderedMap;
	}
}

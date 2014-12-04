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

import java.util.List;

import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.project.internal.StyleBlackboard;

import org.eclipse.birt.report.engine.api.script.IReportContext;
import org.eclipse.birt.report.model.api.DataSetHandle;

/**
 * This provides a presentation link between any BIRT datasethandle
 * and a BIRT map.  Implementations should not store any state in these
 * objects.  The objects are resued for multiple reports.
 * 
 * @author Emily
 *
 */
public interface IBirtMapLayerManager {

	/**
	 *  
	 * @param handle
	 * @return
	 */
	public boolean canAddToMap(DataSetHandle handle);
	
	/**
	 * This style blackboard represents the default style
	 * for the map layer.  Users will have the ability to change the style, but this
	 * function should always return the default style (if defined).  Can
	 * return null if not defined or defined in the georesource.
	 *  
	 * @return the style blackboard to associated with the layer or null if none defined
	 */
	public StyleBlackboard getDefaultStyle(DataSetHandle handle, IGeoResource resource);
	
	/**
	 * Creates georesources based on the dataset handle and report context
	 * 
	 * @param handle
	 * @param context may be null if no context set
	 * @return
	 * @throws Exception
	 */
	public List<IGeoResource> createLayer(DataSetHandle handle, IReportContext context) throws Exception;
}

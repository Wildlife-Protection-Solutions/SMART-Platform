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
package org.wcs.smart.cybertracker.model;

import java.text.MessageFormat;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.internal.wms.WMSGeoResourceImpl;

public enum PackageMapLayerManager {

	INSTANCE;

	public PackageMapLayer toMapLayer(IGeoResource resource) throws Exception {

		if (resource.canResolve(WMSGeoResourceImpl.class)) {

			WMSGeoResourceImpl wms = resource.resolve(WMSGeoResourceImpl.class, new NullProgressMonitor());

			IService service = wms.resolve(IService.class, new NullProgressMonitor());
			org.geotools.ows.wms.Layer layer = wms.resolve(org.geotools.ows.wms.Layer.class, new NullProgressMonitor());

			PackageMapLayer mlayer = new PackageMapLayer("wms"); //$NON-NLS-1$
			mlayer.addProperty("service", service.getIdentifier().toExternalForm()); //$NON-NLS-1$
			mlayer.addProperty("layers", layer.getName()); //$NON-NLS-1$

			return mlayer;
		}
		throw new Exception(MessageFormat.format("The resource type {0} is not supported for package map layers", //$NON-NLS-1$
				resource.getTitle()));

	}
}

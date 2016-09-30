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
package org.wcs.smart.i2.udig;

import java.awt.Color;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.util.EDataTypeUniqueEList;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.core.internal.ExtensionPointList;
import org.locationtech.udig.project.interceptor.LayerInterceptor;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectPackage;
import org.locationtech.udig.project.internal.ProjectPlugin;
import org.locationtech.udig.project.internal.impl.LayerFactoryImpl;
import org.locationtech.udig.project.internal.impl.LayerImpl;
import org.locationtech.udig.project.internal.impl.LayerResource;
import org.locationtech.udig.ui.PlatformGIS;
import org.locationtech.udig.ui.ProgressManager;
import org.locationtech.udig.ui.palette.ColourScheme;

/**
 * Extension to the layer factor that creates ContentFilterLayerImpl instead of 
 * regular layers.
 * 
 * @author Emily
 *
 */
public class ContentFilterLayerFactory extends LayerFactoryImpl {

	public ContentFilterLayerFactory(Map map) {
		super();
		setMap(map);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ContentFilterLayerImpl createLayer(IGeoResource resource) throws IOException {
		IService service = resource.service(ProgressManager.instance().get());

		if (service == null) {
			return null;
		}
		// check that the service is part of catalog... If not add
		ICatalog local = CatalogPlugin.getDefault().getLocalCatalog();
		if (local.getById(IService.class, service.getID(),
				new NullProgressMonitor()) == null) {
			local.add(resource.service(null));
		}

		ContentFilterLayerImpl layer = new ContentFilterLayerImpl();

		ID resourceID = resource.getID();
		layer.setResourceID(resourceID);

		// process the style content extension point to initially populate
		// the style blackboard with style info
		final Layer theLayer = layer;

		ICatalog localCatalog = local;
		ID layerResourceID = layer.getResourceID();
		IProgressMonitor monitor = ProgressManager.instance().get();
		List<IResolve> resolves = localCatalog.find(layerResourceID, monitor);
		if (resolves.isEmpty()) {
			// Identifier lookup is being inconsistent; this often happens when
			// code trips up over
			// converting URLs to and from Files
			throw new IOException("Could not find " + layerResourceID+ " in local catalog"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		EList resources = new EDataTypeUniqueEList(IGeoResource.class, this,
				ProjectPackage.LAYER__GEO_RESOURCES);
		LayerResource preferredResource = null;
		for (IResolve resolve : resolves) {
			if (resolve instanceof IGeoResource) {
				LayerResource layerResource = new LayerResource(
						(LayerImpl) layer, (IGeoResource) resolve);
				if (resolve.getID().equals(layerResourceID)) {
					resources.add(0, layerResource);
				} else {
					resources.add(layerResource);
				}
				if (resolve == resource) {
					preferredResource = layerResource;
				}
			}
		}
		// This is the total list of resources capable of providing information
		layer.setGeoResources(resources);

		// This is the "best" match; usually the one the user supplied
		layer.setGeoResource(preferredResource);

		// determine the default colour
		ColourScheme colourScheme = getColorScheme();
		Color colour = colourScheme.addItem(theLayer.getID().toString());
		theLayer.setDefaultColor(colour);

		runLayerCreatedInterceptor(layer);

		return layer;
	}
	
    private ColourScheme getColorScheme() {
        if (getMap() == null) {
            return ColourScheme.getDefault(PlatformGIS.getColorBrewer().getPalettes()[0]);
        }
        return getMap().getColourScheme();
    }

    private void runLayerCreatedInterceptor(Layer layer) {
        List<IConfigurationElement> list = ExtensionPointList
                .getExtensionPointList(LayerInterceptor.EXTENSION_ID);
        for (IConfigurationElement element : list) {
            if (element.getName().equals(LayerInterceptor.CREATED_ID)) {
                String attribute = element.getAttribute("name"); //$NON-NLS-1$
                try {
                    LayerInterceptor interceptor = (LayerInterceptor) element
                            .createExecutableExtension("class"); //$NON-NLS-1$
                    interceptor.run(layer);
                } catch (CoreException e) {
                    ProjectPlugin
                            .log("Error creating class: " + element.getAttribute("class") + " part of layer interceptor: " + attribute, e); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
                } catch (Throwable t) {
                    ProjectPlugin.log("Error running interceptor: " + attribute, t); //$NON-NLS-1$
                }
            }
        }
    }
}

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

import java.awt.Color;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.core.internal.ExtensionPointList;
import org.locationtech.udig.project.interceptor.LayerInterceptor;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.LayerFactory;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectPackage;

/**
 * Layer factory to produce BIRT Map Layers
 * @author Emily
 *
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class BirtMapLayerFactory extends EObjectImpl implements LayerFactory {

	public BirtMapLayerFactory(){
		super();
	}
	
    @Override
    protected EClass eStaticClass() {
        return ProjectPackage.Literals.LAYER_FACTORY;
    }
    
	@Override
	public List<Layer> getLayers(List selection) throws IOException {
		List<Layer> layers = new LinkedList<Layer>();
        for (Iterator<Object> iter = selection.iterator(); iter.hasNext();) {
            Object obj = iter.next();
            if (obj instanceof IService) {
                layers.addAll(getLayers((IService) obj));
            } else if (obj instanceof IGeoResource) {
                IGeoResource entry = (IGeoResource) obj;
                Layer ref = createLayer(entry);

                if (ref != null)
                    layers.add(ref);
            }
        }
        return layers;
	}

	@Override
	public List<Layer> getLayers(IService service) throws IOException {
		 Layer ref = null;
	        List<Layer> layers = new LinkedList<Layer>();

	        Iterator<? extends IGeoResource> rentryIter = service.resources(null).iterator();
	        while (rentryIter.hasNext()) {
	            IGeoResource entry = rentryIter.next();

	            ref = createLayer(entry);

	            if (ref != null)
	                layers.add(ref);
	        }
	        return layers;
	}

	@Override
	public Layer createLayer(IGeoResource resource) throws IOException {
			BirtMapLayer layer = new BirtMapLayer();

			ID resourceID = resource.getID();
	        layer.setResourceID(resourceID);

	        // process the style content extension point to initially populate
	        // the style blackboard with style info
	        final Layer theLayer = layer;

	        ((BirtMapLayer)layer).setGeoResources(Collections.singletonList(resource));
	        layer.setGeoResource(resource);

	        // determine the default colour
	        Color c = getMap().getColourScheme().addItem(theLayer.getID().toString());
	        theLayer.setDefaultColor(c);
	        runLayerCreatedInterceptor(layer);

	        return layer;
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
                } catch (Throwable e) {
                	Logger.getLogger(BirtMapLayer.class.getName()).log(Level.SEVERE, "Could not run layer interceptor: " + attribute, e); //$NON-NLS-1$
                }
            }
        }
    }

	 @Override
	public Map getMap() {
		if (eContainerFeatureID() != ProjectPackage.LAYER_FACTORY__MAP)
            return null;
        return (Map) eContainer();
	}

	@Override
	public void setMap(Map newMap) {
        if (newMap != eInternalContainer()
                || (eContainerFeatureID() != ProjectPackage.LAYER_FACTORY__MAP && newMap != null)) {
            if (EcoreUtil.isAncestor(this, newMap))
                throw new IllegalArgumentException(
                        "Recursive containment not allowed for " + toString()); //$NON-NLS-1$
            NotificationChain msgs = null;
            if (eInternalContainer() != null)
                msgs = eBasicRemoveFromContainer(msgs);
            if (newMap != null)
                msgs = ((InternalEObject) newMap).eInverseAdd(this,
                        ProjectPackage.MAP__LAYER_FACTORY, Map.class, msgs);
            msgs = eBasicSetContainer((InternalEObject) newMap, ProjectPackage.LAYER_FACTORY__MAP, msgs);
            if (msgs != null)
                msgs.dispatch();
        } else if (eNotificationRequired())
            eNotify(new ENotificationImpl(this, Notification.SET,
                    ProjectPackage.LAYER_FACTORY__MAP, newMap, newMap));
	}
}

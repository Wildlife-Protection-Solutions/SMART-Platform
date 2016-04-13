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

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.emf.common.notify.Adapter;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.InternalEObject;
import org.locationtech.udig.core.internal.ExtensionPointList;
import org.locationtech.udig.project.interceptor.LayerInterceptor;
import org.locationtech.udig.project.interceptor.MapInterceptor;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.impl.IEListVisitor;
import org.locationtech.udig.project.internal.impl.SynchronizedEObjectWithInverseResolvingEList;

/**
 * Layer list for BIRT maps.
 * 
 * @author Emily
 *
 */
public class BirtLayerList extends SynchronizedEObjectWithInverseResolvingEList<Layer> {

    /** long serialVersionUID field */
    private static final long serialVersionUID = 4584718175140573610L;

    private Collection<Adapter> deepAdapters = new CopyOnWriteArraySet<Adapter>();

    public BirtLayerList( Class<Layer> dataClass, InternalEObject owner, int featureID, int inverseFeatureID ) {
        super(dataClass, owner, featureID, inverseFeatureID);
    }

    @Override
    protected void didAdd( int index, Layer newObject ) {
        super.didAdd(index, newObject);
    }

    @Override
    public NotificationChain inverseAdd( Layer object, NotificationChain notifications ) {
        NotificationChain notificationChain = super.inverseAdd(object, notifications);
        runAddInterceptors(object);
        return notificationChain;
    }

    @Override
    protected Layer assign( int index, Layer object ) {
        return super.assign(index, object);
    }
 

    @Override
    protected Layer doRemove( int index ) {
        Object toRemove = get(index);
        runRemoveInterceptor(toRemove);
        return super.doRemove(index);
    }

    @Override
    protected void doClear() {
        Object[] toRemove = toArray();
        removeAllInterceptors(Arrays.asList(toRemove));
        super.doClear();
    }

    private void removeAllInterceptors( Collection<?> c ) {
        // iterating over instances of LayersList2 must be synced
        if (c instanceof BirtLayerList) {
            ((BirtLayerList) c).syncedIteration(new IEListVisitor<Layer>(){
                public void visit( final Layer element ) {
                    runLayerInterceptorAndRemove(element);
                }
            });
        } else {
            for( final Iterator<?> iter = c.iterator(); iter.hasNext(); ) {
                final Layer element = (Layer) iter.next();
                runLayerInterceptorAndRemove(element);
            }
        }
    }

    private void runAddInterceptors( Object element ) {
        Layer layer = (Layer) element;
        for( Adapter deepAdapter : deepAdapters ) {
            if (!layer.eAdapters().contains(deepAdapter))
                layer.eAdapters().add(deepAdapter);
        }
        runLayerInterceptor(layer, LayerInterceptor.ADDED_ID);
    }

    private void runRemoveInterceptor( Object remove ) {
        if (remove == null || !contains(remove))
            return;
        Layer layer = (Layer) remove;
        runLayerInterceptor(layer, LayerInterceptor.REMOVED_ID);
        (layer).eAdapters().removeAll(deepAdapters);
    }

    private void runLayerInterceptor( Layer layer, String configurationName ) {
        runNonDeprecatedInterceptors(layer, configurationName);
        runDeprecatedInterceptors(layer, configurationName);
    }

    private void runNonDeprecatedInterceptors( Layer layer, String configurationName ) {
        List<IConfigurationElement> list = ExtensionPointList
                .getExtensionPointList(LayerInterceptor.EXTENSION_ID);
        for( IConfigurationElement element : list ) {
            if (element.getName().equals(configurationName)) {
                String attribute = element.getAttribute("name"); //$NON-NLS-1$
                try {
                    LayerInterceptor interceptor = (LayerInterceptor) element
                            .createExecutableExtension("class"); //$NON-NLS-1$
                    interceptor.run(layer);
                } catch (CoreException e) {
                	Logger.getLogger(BirtLayerList.class.getName()).log(Level.FINE, "Error creating class: " + element.getAttribute("class") + " part of the layer interceptor: " + attribute,  e); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                } catch (Throwable t) {
                	Logger.getLogger(BirtLayerList.class.getName()).log(Level.SEVERE, "Error running interceptor: " + attribute,  t); //$NON-NLS-1$
                }
            }
        }
    }

    private void runDeprecatedInterceptors( Layer layer, String configurationName ) {
        List<IConfigurationElement> interceptors = ExtensionPointList
                .getExtensionPointList(MapInterceptor.MAP_INTERCEPTOR_EXTENSIONPOINT);
        for( IConfigurationElement element : interceptors ) {
            if (!configurationName.equals(element.getName())) {
                continue;
            }
            try {
                LayerInterceptor interceptor = (LayerInterceptor) element
                        .createExecutableExtension("class"); //$NON-NLS-1$
                interceptor.run(layer);
            } catch (Throwable e) {
            	Logger.getLogger(BirtLayerList.class.getName()).log(Level.SEVERE, "Error running interceptors",  e); //$NON-NLS-1$
            }
        }
    }

    private void runLayerInterceptorAndRemove( final Layer element ) {
        runLayerInterceptor(element, "layerRemoved"); //$NON-NLS-1$
        element.eAdapters().removeAll(deepAdapters);
    }

}
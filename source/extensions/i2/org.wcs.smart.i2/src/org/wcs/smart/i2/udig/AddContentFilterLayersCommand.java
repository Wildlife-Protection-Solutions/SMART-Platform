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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.command.AbstractCommand;
import org.locationtech.udig.project.command.UndoableCommand;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectPlugin;
import org.opengis.filter.Filter;

/**
 * An alternative to the add layers command that creates ContentFilterLayerImpl instead of regular
 * layers so we can filter the content of the layer shown on the map.
 * 
 */
public class AddContentFilterLayersCommand extends AbstractCommand implements UndoableCommand{
    protected Collection<?> resources;
    protected List<Layer> layers;
    private int index;
    private Filter initialFilter;

    /**
     * Creates layers from the set of resources
     *  
     */
    public AddContentFilterLayersCommand( Collection<?> resources ) {
        this(resources, -1);
    }

    /**
     * Creates layers from the set of resources with the default initial filter
     *  
     */
    public AddContentFilterLayersCommand( Collection<?> resources, Filter initialFilter ) {
        this(resources, -1, initialFilter);

    }

    /**
     * Creates layers from the set of resources, placing at the given index in the layer list
     *  
     */
    public AddContentFilterLayersCommand( Collection<?> resources, int i ) {
    	this(resources, i, null);
    }
    
    /**
     * Creates layers from the set of resources using the initial filter and placing at the given index in the layer list
     *  
     */
    public AddContentFilterLayersCommand( Collection<?> resources, int i, Filter initialFilter  ) {
        this.resources = resources;
        index = i;
        this.initialFilter = initialFilter;
    }
    
    public void run( IProgressMonitor monitor ) throws Exception {
        Map map = getMap();
        if (layers == null) {
            layers = new ArrayList<Layer>();

            for( Object o : resources ) {
                try {
                    Layer layer = null;
                    if (o instanceof IGeoResource) {
                        // ensure that the service is part of the Catalog so that the find method in
                        // layer turn into layer
                        IGeoResource resource = (IGeoResource) o;
                        layer = (new ContentFilterLayerFactory(getMap())).createLayer(resource);
                    }
                    if (o instanceof Layer) {
                        // leave as is; may not be a ContentFilterLayerImpl but add anyways
                        layer = (Layer) o;
                    }
                    if (layer != null) {
                    	if (layer instanceof ContentFilterLayerImpl){
                    		((ContentFilterLayerImpl) layer).setContentFilter(initialFilter);
                    	}
                        layers.add(layer);
                    }
                }
                catch (Throwable t){
                    ProjectPlugin.log("Unable to add "+o,t); //$NON-NLS-1$
                }
            }
        }
        if (!layers.isEmpty()) {
            if (index < 0 || index > map.getLayersInternal().size()) {
                index = map.getLayersInternal().size();
            }
            map.getLayersInternal().addAll(index, layers);
            map.getEditManagerInternal().setSelectedLayer(layers.get(0));
        }

    }

    public String getName() {
        return "Add Filtering Layers";
    }

    public List<Layer> getLayers() {
        return layers;
    }

	@Override
	public void rollback(IProgressMonitor monitor) throws Exception {
		//cannot undo - this just removes the annoying dialog that prompts to ensure you want to do the action
	}

}

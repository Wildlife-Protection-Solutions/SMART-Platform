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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IProgressMonitor;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.command.AbstractCommand;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.LayerFactory;

/**
 * A simple command to all layers to a map.
 * 
 * @author Emily
 */
public class AddLayersCommand extends AbstractCommand {

	/** List of resources being turned into layers. */
	private Collection<?> resources;

	/** List of created layers */
	private List<Layer> layers;

	private int index;
	
	/**
	 * Creates a the command from a set of resources
	 * 
	 * @see Layer or
	 * @see IGeoResource objects.
	 * @param resources
	 *            A list containing a combination of layers or resources.
	 */
	public AddLayersCommand(Collection<?> resources, int index) {
		this.resources = resources;
		this.index = index;
	}

	public void run(IProgressMonitor monitor) throws Exception {
		if (layers == null) {
			layers = new ArrayList<Layer>();
			LayerFactory layerFactory = getMap().getLayerFactory();
			for (Object o : resources) {
				try {
					Layer layer = null;
					if (o instanceof IGeoResource) {
						IGeoResource resource = (IGeoResource) o;
						layer = layerFactory.createLayer(resource);
					} else if (o instanceof Layer) {
						layer = (Layer) o;
					}
					if (layer != null) {
						layers.add(layer);
					}
				} catch (Throwable t) {
					Logger.getLogger(AddLayersCommand.class.getName()).log(
							Level.WARNING,
							"Unable to add layer: " + o.toString(), t); //$NON-NLS-1$
				}
			}
		}
		if (!layers.isEmpty()) {
			getMap().getLayersInternal().addAll(index, layers);
		}

	}

	public String getName() {
		return "Add map layers"; //$NON-NLS-1$
	}

	/**
	 * 
	 * @return the added layers
	 */
	public List<Layer> getLayers() {
		return layers;
	}

}

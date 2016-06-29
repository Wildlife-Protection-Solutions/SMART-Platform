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
package org.wcs.smart.observation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.observation.model.IWaypointSource;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.model.IWaypointSourceUiProvider;
/**
 * Manager for dealing with waypoint
 * sources.
 * 
 * @author Emily
 *
 */
public enum WaypointSourceEngine implements IWaypointSourceEngine{
	
	INSTANCE;
	
	private static String SOURCE_EXTENSION_ID = "org.wcs.smart.observation.ObservationSource"; //$NON-NLS-1$
	
	/**
	 * Cached sources
	 */
	private Map<String,IWaypointSource> supportedSources = null;
	/**
	 * Cached sources
	 */
	private Map<String,IWaypointSourceUiProvider> supportedUiSources = null;
	
	/**
	 * private constructor
	 */
	private WaypointSourceEngine(){
		
	}
	
	/**
	 * return set of all supported sources
	 * 
	 * @return
	 */
	public Collection<IWaypointSource> getSupportedSources(){
		if (supportedSources == null){
			loadWaypointSources();
		}
		return supportedSources.values();
	}
	/**
	 * Get the waypoint source for the given key
	 * @param sourceKey
	 * @return
	 */
	public IWaypointSource getSource(String sourceKey){
		if (supportedSources == null){
			loadWaypointSources();
		}
		return supportedSources.get(sourceKey);
	}
	
	/**
	 * Gets the ui provider for a given source key
	 * @param sourceKey
	 * @return
	 */
	public IWaypointSourceUiProvider findUiProvider(String sourceKey){
		IWaypointSource source = getSource(sourceKey);
		if (source == null) return null;
		String key = getSource(sourceKey).getKey();
		if (key == null) return null;
		return supportedUiSources.get(key);
	}
	
	/*
	 * Load source extension points
	 */
	private void loadWaypointSources() {
		supportedSources = new HashMap<String, IWaypointSource>();
		supportedUiSources = new HashMap<String, IWaypointSourceUiProvider>();
		IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(SOURCE_EXTENSION_ID);
		for (IConfigurationElement element : elements){
			try{
				IWaypointSource source = (IWaypointSource) element.createExecutableExtension("class"); //$NON-NLS-1$
				supportedSources.put(source.getKey(), source);
				
				if (element.getAttribute("ui_provider") != null){ //$NON-NLS-1$
					supportedUiSources.put(source.getKey(), (IWaypointSourceUiProvider)element.createExecutableExtension("ui_provider")); //$NON-NLS-1$
				}
			}catch (Exception ex){
				ObservationPlugIn.log("Error loading all waypoint sources", ex); //$NON-NLS-1$
			}
		}
	}
	
	
}

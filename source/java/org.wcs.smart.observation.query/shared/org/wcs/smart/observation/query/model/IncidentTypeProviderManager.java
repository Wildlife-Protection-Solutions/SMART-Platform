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
package org.wcs.smart.observation.query.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.RegistryFactory;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;

/**
 * Manager for supporting getting incident types for queries module.
 * @since 8.1.0
 */
public enum IncidentTypeProviderManager{
	
	INSTANCE;

	private static String SOURCE_EXTENSION_ID = "org.wcs.smart.observation.query.IncidentTypeProvider"; //$NON-NLS-1$

	/**
	 * Cached providers
	 */
	private Set<IIncidentTypeProvider> providers = null;


	/**
	 * return set of all supported sources
	 * 
	 * @return
	 */
	public Collection<IIncidentTypeProvider> getProviders(){
		if (providers == null){
			loadProviders();
		}
		return providers;
	}
	
	/**
	 * 
	 * @return true if incident providers exist
	 */
	public boolean hasProviders() {
		return getProviders().size() > 0;
	}
	
	/**
	 * Get all incidents types associated with the given conservation
	 * areas. The assumption is types with the same key are the same type.
	 * 
	 * @param session
	 * @param cas
	 * @return
	 */
	public List<QueryIncidentType> getTypes(Session session, Collection<ConservationArea> cas){
		List<QueryIncidentType> types = new ArrayList<>();
		for (IIncidentTypeProvider provider : getProviders()) {
			types.addAll(provider.getTypes(session, cas));
		}
		return types;
	}
	
	/**
	 * Find the incident type with the given key.
	 * @param keyId
	 * @param session
	 * @param cas
	 * @return
	 */
	public QueryIncidentType getType(String keyId, Session session, Collection<ConservationArea> cas){
		for (IIncidentTypeProvider provider : getProviders()) {
			for (QueryIncidentType type : provider.getTypes(session, cas)) {
				if (type.getKey().equalsIgnoreCase(keyId)) return type;
			}
		}
		return null;
	}
	
	/*
	 * Load source extension points
	 */
	private synchronized void loadProviders() {
		if (providers != null) return;
		
		Set<IIncidentTypeProvider> items = new HashSet<>();
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint pnt = registry.getExtensionPoint(SOURCE_EXTENSION_ID);
		IConfigurationElement[] elements = pnt.getConfigurationElements();
		for (IConfigurationElement element : elements){
			try{
				if (element.getName().equals("provider")) { //$NON-NLS-1$
					IIncidentTypeProvider source = (IIncidentTypeProvider) element.createExecutableExtension("provider"); //$NON-NLS-1$
					items.add(source);
				}
			}catch (Exception ex){
				Logger.getLogger(getClass().getName()).log(Level.WARNING, "Error loading all incident type providers", ex); //$NON-NLS-1$
			}
		}
		this.providers = items;
	}	

}
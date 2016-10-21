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
package org.wcs.smart.hibernate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Hibernate integrator that looks for extensions and applys them.
 * 
 * @author Emily
 *
 */
public class SmartIntegrator implements Integrator {
	
	public static final String EXTENSION_ID = "org.wcs.smart.hibernate.interceptor";
	
	//http://in.relation.to/2012/01/09/event-listener-registration/
	
	@Override
	public void integrate(Configuration configuration,
			SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {

		//find all contributions and run integrate
		for (Integrator i : getMappings()){
			i.integrate(configuration, sessionFactory, serviceRegistry);
		}
	}

    /** 
     * Ignore this form!  Just do nothing in impl.  It uses the new metamodel api slated for completion in 5.0
     */
	@Override
	public void integrate(MetadataImplementor metadata,
			SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		for (Integrator i : getMappings()){
			i.integrate(metadata, sessionFactory, serviceRegistry);
		}
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		for (Integrator i : getMappings()){
			i.disintegrate(sessionFactory, serviceRegistry);
		}
	}
	/**
	 * @return gets all hibernate mappings
	 */
	private static final List<Integrator>  getMappings(){
		List<Integrator> items = new ArrayList<Integrator>();
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				if (e.getName().equalsIgnoreCase("integrator")){	
					items.add((Integrator)e.createExecutableExtension("class")); //$NON-NLS-1$
				}
			}
		}catch (Exception ex){
			//todo: log this
			ex.printStackTrace();
		}
		return items;
	}
	
}

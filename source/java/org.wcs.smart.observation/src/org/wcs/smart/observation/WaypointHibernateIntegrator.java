/*
 * Copyright (C) 2016 Wildlife Conservation Society
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

import org.hibernate.boot.Metadata;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;

/**
 * Hibernate integrator for intelligence modified dates and last
 *  modified date.
 *  
 * @author Emily
 *
 */
public class WaypointHibernateIntegrator implements Integrator {

	//http://in.relation.to/2012/01/09/event-listener-registration/
	@Override
	public void integrate(Metadata metadata, SessionFactoryImplementor sessionFactory, SessionFactoryServiceRegistry serviceRegistry) {
		// As you might expect, an EventListenerRegistry is the place with which event listeners are registered  It is a service
        // so we look it up using the service registry
        final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

        //2) This form adds the specified listener(s) to the beginning of the listener chain
        WaypointHibernateListener listener = new WaypointHibernateListener();
        eventListenerRegistry.prependListeners( EventType.PRE_INSERT, listener );
        eventListenerRegistry.prependListeners( EventType.PRE_UPDATE, listener );
	}
	
	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {	
	}
	
}

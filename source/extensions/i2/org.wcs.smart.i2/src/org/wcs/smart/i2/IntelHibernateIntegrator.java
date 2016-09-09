package org.wcs.smart.i2;

import org.hibernate.cfg.Configuration;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.hibernate.integrator.spi.Integrator;
import org.hibernate.metamodel.source.MetadataImplementor;
import org.hibernate.service.spi.SessionFactoryServiceRegistry;
import org.wcs.smart.i2.events.IntelHibernateListener;

public class IntelHibernateIntegrator implements Integrator {

	//http://in.relation.to/2012/01/09/event-listener-registration/
	
	@Override
	public void integrate(Configuration configuration,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		System.out.println("LOADED FILES");
		// As you might expect, an EventListenerRegistry is the place with which event listeners are registered  It is a service
        // so we look it up using the service registry
        final EventListenerRegistry eventListenerRegistry = serviceRegistry.getService( EventListenerRegistry.class );

        //2) This form adds the specified listener(s) to the beginning of the listener chain
        IntelHibernateListener listener = new IntelHibernateListener();
        eventListenerRegistry.prependListeners( EventType.PRE_INSERT, listener );
        eventListenerRegistry.prependListeners( EventType.PRE_UPDATE, listener );
	}

    /** 
     * Ignore this form!  Just do nothing in impl.  It uses the new metamodel api slated for completion in 5.0
     */
	@Override
	public void integrate(MetadataImplementor metadata,
			SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disintegrate(SessionFactoryImplementor sessionFactory,
			SessionFactoryServiceRegistry serviceRegistry) {
		// TODO Auto-generated method stub
		
	}

	
}

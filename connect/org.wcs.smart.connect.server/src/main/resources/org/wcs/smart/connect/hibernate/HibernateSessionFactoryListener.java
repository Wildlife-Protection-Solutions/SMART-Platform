/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.hibernate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.AlertType;	
import org.wcs.smart.connect.model.ConnectUuidItem;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.StyleConfiguration;
import org.wcs.smart.connect.model.UploadItem;

/**
 * Web listener to configure the hibernate connection on start up and shut down.
 * @author Emily
 *
 */
@WebListener
public class HibernateSessionFactoryListener implements ServletContextListener{

	private final Logger logger = Logger.getLogger(HibernateSessionFactoryListener.class.getName());
	
	public static final String EXECUTOR_KEY = "threadExecutor"; //$NON-NLS-1$
	
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		SessionFactory sessionFactory = (SessionFactory)sce.getServletContext().getAttribute(HibernateManager.CONTEXT_KEY);
		if(sessionFactory != null && !sessionFactory.isClosed()){
            logger.info("Terminating Hibernate sessionFactory"); //$NON-NLS-1$
            sessionFactory.close();
        }
		
		//shutdown job pool
		ExecutorService scheduler = (ExecutorService)sce.getServletContext().getAttribute(EXECUTOR_KEY);
		if (scheduler != null){
			scheduler.shutdown();
		}
	}

	@Override
	public void contextInitialized(ServletContextEvent sce) {
		
		logger.info("Configuring Hibernate SessionFactory"); //$NON-NLS-1$
		Configuration config = new Configuration();
		
		// Perform some test to verify that the current database is Postgres.
	    // Replace the metadata provider with our custom metadata provider.
	    MetadataProviderInjector reflectionManager = (MetadataProviderInjector)config.getReflectionManager();
	    reflectionManager.setMetadataProvider(new UUIDTypeInsertingMetadataProvider(reflectionManager.getMetadataProvider()));
		
		config.configure("org/wcs/smart/connect/hibernate/hibernate.cfg.xml"); //$NON-NLS-1$
		
		//Add you annotated model classes here
		config.addAnnotatedClass(SmartUser.class);
		config.addAnnotatedClass(ConnectUuidItem.class);
		config.addAnnotatedClass(SmartUserAction.class);
		config.addAnnotatedClass(ConservationAreaInfo.class);
		config.addAnnotatedClass(UploadItem.class);
		config.addAnnotatedClass(StyleConfiguration.class);
		config.addAnnotatedClass(AlertType.class);
		config.addAnnotatedClass(Alert.class);

		for(Class<?> c : SmartHibernateConfiguration.INSTANCE.getTables()){
			config.addAnnotatedClass(c);
		}

		ServiceRegistry service = new ServiceRegistryBuilder().applySettings(config.getProperties()).buildServiceRegistry();
		SessionFactory sf = config.buildSessionFactory(service);
		
		sce.getServletContext().setAttribute(HibernateManager.CONTEXT_KEY, sf);
		
		logger.info("Hibernate SessionFactory Configured successfully"); //$NON-NLS-1$
		
		//start executor for running background jobs
		ExecutorService scheduler = Executors.newSingleThreadExecutor();
		sce.getServletContext().setAttribute(EXECUTOR_KEY, scheduler);
	}

}

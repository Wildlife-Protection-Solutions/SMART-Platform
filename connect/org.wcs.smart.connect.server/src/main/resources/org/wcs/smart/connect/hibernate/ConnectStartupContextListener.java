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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.wcs.smart.connect.apache.CleanUpJob;
import org.wcs.smart.connect.apache.EnvironmentVariables;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.AlertFilterDefault;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.CaPluginVersion;
import org.wcs.smart.connect.model.ConnectPluginVersion;
import org.wcs.smart.connect.model.ConnectUuidItem;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.MapLayer;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.SmartRole;
import org.wcs.smart.connect.model.SmartRoleAction;
import org.wcs.smart.connect.model.SmartUserRole;
import org.wcs.smart.connect.model.StyleConfiguration;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.report.BirtEngine;

/**
 * Web listener to configure the hibernate connection on start up and shut down.
 * @author Emily
 *
 */
@WebListener
public class ConnectStartupContextListener implements ServletContextListener{

	private final Logger logger = Logger.getLogger(ConnectStartupContextListener.class.getName());
	
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
		
		BirtEngine.destroyBirtEngine();
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
		config.addAnnotatedClass(WorkItem.class);
		config.addAnnotatedClass(StyleConfiguration.class);
		config.addAnnotatedClass(AlertType.class);
		config.addAnnotatedClass(Alert.class);
		config.addAnnotatedClass(AlertFilterDefault.class);
		config.addAnnotatedClass(MapLayer.class);
		config.addAnnotatedClass(CaPluginVersion.class);
		config.addAnnotatedClass(ConnectPluginVersion.class);
		config.addAnnotatedClass(SmartRole.class);
		config.addAnnotatedClass(SmartRoleAction.class);
		config.addAnnotatedClass(SmartUserRole.class);
		config.addAnnotatedClass(ServerDataQueueItem.class);

		try{
			for(Class<?> c : SmartHibernateConfiguration.INSTANCE.getTables()){
				config.addAnnotatedClass(c);
			}
		}catch (Exception ex){
			throw new RuntimeException("Could not read hibernate class files.", ex); //$NON-NLS-1$
		}

		ServiceRegistry service = new ServiceRegistryBuilder().applySettings(config.getProperties()).buildServiceRegistry();
		SessionFactory sf = config.buildSessionFactory(service);
		
		sce.getServletContext().setAttribute(HibernateManager.CONTEXT_KEY, sf);
		
		logger.info("Hibernate SessionFactory Configured successfully"); //$NON-NLS-1$
		
		//start executor for running background jobs
		int numthreads = 1;
		try{
			numthreads = (Integer) EnvironmentVariables.INSTANCE.getEnvironmentVariable(EnvironmentVariables.Variable.NUM_BACK_THREADS);
		}catch(Exception ex){
			logger.log(Level.WARNING, "Could not read variable " + EnvironmentVariables.Variable.NUM_BACK_THREADS.key + " from context.xml.", ex); //$NON-NLS-1$ //$NON-NLS-2$
		}
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(numthreads);
		sce.getServletContext().setAttribute(EXECUTOR_KEY, scheduler);
		
		initCleanUpJob(sf, scheduler);
	}
	
	private void initCleanUpJob(SessionFactory sf, ScheduledExecutorService scheduler){
		Integer cleanUpSchedule = 24;
		try{
			cleanUpSchedule = (Integer)EnvironmentVariables.INSTANCE.getEnvironmentVariable(EnvironmentVariables.Variable.CLEANUP_TASK_INTERVAL);
		}catch (Exception ex){
			logger.log(Level.WARNING, "Value not found for environment variable:" + EnvironmentVariables.Variable.CLEANUP_TASK_INTERVAL.key, ex); //$NON-NLS-1$
		}
		if (cleanUpSchedule >= 0){
			CleanUpJob job = new CleanUpJob(sf);
			scheduler.scheduleWithFixedDelay(job, 0, cleanUpSchedule, TimeUnit.HOURS);
		}
	}

}

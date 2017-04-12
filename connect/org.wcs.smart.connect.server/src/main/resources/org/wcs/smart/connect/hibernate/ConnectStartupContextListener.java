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

import java.beans.Introspector;
import java.lang.reflect.Method;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.spi.IIORegistry;
import javax.imageio.spi.IIOServiceProvider;
import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.RegistryMode;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.logging.LogFactory;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataStoreFinder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.AbstractAuthorityFactory;
import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.opengis.referencing.AuthorityFactory;
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
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.model.SmartRole;
import org.wcs.smart.connect.model.SmartRoleAction;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
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
		sce.getServletContext().setAttribute(HibernateManager.CONTEXT_KEY, null);
		
		//shutdown job pool
		ExecutorService scheduler = (ExecutorService)sce.getServletContext().getAttribute(EXECUTOR_KEY);
		if (scheduler != null){
			try{
				scheduler.shutdown();
			}finally{
				try{
					scheduler.shutdownNow();
				}finally{}
			}
		}
		sce.getServletContext().setAttribute(EXECUTOR_KEY, null);
		
		//clean up BIRT
		BirtEngine.destroyBirtEngine();
		
		
		//destroy geotools stuff
		
		//jdbc drivers
		ClassLoader webappClassLoader = getClass().getClassLoader();
		Enumeration<Driver> drivers = DriverManager.getDrivers();
		Set<Driver> driversToUnload = new HashSet<>();
		while (drivers.hasMoreElements()) {
			Driver driver = drivers.nextElement();
			try {
				// the driver class loader can be null if the driver comes from the JDK, such as the sun.jdbc.odbc.JdbcOdbcDriver
				ClassLoader driverClassLoader = driver.getClass().getClassLoader();
				if (driverClassLoader != null && webappClassLoader.equals(driverClassLoader)) {
					driversToUnload.add(driver);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		for (Driver driver : driversToUnload) {
			try {
				DriverManager.deregisterDriver(driver);
				logger.info("Unregistered JDBC driver " + driver); //$NON-NLS-1$
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Could now unload driver " + driver.getClass(), e); //$NON-NLS-1$
			}
		}
		try {
			Class<?> h2Driver = Class.forName("org.h2.Driver"); //$NON-NLS-1$
			Method m = h2Driver.getMethod("unload"); //$NON-NLS-1$
			m.invoke(null);
		} catch (Exception e) {
			logger.log(Level.WARNING, "Failed to unload the H2 driver", e); //$NON-NLS-1$
		}
        
		
		org.geotools.referencing.wkt.Formattable.cleanupThreadLocals(); 
		
		disposeAuthorityFactories(ReferencingFactoryFinder.getCoordinateOperationAuthorityFactories(null));
		disposeAuthorityFactories(ReferencingFactoryFinder.getCRSAuthorityFactories(null));
		disposeAuthorityFactories(ReferencingFactoryFinder.getCSAuthorityFactories(null));
		
		WeakCollectionCleaner.DEFAULT.exit();
        DeferredAuthorityFactory.exit();
        CRS.cleanupThreadLocals();
        CRS.reset("all"); //$NON-NLS-1$
        ReferencingFactoryFinder.reset();
        CommonFactoryFinder.reset();
        DataStoreFinder.reset();
        DataAccessFinder.reset();
        DefaultMathTransformFactory.cleanupThreadLocals();
        
        
        Object o = Hints.getSystemDefault(Hints.EXECUTOR_SERVICE);
		if (o != null && o instanceof ThreadPoolExecutor) {
			final ThreadPoolExecutor executor = (ThreadPoolExecutor) o;
			try {
				executor.shutdown();
			} finally {
				try {
					executor.shutdownNow();
				} finally {
				}
			}
		}
		
		//IMAGE IO STUFF
		// unload everything that JAI ImageIO can still refer to
        // We need to store them and unregister later to avoid concurrent modification exceptions
		final IIORegistry ioRegistry = IIORegistry.getDefaultInstance();
		Set<IIOServiceProvider> providersToUnload = new HashSet<>();
		for (Iterator<Class<?>> cats = ioRegistry.getCategories(); cats.hasNext();) {
			Class<?> category = cats.next();
			for (Iterator<?> it = ioRegistry.getServiceProviders(category,false); it.hasNext();) {
				final IIOServiceProvider provider = (IIOServiceProvider) it.next();
				if (webappClassLoader.equals(provider.getClass().getClassLoader())) {
					providersToUnload.add(provider);
				}
			}
		}
        for (IIOServiceProvider provider : providersToUnload) {
            ioRegistry.deregisterServiceProvider(provider);
        }
        
        // unload everything that JAI can still refer to
		final OperationRegistry opRegistry = JAI.getDefaultInstance()
				.getOperationRegistry();
		for (String mode : RegistryMode.getModeNames()) {
			for (Iterator<?> descriptors = opRegistry.getDescriptors(mode).iterator(); descriptors != null && descriptors.hasNext();) {
				RegistryElementDescriptor red = (RegistryElementDescriptor) descriptors.next();
				int factoryCount = 0;
				int unregisteredCount = 0;
				// look for all the factories for that operation
				for (Iterator<?> factories = opRegistry.getFactoryIterator(
						mode, red.getName()); factories != null
						&& factories.hasNext();) {
					Object factory = factories.next();
					if (factory == null) {
						continue;
					}
                    factoryCount++;
                    
					if (webappClassLoader.equals(factory.getClass().getClassLoader())) {
						boolean unregistered = false;
						// we need to scan against all "products" to unregister
						// the factory
						Vector<?> orderedProductList = opRegistry.getOrderedProductList(mode, red.getName());
						if (orderedProductList != null) {
							for (Iterator<?> products = orderedProductList.iterator(); products != null && products.hasNext();) {
								String product = (String) products.next();
								try {
									opRegistry.unregisterFactory(mode, red.getName(), product, factory);
								} catch (Throwable t) {
									// may fail due to the factory not being
									// registered against that product
								}
							}
						}
						if (unregistered) {
							unregisteredCount++;
						}

					}
				}
                
				// if all the factories were unregistered, get rid of the descriptor as well
				if (factoryCount > 0 && unregisteredCount == factoryCount) {
					opRegistry.unregisterDescriptor(red);
				}
            }
        }
        
        // flush all javabean introspection caches as this too can keep a webapp classloader from being unloaded
        Introspector.flushCaches();
        
        LogFactory.release(Thread.currentThread().getContextClassLoader());
		
		// GeoTools/GeoServer have a lot of finalizers and until they are run the JVM
        // itself wil keepup the class loader...
        try {
            System.gc();
            System.runFinalization();
        }catch(Throwable t){
        	t.printStackTrace();
        }
	}

	private void disposeAuthorityFactories(Set<? extends AuthorityFactory> factories){
		if (factories == null) return;
		try{
			for (AuthorityFactory af : factories) {
				if(af instanceof AbstractAuthorityFactory) {
					((AbstractAuthorityFactory) af).dispose();
				}
			}
		}catch(Throwable e){
			logger.log(Level.WARNING, "Error occurred trying to dispose authority factories", e); //$NON-NLS-1$
		}
	}
	
	@Override
	public void contextInitialized(ServletContextEvent sce) {
		System.setProperty("org.eclipse.emf.common.util.ReferenceClearingQueue", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		System.setProperty("org.geotools.referencing.forceXY", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		
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
		config.addAnnotatedClass(SharedLink.class);
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

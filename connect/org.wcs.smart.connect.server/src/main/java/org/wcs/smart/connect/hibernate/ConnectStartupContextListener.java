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
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLStreamHandlerFactory;
import java.nio.charset.StandardCharsets;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
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
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.apache.commons.logging.LogFactory;
import org.eclipse.birt.core.framework.FrameworkException;
import org.eclipse.birt.core.framework.PlatformConfig;
import org.eclipse.birt.core.framework.jar.ServiceLauncher;
import org.geotools.data.DataAccessFinder;
import org.geotools.data.DataStoreFinder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.factory.AbstractAuthorityFactory;
import org.geotools.referencing.factory.DeferredAuthorityFactory;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.util.WeakCollectionCleaner;
import org.geotools.util.factory.Hints;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.locationtech.udig.catalog.internal.shp.ShpServiceExtension;
import org.opengis.referencing.AuthorityFactory;
import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.asset.query.model.IAssetQueryColumnProvider;
import org.wcs.smart.connect.apache.CleanUpJob;
import org.wcs.smart.connect.apache.EnvironmentVariables;
import org.wcs.smart.connect.dataqueue.ServerDataQueueItem;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.hibernate.listeners.SmartHibernateIntegrator;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.i18n.labels.AdvancedLabelProviderImpl;
import org.wcs.smart.connect.i18n.labels.AssetLabelProvider;
import org.wcs.smart.connect.i18n.labels.AssetQueryLabelProvider;
import org.wcs.smart.connect.i18n.labels.CyberTrackerLabelProvider;
import org.wcs.smart.connect.i18n.labels.ErLabelProvider;
import org.wcs.smart.connect.i18n.labels.GridQueryColumnLabelProvider;
import org.wcs.smart.connect.i18n.labels.IncidentCyberTrackerLabelProvider;
import org.wcs.smart.connect.i18n.labels.IncidentLabelProvider;
import org.wcs.smart.connect.i18n.labels.ObservationLabelProvider;
import org.wcs.smart.connect.i18n.labels.ObservationQueryLabelProvider;
import org.wcs.smart.connect.i18n.labels.PatrolCyberTrackerLabelProvider;
import org.wcs.smart.connect.i18n.labels.PatrolLabelProvider;
import org.wcs.smart.connect.i18n.labels.PatrolQueryLabelProvider;
import org.wcs.smart.connect.i18n.labels.PlanLabelProvider;
import org.wcs.smart.connect.i18n.labels.ProfileEventLabelProvider;
import org.wcs.smart.connect.i18n.labels.QueryDateLabelProvider;
import org.wcs.smart.connect.i18n.labels.SmartCollectLabelProvider;
import org.wcs.smart.connect.i18n.labels.SmartLabelProvider;
import org.wcs.smart.connect.i18n.labels.SurveyCyberTrackerLabelProvider;
import org.wcs.smart.connect.i18n.labels.SurveyQueryLabelProvider;
import org.wcs.smart.connect.model.AbstractSmartAction;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.AlertFilterDefault;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.BasemapBounds;
import org.wcs.smart.connect.model.BasemapTile;
import org.wcs.smart.connect.model.CaPluginVersion;
import org.wcs.smart.connect.model.ConnectPluginVersion;
import org.wcs.smart.connect.model.ConnectSetting;
import org.wcs.smart.connect.model.ConnectUuidItem;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.CyberTrackerApiKey;
import org.wcs.smart.connect.model.CyberTrackerNavigationLayer;
import org.wcs.smart.connect.model.CyberTrackerPackage;
import org.wcs.smart.connect.model.MapLayer;
import org.wcs.smart.connect.model.SharedLink;
import org.wcs.smart.connect.model.SmartCollectConnectUser;
import org.wcs.smart.connect.model.SmartRole;
import org.wcs.smart.connect.model.SmartRoleAction;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.SmartUserRole;
import org.wcs.smart.connect.model.StyleConfiguration;
import org.wcs.smart.connect.model.WorkItem;
import org.wcs.smart.connect.qa.QaErLabelProvider;
import org.wcs.smart.connect.qa.QaIncidentLabelProvider;
import org.wcs.smart.connect.qa.QaLabelProvider;
import org.wcs.smart.connect.qa.QaPatrolLabelProvider;
import org.wcs.smart.connect.query.PatrolContributionFinder;
import org.wcs.smart.connect.query.WaypointSourceEngine;
import org.wcs.smart.connect.query.columns.AssetQueryColumnProvider;
import org.wcs.smart.connect.query.columns.ObservationQueryColumnProvider;
import org.wcs.smart.connect.query.columns.PatrolQueryColumnProvider;
import org.wcs.smart.connect.query.columns.SurveyQueryColumnProvider;
import org.wcs.smart.connect.query.engine.i2.IntelConnectionFactory;
import org.wcs.smart.connect.query.engine.i2.QueryEngineFactory;
import org.wcs.smart.connect.report.BirtEngine;
import org.wcs.smart.connect.report.SmartServiceLabelProvider;
import org.wcs.smart.connect.uploader.sync.ChangeLogManager;
import org.wcs.smart.cybertracker.ICyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.incident.model.IIncidentCyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.patrol.model.IPatrolCyberTrackerLabelProvider;
import org.wcs.smart.cybertracker.survey.model.ISurveyCyberTrackerLabelProvider;
import org.wcs.smart.er.model.IErLabelProvider;
import org.wcs.smart.er.query.ISurveyQueryLabelProvider;
import org.wcs.smart.er.query.model.ISurveyQueryColumnProvider;
import org.wcs.smart.event.i2.IProfileEventLabelProvider;
import org.wcs.smart.i2.IQueryEngineFactory;
import org.wcs.smart.i2.birt.datasource.IConnectionFactory;
import org.wcs.smart.incident.IIncidentLabelProvider;
import org.wcs.smart.observation.model.IWaypointSourceEngine;
import org.wcs.smart.observation.query.model.columns.IObservationQueryColumnProvider;
import org.wcs.smart.observation.query.view.IObservationQueryLabelProvider;
import org.wcs.smart.patrol.model.IPatrolLabelProvider;
import org.wcs.smart.patrol.query.ext.IPatrolContributionFinder;
import org.wcs.smart.patrol.query.model.IPatrolQueryColumnProvider;
import org.wcs.smart.patrol.ui.IQueryPatrolLabelProvider;
import org.wcs.smart.plan.IPlanLabelProvider;
import org.wcs.smart.query.model.IGridQueryColumnLabelProvider;
import org.wcs.smart.query.model.filter.date.IQueryDateLabelProvider;
import org.wcs.smart.smartcollect.model.ISmartCollectLabelProvider;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;
import org.wcs.smart.udig.catalog.smart.ISmartMapLabelProvider;

import it.geosolutions.imageio.stream.input.spi.URLImageInputStreamSpi;

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
		SmartContext.INSTANCE.setClass(ExecutorService.class, null);
		
		//clean up BIRT
		BirtEngine.destroyBirtEngine();
		
		//remove filewatcher
		try {
			ChangeLogManager.INSTANCE.shutDownFilestoreWatcher();
		} catch (IOException ex) {
			logger.warning("Error shutting down file store watcher"); //$NON-NLS-1$
		}
		
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
// This may not be needed anymore
//		try {
//			Class<?> h2Driver = Class.forName("org.h2.Driver"); //$NON-NLS-1$
//			Method m = h2Driver.getMethod("unload"); //$NON-NLS-1$
//			m.invoke(null);
//		} catch (Exception e) {
//			logger.log(Level.WARNING, "Failed to unload the H2 driver", e); //$NON-NLS-1$
//		}
        
		
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
		
	    //this is required to get the geotiff writer to work when writing raster map layers
		//for reports
	    IIORegistry.getDefaultInstance().registerServiceProvider(new URLImageInputStreamSpi());

	    //hack to support smart icons in map styling in reports
	    //allows a custom url handler that deals with the platform:/plugin
	    //urls and loads the corresponding image from the jar file
	    try {
	    	
			final Field factoryField = URL.class.getDeclaredField("factory"); //$NON-NLS-1$
			factoryField.setAccessible(true);
			final Field lockField = URL.class.getDeclaredField("streamHandlerLock"); //$NON-NLS-1$
			lockField.setAccessible(true);

			// use same lock as in java.net.URL.setURLStreamHandlerFactory
			synchronized (lockField.get(null)) {
				final URLStreamHandlerFactory urlStreamHandlerFactory = (URLStreamHandlerFactory) factoryField
						.get(null);
				// Reset the value to prevent Error due to a factory already defined
				factoryField.set(null, null);
				URL.setURLStreamHandlerFactory(new SMARTURLStreamHandlerProvider(urlStreamHandlerFactory));
			}
		} catch (Exception ex) {
			throw new IllegalStateException("Cannot url stream handler factory.", ex); //$NON-NLS-1$

		}
	    
		//configure file store
		try{
			DataStoreManager.INSTANCE.initDatastore();
		}catch(NamingException ex){
			throw new IllegalStateException("Cannot initialize datastore.", ex); //$NON-NLS-1$
		}
		SmartContext.INSTANCE.setFilestoreLocation(DataStoreManager.INSTANCE.getRootDirectory().toAbsolutePath().normalize().toString());
		SmartContext.INSTANCE.setTempFilestoreLocation(((File)sce.getServletContext().getAttribute(ServletContext.TEMPDIR)).toPath());
		
		//configure geotools properties
		System.setProperty("org.eclipse.emf.common.util.ReferenceClearingQueue", "false"); //$NON-NLS-1$ //$NON-NLS-2$
		System.setProperty("org.geotools.referencing.forceXY", "true"); //$NON-NLS-1$ //$NON-NLS-2$
		System.setProperty(ShpServiceExtension.SHP_CHARSET_PARAM_NAME, StandardCharsets.UTF_8.name());
		
		logger.info("Configuring Hibernate SessionFactory"); //$NON-NLS-1$
		Configuration config = new Configuration();
		
		config.configure("org/wcs/smart/connect/hibernate/hibernate.cfg.xml"); //$NON-NLS-1$
		
		//Add you annotated model classes here
		config.addAnnotatedClass(SmartUser.class);
		config.addAnnotatedClass(AbstractSmartAction.class);
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
		config.addAnnotatedClass(CyberTrackerPackage.class);
		config.addAnnotatedClass(CyberTrackerApiKey.class);
		config.addAnnotatedClass(CyberTrackerNavigationLayer.class);
		config.addAnnotatedClass(SmartCollectConnectUser.class);
		config.addAnnotatedClass(BasemapTile.class);
		config.addAnnotatedClass(BasemapBounds.class);
		config.addAnnotatedClass(ConnectSetting.class);
		try{
			for(Class<?> c : SmartHibernateConfiguration.INSTANCE.getTables()){
				config.addAnnotatedClass(c);
			}
		}catch (Exception ex){
			throw new RuntimeException("Could not read hibernate class files.", ex); //$NON-NLS-1$
		}

		BootstrapServiceRegistryBuilder bootstrapRegistryBuilder = new BootstrapServiceRegistryBuilder();
		bootstrapRegistryBuilder.applyIntegrator(new SmartHibernateIntegrator());

		ServiceRegistry service = new StandardServiceRegistryBuilder(bootstrapRegistryBuilder.build())
				.applySettings(config.getProperties()).build();
		
		SessionFactory sf = config.buildSessionFactory(service);
		sce.getServletContext().setAttribute(HibernateManager.CONTEXT_KEY, sf);
		
		SmartContext.INSTANCE.setClass(SessionFactory.class, sf);
		
		logger.info("Hibernate SessionFactory Configured successfully"); //$NON-NLS-1$
		
		// register filestore watcher for replication events
		try {
			ChangeLogManager.INSTANCE.watchFilestore(sf);
		} catch (IOException ex) {
			logger.log(Level.SEVERE, "Could not configure filestore watcher used for logging changes to filestore. " + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
		//register SMART context classes
		initSmartClasses(sce);
	
		//start executor for running background jobs
		int numthreads = 1;
		try{
			numthreads = (Integer) EnvironmentVariables.INSTANCE.getEnvironmentVariable(EnvironmentVariables.Variable.NUM_BACK_THREADS);
		}catch(Exception ex){
			logger.log(Level.WARNING, "Could not read variable " + EnvironmentVariables.Variable.NUM_BACK_THREADS.key + " from context.xml.", ex); //$NON-NLS-1$ //$NON-NLS-2$
		}
		ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(numthreads);
		sce.getServletContext().setAttribute(EXECUTOR_KEY, scheduler);
		SmartContext.INSTANCE.setClass(ExecutorService.class, scheduler);
		
		initCleanUpJob(sf, scheduler, sce.getServletContext());

		//bundle loader for extension points
		try {
			(new ServiceLauncher( )).startup(new PlatformConfig());
			BirtEngine.getBirtEngine(sce.getServletContext());
		} catch (FrameworkException e1) {
			logger.log(Level.WARNING, e1.getMessage(), e1);
		}
	}
	
	private void initCleanUpJob(SessionFactory sf, ScheduledExecutorService scheduler, ServletContext context){
		Integer cleanUpSchedule = 24;
		try{
			cleanUpSchedule = (Integer)EnvironmentVariables.INSTANCE.getEnvironmentVariable(EnvironmentVariables.Variable.CLEANUP_TASK_INTERVAL);
		}catch (Exception ex){
			logger.log(Level.WARNING, "Value not found for environment variable:" + EnvironmentVariables.Variable.CLEANUP_TASK_INTERVAL.key, ex); //$NON-NLS-1$
		}
		if (cleanUpSchedule >= 0){
			CleanUpJob job = new CleanUpJob(sf, context);
			scheduler.scheduleWithFixedDelay(job, 0, cleanUpSchedule, TimeUnit.HOURS);
		}
	}

	
	private void initSmartClasses(ServletContextEvent arg0) {
//		SmartContext.INSTANCE.setClass(IEntityLabelProvider.class, new EntityLabelProvider());
//		SmartContext.INSTANCE.setClass(IEntityQueryLabelProvider.class, new EntityQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IErLabelProvider.class, new ErLabelProvider());
		SmartContext.INSTANCE.setClass(IIncidentCyberTrackerLabelProvider.class, new IncidentCyberTrackerLabelProvider());
		SmartContext.INSTANCE.setClass(IPatrolCyberTrackerLabelProvider.class, new PatrolCyberTrackerLabelProvider());
		SmartContext.INSTANCE.setClass(ISmartCollectLabelProvider.class, new SmartCollectLabelProvider());
		SmartContext.INSTANCE.setClass(ISurveyCyberTrackerLabelProvider.class, new SurveyCyberTrackerLabelProvider());
		
		SmartContext.INSTANCE.setClass(IGridQueryColumnLabelProvider.class, new GridQueryColumnLabelProvider());
//		SmartContext.INSTANCE.setClass(IIntelligenceLabelProvider.class, new IntelligenceLabelProvider());
//		SmartContext.INSTANCE.setClass(IIntelligenceQueryLabelProvider.class, new IntelligenceQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IObservationQueryLabelProvider.class, new ObservationQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IPatrolLabelProvider.class, new PatrolLabelProvider());
		SmartContext.INSTANCE.setClass(IQueryPatrolLabelProvider.class, new PatrolQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IPlanLabelProvider.class, new PlanLabelProvider());
		SmartContext.INSTANCE.setClass(IQueryDateLabelProvider.class, new QueryDateLabelProvider());
		SmartContext.INSTANCE.setClass(ICoreLabelProvider.class, new SmartLabelProvider());
		SmartContext.INSTANCE.setClass(ISurveyQueryLabelProvider.class, new SurveyQueryLabelProvider());
		SmartContext.INSTANCE.setClass(ISurveyQueryLabelProvider.class, new SurveyQueryLabelProvider());
		SmartContext.INSTANCE.setClass(IIncidentLabelProvider.class, new IncidentLabelProvider());
		
//		SmartContext.INSTANCE.setClass(IEntityQueryColumnProvider.class, new EntityQueryColumnProvider());
//		SmartContext.INSTANCE.setClass(IIntelligenceQueryColumnProvider.class, new IntelligenceQueryColumnProvider());
		SmartContext.INSTANCE.setClass(IObservationQueryColumnProvider.class, new ObservationQueryColumnProvider());
		SmartContext.INSTANCE.setClass(IPatrolQueryColumnProvider.class, new PatrolQueryColumnProvider());
		SmartContext.INSTANCE.setClass(ISurveyQueryColumnProvider.class, new SurveyQueryColumnProvider());
		SmartContext.INSTANCE.setClass(IPatrolContributionFinder.class, new PatrolContributionFinder());
		SmartContext.INSTANCE.setClass(IWaypointSourceEngine.class, WaypointSourceEngine.INSTANCE);
		SmartContext.INSTANCE.setClass(ISmartMapLabelProvider.class, new SmartServiceLabelProvider());
		
		SmartContext.INSTANCE.setClass(org.wcs.smart.qa.er.ILabelProvider.class, new QaErLabelProvider());
		SmartContext.INSTANCE.setClass(org.wcs.smart.qa.ILabelProvider.class, new QaLabelProvider());
		SmartContext.INSTANCE.setClass(org.wcs.smart.qa.patrol.ILabelProvider.class, new QaPatrolLabelProvider());
		SmartContext.INSTANCE.setClass(org.wcs.smart.qa.incident.ILabelProvider.class, new QaIncidentLabelProvider());
		
		SmartContext.INSTANCE.setClass(org.wcs.smart.i2.IIntelligenceLabelProvider.class, new AdvancedLabelProviderImpl());
		
		SmartContext.INSTANCE.setClass(IProfileEventLabelProvider.class, new ProfileEventLabelProvider());
		
		SmartContext.INSTANCE.setClass(org.wcs.smart.smartcollect.model.ISmartCollectLabelProvider.class, new ISmartCollectLabelProvider() {
			@Override
			public String getLabel(Object item, Locale l) {
				if (item.getClass() == SmartCollectWaypointSource.class) return Messages.getString("ConnectStartupContextListener.SmartCollectIncidentName", l);  //$NON-NLS-1$
				return null;
			}
		});
		
		SmartContext.INSTANCE.setClass(org.wcs.smart.asset.ui.IQueryAssetLabelProvider.class, new AssetQueryLabelProvider());
		SmartContext.INSTANCE.setClass(org.wcs.smart.asset.IAssetLabelProvider.class, new AssetLabelProvider());
		SmartContext.INSTANCE.setClass(org.wcs.smart.observation.IObservationLabelProvider.class, new ObservationLabelProvider());
		SmartContext.INSTANCE.setClass(IAssetQueryColumnProvider.class, new AssetQueryColumnProvider());
		SmartContext.INSTANCE.setClass(ICyberTrackerLabelProvider.class, new CyberTrackerLabelProvider());
		
		
		SmartContext.INSTANCE.setClass(IConnectionFactory.class, new IntelConnectionFactory());
		
		//SmartContext.INSTANCE.setClass(UUIDBinaryType.class, PostgresUUIDType.INSTANCE);
		SmartContext.INSTANCE.setClass(IQueryEngineFactory.class, new QueryEngineFactory());		
		
		
	}
}

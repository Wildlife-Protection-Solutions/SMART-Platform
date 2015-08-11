package org.wcs.smart.connect.hibernate;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import org.hibernate.SessionFactory;
import org.hibernate.annotations.common.reflection.MetadataProviderInjector;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.service.ServiceRegistryBuilder;
import org.wcs.smart.ca.Agency;
import org.wcs.smart.connect.model.AlertType;	
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.Rank;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.connect.model.ConnectUuidItem;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.SmartUserAction;
import org.wcs.smart.connect.model.StyleConfiguration;
import org.wcs.smart.connect.model.UploadItem;
import org.wcs.smart.patrol.query.model.PatrolObservationQuery;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;

import com.sun.istack.internal.logging.Logger;

@WebListener
public class HibernateSessionFactoryListener implements ServletContextListener{

	public final Logger logger = Logger.getLogger(HibernateSessionFactoryListener.class);
	
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
		
		config.addAnnotatedClass(Employee.class);
		config.addAnnotatedClass(Language.class);
		config.addAnnotatedClass(Agency.class);
		config.addAnnotatedClass(Rank.class);
		config.addAnnotatedClass(ConservationArea.class);
		config.addAnnotatedClass(QueryFolder.class);
		config.addAnnotatedClass(Label.class);
		config.addAnnotatedClass(NamedItem.class);
		config.addAnnotatedClass(UuidItem.class);
		config.addAnnotatedClass(Query.class);
		config.addAnnotatedClass(PatrolObservationQuery.class);
		config.addAnnotatedClass(StyleConfiguration.class);
		config.addAnnotatedClass(AlertType.class);

		ServiceRegistry service = new ServiceRegistryBuilder().applySettings(config.getProperties()).buildServiceRegistry();
		SessionFactory sf = config.buildSessionFactory(service);
		
		sce.getServletContext().setAttribute(HibernateManager.CONTEXT_KEY, sf);
		
		logger.info("Hibernate SessionFactory Configured successfully"); //$NON-NLS-1$
		
		//start executor for running background jobs
		ExecutorService scheduler = Executors.newSingleThreadExecutor();
		sce.getServletContext().setAttribute(EXECUTOR_KEY, scheduler);
	}

}

package org.wcs.smart.connect.hibernate;

import java.util.List;
import java.util.UUID;

import javax.servlet.ServletContext;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.AlertType;
import org.wcs.smart.connect.model.ConservationAreaInfo;
import org.wcs.smart.connect.model.SmartUser;
import org.wcs.smart.connect.model.StyleConfiguration;

public class HibernateManager {
	
	public static final String CONTEXT_KEY = "SessionFactory"; //$NON-NLS-1$
	
	public static Session getSession(ServletContext context){
		return ((SessionFactory)context.getAttribute(CONTEXT_KEY)).getCurrentSession();
	}
	
	public static SessionFactory getSessionFactory(ServletContext context){
		return ((SessionFactory)context.getAttribute(CONTEXT_KEY));
	}
	
	public static SmartUser getUser(Session session, String username){
		SmartUser su = (SmartUser)session
				.createCriteria(SmartUser.class)
				.add(Restrictions.eq("username", username)) //$NON-NLS-1$
				.uniqueResult();
		return su;
	}
	
	public static List<SmartUser> getUsers(Session session){
		return (List<SmartUser>)session
					.createCriteria(SmartUser.class)
					.addOrder(Order.asc("username"))
					.list();
	}

	public static List<StyleConfiguration> getStyleConfigurations(Session session) {
		return (List<StyleConfiguration>)session
				.createCriteria(StyleConfiguration.class)
				.list();
	}

	public static List<ConservationAreaInfo> getConservationAreaInfos(Session session) {
		return (List<ConservationAreaInfo>)session
				.createCriteria(ConservationAreaInfo.class)
				.list();
	}

	public static ConservationAreaInfo getConservationAreaInfo(Session session, UUID caUuid) {
		return (ConservationAreaInfo)session
				.createCriteria(ConservationAreaInfo.class)
				.add(Restrictions.eq("ca_uuid", caUuid)) //$NON-NLS-1$
				.list();
	}
	
	public static List<AlertType> getAlertTypes(Session session) {
		return (List<AlertType>)session
				.createCriteria(AlertType.class)
				.list();
	}

	public static List<Alert> getAlerts(Session session) {
		return (List<Alert>)session
				.createCriteria(Alert.class)
				.list();
	}

	public static Alert getAlert(Session session, UUID alertUuid) {
		Alert a = (Alert)session
				.createCriteria(Alert.class)
				.add(Restrictions.eq("alert_id", alertUuid)) //$NON-NLS-1$
				.uniqueResult();
		return a;
	}

	public static AlertType getAlertType(Session session, UUID typeUuid) {
		AlertType a = (AlertType)session
				.createCriteria(AlertType.class)
				.add(Restrictions.eq("uuid", typeUuid)) //$NON-NLS-1$
				.uniqueResult();
		return a;
	}

	public static Alert getAlertByUserId(Session session, String userGenId) {
		Alert a = (Alert)session
				.createCriteria(Alert.class)
				.add(Restrictions.eq("user_generated_id", userGenId)) //$NON-NLS-1$
				.uniqueResult();
		return a;
	}

	public static List<Alert> getAlertsByCa(Session session, UUID caUuid) {
		return (List<Alert>)session
				.createCriteria(Alert.class)
				.add(Restrictions.eq("ca_uuid", caUuid))
				.list();
	}
	
}

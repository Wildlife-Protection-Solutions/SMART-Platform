package org.wcs.smart.connect;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.SmartDB;

public class ConnectHibernateManager {

	public static ConnectServer getConnectServer(Session session){
		ConnectServer server = (ConnectServer) session.createCriteria(ConnectServer.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
				.uniqueResult();
		return server;
	}
	
	public static ConnectUser getConnectUser(Employee e, Session session){
		return (ConnectUser)session.get(ConnectUser.class, e.getUuid());
	}
	
	public static ConnectServerStatus getConnectServerStatus(Session session){
		return (ConnectServerStatus)session.get(ConnectServerStatus.class, 
				SmartDB.getCurrentConservationArea().getUuid());
		
	}
}

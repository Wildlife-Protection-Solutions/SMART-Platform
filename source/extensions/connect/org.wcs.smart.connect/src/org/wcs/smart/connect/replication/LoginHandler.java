package org.wcs.smart.connect.replication;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ILoginHandler;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.server.UploadCaJob;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public class LoginHandler implements ILoginHandler {

	@Override
	public void onLogin() throws Exception {
		//never replicate multiple conservation areas
		if (SmartDB.isMultipleAnalysis()) return ;
		
		ConnectServerStatus status;
		
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			status = (ConnectServerStatus)s.get(ConnectServerStatus.class, SmartDB.getCurrentConservationArea().getUuid());
			if (status == null){
				return;
			}
			if (status.getStatus() == ConnectServerStatus.Status.ACTIVE ||
				status.getStatus() == ConnectServerStatus.Status.DONE ){
				DerbyReplicationManager.INSTANCE.enableReplication(s);
			}
			
			
		}catch (Exception ex){
			throw ex;
		}finally{
			s.close();
		}
		
		
		//TODO: test this
		s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			if (status.getStatus() == ConnectServerStatus.Status.ACTIVE){
				ConnectServer server = (ConnectServer) s.createCriteria(ConnectServer.class)
							.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea()))
							.uniqueResult();
				if (server == null){
					//no server configured; this should NEVER happen
					return;
				}
				
				ConnectUser user = (ConnectUser) s.createCriteria(ConnectUser.class)
					.add(Restrictions.eq("smartUser", SmartDB.getCurrentEmployee()))
					.uniqueResult();
				if (user == null){
					//this user does not have the ability to update conservation
					//area to smart connect so we have to do something here
					//do we want to warn the user or what??
					return;
				}
				
				SmartConnect connect = new SmartConnect(server.getServerUrl(), user.getConnectUsername(), user.getConnectPassword());
				//need to continue upload
				UploadCaJob job = new UploadCaJob(connect, status);
				job.schedule();
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectPlugIn.log("Error continuing connect upload", ex);
		}finally{
			s.close();
		}
	}

}

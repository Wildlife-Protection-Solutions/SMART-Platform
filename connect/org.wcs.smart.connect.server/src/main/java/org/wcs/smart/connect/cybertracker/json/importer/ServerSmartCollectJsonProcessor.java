package org.wcs.smart.connect.cybertracker.json.importer;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.api.SmartCollectApi;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectSetting;
import org.wcs.smart.connect.model.SmartCollectConnectUser;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.smartcollect.model.SmartCollectUser;
import org.wcs.smart.smartcollect.model.SmartCollectUser.State;
import org.wcs.smart.smartcollection.json.SmartCollectJsonProcessor;

public class ServerSmartCollectJsonProcessor extends SmartCollectJsonProcessor {

	private final Logger logger = Logger.getLogger(ServerSmartCollectJsonProcessor.class.getName());

	private Session session;
	
	public ServerSmartCollectJsonProcessor(ConservationArea ca, Session session) {
		super(ca);
		this.session = session;
	}

	@Override
	protected Map<DeviceUser, SmartCollectUser> getUserState(Set<DeviceUser> users) {
		Map<DeviceUser, SmartCollectUser> values = new HashMap<>();
		for (DeviceUser user : users) {
			if (user.getUser() == null) continue;

			SmartCollectConnectUser collectUser = QueryFactory.buildQuery(session, SmartCollectConnectUser.class, 
					new Object[]{"source", user.getUser()}, //$NON-NLS-1$
					new Object[] {"deviceId", user.getDeviceId()}).uniqueResult(); //$NON-NLS-1$
			
			if (collectUser == null) {
				//create a new user
				collectUser = new SmartCollectConnectUser();
				collectUser.setSource(user.getUser());
				collectUser.setDeviceId(user.getDeviceId());
				collectUser.setState(SmartCollectUser.State.NEW);
				
				session.persist(collectUser);
			}
			
			values.put(user, collectUser.toSmartCollectUser());			
		}
		return values;
	}

	@Override
	protected void logException(String message, Exception ex) {
		logger.log(Level.SEVERE, message, ex);	
	}

	@Override
	protected Boolean processNotOkUsers(Set<SmartCollectUser> notok) throws Exception {
		ConnectSetting option = session.get(ConnectSetting.class, ConnectSetting.Setting.DQ_SMART_COLLECT_USEROPTION.key);
		
		ConnectSetting.SmartCollectUserOption processingOp = null;
		if (option != null) {
			processingOp = ConnectSetting.SmartCollectUserOption.findOption(option.getValue());
		}
		if (processingOp == null) {
			processingOp = ConnectSetting.SmartCollectUserOption.VALIDATE_REQUEUE;
		}
		
		if (processingOp == ConnectSetting.SmartCollectUserOption.DISCARD) {
			warnings.add(new JsonImportWarning("User associated with data is not validated, all data will be discared (this setting can be changed in the Settings section on SMART Connect)."));
			//discard all
			return true;
		}else if (processingOp == ConnectSetting.SmartCollectUserOption.LOAD) {
			//temporarily set to validated for the purposes of processing this dataset
			for (SmartCollectUser u : notok) u.setState(State.VALIDATED);
			return false;
		}else if (processingOp == ConnectSetting.SmartCollectUserOption.REQUEUE) {
			warnings.add(new JsonImportWarning("User associated with data is not validated. File will be requeued (this setting can be changed in the Settings section on SMART Connect)."));
			return null;
		}else if (processingOp == ConnectSetting.SmartCollectUserOption.VALIDATE_REQUEUE) {
			warnings.add(new JsonImportWarning("User associated with data is not validated. Validation request sent and the file requeued (this setting can be changed in the Settings section on SMART Connect)."));
			for (SmartCollectUser u : notok) {
				try {
					sendEmailRequest(u);
				}catch (Exception ex) {
					logException(ex.getMessage(), ex);
				}
			}			
			return null;
		}
		return null;
	}

	private void sendEmailRequest(SmartCollectUser u) throws Exception{
		try(Session session2 = session.getSessionFactory().openSession()){
			session2.beginTransaction();
			
			SmartCollectConnectUser user = session2.get(SmartCollectConnectUser.class, u.getUuid());
			if (user == null) return;
			if (SmartCollectUser.isEmailSource(user.getSource())) {		
	
				ConnectServer server = session2.createQuery("FROM ConnectServer WHERE conservationArea=:ca", ConnectServer.class) //$NON-NLS-1$
						.setParameter("ca", ca) //$NON-NLS-1$
						.uniqueResult();
				if (server == null) return;
				
				String url = server.getServerUrl();
				
				SmartCollectApi.sendValidationRequest(null, user, url);
			}
			session2.getTransaction().commit();
		}
	}


}

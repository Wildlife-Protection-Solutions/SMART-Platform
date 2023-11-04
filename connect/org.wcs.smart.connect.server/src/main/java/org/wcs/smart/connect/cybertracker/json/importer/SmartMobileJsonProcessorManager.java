package org.wcs.smart.connect.cybertracker.json.importer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.model.ConnectSetting;
import org.wcs.smart.cybertracker.incident.IncidentJsonPostProcessor;
import org.wcs.smart.cybertracker.json.IJsonPostProcessor;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonPostProcessor;
import org.wcs.smart.cybertracker.survey.json.MissionJsonPostProcessor;

public enum SmartMobileJsonProcessorManager {
	
	INSTANCE;

	private SmartMobileJsonJob processingJob = null;
	private Future<?> jobFuture = null;
	
	public boolean canProcessOnConnect(Session session) {
		ConnectSetting setting = session.get(ConnectSetting.class, 
				ConnectSetting.Setting.DQ_SMART_MOBILE_PROCESSING.key);
		//default is to assume processing on connect
		if (setting != null && setting.getValue().equalsIgnoreCase("false")) { //$NON-NLS-1$
			//only if setting is false then processing doesn't happen
			return false;
		}
		return true;
	}
	
	public synchronized void startProcessing(SessionFactory sessionFactory) {
		
		if (jobFuture == null || jobFuture.isDone() || jobFuture.isCancelled()) {
			//check if processing on connect
			try(Session session = sessionFactory.openSession()){
				if (!canProcessOnConnect(session)) return;
			}
			
			//schedule job
			ExecutorService executor = SmartContext.INSTANCE.getClass(ExecutorService.class);
			processingJob = new SmartMobileJsonJob(sessionFactory);
			this.jobFuture = executor.submit(processingJob);
		}
		
	}
	
	public IJsonProcessor[] getProcessors(ConservationArea ca, Session session){
		
		return new IJsonProcessor[] {
			new ServerPatrolJsonProcessor(ca),
			new ServerIncidentJsonProcessor(ca),
			new ServerSmartCollectJsonProcessor(ca, session),
			new ServerMissionJsonProcessor(ca)
		};
		
	}
	
	public IJsonPostProcessor[] getPostProcessors(){
		return new IJsonPostProcessor[] {
			new PatrolJsonPostProcessor(),
			new IncidentJsonPostProcessor(),
			new MissionJsonPostProcessor()
		};
	}
}

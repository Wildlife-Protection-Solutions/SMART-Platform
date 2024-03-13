/*
 * Copyright (C) 2023 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.json.importer;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.cybertracker.incident.IncidentJsonPostProcessor;
import org.wcs.smart.cybertracker.json.IJsonPostProcessor;
import org.wcs.smart.cybertracker.json.IJsonProcessor;
import org.wcs.smart.cybertracker.patrol.json.PatrolJsonPostProcessor;
import org.wcs.smart.cybertracker.survey.json.MissionJsonPostProcessor;

public enum SmartMobileJsonProcessorManager {
	
	INSTANCE;

	private SmartMobileJsonJob processingJob = null;
	private Future<?> jobFuture = null;
	
	/**
	 * 
	 * @param session
	 * @return true if at least one ConservationArea can be processed on Connect
	 */
	public boolean canProcessOnConnect(Session session) {
		Long count = session.createQuery("SELECT count(*) FROM ConservationAreaInfo WHERE smartMobileDqProcessor = true and uuid != :ccaa", Long.class)//$NON-NLS-1$
				.setParameter("ccaa", ConservationArea.MULTIPLE_CA)//$NON-NLS-1$
				.uniqueResult();
		return count > 0;
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

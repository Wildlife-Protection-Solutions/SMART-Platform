/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.engine;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.internal.Messages;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsRun.Status;
import org.wcs.smart.paws.model.PawsService;

/**
 * API for interacting with the PAWS Service
 * @author Emily
 *
 */
public enum PawsApi {
	
	INSTANCE;
	
	enum PawsStatus{
		IN_PROGRESS(""), //$NON-NLS-1$
		ERROR("failed"), //$NON-NLS-1$
		DONE("completed"); //$NON-NLS-1$
		
		public String responseCode;
		
		PawsStatus(String code){
			this.responseCode = code;
		}
	};
	
	public void run(PawsRun run, String json) throws Exception{
		String surl = null;
		String key = null;
		try(Session session = HibernateManager.openSession()){
			PawsService service = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult(); //$NON-NLS-1$
				
			if (service == null || !service.isConfigured()) {
				throw new Exception(Messages.PawsApi_ServiceNotConfigured);
			}
			surl = service.getPawsApiUrl();
			key = service.getApiKey();
		}
		
		//call the service with the json payload
		URL url = URI.create(surl).toURL();
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		try{
			conn.setRequestMethod("POST"); //$NON-NLS-1$
			conn.setRequestProperty("Content-Type", "application/json"); //$NON-NLS-1$ //$NON-NLS-2$
			conn.setRequestProperty("Ocp-Apim-Subscription-Key", key); //$NON-NLS-1$
			conn.setDoOutput(true);
			//headers
			OutputStream os = conn.getOutputStream();
			os.write(json.getBytes(StandardCharsets.UTF_8));
			os.flush();		
			
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK && 
					conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
				
				throw new Exception(Messages.PawsApi_RunFailed
					+ conn.getResponseCode());
			}
			
			StringBuffer content = new StringBuffer();
			String inputLine;
			try(BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
			}
			
			PawsTask task = PawsTask.parse(content.toString());
			
			try(Session session = HibernateManager.openSession()){
				session.beginTransaction();
				try {
					PawsRun r = session.get(PawsRun.class, run.getUuid());
					r.setTaskId(task.getTaskId());
					session.getTransaction().commit();
				}catch (Exception ex) {
					session.getTransaction().rollback();
					throw new Exception(Messages.PawsApi_TaskIdNotFound);
				}
			}
			
		}catch (Exception ex){
			PawsPlugIn.log(ex.getMessage(), ex);
			throw ex;
		}finally{
			conn.disconnect();
		}
	}
	
	public PawsStatus checkStatus(PawsRun run) throws Exception{
		String surl = null;
		String key = null;
		try(Session session = HibernateManager.openSession()){
			PawsRun r = (PawsRun)session.get(PawsRun.class, run.getUuid());
			if (r == null) throw new Exception(Messages.PawsApi_RunNotFound);
			
			PawsService service = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult(); //$NON-NLS-1$
				
			if (service == null || !service.isConfigured()){
				throw new Exception(Messages.PawsApi_ServiceNotConfigured2);
			}
			surl = service.getTaskApiUrl() + "/" + run.getTaskId(); //$NON-NLS-1$
			key = service.getApiKey();
		}
		
		//call the service with the json payload
		URL url = URI.create(surl).toURL();
		
		
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		try{
			conn.setRequestMethod("GET"); //$NON-NLS-1$
			conn.setRequestProperty("Ocp-Apim-Subscription-Key", key); //$NON-NLS-1$
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				
			}else if (conn.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
				//error; run doesn't exists
				try(Session session = HibernateManager.openSession()){
					PawsRun r = (PawsRun)session.get(PawsRun.class, run.getUuid());
					if (r != null) {
						session.beginTransaction();
						try {
							r.setStatus(Status.ERROR);
							r.setStatusMessage(Messages.PawsApi_TaskNotFoundOnService);
							session.getTransaction().commit();
						}catch (Exception ex) {
							if (session.getTransaction().isActive()) session.getTransaction().rollback();
							PawsPlugIn.displayLog(ex.getMessage(), ex);
						}
					}
				}
				return PawsStatus.ERROR;
			}else {
				throw new Exception(Messages.PawsApi_RunFailedWithCode + conn.getResponseCode());
			}
			
			StringBuffer content = new StringBuffer();
			String inputLine;
			try(BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))){
				while ((inputLine = in.readLine()) != null) {
					content.append(inputLine);
				}
			}
			
			PawsTask task = PawsTask.parse(content.toString());


			try(Session session = HibernateManager.openSession()){
				PawsRun r = (PawsRun)session.get(PawsRun.class, run.getUuid());
				if (r != null) {
					session.beginTransaction();
					try {
						String status = task.getStatus();
						if (status.length() > 32700) status = status.substring(0,32700);
						r.setStatusMessage(status);
						
						String json = content.toString();
						if (json.length() > 32700) json = json.substring(0, 32700);
						r.setServerStatusJson(json);
						
						if (task.getBackendStatus().equalsIgnoreCase(PawsStatus.ERROR.responseCode)) {
							r.setStatus(Status.ERROR);
						}
						
						session.getTransaction().commit();
					}catch (Exception ex) {
						if (session.getTransaction().isActive()) session.getTransaction().rollback();
						PawsPlugIn.displayLog(ex.getMessage(), ex);
					}
				}
			}

			if (task.getBackendStatus().equalsIgnoreCase(PawsStatus.ERROR.responseCode)) return PawsStatus.ERROR;
			if (task.getBackendStatus().equalsIgnoreCase(PawsStatus.DONE.responseCode)) return PawsStatus.DONE;
			return PawsStatus.IN_PROGRESS;
		}catch (Exception ex){
			PawsPlugIn.log(ex.getMessage(), ex);
			throw ex;
		}finally{
			conn.disconnect();
		}
	}
}

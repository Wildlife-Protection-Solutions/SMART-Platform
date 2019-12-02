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
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsPlugIn;
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
		IN_PROGRESS,
		ERROR,
		DONE
	};
	
	public void run(PawsRun run, String json) throws Exception{
		String surl = null;
		String key = null;
		try(Session session = HibernateManager.openSession()){
			PawsService service = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult();
				
			if (service == null || !service.isConfigured()) {
				throw new Exception("PAWS Service not configured.  You must first configure the PAWS Service before you can run paws analysis.");
			}
			surl = service.getHeatmapApi();
			key = service.getApiKey();
		}
		
		//call the service with the json payload
		URL url = new URL(surl);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		try{
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("Ocp-Apim-Subscription-Key", key);
			conn.setDoOutput(true);
			//headers
			OutputStream os = conn.getOutputStream();
			os.write(json.getBytes(StandardCharsets.UTF_8));
			os.flush();
			
			
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK && 
					conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
				
				throw new Exception("Failed to run PAWS Service - HTTP error code : "
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
					throw new Exception("Unable to update local database with PAWS task id.  Run will not be configured correctly.  You should delete it locally and all data uploaded to the cloud storage and try again.");
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
			if (r == null) throw new Exception("No object found in database.");
			
			PawsService service = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult();
				
			if (service == null || !service.isConfigured()){
				throw new Exception("PAWS Service not configured.");
			}
			//TODO: header
			surl = service.getTaskApi() + "/" + run.getTaskId();// + "?subscription-key=" + service.getApiKey();
			key = service.getApiKey();
		}
		
		//call the service with the json payload
		URL url = new URL(surl);
		
		
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		try{
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Ocp-Apim-Subscription-Key", key);
			if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
				
			}else if (conn.getResponseCode() == HttpURLConnection.HTTP_NO_CONTENT) {
				//error; run doesn't exists
				try(Session session = HibernateManager.openSession()){
					PawsRun r = (PawsRun)session.get(PawsRun.class, run.getUuid());
					if (r != null) {
						session.beginTransaction();
						try {
							r.setStatus(Status.ERROR);
							r.setStatusMessage("Task not found on server.");
							session.getTransaction().commit();
						}catch (Exception ex) {
							if (session.getTransaction().isActive()) session.getTransaction().rollback();
							PawsPlugIn.displayLog(ex.getMessage(), ex);
						}
					}
				}
				return PawsStatus.ERROR;
			}else {
				throw new Exception("Failed to run PAWS Service - HTTP error code : " + conn.getResponseCode());
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
						r.setServerStatusJson(content.toString());
						r.setStatusMessage(task.getStatus());
						if (task.getBackendStatus().equalsIgnoreCase("failed")) {
							r.setStatus(Status.ERROR);
							r.setStatusMessage(task.getStatus());
						}
						
						session.getTransaction().commit();
					}catch (Exception ex) {
						if (session.getTransaction().isActive()) session.getTransaction().rollback();
						PawsPlugIn.displayLog(ex.getMessage(), ex);
					}
				}
			}

			if (task.getBackendStatus().equalsIgnoreCase("failed")) return PawsStatus.ERROR;
			if (task.getBackendStatus().equalsIgnoreCase("complete")) return PawsStatus.DONE;
			return PawsStatus.IN_PROGRESS;
		}catch (Exception ex){
			PawsPlugIn.log(ex.getMessage(), ex);
			throw ex;
		}finally{
			conn.disconnect();
		}
	}
}

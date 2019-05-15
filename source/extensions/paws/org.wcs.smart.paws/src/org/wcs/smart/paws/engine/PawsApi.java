package org.wcs.smart.paws.engine;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.paws.PawsPlugIn;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.model.PawsService;

public enum PawsApi {
	
	INSTANCE;
	
	public void run(ConservationArea ca, String json) throws Exception{
		String surl = null;
		try(Session session = HibernateManager.openSession()){
			PawsService service = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", ca}).uniqueResult();
				
			if (service == null || service.getApiKey() == null || service.getUrl() == null){
				throw new Exception("PAWS Service not configured.  You must first configure the PAWS Service before you can run paws analysis.");
			}
			surl = service.getUrl() + "?" + service.getApiKey();
		}
		
		//call the service with the json payload
		URL url = new URL(surl);
		HttpURLConnection conn = (HttpURLConnection)url.openConnection();
		try{
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			OutputStream os = conn.getOutputStream();
			os.write(json.getBytes(StandardCharsets.UTF_8));
			os.flush();
			
			if (conn.getResponseCode() != HttpURLConnection.HTTP_OK && conn.getResponseCode() != HttpURLConnection.HTTP_CREATED) {
				throw new Exception("Failed to run PAWS Service - HTTP error code : "
					+ conn.getResponseCode());
			}
		}catch (Exception ex){
			PawsPlugIn.log(ex.getMessage(), ex);
			throw ex;
		}finally{
			conn.disconnect();
		}
	}
	
	public boolean checkStatus(PawsRun run) throws Exception{
		String surl = null;
		try(Session session = HibernateManager.openSession()){
			PawsRun r = (PawsRun)session.get(PawsRun.class, run.getUuid());
			if (r == null) throw new Exception("No object found in database.");
			
			PawsService service = QueryFactory.buildQuery(session, PawsService.class,  
					new Object[] {"conservationArea", run.getConservationArea()}).uniqueResult();
				
			if (service == null || service.getApiKey() == null || service.getUrl() == null || service.getApiKey().isBlank() || service.getUrl().isBlank()){
				throw new Exception("PAWS Service not configured.");
			}
			surl = service.getUrl() + "/task/" + run.getRunId();
		}
		
		//call the service with the json payload
		URL url = new URL(surl);
		//TODO:
		return false;
	}
}

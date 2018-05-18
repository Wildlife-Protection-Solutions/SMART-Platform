package org.wcs.smart.connect.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.GlobalForestWatchApi;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;
import org.wcs.smart.connect.model.GlobalForestWatch;
import org.wcs.smart.util.UuidUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@WebServlet(ConnectRESTApplication.NO_AUTH_PATH  + GlobalForestWatchApi.PATH + "/*")
public class GlobalForestWatchProcessorServlet extends HttpServlet {
	
	private final Logger logger = Logger.getLogger(GlobalForestWatchProcessorServlet.class.getName());

	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException{
		
		String pathInfo = request.getPathInfo();
		if (pathInfo.startsWith("/")) {
			pathInfo = pathInfo.substring(1);
		}
		
		UUID gfwUuid = null;
		try {
			gfwUuid = UuidUtils.stringToUuid(pathInfo);
		}catch (Exception ex) {
			response.setStatus(Status.BAD_REQUEST.getStatusCode());
			return;
		}
		
		GlobalForestWatch gw = null;
		Session s = HibernateManager.getSession(request.getServletContext());
		
		
		try {
			s.beginTransaction();
			gw = s.get(GlobalForestWatch.class, gfwUuid);
		
		
			if (gw == null) {
				response.setStatus(Status.NOT_FOUND.getStatusCode());
				return;
			}
			
			//log the request json so I can figure out how to parse it
			StringBuilder json = new StringBuilder();
			try(BufferedReader reader = request.getReader()){
				reader.lines().forEach(e->json.append(e));
			}
			logger.log(Level.SEVERE, json.toString());
			
			
			List<Alert> alerts = processJson(json.toString(), gw);
			//TODO: remove duplicates
			for (Alert a : alerts) {
				s.saveOrUpdate(a);
			}
			s.getTransaction().commit();
		}catch (Exception ex) {
			logger.log(Level.SEVERE, "Error saving global forest watch alerts",  ex);
			s.getTransaction().rollback();
		}
	}

	
	private List<Alert> processJson(String json, GlobalForestWatch gfw) throws JsonParseException, JsonMappingException, IOException {
		List<Alert> createdAlerts = new ArrayList<>();
		
		StringBuilder sbDescription = new StringBuilder();
		sbDescription.append("Global Forest Watch");
		
		ObjectMapper objectMapper = new ObjectMapper();
		HashMap<String, Object> jsonData  = objectMapper.readValue(json, HashMap.class);
		
		if (jsonData.containsKey("alert_name")) {
			sbDescription.append(" (");
			sbDescription.append(jsonData.get("alert_name"));
			sbDescription.append(")");
			
		}
		
		List<Map<String, Object>> alerts = null;
		if (jsonData.containsKey("downloadUrls")) {
			Map<String, Object> downloads = (Map<String, Object>) jsonData.get("downloadUrls");
			if (downloads.containsKey("json")) {
				String alertsUrl =(String) downloads.get("json");
				
				//reading alert data from url
				logger.log(Level.WARNING, "Reading GFW JSON Alert Data From File: " + alertsUrl);
				
				StringWriter sw = new StringWriter();
				try {
					URL url = new URL(alertsUrl);
					
					IOUtils.copy(url.openStream(), sw, StandardCharsets.UTF_8);
				}catch (Exception ex) {
					logger.log(Level.SEVERE, "Unable to read gfw json alert data from additional json file.",  ex);
					throw ex;
				}
				try {
					alerts = objectMapper.readValue(sw.toString(), List.class);
				}catch (Exception ex) {
					logger.log(Level.WARNING, "Json data attempting to parse");
					logger.log(Level.WARNING, sw.toString());
					logger.log(Level.SEVERE, "Unable to read parse json data from additional json file.",  ex);
					throw ex;
				}
			}
		}
		
		if (alerts == null) {
			if (jsonData.containsKey("alerts")) {
				alerts = (List<Map<String, Object>>) jsonData.get("alerts");
			}
		}
		
		for (Map<String, Object> alertMap: alerts) {
			
			Double lat = null;
			Double lng = null;
			String date = null;
			String time = null;
			Date datetime = null;
			
			if (alertMap.containsKey("latitude")) {
				lat = (Double)alertMap.get("latitude");
			}
			
			if (alertMap.containsKey("longitude")) {
				lng = (Double)alertMap.get("latitude");
			}
			
			if (alertMap.containsKey("acq_date")) {
				date = (String)alertMap.get("acq_date");
			}
			
			if (alertMap.containsKey("acq_time")) {
				time = (String)alertMap.get("acq_time");
			}
			
			if (date != null && time != null) {
				SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
				try {
					datetime = sdf.parse(date +" " + time);
				}catch (Exception ex) {
					//TODO:
				}
			}
			
			if (datetime == null || lat == null || lng == null) continue;
			
			//create a new alert here; check for duplicates later
			Alert alert = new Alert();
			alert.setCa(null);
			alert.setCreatorUuid(gfw.getCreator().getUuid());
			alert.setDate(datetime);
			alert.setDescription(sbDescription.toString());
			alert.setLevel(gfw.getLevel());
			alert.setStatus(AlertStatusEnum.ACTIVE);
			alert.setTrack(null);
			alert.setTypeUuid(gfw.getAlertType().getUuid());
			alert.setX(lng);
			alert.setY(lat);
			alert.setUserGeneratedId((UUID.randomUUID().toString()));
			
			
			createdAlerts.add(alert);
		}
		return createdAlerts;
		
	}
}

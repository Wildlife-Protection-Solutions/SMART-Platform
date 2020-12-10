/*
 * Copyright (C) 2015 Wildlife Conservation Society
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
package org.wcs.smart.connect.api.noa;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.io.IOUtils;
import org.hibernate.Session;
import org.wcs.smart.connect.SmartUtils;
import org.wcs.smart.connect.api.ConnectRESTApplication;
import org.wcs.smart.connect.api.GlobalForestWatchApi;
import org.wcs.smart.connect.datastore.DataStoreManager;
import org.wcs.smart.connect.exceptions.SmartConnectException;
import org.wcs.smart.connect.hibernate.HibernateManager;
import org.wcs.smart.connect.i18n.Messages;
import org.wcs.smart.connect.model.Alert;
import org.wcs.smart.connect.model.Alert.AlertStatusEnum;
import org.wcs.smart.connect.model.GlobalForestWatch;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

/**
 * Servlet for processing global forest watch push requests
 * 
 * @author Emily
 *
 */
@javax.ws.rs.Path(ConnectRESTApplication.PATH_SEPERATOR + GlobalForestWatchApi.PATH)
public class GlobalForestWatchNoa extends HttpServlet {
	
	
	private static final long serialVersionUID = 1L;

	/**
	 * Directory in file store for logging global forest watch data files.  These
	 * will be cleaned up as part of the clean up tasks
	 */
	public static final String LOG_DIRECTORY = "gfw"; //$NON-NLS-1$

	/**
	 * Date format for filenames for logging gfw files
	 */
	public static final String DATE_FORMAT = "yyyyMMdd"; //$NON-NLS-1$
	/*
	 * JSON fields
	 */
	private static final String ACQ_TIME_JSON_KEY = "acq_time"; //$NON-NLS-1$
	private static final String ACQ_DATE_JSON_KEY = "acq_date"; //$NON-NLS-1$
	private static final String LONGITUDE_JSON_KEY = "longitude"; //$NON-NLS-1$
	private static final String LATITUDE_JSON_KEY = "latitude"; //$NON-NLS-1$
	private static final String ALERTS_JSON_KEY = "alerts"; //$NON-NLS-1$
	private static final String JSONFILE_JSON_KEY = "json"; //$NON-NLS-1$
	private static final String DOWNLOAD_URLS_JSON_KEY = "downloadUrls"; //$NON-NLS-1$
	private static final String ALERT_NAME_JSON_KEY = "alert_name"; //$NON-NLS-1$

	
	private final Logger logger = Logger.getLogger(GlobalForestWatchNoa.class.getName());
	
	@Context private ServletContext context;
	@Context private HttpServletRequest request;

	/**
	 * Process Global Forest Watch post request
	 */
	@POST
    @javax.ws.rs.Path("{uuid}")
	@Operation(description = "Processes global forest watch push requests, converting the notifications to SMART Connect Alerts.")
	@ApiResponse(responseCode = "200", description="Request processed successfully")
	@ApiResponse(responseCode = "400", description = "Invalid parameters supplied")
	@ApiResponse(responseCode = "404", description = "Requested gfw configuration not found")
	@ApiResponse(responseCode = "500", description = "Parsing or other internal server error")
	public void postGfwData(@PathParam("uuid") String gfwuuid){
		UUID gfwUuid = null;
		try {
			gfwUuid = UuidUtils.stringToUuid(gfwuuid);
		}catch (Exception ex) {
			throw new SmartConnectException(Status.BAD_REQUEST, Messages.getString("GlobalForestWatchNoa.InvalidUuid", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
		}
		
		GlobalForestWatch gw = null;
		Session s = HibernateManager.getSession(context);
		try {
			s.beginTransaction();
			gw = s.get(GlobalForestWatch.class, gfwUuid);
		
			if (gw == null) {
				throw new SmartConnectException(Status.NOT_FOUND, Messages.getString("GlobalForestWatchNoa.GFWNotFound", SmartUtils.getRequestLocale(request))); //$NON-NLS-1$
			}
			
			gw.setLastDataDate(LocalDateTime.now());
			StringBuilder json = new StringBuilder();

			try(BufferedReader reader = request.getReader()){
				reader.lines().forEach(e->json.append(e));
			}
			
			List<Alert> alerts = processJson(json.toString(), gw);
			for (Alert a : alerts) {
				if (!isDuplicate(a, s)) {
					a.setTrack("[[" + a.getX() + "," + a.getY() + "]]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
					s.saveOrUpdate(a);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex) {
			logger.log(Level.SEVERE, "Error saving global forest watch alerts",  ex); //$NON-NLS-1$
			throw new SmartConnectException(Status.INTERNAL_SERVER_ERROR, ex.getMessage());
		}finally {
			if (s.getTransaction().isActive()) s.getTransaction().rollback();
		}
	}

	/**
	 * Checks the database for a duplicate alert.  Duplicate alerts
	 * are alerts that have the smart conservationarea, date, time, location, level
	 * and type
	 * 
	 * @param a
	 * @param s
	 * @return
	 */
	private boolean isDuplicate(Alert a, Session s) {
		Long cnt = QueryFactory.buildCountQuery(s, Alert.class, 
				new Object[] {"ca", a.getCa()}, //$NON-NLS-1$
				new Object[] {"level", a.getLevel()}, //$NON-NLS-1$
				new Object[] {"typeUuid", a.getTypeUuid()}, //$NON-NLS-1$
				new Object[] {"x",a.getX()}, //$NON-NLS-1$
				new Object[] {"y", a.getY()}, //$NON-NLS-1$
				new Object[] {"date", a.getDate()}); //$NON-NLS-1$
		if (cnt > 0) return true;
		return false;
	}
	
	@SuppressWarnings("unchecked")
	private List<Alert> processJson(String json, GlobalForestWatch gfw) throws JsonParseException, JsonMappingException, IOException {

		//log json to filestore; mostly for debugging but these will be cleaned up as part of the cleanup task
		Path[] jsonLogFiles = new Path[] {null, null};
		try {
			jsonLogFiles = getJsonFileNames();
			Files.write(jsonLogFiles[0], json.getBytes());
		}catch (Exception ex) {
			logger.log(Level.WARNING, "Unable to log GFW JSON data to filestore: " + ex.getMessage(), ex); //$NON-NLS-1$
		}
		
		
		List<Alert> createdAlerts = new ArrayList<>();
		
		ObjectMapper objectMapper = new ObjectMapper();
		HashMap<String, Object> jsonData  = objectMapper.readValue(json, HashMap.class);
	
		//alert description
		StringBuilder sbDescription = new StringBuilder();
		sbDescription.append("Global Forest Watch"); //$NON-NLS-1$
		if (jsonData.containsKey(ALERT_NAME_JSON_KEY)) {
			sbDescription.append(" ("); //$NON-NLS-1$
			sbDescription.append(jsonData.get(ALERT_NAME_JSON_KEY));
			sbDescription.append(")"); //$NON-NLS-1$
		}
		
		List<Map<String, Object>> alerts = null;
		if (jsonData.containsKey(DOWNLOAD_URLS_JSON_KEY)) {
			Map<String, Object> downloads = (Map<String, Object>) jsonData.get(DOWNLOAD_URLS_JSON_KEY);
			if (downloads.containsKey(JSONFILE_JSON_KEY)) {
				String alertsUrl =(String) downloads.get(JSONFILE_JSON_KEY);
				//reading alert data from url
				//logger.log(Level.WARNING, "Reading GFW JSON Alert Data From File: " + alertsUrl); //$NON-NLS-1$
				StringWriter sw = new StringWriter();
				try {
					URL url = new URL(alertsUrl);
					IOUtils.copy(url.openStream(), sw, StandardCharsets.UTF_8);
				}catch (Exception ex) {
					logger.log(Level.SEVERE, "Unable to read GFW JSON alert data from additional JSON file.",  ex); //$NON-NLS-1$
					throw ex;
				}
				
				//write json to log file
				try {
					if (jsonLogFiles[1] == null) {
						throw new Exception("Unable to determine log file for logging GFW data."); //$NON-NLS-1$
					}
					Files.write(jsonLogFiles[1], sw.toString().getBytes());
				}catch (Exception ex) {
					logger.log(Level.WARNING, "Unable to log GFW JSON data to filestore: " + ex.getMessage(), ex); //$NON-NLS-1$
				}
				
				if (!sw.toString().isEmpty()) {
					try {
						alerts = objectMapper.readValue(sw.toString(), List.class);
					}catch (Exception ex) {
						logger.log(Level.WARNING, "JSON data attempting to parse"); //$NON-NLS-1$
						logger.log(Level.WARNING, sw.toString());
						logger.log(Level.SEVERE, "Unable to read parse JSON data from additional JSON file.",  ex); //$NON-NLS-1$
						throw ex;
					}
				}else {
					logger.log(Level.WARNING, "No JSON data to parse"); //$NON-NLS-1$
				}
			}
		}
		
		if (alerts == null) {
			if (jsonData.containsKey(ALERTS_JSON_KEY)) {
				alerts = (List<Map<String, Object>>) jsonData.get(ALERTS_JSON_KEY);
			}
		}
		
		if (alerts != null) {
			for (Map<String, Object> alertMap: alerts) {
				
				Double lat = null;
				Double lng = null;
				String date = null;
				String time = null;
				LocalDateTime datetime = null;
				
				if (alertMap.containsKey(LATITUDE_JSON_KEY)) {
					lat = (Double)alertMap.get(LATITUDE_JSON_KEY);
				}
				
				if (alertMap.containsKey(LONGITUDE_JSON_KEY)) {
					lng = (Double)alertMap.get(LONGITUDE_JSON_KEY);
				}
				
				if (alertMap.containsKey(ACQ_DATE_JSON_KEY)) {
					date = (String)alertMap.get(ACQ_DATE_JSON_KEY);
				}
				
				if (alertMap.containsKey(ACQ_TIME_JSON_KEY)) {
					time = (String)alertMap.get(ACQ_TIME_JSON_KEY);
				}
				
				if (date != null && time != null) {
					DateTimeFormatter sdf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"); //$NON-NLS-1$
					try {
						datetime = LocalDateTime.parse(date + " " + time, sdf); //$NON-NLS-1$
					}catch (Exception ex) {
						logger.log(Level.WARNING, "Unable to parse datetime for alert: " + ex.getMessage(), ex); //$NON-NLS-1$
					}
				}
				
				if (datetime == null || lat == null || lng == null) continue;
				
				//create a new alert here; check for duplicates later
				Alert alert = new Alert();
				alert.setCa(null);
				alert.setCreatorUuid(gfw.getCreator().getUuid());
				alert.setSource(Alert.Source.GLOBALFORESTWATCH);
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
		}
		return createdAlerts;
	}
	
	/*
	 * compute the filename for logging json data to
	 */
	private Path[] getJsonFileNames() throws IOException {
		
		Path gfwDir = DataStoreManager.INSTANCE.getRootDirectory().resolve(LOG_DIRECTORY);
		if (!Files.exists(gfwDir)) Files.createDirectories(gfwDir);
		
		DateTimeFormatter df = DateTimeFormatter.ofPattern(DATE_FORMAT);
		String fileName = "gfw." + df.format(LocalDateTime.now()); //$NON-NLS-1$
		
		Path jsonFile = gfwDir.resolve(fileName + ".json"); //$NON-NLS-1$
		Path extraFile = gfwDir.resolve(fileName + "extra.json"); //$NON-NLS-1$
		int cnt = 1;
		while(Files.exists(jsonFile) && cnt < 5000) {
			jsonFile = gfwDir.resolve(fileName + "." + cnt + ".json"); //$NON-NLS-1$ //$NON-NLS-2$
			extraFile = gfwDir.resolve(fileName + ".extra." + cnt + ".json"); //$NON-NLS-1$ //$NON-NLS-2$
			cnt ++;
		}
		return new Path[] {jsonFile, extraFile};
	}

		
}

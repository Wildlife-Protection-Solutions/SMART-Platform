/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.smartcollect.json;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.json.simple.JSONObject;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.export.ScreensUtil;
import org.wcs.smart.cybertracker.importer.json.IJsonProcessor;
import org.wcs.smart.cybertracker.importer.json.JsonCtParser;
import org.wcs.smart.cybertracker.importer.json.UserCancelledException;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.observation.model.WaypointObservation;
import org.wcs.smart.observation.model.WaypointObservationGroup;
import org.wcs.smart.smartcollect.SmartCollectPlugIn;
import org.wcs.smart.smartcollect.connect.SmartCollectConnectClient;
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.model.SmartCollectUser;
import org.wcs.smart.smartcollect.model.SmartCollectUser.State;
import org.wcs.smart.smartcollect.model.SmartCollectWaypoint;
import org.wcs.smart.smartcollect.model.SmartCollectWaypointSource;
import org.wcs.smart.smartcollect.pkg.SmartCollectPackageManager;

/**
 * JSON processor for SMARTCollect data.  Assumes all data for an individual
 * waypoint is included in the same file in order.  More than one waypoint can 
 * be in the file but observations must be in order.
 * 
 * @author Emily
 *
 */
public class SmartCollectDataProcessor implements IJsonProcessor {

	private SmartCollectConnectClient client;
	
	private List<String> warnings;

	private HashMap<String, WaypointObservationGroup> obsgroupmapping;
	private Set<Waypoint> waypoints;
	
	//resize value for apply to all option
	private Point allSize = null;
	
	public enum ProcessingOption{
		LOADDATA,
		ACCEPTANDLOAD,
		BLACKLISTANDDISCARD,
		DISCARD,
		VERIFYREQUEUE,
		CANCEL
	}
	
	public SmartCollectDataProcessor() {
	}
	
	@Override
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception {
	
		warnings = new ArrayList<>();
		waypoints = new HashSet<>();
		
		List<JSONObject> processedFeatures = new ArrayList<JSONObject>();
		
		SmartCollectWaypoint currentWaypoint = null;
		
		//preprocess users
		Set<String> users = new HashSet<>();
		for (JSONObject feature : features){
			JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
			if (properties == null) continue;
			JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
			if (sighting == null) continue;				
			
			String type = (String) sighting.get(ScreensUtil.RESULT_DATATYPE);
			// Validate data type
			if (!SmartCollectPackageManager.SMARTCOLLECT_RESOURCE_ID.equalsIgnoreCase(type)) continue;
	
			
			String wpsource = ((String)sighting.get(SmartCollectPackageManager.USERNAMEMETADATA_KEY));
			if (wpsource == null) continue;
			users.add(wpsource);
		}
		
		if (users.isEmpty()) {
			return Collections.emptyList();
		}
		
		//check the state for the users
		Map<String, SmartCollectUser> userStatus = getUserState(users);
		//if we have new users or validaton pending then we need more information
		Set<SmartCollectUser> notok = new HashSet<>();
		for (SmartCollectUser user : userStatus.values()) {
			if (user.getState() == State.NEW || user.getState() == State.VALIDATION_PENDING) {
				notok.add(user);
			}
		}
		
		boolean discardall = false;
		
		if (!notok.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			for (SmartCollectUser u : notok) {
				sb.append(u.getSource() + "[" + u.getState() + "]"); //$NON-NLS-1$ //$NON-NLS-2$
				sb.append(", "); //$NON-NLS-1$
			}
			sb.delete(sb.length() - 2, sb.length());
			
			ProcessingOption[] cancel = {ProcessingOption.CANCEL};
			Display.getDefault().syncExec(()->{
				ValidationDialog dialog = new ValidationDialog(Display.getDefault().getActiveShell(), sb.toString());
				dialog.open();
				cancel[0] = dialog.getSelectedOption();
			});
			
			if (cancel[0] == ProcessingOption.CANCEL) {
				throw new UserCancelledException(Messages.SmartCollectDataProcessor_Cancelled);
			}else if (cancel[0] == ProcessingOption.LOADDATA) {
				//temporarily set to validated for the purposes of processing this dataset
				for (SmartCollectUser u : notok) u.setState(State.VALIDATED);
			}else if (cancel[0] == ProcessingOption.DISCARD) {
				discardall = true;
			}else if (cancel[0] == ProcessingOption.ACCEPTANDLOAD) {			
				for (SmartCollectUser u : notok) {
					try {
						validateUser(u);
					}catch (Exception ex) {
						throw new Exception(MessageFormat.format(Messages.SmartCollectDataProcessor_UserValidationFailed,u.getSource(),ex.getMessage()), ex);
					}
				}
			}else if (cancel[0] == ProcessingOption.BLACKLISTANDDISCARD) {
				for (SmartCollectUser u : notok) {
					try {
						blacklistUser(u);
					}catch (Exception ex) {
						throw new Exception(MessageFormat.format(Messages.SmartCollectDataProcessor_UserBlackListFailed,u.getSource(),ex.getMessage()), ex);
					}
				}
			}else if (cancel[0] == ProcessingOption.VERIFYREQUEUE) {
				for (SmartCollectUser u : notok) {
					try {
						sendEmailRequest(u);
					}catch (Exception ex) {
						throw new Exception(MessageFormat.format(Messages.SmartCollectDataProcessor_EmailSendFailed,u.getSource(),ex.getMessage()), ex);
					}
				}
				return Collections.emptyList();
			}
		}
		
		Map<String, Integer> blacklistCount = new HashMap<>();
		
		for (JSONObject feature : features){
			JsonCtParser parser = new JsonCtParser();
			if (JsonCtParser.isTrackPoint(feature)) continue;
			
			try{
				JSONObject properties = (JSONObject) feature.get(JsonCtParser.PROPERTIES_KEY);
				if (properties == null) continue;
				JSONObject sighting = (JSONObject)properties.get(JsonCtParser.SIGHTINGS_KEY);
				if (sighting == null) continue;				
				
				String type = (String) sighting.get(ScreensUtil.RESULT_DATATYPE);
				// Validate data type
				if (!SmartCollectPackageManager.SMARTCOLLECT_RESOURCE_ID.equalsIgnoreCase(type)) continue;
				
				processedFeatures.add(feature);
				
				if (discardall) {
					//flag this feature as processed 
					continue;
				}
				
				//read group id 
				String strGroupId = ((String) sighting.get(ScreensUtil.RESULT_SIGHTINGGROUPID)).trim();
				if (strGroupId == null || strGroupId.isEmpty()) throw new Exception("No group id provided for independent incident.  Incident cannot be loaded"); //$NON-NLS-1$
				
				String wpsource = ((String)sighting.get(SmartCollectPackageManager.USERNAMEMETADATA_KEY));
				if (wpsource == null) {
					warnings.add(Messages.SmartCollectDataProcessor_NoUserForFeature + feature.toString());
					continue;
				}
			
				if (userStatus.get(wpsource).getState() == State.BLACKLISTED) {
					Integer cnt = blacklistCount.get(wpsource);
					if (cnt == null) {
						blacklistCount.put(wpsource, 1);
					}else {
						blacklistCount.put(wpsource, cnt+ 1);
					}
					
					continue;
				}
				
				//parse waypoint
				Waypoint parsedWp = parser.createWaypoint(feature, SmartDB.getCurrentConservationArea(), session);
				allSize = JsonCtParser.processImages(parsedWp, allSize, session);
				warnings.addAll(parser.getWarnings());

				if (currentWaypoint == null || (boolean)sighting.get(ScreensUtil.RESULT_NEW_WAYPOINT)) {
					currentWaypoint = new SmartCollectWaypoint();
					currentWaypoint.setWaypoint(parsedWp);
					currentWaypoint.setSource(wpsource);
					
					parsedWp.setConservationArea(SmartDB.getCurrentConservationArea());
					parsedWp.setSourceId(SmartCollectWaypointSource.KEY);
					
					int id = IncidentManager.getInstance().getNextIncidentId(session);
					currentWaypoint.getWaypoint().setId(id);
					
					obsgroupmapping = new HashMap<>();
					obsgroupmapping.put(strGroupId, currentWaypoint.getWaypoint().getObservationGroups().get(0));
				}else {
					WaypointObservationGroup group = obsgroupmapping.get(strGroupId);
					if (group == null) {
						group = new WaypointObservationGroup();
						group.setWaypoint(currentWaypoint.getWaypoint());
						currentWaypoint.getWaypoint().getObservationGroups().add(group);
						group.setObservations(new ArrayList<>());
						obsgroupmapping.put(strGroupId, group);
					}
					
					for (WaypointObservation wo : parsedWp.getAllObservations()) {
						group.getObservations().add(wo);
						wo.setObservationGroup(group);
					}
				}
				session.saveOrUpdate(currentWaypoint.getWaypoint());
				session.saveOrUpdate(currentWaypoint);
				waypoints.add(currentWaypoint.getWaypoint());
				
			}catch (Exception ex) {
				SmartCollectPlugIn.log(ex.getMessage() + ": " + feature.toJSONString(), ex); //$NON-NLS-1$
				warnings.add(Messages.SmartCollectDataProcessor_ParseError + ex.getMessage());
			}
		}
		
		if (discardall) {
			warnings.add(MessageFormat.format(Messages.SmartCollectDataProcessor_FeaturesDiscared, processedFeatures.size()));
		}
		for (Entry<String, Integer> bcnt : blacklistCount.entrySet()) {
			warnings.add(MessageFormat.format(Messages.SmartCollectDataProcessor_UserBlacklistedFeaturesDiscarded, bcnt.getKey(), bcnt.getValue()));
		}
		//display warnings to user; this may throw a cancelled exception if the user doesn't want to proceed
		displayWarnings(warnings);
				
		return processedFeatures;		
				
	}

	@Override
	public void afterSave() {
		for (Waypoint p : waypoints){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_MODIFIED, p);
			}catch (Exception ex){
				SmartCollectPlugIn.displayLog(Messages.SmartCollectDataProcessor_EventError + ex.getMessage(), ex);
			}
		}
	}

	@Override
	public String getStatusMessage() {
		if (waypoints.isEmpty() ) return null;
		
		StringBuilder sb = new StringBuilder();
		if (!waypoints.isEmpty()){
			sb.append(MessageFormat.format(Messages.SmartCollectDataProcessor_CreatedMessage, waypoints.size()));
			sb.append("("); //$NON-NLS-1$
			for(Waypoint p : waypoints){
				sb.append(p.getId());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			sb.append(") "); //$NON-NLS-1$
		}
		
		return sb.toString();
	}

	
	/*
	 * displays warning dialog to user allowing them to cancel the processing
	 */
	private void displayWarnings(List<String> warnings) throws Exception{
		 if (!warnings.isEmpty()){
			 	final boolean[] cont = {false};
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), 
								Messages.SmartCollectDataProcessor_WarningsTitle, 
								Messages.SmartCollectDataProcessor_WarningsMessage,
								warnings,
								new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
						if (wd.open() == 0){
							cont[0] = true;
						}else{
							cont[0] = false;
						}
					}	
				});
				if (!cont[0]){
					throw new UserCancelledException(Messages.SmartCollectDataProcessor_Cancelled);
				}
		 }
	}
	
	
	private Map<String, SmartCollectUser> getUserState(Set<String> users) {
		
		SmartConnect[] connect = {null};

		try {
			ConnectServer cs = null;
			ConnectUser user = null;
			try(Session session = HibernateManager.openSession()){
				cs = ConnectHibernateManager.getConnectServer(session);
				user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), session);			
			}catch (Exception ex) {
				throw ex;
			}
			if (user.getConnectPassword().isBlank()) throw new Exception();
		
			SmartConnect temp = SmartConnect.findInstance(cs, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
			if (temp == null) throw new Exception();
			
			String error = temp.validateUser();
			if (error != null) throw new Exception();
			
			connect[0] = temp;
		}catch (Exception ex){
			
		}
		
		if (connect[0] == null) {
			//prompt for server details
			Display.getDefault().syncExec(()->{
				ConnectDialog cd = new ConnectDialog(Display.getCurrent().getActiveShell(), true) {
					@Override
					protected Control createDialogArea(Composite parent) {
						setTitle(Messages.SmartCollectDataProcessor_Title);
						getShell().setText(Messages.SmartCollectDataProcessor_Title);
						setMessage(Messages.SmartCollectDataProcessor_Message);	
						return super.createDialogArea(parent);
					}
				};
				if (cd.open() == Window.OK) {
					connect[0] = cd.getConnection();
				}
			});
		}
		
		if (connect[0] == null) return null;
		
		
		ResteasyClient rclient = connect[0].getClient();
		ResteasyWebTarget target = rclient.target(connect[0].getServer().getServerUrl() + SmartConnect.API_URL);
		client = target.proxy(SmartCollectConnectClient.class);
		
		Map<String, SmartCollectUser> cusers = new HashMap<>();
		for (String user : users) {
			List<SmartCollectUser> cuser = client.getUser(user);
			for (SmartCollectUser i : cuser) cusers.put(i.getSource(),i);
		}
		
		return cusers;
	}
	
	private void sendEmailRequest(SmartCollectUser user) {
		try(Response r = client.updateUserState(user.getUuid().toString(), SmartCollectUser.State.VALIDATED.name(), Boolean.TRUE.toString())){}
	}
	
	private void validateUser(SmartCollectUser user) {
		try(Response r = client.updateUserState(user.getUuid().toString(), SmartCollectUser.State.VALIDATED.name(), Boolean.FALSE.toString())){
			user.setState(State.VALIDATED);
		}
	}
	
	private void blacklistUser(SmartCollectUser user) {
		try(Response r = client.updateUserState(user.getUuid().toString(), SmartCollectUser.State.BLACKLISTED.name(), Boolean.FALSE.toString())){
			user.setState(State.BLACKLISTED);
		}
	}
}

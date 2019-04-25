package org.wcs.smart.connect.cybertracker.ctpackage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.UuidUtils;

public class AlertUtils {
	
	private static final String LABEL_KEY = "label"; //$NON-NLS-1$
	private static final String UUID_KEY = "uuid"; //$NON-NLS-1$
	
	public static void cacheAlertTypes(List<AlertType> types) {
		JSONArray array = new JSONArray();
		for (AlertType a : types){
			JSONObject obj = new JSONObject();
			obj.put(UUID_KEY, a.getUuid().toString());
			obj.put(LABEL_KEY, a.getLabel());
			array.add(obj);
		}
		ConnectPlugIn.getDefault().getPreferenceStore().setValue(getPreferenceKey(), array.toJSONString());
	}
	
	public static ArrayList<AlertType> getCachedAlertTypes(){
		
		String types = ConnectPlugIn.getDefault().getPreferenceStore().getString(getPreferenceKey());
		if (types == null || types.isEmpty()){
			//nothing found - empty list
			return null;
		}else{
			ArrayList<AlertType> newAlerts = new ArrayList<AlertType>();
			JSONParser parser = new JSONParser();
			JSONArray array;
			try {
				array = ((JSONArray )parser.parse(types));
				for (Iterator<?> iterator = array.iterator(); iterator.hasNext();) {
					JSONObject object = (JSONObject)iterator.next();
					
					UUID uuid = UUID.fromString((String)object.get(UUID_KEY));
					String label = (String)object.get(LABEL_KEY);
					AlertType type = new AlertType();
					type.setUuid(uuid);
					type.setLabel(label);
					newAlerts.add(type);
				}
			} catch (ParseException e) {
				ConnectPlugIn.log("Error parsing alert types from preference store.", e); //$NON-NLS-1$
			}
			return newAlerts;
		}
		
	}
	
	private static String getPreferenceKey(){
		String cauuid = UuidUtils.uuidToString(SmartDB.getCurrentConservationArea().getUuid());
		return ConnectPlugIn.CONNECT_ALERT_TYPE_CACHE_PREF + "." + cauuid; //$NON-NLS-1$
	}
}

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

/**
 * Utilities for managing cached alert types across sessions
 * and conservation areas
 * 
 * @author Emily
 *
 */
public class AlertUtils {
	
	private static final String LABEL_KEY = "label"; //$NON-NLS-1$
	private static final String UUID_KEY = "uuid"; //$NON-NLS-1$
	
	@SuppressWarnings("unchecked")
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

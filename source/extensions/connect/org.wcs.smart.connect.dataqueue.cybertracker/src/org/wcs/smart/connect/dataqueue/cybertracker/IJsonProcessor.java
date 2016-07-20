package org.wcs.smart.connect.dataqueue.cybertracker;

import java.util.List;

import org.hibernate.Session;
import org.json.simple.JSONObject;


public interface IJsonProcessor {

	public static final String EXTENSION_ID = "org.wcs.smart.connect.dataqueue.cybertracker.json.processor"; //$NON-NLS-1$
	/**
	 * @param features
	 * @return a list of objects that have been processed
	 */
	public List<JSONObject> processJson(List<JSONObject> features, Session session) throws Exception;
}

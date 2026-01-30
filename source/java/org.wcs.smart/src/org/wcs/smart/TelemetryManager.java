/*
 * Copyright (C) 2026 Wildlife Conservation Society
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
package org.wcs.smart;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.hibernate.Session;
import org.osgi.service.prefs.BackingStoreException;
import org.wcs.smart.hibernate.HibernateManager;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import jakarta.persistence.Tuple;

public enum TelemetryManager {

	INSTANCE;

	private static final String NODE_PATH = "org.wcs.smart.telemetry"; //$NON-NLS-1$
	private static final String TELEMETRY_PATH = "org.wcs.smart.telemetry.statistics"; //$NON-NLS-1$

	private static final String INSTALL_KEY_NAME = "installKey"; //$NON-NLS-1$
	private static final String ENABLED_KEY_NAME = "telemetry"; //$NON-NLS-1$
	private static final String LASTUPLOADED_KEY_NAME = "lastuploaded"; //$NON-NLS-1$

	public enum Key {

		CA_LOGIN("usage.ca.login.count"),  //$NON-NLS-1$
		PATROL_VIEW("usage.patrol.view.count"), //$NON-NLS-1$
		MISSION_VIEW("usage.mission.view.count"),  //$NON-NLS-1$
		INCIDENT_VIEW("usage.incident.view.count"), //$NON-NLS-1$
		SMARTCOLLECT_VIEW("usage.smartcollect.view.count"),  //$NON-NLS-1$
		FIELDSENSOR_VIEW("usage.fieldsensor.view.count"), //$NON-NLS-1$
		PLAN_VIEW("usage.plan.view.count"),  //$NON-NLS-1$

		MANUAL_QA_RUN("usage.qa.manual.run.count"),  //$NON-NLS-1$
		AUTO_QA_VIEW("usage.qa.auto.view.count"), //$NON-NLS-1$

		RUN_QUERY("usage.query.run.count"),  //$NON-NLS-1$
		RUN_R_QUERY("usage.query.run.count.r"), //$NON-NLS-1$
		RUN_REPORT("usage.report.run.count"), //$NON-NLS-1$

		RUN_CONNECT_SYNC("usage.connect.sync.manual.count"),  //$NON-NLS-1$
		RUN_CONNECT_UPSYNC("usage.connect.upsync.manual.count"),  //$NON-NLS-1$
		RUN_CONNECT_DOWNSYNC("usage.connect.downsync.manual.count"), //$NON-NLS-1$
		RUN_CONNECT_AUTOSYNC("usage.connect.autosync.manual.count"), //$NON-NLS-1$
		
		RUN_CONNECT_DATAQUEUE_PROCESSING("usage.connect.dataqueue.itemprocessed.count"), //$NON-NLS-1$
		
		RUN_CONNECT_RECOVER("usage.connect.recover.count"), //$NON-NLS-1$
		
		PROFILE_ENTITY_VIEW("usage.profile.entity.view.count"),  //$NON-NLS-1$
		PROFILE_RECORD_VIEW("usage.profile.record.view.count"), //$NON-NLS-1$
		PROFILE_WORKING_SET_VIEW("usage.profile.workingset.view.count");  //$NON-NLS-1$

		String dbKey;

		Key(String key) {
			this.dbKey = key;
		}
	}

	private HashMap<String, Key> mapId2Key = new HashMap<>();

	private BlockingQueue<String> statsToadd = new LinkedBlockingQueue<String>();
	
	private boolean isEnabled = false;
	
	//the datetime the installkey was set; if set on this launch otherwise null
	private LocalDateTime keySet = null;
	
	private IEclipsePreferences telemetryStore;
	
	private Job addStatisticJob = new Job("updating statistics") {  //$NON-NLS-1$
		
		@Override
		protected IStatus run (IProgressMonitor monitor) {
			
			while(!monitor.isCanceled()) {
				String key;
				try {
					key = statsToadd.take();
				} catch (InterruptedException e) {
					return Status.CANCEL_STATUS;
				}
				
				if (key == null) continue;
			
				LocalDate now = LocalDate.now();
				String thismonth = now.getYear() + "_" + now.getMonthValue();  //$NON-NLS-1$
			
				key = key + "." + thismonth;  //$NON-NLS-1$ 
						
				int cnt = telemetryStore.getInt(key, 0);
				telemetryStore.putInt(key, cnt+1);
				flushStore(telemetryStore);
				
			}
			return Status.OK_STATUS;
		}
	};

	
	private TelemetryManager() {
		this.addStatisticJob.setSystem(true);
	
		this.telemetryStore = ConfigurationScope.INSTANCE.getNode(TELEMETRY_PATH);

		computeEnabled();
		getInstallKey();
	}

	/**
	 * If this is the first time accessing the telemetry store. A message will be displayed to the user
	 * warning them. After that the install key is set and users will not be notified anymore.
	 * @return
	 */
	public boolean isFirst() {
		return this.keySet != null;
	}
	
	
	public void shutdown() {		
		addStatisticJob.cancel();
		if (addStatisticJob.getThread() != null) addStatisticJob.getThread().interrupt();
	}
	
	/**
	 * called when a part is opened - will increment the statistic associated with the part
	 * @param partId
	 */
	public void trackPartOpened(String partId) {
		if (!mapId2Key.containsKey(partId))
			return;
		this.incrementStatistic(mapId2Key.get(partId));
	}

	/**
	 * registers a part id with the statistic collector; whenever the part is opened the
	 * statistic will be incremented
	 * 
	 * @param partId
	 * @param key
	 */
	public void registerPartId(String partId, Key key) {
		mapId2Key.put(partId, key);
	}

	/**
	 * increment the statistic 
	 * @param key the statistic key
	 * @param subkey statistic subkey - used for queries to differentiate specific query types
	 */
	public void incrementStatistic(TelemetryManager.Key key, String subkey) {
		if (!this.isEnabled()) return;
		
		//put this in a queue 
		String thiskey = key.dbKey + (subkey == null ? "" : "." + subkey);  //$NON-NLS-1$  //$NON-NLS-2$
		statsToadd.add(thiskey);
		if (addStatisticJob.getState() != Job.RUNNING) {
			addStatisticJob.schedule();
		}
	}

	/**
	 * increment the statistic 
	 * @param key the statistic key
	 */
	public void incrementStatistic(TelemetryManager.Key key) {
		this.incrementStatistic(key, null);

	}

	/**
	 * 
	 * @return if telemetry data collection is enabled
	 */
	public boolean isEnabled() {
		return this.isEnabled;
	}
	
	/*
	 * flushes preference store writing errors to log file and returning true/false
	 */
	private boolean flushStore(IEclipsePreferences store) {
		try {
			store.flush();
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			return false;
		}
		return true;
	}
	
	/**
	 * Reads and initializes the enabled status from the preference store.
	 */
	private void computeEnabled() {
		
		IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(NODE_PATH);
		if (prefs.get(ENABLED_KEY_NAME, null) == null) {
			prefs.putBoolean(ENABLED_KEY_NAME, true);
			flushStore(prefs);			
		}
		this.isEnabled = prefs.getBoolean(ENABLED_KEY_NAME, true);
	}
	
	/**
	 * Updates if telemetry data is collected or not 
	 * @param enabled 
	 * @return true if update successful, false otherwise
	 */ 
	public boolean setEnabled(boolean enabled) {
		if (this.isEnabled == enabled) return true;
		
		//store preference
		IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(NODE_PATH);		
		prefs.putBoolean(ENABLED_KEY_NAME, enabled);
		try {
			prefs.flush();
		
			if (!enabled) {				
				telemetryStore.clear();
				telemetryStore.flush();
			}
		}catch (BackingStoreException ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			return false;
		}
		
		this.computeEnabled();
		
		if (this.isEnabled) {
			this.startDispatcher();
		}else {
			this.stopDispatcher();
		}
		
		return true;
	}
	
	/**
	 * If stats are enabled, and we can upload 
	 */
	public boolean canUploadStats() {
		if (!this.isEnabled) return false;
		//this gives users 12 hours to opt out of telemetry sending 
		//when first launched
		return this.keySet == null || this.keySet.isBefore(LocalDateTime.now().minusHours(12));		
	}

	
	/*
	 * computes the install key and stores it in the preferences store
	 */
	public String getInstallKey() {
		IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(NODE_PATH);
		String key = prefs.get(INSTALL_KEY_NAME, null);

		if (key == null) {
			this.keySet = LocalDateTime.now(); 
			// generate a new install key
			key = UUID.randomUUID().toString();
			prefs.put(INSTALL_KEY_NAME, key);

			if (!flushStore(prefs)) {
				return null;
			}
		}
		return key;
	}
	
	public void setLastUploaded(LocalDateTime date) {
		IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(NODE_PATH);
		prefs.put(LASTUPLOADED_KEY_NAME, date.toString());
		flushStore(prefs);
	}
	
	public LocalDateTime getLastUploaded() {
		IEclipsePreferences prefs = ConfigurationScope.INSTANCE.getNode(NODE_PATH);
		String key = prefs.get(LASTUPLOADED_KEY_NAME, null);
		if (key == null) return null;
		try {
			return LocalDateTime.parse(key);
		}catch (Exception ex) {
			SmartPlugIn.log(ex.getMessage(), ex);
			return null;
		}
	}

	/**
	 * Packages the telemetry data into a json string. will return null if
	 * telemetry not enabled.
	 * 
	 * @param prettyPrint if the compressed or pretty json
	 * @return
	 */
	public String packageData(boolean prettyPrint) {

		if (!this.isEnabled()) return null;
		
		if (getInstallKey() == null) return "{\"error\": \"cannot configure install key\"}"; //$NON-NLS-1$ 
		
		JsonObject jinfo = new JsonObject();
		try (Session session = HibernateManager.openSession()) {

			jinfo.addProperty("installKey", getInstallKey()); //$NON-NLS-1$
			jinfo.addProperty("datetime", Instant.now().toString()); //$NON-NLS-1$
			jinfo.addProperty("os.name", System.getProperty("os.name")); //$NON-NLS-1$ //$NON-NLS-2$
			jinfo.addProperty("os.version", System.getProperty("os.version")); //$NON-NLS-1$ //$NON-NLS-2$
			jinfo.addProperty("os.arch", System.getProperty("os.arch")); //$NON-NLS-1$ //$NON-NLS-2$

			jinfo.addProperty("smart.version", System.getProperty("org.wcs.smart.version.simple")); //$NON-NLS-1$ //$NON-NLS-2$

			JsonObject jversions = new JsonObject();
			jinfo.add("db_version", jversions); //$NON-NLS-1$

			HashMap<String, String> dbVersions = HibernateManager.getPlugInVersions(session);
			dbVersions.forEach((key, value) -> jversions.addProperty(key, value));

			JsonObject jstats = new JsonObject();
			jinfo.add("stats", jstats); //$NON-NLS-1$

			try {
			for (String key : telemetryStore.keys()) {
				int index = key.lastIndexOf("."); //$NON-NLS-1$
				if (index < 0) continue;
				int value = telemetryStore.getInt(key, 0);
				String thiskey = key.substring(0, index);
				String month = key.substring(index + 1);
				
				key = "usage." + key; //$NON-NLS-1$
				JsonObject monthly = (JsonObject) jstats.get(thiskey);
				if (monthly == null) {
					monthly = new JsonObject();
					jstats.add(thiskey, monthly);
				}
				monthly.addProperty(month, value);
			}
			}catch (Exception ex) {
				SmartPlugIn.log(ex.getMessage(), ex);
				jstats.addProperty("usage.error", ex.getMessage()); //$NON-NLS-1$
			}
			
			// waypoint counts by type
			List<Tuple> wpcnt = session
					.createQuery("SELECT sourceId, count(*) from Waypoint GROUP BY sourceId", Tuple.class).list(); //$NON-NLS-1$
			for (Tuple x : wpcnt) {
				String key = "data.waypoint." + x.get(0).toString().toLowerCase(); //$NON-NLS-1$
				Long value = (Long) x.get(1);
				jstats.addProperty(key, value);
			}

			// count of all tables in the smart schema
			StringBuilder sb = new StringBuilder();
			sb.append(
					"SELECT 'SELECT count(*), ''' || s.schemaname || '.' || t.tablename || ''' FROM ' || s.schemaname || '.' || t.tablename "); //$NON-NLS-1$
			sb.append(" FROM sys.SYSTABLES t join sys.SYSSCHEMAS s on t.schemaid = s.schemaid "); //$NON-NLS-1$
			sb.append(" WHERE s.schemaname = 'SMART'"); //$NON-NLS-1$

			List<String> tables = session.createNativeQuery(sb.toString(), String.class).list();
			StringJoiner joiner = new StringJoiner(" UNION "); //$NON-NLS-1$
			tables.forEach(s -> joiner.add(s));

			List<Tuple> tableCnts = session.createNativeQuery(joiner.toString(), Tuple.class).list();
			for (Tuple x : tableCnts) {
				String tname = x.get(1).toString().toLowerCase().trim();
				Integer cnt = (Integer) x.get(0);

				String key = "data.table." + tname; //$NON-NLS-1$
				jstats.addProperty(key, cnt);
			}

			// configuration model details

			// attribute options
			sb = new StringBuilder();
			sb.append(
					"select case when number_value = 1 then 'true' else 'false' end , count(*), 'multiselect' From smart.CM_ATTRIBUTE_OPTION where option_id = 'MULTISELECT' group by number_value"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when number_value = 1 then 'true' else 'false' end, count(*), 'numeric' From smart.CM_ATTRIBUTE_OPTION where option_id = 'NUMERIC' group by number_value"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when number_value = 1 then 'true' else 'false' end, count(*), 'flatten_tree' From smart.CM_ATTRIBUTE_OPTION where option_id = 'FLATTEN_TREE' group by number_value"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when number_value = 1 then 'true' else 'false' end, count(*), 'input_group' From smart.CM_ATTRIBUTE_OPTION where option_id = 'INPUT_GROUP' group by number_value"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when number_value = 1 then 'true' else 'false' end, count(*), 'is_visible' From smart.CM_ATTRIBUTE_OPTION where option_id = 'IS_VISIBLE' group by number_value"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select string_value, count(*), 'enter_once' From smart.CM_ATTRIBUTE_OPTION where option_id = 'ENTER_ONCE' group by string_value"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select cast(null as varchar(10000)), count(*), 'help_text' From smart.CM_ATTRIBUTE_OPTION where option_id = 'HELP_TEXT' and string_value is not null"); //$NON-NLS-1$

			List<Tuple> cmstats = session.createNativeQuery(sb.toString(), Tuple.class).list();
			for (Tuple x : cmstats) {
				String last = x.get(2).toString().toLowerCase().trim();
				String first = (String) x.get(0);
				if (first != null) first = first.toLowerCase().trim();			
				String key = "data.cm.attribute." + last + (first != null? "." + first : "");						 //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Integer cnt = (Integer) x.get(1);
				jstats.addProperty(key, cnt);
			}

			// cm node/category options
			sb = new StringBuilder();
			sb.append(
					"select case when display_mode is null then 'default' else display_mode end, count(*), 'display_mode' from smart.cm_node group by display_mode"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when collect_multiple_obs then 'true' else 'false' end, count(*), 'collect_multiple' from smart.cm_node where category_uuid is not null group by collect_multiple_obs"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when photo_allowed then 'true' else 'false' end, count(*), 'photo_allowed' from smart.cm_node where category_uuid is not null group by photo_allowed"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when photo_required then 'true' else 'false' end, count(*), 'photo_required' from smart.cm_node where category_uuid is not null group by photo_required"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select cast(null as varchar(10000)), count(*), 'signatures' from smart.cm_node where category_uuid is not null and signatures is not null"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select cast(null as varchar(10000)), count(*), 'attachment_tags' from smart.cm_node where category_uuid is not null and attachment_tags is not null"); //$NON-NLS-1$

			cmstats = session.createNativeQuery(sb.toString(), Tuple.class).list();
			for (Tuple x : cmstats) {
				String last = x.get(2).toString().toLowerCase().trim();
				String first = (String) x.get(0);
				if (first != null) first = first.toLowerCase().trim();		
				String key = "data.cm.category." + last + (first != null? "." + first : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Integer cnt = (Integer) x.get(1);
				jstats.addProperty(key, cnt);
			}

			// cm options
			sb = new StringBuilder();
			sb.append(
					"select case when use_earth_ranger then 'true' else 'false' end , count(*), 'use_earth_ranger' from smart.CONFIGURABLE_MODEL group by use_earth_ranger"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when photo_first then 'true' else 'false' end , count(*), 'photo_first' from smart.CONFIGURABLE_MODEL group by photo_first"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when instant_gps then 'true' else 'false' end , count(*), 'instant_gps' from smart.CONFIGURABLE_MODEL group by instant_gps"); //$NON-NLS-1$
			sb.append(" union "); //$NON-NLS-1$
			sb.append(
					"select case when display_mode is null then 'default' else display_mode  end , count(*), 'display_mode' from smart.CONFIGURABLE_MODEL group by display_mode"); //$NON-NLS-1$

			cmstats = session.createNativeQuery(sb.toString(), Tuple.class).list();
			for (Tuple x : cmstats) {
				String last = x.get(2).toString().toLowerCase().trim();
				String first = (String) x.get(0);
				if (first != null) first = first.toLowerCase().trim();				
				String key = "data.cm.cm." + last + (first != null? "." + first : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				Integer cnt = (Integer) x.get(1);
				jstats.addProperty(key, cnt);
			}
		}
		
		GsonBuilder builder = new GsonBuilder();
		if (prettyPrint) {
			builder.setPrettyPrinting();
		}
		return builder.create().toJson(jinfo);

	}

	/**
	 * Starts the job that send statistics to the server
	 */
	public void startDispatcher() {
		TelemetryDispatcherJob.INSTANCE.schedule();
	}

	/**
	 * Stops the job that send statistics to the server
	 */
	public void stopDispatcher() {
		TelemetryDispatcherJob.INSTANCE.cancel();
	}
	
}

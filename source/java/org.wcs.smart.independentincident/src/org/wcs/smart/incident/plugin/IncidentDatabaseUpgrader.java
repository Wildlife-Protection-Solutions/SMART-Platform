/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.incident.plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;

import com.ibm.icu.text.MessageFormat;

/**
 * Installer/upgrader for incident plugin
 * @since 8.0.0
 */
public class IncidentDatabaseUpgrader implements IDatabaseUpgrader {

	public IncidentDatabaseUpgrader() {
	}

	@Override
	public String getPluginId() {
		return IncidentPlugIn.PLUGIN_ID;
	}

	@Override
	public String getPluginName() {
		return getName(IncidentPlugIn.getDefault().getBundle());
	}

	@Override
	public boolean isUpdateToDate(Map<String, String> currentVersions) {
		if (!currentVersions.containsKey(IncidentPlugIn.PLUGIN_ID)) return false;
		return currentVersions.get(IncidentPlugIn.PLUGIN_ID).equals(IncidentPlugIn.DB_VERSION);
		
	}

	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(PROGRESS_MESSAGE,  getPluginName()));
		
		try(Session session = HibernateManager.openSession()){
			session.beginTransaction();
			try {
				
				Map<String, String> versions = UpgradeEngine.getVersions(session);
				upgrade(versions.get(IncidentPlugIn.PLUGIN_ID), session);
				
				session.getTransaction().commit();
			}catch (Exception ex){
				session.getTransaction().rollback();
				throw ex;
			}
		}
		monitor.done();
	}

	/**
	 * Upgrades from the currentVersion to the most recent version.
	 * @param currentVersion
	 * @param session is active transaction
	 */
	private void upgrade(String currentVersion, Session session){
		if (currentVersion == null) {
			createTables(session);
			upgradeV1toV2(session);
		}else if (currentVersion.equalsIgnoreCase(IncidentPlugIn.DB_VERSION_1)){
			upgradeV1toV2(session);
		}
	}
	
	private void upgradeV1toV2(Session session) {

		String[] sql = {
				"create table smart.incident_type(uuid char(16) for bit data not null, ca_uuid char(16) for bit data not null, keyid varchar(128) not null, is_active boolean not null default true, icon_uuid char(16) for bit data, options varchar(32672), fallback_type_uuid char(16) for bit data, primary key (uuid) )", //$NON-NLS-1$

				"ALTER TABLE smart.incident_type add constraint incident_type_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$

				"ALTER TABLE smart.incident_type add constraint incident_unq_chk UNIQUE (ca_uuid, keyid)", //$NON-NLS-1$

				"alter table smart.icon drop constraint ICON_CAUUID_FK", //$NON-NLS-1$
				"ALTER TABLE smart.incident_type add constraint incident_type_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) on delete set null on update restrict deferrable initially immediate", //$NON-NLS-1$
				"alter table smart.icon add constraint ICON_CAUUID_FK foreign key (ca_uuid) references smart.conservation_area(uuid) on update restrict on delete cascade deferrable initially immediate", //$NON-NLS-1$

				"CREATE FUNCTION smart.uuidtemp() returns char(16) for bit data LANGUAGE JAVA NOT deterministic external name 'org.wcs.smart.util.DerbyUtils.createUuid' PARAMETER STYLE JAVA NO SQL RETURNS NULL ON NULL INPUT", //$NON-NLS-1$

				"insert into smart.incident_type(uuid, ca_uuid, keyid) select smart.uuidtemp(), uuid, 'incident' from smart.conservation_area where uuid != x'00000000000000000000000000000000'", //$NON-NLS-1$
				"insert into smart.incident_type(uuid, ca_uuid, keyid) select smart.uuidtemp(), uuid, 'integrate' from smart.conservation_area where uuid != x'00000000000000000000000000000000'", //$NON-NLS-1$
				"insert into smart.incident_type(uuid, ca_uuid, keyid, options) select smart.uuidtemp(), uuid, 'integratelink', 'linkpatrol' from smart.conservation_area where uuid != x'00000000000000000000000000000000'", //$NON-NLS-1$
				"insert into smart.incident_type(uuid, ca_uuid, keyid, options) select smart.uuidtemp(), uuid, 'integratemove', 'movepatrol' from smart.conservation_area where uuid != x'00000000000000000000000000000000'", //$NON-NLS-1$

				"update smart.incident_type set fallback_type_uuid = (select a.uuid from smart.incident_type a where a.ca_uuid = smart.incident_type.ca_uuid and a.keyid='integrate') where keyid in ('integratelink', 'integratemove')", //$NON-NLS-1$
				
				"update smart.waypoint set incident_type_uuid = (select a.uuid from smart.incident_type a where a.ca_uuid = smart.waypoint.ca_uuid and a.keyid = 'incident') where source = 'INDINC'", //$NON-NLS-1$
				"update smart.waypoint set incident_type_uuid = (select a.uuid from smart.incident_type a where a.ca_uuid = smart.waypoint.ca_uuid and a.keyid = 'integrate') where source = 'INTEGRATE'", //$NON-NLS-1$
				"update smart.waypoint set incident_type_uuid = (select a.uuid from smart.incident_type a where a.ca_uuid = smart.waypoint.ca_uuid and a.keyid = 'integratelink') where source = 'INTEGRATEPLLINK'", //$NON-NLS-1$
				"update smart.waypoint set incident_type_uuid = (select a.uuid from smart.incident_type a where a.ca_uuid = smart.waypoint.ca_uuid and a.keyid = 'integratemove') where source = 'INTEGRATEPATROL'", //$NON-NLS-1$
				
				"update smart.waypoint set source = 'INDINC' where source in ('INTEGRATE', 'INTEGRATEPLLINK', 'INTEGRATEPATROL')", //$NON-NLS-1$
				
				"DROP FUNCTION smart.uuidtemp", //$NON-NLS-1$
				
		};

		//translations
		String[][] data = new String[][]{
				{"incident", "en", "Independent Incident"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "ar", "\u062d\u0627\u062f\u062b \u0645\u0633\u062a\u0642\u0644"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "es", "Incidente Independiente"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "fr", "Incident Independant"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "in", "Insiden Independen"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "ka", "\u10d3\u10d0\u10db\u10dd\u10e3\u10d9\u10d8\u10d3\u10d4\u10d1\u10d4\u10da\u10d8 \u10d8\u10dc\u10ea\u10d8\u10d3\u10d4\u10dc\u10e2\u10d8"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "km", "\u17a7\u1794\u17d2\u1794\u178f\u17d2\u178f\u17b7\u17a0\u17c1\u178f\u17bb\u17af\u1780\u179a\u17b6\u1787\u17d2\u1799"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "mn", "\u0411\u0438\u0435 \u0434\u0430\u0430\u0441\u0430\u043d \u0445\u044d\u0440\u044d\u0433"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "pt", "Incidente Independente"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "ru", "\u041e\u0431\u043e\u0441\u043e\u0431\u043b\u0435\u043d\u043d\u044b\u0439 \u0438\u043d\u0446\u0438\u0434\u0435\u043d\u0442"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "sw", "Tukio huru"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "th", "\u0e40\u0e2b\u0e15\u0e38\u0e01\u0e32\u0e23\u0e13\u0e4c\u0e2d\u0e34\u0e2a\u0e23\u0e30"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "uk", "\u041d\u0435\u0437\u0430\u043b\u0435\u0436\u043d\u0438\u0439 \u0456\u043d\u0446\u0438\u0434\u0435\u043d\u0442"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "vi", "V\u1ee5 vi\u1ec7c \u0111\u1ed9c l\u1eadp"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"incident", "zh", "\u72ec\u7acb\u4e8b\u4ef6"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				{"integrate", "en", "SMART Integrate Incident"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integrate", "es", " Incidente de SMART Integrate"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integrate", "in", " Insiden Integrasi SMART"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integrate", "pt", " Incidente Integrado SMART"},  //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integrate", "ru", " \u0418\u043d\u0446\u0438\u0434\u0435\u043d\u0442 SMART Integrate"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integrate", "th", " \u0e40\u0e2b\u0e15\u0e38\u0e01\u0e32\u0e23\u0e13\u0e4c\u0e23\u0e27\u0e21\u0e02\u0e2d\u0e07\u0e2a\u0e21\u0e32\u0e23\u0e4c\u0e17"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integrate", "uk", " \u0406\u043d\u0442\u0435\u0433\u0440\u043e\u0432\u0430\u043d\u0438\u0439 SMART \u0406\u043d\u0446\u0438\u0434\u0435\u043d\u0442"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integrate", "vi", " S\u1ef1 c\u1ed1 t\u00edch h\u1ee3p SMART"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integrate", "zh", "SMART\u96c6\u6210\u4e8b\u4ef6"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				{"integratemove", "en", "SMART Integrate Move To Patrol Incident"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integratemove", "es", "SMART Integrar Mover al Incidente de Patrullaje."}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integratemove", "in", "SMART Integrasikan Pindah ke Insiden Patroli"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				{"integratemove", "km", "Smart \u1794\u17b6\u1793\u1792\u17d2\u179c\u17be\u179f\u1798\u17b6\u17a0\u179a\u178e\u1780\u1798\u17d2\u1798\u1785\u179b\u1793\u17b6\u1791\u17c5\u17a7\u1794\u17d2\u1794\u178f\u17d2\u178f\u17b7\u17a0\u17c1\u178f\u17bb\u179b\u17d2\u1794\u17b6\u178f"},   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				{"integratemove", "ru", "\u041f\u0435\u0440\u0435\u043d\u043e\u0441 SMART Integrate \u0432 \u0438\u043d\u0446\u0438\u0434\u0435\u043d\u0442 \u0440\u0435\u0439\u0434\u0430\t"},  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				{"integratemove", "uk", "\u0406\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0456\u044f \u043f\u0435\u0440\u0435\u043c\u0456\u0449\u0435\u043d\u043d\u044f SMART \u0434\u043e \u043f\u0430\u0442\u0440\u0443\u043b\u044c\u043d\u043e\u0433\u043e \u0456\u043d\u0446\u0438\u0434\u0435\u043d\u0442\u0443"},   //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				{"integratemove", "zh", "SMART\u96c6\u6210\u79fb\u52a8\u5230\u5de1\u62a4\u4e8b\u4ef6"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

				{"integratelink", "en", "SMART Integrate Link To Patrol Incident"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integratelink", "es", "SMART Integrar Enlace con Incidente de Patrullaje."}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integratelink", "in", "SMART Integrasikan Tautan ke Insiden Patroli"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				
				{"integratelink", "km", "\u1780\u1798\u17d2\u1798\u179c\u17b7\u1792\u17b8 SMART \u179a\u17bd\u1798\u1794\u1789\u17d2\u1785\u17bc\u179b\u178f\u17c6\u178e\u1791\u17c5\u1793\u17b9\u1784\u17a7\u1794\u17d2\u1794\u178f\u17d2\u178f\u17b7\u17a0\u17c1\u178f\u17bb\u179b\u17d2\u1794\u17b6\u178f"},  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				{"integratelink", "ru", "C\u0441\u044b\u043b\u043a\u0430 SMART Integrate \u0432 \u0438\u043d\u0446\u0438\u0434\u0435\u043d\u0442 \u0440\u0435\u0439\u0434\u0430\t"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				{"integratelink", "uk", "\u0406\u043d\u0442\u0435\u0433\u0440\u0430\u0446\u0456\u044f \u043b\u0456\u043d\u043a\u0443 SMART \u0434\u043e \u043f\u0430\u0442\u0440\u0443\u043b\u044c\u043d\u043e\u0433\u043e \u0456\u043d\u0446\u0438\u0434\u0435\u043d\u0442\u0443"},  //$NON-NLS-1$ //$NON-NLS-2$//$NON-NLS-3$
				{"integratelink", "zh", "SMART\u96c6\u6210\u94fe\u63a5\u5230\u5de1\u62a4\u4e8b\u4ef6"}, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				};
				
		session.doWork(new Work(){
			@Override
			public void execute(Connection connection) throws SQLException {
				for (String s : sql){
					IncidentPlugIn.log(s, null);
					connection.createStatement().executeUpdate(s);
				}

				String sql ="insert into smart.i18n_label(language_uuid, element_uuid, value) select l.uuid, a.uuid, ? from smart.language l join smart.incident_type a on a.ca_uuid = l.ca_uuid WHERE a.keyid = ? and l.code = ?"; //$NON-NLS-1$
				IncidentPlugIn.log(sql, null);
				//name
				//key
				//language code
				try(PreparedStatement ps = connection.prepareStatement(sql)){
					for (String[] row : data) {
						ps.setString(1, row[2]);
						ps.setString(2, row[0]);
						ps.setString(3, row[1]);
						ps.execute();
					}
				}
			}			
		});
		
		HibernateManager.setPlugInVersion(IncidentPlugIn.PLUGIN_ID, IncidentPlugIn.DB_VERSION_2, session);

	}
	
	private void createTables(Session session) {
		
		Integer cnt = session.createNativeQuery("select count(*) from sys.systables where tablename = 'INCIDENT_WAYPOINT'", Integer.class).uniqueResult(); //$NON-NLS-1$
		if (cnt == 0) {
			String[] sql = new String[] {
				"create table smart.incident_waypoint(wp_uuid char(16) for bit data not null, patrol_uuid char(16) for bit data, primary key (wp_uuid) )", //$NON-NLS-1$
				"ALTER TABLE smart.incident_waypoint add constraint incident_wp_wpuuid_fk FOREIGN KEY (wp_uuid) REFERENCES smart.waypoint(uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
				"ALTER TABLE smart.incident_waypoint add constraint incident_wp_patroluuid_fk FOREIGN KEY (patrol_uuid) REFERENCES smart.patrol(uuid) on delete cascade on update restrict deferrable initially immediate", //$NON-NLS-1$
			};
			session.doWork(new Work(){
				@Override
				public void execute(Connection connection) throws SQLException {
					for (String s : sql){
						IncidentPlugIn.log(s, null);
						connection.createStatement().executeUpdate(s);
					}
				}
				
			});
		}
		HibernateManager.setPlugInVersion(IncidentPlugIn.PLUGIN_ID, IncidentPlugIn.DB_VERSION_1, session);
	}
}

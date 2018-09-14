package org.wcs.smart.upgrade.v600;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.IDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.DerbyUtils;
import org.wcs.smart.util.UuidUtils;

public class Upgrader610To620 implements IDatabaseUpgrader { 
	private Exception thrownException = null;

	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask("Upgrading from 6.1.0 to 6.2.0");
		thrownException = null;
		try(Session s = HibernateManager.openSession()){
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					s.beginTransaction();
					try {
						c.setAutoCommit(false);
						upgrade(c, s, monitor);
						c.setAutoCommit(true);
						s.getTransaction().commit();
					} catch (final Exception e) {
						thrownException = new Exception("Error upgrading from 6.1.0 to 6.2.0", e);
					}
				}
			});
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void upgrade(Connection c, Session session, IProgressMonitor monitor)
			throws Exception {

		String[] sql = new String[] {
				"CREATE TABLE smart.iconset (uuid char(16) for bit data not null, keyid varchar(64) not null, ca_uuid char(16) for bit data not null, is_default boolean default false not null, primary key(uuid))",
				"CREATE TABLE smart.icon (uuid char(16) for bit data not null, keyid varchar(64) not null, ca_uuid char(16) for bit data not null, primary key(uuid))",
				"CREATE TABLE smart.iconfile (uuid char(16) for bit data not null, icon_uuid char(16) for bit data not null, iconset_uuid char(16) for bit data not null, filename varchar(2064) not null, primary key(uuid))",
				
				"GRANT ALL PRIVILEGES ON smart.iconset TO admin,manager,data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.icon TO admin,manager,data_entry", //$NON-NLS-1$
				"GRANT ALL PRIVILEGES ON smart.iconfile TO admin,manager,data_entry", //$NON-NLS-1$
				
				"ALTER TABLE smart.iconset ADD CONSTRAINT iconset_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.icon ADD CONSTRAINT icon_cauuid_fk FOREIGN KEY (ca_uuid) REFERENCES smart.conservation_area(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.iconfile ADD CONSTRAINT iconfile_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.iconfile ADD CONSTRAINT iconfile_iconsetuuid_fk FOREIGN KEY (iconset_uuid) REFERENCES smart.iconset(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$

				
				"ALTER TABLE smart.dm_category add column icon_uuid char(16) for bit data",
				"ALTER TABLE smart.dm_attribute add column icon_uuid char(16) for bit data",
				"ALTER TABLE smart.dm_attribute_list add column icon_uuid char(16) for bit data",
				"ALTER TABLE smart.dm_attribute_tree add column icon_uuid char(16) for bit data",
				
				
				"ALTER TABLE smart.dm_category ADD CONSTRAINT dmcat_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute ADD CONSTRAINT dmatt_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_list ADD CONSTRAINT dmattlist_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				"ALTER TABLE smart.dm_attribute_tree ADD CONSTRAINT dmatttree_iconuuid_fk FOREIGN KEY (icon_uuid) REFERENCES smart.icon(uuid) ON DELETE RESTRICT ON UPDATE RESTRICT  DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
				
				//TODO: triggers
				
		};

		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		PreparedStatement psiconset = c.prepareStatement("INSERT INTO smart.iconset (uuid, keyid, ca_uuid, is_default) VALUES (?, ?, ?, ?)");		 //$NON-NLS-1$
		PreparedStatement pslabel = c.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) VALUES(?, ?, ?)");
		PreparedStatement psicon = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) VALUES(?, ?, ?)");
		PreparedStatement psiconfile = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) VALUES(?, ?, ?, ?)");

		
		//create default icon sets
		//NOTE: We cannot use hibernate objects here - this may cause issues in the future
		try(ResultSet rs = c.createStatement().executeQuery("SELECT uuid FROM smart.conservation_area")){
		
			
			while(rs.next()) {

				byte[] cuuid = rs.getBytes(1);
				if (UuidUtils.byteToUUID(cuuid).equals(ConservationArea.MULTIPLE_CA)) continue;
				//TODO:ccaa uuid ignore
				PreparedStatement ps = c.prepareStatement("SELECT uuid FROM smart.language WHERE ca_uuid = ? and isdefault");
				ps.setBytes(1, cuuid);
				
				byte[] luuid = null;
				try(ResultSet rs2 = ps.executeQuery()){
					if (!rs2.next()) continue; //no default language for this ca; skip
					luuid = rs2.getBytes(1);
				}
				
				if (luuid == null) continue; 
				
				byte[] lineuuid = DerbyUtils.createUuid();
				psiconset.setBytes(1, lineuuid);
				psiconset.setString(2, "line");
				psiconset.setBytes(3, cuuid);
				psiconset.setBoolean(4, false);
				psiconset.addBatch();
				
				pslabel.setBytes(1, luuid);
				pslabel.setBytes(2, lineuuid);
				pslabel.setString(3, "Outline Only");
				pslabel.addBatch();
				
				byte[] blackuuid = DerbyUtils.createUuid();
				psiconset.setBytes(1, blackuuid);
				psiconset.setString(2, "black");
				psiconset.setBytes(3, cuuid);
				psiconset.setBoolean(4, false);
				psiconset.addBatch();
				
				pslabel.setBytes(1, luuid);
				pslabel.setBytes(2, blackuuid);
				pslabel.setString(3, "Black and White");
				pslabel.addBatch();
				
				byte[] coloruuid = DerbyUtils.createUuid();
				psiconset.setBytes(1, coloruuid);
				psiconset.setString(2, "color");
				psiconset.setBytes(3, cuuid);
				psiconset.setBoolean(4, true);
				psiconset.addBatch();
				
				pslabel.setBytes(1, luuid);
				pslabel.setBytes(2, coloruuid);
				pslabel.setString(3, "Full Color");
				pslabel.addBatch();
				
				psiconset.executeBatch();
				pslabel.executeBatch();
				
				
				for (String[] icon : ICONLINKS) {
					
					byte[] iconuuid = DerbyUtils.createUuid();
					
					psicon.setBytes(1, iconuuid);
					psicon.setString(2, icon[0]);
					psicon.setBytes(3, cuuid);
					psicon.addBatch();
					
					pslabel.setBytes(1, luuid);
					pslabel.setBytes(2, iconuuid);
					pslabel.setString(3, icon[1]);
					pslabel.addBatch();
					
					byte[] fileuuid = DerbyUtils.createUuid();
					psiconfile.setBytes(1, fileuuid);
					psiconfile.setBytes(2, iconuuid);
					psiconfile.setBytes(3, blackuuid);
					psiconfile.setString(4, icon[2]);
					psiconfile.addBatch();
					
					fileuuid = DerbyUtils.createUuid();
					psiconfile.setBytes(1, fileuuid);
					psiconfile.setBytes(2, iconuuid);
					psiconfile.setBytes(3, lineuuid);
					psiconfile.setString(4, icon[3]);
					psiconfile.addBatch();
					
					fileuuid = DerbyUtils.createUuid();
					psiconfile.setBytes(1, fileuuid);
					psiconfile.setBytes(2, iconuuid);
					psiconfile.setBytes(3, coloruuid);
					psiconfile.setString(4, icon[4]);
					psiconfile.addBatch();
					
					
					psicon.executeBatch();
					pslabel.executeBatch();
					psiconfile.executeBatch();
					
					
					//update data model items
					updateDataModel(c, iconuuid, icon[5]);
				}
				
			}
		}
		
		
		

		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V620.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		c.commit();
	}

	private void updateDataModel(Connection c, byte[] iconuuid, String mappingString) throws SQLException {
		if (mappingString.trim().length() == 0) return;
		
		PreparedStatement pscat = c.prepareStatement("UPDATE smart.dm_category SET icon_uuid = ? WHERE hkey = ?");
		PreparedStatement psatt = c.prepareStatement("UPDATE smart.dm_attribute SET icon_uuid = ? WHERE keyid = ?");
		
		PreparedStatement psattlist = c.prepareStatement("UPDATE smart.dm_attribute_list SET icon_uuid = ? WHERE keyid = ? and attribute_uuid in (select uuid from smart.dm_attribute where keyid = ?)");
		PreparedStatement psatttree = c.prepareStatement("UPDATE smart.dm_attribute_tree SET icon_uuid = ? WHERE hkey = ? and attribute_uuid in (select uuid from smart.dm_attribute where keyid = ?)");

		
		String[] mappings = mappingString.split(",");
		for (String mapping : mappings) {
			if (mapping.trim().isEmpty()) continue;
			
			String[] parts = mapping.split(":");
			
			if (parts[0].equalsIgnoreCase("attribute")) {
				if (parts.length == 2) {
					//attribute
					psatt.setBytes(1, iconuuid);
					psatt.setString(2, parts[1]);
					psatt.executeUpdate();
					
				}else if (parts.length > 2) {
					String keyid = parts[2];
					psattlist.setBytes(1, iconuuid);
					psattlist.setString(2, keyid);
					psattlist.setString(3, parts[1]);
					psattlist.executeUpdate();
					
					psatttree.setBytes(1, iconuuid);
					psatttree.setString(2, keyid + ".");
					psatttree.setString(3, parts[1]);
					psatttree.executeUpdate();
				}
				
				
			}else if (parts[0].equalsIgnoreCase("category")) {
				String hkey = parts[1];
				
				pscat.setBytes(1, iconuuid);
				pscat.setString(2, hkey + ".");
				pscat.executeUpdate();
			}
		}
	}

	private static final String[][] ICONLINKS = new String[][] {
		{"abandoned","Abandoned","platform:/plugin/org.wcs.smart/images/datamodel/black/Abandoned_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Abandoned_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Abandoned_icon.svg","attribute:status:abandoned"},
		{"action","Action","platform:/plugin/org.wcs.smart/images/datamodel/black/Action_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Action_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Action_icon.svg","attribute:actiontakenmp"},
		{"action_taken","Action taken","platform:/plugin/org.wcs.smart/images/datamodel/black/Action_taken_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Action_taken_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Action_taken_icon.svg","attribute:actiontakenitems,attribute:actiontakencamp,attribute:actiontakencarcass,attribute:actiontaken_items,attribute:actiontaken_landclearning,attribute:actiontaken_liveanimals,attribute:actiontakenliveanimals,attribute:actiontakenrcass"},
		{"action_taken_people","Action taken people","platform:/plugin/org.wcs.smart/images/datamodel/black/Action_taken_people_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Action_taken_people_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Action_taken_people_icon.svg","attribute:actiontakenpeople"},
		{"active","Active","platform:/plugin/org.wcs.smart/images/datamodel/black/Active_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Active_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Active_icon.svg","attribute:status:active"},
		{"adult_female","Adult female","platform:/plugin/org.wcs.smart/images/datamodel/black/Adult_female_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Adult_female_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Adult_female_icon.svg","attribute:numberofadultfemales"},
		{"adult","Adult","platform:/plugin/org.wcs.smart/images/datamodel/black/Adult_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Adult_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Adult_icon.svg","attribute:ageofanimal:adult"},
		{"adult_male","Adult male","platform:/plugin/org.wcs.smart/images/datamodel/black/Adult_male_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Adult_male_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Adult_male_icon.svg","attribute:numberofadultmales"},
		{"age_of_animal","Age of animal","platform:/plugin/org.wcs.smart/images/datamodel/black/Age_of_animal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Age_of_animal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Age_of_animal_icon.svg","attribute:ageofanimal"},
		{"age_of_carcass","Age of carcass","platform:/plugin/org.wcs.smart/images/datamodel/black/Age_of_carcass_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Age_of_carcass_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Age_of_carcass_icon.svg","attribute:ageofanimalcarcass"},
		{"age_of_sign","Age of sign","platform:/plugin/org.wcs.smart/images/datamodel/black/Age_of_sign_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Age_of_sign_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Age_of_sign_icon.svg","attribute:ageofsign"},
		{"ammunition","Ammunition","platform:/plugin/org.wcs.smart/images/datamodel/black/Ammunition_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Ammunition_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Ammunition_icon.svg","category:humanactivity.weaponsequipment.firearmsammunition.ammunition"},
		{"antlers","Antlers","platform:/plugin/org.wcs.smart/images/datamodel/black/Antlers_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Antlers_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Antlers_icon.svg","attribute:typeofanimalpart:hornsorantlers,attribute:typeofanimalparts:antlers,attribute:trophymissing:hornorantler"},
		{"armed","Armed","platform:/plugin/org.wcs.smart/images/datamodel/black/Armed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Armed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Armed_icon.svg","attribute:peoplearmed:armed"},
		{"arrested","Arrested","platform:/plugin/org.wcs.smart/images/datamodel/black/Arrested_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Arrested_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Arrested_icon.svg","attribute:actiontaken_people:arrested,attribute:actiontakenpeople:arrested"},
		{"axe","Axe","platform:/plugin/org.wcs.smart/images/datamodel/black/Axe_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Axe_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Axe_icon.svg","attribute:typeofcuttingtool:axe"},
		{"bamboo","Bamboo","platform:/plugin/org.wcs.smart/images/datamodel/black/Bamboo_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Bamboo_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Bamboo_icon.svg","attribute:typeofforestproduct:bamboo"},
		{"bicycle","Bicycle","platform:/plugin/org.wcs.smart/images/datamodel/black/Bicycle_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Bicycle_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Bicycle_icon.svg","attribute:typeoftransportation:bicycle"},
		{"boat","Boat","platform:/plugin/org.wcs.smart/images/datamodel/black/Boat_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Boat_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Boat_icon.svg","attribute:typeoftransportation:boat"},
		{"boulders","Boulders","platform:/plugin/org.wcs.smart/images/datamodel/black/Boulders_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Boulders_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Boulders_icon.svg","attribute:typeofrockormineral:boulders"},
		{"bovidae","Bovidae","platform:/plugin/org.wcs.smart/images/datamodel/black/Bovidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Bovidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Bovidae_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl.bovidae_rl"},
		{"bow","Bow","platform:/plugin/org.wcs.smart/images/datamodel/black/Bow_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Bow_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Bow_icon.svg","attribute:typeoftraditionalweapon:bows"},
		{"bridge","Bridge","platform:/plugin/org.wcs.smart/images/datamodel/black/Bridge_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Bridge_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Bridge_icon.svg","attribute:typeofinfrastructure:bridge"},
		{"buffalo","Buffalo","platform:/plugin/org.wcs.smart/images/datamodel/black/Buffalo_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Buffalo_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Buffalo_icon.svg","attribute:typeofdomesticanimal:buffalo"},
		{"buffalo_syncerus","Buffalo syncerus","platform:/plugin/org.wcs.smart/images/datamodel/black/Buffalo_syncerus_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Buffalo_syncerus_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Buffalo_syncerus_icon.svg",""},
		{"bushmeat","Bushmeat","platform:/plugin/org.wcs.smart/images/datamodel/black/Bushmeat_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Bushmeat_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Bushmeat_icon.svg","category:animals.animalpartsandbushmeat,category:animals.bushmeat"},
		{"camel","Camel","platform:/plugin/org.wcs.smart/images/datamodel/black/Camel_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Camel_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Camel_icon.svg","attribute:typeoftransportation:camel"},
		{"camp","Camp","platform:/plugin/org.wcs.smart/images/datamodel/black/Camp_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Camp_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Camp_icon.svg","category:humanactivity.shelterorcamp,attribute:sheltertype:camp"},
		{"camp_type","Camp type","platform:/plugin/org.wcs.smart/images/datamodel/black/Camp_type_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Camp_type_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Camp_type_icon.svg","attribute:sheltertype"},
		{"canines","Canines","platform:/plugin/org.wcs.smart/images/datamodel/black/Canines_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Canines_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Canines_icon.svg","attribute:typeofanimalpart:tusksorcanines,attribute:typeofanimalparts:canines"},
		{"capacity","Capacity","platform:/plugin/org.wcs.smart/images/datamodel/black/Capacity_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Capacity_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Capacity_icon.svg","attribute:sheltercapacity"},
		{"carcass","Carcass","platform:/plugin/org.wcs.smart/images/datamodel/black/Carcass_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Carcass_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Carcass_icon.svg","category:animals.carcass"},
		{"carnivora","Carnivora","platform:/plugin/org.wcs.smart/images/datamodel/black/Carnivora_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Carnivora_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Carnivora_icon.svg","attribute:species:chordata_rl.mammalia_rl.carnivora_rl"},
		{"cart","Cart","platform:/plugin/org.wcs.smart/images/datamodel/black/Cart_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cart_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cart_icon.svg","attribute:typeoftransportation:cart"},
		{"car","Car","platform:/plugin/org.wcs.smart/images/datamodel/black/Car_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Car_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Car_icon.svg","attribute:typeoftransportation:car"},
		{"caterpillar","Caterpillar","platform:/plugin/org.wcs.smart/images/datamodel/black/Caterpillar_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Caterpillar_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Caterpillar_icon.svg","attribute:typeofforestproduct:caterpiller"},
		{"cat","Cat","platform:/plugin/org.wcs.smart/images/datamodel/black/Cat_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cat_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cat_icon.svg","attribute:typeofdomesticanimal:cat"},
		{"cause_of_death","Cause of death","platform:/plugin/org.wcs.smart/images/datamodel/black/Cause_of_death_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cause_of_death_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cause_of_death_icon.svg","attribute:causeofdeath"},
		{"cephalphus_duikers","Cephalphus duikers","platform:/plugin/org.wcs.smart/images/datamodel/black/Cephalphus_duikers_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cephalphus_duikers_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cephalphus_duikers_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl.bovidae_rl.cephalophus_rl"},
		{"cercopithecidae","Cercopithecidae","platform:/plugin/org.wcs.smart/images/datamodel/black/Cercopithecidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cercopithecidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cercopithecidae_icon.svg","attribute:species:chordata_rl.mammalia_rl.primates_rl.cercopithecidae_rl"},
		{"cetartiodactyla","Cetartiodactyla","platform:/plugin/org.wcs.smart/images/datamodel/black/Cetartiodactyla_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cetartiodactyla_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cetartiodactyla_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl"},
		{"chainsaw","Chainsaw","platform:/plugin/org.wcs.smart/images/datamodel/black/Chainsaw_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Chainsaw_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Chainsaw_icon.svg","attribute:typeofcuttingtool:chainsaw"},
		{"charcoal","Charcoal","platform:/plugin/org.wcs.smart/images/datamodel/black/Charcoal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Charcoal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Charcoal_icon.svg","category:humanactivity.timber.charcoal"},
		{"chemical","Chemical","platform:/plugin/org.wcs.smart/images/datamodel/black/Chemical_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Chemical_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Chemical_icon.svg","attribute:typeofpoison:chemical,attribute:typeofpollution:chemical"},
		{"chimp","Chimp","platform:/plugin/org.wcs.smart/images/datamodel/black/Chimp_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Chimp_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Chimp_icon.svg","attribute:species:chordata_rl.mammalia_rl.primates_rl.hominidae_rl.pan_rl"},
		{"clay","Clay","platform:/plugin/org.wcs.smart/images/datamodel/black/Clay_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Clay_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Clay_icon.svg","attribute:typeofrockormineral:clay"},
		{"club","Club","platform:/plugin/org.wcs.smart/images/datamodel/black/Club_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Club_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Club_icon.svg","attribute:typeoftraditionalweapon:club"},
		{"collected","Collected","platform:/plugin/org.wcs.smart/images/datamodel/black/Collected_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Collected_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Collected_icon.svg","attribute:actiontakenrcass:collected,attribute:actiontakencarcass:collected"},
		{"confiscated_1","Confiscated 1","platform:/plugin/org.wcs.smart/images/datamodel/black/Confiscated_1_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Confiscated_1_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Confiscated_1_icon.svg","attribute:actiontakenrcass:confiscated,attribute:actiontakencarcass:confiscated,attribute:actiontaken_liveanimals:confiscated,attribute:actiontakenliveanimals:confiscated,attribute:actiontaken_items:confiscated,attribute:actiontakenitems:confiscated"},
		{"confiscated_2","Confiscated 2","platform:/plugin/org.wcs.smart/images/datamodel/black/Confiscated_2_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Confiscated_2_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Confiscated_2_icon.svg",""},
		{"cooking_pot","Cooking pot","platform:/plugin/org.wcs.smart/images/datamodel/black/Cooking_pot_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cooking_pot_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cooking_pot_icon.svg","attribute:typeofequipment:cookingpot"},
		{"cow","Cow","platform:/plugin/org.wcs.smart/images/datamodel/black/Cow_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cow_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cow_icon.svg","attribute:typeofdomesticanimal:cow"},
		{"cutting_tools","Cutting tools","platform:/plugin/org.wcs.smart/images/datamodel/black/Cutting_tools_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cutting_tools_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cutting_tools_icon.svg","category:humanactivity.weaponsequipment.cuttingtools"},
		{"cut_pieces","Cut pieces","platform:/plugin/org.wcs.smart/images/datamodel/black/Cut_pieces_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Cut_pieces_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Cut_pieces_icon.svg","category:humanactivity.timber.cutpieces"},
		{"destroyed","Destroyed","platform:/plugin/org.wcs.smart/images/datamodel/black/Destroyed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Destroyed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Destroyed_icon.svg","attribute:actiontakenrcass:destroyed,attribute:actiontakencarcass:destroyed,attribute:actiontakenmp:destroyed,attribute:actiontaken_items:destroyed,attribute:actiontakenitems:destroyed,attribute:actiontaken_landclearning:destroyedcrops,attribute:actiontaken_liveanimals:destroyed,attribute:actiontakenliveanimals:destroyed,attribute:actiontakencamp:destroyed"},
		{"diamonds","Diamonds","platform:/plugin/org.wcs.smart/images/datamodel/black/Diamonds_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Diamonds_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Diamonds_icon.svg","attribute:typeofrockormineral:diamonds"},
		{"dock","Dock","platform:/plugin/org.wcs.smart/images/datamodel/black/Dock_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Dock_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Dock_icon.svg","attribute:typeofinfrastructure:dock"},
		{"dog","Dog","platform:/plugin/org.wcs.smart/images/datamodel/black/Dog_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Dog_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Dog_icon.svg","attribute:typeofdomesticanimal:dog"},
		{"domestic_animals","Domestic animals","platform:/plugin/org.wcs.smart/images/datamodel/black/Domestic_animals_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Domestic_animals_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Domestic_animals_icon.svg","category:humanactivity.domesticanimals"},
		{"dung","Dung","platform:/plugin/org.wcs.smart/images/datamodel/black/Dung_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Dung_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Dung_icon.svg","category:animals.sign.dung"},
		{"dynamite","Dynamite","platform:/plugin/org.wcs.smart/images/datamodel/black/Dynamite_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Dynamite_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Dynamite_icon.svg","attribute:typeoffishingequipment:dynamite"},
		{"electric_rod","Electric rod","platform:/plugin/org.wcs.smart/images/datamodel/black/Electric_rod_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Electric_rod_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Electric_rod_icon.svg","attribute:typeoffishingequipment:electricrod"},
		{"elephant","Elephant","platform:/plugin/org.wcs.smart/images/datamodel/black/Elephant_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Elephant_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Elephant_icon.svg","attribute:typeofdomesticanimal:elephant"},
		{"equipment","Equipment","platform:/plugin/org.wcs.smart/images/datamodel/black/Equipment_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Equipment_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Equipment_icon.svg","category:humanactivity.weaponsequipment,category:humanactivity.weaponsequipment.equipment"},
		{"eudorvas_gazelle","Eudorvas gazelle","platform:/plugin/org.wcs.smart/images/datamodel/black/Eudorvas_gazelle_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Eudorvas_gazelle_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Eudorvas_gazelle_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl.bovidae_rl.gazella_rl"},
		{"extractive_industry_basecamp","Extractive industry basecamp","platform:/plugin/org.wcs.smart/images/datamodel/black/Extractive_industry_basecamp_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Extractive_industry_basecamp_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Extractive_industry_basecamp_icon.svg","attribute:typeofinfrastructure:extractiveindustrybasecamp"},
		{"feeding","Feeding","platform:/plugin/org.wcs.smart/images/datamodel/black/Feeding_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Feeding_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Feeding_icon.svg","category:animals.sign.feeding"},
		{"felidae","Felidae","platform:/plugin/org.wcs.smart/images/datamodel/black/Felidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Felidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Felidae_icon.svg","attribute:species:chordata_rl.mammalia_rl.carnivora_rl.felidae_rl"},
		{"female","Female","platform:/plugin/org.wcs.smart/images/datamodel/black/Female_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Female_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Female_icon.svg","attribute:sex:female"},
		{"firearms_ammunition","Firearms & ammunition","platform:/plugin/org.wcs.smart/images/datamodel/black/Firearms_ammunition_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Firearms_ammunition_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Firearms_ammunition_icon.svg","category:humanactivity.weaponsequipment.firearmsammunition"},
		{"firearms","Firearms","platform:/plugin/org.wcs.smart/images/datamodel/black/Firearms_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Firearms_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Firearms_icon.svg","category:humanactivity.weaponsequipment.firearmsammunition.firearms"},
		{"firewood","Firewood","platform:/plugin/org.wcs.smart/images/datamodel/black/Firewood_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Firewood_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Firewood_icon.svg","category:humanactivity.timber.firewood"},
		{"fire","Fire","platform:/plugin/org.wcs.smart/images/datamodel/black/Fire_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Fire_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Fire_icon.svg","category:humanactivity.fire"},
		{"fishing_tools","Fishing tools","platform:/plugin/org.wcs.smart/images/datamodel/black/Fishing_tools_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Fishing_tools_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Fishing_tools_icon.svg","category:humanactivity.weaponsequipment.fishingtools"},
		{"fish_trap","Fish trap","platform:/plugin/org.wcs.smart/images/datamodel/black/Fish_trap_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Fish_trap_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Fish_trap_icon.svg","attribute:typeoffishingequipment:fishtrap"},
		{"footprint_1","Footprint 1","platform:/plugin/org.wcs.smart/images/datamodel/black/Footprint_1_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Footprint_1_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Footprint_1_icon.svg",""},
		{"footprint_2","Footprint 2","platform:/plugin/org.wcs.smart/images/datamodel/black/Footprint_2_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Footprint_2_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Footprint_2_icon.svg","attribute:typeofhumansign:footprint"},
		{"fossil","Fossil","platform:/plugin/org.wcs.smart/images/datamodel/black/Fossil_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Fossil_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Fossil_icon.svg","attribute:typeofrockormineral:fossil"},
		{"fresh","Fresh","platform:/plugin/org.wcs.smart/images/datamodel/black/Fresh_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Fresh_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Fresh_icon.svg","attribute:ageofanimalcarcass:fresh,attribute:typeofanimalpart:meat.fresh,attribute:typeofanimalpart:wholeanimal.fresh,attribute:attribute:trophymissing:meat,attribute:statusofbushmeat:fresh"},
		{"gall_bladder","Gall bladder","platform:/plugin/org.wcs.smart/images/datamodel/black/Gall_bladder_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Gall_bladder_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Gall_bladder_icon.svg","attribute:typeofanimalpart:gallbladder,attribute:typeofanimalparts:gallbladder"},
		{"garbage","Garbage","platform:/plugin/org.wcs.smart/images/datamodel/black/Garbage_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Garbage_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Garbage_icon.svg","attribute:typeofpollution:garbage"},
		{"giraffe","Giraffe","platform:/plugin/org.wcs.smart/images/datamodel/black/Giraffe_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Giraffe_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Giraffe_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl.giraffidae_rl.giraffa_rl"},
		{"giraffidae","Giraffidae","platform:/plugin/org.wcs.smart/images/datamodel/black/Giraffidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Giraffidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Giraffidae_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl.giraffidae_rl"},
		{"goat","Goat","platform:/plugin/org.wcs.smart/images/datamodel/black/Goat_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Goat_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Goat_icon.svg","attribute:typeofdomesticanimal:goat"},
		{"gold","Gold","platform:/plugin/org.wcs.smart/images/datamodel/black/Gold_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Gold_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Gold_icon.svg","attribute:typeofrockormineral:gold"},
		{"gorilla","Gorilla","platform:/plugin/org.wcs.smart/images/datamodel/black/Gorilla_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Gorilla_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Gorilla_icon.svg","attribute:species:chordata_rl.mammalia_rl.primates_rl.hominidae_rl.gorilla_rl"},
		{"gravel","Gravel","platform:/plugin/org.wcs.smart/images/datamodel/black/Gravel_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Gravel_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Gravel_icon.svg","attribute:typeofrockormineral:gravel"},
		{"ground_hide","Ground hide","platform:/plugin/org.wcs.smart/images/datamodel/black/Ground_hide_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Ground_hide_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Ground_hide_icon.svg","attribute:sheltertype:groundhide"},
		{"handsaw","Handsaw","platform:/plugin/org.wcs.smart/images/datamodel/black/Handsaw_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Handsaw_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Handsaw_icon.svg","attribute:typeofcuttingtool:handsaw"},
		{"heard","Heard","platform:/plugin/org.wcs.smart/images/datamodel/black/Heard_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Heard_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Heard_icon.svg","attribute:actiontaken_items:heardonly,attribute:actiontakenitems:heardonly"},
		{"hippopotamidae","Hippopotamidae","platform:/plugin/org.wcs.smart/images/datamodel/black/Hippopotamidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Hippopotamidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Hippopotamidae_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl.hippopotamidae_rl"},
		{"hippos","Hippos","platform:/plugin/org.wcs.smart/images/datamodel/black/Hippos_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Hippos_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Hippos_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl.hippopotamidae_rl.hippopotamus_rl"},
		{"homonidae","Homonidae","platform:/plugin/org.wcs.smart/images/datamodel/black/Homonidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Homonidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Homonidae_icon.svg","attribute:species:chordata_rl.mammalia_rl.primates_rl.hominidae_rl"},
		{"honey","Honey","platform:/plugin/org.wcs.smart/images/datamodel/black/Honey_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Honey_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Honey_icon.svg","attribute:typeofforestproduct:honey"},
		{"horns","Horns","platform:/plugin/org.wcs.smart/images/datamodel/black/Horns_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Horns_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Horns_icon.svg","attribute:partremoved1:hornorantler,attribute:partremoved2:hornorantler,attribute:partremoved3:hornorantler,attribute:typeofanimalparts:horns"},
		{"horse","Horse","platform:/plugin/org.wcs.smart/images/datamodel/black/Horse_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Horse_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Horse_icon.svg","attribute:typeoftransportation:horse"},
		{"human_activity","Human activity","platform:/plugin/org.wcs.smart/images/datamodel/black/Human_activity_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Human_activity_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Human_activity_icon.svg","category:humanactivity"},
		{"illegal","Illegal","platform:/plugin/org.wcs.smart/images/datamodel/black/Illegal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Illegal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Illegal_icon.svg","attribute:causeofdeath:illegal"},
		{"inactive","Inactive","platform:/plugin/org.wcs.smart/images/datamodel/black/Inactive_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Inactive_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Inactive_icon.svg","attribute:status:inactive"},
		{"infrastructure_roads","Infrastructure & roads","platform:/plugin/org.wcs.smart/images/datamodel/black/Infrastructure & roads_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Infrastructure & roads_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Infrastructure & roads_icon.svg","category:humanactivity.infrastructureandroads"},
		{"infrastructure_size","Infrastructure size","platform:/plugin/org.wcs.smart/images/datamodel/black/Infrastructure_size_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Infrastructure_size_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Infrastructure_size_icon.svg","attribute:infrastructuresize"},
		{"jaw_trap","Jaw trap","platform:/plugin/org.wcs.smart/images/datamodel/black/Jaw_trap_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Jaw_trap_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Jaw_trap_icon.svg","attribute:typeoftrap:jawtrap"},
		{"juvenile","Juvenile","platform:/plugin/org.wcs.smart/images/datamodel/black/Juvenile_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Juvenile_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Juvenile_icon.svg","attribute:ageofanimal:juvenile"},
		{"knife","Knife","platform:/plugin/org.wcs.smart/images/datamodel/black/Knife_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Knife_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Knife_icon.svg","attribute:typeofcuttingtool:knife"},
		{"large","Large","platform:/plugin/org.wcs.smart/images/datamodel/black/Large_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Large_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Large_icon.svg","attribute:sheltercapacity:large,attribute:infrastructuresize:large"},
		{"latex","Latex","platform:/plugin/org.wcs.smart/images/datamodel/black/Latex_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Latex_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Latex_icon.svg","attribute:typeofforestproduct:latex"},
		{"left_at_scene","Left at scene","platform:/plugin/org.wcs.smart/images/datamodel/black/Left_at_scene_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Left_at_scene_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Left_at_scene_icon.svg","attribute:actiontakenrcass:leftatscene,attribute:actiontakencarcass:leftatscene"},
		{"legal","Legal","platform:/plugin/org.wcs.smart/images/datamodel/black/Legal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Legal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Legal_icon.svg","attribute:causeofdeath:legal"},
		{"litter","Litter","platform:/plugin/org.wcs.smart/images/datamodel/black/Litter_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Litter_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Litter_icon.svg","attribute:typeofhumansign:litter"},
		{"logs","Logs","platform:/plugin/org.wcs.smart/images/datamodel/black/Logs_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Logs_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Logs_icon.svg","category:humanactivity.timber.logs"},
		{"machete_cut","Machete cut","platform:/plugin/org.wcs.smart/images/datamodel/black/Machete_cut_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Machete_cut_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Machete_cut_icon.svg","attribute:typeofhumansign:machetecut"},
		{"machete","Machete","platform:/plugin/org.wcs.smart/images/datamodel/black/Machete_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Machete_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Machete_icon.svg","attribute:typeofcuttingtool:machete "},
		{"male","Male","platform:/plugin/org.wcs.smart/images/datamodel/black/Male_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Male_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Male_icon.svg","attribute:sex:male"},
		{"mammal","Mammal","platform:/plugin/org.wcs.smart/images/datamodel/black/Mammal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Mammal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Mammal_icon.svg","attribute:species:chordata_rl.mammalia_rl"},
		{"medicinal_plants","Medicinal plants","platform:/plugin/org.wcs.smart/images/datamodel/black/Medicinal_plants_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Medicinal_plants_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Medicinal_plants_icon.svg","attribute:typeofforestproduct:medicinalplants"},
		{"med","Med","platform:/plugin/org.wcs.smart/images/datamodel/black/Med_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Med_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Med_icon.svg","attribute:sheltercapacity:medium,attribute:infrastructuresize:medium"},
		{"mine","Mine","platform:/plugin/org.wcs.smart/images/datamodel/black/Mine_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Mine_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Mine_icon.svg","attribute:typeofinfrastructure:mine"},
		{"motorbike","Motorbike","platform:/plugin/org.wcs.smart/images/datamodel/black/Motorbike_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Motorbike_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Motorbike_icon.svg","attribute:typeoftransportation:motorbike"},
		{"mushroom","Mushroom","platform:/plugin/org.wcs.smart/images/datamodel/black/Mushroom_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Mushroom_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Mushroom_icon.svg","attribute:typeofforestproduct:mushroom"},
		{"muzzle_loader","Muzzle loader","platform:/plugin/org.wcs.smart/images/datamodel/black/Muzzle_loader_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Muzzle_loader_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Muzzle_loader_icon.svg","attribute:typeoffirearm:muzzleloader"},
		{"natural","Natural","platform:/plugin/org.wcs.smart/images/datamodel/black/Natural_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Natural_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Natural_icon.svg","attribute:typeofpoison:natural,attribute:causeofdeathnatural"},
		{"nest","Nest","platform:/plugin/org.wcs.smart/images/datamodel/black/Nest_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Nest_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Nest_icon.svg","category:animals.sign.nest"},
		{"net","Net","platform:/plugin/org.wcs.smart/images/datamodel/black/Net_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Net_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Net_icon.svg","attribute:typeoffishingequipment:net"},
		{"ntfps","NTFPs","platform:/plugin/org.wcs.smart/images/datamodel/black/NTFPs_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/NTFPs_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/NTFPs_icon.svg","category:humanactivity.nontimberforestproducts"},
		{"observed","Observed","platform:/plugin/org.wcs.smart/images/datamodel/black/Observed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Observed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Observed_icon.svg",""},
		{"observed_only","Observed only","platform:/plugin/org.wcs.smart/images/datamodel/black/Observed_only_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Observed_only_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Observed_only_icon.svg","attribute:actiontakenmp:observedonly,attribute:actiontaken_people:observedonly,attribute:actiontaken_items:observedonly,attribute:actiontakenitems:observedonly,attribute:actiontaken_liveanimals:observedonly,attribute:actiontakenliveanimals:observedonly,attribute:actiontaken_landclearning:observedonly,attribute:actiontakencamp:observedonly,attribute:actiontakenpeople:observedonly"},
		{"orchid","Orchid","platform:/plugin/org.wcs.smart/images/datamodel/black/Orchid_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Orchid_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Orchid_icon.svg","attribute:typeofforestproduct:orchid"},
		{"other","Other","platform:/plugin/org.wcs.smart/images/datamodel/black/Other_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Other_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Other_icon.svg","attribute:typeofcuttingtool:other,attribute:typeofforestproduct:other,attribute:typeoftransportation:other,attribute:typeoftraditionalweapon:other,attribute:typeofinfrastructure:other,attribute:sheltertype:other,attribute:typeofdomesticanimal:other,attribute:typeofhumansign:other,attribute:partremoved1:other,attribute:partremoved2:other,attribute:partremoved3:other,attribute:typeofpollution:other,attribute:typeoftrap:other,attribute:typeoffirearm:other,attribute:typeoffishingequipment:other,attribute:typeofequipment:other,attribute:typeofequipment:object,attribute:typeofpoisonother,attribute:trophymissing:other"},
		{"paws","Paws","platform:/plugin/org.wcs.smart/images/datamodel/black/Paws_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Paws_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Paws_icon.svg","attribute:typeofanimalpart:paw,attribute:typeofanimalparts:paw"},
		{"penis","Penis","platform:/plugin/org.wcs.smart/images/datamodel/black/Penis_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Penis_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Penis_icon.svg","attribute:typeofanimalparts:penis,attribute:typeofanimalpart:penis"},
		{"people_armed","People armed","platform:/plugin/org.wcs.smart/images/datamodel/black/People_armed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/People_armed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/People_armed_icon.svg","attribute:peoplearmed"},
		{"people_direct_observation","People direct observation","platform:/plugin/org.wcs.smart/images/datamodel/black/People_direct_observation_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/People_direct_observation_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/People_direct_observation_icon.svg","category:humanactivity.people"},
		{"people_indirect_sign","People indirect sign","platform:/plugin/org.wcs.smart/images/datamodel/black/People_indirect_sign_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/People_indirect_sign_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/People_indirect_sign_icon.svg","category:humanactivity.humansign"},
		{"perissodactyla","Perissodactyla","platform:/plugin/org.wcs.smart/images/datamodel/black/Perissodactyla_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Perissodactyla_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Perissodactyla_icon.svg","attribute:species:chordata_rl.mammalia_rl.perissodactyla_rl"},
		{"piece","Piece","platform:/plugin/org.wcs.smart/images/datamodel/black/Piece_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Piece_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Piece_icon.svg","attribute:typeofbushmeat:piece"},
		{"pit_trap","Pit trap","platform:/plugin/org.wcs.smart/images/datamodel/black/Pit_trap_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Pit_trap_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Pit_trap_icon.svg","attribute:typeoftrap:pittrap"},
		{"place_of_origin","Place of origin","platform:/plugin/org.wcs.smart/images/datamodel/black/Place_of_origin_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Place_of_origin_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Place_of_origin_icon.svg","attribute:placeoforigin"},
		{"planks","Planks","platform:/plugin/org.wcs.smart/images/datamodel/black/Planks_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Planks_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Planks_icon.svg","category:humanactivity.timber.planks"},
		{"poison","Poison","platform:/plugin/org.wcs.smart/images/datamodel/black/Poison_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Poison_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Poison_icon.svg","category:humanactivity.weaponsequipment.poison"},
		{"pollution","Pollution","platform:/plugin/org.wcs.smart/images/datamodel/black/Pollution_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Pollution_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Pollution_icon.svg","category:humanactivity.pollution"},
		{"primates","Primates","platform:/plugin/org.wcs.smart/images/datamodel/black/Primates_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Primates_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Primates_icon.svg","attribute:species:chordata_rl.mammalia_rl.primates_rl"},
		{"proboscoidea","Proboscoidea","platform:/plugin/org.wcs.smart/images/datamodel/black/Proboscoidea_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Proboscoidea_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Proboscoidea_icon.svg",""},
		{"raft","Raft","platform:/plugin/org.wcs.smart/images/datamodel/black/Raft_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Raft_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Raft_icon.svg","attribute:typeoftransportation:raft"},
		{"ratan","Ratan","platform:/plugin/org.wcs.smart/images/datamodel/black/Ratan_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Ratan_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Ratan_icon.svg","attribute:typeofforestproduct:ratan"},
		{"registration_number","Registration number","platform:/plugin/org.wcs.smart/images/datamodel/black/Registration_number_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Registration_number_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Registration_number_icon.svg","attribute:registrationnumber"},
		{"resin","Resin","platform:/plugin/org.wcs.smart/images/datamodel/black/Resin_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Resin_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Resin_icon.svg","attribute:typeofforestproduct:resin"},
		{"rhino","Rhino","platform:/plugin/org.wcs.smart/images/datamodel/black/Rhino_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Rhino_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Rhino_icon.svg","attribute:species:chordata_rl.mammalia_rl.perissodactyla_rl.rhinocerotidae_rl"},
		{"road","Road","platform:/plugin/org.wcs.smart/images/datamodel/black/Road_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Road_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Road_icon.svg","attribute:typeofinfrastructure:road"},
		{"rocks_minerals","Rocks & minerals","platform:/plugin/org.wcs.smart/images/datamodel/black/Rocks & minerals_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Rocks & minerals_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Rocks & minerals_icon.svg","category:humanactivity.rocksminerals"},
		{"rod","Rod","platform:/plugin/org.wcs.smart/images/datamodel/black/Rod_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Rod_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Rod_icon.svg","attribute:typeoffishingequipment:rod"},
		{"sand","Sand","platform:/plugin/org.wcs.smart/images/datamodel/black/Sand_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Sand_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Sand_icon.svg","attribute:typeofrockormineral:sand"},
		{"sawmill","Sawmill","platform:/plugin/org.wcs.smart/images/datamodel/black/Sawmill_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Sawmill_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Sawmill_icon.svg","attribute:typeofinfrastructure:sawmill"},
		{"scent","Scent","platform:/plugin/org.wcs.smart/images/datamodel/black/Scent_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Scent_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Scent_icon.svg","category:animals.sign.scent"},
		{"scrape","Scrape","platform:/plugin/org.wcs.smart/images/datamodel/black/Scrape_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Scrape_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Scrape_icon.svg","category:animals.sign.scrape"},
		{"semi_automatic","Semi automatic","platform:/plugin/org.wcs.smart/images/datamodel/black/Semi_automatic_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Semi_automatic_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Semi_automatic_icon.svg","attribute:typeoffirearm:semiautomatic"},
		{"sex","Sex","platform:/plugin/org.wcs.smart/images/datamodel/black/Sex_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Sex_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Sex_icon.svg","attribute:sex"},
		{"shotgun","Shotgun","platform:/plugin/org.wcs.smart/images/datamodel/black/Shotgun_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Shotgun_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Shotgun_icon.svg","attribute:typeoffirearm:shotgun"},
		{"skin","Skin","platform:/plugin/org.wcs.smart/images/datamodel/black/Skin_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Skin_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Skin_icon.svg","attribute:partremoved1:skin,attribute:partremoved2:skin,attribute:partremoved3:skin,attribute:typeofanimalpart:skin,attribute:typeofanimalparts:skin,attribute:trophymissing:skin"},
		{"small","Small","platform:/plugin/org.wcs.smart/images/datamodel/black/Small_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Small_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Small_icon.svg","attribute:sheltercapacity:small,attribute:infrastructuresize:small"},
		{"smoked","Smoked","platform:/plugin/org.wcs.smart/images/datamodel/black/Smoked_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Smoked_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Smoked_icon.svg","attribute:typeofanimalpart:meat.smoked,attribute:typeofanimalpart:wholeanimal.smoked,attribute:statusofbushmeat:smoked"},
		{"sound","Sound","platform:/plugin/org.wcs.smart/images/datamodel/black/Sound_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Sound_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Sound_icon.svg","category:animals.sign.sound"},
		{"spears","Spears","platform:/plugin/org.wcs.smart/images/datamodel/black/Spears_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Spears_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Spears_icon.svg","attribute:typeoffishingequipment:spears"},
		{"spear","Spear","platform:/plugin/org.wcs.smart/images/datamodel/black/Spear_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Spear_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Spear_icon.svg","attribute:typeoftraditionalweapon:spear"},
		{"species","Species","platform:/plugin/org.wcs.smart/images/datamodel/black/Species_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Species_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Species_icon.svg","attribute:species"},
		{"spent_cartridges","Spent cartridges","platform:/plugin/org.wcs.smart/images/datamodel/black/Spent_cartridges_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Spent_cartridges_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Spent_cartridges_icon.svg","category:humanactivity.weaponsequipment.firearmsammunition.spentcartridges"},
		{"status","Status","platform:/plugin/org.wcs.smart/images/datamodel/black/Status_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Status_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Status_icon.svg","attribute:status"},
		{"stump","Stump","platform:/plugin/org.wcs.smart/images/datamodel/black/Stump_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Stump_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Stump_icon.svg","category:humanactivity.timber.stump"},
		{"sub_adult","Sub adult","platform:/plugin/org.wcs.smart/images/datamodel/black/Sub_adult_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Sub_adult_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Sub_adult_icon.svg","attribute:ageofanimal:subadult"},
		{"suidae","Suidae","platform:/plugin/org.wcs.smart/images/datamodel/black/Suidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Suidae_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Suidae_icon.svg","attribute:species:chordata_rl.mammalia_rl.cetartiodactyla_rl.suidae_rl"},
		{"tarp","Tarp","platform:/plugin/org.wcs.smart/images/datamodel/black/Tarp_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Tarp_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Tarp_icon.svg","attribute:typeofequipment:tarp"},
		{"tent","Tent","platform:/plugin/org.wcs.smart/images/datamodel/black/Tent_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Tent_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Tent_icon.svg","attribute:typeofequipment:tents"},
		{"timber","Timber","platform:/plugin/org.wcs.smart/images/datamodel/black/Timber_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Timber_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Timber_icon.svg","category:humanactivity.timber"},
		{"track","Track","platform:/plugin/org.wcs.smart/images/datamodel/black/Track_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Track_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Track_icon.svg","category:animals.sign.track"},
		{"traditional_weapons","Traditional weapons","platform:/plugin/org.wcs.smart/images/datamodel/black/Traditional_weapons_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Traditional_weapons_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Traditional_weapons_icon.svg","category:humanactivity.weaponsequipment.traditionalweapons"},
		{"trail","Trail","platform:/plugin/org.wcs.smart/images/datamodel/black/Trail_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Trail_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Trail_icon.svg","Attribute:typeofhumansign:trail"},
		{"transportation","Transportation","platform:/plugin/org.wcs.smart/images/datamodel/black/Transportation_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Transportation_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Transportation_icon.svg","category:humanactivity.transportation"},
		{"traps_snares","Traps snares","platform:/plugin/org.wcs.smart/images/datamodel/black/Traps_snares_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Traps_snares_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Traps_snares_icon.svg","category:humanactivity.weaponsequipment.trap"},
		{"tree_hide","Tree hide","platform:/plugin/org.wcs.smart/images/datamodel/black/Tree_hide_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Tree_hide_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Tree_hide_icon.svg","attribute:sheltertype:treehide"},
		{"trophies_seized","Trophies seized","platform:/plugin/org.wcs.smart/images/datamodel/black/Trophies_seized_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Trophies_seized_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Trophies_seized_icon.svg","category:animals.trophiesseized"},
		{"trophy_type","Trophy type","platform:/plugin/org.wcs.smart/images/datamodel/black/Trophy_type_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Trophy_type_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Trophy_type_icon.svg","attribute:typeofanimalpart,attribute:trophymissing,attribute:typeofanimalparts"},
		{"truck","Truck","platform:/plugin/org.wcs.smart/images/datamodel/black/Truck_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Truck_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Truck_icon.svg","attribute:typeoftransportation:truck"},
		{"tusks","Tusks","platform:/plugin/org.wcs.smart/images/datamodel/black/Tusks_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Tusks_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Tusks_icon.svg","attribute:partremoved1:tusk,attribute:partremoved2:tusk,attribute:partremoved3:tusk,attribute:trophymissing:tusk,,attribute:typeofanimalparts:tusks"},
		{"type","Type","platform:/plugin/org.wcs.smart/images/datamodel/black/Type_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Type_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Type_icon.svg","attribute:typeofcuttingtool,attribute:typeoftraditionalweapon,attribute:typeoffishingequipment,attribute:typeofequipment,attribute:typeofpoison,attribute:typeofforestproduct,attribute:typeofdomesticanimal,attribute:typeofinfrastructure,attribute:typeofrockormineral,attribute:typeofpollution"},
		{"type_of_sign","Type of sign","platform:/plugin/org.wcs.smart/images/datamodel/black/Type_of_sign_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Type_of_sign_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Type_of_sign_icon.svg","attribute:typeofhumansign"},
		{"unarmed","Unarmed","platform:/plugin/org.wcs.smart/images/datamodel/black/Unarmed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Unarmed_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Unarmed_icon.svg","attribute:peoplearmed:unarmed"},
		{"unknown","Unknown","platform:/plugin/org.wcs.smart/images/datamodel/black/Unknown_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Unknown_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Unknown_icon.svg","attribute:sheltertype:unknown,attribute:sex:unknown,attribute:ageofanimal:unknown,attribute:ageofanimalcarcass:unknown,attribute:typeofpollution:unknown,attribute:peoplearmed:unknown,attribute:statusunknown,attribute:typeofpoisonunknown,attribute:numberofagesexunknownattribute:causeofdeathunknown,attribute:status:unknown"},
		{"unsuccessful_pursuit","Unsuccessful pursuit","platform:/plugin/org.wcs.smart/images/datamodel/black/Unsuccessful_pursuit_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Unsuccessful_pursuit_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Unsuccessful_pursuit_icon.svg","attribute:actiontaken_people:unsccessfulpursuit,,attribute:actiontakenpeople:unsccessfulpursuit"},
		{"verbal_warning","Verbal warning","platform:/plugin/org.wcs.smart/images/datamodel/black/Verbal_warning_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Verbal_warning_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Verbal_warning_icon.svg","attribute:actiontaken_people:verbalwarning,attribute:actiontakenpeople:verbalwarning"},
		{"village","Village","platform:/plugin/org.wcs.smart/images/datamodel/black/Village_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Village_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Village_icon.svg",""},
		{"weapons_gear_seized","Weapons & Gear seized","platform:/plugin/org.wcs.smart/images/datamodel/black/Weapons & Gear_seized_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Weapons & Gear_seized_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Weapons & Gear_seized_icon.svg",""},
		{"whole_animal","Whole animal","platform:/plugin/org.wcs.smart/images/datamodel/black/Whole_animal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Whole_animal_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Whole_animal_icon.svg","attribute:typeofanimalpart:wholeanimal,attribute:typeofanimalparts:wholeanimal,attribute:typeofbushmeat:whole"},
		{"wildlife_direct_observation","Wildlife direct observation","platform:/plugin/org.wcs.smart/images/datamodel/black/Wildlife_direct_observation_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Wildlife_direct_observation_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Wildlife_direct_observation_icon.svg","category:animals.liveanimals"},
		{"wildlife","Wildlife","platform:/plugin/org.wcs.smart/images/datamodel/black/Wildlife_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Wildlife_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Wildlife_icon.svg","category:animals"},
		{"wildlife_indirect_sign","Wildlife indirect sign","platform:/plugin/org.wcs.smart/images/datamodel/black/Wildlife_indirect_sign_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Wildlife_indirect_sign_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Wildlife_indirect_sign_icon.svg","category:animals.sign"},
		{"wild_fruit","Wild fruit","platform:/plugin/org.wcs.smart/images/datamodel/black/Wild_fruit_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Wild_fruit_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Wild_fruit_icon.svg","attribute:typeofforestproduct:wildfruit"},
		{"wire_snare","Wire snare","platform:/plugin/org.wcs.smart/images/datamodel/black/Wire_snare_1_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Wire_snare_1_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Wire_snare_1_icon.svg","attribute:typeoftrap:wiresnare"},
		{"wire_snare2","Wire snare 2","platform:/plugin/org.wcs.smart/images/datamodel/black/Wire_snare_2_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Wire_snare_2_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Wire_snare_2_icon.svg","attribute:typeoftrap:wiresnare"},
		{"written_warning","Written warning","platform:/plugin/org.wcs.smart/images/datamodel/black/Written_warning_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Written_warning_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Written_warning_icon.svg","attribute:actiontaken_people:writtenwarning,attribute:actiontakenpeople:writtenwarning"},
		{"young","Young","platform:/plugin/org.wcs.smart/images/datamodel/black/Young_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/line/Young_icon.svg","platform:/plugin/org.wcs.smart/images/datamodel/color/Young_icon.svg","attribute:numberofyoung"},
		
	};
	
}

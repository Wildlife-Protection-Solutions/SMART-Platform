/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart.upgrade.v800;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.icon.FixedIconSet;
import org.wcs.smart.ca.icon.Icon;
import org.wcs.smart.ca.icon.IconFile;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.ca.icon.IconUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
import org.wcs.smart.util.I18nUtil;
import org.wcs.smart.util.UuidUtils;

/**
 * 8.0.0 to 8.0.1 upgrader
 * 
 * @author Emily
 *
 */
public class Upgrader801To810 extends AbstractInteralDatabaseUpgrader { 
	
	private Exception thrownException = null;

	private HashMap<ConservationArea, String> caTimeZoneMapping;
	
	
	public HashMap<ConservationArea, String> getCaTimeZoneMapping(){
		return this.caTimeZoneMapping;
	}
	
	@Override
	public void upgrade(final IProgressMonitor monitor) throws Exception {
		monitor.subTask(MessageFormat.format(Messages.Upgrader700To741_UpgradeMsg, 
				UpgradeEngine.UpgradeFromVersion.V810.fromVersion, 
				UpgradeEngine.UpgradeFromVersion.V810.toVersion));  
		
		
		try(Session s = HibernateManager.openSession()){
			
			
			s.doWork(new Work() {
				@Override
				public void execute(Connection c) throws SQLException {
					s.beginTransaction();
					try {
						c.setAutoCommit(false);
						upgrade(c, monitor);
						c.setAutoCommit(true);
						s.getTransaction().commit();
					} catch (final Exception e) {
						thrownException = new Exception(MessageFormat.format(Messages.Upgrader700To741_UpgradeErrorMsage, 
								UpgradeEngine.UpgradeFromVersion.V810.fromVersion, 
								UpgradeEngine.UpgradeFromVersion.V810.toVersion), e); 
					}
				}
			});
		}
		if (thrownException != null)
			throw thrownException;

		monitor.done();
	}

	private void processCategory(Category category, List<CategoryAttribute> parentAttributes, Connection c,
			PreparedStatement attributeSelect, PreparedStatement categoryKidSelect, 
			PreparedStatement insertQuery, PreparedStatement updateQuery) throws SQLException {

		int order = 1;
		
		//find the existing root attributes for this category
		List<Attribute> attributes = new ArrayList<>();
		attributeSelect.setObject(1, UuidUtils.uuidToByte(category.getUuid()));			
		try(ResultSet rs = attributeSelect.executeQuery()){
			while(rs.next()) {
				byte[] attribute_uuid = rs.getBytes(1);
				boolean isActive = rs.getBoolean(2);
				Attribute temp = new Attribute();
				temp.setIsRequired(isActive);
				temp.setUuid(UuidUtils.byteToUUID(attribute_uuid));
				attributes.add(temp);
			}
		}
		
		for (CategoryAttribute parentAtt: parentAttributes) {
			insertQuery.setBytes(1, UuidUtils.uuidToByte(category.getUuid()));
			insertQuery.setBytes(2, UuidUtils.uuidToByte(parentAtt.getAttribute().getUuid()));
			insertQuery.setBoolean(3, false); //root
			insertQuery.setBoolean(4, parentAtt.getIsActive() && category.getIsActive()); //active
			insertQuery.setInt(5, order++); //order
			insertQuery.addBatch();
		}
		insertQuery.executeBatch();
		
		//find the existing root attributes for this category
		attributeSelect.setObject(1, UuidUtils.uuidToByte(category.getUuid()));			
		for (Attribute temp : attributes) {
			CategoryAttribute  cao = new CategoryAttribute();
			cao.setCategory(category);
			cao.setAttribute(temp);
			cao.setOrder(order++);
			cao.setIsActive(temp.getIsRequired());
			parentAttributes.add(cao);
				
			updateQuery.setInt(1, cao.getOrder());
			updateQuery.setBytes(2, UuidUtils.uuidToByte(temp.getUuid()));
			updateQuery.setBytes(3, UuidUtils.uuidToByte(category.getUuid()));
			updateQuery.addBatch();
				
			
		}	
		updateQuery.executeBatch();
		
		
		//process children
		List<Category> toProcess = new ArrayList<>();
		categoryKidSelect.setObject(1, UuidUtils.uuidToByte(category.getUuid()));
		try(ResultSet rs = categoryKidSelect.executeQuery()){
			while(rs.next()) {
				byte[] category_uuid = rs.getBytes(1);
				boolean isactive = rs.getBoolean(2);
				Category temp = new Category();		
				temp.setUuid(UuidUtils.byteToUUID(category_uuid));
				temp.setIsActive(isactive);
				toProcess.add(temp);					
			}
		}
		
		for (Category kid : toProcess) {
			processCategory(kid, new ArrayList<>(parentAttributes), c, attributeSelect, categoryKidSelect, insertQuery, updateQuery);
		}
	}
	
	private void upgrade(Connection c, IProgressMonitor monitor)
			throws Exception {

		//ensure patrol_pilot_airplane, patrol_pilot_boat, and foot exist as icons for each ca
		try(Statement s = c.createStatement();
				ResultSet rs = s.executeQuery("select uuid from smart.conservation_area where uuid != x'00000000000000000000000000000000'")){ //$NON-NLS-1$
			
			while(rs.next()) {
				byte[] cauuid = rs.getBytes(1);
				for(String iconKey : new String[] {"patrol_pilot_airplane", "patrol_pilot_boat", "foot"}) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						
					boolean hasicon = false;
					try(PreparedStatement s2 = 
							c.prepareStatement("SELECT uuid from smart.icon where ca_uuid = ? and keyid = ?")){ //$NON-NLS-1$
					
						s2.setBytes(1, cauuid);
						s2.setString(2, iconKey);
						
						try(ResultSet rs2 = s2.executeQuery()){
							hasicon = rs2.next();
						}
					}
					if (!hasicon) {
						byte[] black = null;
						byte[] color = null;
						byte[] line = null;
						try(PreparedStatement ps3 = c.prepareStatement("SELECT uuid FROM smart.iconset WHERE keyid = ? and ca_uuid = ?")){ //$NON-NLS-1$
							ps3.setString(1, FixedIconSet.BLACK.key);
							ps3.setBytes(2, cauuid);
							try(ResultSet rs3 = ps3.executeQuery()){
								if (rs3.next()) {
									black = rs3.getBytes(1);
								}
							}
							
							ps3.setString(1, FixedIconSet.COLOR.key);
							ps3.setBytes(2, cauuid);
							try(ResultSet rs3 = ps3.executeQuery()){
								if (rs3.next()) {
									color = rs3.getBytes(1);
								}
							}
							
							ps3.setString(1, FixedIconSet.LINE.key);
							ps3.setBytes(2, cauuid);
							try(ResultSet rs3 = ps3.executeQuery()){
								if (rs3.next()) {
									line = rs3.getBytes(1);
								}
							}
						}
						
						for (String[] icondef : IconUtils.INSTANCE.SMART_ICON_MAPPING) {
							if (!icondef[0].equalsIgnoreCase(iconKey)) continue;
							
							UUID iconUuid = UUID.randomUUID();
								
							try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.icon(uuid, keyid, ca_uuid) values (?,?,?)")){ //$NON-NLS-1$
								ps3.setBytes(1, UuidUtils.uuidToByte(iconUuid));
								ps3.setString(2, iconKey);
								ps3.setBytes(3, cauuid);
								
								ps3.execute();
							}
							
							try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.i18n_label(language_uuid, element_uuid, value) select uuid, ?, ? from smart.language where isdefault and ca_uuid = ?")){ //$NON-NLS-1$
								ps3.setBytes(1, UuidUtils.uuidToByte(iconUuid));
								ps3.setString(2, icondef[1]);
								ps3.setBytes(3, cauuid);
								ps3.execute();
							}
							
							if (black != null) {
								try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) values (?,?,?, ?)")){ //$NON-NLS-1$
									ps3.setBytes(1, UuidUtils.uuidToByte(UUID.randomUUID()));
									ps3.setBytes(2, UuidUtils.uuidToByte(iconUuid));
									ps3.setBytes(3, black);
									ps3.setString(4, icondef[2]);
									ps3.execute();
								}
							}
							if (line != null) {
								try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) values (?,?,?, ?)")){ //$NON-NLS-1$
									ps3.setBytes(1, UuidUtils.uuidToByte(UUID.randomUUID()));
									ps3.setBytes(2, UuidUtils.uuidToByte(iconUuid));
									ps3.setBytes(3, line);
									ps3.setString(4, icondef[3]);
									ps3.execute();
								}
							}
							if (color != null) {
								try(PreparedStatement ps3 = c.prepareStatement("INSERT INTO smart.iconfile(uuid, icon_uuid, iconset_uuid, filename) values (?,?,?, ?)")){ //$NON-NLS-1$
									ps3.setBytes(1, UuidUtils.uuidToByte(UUID.randomUUID()));
									ps3.setBytes(2, UuidUtils.uuidToByte(iconUuid));
									ps3.setBytes(3, color);
									ps3.setString(4, icondef[4]);
									ps3.execute();
								}
							}	
						}
					}
				}
			}
		}
		
		String[] sql = new String[] {		
			"CREATE TABLE smart.patrol_attribute_tree (uuid char(16) for bit data , patrol_attribute_uuid char(16) for bit data, keyid varchar(128), node_order smallint, parent_uuid char(16) for bit data, is_active boolean, hkey varchar(32672), icon_uuid char(16) for bit data, primary key (uuid))",  //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_patrol_att_uuid_fk FOREIGN KEY(patrol_attribute_uuid) REFERENCES SMART.PATROL_ATTRIBUTE (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_parent_uuid_fk FOREIGN KEY(parent_uuid) REFERENCES smart.patrol_attribute_tree (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_icon_uuid_fk FOREIGN KEY(icon_uuid) REFERENCES smart.icon (UUID) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"ALTER TABLE smart.patrol_attribute_value ADD COLUMN tree_node_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_value ADD CONSTRAINT patrol_att_value_tree_node_uuid_fk FOREIGN KEY(tree_node_uuid) REFERENCES smart.patrol_attribute_tree (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			//patrol types -> track types
			"UPDATE smart.patrol_type SET patrol_type = lower(patrol_type)", //$NON-NLS-1$
			"RENAME COLUMN smart.patrol_type.patrol_type to keyId", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_type alter column keyId set data type varchar(128)", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_type add column uuid char(16) for bit data", //$NON-NLS-1$
			"UPDATE smart.patrol_type set uuid = smart.uuid()", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_type alter column uuid set not null", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_type drop primary key", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_type add primary key (uuid) ", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_type add column requires_pilot boolean default false not null", //$NON-NLS-1$
			"UPDATE smart.patrol_type set requires_pilot = true where keyid in ('marine', 'air')", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_transport add column patrol_type_uuid char(16) for bit data ", //$NON-NLS-1$
			"UPDATE smart.patrol_transport set patrol_type_uuid = (select a.uuid from smart.patrol_type a where a.keyid = lower(smart.patrol_transport.patrol_type) and a.ca_uuid = smart.patrol_transport.ca_uuid)", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_transport drop column patrol_type", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_transport ADD CONSTRAINT pt_patrol_type_uuid_fk FOREIGN KEY(patrol_type_uuid) REFERENCES smart.patrol_type (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"ALTER TABLE smart.patrol add column patrol_type_uuid char(16) for bit data ", //$NON-NLS-1$
			"UPDATE smart.patrol set patrol_type_uuid = (select a.uuid from smart.patrol_type a where a.keyid = lower(smart.patrol.patrol_type) and a.ca_uuid = smart.patrol.ca_uuid)", //$NON-NLS-1$
			"ALTER TABLE smart.patrol drop column patrol_type", //$NON-NLS-1$
			"ALTER TABLE smart.patrol ADD CONSTRAINT patrol_patrol_type_uuid_fk FOREIGN KEY(patrol_type_uuid) REFERENCES smart.patrol_type (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"ALTER TABLE smart.patrol_type add column icon_uuid char(16) for bit data ", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_type add constraint patrol_type_icon_uuid_fk foreign key (icon_uuid) references smart.icon(uuid) on update restrict on delete set null deferrable initially immediate", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_type add constraint patrol_type_unq unique(ca_uuid, keyid)", //$NON-NLS-1$

			//TODO: MIXED???
			"update smart.patrol_type set icon_uuid = (select a.uuid from smart.icon a where a.keyid = 'foot' and a.ca_uuid = smart.patrol_type.ca_uuid) where smart.patrol_type.keyid = 'ground'", //$NON-NLS-1$
			"update smart.patrol_type set icon_uuid = (select a.uuid from smart.icon a where a.keyid = 'patrol_pilot_boat' and a.ca_uuid = smart.patrol_type.ca_uuid) where smart.patrol_type.keyid = 'marine'", //$NON-NLS-1$
			"update smart.patrol_type set icon_uuid = (select a.uuid from smart.icon a where a.keyid = 'patrol_pilot_airplane' and a.ca_uuid = smart.patrol_type.ca_uuid) where smart.patrol_type.keyid = 'air'", //$NON-NLS-1$
			
			
			"ALTER TABLE smart.dm_cat_att_map ADD COLUMN is_root boolean", //$NON-NLS-1$
			"UPDATE smart.dm_cat_att_map set is_root = true", //$NON-NLS-1$
			
		};
		
		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
		}
		
		//insert i18n names for patrol types
		HashMap<String, String> ptTranslations = new HashMap<>();
		ptTranslations.put("air_en", "Air"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_en", "Ground"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_en", "Mixed"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_en", "Water"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_ar", "\u0647\u0648\u0627\u0621"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_ar", "\u0623\u0631\u0636\u064a"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_ar", "\u0645\u062e\u062a\u0644\u0637"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_ar", "\u0645\u0627\u0621"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_es", "Aereo"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_es", "Terrestre"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_es", "Mixto"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_es", "Acuatico"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_fr", "A\u00e9rien"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_fr", "Terrestre"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_fr", "Mixte"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_fr", "Eau"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_in", "Udara"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_in", "Darat"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_in", "Gabungan"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_in", "Air"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_ka", "\u10e1\u10d0\u10f0\u10d0\u10d4\u10e0\u10dd"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_ka", "\u10e1\u10d0\u10ee\u10db\u10d4\u10da\u10d4\u10d7\u10dd"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_ka", "\u10d0\u10e0\u10d4\u10e3\u10da\u10d8"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_ka", "\u10ec\u10e7\u10da\u10d8\u10e1"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_km", "\u1781\u17d2\u1799\u179b\u17cb"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_km", "\u178a\u17b8"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_km", "\u179b\u17b6\u1799"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_km", "\u1791\u17b9\u1780"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_lo", "\u0e97\u0eb2\u0e87\u200b\u0ead\u0eb2\u200b\u0e81\u0eb2\u0e94"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_lo", "\u0e97\u0eb2\u0e87\u0e9e\u0eb7\u0ec9\u0e99\u0e94\u0eb4\u0e99"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_mn", "\u0410\u0433\u0430\u0430\u0440"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_mn", "\u0413\u0430\u0437\u0430\u0440"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_mn", "\u0425\u043e\u0441\u043b\u043e\u0441\u043e\u043d"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_mn", "\u0423\u0441"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_ms", "Udara"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_ms", "Ground"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_pt", "A\u00e9reo"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_pt", "Terrestre"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_pt", "Misto"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_pt", "Aqu\u00e1tico"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_ru", "\u0412\u043e\u0437\u0434\u0443\u0448\u043d\u044b\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_ru", "\u041d\u0430\u0437\u0435\u043c\u043d\u044b\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_ru", "\u0421\u043c\u0435\u0448\u0430\u043d\u043d\u044b\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_ru", "\u0412\u043e\u0434\u043d\u044b\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_sw", "Hewa"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_sw", "Uwanja"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_sw", "Mchanganyiko");  //$NON-NLS-1$//$NON-NLS-2$
		ptTranslations.put("marine_sw", "Maji"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_th", "\u0e17\u0e32\u0e07\u0e2d\u0e32\u0e01\u0e32\u0e28"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_th", "\u0e17\u0e32\u0e07\u0e1e\u0e37\u0e49\u0e19\u0e14\u0e34\u0e19"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_th", "\u0e1c\u0e2a\u0e21\u0e1c\u0e2a\u0e32\u0e19"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_th", "\u0e17\u0e32\u0e07\u0e19\u0e49\u0e33"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_uk", "\u041f\u043e\u0432\u0456\u0442\u0440\u044f"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_uk", "\u0417\u0435\u043c\u043b\u044f"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_uk", "\u0417\u043c\u0456\u0449\u0430\u043d\u0438\u0439"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_uk", "\u0412\u043e\u0434\u0430"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_vi", "\u0110\u01b0\u1eddng kh\u00f4ng"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_vi", "\u0110\u01b0\u1eddng b\u1ed9"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_vi", "H\u1ed7n h\u1ee3p"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_vi", "\u0110\u01b0\u1eddng th\u1ee7y"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("air_zh", "\u822a\u7a7a"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("ground_zh", "\u5730\u9762"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("mixed_zh", "\u6df7\u5408"); //$NON-NLS-1$ //$NON-NLS-2$
		ptTranslations.put("marine_zh", "\u6c34\u4e0a"); //$NON-NLS-1$ //$NON-NLS-2$
		
		String query = "select pt.uuid, pt.keyid, l.code, l.uuid, l.isdefault from smart.patrol_type pt join smart.language l on pt.ca_uuid = l.ca_uuid"; //$NON-NLS-1$
		String insertQuery2 = "insert into smart.i18n_label (language_uuid, element_uuid, value) values (?, ?, ?)"; //$NON-NLS-1$
		try(Statement s = c.createStatement(); PreparedStatement psinsert = c.prepareStatement(insertQuery2);
				ResultSet rs = s.executeQuery(query)){ 
			while(rs.next()) {
				byte[] elementuuid = rs.getBytes(1);
				byte[] languageuuid = rs.getBytes(4);
				String code = rs.getString(3);
				String key = rs.getString(2);
				boolean isDefault = rs.getBoolean(5);
				
				String lookup = key + "_" + I18nUtil.stringToLocale(code).getCountry(); //$NON-NLS-1$
				if (!ptTranslations.containsKey(lookup) && isDefault) {
					lookup = key + "_en"; //$NON-NLS-1$
				}
				String value = ptTranslations.get(lookup);
				if (value == null) value = key;
				
				String y = new String(value.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
				
				psinsert.setBytes(1, languageuuid);
				psinsert.setBytes(2, elementuuid);
				psinsert.setString(3, y);
				psinsert.addBatch();
				
			}
			psinsert.executeBatch();
		}
		
		//populate dm_cat_att_map table
		//with child category/attribute objects
		//so we can order them correctly 
		//https://app.assembla.com/spaces/smart-cs/tickets/3297		
		
		
		String attributeQuery = "select attribute_uuid, is_active from smart.dm_cat_att_map where category_uuid = ? order by att_order"; //$NON-NLS-1$
		String cateogryQuery = "select uuid, is_active from smart.dm_category where parent_category_uuid = ?"; //$NON-NLS-1$
		String insertQuery = "insert into smart.dm_cat_att_map (category_uuid, attribute_uuid, is_root, is_active, att_order) values (?,?,?,?,?)"; //$NON-NLS-1$
		String updateQuery = "update smart.dm_cat_att_map set att_order = ? where attribute_uuid = ? and category_uuid = ?"; //$NON-NLS-1$
		
		List<Category> toProcess = new ArrayList<>();
		try(Statement s = c.createStatement();
				ResultSet rs = s.executeQuery("SELECT uuid, is_active FROM smart.dm_category WHERE parent_category_uuid is null")){ //$NON-NLS-1$
			while(rs.next()) {
				Category temp = new Category();
				temp.setUuid(UuidUtils.byteToUUID(rs.getBytes(1)));
				temp.setIsActive(rs.getBoolean(2));
				toProcess.add(temp);
			}
		}
		
		try(PreparedStatement attributeSelect = c.prepareStatement(attributeQuery);
				PreparedStatement categorySelect = c.prepareStatement(cateogryQuery);
				PreparedStatement insertStatement = c.prepareStatement(insertQuery);
				PreparedStatement updateStatement = c.prepareStatement(updateQuery);){
						
			for(Category category : toProcess) {
				processCategory(category, new ArrayList<>(), c, attributeSelect, categorySelect, insertStatement, updateStatement);
			}
		}
		
		/* VERSION UDATE */
		String ssql = "update smart.db_version set version = '" + UpgradeEngine.UpgradeFromVersion.V810.toVersion + "' where plugin_id = 'org.wcs.smart'"; //$NON-NLS-1$ //$NON-NLS-2$
		c.createStatement().execute(ssql);
		
	}

}

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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;
import org.wcs.smart.upgrade.UpgradeEngine;
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

		String[] sql = new String[] {		
			"CREATE TABLE smart.patrol_attribute_tree (uuid char(16) for bit data , patrol_attribute_uuid char(16) for bit data, keyid varchar(128), node_order smallint, parent_uuid char(16) for bit data, is_active boolean, hkey varchar(32672), icon_uuid char(16) for bit data, primary key (uuid))",  //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_patrol_att_uuid_fk FOREIGN KEY(patrol_attribute_uuid) REFERENCES SMART.PATROL_ATTRIBUTE (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_parent_uuid_fk FOREIGN KEY(parent_uuid) REFERENCES smart.patrol_attribute_tree (UUID) ON UPDATE RESTRICT ON DELETE CASCADE DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_tree ADD CONSTRAINT patrol_att_tree_icon_uuid_fk FOREIGN KEY(icon_uuid) REFERENCES smart.icon (UUID) ON UPDATE RESTRICT ON DELETE SET NULL DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"ALTER TABLE smart.patrol_attribute_value ADD COLUMN tree_node_uuid char(16) for bit data", //$NON-NLS-1$
			"ALTER TABLE smart.patrol_attribute_value ADD CONSTRAINT patrol_att_value_tree_node_uuid_fk FOREIGN KEY(tree_node_uuid) REFERENCES smart.patrol_attribute_tree (UUID) ON UPDATE RESTRICT ON DELETE RESTRICT DEFERRABLE INITIALLY IMMEDIATE", //$NON-NLS-1$
			
			"ALTER TABLE smart.dm_cat_att_map ADD COLUMN is_root boolean", //$NON-NLS-1$
			"UPDATE smart.dm_cat_att_map set is_root = true", //$NON-NLS-1$
			
		};
		
		for (String s : sql) {
			SmartPlugIn.logInfo(s);
			c.createStatement().execute(s);
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

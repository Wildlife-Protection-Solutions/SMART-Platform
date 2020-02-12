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
package org.wcs.smart.paws.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.paws.model.PawsSimpleClass;

/**
 * Engine for filtering simple data filters.
 * 
 * @author Emily
 *
 */
public class SimpleClassEngine {
	
	private PawsSimpleClass pc;
	private String mastertable;

	/**
	 * 
	 * @param pc
	 * @param mastertable table of all waypoints
	 */
	public SimpleClassEngine(PawsSimpleClass pc, String mastertable) {
		this.pc = pc;
		this.mastertable = mastertable;
	}
	
	public void process(Session session, String tablename) {
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE "); //$NON-NLS-1$
				sb.append( tablename );
				sb.append( "(obs_uuid char(16) for bit data, pawsclass varchar(8192))"); //$NON-NLS-1$
				
				c.createStatement().execute(sb.toString());
				
				sb = new StringBuilder();
				sb.append(" INSERT INTO "); //$NON-NLS-1$
				sb.append(tablename);
				sb.append(" SELECT obs.uuid, ? "); //$NON-NLS-1$
				sb.append(" FROM "); //$NON-NLS-1$
				sb.append( mastertable + " wp "); //$NON-NLS-1$
				sb.append(" JOIN smart.wp_observation obs ON obs.uuid = wp.obs_uuid " ); //$NON-NLS-1$
				sb.append(" JOIN smart.dm_category c ON obs.category_uuid = c.uuid " ); //$NON-NLS-1$
				
				if (pc.getAttributeListItemKey() != null || pc.getAttributeTreeNodeHkey() != null) {
					sb.append(" JOIN smart.wp_observation_attributes oba ON obs.uuid = oba.observation_uuid " ); //$NON-NLS-1$
					sb.append(" JOIN smart.dm_attribute a on oba.attribute_uuid = a.uuid and a.keyid = ? "); //$NON-NLS-1$
					if (pc.getAttributeListItemKey() != null){
						sb.append(" JOIN smart.dm_attribute_list al on al.uuid = oba.list_element_uuid and al.keyid = ? "); //$NON-NLS-1$
					}
					if (pc.getAttributeTreeNodeHkey() != null){
						sb.append(" JOIN smart.dm_attribute_tree att on att.uuid = oba.tree_node_uuid and ( att.hkey >= ? and att.hkey < ? )" ); //$NON-NLS-1$
					}
				}
				
				
				sb.append(" WHERE "); //$NON-NLS-1$
				sb.append(" (c.hkey >= ?  and c.hkey < ? ) "); //$NON-NLS-1$
		
				PreparedStatement ps = c.prepareStatement(sb.toString());
				int index = 1;
				ps.setString(index++, pc.getClassification());
				if (pc.getAttributeListItemKey() != null){
					ps.setString(index++,  pc.getAttributeKey());
					ps.setString(index++, pc.getAttributeListItemKey());
				}else if (pc.getAttributeTreeNodeHkey() != null){
					ps.setString(index++,  pc.getAttributeKey());
					ps.setString(index++, pc.getAttributeTreeNodeHkey());
					ps.setString(index++, pc.getAttributeTreeNodeHkey().substring(0, pc.getAttributeTreeNodeHkey().length() - 1) + "/"); //$NON-NLS-1$
				}

				ps.setString(index++, pc.getCategoryHkey());
				ps.setString(index++, pc.getCategoryHkey().substring(0, pc.getCategoryHkey().length() - 1) + "/"); //$NON-NLS-1$
				ps.execute();
			}
		});
	}
}

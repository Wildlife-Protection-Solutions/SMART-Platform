package org.wcs.smart.paws.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.paws.model.PawsClassification;

public class SimpleClassEngine {

	private PawsClassification pc;
	private LocalDate startDate;
	private LocalDate endDate;
	
	public SimpleClassEngine(PawsClassification pc, LocalDate startDate, LocalDate endDate) {
		this.pc = pc;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public void process(Session session, String tablename) {
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE ");
				sb.append( tablename );
				sb.append( "(obs_uuid char(16) for bit data, pawsclass varchar(8192))");
				System.out.println(sb.toString());
				c.createStatement().execute(sb.toString());
				
				sb = new StringBuilder();
				sb.append(" INSERT INTO ");
				sb.append(tablename);
				sb.append(" SELECT obs.uuid, ? ");
				sb.append(" FROM smart.waypoint wp ");
				sb.append(" JOIN smart.wp_observation obs ON obs.wp_uuid = wp.uuid " );
				sb.append(" JOIN smart.dm_category c ON obs.category_uuid = c.uuid " );
				
				if (pc.getAttributeListItemKey() != null || pc.getAttributeTreeNodeHkey() != null) {
					sb.append(" JOIN smart.wp_observation_attributes oba ON obs.uuid = oba.observation_uuid " );
					sb.append(" JOIN smart.dm_attribute a on oba.attribute_uuid = a.uuid and a.keyid = ? ");
					if (pc.getAttributeListItemKey() != null){
						sb.append(" JOIN smart.dm_attribute_list al on al.uuid = oba.list_element_uuid and al.keyid = ? ");
					}
					if (pc.getAttributeTreeNodeHkey() != null){
						sb.append(" JOIN smart.dm_attribute_tree att on att.uuid = oba.tree_node_uuid and ( att.hkey >= ? and att.hkey < ? )" );
					}
				}
				
				
				sb.append(" WHERE ");
				sb.append(" wp.datetime between ? and ?");
				sb.append(" and (c.hkey >= ?  and c.hkey < ? ) ");
		
				//TODO: filter on CA or CCAA depending on 
				//analysis 
				
				System.out.println(sb.toString());
				PreparedStatement ps = c.prepareStatement(sb.toString());
				int index = 1;
				ps.setString(index++, pc.getClassification());
				if (pc.getAttributeListItemKey() != null){
					ps.setString(index++,  pc.getAttributeKey());
					ps.setString(index++, pc.getAttributeListItemKey());
				}else if (pc.getAttributeTreeNodeHkey() != null){
					ps.setString(index++,  pc.getAttributeKey());
					ps.setString(index++, pc.getAttributeTreeNodeHkey());
					ps.setString(index++, pc.getAttributeTreeNodeHkey().substring(0, pc.getAttributeTreeNodeHkey().length() - 1) + "/");
				}
				
				ps.setTimestamp(index++, java.sql.Timestamp.valueOf(startDate.atStartOfDay()));
				ps.setTimestamp(index++, java.sql.Timestamp.valueOf(endDate.atTime(23, 59, 59)));
				ps.setString(index++, pc.getCategoryHkey());
				ps.setString(index++, pc.getCategoryHkey().substring(0, pc.getCategoryHkey().length() - 1) + "/");
			
				ps.execute();
			}
		});
	

		//execute query and do what with the results
	}
}

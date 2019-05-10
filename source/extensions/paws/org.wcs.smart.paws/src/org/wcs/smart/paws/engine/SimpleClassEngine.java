package org.wcs.smart.paws.engine;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.paws.model.PawsSimpleClass;
import org.wcs.smart.util.UuidUtils;

public class SimpleClassEngine {

	
	private PawsSimpleClass pc;
	private LocalDate startDate;
	private LocalDate endDate;
	
	public SimpleClassEngine(PawsSimpleClass pc, LocalDate startDate, LocalDate endDate) {
		this.pc = pc;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	public void process(Session session, String tablename) {
		
		session.doWork(new Work() {
			@Override
			public void execute(Connection c) throws SQLException {
				// TODO Auto-generated method stub
				StringBuilder sb = new StringBuilder();
				sb.append("CREATE TABLE ");
				sb.append( tablename );
				sb.append( "(wp_uuid char(16) for bit data, obs_uuid char(16) for bit data, x double, y double, datetime timestamp, pawsclass varchar(8192))");
				c.createStatement().execute(sb.toString());
				
				
				sb = new StringBuilder();
				sb.append(" INSERT INTO ");
				sb.append(tablename);
				sb.append(" SELECT wp.uuid, obs.uuid, wp.x, wp.y, wp.datetime, ? ");
				sb.append(" FROM smart.waypoint wp ");
				sb.append(" JOIN smart.wp_observation obs ON obs.wp_uuid = wp.uuid " );
				
				if (pc.getAttributeListItem() != null || pc.getAttributeTreeNode() != null) {
					sb.append(" JOIN smart.wp_observation_attributes oba ON obs.uuid = oba.observation_uuid " );
				}
				
				sb.append(" WHERE ");
				sb.append(" wp.datetime between ? and ?");
				sb.append(" and obs.category_uuid = ? ");
				
				if (pc.getAttributeTreeNode() != null) {
					sb.append(" AND ");
					sb.append( "oba.tree_node_uuid = ? ");
				}else if (pc.getAttributeListItem() != null) {
					sb.append(" AND ");
					sb.append( "oba.list_element_uuid = ? ");
				}
				System.out.println(sb.toString());
				PreparedStatement ps = c.prepareStatement(sb.toString());
				ps.setString(1, pc.getClassification());
				ps.setDate(2, java.sql.Date.valueOf(startDate));
				ps.setDate(3, java.sql.Date.valueOf(endDate));
				ps.setBytes(4, UuidUtils.uuidToByte(pc.getCategory().getUuid()));
				if (pc.getAttributeTreeNode() != null) {
					ps.setBytes(5, UuidUtils.uuidToByte(pc.getAttributeTreeNode().getUuid()));
				}else if (pc.getAttributeListItem() != null) {
					ps.setBytes(5, UuidUtils.uuidToByte(pc.getAttributeListItem().getUuid()));
				}
				ps.execute();
			}
		});
	

		//execute query and do what with the results
	}
}

package org.wcs.smart.connect.replication.changelog;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.util.UuidUtils;

public class ChangeLogApplier {

	
	public static void applyChangeLog(){
		Session s = HibernateManager.openSession();
		s.doWork(new Work(){

			@Override
			public void execute(Connection connection) throws SQLException {
				
				File f = new File("C:\\temp\\smartchangelog.log");
				
				try(FileInputStream fin = new FileInputStream("C:\\temp\\smartchangelog.log");
						ObjectInputStream oin = new ObjectInputStream(fin)){
					int size = oin.readInt();
				
					for (int i = 0; i < size; i ++){
						ChangeLogItem it = (ChangeLogItem) oin.readObject();
						
						System.out.println(it.getAction().name() + ":  " + it.getTableName() + ":" + it.getFieldName1() + ":" + it.getKey1().toString());
						
						if (it.getAction() == Action.DELETE){
							processDelete(it, connection);
						}else if (it.getAction() == Action.INSERT){
							processInsert(it, oin, connection);
						}else if (it.getAction() == Action.UPDATE){
							processUpdate(it, oin, connection);
						}else{
							//filestore type do nothing with database
						}
					}
				}catch (Exception ex){
					throw new SQLException (ex);
				}
				connection.commit();
			}
			
		});
		
		
	}
	
	private static void processDelete(ChangeLogItem item, Connection c) throws SQLException{
		
		StringBuilder sb = new StringBuilder();
		sb.append("DELETE FROM " + item.getTableName());
		sb.append(" WHERE " + item.getFieldName1() + " = ?");
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?");
		}
		PreparedStatement ps = c.prepareStatement(sb.toString());
		ps.setBytes(1, UuidUtils.uuidToByte(item.getKey1()));
		if (item.getKey2() != null){
			ps.setBytes(2, UuidUtils.uuidToByte(item.getKey2()));	
		}else if (item.getKey2String() != null){
			ps.setString(2, item.getKey2String());
		}
		int up = ps.executeUpdate();
		if (up != 1){
			throw new SQLException("Invalid number of row deleted.");
		}
	}
	
	private static void processUpdate(ChangeLogItem item, ObjectInputStream is, Connection c) throws ClassNotFoundException, IOException, SQLException{
		StringBuilder sb = new StringBuilder();
		sb.append("UPDATE " + item.getTableName());
		sb.append("SET ");
		
		int numCols = is.readInt();
		List<Object> params = new ArrayList<Object>();
		for (int i = 0; i < numCols; i ++){
			String name = (String) is.readObject();
			int type = is.readInt();
			sb.append(name + " = ?");
			
			if (type == Types.BLOB){
				long length = is.readLong();
				byte[] data = new byte[(int)length];
				is.readFully(data);
				params.add(data);
			}else{
				params.add(is.readObject());
			}
		}
		
		sb.append(" WHERE " + item.getFieldName1() + " = ?");
		if (item.getFieldName2() != null){
			sb.append(" AND " + item.getFieldName2()  + " = ?");
		}
		PreparedStatement ps = c.prepareStatement(sb.toString());
		for (int i = 1; i <= params.size(); i ++){
			ps.setObject(i, params.get(i-1));
		}
		int cnt = ps.executeUpdate();
		if (cnt != 1){
			throw new SQLException("Invalid number of rows updated.");
		}
	}
	
	private static void processInsert(ChangeLogItem item, ObjectInputStream is, Connection c) throws ClassNotFoundException, IOException, SQLException{
		StringBuilder sb = new StringBuilder();
		sb.append("INSERT INTO " + item.getTableName() + "(");
		
		StringBuilder values = new StringBuilder();
		values.append("VALUES(");
		
		int numCols = is.readInt();
		List<Object> params = new ArrayList<Object>();
		for (int i = 0; i < numCols; i ++){
			String name = (String) is.readObject();
			int type = is.readInt();
			sb.append(name + ",");
			values.append("?,");
			
			if (type == Types.BLOB){
				long length = is.readLong();
				byte[] data = new byte[(int)length];
				is.readFully(data);
				params.add(data);
			}else{
				params.add(is.readObject());
			}
		}
		sb.deleteCharAt(sb.length() - 1);
		values.deleteCharAt(values.length() - 1);
		
		sb.append(") ");
		sb.append(values.toString());
		sb.append(")");
		PreparedStatement ps = c.prepareStatement(sb.toString() );
		for (int i = 1; i <= params.size(); i ++){
			ps.setObject(i, params.get(i-1));
		}
		int cnt = ps.executeUpdate();
		if (cnt != 1){
			throw new SQLException("Invalid number of rows updated.");
		}
	}
}

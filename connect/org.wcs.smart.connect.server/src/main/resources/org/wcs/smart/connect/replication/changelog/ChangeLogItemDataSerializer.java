package org.wcs.smart.connect.replication.changelog;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.ByteBuffer;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.wcs.smart.connect.replication.changelog.ChangeLogItem.Action;
import org.wcs.smart.util.UuidUtils;

public class ChangeLogItemDataSerializer {

	
	public static void serializeData(ObjectOutputStream stream,
			ChangeLogItem item, Connection c) throws SQLException, IOException{
		
		if (item.getAction() == Action.FS_DELETE || 
				item.getAction() == Action.FS_UPDATE || 
				item.getAction() == Action.FS_INSERT ){
			//TODO: ignore for now
			return;
		}
			
		//do not record data for delete actions
		if (item.getAction()== Action.DELETE) return;
		
		if (item.getFieldName2() != null 
				&& (item.getKey2() == null && item.getKey2String() == null)){
			throw new SQLException("Primary key values not set for key 2");
		}
		if (item.getKey1() == null){
			throw new SQLException("Pirmary key values not set for key 1");
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM ");
		sb.append(item.getTableName());
		sb.append(" WHERE ");
		sb.append(item.getFieldName1() + " = ? ");
		if (item.getFieldName2() != null){
			sb.append(item.getFieldName2() + " = ? " );
		}
		PreparedStatement ps = c.prepareStatement(sb.toString());
		UUID key1 = item.getKey1();
		ps.setObject(1, key1);
		if (item.getKey2() != null){
			UUID key2 = item.getKey2();
			ps.setObject(2, key2);
		}else if (item.getKey2String() != null){
			ps.setString(2, item.getKey2String());
		}
		
		ResultSet rs = ps.executeQuery();
		if (!rs.next()){
			throw new SQLException("Not value found for object: " + item.getTableName() + " _ " + item.getFieldName1() + " _ " + item.getKey1().toString());
		}
		
		ResultSetMetaData md = rs.getMetaData();
		int colCnt = md.getColumnCount();
		stream.writeInt(colCnt);
		for (int i = 1; i <= colCnt; i ++){
			stream.writeObject(md.getColumnName(i));
			stream.writeInt(md.getColumnType(i));
			
			if (md.getColumnType(i) == Types.BLOB){
				Blob b = rs.getBlob(i);
				stream.writeLong(b.length());
				IOUtils.copy(b.getBinaryStream(), stream);
			}else{
				Object x = rs.getObject(i);
				stream.writeObject(x);	
			}
		}
	}
	
}

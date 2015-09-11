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
package org.wcs.smart.connect.replication.changelog;

import java.io.IOException;
import java.io.ObjectOutputStream;
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


/**
 * Tool for serializing change log items and associated data.
 * 
 * @author Emily
 *
 */
public abstract class ChangeLogItemSerializer {

	public abstract void prepareUuid(PreparedStatement ps, int index, UUID value) throws SQLException;
	
	public void serialize(ObjectOutputStream stream,
			ChangeLogItem item, Connection c) throws SQLException, IOException{
		
		if (item.getAction() == Action.FS_DELETE || 
				item.getAction() == Action.FS_UPDATE || 
				item.getAction() == Action.FS_INSERT ){
			//TODO: ignore for now
			stream.writeObject(item);
			return;
		}
			
		//do not record data for delete actions
		if (item.getAction()== Action.DELETE){
			stream.writeObject(item);
			return;
		}
		
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
			sb.append(" AND ");
			sb.append(item.getFieldName2() + " = ? " );
		}
		PreparedStatement ps = c.prepareStatement(sb.toString());
		UUID key1 = item.getKey1();
//		ps.setBytes(1, UuidUtils.uuidToByte(key1));
		prepareUuid(ps, 1, key1);
		if (item.getKey2() != null){
			UUID key2 = item.getKey2();
//			ps.setBytes(2, UuidUtils.uuidToByte(key2));
			prepareUuid(ps, 2, key2);
		}else if (item.getKey2String() != null){
			ps.setString(2, item.getKey2String());
		}
		
		ResultSet rs = ps.executeQuery();
		if (!rs.next()){
			//throw new SQLException("Not value found for object: " + item.getTableName() + " _ " + item.getFieldName1() + " _ " + item.getKey1().toString());
			//not data was found for the object;
			//this should be because it is deleted later in the change log history
			//TODO: for validation purposes we could valid this is the case
			item.setAction(Action.DELETE);
			stream.writeObject(item);
			return;
		}
		
		//serialize change log item
		stream.writeObject(item);
		
		//serialize data
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

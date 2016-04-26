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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.UUID;

import org.apache.commons.io.IOUtils;
import org.wcs.smart.SmartContext;
import org.wcs.smart.connect.model.ChangeLogItem;
import org.wcs.smart.connect.model.ChangeLogItem.Action;
import org.wcs.smart.util.UuidUtils;


/**
 * Tool for serializing change log items and associated data.
 * 
 * @author Emily
 *
 */
public abstract class ChangeLogItemSerializer {

	public abstract void prepareUuid(PreparedStatement ps, int index, UUID value) throws SQLException;
	
	protected abstract int convertType(int type, int precision);
	
	public void serialize(ObjectOutputStream stream,
			ChangeLogItem item, 
			Path packageFileLocation, Connection c) throws SQLException, IOException{
		
		if (item.getAction() == Action.FS_DELETE){
			stream.writeObject(item);
			return;
		}
		if (item.getAction() == Action.FS_UPDATE || 
			item.getAction() == Action.FS_INSERT ){
			stream.writeObject(item);
			
			//copy file
			Path sourcePath = FileSystems.getDefault()
					.getPath(SmartContext.INSTANCE.getFilestoreLocation())
					.resolve(item.getFileName());
			if (!Files.isDirectory(sourcePath)){
				Path toPath = packageFileLocation.resolve(item.getFileName());
				Files.createDirectories(toPath.getParent());
				if(!Files.exists(toPath)){
					if (Files.exists(sourcePath)){
						Files.copy(sourcePath, toPath);
					}
				}
			}
			return;
		}
			
		//do not record data for delete actions
		if (item.getAction()== Action.DELETE){
			stream.writeObject(item);
			return;
		}
		
		if (item.getFieldName2() != null 
				&& (item.getKey2() == null && item.getKey2String() == null)){
			throw new SQLException("Primary key values not set for key 2"); //$NON-NLS-1$
		}
		if (item.getKey1() == null){
			throw new SQLException("Pirmary key values not set for key 1"); //$NON-NLS-1$
		}
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM "); //$NON-NLS-1$
		sb.append(item.getTableName());
		sb.append(" WHERE "); //$NON-NLS-1$
		sb.append(item.getFieldName1() + " = ? "); //$NON-NLS-1$
		if (item.getFieldName2() != null){
			sb.append(" AND "); //$NON-NLS-1$
			sb.append(item.getFieldName2() + " = ? " ); //$NON-NLS-1$
		}
		try(PreparedStatement ps = c.prepareStatement(sb.toString())){
			UUID key1 = item.getKey1();
			prepareUuid(ps, 1, key1);
			
			if (item.getKey2() != null){
				UUID key2 = item.getKey2();
				prepareUuid(ps, 2, key2);
			}else if (item.getKey2String() != null){
				ps.setString(2, item.getKey2String());
			}
			
			ResultSet rs = ps.executeQuery();
			if (!rs.next()){
				//not data was found for the object;
				//this should be because it is deleted later in the change log history
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
				int type = convertType(md.getColumnType(i), md.getPrecision(i));
				stream.writeInt(type);
				
				if (type == Types.BLOB){
					Blob b = rs.getBlob(i);
					stream.writeLong(b.length());
					IOUtils.copy(b.getBinaryStream(), stream);
				}else if (type == Types.BINARY){
						
					byte[] parts = rs.getBytes(i);
					stream.writeLong(parts.length);
					try(ByteArrayInputStream bis = new ByteArrayInputStream(parts)){
						IOUtils.copy(bis, stream);
					}
				}else if (type == Types.OTHER){
					//uuid
					Object data = rs.getObject(i);
					if (data == null){
						stream.writeObject((UUID)null);
					}else if (data instanceof UUID){
						stream.writeObject((UUID)data);
					}else if (data instanceof byte[]){
						stream.writeObject( UuidUtils.byteToUUID((byte[])data) );
					}else{
						throw new IllegalStateException("Invalid representation of UUID."); //$NON-NLS-1$
					}
				}else if (type == Types.CLOB){
					Clob clob = rs.getClob(i);
					stream.writeLong(clob.length());
					try(Reader reader = clob.getCharacterStream() ){
						IOUtils.copy(reader, stream);
					}
				}else{
					Object x = rs.getObject(i);
					stream.writeObject(x);	
				}
			}
		}
	}
}

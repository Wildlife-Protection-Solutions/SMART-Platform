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
package org.wcs.smart.connect.model;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;

/**
 * Change log item.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.connect_change_log")
public class ChangeLogItem extends UuidItem implements Externalizable{

	private static final long serialVersionUID = 01L;
	
	public enum Action{
		INSERT,
		UPDATE,
		DELETE,
		FS_INSERT,
		FS_UPDATE,
		FS_DELETE
	}
	
	public enum Source{
		LOCAL,
		SERVER
	}
	
	private long revision;
	private Action action;
	private String fileName;
	private String tableName;
	
	private String fieldName1;
	private UUID key1;
	
	private String fieldName2;
	private UUID key2;
	private String key2str;
	
	private UUID caUuid;
	private Source source; 
	
	@Column(name="revision")
	public Long getRevision(){
		return this.revision;
	}
	
	public void setRevision(long revision){
		this.revision = revision;
	}
	
	@Column(name="action")
	@Enumerated(EnumType.STRING)
	public Action getAction(){
		return this.action;
	}
	
	public void setAction(Action action){
		this.action = action;
	}
	
	@Column(name="filename")
	public String getFileName(){
		return this.fileName;
	}
	public void setFileName(String filename){
		this.fileName = filename;
	}
	
	@Column(name="tablename")
	public String getTableName(){
		return this.tableName;
	}
	public void setTableName(String tablename){
		this.tableName = tablename;
	}
	
	@Column(name="key1_fieldname")
	public String getFieldName1(){
		return this.fieldName1;
	}
	public void setFieldName1(String fieldName1){
		this.fieldName1 = fieldName1;
	}
	
	@Column(name="key1")
	public UUID getKey1(){
		return this.key1;
	}
	public void setKey1(UUID key1){
		this.key1 = key1;
	}
	
	@Column(name="key2_fieldname")
	public String getFieldName2(){
		return this.fieldName2;
	}
	public void setFieldName2(String fieldName2){
		this.fieldName2 = fieldName2;
	}
	
	@Column(name="key2_uuid")
	public UUID getKey2(){
		return this.key2;
	}
	public void setKey2(UUID key2){
		this.key2 = key2;
	}
	
	@Column(name="key2_str")
	public String getKey2String(){
		return this.key2str;
	}
	public void setKey2String(String key2str){
		this.key2str = key2str;
	}
	
	@Column(name="ca_uuid")
	public UUID getConservationArea(){
		return this.caUuid;
	}
	public void setConservationArea(UUID ca){
		this.caUuid = ca;
	}

	@Column(name="source")
	@Enumerated(EnumType.STRING)
	public Source getSource(){
		return this.source;
	}
	public void setSource(Source source){
		this.source = source;
	}
	
	@Override
	public void readExternal(ObjectInput oi) throws IOException,
			ClassNotFoundException {
		setUuid((UUID) oi.readObject());
		revision = oi.readLong();
		action = Action.valueOf((String)oi.readObject());
		fileName = (String)oi.readObject();
		tableName = (String)oi.readObject();
		fieldName1 = (String)oi.readObject();
		String obj = (String) oi.readObject();
		if (obj != null){
			key1 = UUID.fromString(obj);
		}
		fieldName2 = (String)oi.readObject();
		obj = (String) oi.readObject();
		if (obj != null){
			key2 = UUID.fromString(obj);
		}
		key2str = (String)oi.readObject();
		caUuid = UUID.fromString((String)oi.readObject());
	}

	@Override
	public void writeExternal(ObjectOutput oo) throws IOException {
		oo.writeObject(getUuid());
		oo.writeLong(revision);
		oo.writeObject(action.name());
		oo.writeObject(fileName);
		oo.writeObject(tableName);
		oo.writeObject(fieldName1);
		if (key1 == null){
			oo.writeObject(null);
		}else{
			oo.writeObject(key1.toString());
		}
		oo.writeObject(fieldName2);

		if (key2 == null){
			oo.writeObject(null);
		}else{
			oo.writeObject(key2.toString());
		}
		oo.writeObject(key2str);
		oo.writeObject(caUuid.toString());	
	}
}

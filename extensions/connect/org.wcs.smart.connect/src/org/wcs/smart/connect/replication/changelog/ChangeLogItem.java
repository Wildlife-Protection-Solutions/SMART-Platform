package org.wcs.smart.connect.replication.changelog;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.UUID;

public class ChangeLogItem implements Externalizable{

	private static final long serialVersionUID = 01L;
	
	public enum Action{
		INSERT,
		UPDATE,
		DELETE,
		FS_INSERT,
		FS_UPDATE,
		FS_DELETE
	}
	private UUID uuid;
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
	
	public UUID getUuid(){
		return this.uuid;
	}
	public void setUuid(UUID uuid){
		this.uuid = uuid;
	}
	
	public Long getRevision(){
		return this.revision;
	}
	
	public void setRevision(long revision){
		this.revision = revision;
	}
	
	public Action getAction(){
		return this.action;
	}
	
	public void setAction(Action action){
		this.action = action;
	}
	
	public String getFileName(){
		return this.fileName;
	}
	public void setFileName(String filename){
		this.fileName = filename;
	}
	
	public String getTableName(){
		return this.tableName;
	}
	public void setTableName(String tablename){
		this.tableName = tablename;
	}
	
	public String getFieldName1(){
		return this.fieldName1;
	}
	public void setFieldName1(String fieldName1){
		this.fieldName1 = fieldName1;
	}
	
	public UUID getKey1(){
		return this.key1;
	}
	public void setKey1(UUID key1){
		this.key1 = key1;
	}
	
	public String getFieldName2(){
		return this.fieldName2;
	}
	public void setFieldName2(String fieldName1){
		this.fieldName2 = fieldName2;
	}
	public UUID getKey2(){
		return this.key2;
	}
	public void setKey2(UUID key1){
		this.key2 = key2;
	}
	public String getKey2String(){
		return this.key2str;
	}
	public void setKey2String(String key2str){
		this.key2str = key2str;
	}
	
	public UUID getConservationArea(){
		return this.caUuid;
	}
	public void setConservationArea(UUID ca){
		this.caUuid = ca;
	}

	@Override
	public void readExternal(ObjectInput oi) throws IOException,
			ClassNotFoundException {
		uuid = (UUID) oi.readObject();
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
		oo.writeObject(uuid);
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

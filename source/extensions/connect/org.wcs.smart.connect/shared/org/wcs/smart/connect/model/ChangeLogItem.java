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
import java.nio.file.Path;
import java.util.UUID;

import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Change log item.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="connect_change_log", schema="smart")
public class ChangeLogItem extends UuidItem implements Externalizable{

	public static final String TABLENAME="smart.connect_change_log"; //$NON-NLS-1$
	
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
	
	
	/**
	 * Determines in the file is a shapefile index files in the map directory
	 * of the SMART filestore.
	 * 
	 * @param p
	 * @return
	 */
	//New to 8.1 we are skipping the recording of all filestore events
	//of qix and fix files in the SMART maps directory 
	//Despite not getting marked as conflicts they still were getting flagged as changes
	//prompting user to require a sync when there were no real changes.
	//As a result of this we also had to update the delete events for shapefiles to ensure
	//qix/fix files are deleted locally if a shapefile is deleted (otherwise they would hang
	//around as they won't get recorded in the changelog).
	//see: ticket #3599
	@Transient
    public static final boolean isIndexShp(Path p) {
    	if (p.toString().endsWith(".qix") || p.toString().endsWith(".fix")){ //$NON-NLS-1$ //$NON-NLS-2$
			if (p.getParent().getFileName().toString().equals("maps")){ //$NON-NLS-1$
				return true;
			}			
		}
    	return false;
    }
	
	/**
	 * Determines in the file is a shapefile in the map directory
	 * of the SMART filestore.
	 * 
	 * @param p
	 * @return
	 */
	@Transient
    public static final boolean isMapDirShapeFile(Path p) {
    	if (p.toString().endsWith(".shp") ){ //$NON-NLS-1$
			if (p.getParent().getFileName().toString().equals("maps")){ //$NON-NLS-1$
				return true;
			}			
		}
    	return false;
    }
}

package org.wcs.smart.i2.query.engine;

import java.util.Date;
import java.util.UUID;

import org.wcs.smart.i2.query.IResultItem;

public class IntelRecordResultItem implements IResultItem {

	private UUID observationUuid;
	private UUID locationUuid;
	private UUID recordUuid;
	
	private String recordStatus;
	private String recordTitle;
	
	private String locationId ;
	private Date locationTime;
	private String locationComment;
	
	private byte[] locationGeometry;
	
	private UUID categoryUuid;
	
	private String[] categoryLabels;
	
	public IntelRecordResultItem(){
		
	}
	
	public UUID getObservationUuid(){
		return this.observationUuid;
	}
	public void setObservationUuid(UUID observationUuid){
		this.observationUuid = this.observationUuid;
	}
	public String getRecordStatus(){
		return this.recordStatus;
	}
	public void setRecordStatus(String status){
		this.recordStatus = status;
	}
	public String getRecordTitle(){
		return this.recordTitle;
	}
	public void setRecordTitle(String title){
		this.recordTitle = title;
	}
	public UUID getRecordUuid(){
		return this.recordUuid;
	}
	public void setRecordUuid(UUID uuid){
		this.recordUuid = uuid;
	}
	
	public UUID getLocationUuid(){
		return this.locationUuid;
	}
	public void setLocationUuid(UUID uuid){
		this.locationUuid = uuid;
	}
	public Date getLocationDate(){
		return this.locationTime;
	}
	public void setLocationDate(Date date){
		this.locationTime = date;
	}
	public String getLocationId(){
		return this.locationId;
	}
	public void setLocationId(String id){
		this.locationId = id;
	}
	public String getLocationComment(){
		return this.locationComment;
	}
	public void setLocationComment(String comment){
		this.locationComment = comment;
	}
	public byte[] getGeometry(){
		return this.locationGeometry;
	}
	public void setGeometry(byte[] geom){
		this.locationGeometry = geom;
	}
	public UUID getCategoryUuid(){
		return this.categoryUuid;
	}
	public void setCategoryUuid(UUID uuid){
		this.categoryUuid = uuid;
	}
	
	public void setCategoryLabels(String[] labels){
		this.categoryLabels = labels;
	}
	public String getCategoryLabel(int level){
		if (level < categoryLabels.length ){
			return categoryLabels[level];
		}
		return "";
	}
	
}

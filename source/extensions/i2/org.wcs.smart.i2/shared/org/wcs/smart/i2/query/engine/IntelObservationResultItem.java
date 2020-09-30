/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.query.engine;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.UUID;

import org.locationtech.jts.geom.Geometry;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.query.IGeometryResultItem;
import org.wcs.smart.i2.query.observation.filter.IQueryFilter;

/**
 * Intelligence observation query results item
 * @author Emily
 *
 */
public class IntelObservationResultItem implements IGeometryResultItem {

	private UUID observationUuid;
	private UUID locationUuid;
	private UUID recordUuid;
	private UUID profileUuid;
	private IntelRecordSource recordSource;
	
	private String recordStatus;
	private String recordTitle;
	
	private String locationId ;
	private LocalDateTime locationTime;
	private String locationComment;
	
	private Geometry locationGeometry;
	private Exception geometryError;
	
	private String profileName;
	
	private UUID categoryUuid;
	
	private String[] categoryLabels;
	private HashMap<String, Object> attributes = new HashMap<String, Object>();
	
	private HashMap<IQueryFilter, Boolean> filterValues = new HashMap<>(); 
	
	private String caId;
	private String caName;
	
	public IntelObservationResultItem(){
		
	}
	
	public void setProfile(UUID profileUuid, String name) {
		this.profileUuid = profileUuid;
		this.profileName = name;
	}
	
	public String getProfileName() {
		return this.profileName;
	}

	public void setConservationAreaId(String caId) {
		this.caId = caId;
	}
	
	public void setConservationAreaName(String name) {
		this.caName = name;
	}
	
	public String getConservationAreaId() {
		return this.caId;
	}
	
	public String getConservationAreaName() {
		return this.caName;
	}
	
	
	public UUID getObservationUuid(){
		return this.observationUuid;
	}
	public void setObservationUuid(UUID observationUuid){
		this.observationUuid = observationUuid;
	}
	
	public void setRecordSource(IntelRecordSource s ) {
		this.recordSource = s;
	}
	
	public IntelRecordSource getRecordSource() {
		return this.recordSource;
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
	public UUID getProfileUuid(){
		return this.profileUuid;
	}
	
	public UUID getLocationUuid(){
		return this.locationUuid;
	}
	public void setLocationUuid(UUID uuid){
		this.locationUuid = uuid;
	}
	public LocalDateTime getLocationDate(){
		return this.locationTime;
	}
	public void setLocationDate(LocalDateTime date){
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
	
	@Override
	public Geometry getGeometry(){
		return this.locationGeometry;
	}
	public Exception getGeometryError(){
		return this.geometryError;
	}
	public void setGeometry(Geometry geom, Exception geometryError ){
		this.locationGeometry = geom;
		this.geometryError = geometryError;
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
		return ""; //$NON-NLS-1$
	}
	
	public void addAttribute(String attributeKey, Object value){
		attributes.put(attributeKey, value);
	}
	
	public Object getAttributeValue(String attributeKey){
		return attributes.get(attributeKey);
	}
	
	public void addFilterValue(IQueryFilter filter, Boolean value){
		filterValues.put(filter, value);
	}
	public HashMap<IQueryFilter, Boolean> getFilterValues(){
		return filterValues;
	}
	
}

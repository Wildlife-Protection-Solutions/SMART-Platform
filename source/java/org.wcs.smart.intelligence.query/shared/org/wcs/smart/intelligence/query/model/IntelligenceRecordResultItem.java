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
package org.wcs.smart.intelligence.query.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.query.common.engine.IGeometryResultItem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;

/**
 * Query result item for storing intelligence record query results.
 * 
 * @author Emily
 *
 */
public class IntelligenceRecordResultItem implements IGeometryResultItem {

	/**
	 * Intelligence point geometry field name
	 */
	public static final String GEOMCOLUMN_KEY = "intel:geometry"; //$NON-NLS-1$
		
	private String conservationAreaName;
	private String conservationAreaId;
	private UUID uuid;
	private String name;
	private Date receivedDate;
	private Date fromDate;
	private Date toDate;
	private String source;
	private UUID sourceUuid;
	private String patrolId;
	private String informantId;
	private String description;
	
	private List<Coordinate> positions = new ArrayList<Coordinate>();
			
	public String getConservationAreaName() {
		return conservationAreaName;
	}
	public void setConservationAreaName(String conservationAreaName) {
		this.conservationAreaName = conservationAreaName;
	}
	public String getConservationAreaId() {
		return conservationAreaId;
	}
	public void setConservationAreaId(String conservationAreaId) {
		this.conservationAreaId = conservationAreaId;
	}
	public UUID getUuid() {
		return uuid;
	}
	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public Date getReceivedDate() {
		return receivedDate;
	}
	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}
	public Date getFromDate() {
		return fromDate;
	}
	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}
	public Date getToDate() {
		return toDate;
	}
	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public UUID getSourceUuid() {
		return sourceUuid;
	}
	public void setSourceUuid(UUID sourceUuid) {
		this.sourceUuid = sourceUuid;
	}
	public String getPatrolId() {
		return patrolId;
	}
	public void setPatrolId(String patrolId) {
		this.patrolId = patrolId;
	}
	public String getInformantId() {
		return informantId;
	}
	public void setInformantId(String informantId) {
		this.informantId = informantId;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public void addCoordinate(double x, double y){
		positions.add(new Coordinate(x,y));
	}
	
	@Override
	public Geometry asGeometry(String columnName) {
		if (columnName.equals(GEOMCOLUMN_KEY)){
			return GeometryFactoryProvider.getFactory().createMultiPoint(positions.toArray(new Coordinate[positions.size()]));
		}
		return null;
	}
	
		
}

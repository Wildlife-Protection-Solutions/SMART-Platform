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
package org.wcs.smart.er.model;

import java.util.Locale;
import java.util.TimeZone;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * Mission track item.  A mission may have more than one tracks.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.mission_track")
public class MissionTrack extends UuidItem{

	public static final int MAX_ID_LENGTH = 128;
	
	public static TimeZone ZTIMEZONE = TimeZone.getTimeZone("GMT"); //$NON-NLS-1$
	
	public enum TrackType {
		// track as associated with su 
		SAMPLING_UNIT,
		// track has not su association
		TRACK;
		
		public String getGuiName(Locale l){
			return SmartContext.INSTANCE.getClass(IErLabelProvider.class).getLabel(this, l);
		}
	};
	
	private byte[] geom;
	private Float distance = null;
	private TrackType type;
	private MissionDay missionDay;
	private LineString ls;
	private String id;
	private SamplingUnit unit;
	
	public MissionTrack(){
		this.type = TrackType.TRACK;
	}
	
	@Column(name="geometry")
	@Lob
	@Basic(fetch = FetchType.LAZY)
	public byte[] getGeom() {
		return geom;
	}
	public void setGeom(byte[] geom) {
		this.geom = geom;
	}
	
	@Transient
	public Float getDistance() throws Exception {
		if (distance == null){
			distance = (float)(GeometryUtils.distanceInMeters(getLineString()) / 1000.0);
		}
		return distance;
	}
	
	@Enumerated(EnumType.STRING)
	@Column(name="track_type")
	public TrackType getType(){
		return this.type;
	}
	
	protected void setType(TrackType type){
		this.type = type;
	}
	
	
	@ManyToOne
	@JoinColumn(name="mission_day_uuid")
	public MissionDay getMissionDay() {
		return missionDay;
	}
	
	public void setMissionDay(MissionDay missionDay) {
		this.missionDay = missionDay;
	}
	
	@ManyToOne
	@JoinColumn(name="sampling_unit_uuid")
	public SamplingUnit getSamplingUnit() {
		return unit;
	}
	
	public void setSamplingUnit(SamplingUnit unit) {
		this.unit = unit;
		if (unit == null){
			setType(TrackType.TRACK);
		}else{
			setType(TrackType.SAMPLING_UNIT);
		}
	}
		
	@Column(name="id")
	public String getId(){
		return this.id;
	}
	
	public void setId(String id){
		this.id = id;
	}
	
	@Transient
	public LineString getLineString() throws Exception{
		if (this.ls == null && geom != null){
			WKBReader reader = new WKBReader();
			this.ls = (LineString)reader.read(geom);
		}
		return this.ls;
	}
	
	/**
	 * Sets the linestring.  Also updates the distance field.  Linestring
	 * must be in EPSG:4326
	 * 
	 * @param ls new linestring
	 */
	public void setLineString(LineString ls){
		if (ls == null){
			this.ls = null;
			this.distance = 0.0f;
			this.geom = null;
			return;
		}
		this.distance = (float)(GeometryUtils.distanceInMeters(ls) / 1000.0);
		
		WKBWriter writer = new WKBWriter(3);
		this.geom = writer.write(ls);
		this.ls = ls;
	}
	
	/**
	 * @return the geometry length only if geometry represents a linestring; otherwise
	 * null is returned
	 * @throws Exception 
	 */
	@Transient
	public Double getGeometryLengthKm() throws Exception{
		return (GeometryUtils.distanceInMeters((LineString)getLineString()) / 1000.0);
	}
}
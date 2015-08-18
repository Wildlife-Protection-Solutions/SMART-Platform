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
package org.wcs.smart.patrol.model;

import java.util.TimeZone;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.util.GeometryUtils;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKBReader;
import com.vividsolutions.jts.io.WKBWriter;

/**
 * Patrol track
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.track")
public class Track extends UuidItem {
	
	public static TimeZone ZTIMEZONE = TimeZone.getTimeZone("GMT"); //$NON-NLS-1$
	
	private byte[] geom;
	private Float distance;
	private PatrolLegDay patrolLegDay;
	private LineString ls;
	
	public Track(){
		
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
	
	@Column(name="distance")
	public Float getDistance() {
		return distance;
	}
	public void setDistance(Float distance) {
		this.distance = distance;
	}
	
	@ManyToOne
	@JoinColumn(name="patrol_leg_day_uuid")
	public PatrolLegDay getPatrolLegDay() {
		return patrolLegDay;
	}
	
	public void setPatrolLegDay(PatrolLegDay patrolLegDay) {
		this.patrolLegDay = patrolLegDay;
	}
	
	@Transient
	public LineString getLineString() throws ParseException{
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
	
}

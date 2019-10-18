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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
import org.wcs.smart.map.GeometryFactoryProvider;
import org.wcs.smart.util.GeometryUtils;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;

/**
 * Patrol track
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.track")
public class Track extends UuidItem {
	
	private static final long serialVersionUID = 1L;
	
	public static TimeZone ZTIMEZONE = TimeZone.getTimeZone("GMT"); //$NON-NLS-1$
	
	private byte[] geom;
	private Float distance;
	private PatrolLegDay patrolLegDay;

	private Geometry geometry;
	private List<LineString> lsList;
	private List<TrackPart> trackParts;
	
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
	public Geometry getGeometry() throws ParseException {
		if (this.geometry == null && geom != null){
			WKBReader reader = new WKBReader();
			geometry = reader.read(geom);
		}
		return geometry;
	}

	@Transient
	public List<TrackPart> getTrackParts() throws ParseException {
		if (trackParts == null) {
			trackParts = new ArrayList<>();
			List<LineString> lsLst = getLineStrings();
			for (int i = 0; i < lsLst.size(); i++) {
				trackParts.add(new TrackPart(this, lsLst.get(i), i));
			}
		}
		return Collections.unmodifiableList(trackParts);
	}
	
	@Transient
	public List<LineString> getLineStrings() throws ParseException {
		if (lsList == null) {
			lsList = new ArrayList<>();
			Geometry gmt = getGeometry();
			if (gmt instanceof MultiLineString) {
				MultiLineString mls = (MultiLineString) gmt;
				for (int i = 0; i < mls.getNumGeometries(); i++) {
					Geometry g = mls.getGeometryN(i);
					if (g instanceof LineString) {
						lsList.add((LineString)g);
					}
				}
			} else if (gmt instanceof LineString) {
				lsList.add((LineString)gmt);
			}
		}
		return Collections.unmodifiableList(lsList);
	}
	
	/**
	 * Sets the linestring array. Also updates the distance field. Linestring must be in EPSG:4326
	 * 
	 * @param lsArray new linestring array
	 */
	@Transient
	public void setLineStrings(List<LineString> ls){
		lsList = null;
		trackParts = null;
		this.distance = 0.0f;
		if (ls == null || ls.isEmpty()) {
			this.geometry = null;
			this.geom = null;
			return;
		}

		for (LineString lineString : ls) {
			this.distance += (float)(GeometryUtils.distanceInMeters(lineString) / 1000.0);
		}
		
		WKBWriter writer = new WKBWriter(3);
		geometry = new MultiLineString(ls.toArray(new LineString[ls.size()]), GeometryFactoryProvider.getFactory());
		this.geom = writer.write(geometry);
	}

	
}

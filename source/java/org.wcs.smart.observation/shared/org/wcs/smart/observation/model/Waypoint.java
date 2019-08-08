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
package org.wcs.smart.observation.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Category;

/**
 * Waypoint object
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.waypoint")
public class Waypoint extends UuidItem {
	
	//earth raidus in meters
	public static final int RADIUS = 6378100;//6371000;//6378.1;

	public static final int COMMENT_MAX_LENGTH = 4096;
	
	private ConservationArea ca;

	private String sourceId;
	
	private int id;
	private Double x;
	private Double y;
	
	@Transient
	private Double prjx;
	@Transient
	private Double prjy;
	
	private Date dateTime;
	
	private Date lastModifiedDate;
	private Employee lastModifiedBy;
	
	private Float direction;
	private Float distance;
	private String comment;
	
	private List<WaypointAttachment> attachments;
	private List<WaypointObservation> observations;
	
	
	public Waypoint(){
		
	}
	
	@Column(name="source")
	public String getSourceId(){
		return this.sourceId;
	}
	public void setSourceId(String sourceId){
		this.sourceId = sourceId;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}

	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	@Column(name="id")
	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@Column(name="x")
	public Double getRawX() {
		return x;
	}

	public void setRawX(double x) {
		this.x = x;
		computePrj();
//		System.out.println("x:" + this.x + " : " + x);
	}

	@Column(name="y")
	public Double getRawY() {
		return y;
	}

	public void setRawY(double y) {
		this.y = y;
		computePrj();
	}

	private void computePrj() {
		
		if (x == null || y == null) return;
		if (direction == null || distance == null) {
			prjx = x;
			prjy = y;
			return;
		}
		
		//TODO: find a value for R
		double a = Math.toRadians(direction);
		double dR = distance/RADIUS;		
		double ry = Math.toRadians(y);
		double rx = Math.toRadians(x);
		double prjy1 = Math.asin( Math.sin(ry)*Math.cos(dR) + Math.cos(ry)*Math.sin(dR)*Math.cos(a) );
		double prjx1 = rx + Math.atan2(Math.sin(a)*Math.sin(dR)*Math.cos(ry), Math.cos(dR)-Math.sin(ry)*Math.sin(prjy1));
		prjx = Math.toDegrees(prjx1);
		prjy = Math.toDegrees(prjy1);
//		System.out.println(x + ", " + y + ":" + prjx + "," + prjy);
		
	}
	
	@Transient
	public Double getX() {
		return prjx;
		
	}
	
	@Transient
	public Double getY() {
		return prjy;
	}
	

	@Column(name="last_modified")
	public Date getLastModified() {
		return lastModifiedDate;
	}

	/**
	 * @param the last modified date time
	 */
	public void setLastModified(Date modifiedDateTime) {
		this.lastModifiedDate = modifiedDateTime;
	}
	
	/**
	 * Get the last modified by. This can be null for waypoints that
	 * have not been modified/added since this was added to the model (version 6.1.0)
	 * 
	 * @return last_modified_by
	 */
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="last_modified_by", referencedColumnName="uuid")
	public Employee getLastModifiedBy() {
		return this.lastModifiedBy;
	}
	
	/**
	 * Set the created_by.
	 * 
	 * @param createdBy
	 *            created_by
	 */
	public void setLastModifiedBy(Employee lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}
	
	@Column(name="datetime")
	public Date getDateTime() {
		return dateTime;
	}

	/**
	 * @param time
	 */
	public void setDateTime(Date dateTime) {
		this.dateTime = dateTime;
	}

	@Column(name="direction")
	public Float getDirection() {
		return direction;
	}

	public void setDirection(Float direction) {
		this.direction = direction;
		computePrj();
	}

	@Column(name="distance")
	public Float getDistance() {
		return distance;
	}

	public void setDistance(Float distance) {
		this.distance = distance;
		computePrj();
	}

	@Column(name="wp_comment")
	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	@OneToMany(fetch = FetchType.EAGER, mappedBy="waypoint", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<WaypointAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<WaypointAttachment> attachments) {
		this.attachments = attachments;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy="waypoint", orphanRemoval=true, cascade={CascadeType.ALL})
	@BatchSize(size=500)
	public List<WaypointObservation> getObservations(){
		return this.observations;
	}
	public void setObservations(List<WaypointObservation> observations){
		this.observations = observations;
	}
	
	/**
	 * Concatenates together all the categories
	 * which make up the observations
	 * at this waypoint. 
	 *  
	 * 
	 * @return <code>null</code> if no observations otherwise
	 * string of category names
	 * 
	 */
	@Transient
	public String getObservationsAsString(){
		if (getObservations() == null || getObservations().size() == 0){
			return null;
		}
		StringBuilder text = new StringBuilder();
		HashMap<Category, Integer> added = new HashMap<Category, Integer>();
		
		for (WaypointObservation ob : getObservations()){
			Integer x = added.get(ob.getCategory());
			if (x == null){
				x = 0;
			}
			added.put(ob.getCategory(), x+1);
			
		}
		for (Entry<Category, Integer> item : added.entrySet()){
			text.append(item.getKey().getName() + " (" + item.getValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			text.append("; "); //$NON-NLS-1$
		}
		text.delete(text.length() - 2, text.length());
		return text.toString();
	}
	
	public Waypoint clone(Session session) {
		
		Waypoint wp = new Waypoint();

		
		wp.setComment(this.comment);
		wp.setDirection(this.direction);
		wp.setDistance(this.distance);
		wp.setId(this.id);
		wp.setDateTime(this.dateTime);		
		wp.setRawX(this.x);
		wp.setRawY(this.y);
		wp.setConservationArea(this.ca);
		wp.setSourceId(this.sourceId);
		
		if (this.observations != null && this.observations.size() > 0){
			wp.setObservations(new ArrayList<WaypointObservation>());
			for (WaypointObservation wobp : this.observations){
				
				WaypointObservation cloned = wobp.clone(session);
				cloned.setUuid(null);
				cloned.setWaypoint(wp);
				wp.getObservations().add(cloned);
			}
		}
		
		if (this.attachments != null) {
			wp.setAttachments(new ArrayList<WaypointAttachment>());
			for (WaypointAttachment sp : this.attachments) {
				WaypointAttachment att = new WaypointAttachment();
				try {
					// copy file to temp location so it won't be deleted out from
					// under us
					File tmpLocation = File.createTempFile(
							"smart_" + System.nanoTime(), ""); //$NON-NLS-1$ //$NON-NLS-2$
					tmpLocation.deleteOnExit();
					sp.computeFileLocation(session);
					FileUtils.copyFile(sp.getAttachmentFile(), tmpLocation);

					att.setCopyFromLocation(tmpLocation);
					att.setFilename(sp.getFilename());
					att.setWaypoint(wp);
					wp.getAttachments().add(att);
				} catch (Exception ex) {
					throw new RuntimeException("Error cloning waypoint.  Waypoint attachments could not be cloned.", ex); //$NON-NLS-1$
				}

			}
		}
		
		return wp;
	}
	

	//https://www.movable-type.co.uk/scripts/latlong.html
	/**
	 * Computes the distance and bearing between two points.
	 * 
	 * @param c1
	 * @param c2
	 * @return array where the first element is the distance in meters and the
	 * second is the bearing in degrees
	 */
	public static Float[] computeDistanceBearing(Coordinate c1, Coordinate c2) {
		//initial bearing
		double y = Math.sin(c2.x-c1.x) * Math.cos(c2.y);
		double x = Math.cos(c1.y)*Math.sin(c2.y) -
		        Math.sin(c1.y)*Math.cos(c2.y)*Math.cos(c2.x-c1.x);
		double brng = Math.toDegrees( Math.atan2(y, x) );
		brng = (brng + 360) % 360;
				
		//Distance haversine formula
		var lat1 = Math.toRadians(c1.y);
		var lat2 = Math.toRadians(c2.y);
		var latdiff = Math.toRadians(c2.y-c1.y);
		var longdiff = Math.toRadians(c2.x-c1.x);
		var a = Math.sin(latdiff/2) * Math.sin(latdiff/2) +
		        Math.cos(lat1) * Math.cos(lat2) *
		        Math.sin(longdiff/2) * Math.sin(longdiff/2);
		var c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
		var d = RADIUS * c;
				
		return new Float[] {(float)d, (float)brng};
	}
}

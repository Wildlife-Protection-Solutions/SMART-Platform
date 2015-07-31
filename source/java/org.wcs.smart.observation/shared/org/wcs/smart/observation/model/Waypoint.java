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
import org.hibernate.annotations.BatchSize;
import org.wcs.smart.ca.ConservationArea;
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
	
	public static final int COMMENT_MAX_LENGTH = 4096;
	
	private ConservationArea ca;

	private String sourceId;
	
	private int id;
	private Double x;
	private Double y;
	private Date dateTime;
	
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
	public Double getX() {
		return x;
	}

	public void setX(double x) {
		this.x = x;
	}

	@Column(name="y")
	public Double getY() {
		return y;
	}

	public void setY(double y) {
		this.y = y;
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
	}

	@Column(name="distance")
	public Float getDistance() {
		return distance;
	}

	public void setDistance(Float distance) {
		this.distance = distance;
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
	
	public Waypoint clone() {
		
		Waypoint wp = new Waypoint();

		
		wp.setComment(this.comment);
		wp.setDirection(this.direction);
		wp.setDistance(this.distance);
		wp.setId(this.id);
		wp.setDateTime(this.dateTime);		
		wp.setX(this.x);
		wp.setY(this.y);
		wp.setConservationArea(this.ca);
		wp.setSourceId(this.sourceId);
		
		if (this.observations != null && this.observations.size() > 0){
			wp.setObservations(new ArrayList<WaypointObservation>());
			for (WaypointObservation wobp : this.observations){
				
				WaypointObservation cloned = wobp.clone();
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
					FileUtils.copyFile(sp.getFullFile(), tmpLocation);

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
	

}

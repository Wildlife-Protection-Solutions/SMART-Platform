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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.hibernate.Session;
import org.hibernate.annotations.BatchSize;
import org.locationtech.jts.geom.Coordinate;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.util.GeometryUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Waypoint object
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="waypoint", schema="smart")
public class Waypoint extends UuidItem {
	
	private static final long serialVersionUID = 1L;

	public static final int COMMENT_MAX_LENGTH = 4096;
	public static final int ID_MAX_LENGTH = 256;
	
	private ConservationArea ca;

	private String sourceId;
	
	private String id;
	private Double x;
	private Double y;
	
	@Transient
	private Double prjx;
	@Transient
	private Double prjy;
	
	private LocalDateTime dateTime;
	
	private LocalDateTime lastModifiedDate;
	private Employee lastModifiedBy;
	
	private Float direction;
	private Float distance;
	private String comment;
	
	private List<WaypointAttachment> attachments;
	private List<WaypointObservationGroup> groups;
	
	//source configurable model used to collect data (as of 757)
	private ConfigurableModel sourceCm;

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
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="source_cm_uuid ", referencedColumnName="uuid")
	public ConfigurableModel getSourceConfigurableModel() {
		return sourceCm;
	}

	public void setSourceConfigurableModel(ConfigurableModel sourceCm) {
		this.sourceCm = sourceCm;
	}
	
	
	@Column(name="id")
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	@Column(name="x")
	public Double getRawX() {
		return x;
	}

	public void setRawX(double x) {
		this.x = x;
		computePrj();
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
		
		Coordinate c = projectPoint(new Coordinate(x, y), distance, direction);
		prjx = c.x;
		prjy = c.y;
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
	public LocalDateTime getLastModified() {
		return lastModifiedDate;
	}

	/**
	 * @param the last modified date time
	 */
	public void setLastModified(LocalDateTime modifiedDateTime) {
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
	public LocalDateTime getDateTime() {
		return dateTime;
	}

	/**
	 * @param time
	 */
	public void setDateTime(LocalDateTime dateTime) {
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
	public List<WaypointObservationGroup> getObservationGroups(){
		return this.groups;
	}
	public void setObservationGroups(List<WaypointObservationGroup> groups){
		this.groups = groups;
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
		if (getObservationGroups() == null || getObservationGroups().size() == 0){
			return null;
		}
		StringBuilder text = new StringBuilder();
		HashMap<Category, Integer> added = new HashMap<Category, Integer>();
		
		for (WaypointObservationGroup grp : getObservationGroups()) {
			for (WaypointObservation ob : grp.getObservations()){
				Integer x = added.get(ob.getCategory());
				if (x == null){
					x = 0;
				}
				added.put(ob.getCategory(), x+1);
			}
		}
		for (Entry<Category, Integer> item : added.entrySet()){
			text.append(item.getKey().getName() + " (" + item.getValue() + ")"); //$NON-NLS-1$ //$NON-NLS-2$
			text.append("; "); //$NON-NLS-1$
		}
		if (text.length() > 1) text.delete(text.length() - 2, text.length());
		
		return text.toString();
	}
	
	/**
	 * 
	 * @return all waypoints associated with any group.  Will
	 * never return null.
	 */
	@Transient
	public List<WaypointObservation> getAllObservations(){
		List<WaypointObservation> all = new ArrayList<>();
		if (getObservationGroups() == null) return all;
		for (WaypointObservationGroup g : getObservationGroups()) {
			for (WaypointObservation o : g.getObservations()) all.add(o);
		}
		return all;
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
		wp.setSourceConfigurableModel(this.getSourceConfigurableModel());
		wp.setObservationGroups(new ArrayList<WaypointObservationGroup>());
		
		if (this.groups != null && !this.groups.isEmpty()) {
			for (WaypointObservationGroup g : this.groups) {
				WaypointObservationGroup groupclone = new WaypointObservationGroup();
				groupclone.setWaypoint(wp);
				wp.getObservationGroups().add(groupclone);
				groupclone.setObservations(new ArrayList<>());
				for (WaypointObservation wobp : g.getObservations()){
					WaypointObservation cloned = wobp.clone(session);
					cloned.setUuid(null);
					cloned.setObservationGroup(groupclone);
					groupclone.getObservations().add(cloned);
				}
			}
		}
		
		if (this.attachments != null) {
			wp.setAttachments(new ArrayList<WaypointAttachment>());
			for (WaypointAttachment sp : this.attachments) {
				WaypointAttachment att = new WaypointAttachment();
				try {
					// copy file to temp location so it won't be deleted out from
					// under us
					Path tmpLocation = Files.createTempFile(
							"smart_" + System.nanoTime(), ""); //$NON-NLS-1$ //$NON-NLS-2$
					tmpLocation.toAbsolutePath().toFile().deleteOnExit();
					sp.computeFileLocation(session);
					
					Files.copy(sp.getAttachmentFile(), tmpLocation, StandardCopyOption.REPLACE_EXISTING);

					att.setCopyFromLocation(tmpLocation);
					att.setFilename(sp.getFilename());
					att.setSignatureType(sp.getSignatureType());
					att.setWaypoint(wp);
					wp.getAttachments().add(att);
				} catch (Exception ex) {
					throw new RuntimeException("Error cloning waypoint.  Waypoint attachments could not be cloned.", ex); //$NON-NLS-1$
				}

			}
		}
		return wp;
	}
	
	public static Coordinate projectPoint(Coordinate c, double distance, double direction) {
		return GeometryUtils.projectPoint(c, distance, direction);
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
		return GeometryUtils.computeDistanceBearing(c1, c2);
	}
	
	@Transient
	public void saveNewAttachments(Session session) {
		if (getAttachments() != null) {
			getAttachments().forEach(wa->{
				if (wa.getUuid() == null) session.persist(wa);
			});
		}
		for (WaypointObservation wo : getAllObservations()) {
			if (wo.getAttachments() == null) continue;
			wo.getAttachments().forEach(a->{
				if (a.getUuid() == null) session.persist(wo);
			});
		}
	}
}

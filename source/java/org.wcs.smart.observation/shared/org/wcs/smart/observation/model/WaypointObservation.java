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
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.io.FileUtils;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;

/**
 * Waypoint observation
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.wp_observation")
public class WaypointObservation extends UuidItem {
	
	private Waypoint waypoint = null;
	private Category category = null;
	
	private List<WaypointObservationAttribute> attributes = null; 
	private List<ObservationAttachment> attachments;
	
	private Employee observer;
	
	public WaypointObservation(){
		
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="wp_uuid", referencedColumnName="uuid")
	public Waypoint getWaypoint(){
		return this.waypoint;
	}
	public void setWaypoint(Waypoint waypoint){
		this.waypoint = waypoint;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="employee_uuid", referencedColumnName="uuid")
	public Employee getObserver(){
		return this.observer;
	}
	public void setObserver(Employee observer){
		this.observer = observer;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="category_uuid", referencedColumnName="uuid")
	public Category getCategory(){
		return this.category;
	}
	public void setCategory(Category category){
		this.category = category;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="id.observation", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<WaypointObservationAttribute> getAttributes(){
		return this.attributes;
	}
	public void setAttributes(List<WaypointObservationAttribute> attributes){
		this.attributes = attributes;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy="observation", orphanRemoval=false, cascade={CascadeType.ALL})
	public List<ObservationAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<ObservationAttachment> attachments) {
		this.attachments = attachments;
	}
	/**
	 * Finds the observation attribute for a given attribute.
	 * 
	 * @param attribute
	 * @return observation attribute or null if not found
	 */
	@Transient
	public WaypointObservationAttribute findAttribute(Attribute attribute){
		if (getAttributes() ==  null){
			return null;
		}
		for (WaypointObservationAttribute att: getAttributes()){
			if (att.getAttribute().equals(attribute)){
				return att;
			}
		}
		return null;
	}
	
	/**
	 * Clones the waypoint observation object.  Does not
	 * clone the uuid or the waypoint.
	 */
	public WaypointObservation clone(Session session){
		WaypointObservation clone = new WaypointObservation();
		
		clone.setCategory(getCategory());
		clone.setAttributes(new ArrayList<WaypointObservationAttribute>());
		if (attributes != null) {
			for (Iterator<WaypointObservationAttribute> iterator = attributes.iterator(); iterator.hasNext();) {
				WaypointObservationAttribute type = (WaypointObservationAttribute) iterator.next();
				WaypointObservationAttribute ctype = type.clone();
				ctype.setObservation(clone);
				clone.getAttributes().add(ctype);
			}
		}
		
		//clone attachments
		if (this.attachments != null) {
			clone.setAttachments(new ArrayList<ObservationAttachment>());
			for (ObservationAttachment sp : this.attachments) {
				ObservationAttachment att = new ObservationAttachment();
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
					att.setObservation(clone);
					clone.getAttachments().add(att);
				} catch (Exception ex) {
					throw new RuntimeException("Error cloning waypoint.  Waypoint attachments could not be cloned.", ex); //$NON-NLS-1$
				}

			}
		}
		return clone;
		
	}
}

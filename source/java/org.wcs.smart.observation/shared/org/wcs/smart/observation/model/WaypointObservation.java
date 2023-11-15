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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Category;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Waypoint observation
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="wp_observation", schema="smart")
public class WaypointObservation extends UuidItem {
	
	private static final long serialVersionUID = 1L;
	
	private WaypointObservationGroup obsgroup = null;
	private Category category = null;
	
	private List<WaypointObservationAttribute> attributes = null; 
	private List<ObservationAttachment> attachments;
	
	private Employee observer;
	
	public WaypointObservation(){
		
	}
	
	@Transient
	public Waypoint getWaypoint() {
		return this.getObservationGroup().getWaypoint();
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="wp_group_uuid", referencedColumnName="uuid")
	public WaypointObservationGroup getObservationGroup(){
		return this.obsgroup;
	}
	public void setObservationGroup(WaypointObservationGroup obsgroup){
		this.obsgroup = obsgroup;
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
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="observation", orphanRemoval = true, cascade={CascadeType.ALL})
	public List<WaypointObservationAttribute> getAttributes(){
		return this.attributes;
	}
	
	/**
	 * Gets attribute observation sorted by attribute data model order. 
	 * Object must be attached to valid session when calling. Returned array
	 * is not associated with the session.
	 * 
	 * @return
	 */
	@Transient
	public List<WaypointObservationAttribute> getAttributesSorted(){
		List<Attribute> order = new ArrayList<>();
		getCategory().getAllAttribute(order, null);
		List<WaypointObservationAttribute> sorted = new ArrayList<>(getAttributes());
		sorted.sort((a, b)-> Integer.compare(order.indexOf(a.getAttribute()), order.indexOf(b.getAttribute())) );
		return sorted;
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
					Path tmpLocation = Files.createTempFile(
							"smart_" + System.nanoTime(), ""); //$NON-NLS-1$ //$NON-NLS-2$
					tmpLocation.toAbsolutePath().toFile().deleteOnExit();
					sp.computeFileLocation(session);
					Files.copy(sp.getAttachmentFile(), tmpLocation, StandardCopyOption.REPLACE_EXISTING);
					
					att.setCopyFromLocation(tmpLocation);
					att.setFilename(sp.getFilename());
					att.setObservation(clone);
					att.setSignatureType(sp.getSignatureType());
					clone.getAttachments().add(att);
				} catch (Exception ex) {
					throw new RuntimeException("Error cloning waypoint.  Waypoint attachments could not be cloned.", ex); //$NON-NLS-1$
				}

			}
		}
		return clone;
		
	}
}

/*
 * Copyright (C) 2021 Wildlife Conservation Society
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

import org.wcs.smart.ca.AttachmentTag;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.common.attachment.ISmartAttachment;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Links tag to an attachment (either waypoint or observation)
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="attachment_tag_link", schema="smart")
public class AttachmentTagLink extends UuidItem{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private AttachmentTag tag;
	private WaypointAttachment wpAttachment;
	private ObservationAttachment obsAttachment;
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="tag_uuid", referencedColumnName="uuid")
	public AttachmentTag getTag() {
		return this.tag;
	}
	
	public void setTag(AttachmentTag tag){
		this.tag = tag;
	}

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="wp_attachment_uuid", referencedColumnName="uuid")
	public WaypointAttachment getWaypointAttachment() {
		return wpAttachment;
	}
	
	public void setWaypointAttachment(WaypointAttachment wpAttachment){
		this.wpAttachment = wpAttachment;
	}
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="obs_attachment_uuid", referencedColumnName="uuid")
	public ObservationAttachment getObservationAttachment() {
		return obsAttachment;
	}
	
	public void setObservationAttachment(ObservationAttachment obsAttachment){
		this.obsAttachment = obsAttachment;
	}
	
	@Transient
	public ISmartAttachment getAttachment() {
		if (getWaypointAttachment() != null) return getWaypointAttachment();
		return getObservationAttachment();
	}
}

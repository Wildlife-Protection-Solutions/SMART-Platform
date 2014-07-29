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

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;


/**
 * Link between a sampling group and it's children.  The children
 * may be other groups or units.  Users are required to ensure
 * no cycles are created.
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.sampling_group_item")
public class SamplingGroupItem extends UuidItem {
	
	private SamplingGroup parent;
	private SamplingGroup childGroup;
	private SamplingUnit childUnit;
	private MissionTrack childTrack;
	
	public SamplingGroupItem (){
	}


	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="parent_group_uuid", referencedColumnName="uuid")
	public SamplingGroup getParent() {
		return parent;
	}

	public void setParent(SamplingGroup parent) {
		this.parent = parent;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="group_uuid", referencedColumnName="uuid")
	public SamplingGroup getChildGroup() {
		return childGroup;
	}

	public void setChildGroup(SamplingGroup childGroup) {
		this.childGroup = childGroup;
	}

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="unit_uuid", referencedColumnName="uuid")
	public SamplingUnit getChildUnit() {
		return childUnit;
	}

	public void setChildUnit(SamplingUnit childUnit) {
		this.childUnit = childUnit;
	}


	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="track_uuid", referencedColumnName="uuid")
	public MissionTrack getTrack() {
		return childTrack;
	}

	public void setTrack(MissionTrack childTrack) {
		this.childTrack = childTrack;
	}

}

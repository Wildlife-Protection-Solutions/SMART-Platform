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
package org.wcs.smart.intelligence.model;

import java.io.File;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.GenericGenerator;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.util.SmartUtils;

/**
 * @author elitvin
 *
 */
@Entity
@Table(name = "smart.intelligence")
public class Intelligence {

	private byte[] uuid;
    private ConservationArea conservationArea;
    private Date receivedDate;
    private IntelligenceSourceType source;
    private Patrol patrol;
    private Date fromDate;
    private Date toDate;
    private String shortName;
    private String description;
	private List<IntelligencePoint> points;
	private List<IntelligenceAttachment> attachments;
   
 
	@Id
	@GeneratedValue(generator="uuid")
	@GenericGenerator(name= "uuid", strategy="uuid2")
    public byte[] getUuid() {
		return uuid;
	}

	public void setUuid(byte[] uuid) {
		this.uuid = uuid;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
        return conservationArea;
    }

    public void setConservationArea(ConservationArea conservationArea) {
        this.conservationArea = conservationArea;
    }

	@Column(name="received_date")
   public Date getReceivedDate() {
        return receivedDate;
    }

    public void setReceivedDate(Date receivedDate) {
        this.receivedDate = receivedDate;
    }

	@Column(name="source")
	@Enumerated(EnumType.STRING)
	public IntelligenceSourceType getSource() {
		return source;
	}

	public void setSource(IntelligenceSourceType source) {
		this.source = source;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="patrol_uuid", referencedColumnName="uuid")
	public Patrol getPatrol() {
		return patrol;
	}

	public void setPatrol(Patrol patrol) {
		this.patrol = patrol;
	}

	@Column(name="from_date")
	public Date getFromDate() {
		return fromDate;
	}

	public void setFromDate(Date fromDate) {
		this.fromDate = fromDate;
	}

	@Column(name="to_date")
	public Date getToDate() {
		return toDate;
	}

	public void setToDate(Date toDate) {
		this.toDate = toDate;
	}

	@Column(name="short_name")
	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

	@Column(name="description")
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy="intelligence", orphanRemoval=true, cascade={CascadeType.ALL})
//	@BatchSize(size=200)
	public List<IntelligencePoint> getPoints() {
		return points;
	}

	public void setPoints(List<IntelligencePoint> points) {
		this.points = points;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="intelligence", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<IntelligenceAttachment> getAttachments() {
		return attachments;
	}

	public void setAttachments(List<IntelligenceAttachment> attachments) {
		this.attachments = attachments;
	}

	@Override
	public int hashCode() {
		if (uuid != null) {
			return Arrays.hashCode(uuid);
		}
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof Intelligence) {
			Intelligence i = (Intelligence)other;
			if (i.getUuid() == null && this.getUuid() == null) {
				return super.equals(i);
			}else if (i.getUuid() != null && this.getUuid() != null) {
				return Arrays.equals(i.getUuid(), this.getUuid());
			}
		}
		return false;
	}
	
	/**
	 * 
	 * <p>
	 * To get full file names you must prepend this with the conservation area file store location.
	 * </p>
	 * <code>
	 * ConservationArea.getFileDataStoreLocation() + File.separator + Intelligence.getPatrolDatastorePath();
	 * </code>
	 * @return the file store location for the intelligence relative to the conservation area file store
	 */
	@Transient
	public String getIntelligenceDatastorePath(){
		return "intelligence" + File.separator + SmartUtils.getDirectoryPath(uuid); //$NON-NLS-1$
	}
	

}

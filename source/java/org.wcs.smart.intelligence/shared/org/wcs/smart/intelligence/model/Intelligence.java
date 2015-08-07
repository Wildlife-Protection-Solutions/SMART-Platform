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
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.util.UuidUtils;

/**
 * @author elitvin
 *
 */
@Entity
@Table(name = "smart.intelligence")
public class Intelligence extends NamedItem {

	public static final String  INTELLIGENCE_DIR = "intelligence"; //$NON-NLS-1$
	
	
	/**
	 * Maximum length of description field
	 */
	public static final int MAX_DESCRIPTION_LENTH = 32700;
				
    private ConservationArea conservationArea;
    private Date receivedDate;
    private IntelligenceSource source;
    private Patrol patrol;
    private Informant informant;
    private Date fromDate;
    private Date toDate;
    private String description;
	private List<IntelligencePoint> points;
	private List<IntelligenceAttachment> attachments;
	private Employee creator;

   
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

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="source_uuid", referencedColumnName="uuid")
	public IntelligenceSource getSource() {
		return source;
	}

	public void setSource(IntelligenceSource source) {
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="informant_uuid", referencedColumnName="uuid")
	public Informant getInformant() {
		return informant;
	}
	
	public void setInformant(Informant informant) {
		this.informant = informant;
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
		if (points == null) {
			points = new ArrayList<IntelligencePoint>();
		}
		return points;
	}

	public void setPoints(List<IntelligencePoint> points) {
		this.points = points;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="intelligence", orphanRemoval=true, cascade={CascadeType.ALL})
	public List<IntelligenceAttachment> getAttachments() {
		if (attachments == null) {
			attachments = new ArrayList<IntelligenceAttachment>();
		}
		return attachments;
	}

	public void setAttachments(List<IntelligenceAttachment> attachments) {
		this.attachments = attachments;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="creator_uuid", referencedColumnName="uuid")
	public Employee getCreator(){
		return creator;
	}
	
	public void setCreator(Employee creator){
		this.creator = creator;
	}

	
	@Transient
	public static String generateLabel(String name, Date receivedDate) {
		return name + "  [" + DateFormat.getDateInstance(DateFormat.SHORT).format(receivedDate) + "]"; //$NON-NLS-1$ //$NON-NLS-2$
	}
	
	@Transient
	public String getDatastoreLocation() {
		return  getConservationArea().getFileDataStoreLocation()
				+ File.separator
				+ INTELLIGENCE_DIR
				+ File.separator
				+ UuidUtils.getDirectoryPath(getUuid());
	}
}

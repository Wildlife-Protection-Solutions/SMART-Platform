/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKBReader;
import org.locationtech.jts.io.WKBWriter;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.qa.ILabelProvider;
import org.wcs.smart.qa.ILabelProvider.Key;
import org.wcs.smart.qa.RoutineExtensionManager;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * A QA error item. 
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="qa_error", schema="smart")
public class QaError extends UuidItem{

	private static final long serialVersionUID = 1L;
	
	public enum Status{
		NEW,
		IGNORED,
		DELETED,
		ERROR,
		FIXED,
		UNKNOWN;
		
		public boolean isNew() {
			return this == NEW || this == UNKNOWN;
		}
		
		public String getGuiName(Locale l){
			switch(this){
			case DELETED:
				return ILabelProvider.getLabel(Key.QaError_Status_Deleted, l);
			case ERROR:
				return ILabelProvider.getLabel(Key.QaError_Status_Error, l);
			case FIXED:
				return ILabelProvider.getLabel(Key.QaError_Status_Fixed, l);
			case IGNORED:
				return ILabelProvider.getLabel(Key.QaError_Status_Ignored, l);
			case NEW:
				return ILabelProvider.getLabel(Key.QaError_Status_New, l);
			case UNKNOWN:
				return ILabelProvider.getLabel(Key.QaError_Status_Unknown, l);
			}
			return this.name();
		}
	}
	
	private Status status;
	private LocalDateTime validateDate;
	private String errorId;
	private String errorDescription;
	private String fixMessage;
	private UUID srcId;
	private QaRoutine routine;
	private String dataProviderId;
	private byte[] bytegeom; 	 
	private ConservationArea conservationArea;
	
	@Transient
	private IQaDataProvider dataProvider;
	private Geometry geometry;
	private Set<QaError> links;
	
	public QaError() {
	}

	
	/**
	 * Get the conservation area.
	 * 
	 * @return conservation area
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.conservationArea;
	}
	
	/**
	 * Set the conservation area.
	 * 
	 * @param conservationArea
	 */
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}
	
	/**
	 * Get the status.
	 * 
	 * @return status
	 */
	@Column(name="status")
	@Enumerated(value = EnumType.STRING)
	public Status getStatus() {
		return this.status;
	}
	
	/**
	 * Set the status.
	 * 
	 * @param status
	 */
	public void setStatus(Status status) {
		this.status = status;
	}

	
	/**
	 * Get the validate_date.
	 * 
	 * @return validate_date
	 */
	@Column(name="validate_date")
	public LocalDateTime getValidateDate() {
		return this.validateDate;
	}
	
	/**
	 * Set the date validation ran.
	 * 
	 * @param validateDate
	 */
	
	public void setValidateDate(LocalDateTime validateDate) {
		this.validateDate = validateDate;
	}



	/**
	 * Get the error identifier.  Defined
	 * by the qa routine.
	 * 
	 * @return error_id
	 */
	@Column(name="error_id")
	public String getErrorId() {
		return this.errorId;
	}
	
	/**
	 * Set the error identifier.  Generally the 
	 * identifier of the feature in error, however each
	 * routine can provide details
	 * 
	 * @param errorId
	 *            error_id
	 */
	public void setErrorId(String errorId) {
		this.errorId = errorId;
	}

	

	/**
	 * Get the error description. A more
	 * detailed version of the error provided
	 * by the qa routine.
	 * 
	 * @return error_description
	 */
	@Column(name="error_description")
	public String getErrorDescription() {
		return this.errorDescription;
	}
	/**
	 * Set the error description
	 * 
	 * @param errorDescription
	 *            error_description
	 */
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}


	/**
	 * Get the identifier of the source feature
	 * creating the error 
	 * 
	 * @return src_identifier
	 */
	@Column(name="src_identifier")
	public UUID getSourceId() {
		return this.srcId;
	}
	/**
	 * Set the src_identifier.
	 * 
	 * @param srcId
	 */
	
	public void setSourceId(UUID srcId) {
		this.srcId = srcId;
	}
	
	/**
	 * Get the qa routine.
	 * 
	 * @return status
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="qa_routine_uuid", referencedColumnName="uuid")
	public QaRoutine getQaRoutine() {
		return this.routine;
	}
	
	/**
	 * Set the qa routine.
	 * 
	 * @param routine
	 */
	public void setQaRoutine(QaRoutine routine) {
		this.routine = routine;
	}

	/**
	 * Get the data provider identifier
	 * 
	 * @return status
	 */
	@Column(name="data_provider_id")
	public String getDataProviderId() {
		return this.dataProviderId;
	}
	
	/**
	 * Set the data provider that provided the data
	 * 
	 * @param routine
	 */
	public void setDataProviderId(String dataProviderId) {
		this.dataProviderId = dataProviderId;
	}
	
	
	/**
	 * The location of the feature in error; can be null if no
	 * location
	 * @param geom
	 */
	@Column(name="geometry")
	@Lob
	public byte[] getGeometry(){
		return this.bytegeom;
	}
	
	
	public void setGeometry(byte[] geom){
		this.bytegeom = geom;
	}
	
	/**
	 * Details about the fix applied to the qa error
	 * @return
	 */
	@Column(name="fix_message")
	public String getFixMessage(){
		return this.fixMessage;
	}
	
	public void setFixMessage(String message){
		this.fixMessage = message;
	}
	
	
	/**
	 * Get the data provider identifier
	 * 
	 * @return status
	 */
	@Transient
	public IQaDataProvider getDataProvider() {
		for (IQaDataProvider p : RoutineExtensionManager.INSTANCE.getDataProviders()){
			if (p.getId().equals(dataProviderId)) return p;
		}
		return null;
	}
	
	
	/**
	 * All geometries should be stored in lat/long coordinates
	 * @param g
	 */
	@Transient
	public void setGeometryObject(Geometry g){
		if (g == null){
			bytegeom = null;
		}else{
			WKBWriter writer = new WKBWriter();
			this.bytegeom = writer.write(g);
		}
		this.geometry = g;
	}
	
	/**
	 * Gets a geometry associated with the error item
	 * 
	 * @return
	 */
	@Transient
	public Geometry getGeometryObject() {
		if (geometry == null && getGeometry() != null){
			WKBReader reader = new WKBReader();
			try {
				this.geometry = reader.read(getGeometry());
			} catch (ParseException e) {
				Logger.getLogger(QaError.class.getName()).log(Level.WARNING, e.getMessage(), e);
			}
		}
		return geometry;
	}
	
	
	/**
	 * Links another QA error with this one.  Links
	 * are QA errors generated on the same data item by
	 * different routines. 
	 * @param g
	 */
	@Transient
	public void addLink(QaError error){
		getLinks().add(error);
	}
	
	/**
	 * Links
	 * are QA errors generated on the same data item by
	 * different routines. 
	 * 
	 * @return
	 */
	@Transient
	public Set<QaError> getLinks() {
		if (links == null){
			links = new HashSet<>();
		}
		return links;
	}
}

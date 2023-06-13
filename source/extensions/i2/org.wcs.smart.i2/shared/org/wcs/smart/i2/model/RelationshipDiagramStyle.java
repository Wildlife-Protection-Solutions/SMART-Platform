/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.model;

import java.util.HashMap;
import java.util.Map;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKey;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Style representation for relationship diagram.
 *  
 * @author elitvin
 * @since 6.0.0
 */
@Entity
@Table(name = "i_diagram_style", schema="smart")
public class RelationshipDiagramStyle extends NamedItem {
	
	private static final long serialVersionUID = 1L;
	
	private ConservationArea conservationArea;
	private boolean isDefault = false;
	private String options;
	private Map<IntelEntityType, RelationshipDiagramEntityTypeStyle> entityTypeStyles;
	private Map<IntelRelationshipType, RelationshipDiagramRelationshipTypeStyle> relationshipTypeStyles;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return this.conservationArea;
	}
	public void setConservationArea(ConservationArea ca) {
		this.conservationArea = ca;
	}
	
	@Column(name = "is_default")
	public boolean isDefault() {
		return isDefault;
	}
	public void setDefault(Boolean isDefault) {
		this.isDefault = Boolean.TRUE.equals(isDefault); //null <==> false
	}

	//note: using getOptions and setOptions here causes hibernate failure
	@Column(name="options")
	public String getOptionValues() {
		return options;
	}
	public void setOptionValues(String options) {
		this.options = options;
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="style", cascade = {CascadeType.ALL}, orphanRemoval = true)
	@MapKey(name = "entityType")
	public Map<IntelEntityType, RelationshipDiagramEntityTypeStyle> getEntityTypeStyles() {
		if (entityTypeStyles == null)
			entityTypeStyles = new HashMap<>();
		return entityTypeStyles;
	}
	public void setEntityTypeStyles(Map<IntelEntityType, RelationshipDiagramEntityTypeStyle> entityTypeStyles) {
		this.entityTypeStyles = entityTypeStyles;
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="style", cascade = {CascadeType.ALL}, orphanRemoval = true)
	@MapKey(name = "relationshipType")
	public Map<IntelRelationshipType, RelationshipDiagramRelationshipTypeStyle> getRelationshipTypeStyles() {
		if (relationshipTypeStyles == null)
			relationshipTypeStyles = new HashMap<>();
		return relationshipTypeStyles;
	}
	public void setRelationshipTypeStyles(Map<IntelRelationshipType, RelationshipDiagramRelationshipTypeStyle> relationshipTypeStyles) {
		this.relationshipTypeStyles = relationshipTypeStyles;
	}
	
	@Transient
	public RelationshipDiagramStyleOptions getStyleOptions() {
		return new RelationshipDiagramStyleOptions(getOptionValues());
	}
	public void setStyleOptions(RelationshipDiagramStyleOptions styleOptions) {
		setOptionValues(styleOptions.getJson().toString());
	}
	
}

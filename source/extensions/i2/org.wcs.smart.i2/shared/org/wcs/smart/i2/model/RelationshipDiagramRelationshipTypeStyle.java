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

import org.wcs.smart.ca.UuidItem;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Specific styles configuration related to {@link IntelRelationshipType}
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
@Entity
@Table(name = "i_diagram_relationship_type_style", schema="smart")
public class RelationshipDiagramRelationshipTypeStyle extends UuidItem {

	private static final long serialVersionUID = 1L;
	
	private RelationshipDiagramStyle style;
	private IntelRelationshipType relationshipType;
	private String options;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="style_uuid", referencedColumnName="uuid")
	public RelationshipDiagramStyle getStyle() {
		return style;
	}
	public void setStyle(RelationshipDiagramStyle style) {
		this.style = style;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="relationship_type_uuid", referencedColumnName="uuid")
	public IntelRelationshipType getRelationshipType() {
		return relationshipType;
	}
	public void setRelationshipType(IntelRelationshipType relationshipType) {
		this.relationshipType = relationshipType;
	}
	
	//note: using getOptions and setOptions here causes hibernate failure
	@Column(name="options")
	public String getOptionValues() {
		return options;
	}
	public void setOptionValues(String options) {
		this.options = options;
	}

	@Transient
	public RelationshipDiagramEdgeStyleOptions getStyleOptions() {
		return getOptionValues() != null ? new RelationshipDiagramEdgeStyleOptions(getOptionValues()) : null;
	}
	public void setStyleOptions(RelationshipDiagramEdgeStyleOptions styleOptions) {
		if (styleOptions != null) {
			setOptionValues(styleOptions.getJson().toString());
		} else {
			setOptionValues(null);
		}
	}
	
}

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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.UuidItem;

/**
 * Specific styles configuration related to {@link IntelEntityType}
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
@Entity
@Table(name = "smart.i_diagram_entity_type_style")
public class RelationshipDiagramEntityTypeStyle extends UuidItem {
	
	private static final long serialVersionUID = 1L;
	
	private RelationshipDiagramStyle style;
	private IntelEntityType entityType;
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
	@JoinColumn(name="entity_type_uuid", referencedColumnName="uuid")
	public IntelEntityType getEntityType() {
		return entityType;
	}
	public void setEntityType(IntelEntityType entityType) {
		this.entityType = entityType;
	}
	
	@Column(name="options")
	public String getOptions() {
		return options;
	}
	public void setOptions(String options) {
		this.options = options;
	}

	@Transient
	public RelationshipDiagramNodeStyleOptions getStyleOptions() {
		return getOptions() != null ? new RelationshipDiagramNodeStyleOptions(getOptions()) : null;
	}
	public void setStyleOptions(RelationshipDiagramNodeStyleOptions styleOptions) {
		if (styleOptions != null) {
			setOptions(styleOptions.getJson().toString());
		} else {
			setOptions(null);
		}
	}

}

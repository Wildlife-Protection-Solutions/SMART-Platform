/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.paws.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;

@Entity
@Table(name="smart.paws_simple_classification")
public class PawsSimpleClass extends UuidItem{

	private PawsConfiguration config;
	
	private String daterange;
	private String classification;
	
	private Category category;
	private Attribute attribute;
	private AttributeListItem list;
	private AttributeTreeNode node;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="config_uuid", referencedColumnName="uuid")
	public PawsConfiguration getConfiguration() {
		return this.config;
	}
	
	public void setConfiguration(PawsConfiguration config) {
		this.config = config;
	}
	
	@Column(name="classification")
	public String getClassification() {
		return this.classification;
	}
	
	public void setClassification(String classification) {
		this.classification = classification;
	}
	
	@Column(name="date_range")
	public String getDateRange() {
		return this.daterange;
	}
	
	public void setDateRange(String daterange) {
		this.daterange = daterange;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="category_uuid", referencedColumnName="uuid")
	public Category getCategory() {
		return this.category;
	}
	
	public void setCategory(Category category) {
		this.category = category;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="attribute_uuid", referencedColumnName="uuid")
	public Attribute getAttribute() {
		return this.attribute;
	}
	
	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="list_uuid", referencedColumnName="uuid")
	public AttributeListItem getAttributeListItem() {
		return this.list;
	}
	
	public void setAttributeListItem(AttributeListItem list) {
		this.list = list;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="tree_uuid", referencedColumnName="uuid")
	public AttributeTreeNode getAttributeTreeNode() {
		return this.node;
	}
	
	public void setAttributeTreeNode(AttributeTreeNode node) {
		this.node = node;
	}
}

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
package org.wcs.smart.entity.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;

@javax.persistence.Entity
@Table(name="smart.entity_attribute_value")
public class EntityAttributeValue {
	
	private EntityAttributeValuePk id = new EntityAttributeValuePk();
	
	
	private String stringValue;
	private Double doubleValue;
	private AttributeListItem listItem;
	private AttributeTreeNode treeNode;

	public EntityAttributeValue(){
		
	}
	
	@EmbeddedId
	public EntityAttributeValuePk getId(){
		return this.id;
	}
	
	public void setId(EntityAttributeValuePk id){
		this.id = id;
	}
	
	@Transient
	public Entity getEntity() {
		return id.getEntity();
	}

	public void setEntity(Entity entity) {
		id.setEntity(entity);
	}

	
	@Transient
	public EntityAttribute getEntityAttribute() {
		return id.getEntityAttribute();
	}

	public void setEntityAttribute(EntityAttribute attribute) {
		id.setEntityAttribute(attribute);
	}

	
	/**
	 * value for string attributes
	 * @return
	 */
	@Column(name="string_value")
	public String getStringValue(){
		return this.stringValue;
	}
	
	public void setStringValue(String stringValue){
		this.stringValue = stringValue;
	}
	
	/**
	 * value for double/boolean attributes
	 * @return
	 */
	@Column(name="double_value")
	public Double getDoubleValue(){
		return this.doubleValue;
	}
	
	public void setDoubleValue(Double doubleValue){
		this.doubleValue = doubleValue;
	}
	
	/**
	 * value for list attributes
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="list_element_uuid", referencedColumnName="uuid")
	public AttributeListItem getListItem(){
		return this.listItem;
	}
	
	public void setListItem(AttributeListItem listItem){
		this.listItem = listItem;
	}

	/**
	 * value for tree attributes
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="tree_node_uuid", referencedColumnName="uuid")
	public AttributeTreeNode getTreeNode(){
		return this.treeNode;
	}
	
	public void setTreeNode(AttributeTreeNode treeNode){
		this.treeNode = treeNode;
	}
	
	@Embeddable
	private static class EntityAttributeValuePk implements Serializable{
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		private Entity entity;
		private EntityAttribute attribute;
		
		public EntityAttributeValuePk(){
		}
		
		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="entity_uuid", referencedColumnName="uuid")
		public Entity getEntity() {
			return entity;
		}

		public void setEntity(Entity entity) {
			this.entity = entity;
		}

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinColumn(name="entity_attribute_uuid", referencedColumnName="uuid")
		public EntityAttribute getEntityAttribute() {
			return attribute;
		}

		public void setEntityAttribute(EntityAttribute attribute) {
			this.attribute = attribute;
		}
	}
}

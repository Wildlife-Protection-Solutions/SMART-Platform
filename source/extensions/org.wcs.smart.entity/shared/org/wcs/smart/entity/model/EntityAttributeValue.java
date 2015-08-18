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
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;

/**
 * Represents an value for an entity attribute.
 * 
 * @author Emily
 *
 */
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
	@Column(name="number_value")
	public Double getNumberValue(){
		return this.doubleValue;
	}
	
	public void setNumberValue(Double doubleValue){
		this.doubleValue = doubleValue;
	}
	
	/**
	 * value for list attributes
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="list_element_uuid", referencedColumnName="uuid")
	public AttributeListItem getAttributeListItem(){
		return this.listItem;
	}
	
	public void setAttributeListItem(AttributeListItem listItem){
		this.listItem = listItem;
	}

	/**
	 * value for tree attributes
	 * @return
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="tree_node_uuid", referencedColumnName="uuid")
	public AttributeTreeNode getAttributeTreeNode(){
		return this.treeNode;
	}
	
	public void setAttributeTreeNode(AttributeTreeNode treeNode){
		this.treeNode = treeNode;
	}
	
	/**
	 * Date attribute types are stored
	 * as in the string field in the ISO8601 format
	 * of yyyy-mm-dd.  This is a transient
	 * function which converts the string value to 
	 * a date.
	 * 
	 * @return
	 */
	@Transient
	public Date getDateValue(){
		if (getStringValue() == null){
			return null;
		}
		return java.sql.Date.valueOf(getStringValue());
	}
	
	/**
	 * This calls setStringValue formating the
	 * date as required for SMART
	 * @return
	 */
	@Transient
	public void setDateValue(Date date){
		if (date == null){
			setStringValue(null);
			return;
		}
		java.sql.Date tmp = new java.sql.Date(date.getTime());
		setStringValue(tmp.toString());
	}
	
	/**
	 * 
	 * @return the string representation of the 
	 * entity attribute value
	 */
	@Transient
	public String getValueAsString(Locale l){
		String text = ""; //$NON-NLS-1$
		switch (getEntityAttribute().getDmAttribute().getType()){
		case TEXT:
			text = getStringValue();
			break;
		case DATE:
			if (getStringValue() != null){
				text = DateFormat.getDateInstance(DateFormat.MEDIUM).format(java.sql.Date.valueOf(getStringValue()));
			}
		case NUMERIC:
			if (getNumberValue() != null){
				text = String.valueOf(getNumberValue());	
			}
			break;
		case BOOLEAN:
			if (getNumberValue() != null){
				if (getNumberValue() < 0.5){
					text = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE,l);
				}else{
					text = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE,l);
				}
			}
			break;
		case LIST:
			if (getAttributeListItem() != null){
				text = getAttributeListItem().getName();
			}
			break;
		case TREE:
			if (getAttributeTreeNode() != null){
				text = getAttributeTreeNode().getName();
			}
			break;
		}
		if (text == null){
			text = ""; //$NON-NLS-1$
		}
		return text;
	}
	
	/**
	 * 
	 * @return the object representation of the 
	 * entity attribute value
	 */
	@Transient
	public Object getValue(){
		switch (getEntityAttribute().getDmAttribute().getType()){
		case TEXT:
			return getStringValue();
		case NUMERIC:
			return getNumberValue();	
		case BOOLEAN:
			return getNumberValue();
		case LIST:
			return getAttributeListItem();
		case TREE:
			return getAttributeTreeNode();
		case DATE:
			return getDateValue();
		}
		return null;
	}
	
	/**
	 * 
	 * @return set the object representation of the 
	 * entity attribute value
	 */
	@Transient
	public void setValue(Object value){
		switch (getEntityAttribute().getDmAttribute().getType()){
		case TEXT:
			setStringValue((String)value);
			break;
		case DATE:
			setDateValue((Date)value);
			break;
		case NUMERIC:
			setNumberValue((Double)value);
			break;
		case BOOLEAN:
			if (value == null){
				setNumberValue(null);
			}else if ((Boolean)value){
				setNumberValue(1.0);
			}else{
				setNumberValue(0.0);
			}
			break;
		case LIST:
			setAttributeListItem((AttributeListItem) value);
			break;
		case TREE:
			setAttributeTreeNode((AttributeTreeNode) value);
			break;
		}
	}
	
	/**
	 * @param o
	 * @return
	 */
	@Override
	public boolean equals(Object o){
		if (o instanceof EntityAttributeValue){
			return this.id.equals(((EntityAttributeValue)o).id);
		}
		return false;
	}
	
	/**
	 * @return
	 */
	@Override
	public int hashCode(){
		return id.hashCode();
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
		
		@ManyToOne(fetch = FetchType.LAZY, cascade={CascadeType.ALL})
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
		
		@Override
		public boolean equals(Object key) {
			if (! (key instanceof EntityAttributeValuePk)){
				return false;
			}
			EntityAttributeValuePk p = (EntityAttributeValuePk)key;
			
			if (p.entity == null || this.entity == null ||
				p.attribute == null || this.attribute == null ){
				
				if (p.entity == null && this.entity == null && 
					p.attribute == null && this.attribute == null){
						return true;
				}
				return false;
			}
			
			return p.entity.equals(this.entity) &&
					p.attribute.equals(this.attribute);
		}
		
		@Override
		public int hashCode() {
		    int code = 0;
		    if (entity != null) {code += entity.hashCode();}
		    code *= 31;
		    if (attribute != null) {code += attribute.hashCode(); }
		    return code;
		}
	}
}

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
package org.wcs.smart.observation.model;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ICoreLabelProvider;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;


/**
 * Waypoint observation attribute.
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name="smart.wp_observation_attributes")

public class WaypointObservationAttribute extends UuidItem{

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private WaypointObservation observation;
	private Attribute attribute;
	
	private AttributeListItem listItem;
	private AttributeTreeNode nodeItem;
	private String sValue;
	private Double dValue;
	
	private Collection<WaypointObservationAttributeList> listItems;
	
	public WaypointObservationAttribute(){
		
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="observation_uuid")
	public WaypointObservation getObservation(){
		return this.observation;
	}
	public void setObservation(WaypointObservation observation){
		this.observation = observation;
	}
	
	@ManyToOne(fetch =FetchType.LAZY)
	@JoinColumn(name="attribute_uuid", referencedColumnName="uuid")
	public Attribute getAttribute(){
		return this.attribute;
	}
	public void setAttribute(Attribute attribute){
		this.attribute = attribute;
	}
	
	@ManyToOne(fetch =FetchType.LAZY)
	@JoinColumn(name="list_element_uuid", referencedColumnName="uuid")
	public AttributeListItem getAttributeListItem(){
		return this.listItem;
	}
	public void setAttributeListItem(AttributeListItem item){
		this.listItem = item;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy = "id.observationAttribute", cascade=CascadeType.ALL, orphanRemoval = true)
	public Collection<WaypointObservationAttributeList> getAttributeListItems(){
		return this.listItems;
	}
	public void setAttributeListItems(Collection<WaypointObservationAttributeList> items){
		this.listItems = items;
	}
	
	@ManyToOne(fetch =FetchType.LAZY)
	@JoinColumn(name="tree_node_uuid", referencedColumnName="uuid")
	public AttributeTreeNode getAttributeTreeNode(){
		return this.nodeItem;
	}
	public void setAttributeTreeNode(AttributeTreeNode node){
		this.nodeItem = node;
	}
	
	@Column(name="string_value")
	public String getStringValue(){
		return this.sValue;
	}
	public void setStringValue(String value){
		this.sValue = value;
	}
	
	@Column(name="number_value")
	public Double getNumberValue(){
		return this.dValue;
	}
	public void setNumberValue(Double value){
		this.dValue = value;
	}
	
	public boolean hasValue(){
		return this.dValue != null || this.listItem != null || this.nodeItem != null || this.sValue != null
				|| (this.listItems != null && !this.listItems.isEmpty());
	}
	
	
	/**
	 * Clones the observation attribute.  Does
	 * not clone the observation field - that must be set
	 * by the function calling clone.
	 */
	public WaypointObservationAttribute clone(){
		WaypointObservationAttribute clone = new WaypointObservationAttribute();

		clone.attribute = attribute;
		clone.attribute.getType(); /*ensure attribute has been loaded*/
		
		clone.listItem = listItem;
		if (dValue != null){
			clone.dValue = Double.valueOf(dValue);
		}
		clone.nodeItem = nodeItem;
		if (sValue != null){
			clone.sValue = new String(sValue);
		}
		
		if (listItems != null) {
			clone.setAttributeListItems(new ArrayList<>());
			for (WaypointObservationAttributeList li : listItems) {
				WaypointObservationAttributeList nli = new WaypointObservationAttributeList();
				nli.setObservationAttribute(clone);
				nli.setAttributeLisItem(li.getAttributeListItem());
				clone.getAttributeListItems().add(nli);
			}
		}
		return clone;
	}
	
	/**
	 * 
	 * @return the value of the observation based
	 * on the attribute type.
	 */
	@Transient
	public Object getAttributeValue(){
		AttributeType type = getAttribute().getType();
		switch(type) {
			case BOOLEAN:
			case NUMERIC:return getNumberValue();
			case DATE: return getDateValue();
			case LIST: return getAttributeListItem();
			case MLIST: {
				if (getAttributeListItems() == null) return Collections.emptySet();
				return getAttributeListItems().stream().map(m->m.getAttributeListItem()).collect(Collectors.toSet());
			}
			case TEXT: return getStringValue();
			case TREE: return getAttributeTreeNode();
		}
		
		throw new IllegalStateException("Invalid attribute type"); //$NON-NLS-1$
	}
	
	
	/**
	 * 
	 * @return sets the value of the given attribute based on the attribute type
	 * and type of object supplied
	 */
	@Transient
	public void setAttributeValue(Object newValue){
		AttributeType type = getAttribute().getType();
		switch(type){
		case BOOLEAN:
			if (newValue == null){
				setNumberValue(null);
			}else if (newValue instanceof Boolean){
				if ((Boolean)newValue){
					setNumberValue(1.0);
				}else{
					setNumberValue(0.0);
				}
			}else if (newValue instanceof Double){
				if (((Double)newValue) > 0.5){
					setNumberValue(1.0);
				}else{
					setNumberValue(0.0);
				}
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for boolean attribute"); //$NON-NLS-1$
			}
			break;
		case DATE:
			if (newValue == null){
				setDateValue(null);
			}else if (newValue instanceof LocalDate){
				setDateValue((LocalDate)newValue);
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for date attribute"); //$NON-NLS-1$
			}
			break;
		case LIST:
			if (newValue == null){
				setAttributeListItem(null);
			}else if (newValue instanceof AttributeListItem){
				setAttributeListItem((AttributeListItem)newValue);
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for list attribute"); //$NON-NLS-1$
			}
			break;
		case NUMERIC:
			if (newValue == null){
				setNumberValue(null);
			} else if (newValue instanceof Number){
				setNumberValue( ((Number)newValue).doubleValue());
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for numberic attribute"); //$NON-NLS-1$
			}
			break;
		case TEXT:
			if (newValue == null){
				setStringValue(null);
			}else if (newValue instanceof String){
				if (((String)newValue).length() == 0){
					setStringValue(null);	
				}else{
					setStringValue( (String)newValue );
				}
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for string attribute"); //$NON-NLS-1$
			}
			break;
		case TREE:
			if (newValue == null){
				setAttributeTreeNode(null);
			}else if (newValue instanceof AttributeTreeNode ){
				setAttributeTreeNode( (AttributeTreeNode)newValue );
			}else{
				throw new IllegalArgumentException(newValue.getClass() + " not a valid type for tree attribute"); //$NON-NLS-1$
			}
			break;		
		case MLIST:
			if (newValue == null) {
				if (getAttributeListItems() != null) getAttributeListItems().clear();
			}else if (newValue instanceof Collection<?>) {
				if (getAttributeListItems() == null) setAttributeListItems(new ArrayList<>());

				Collection<?> newItems = (Collection<?>)newValue;
				Set<AttributeListItem> addItems = new HashSet<>();				
				for (Object x : newItems) {
					AttributeListItem li = null;
					if (x instanceof AttributeListItem) li = (AttributeListItem)x;
					if (x instanceof WaypointObservationAttributeList) li = ((WaypointObservationAttributeList)x).getAttributeListItem();
					if (li == null) continue;
					addItems.add(li);
				}
				
				List<WaypointObservationAttributeList> toRemove = new ArrayList<>();
				for (Iterator<WaypointObservationAttributeList> iterator = getAttributeListItems().iterator(); iterator.hasNext();) {
					WaypointObservationAttributeList item = (WaypointObservationAttributeList) iterator.next();
					if (!addItems.contains(item.getAttributeListItem())) {
						toRemove.add(item);
					}else {
						addItems.remove(item.getAttributeListItem());
					}
				}
				getAttributeListItems().removeAll(toRemove);
				
				for (AttributeListItem li : addItems) {
					WaypointObservationAttributeList newitem = new WaypointObservationAttributeList();
					newitem.setObservationAttribute(this);
					newitem.setAttributeLisItem(li);
					getAttributeListItems().add(newitem);
				}
				
			}
			break;
		default:
			throw new IllegalStateException("Invalid attribute type"); //$NON-NLS-1$
		}
		
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
	public LocalDate getDateValue(){
		if (getStringValue() == null){
			return null;
		}
		return LocalDate.parse(getStringValue(), DateTimeFormatter.ISO_LOCAL_DATE);
	}
	
	/**
	 * This calls setStringValue formating the
	 * date as required for SMART
	 * @return
	 */
	@Transient
	public void setDateValue(LocalDate date){
		if (date == null){
			setStringValue(null);
			return;
		}
		setStringValue(DateTimeFormatter.ISO_LOCAL_DATE.format(date));
	}
	
	
	/**
	 * The string representation of the attribute value based
	 * on the attribute type as follows:
	 * * TEXT - return the text string
	 * * BOOLEAN - return Attribute.BOOLEAN_FALSE_LABEL or BOOLEAN_TRUE_LABEL
	 * * LIST - return the name of the list item or empty string
	 * * TREE - name of the tree node or empty string
	 * * NUMERIC - string representation of numeric value
	 * * DATE - the date string in format default locale medium format
	 * 
	 * @return the string representation of the attribute values.
	 */
	@Transient
	public String getAttributeValueAsString(Locale l){
		String text = ""; //$NON-NLS-1$
		switch (getAttribute().getType()){
		case TEXT:
			if (getStringValue() != null){
				text = getStringValue();
			}
			break;
		case DATE:
			if (getStringValue() != null){
				text = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).format(getDateValue());
			}
			break;
		case NUMERIC:
			if (getNumberValue() != null){
				text = String.valueOf(getNumberValue());	
			}
			break;
		case BOOLEAN:
			if (getNumberValue() != null){
				if (getNumberValue() < 0.5){
					text = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.FALSE, l);
				}else{
					text = SmartContext.INSTANCE.getClass(ICoreLabelProvider.class).getLabel(Boolean.TRUE, l);
				}
			}
			break;
		case LIST:
			if (getAttributeListItem() != null){
				text = getAttributeListItem().getName();
			}
			break;
		case MLIST:
			if (getAttributeListItems() != null && !getAttributeListItems().isEmpty()) {
				
				StringBuilder sb = new StringBuilder() ;
				for (WaypointObservationAttributeList li : getAttributeListItems()) {
					sb.append(li.getAttributeListItem().getName());
					sb.append(", "); //$NON-NLS-1$
				}
				text = sb.substring(0,  sb.length() - 2);
			}
		case TREE:
			if (getAttributeTreeNode() != null){
				text = getAttributeTreeNode().getName();
			}
			break;
		}
		return text;
	}
	
	
}

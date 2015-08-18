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
package org.wcs.smart.ca.datamodel;

import java.sql.Date;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;


import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Where;
import org.wcs.smart.ca.ConservationArea;

/**
 * Conservation area data model attribute object
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.dm_attribute")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Attribute extends DmObject{
	public static final int STRING_ATTRIBUTE_MAX_LENGTH = 1024;
	public static final String DATE_FORMAT = "yyyy-mm-dd"; //$NON-NLS-1$
	
	/**
	 * Conservation are associated with attribute
	 */
	private ConservationArea ca;
	
	/**
	 * If the attribute is required
	 */
	private boolean isRequired;
	/**
	 * Type of attribute
	 */
	private AttributeType type;
	
	/* for numeric attributes */
	private List<Aggregation> aggregations = null; //list of aggregations
	private Double minValue;	//minimum value
	private Double maxValue;	//maximum value
	
	/* for string attributes */
	private String regex;	
	
	/* for list type attributes */
	private List<AttributeListItem> attributeList = null;
	
	/* for tree type attributes */
	private List<AttributeTreeNode> rootTreeNodes = null;
	private List<AttributeTreeNode> activeTootTreeNodes = null;
	
	/**
	 * Represents the type of the data model attribute
	 */
	public enum AttributeType{
		NUMERIC("n"), //$NON-NLS-1$
		TEXT("s"), //$NON-NLS-1$
		LIST("l"), //$NON-NLS-1$
		TREE("t"), //$NON-NLS-1$
		BOOLEAN("b"), //$NON-NLS-1$
		DATE("d"); //$NON-NLS-1$
		
		/**
		 * type key is used in the queries
		 */
		public String typeKey;
	
		private AttributeType(String typeKey){
			this.typeKey = typeKey;
		}
	}
	
	/**
	 * Parses the attribute type key (n, l etc.) into an attribute type.
	 * @param typeKey attribute type key
	 * @return attribute type or null if not found
	 */
	public static final AttributeType decodeAttributeTypeKey(String typeKey){
		for (int i = 0; i < AttributeType.values().length; i ++){
			if (AttributeType.values()[i].typeKey.equalsIgnoreCase(typeKey)){
				return AttributeType.values()[i];
			}
		}
		return null;
	}
	
	/**
	 * Parses the attribute type NAME into an attribute type.
	 * @param type attribute type (tree, list etc.)
	 * @return attribute type or null if not found
	 */
	public static final AttributeType decodeAttributeType(String type){
		for (int i = 0; i < AttributeType.values().length; i ++){
			if (AttributeType.values()[i].name().equalsIgnoreCase(type)){
				return AttributeType.values()[i];
			}
		}
		return null;
	}
	
	/**
	 * Creates a new attribute
	 */
	public Attribute(){
		super();
	}
	
	/**
	 * 
	 * @return <code>true</code> if attribute must be populated, <code>false</code> otherwise
	 */
	@Column(name = "is_required")
	public boolean getIsRequired(){
		return isRequired;
	}
	/**
	 * 
	 * @param isRequired if attribute is required
	 */
	public void setIsRequired(boolean isRequired){
		this.isRequired = isRequired;
	}
	
	/**
	 * 
	 * @return the attribute type
	 */
	@Column(name="att_type")
	@Enumerated(EnumType.STRING)
	public AttributeType getType() {
		return type;
	}
	/**
	 * 
	 * @param type the attribute type
	 */
	public void setType(AttributeType type) {
		this.type = type;
	}
	
	/**
	 * 
	 * @return the conservation area associated with the attribute
	 */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return ca;
	}
	/**
	 * 
	 * @param ca the conservation area to be associated with the attribute
	 */
	public void setConservationArea(ConservationArea ca) {
		this.ca = ca;
	}
	
	/**
	 * Only valid for numeric attributes.
	 * @return the minimum value of the attribute
	 */
	@Column(name="min_value")
	public Double getMinValue() {
		return minValue;
	}


	/**
	 * Only valid for numeric attributes.
	 * @param minValue the minimum value of the attribute
	 */
	public void setMinValue(Double minValue) {
		this.minValue = minValue;
	}

	/**
	 * Only valid for numeric attributes.
	 * @return the maximum value of the attribute
	 */
	@Column(name="max_value")
	public Double getMaxValue() {
		return maxValue;
	}

	/**
	 * Only valid for numeric attributes.
	 * @param maxValue the maximum value of the attribute
	 */
	public void setMaxValue(Double maxValue) {
		this.maxValue = maxValue;
	}

	/**
	 * Only valid for text attributes.
	 * 
	 * @return a regex pattern for validating string values
	 */
	public String getRegex() {
		return regex;
	}


	/**
	 * Only valid for text attributes.
	 * 
	 * @param regex the regex pattern for validating string values
	 */
	public void setRegex(String regex) {
		this.regex = regex;
	}


	/**
	 * 
	 * Only valid for numeric attributes.
	 * 
	 * @return the set of aggregations that are valid for the attribute
	 */
	@ManyToMany(fetch = FetchType.LAZY)
	@JoinTable(name="smart.dm_att_agg_map", 
	 joinColumns = {@JoinColumn(name="attribute_uuid")},
	 inverseJoinColumns = {@JoinColumn(name="agg_name")}
	)
	public List<Aggregation> getAggregations(){
		return this.aggregations;
	}
	/**
	 *  Only valid for numeric attributes.
	 *  
	 * @param aggs the set of aggregations that are valid for the attribute
	 */
	public void setAggregations(List<Aggregation> aggs){
		this.aggregations = aggs;
	}
	
	/**
	 * 
	 * @return list of active list items
	 */
	@Transient
	public List<AttributeListItem> getActiveListItems(){
		List<AttributeListItem> items = new ArrayList<AttributeListItem>();
		for(AttributeListItem item : getAttributeList()){
			if (item.getIsActive()){
				items.add(item);
			}
		}
		return items;
	}
	/**
	 * Only valid for list attributes.
	 * 
	 * @return set of valid list elements
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute", cascade={CascadeType.ALL}, orphanRemoval=true)
	@OrderBy(clause = "list_order")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<AttributeListItem> getAttributeList(){
		return this.attributeList;
	}
	/**
	 * Only valid for list attributes.
	 * 
	 * @param attributeList the set of valid list elements
	 */
	public void setAttributeList(List<AttributeListItem> attributeList){
		this.attributeList = attributeList;
	}
	

	/**
	 * Only valid for tree attributes.
	 * @return  set of root tree nodes
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute", cascade = {CascadeType.ALL}, orphanRemoval=true)
	@Where(clause = "parent_uuid is null")
	@OrderBy(clause = "node_order")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<AttributeTreeNode> getTree(){
		return this.rootTreeNodes;
	}
	/**
	 * Only valid for tree attributes.
	 * 
	 * @param tree the set of root tree nodes
	 */
	public void setTree(List<AttributeTreeNode> tree){
		this.rootTreeNodes = tree;
	}
	
	
	/**
	 * Only valid for tree attributes.
	 * @return  set of root tree nodes
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute")
	@Where(clause = "parent_uuid is null and is_active")
	@OrderBy(clause = "node_order")
	//@BatchSize(size=200)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<AttributeTreeNode> getActiveTreeNodes(){
		return this.activeTootTreeNodes;
	}
	/**
	 * Only valid for tree attributes.
	 * 
	 * @param tree the set of root tree nodes
	 */
	public void setActiveTreeNodes(List<AttributeTreeNode> activeTootTreeNodes){
		this.activeTootTreeNodes = activeTootTreeNodes;
	}
	
	/**
	 * Clones an attribute.
	 * @param newCa the new conservation area to associated with the attribute
	 * 
	 * @return a cloned attribute 
	 */
	public Attribute clone(ConservationArea newCa, String defaultLang){
		Attribute clone = new Attribute();
		clone.copyValues(this, newCa, defaultLang);
		clone.setConservationArea(newCa);
		clone.setIsRequired(this.isRequired);
		if (this.maxValue != null){
			clone.setMaxValue(this.maxValue.doubleValue());
		}
		if (this.minValue != null){
			clone.setMinValue(this.minValue.doubleValue());
		}
		clone.setRegex(this.getRegex());
		clone.setType(this.getType());
		
		if (this.getAggregations() != null){
			clone.setAggregations(new ArrayList<Aggregation>());
			for (Aggregation agg: this.getAggregations()){
				clone.aggregations.add(agg);
			}
		}
		if (this.getAttributeList() != null){
			clone.attributeList = new ArrayList<AttributeListItem>();
			for(AttributeListItem it : this.getAttributeList()){
				clone.attributeList.add(it.clone(clone, this.ca,defaultLang));
			}
		}
		if (getTree() != null){
			clone.rootTreeNodes = new ArrayList<AttributeTreeNode>();
			for (Iterator<AttributeTreeNode> iterator = getTree().iterator(); iterator.hasNext();) {
				AttributeTreeNode node = (AttributeTreeNode) iterator.next();
				clone.rootTreeNodes.add(node.clone(newCa, this.ca, null,defaultLang, clone));
			}
			
		}
		
		return clone;
	}

	public static void moveAttributeTreeNode(List<AttributeTreeNode> rootNodes, AttributeTreeNode toMove, 
			AttributeTreeNode to, boolean before){
		if (to.equals(toMove)){
			return;
		}
		List<AttributeTreeNode> siblings = null;
		if (toMove.getParent() == null){
			siblings = rootNodes;
		}else{
			siblings = toMove.getParent().getChildren();
		}
		
		
		siblings.remove(toMove);
		int index = siblings.indexOf(to);
		if (before){
			siblings.add(index, toMove);
		}else{
			siblings.add(index + 1, toMove);
		}
		
		//reset numbers
		for (int i = 0; i < siblings.size(); i ++){
			siblings.get(i).setNodeOrder(i);
		}
	}
	
	public static boolean isValidDateString(String date){
		try{
			Date.valueOf(date);
			return true;
		}catch (Exception ex){
			return false;
		}
	}
}

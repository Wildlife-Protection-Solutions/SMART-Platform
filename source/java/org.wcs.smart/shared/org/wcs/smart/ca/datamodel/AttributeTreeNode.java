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

import java.util.ArrayList;
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

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Where;
import org.wcs.smart.ca.ConservationArea;

/**
 * An attribute tree node element.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.dm_attribute_tree")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class AttributeTreeNode extends DmObject implements HkeyObject{

	private static final String FULL_NAME_SEPARATOR = " - "; //$NON-NLS-1$
	
	private Attribute attribute = null;
	private int nodeOrder;
	private List<AttributeTreeNode> children = new ArrayList<AttributeTreeNode>();
	private List<AttributeTreeNode> activeChildren = new ArrayList<AttributeTreeNode>();
	private AttributeTreeNode parent = null;
	private boolean isActive;
	
	private String hkey;
	
	public AttributeTreeNode(){
		super();
	}
	
	/*
	 * This attribute is here to to able
	 * to delete references quickly.  A database
	 * FK cascade delete link is used to cascade 
	 * delete when the attribute is removed.
	 */
	@ManyToOne
	@JoinColumn(name="attribute_uuid")
	public Attribute getAttribute(){
		return this.attribute;
	}
	
	public void setAttribute(Attribute attribute){
		this.attribute = attribute;
	}
	
	@Column(name="node_order")
	public int getNodeOrder(){
		return this.nodeOrder;
	}
	
	public void setNodeOrder(int nodeOrder){
		this.nodeOrder = nodeOrder;
	}
	
	/**
	 * Updates the hkey of this object
	 * and children tree nodes.
	 */
	public void updateHkey(){
		setHkey(computeHkey());
		
		if (getChildren() != null){
			for (AttributeTreeNode child : getChildren()){
				child.updateHkey();
			}
		}
	}
	
	
	/**
	 * Computes the hkey for the given category.
	 * 
	 * @return the hkey for this category.
	 */
	private String computeHkey(){
		if (parent == null){
			return this.getKeyId() + HkeyObject.HKEY_SEPERATOR;
		}
		return parent.computeHkey() + this.getKeyId() + HkeyObject.HKEY_SEPERATOR;
	}
	
	@Column(name="hkey")
	public String getHkey(){
		return this.hkey;
	}
	
	public void setHkey(String hkey){
		this.hkey = hkey;
	}
	
	/**
	 * 
	 * @return children nodes
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="parent", cascade = {CascadeType.ALL}, orphanRemoval = true)
	@OrderBy(clause = "node_order")
	@BatchSize(size=200)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<AttributeTreeNode> getChildren(){
		return this.children;
	}
	
	public void setChildren(List<AttributeTreeNode> children){
		this.children = children;
	}
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="parent", cascade = {CascadeType.ALL})
	@Where(clause = "is_active")
	@OrderBy(clause = "node_order")
	@BatchSize(size=200)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<AttributeTreeNode> getActiveChildren(){
		return this.activeChildren;
	}
	
	public void setActiveChildren(List<AttributeTreeNode> activeChildren){
		this.activeChildren = activeChildren;
	}
	
	/**
	 * 
	 * @return parent tree node or null if root node
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="parent_uuid")
//	@Cascade({CascadeType.SAVE_UPDATE})	
	public AttributeTreeNode getParent(){
		return this.parent;
	}
	public void setParent(AttributeTreeNode parent){
		this.parent = parent;
	}
	
	/**
	 * 
	 * @param isActive the active state of the tree node
	 */
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	@Column(name="is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	
	/**
	 * 
	 * @return the attribute name concatenated with
	 * all parent attribute names.
	 */
	@Transient
	public String getFullCategoryName(){
		if (parent == null){
			return getName();
		}else{
			return getName() + FULL_NAME_SEPARATOR + parent.getFullCategoryName(); 
		}
	}
	
	/**
	 * Clones an attribute tree node and children nodes excluding the uuid field
	 * @param newCa the new conservation area to associated with the tree node
	 * @param oldCa the conservation area of the object being cloned 
	 * @param parent the attribute tree parent node 
	 * @return a cloned attribute tree node
	 */
	public AttributeTreeNode clone(ConservationArea newCa, ConservationArea oldCa, AttributeTreeNode parent, 
			String defaultLang,  Attribute clonedAttribute  ){
		return clone(newCa, oldCa, parent, defaultLang, clonedAttribute, false);
	}
	/**
	 * Clones the attribute tree node and children nodes
	 * @param newCa conservation area cloning to (may be same as oldCa)
	 * @param oldCa original conservation area cloning from
	 * @param parent parent tree node
	 * @param defaultLang defaultLanguage to copy labels from
	 * @param clonedAttribute parent attribute tree node
	 * @param copyUuid if uuid field should be cloned as well
	 * @return
	 */
	public AttributeTreeNode clone(ConservationArea newCa, ConservationArea oldCa, AttributeTreeNode parent, 
			String defaultLang,  Attribute clonedAttribute, boolean copyUuid  ){
		
		AttributeTreeNode clone = new AttributeTreeNode();
		clone.copyValues(this, newCa, defaultLang);
		if (copyUuid){
			clone.setUuid(getUuid());
		}
		clone.setIsActive(this.isActive);
		clone.setNodeOrder(this.getNodeOrder());
		clone.setParent(parent);
		clone.setAttribute(clonedAttribute);
		
		if (this.getChildren() != null){
			clone.setChildren(new ArrayList<AttributeTreeNode>());
			for (AttributeTreeNode node : this.getChildren()){
				clone.getChildren().add(node.clone(newCa, oldCa, clone, defaultLang, clonedAttribute, copyUuid));
			}
		}
		clone.updateHkey();
	
		return clone;
	}

}

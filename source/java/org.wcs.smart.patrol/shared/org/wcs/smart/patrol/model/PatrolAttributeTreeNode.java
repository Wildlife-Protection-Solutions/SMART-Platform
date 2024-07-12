/*
 * Copyright (C) 2024 Wildlife Conservation Society
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
package org.wcs.smart.patrol.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.hibernate.annotations.BatchSize;
import org.wcs.smart.ca.Label;
import org.wcs.smart.ca.NamedKeyIconItem;
import org.wcs.smart.ca.datamodel.HkeyObject;
import org.wcs.smart.ca.datamodel.ITreeNode;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * An attribute tree node element.
 * 
 * @author Emily
 * @since 8.1.0
 */
@Entity
@Table(name = "patrol_attribute_tree", schema="smart")
public class PatrolAttributeTreeNode extends NamedKeyIconItem implements HkeyObject, ITreeNode<PatrolAttributeTreeNode>{

	private static final long serialVersionUID = 1L;
	
	private PatrolAttribute attribute = null;
	private int nodeOrder;
	private List<PatrolAttributeTreeNode> children = new ArrayList<PatrolAttributeTreeNode>();
	private PatrolAttributeTreeNode parent = null;

	private boolean isActive;
	private String hkey;
	
	public PatrolAttributeTreeNode(){
		super();
	}
	
	@ManyToOne
	@JoinColumn(name="patrol_attribute_uuid")
	public PatrolAttribute getAttribute(){
		return this.attribute;
	}
	
	public void setAttribute(PatrolAttribute attribute){
		this.attribute = attribute;
	}
	
	@Column(name="node_order")
	public int getNodeOrder(){
		return this.nodeOrder;
	}
	
	public void setNodeOrder(int nodeOrder){
		this.nodeOrder = nodeOrder;
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
	@OrderBy("node_order")
	@BatchSize(size=200)
	public List<PatrolAttributeTreeNode> getChildren(){
		return this.children;
	}
	
	public void setChildren(List<PatrolAttributeTreeNode> children){
		this.children = children;
	}
	
	/**
	 * 
	 * @return parent tree node or null if root node
	 */
	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="parent_uuid")
	public PatrolAttributeTreeNode getParent(){
		return this.parent;
	}
	public void setParent(PatrolAttributeTreeNode parent){
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
	 * Clones the tree node and all children.
	 * 
	 * Set icon, but does not clone icons.
	 * Sets attribute but does not clone patrol attribute.
	 * Clones labels associated  with each node.
	 * @param parent
	 * @return
	 */
	public PatrolAttributeTreeNode cloneAll(PatrolAttributeTreeNode parent) {
		PatrolAttributeTreeNode clone = new PatrolAttributeTreeNode();
		clone.setUuid(getUuid());
//		clone.copyValues(this, newCa, defaultLang);
		clone.setIsActive(getIsActive());
		clone.setNodeOrder(getNodeOrder());
		clone.setAttribute(getAttribute());
		clone.setName(getName());
		clone.setIcon(getIcon());
		clone.setKeyId(getKeyId());
		clone.setHkey(getHkey());
		clone.setNames(new HashSet<>());
		for (Label l : getNames()) {
			clone.updateName(l.getLanguage(), l.getValue());
		}
		
		
		if (this.getChildren() != null){
			clone.setChildren(new ArrayList<>());
			for (PatrolAttributeTreeNode node : this.getChildren()){
				clone.getChildren().add(node.cloneAll(clone));
			}
		}
		clone.updateHkey();
		return clone;
		
	}

	@Transient
	public List<PatrolAttributeTreeNode> getActiveChildren() {
		return getChildren().stream().filter(f->f.getIsActive()).collect(Collectors.toList());
	}
	
	
}

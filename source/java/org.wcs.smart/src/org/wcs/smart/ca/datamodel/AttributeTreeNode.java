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
import java.util.Comparator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderColumn;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.CascadeType;
import org.hibernate.annotations.OrderBy;
import org.wcs.smart.ca.ConservationArea;

/**
 * An attribute tree node element.
 * 
 * @author Emily
 * @since 1.0.0
 */
@Entity
@Table(name = "smart.dm_attribute_tree")
public class AttributeTreeNode extends DmObject {

	private int nodeOrder;
	private List<AttributeTreeNode> children = new ArrayList<AttributeTreeNode>();
	private AttributeTreeNode parent = null;
	private boolean isActive;
	
	public AttributeTreeNode(){
		super();
	}
	
	@Column(name="node_order")
	public int getNodeOrder(){
		return this.nodeOrder;
	}
	
	public void setNodeOrder(int nodeOrder){
		this.nodeOrder = nodeOrder;
	}
	
	@OneToMany(fetch=FetchType.LAZY)
	@Cascade({CascadeType.SAVE_UPDATE})
	@JoinColumn(name="parent_uuid")
	@OrderBy(clause = "node_order")
	@BatchSize(size=100)
	public List<AttributeTreeNode> getChildren(){
		return this.children;
	}
	
	public void setChildren(List<AttributeTreeNode> children){
		this.children = children;
	}
	
	@ManyToOne(fetch=FetchType.LAZY)
	@Cascade({CascadeType.SAVE_UPDATE})
	@JoinColumn(name="parent_uuid", insertable=false, updatable=false)
	public AttributeTreeNode getParent(){
		return this.parent;
	}
	public void setParent(AttributeTreeNode parent){
		this.parent = parent;
	}
	
	public void setIsActive(boolean isActive){
		this.isActive = isActive;
	}
	@Column(name="is_active")
	public boolean getIsActive(){
		return this.isActive;
	}
	
	/**
	 * Clones an attribute tree node
	 * @param newCa the new conservation area to associated with the tree node
	 * @param oldCa the conservation area of the object being cloned 
	 * @param parent the attribute tree parent node 
	 * @return a cloned attribute tree node
	 */
	public AttributeTreeNode clone(ConservationArea newCa, ConservationArea oldCa, AttributeTreeNode parent, String defaultLang  ){
		AttributeTreeNode clone = new AttributeTreeNode();
		clone.copyValues(this, newCa, oldCa, defaultLang);
		clone.setIsActive(this.isActive);
		
		clone.setNodeOrder(this.getNodeOrder());
		clone.setParent(parent);
		
		if (this.getChildren() != null){
			clone.setChildren(new ArrayList<AttributeTreeNode>());
			for (AttributeTreeNode node : this.getChildren()){
				clone.getChildren().add(node.clone(newCa, oldCa, clone, defaultLang));
			}
		}
	
		return clone;
	}
	
	
	public static class NodeComparator implements Comparator<AttributeTreeNode> {

		public int compare(AttributeTreeNode d1, AttributeTreeNode d2){
			if (d1.getNodeOrder() == d2.getNodeOrder()) {
				return 0;
			} else if (d1.getNodeOrder() > d2.getNodeOrder()) {
				return 1;
			} else {
				return -1;
			}

		}
	}

}

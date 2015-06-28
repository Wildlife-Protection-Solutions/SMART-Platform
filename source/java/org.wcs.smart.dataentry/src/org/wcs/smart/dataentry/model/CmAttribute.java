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
package org.wcs.smart.dataentry.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Where;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;

/**
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.cm_attribute")
public class CmAttribute extends NamedItem {

	private CmNode node;
	private Attribute attribute;
	private Map<String, CmAttributeOption> cmAttributeOptions;
	private int order;
	
	/* for tree type attributes */
	private List<CmAttributeTreeNode> rootTreeNodes = null;
	
	/* for list type attributes */
	private List<CmAttributeListItem> listItems = null;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="node_uuid", referencedColumnName="uuid")
	public CmNode getNode() {
		return node;
	}
	public void setNode(CmNode node) {
		this.node = node;
	}
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="attribute_uuid", referencedColumnName="uuid")
	public Attribute getAttribute() {
		return attribute;
	}
	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}
	
	@OneToMany(fetch = FetchType.EAGER, mappedBy="cmAttribute", cascade={CascadeType.ALL}, orphanRemoval = true)
	@MapKey(name="optionId")
	public Map<String, CmAttributeOption> getCmAttributeOptions() {
		if (cmAttributeOptions == null)
			cmAttributeOptions = new HashMap<String, CmAttributeOption>();
		return cmAttributeOptions;
	}
	public void setCmAttributeOptions(Map<String, CmAttributeOption> cmAttributeOptions) {
		this.cmAttributeOptions = cmAttributeOptions;
	}
	
	@Column(name = "attribute_order")
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}

	/**
	 * Only valid for tree attributes.  Only returns nodes
	 * if a customized tree configuration is used for this attribute.
	 * 
	 * @return  set of root tree nodes
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute", cascade = {CascadeType.ALL}, orphanRemoval=true)
	@Where(clause = "parent_uuid is null")
	@OrderBy(clause = "node_order")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<CmAttributeTreeNode> getTree(){
		if (rootTreeNodes == null) {
			rootTreeNodes = new ArrayList<CmAttributeTreeNode>();
		}
		return this.rootTreeNodes;
	}
	public void setTree(List<CmAttributeTreeNode> tree){
		this.rootTreeNodes = tree;
	}

	/**
	 * 
	 * @return the nodes for the custom tree or the default tree depending on
	 * if a custom tree is configured for this node or not
	 */
	@Transient
	public List<CmAttributeTreeNode> getCurrentTree() {
		return isUseCustomConfig() ? getTree() : getDefaultTree();
	}
	
	/**
	 * Gets the default model (defined for the entire cm) for the 
	 * tree attribute
	 * @return
	 */
	@Transient
	public List<CmAttributeTreeNode> getDefaultTree() {
		return node.getModel().getDefaultTrees(attribute);
	}
	
	/**
	 * Only valid for list attributes.  Only returns items
	 * if a customized list configuration is used for this attribute.
	 * 
	 * @return  set of root tree nodes
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="attribute", cascade = {CascadeType.ALL}, orphanRemoval=true)
	@OrderBy(clause = "list_order")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<CmAttributeListItem> getList() {
		if (listItems == null) {
			listItems = new ArrayList<CmAttributeListItem>();
		}
		return this.listItems;
	}
	public void setList(List<CmAttributeListItem> list){
		this.listItems = list;
	}

	/**
	 * 
	 * @return the items for the custom list or the default list depending on
	 * if a custom list is configured for this attribute or not
	 */
	@Transient
	public List<CmAttributeListItem> getCurrentList() {
		return isUseCustomConfig() ? getList() : getDefaultList();
	}
	
	/**
	 * Gets the default model (defined for the entire cm) for the 
	 * list attribute
	 * @return
	 */
	@Transient
	public List<CmAttributeListItem> getDefaultList() {
		return node.getModel().getDefaultLists(attribute);
	}

	@Transient
	public boolean isVisible() {
		CmAttributeOption option = getCmAttributeOptions().get(CmAttributeOption.ID_IS_VISIBLE);
		return option == null || Boolean.TRUE.equals(option.getBooleanValue());
	}

	@Transient
	public boolean isMultiselect() {
		CmAttributeOption option = getCmAttributeOptions().get(CmAttributeOption.ID_MULTISELECT);
		return option != null && Boolean.TRUE.equals(option.getBooleanValue());
	}

	@Transient
	public boolean isNumeric() {
		CmAttributeOption option = getCmAttributeOptions().get(CmAttributeOption.ID_NUMERIC);
		return option != null && Boolean.TRUE.equals(option.getBooleanValue());
	}

	@Transient
	public boolean isFlattenTree() {
		CmAttributeOption option = getCmAttributeOptions().get(CmAttributeOption.ID_FLATTEN_TREE);
		return option != null && Boolean.TRUE.equals(option.getBooleanValue());
	}

	@Transient
	public boolean isUseCustomConfig() {
		CmAttributeOption option = getCmAttributeOptions().get(CmAttributeOption.ID_CUSTOM_CONFIG);
		return option != null && Boolean.TRUE.equals(option.getBooleanValue());
	}
	
}

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
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Where;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;

/**
 * Named configuration associated with specific datamodel attribute that can be shared
 * among several configurable model attributes. Configuration is shared only within
 * a configurable model (by design).
 * 
 * @author elitvin
 * @since 6.0.0
 *
 */
@Entity
@Table(name = "smart.cm_attribute_config")
public class CmAttributeConfig extends NamedItem {

	private ConfigurableModel model; 
	private Attribute attribute;
	private DisplayMode displayMode;
	private boolean isDefault;

	private List<CmAttributeTreeNode> rootTreeNodes = null;
	private List<CmAttributeListItem> listItems = null;

	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
	public ConfigurableModel getModel() {
		return model;
	}
	public void setModel(ConfigurableModel model) {
		this.model = model;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="dm_attribute_uuid", referencedColumnName="uuid")
	public Attribute getAttribute() {
		return attribute;
	}
	public void setAttribute(Attribute attribute) {
		this.attribute = attribute;
	}

	/**
	 * {@link DisplayMode} makes sense only for list and tree attributes
	 */
	@Column(name="display_mode")
	@Enumerated(EnumType.STRING)
	public DisplayMode getDisplayMode() {
		return displayMode;
	}
	public void setDisplayMode(DisplayMode displayMode) {
		this.displayMode = displayMode;
	}
	
	@Column(name="is_default")
	public boolean isDefault() {
		return isDefault;
	}
	public void setDefault(boolean isDefault) {
		this.isDefault = isDefault;
	}

	/**
	 * Only valid for tree attributes.  Only returns nodes
	 * if a customized tree configuration is used for this attribute.
	 * 
	 * @return  set of root tree nodes
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="config", cascade = {CascadeType.ALL}, orphanRemoval=true)
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
	 * Only valid for list attributes.  Only returns items
	 * if a customized list configuration is used for this attribute.
	 * 
	 * @return  set of root tree nodes
	 */
	@OneToMany(fetch=FetchType.LAZY, mappedBy="config", cascade = {CascadeType.ALL}, orphanRemoval=true)
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

}

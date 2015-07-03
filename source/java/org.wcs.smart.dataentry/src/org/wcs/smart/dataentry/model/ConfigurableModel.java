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
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Where;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.dataentry.dialog.composite.CmDefaultListsUtil;
import org.wcs.smart.dataentry.dialog.composite.CmDefaultTreesUtil;


/**
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.configurable_model")
public class ConfigurableModel extends NamedItem {

    private ConservationArea conservationArea;
    private List<CmNode> nodes; //the root nodes for the data model

	private List<CmAttributeTreeNode> defaultRootTreeNodes = null;
	private Map<Attribute, List<CmAttributeTreeNode>> attr2TreeMap = null;

	private List<CmAttributeListItem> defaultListItems = null;
	private Map<Attribute, List<CmAttributeListItem>> attr2ListMap = null;
    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
        return conservationArea;
    }

    public void setConservationArea(ConservationArea conservationArea) {
        this.conservationArea = conservationArea;
    }

	@OneToMany(fetch = FetchType.LAZY, mappedBy="model", orphanRemoval=true, cascade={CascadeType.ALL})
	@Where(clause = "parent_node_uuid is null")
	@OrderBy(clause = "node_order")
	public List<CmNode> getNodes() {
		if (nodes == null)
			nodes = new ArrayList<CmNode>();
		return nodes;
	}
	public void setNodes(List<CmNode> nodes) {
		this.nodes = nodes;
	}

	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="configurableModel", cascade = {CascadeType.ALL}, orphanRemoval=true)
	@Where(clause = "parent_uuid is null and dm_attribute_uuid is not null")
	@OrderBy(clause = "node_order")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<CmAttributeTreeNode> getDefaultTrees() {
		if (defaultRootTreeNodes == null) {
			defaultRootTreeNodes = new ArrayList<CmAttributeTreeNode>();
		}
		return this.defaultRootTreeNodes;
	}
	public void setDefaultTrees(List<CmAttributeTreeNode> tree){
		this.defaultRootTreeNodes = tree;
	}

	@Transient
	public List<CmAttributeTreeNode> getDefaultTrees(final Attribute attribute) {
		if (attr2TreeMap == null) {
			attr2TreeMap = new HashMap<Attribute, List<CmAttributeTreeNode>>();
		}
		List<CmAttributeTreeNode> result = attr2TreeMap.get(attribute);
		if (result == null) {
			result = new FilteredSubList<CmAttributeTreeNode>(getDefaultTrees()) {
				@Override
				protected boolean matches(CmAttributeTreeNode t) {
					return attribute.equals(t.getDmAttribute());
				}
			};
			attr2TreeMap.put(attribute, result);
		}
		return result;
	}
	
	@Transient
	public void addDefaultTreeModes(final Attribute attribute) {
		if (attr2TreeMap == null) {
			attr2TreeMap = new HashMap<Attribute, List<CmAttributeTreeNode>>();
		}
		List<CmAttributeTreeNode> result = attr2TreeMap.get(attribute);
		if (result == null) {
			result = new FilteredSubList<CmAttributeTreeNode>(getDefaultTrees()) {
				@Override
				protected boolean matches(CmAttributeTreeNode t) {
					return attribute.equals(t.getDmAttribute());
				}
			};
			if (result.isEmpty()) {
				//if we are here that this attribute was not added before (no data for it in default trees)
				result = CmDefaultTreesUtil.buildDefaultTree(this, attribute);
				getDefaultTrees().addAll(result);
			} else {
				attr2TreeMap.put(attribute, result);
			}
		}
	}	

	@Transient
	public void removeDefaultTrees(final Attribute attribute) {
		List<CmAttributeTreeNode> tree = getDefaultTrees(attribute);
		tree.clear(); //NOTE: as this is FilteredSubList is will remove given items from original defaultRootTreeNodes list
		attr2TreeMap.remove(attribute);
	}

	@OneToMany(fetch=FetchType.LAZY, mappedBy="configurableModel", cascade = {CascadeType.ALL}, orphanRemoval=true)
	@Where(clause = "dm_attribute_uuid is not null")
	@OrderBy(clause = "list_order")
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<CmAttributeListItem> getDefaultLists() {
		if (defaultListItems == null) {
			defaultListItems = new ArrayList<CmAttributeListItem>();
		}
		return this.defaultListItems;
	}
	public void setDefaultLists(List<CmAttributeListItem> list){
		this.defaultListItems = list;
	}

	@Transient
	public List<CmAttributeListItem> getDefaultLists(final Attribute attribute) {
		if (attr2ListMap == null) {
			attr2ListMap = new HashMap<Attribute, List<CmAttributeListItem>>();
		}
		List<CmAttributeListItem> result = attr2ListMap.get(attribute);
		if (result == null) {
			result = new FilteredSubList<CmAttributeListItem>(getDefaultLists()) {
				@Override
				protected boolean matches(CmAttributeListItem t) {
					return attribute.equals(t.getDmAttribute());
				}
			};
			attr2ListMap.put(attribute, result);
		}
		return result;
	}

	@Transient
	public void addDefaultListItems(final Attribute attribute) {
		if (attr2ListMap == null) {
			attr2ListMap = new HashMap<Attribute, List<CmAttributeListItem>>();
		}
		List<CmAttributeListItem> result = attr2ListMap.get(attribute);
		if (result == null) {
			result = new FilteredSubList<CmAttributeListItem>(getDefaultLists()) {
				@Override
				protected boolean matches(CmAttributeListItem t) {
					return attribute.equals(t.getDmAttribute());
				}
			};
			if (result.isEmpty()) {
				//if we are here that this attribute was not added before (no data for it in default lists)
				result = CmDefaultListsUtil.buildDefaultList(this, attribute);
				getDefaultLists().addAll(result);
			} else {
				attr2ListMap.put(attribute, result);
			}
		}
	}
	
	@Transient
	public void removeDefaultLists(final Attribute attribute) {
		List<CmAttributeListItem> list = getDefaultLists(attribute);
		list.clear(); //NOTE: as this is FilteredSubList is will remove given items from original defaultRootListItems list
		attr2ListMap.remove(attribute);
	}
	
	/**
	 * Moves an {@link CmNode} to a new position in the sibling list.
	 * 
	 * @param source the node to move
	 * @param target the node to move it to
	 * @param moveBefore if it should be moved before or after the <b>source</b> parameter
	 */
	@Transient
	public void moveNodePosition(CmNode source, CmNode target, boolean moveBefore) {
		if (source == target || source.equals(target)) {
			return;
		}
		List<CmNode> list = null;
		if (source.getParent() != null) {
			list = source.getParent().getChildren();
		} else {
			list = getNodes();
		}
		
		list.remove(source);
		if (moveBefore) {
			list.add(list.indexOf(target), source);
		} else {
			list.add(list.indexOf(target)+1, source);
		}
			
		for (int i = 0; i < list.size(); i ++){
			list.get(i).setNodeOrder(i);
		}
	}

	/**
	 * Moves {@link CmAttribute} to a new position in the sibling list.
	 * 
	 * @param source the attribute to move
	 * @param target the attribute to move it to
	 * @param moveBefore if it should be moved before or after the <b>source</b> parameter
	 */
	@Transient
	public void moveAttributePosition(CmAttribute source, CmAttribute target, boolean moveBefore) {
		if (source == target || source.equals(target)) {
			return;
		}
		if (source.getNode() != null) {
			List<CmAttribute> attrList = source.getNode().getCmAttributes();
			attrList.remove(source);
			if (moveBefore) {
				attrList.add(source.getNode().getCmAttributes().indexOf(target), source);
			} else {
				attrList.add(source.getNode().getCmAttributes().indexOf(target) + 1, source);
			}
			
			for (int i = 0; i < attrList.size(); i ++){
				attrList.get(i).setOrder(i);
			}
		}
	}

}

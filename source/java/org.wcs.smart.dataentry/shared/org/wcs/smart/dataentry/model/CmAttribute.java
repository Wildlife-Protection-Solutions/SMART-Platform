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

import java.util.Collections;
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

import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.model.CmAttributeOption.EnterOnceType;

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
	
	private CmAttributeConfig config = null;

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

	@ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.ALL})
	@JoinColumn(name="config_uuid", referencedColumnName="uuid")
	public CmAttributeConfig getConfig() {
		return config;
	}
	public void setConfig(CmAttributeConfig config) {
		this.config = config;
	}

	/**
	 * 
	 * @return the nodes for the custom tree or the default tree depending on
	 * if a custom tree is configured for this node or not
	 */
	@Transient
	public List<CmAttributeTreeNode> getCurrentTree() {
		return AttributeType.TREE.equals(attribute.getType()) ? getConfig().getTree() : Collections.emptyList();
	}
	
	/**
	 * 
	 * @return the items for the custom list or the default list depending on
	 * if a custom list is configured for this attribute or not
	 */
	@Transient
	public List<CmAttributeListItem> getCurrentList() {
		return AttributeType.LIST.equals(attribute.getType()) ? getConfig().getList() : Collections.emptyList();
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
	public EnterOnceType getEnterOnce() {
		CmAttributeOption option = getCmAttributeOptions().get(CmAttributeOption.ID_ENTER_ONCES);
		return option != null && option.getStringValue() != null ? EnterOnceType.valueOf(option.getStringValue()) : EnterOnceType.NONE;
	}
	
	/**
	 * Only valid for list and tree attributes.
	 * Option is responsible for display mode of list attributes configurations (or tree nodes at root level configuration).
	 * @return the {@link DisplayMode} configured in {@link CmAttributeConfig}.
	 */
	@Transient
	public DisplayMode getConfigDisplayMode() {
		return getConfig().getDisplayMode();
	}

}

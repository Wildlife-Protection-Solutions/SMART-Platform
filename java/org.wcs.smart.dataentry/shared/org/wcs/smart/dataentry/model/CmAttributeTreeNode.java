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

import java.io.File;
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
import javax.persistence.Transient;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.OrderBy;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.util.UuidUtils;

/**
 * Configurable model tree node.  Single node
 * that links back to the main data model tree node;
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "smart.cm_attribute_tree_node")
public class CmAttributeTreeNode extends NamedItem implements IImageAssociatedObject {
	
	private AttributeTreeNode dmTreeNode;

	private CmAttribute attribute = null;
	private Attribute dmAttribute = null;
	private int nodeOrder;
	private List<CmAttributeTreeNode> children = new ArrayList<CmAttributeTreeNode>();
	private CmAttributeTreeNode parent = null;
	private DisplayMode displayMode;
	private File imageFile;
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="dm_tree_node_uuid", referencedColumnName="uuid")
	public AttributeTreeNode getDmTreeNode() {
		return dmTreeNode;
	}
	public void setDmTreeNode(AttributeTreeNode dmTreeNode) {
		this.dmTreeNode = dmTreeNode;
	}
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="parent", cascade = {CascadeType.ALL}, orphanRemoval = true)
	@OrderBy(clause = "node_order")
	@BatchSize(size=200)
	@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
	public List<CmAttributeTreeNode> getChildren(){
		if (children == null) {
			children = new ArrayList<CmAttributeTreeNode>();
		}
		return this.children;
	}
	public void setChildren(List<CmAttributeTreeNode> children){
		this.children = children;
	}

	@ManyToOne(fetch=FetchType.LAZY)
	@JoinColumn(name="parent_uuid")
//	@Cascade({CascadeType.SAVE_UPDATE})	
	public CmAttributeTreeNode getParent(){
		return this.parent;
	}
	public void setParent(CmAttributeTreeNode parent){
		this.parent = parent;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_attribute_uuid", referencedColumnName="uuid")
	public CmAttribute getAttribute() {
		return attribute;
	}
	public void setAttribute(CmAttribute attribute) {
		this.attribute = attribute;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="dm_attribute_uuid", referencedColumnName="uuid")
	public Attribute getDmAttribute() {
		return dmAttribute;
	}
	public void setDmAttribute(Attribute dmAttribute) {
		this.dmAttribute = dmAttribute;
	}
	
	@Column(name="node_order")
	public int getNodeOrder(){
		return this.nodeOrder;
	}
	public void setNodeOrder(int nodeOrder){
		this.nodeOrder = nodeOrder;
	}

	private ConfigurableModel configurableModel;
	private boolean isActive;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
	public ConfigurableModel getConfigurableModel() {
		return configurableModel;
	}
	public void setConfigurableModel(ConfigurableModel configurableModel) {
		this.configurableModel = configurableModel;
	}
	
	@Column(name="is_active")
	public boolean getIsActive() {
		return isActive;
	}
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	/**
	 * {@link DisplayMode} indicates how children will be displayed for this node
	 */
	@Column(name="display_mode")
	@Enumerated(EnumType.STRING)
	public DisplayMode getDisplayMode() {
		return displayMode;
	}
	public void setDisplayMode(DisplayMode displayMode) {
		this.displayMode = displayMode;
	}
	
	@Transient
	@Override
	public File getImageFile() {
		return imageFile != null ? imageFile : new File(getImagePersistenceLocation());
	}
	@Transient
	@Override
	public void setImageFile(File file) {
		imageFile = file;
	}
	@Transient
	@Override
	public String getImagePersistenceLocation() {
		return getConfigurableModel().getFileDataStoreLocation() + File.separator + "tn_img1_" + UuidUtils.getDirectoryPath(getUuid()); //$NON-NLS-1$
	}
}

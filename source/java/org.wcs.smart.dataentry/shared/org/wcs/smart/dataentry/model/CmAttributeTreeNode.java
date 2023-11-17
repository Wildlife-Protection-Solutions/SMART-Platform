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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.SQLOrder;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.util.UuidUtils;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/**
 * Configurable model tree node.  Single node
 * that links back to the main data model tree node;
 * 
 * @author Emily
 *
 */
@Entity
@Table(name = "cm_attribute_tree_node", schema="smart")
public class CmAttributeTreeNode extends NamedItem implements IImageAssociatedObject {
	
	private static final long serialVersionUID = 1L;
	
	private AttributeTreeNode dmTreeNode;

	private CmAttributeConfig config;
	private int nodeOrder;
	private List<CmAttributeTreeNode> children = new ArrayList<CmAttributeTreeNode>();
	private CmAttributeTreeNode parent = null;
	private DisplayMode displayMode;
	private Path imageFile;
	private boolean isActive;
	private String extension; //image name extension
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="dm_tree_node_uuid", referencedColumnName="uuid")
	public AttributeTreeNode getDmTreeNode() {
		return dmTreeNode;
	}
	public void setDmTreeNode(AttributeTreeNode dmTreeNode) {
		this.dmTreeNode = dmTreeNode;
	}
	
	@OneToMany(fetch=FetchType.LAZY, mappedBy="parent", cascade = {CascadeType.ALL}, orphanRemoval = true)
	@SQLOrder("node_order")
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
	@JoinColumn(name="config_uuid", referencedColumnName="uuid")
	public CmAttributeConfig getConfig() {
		return config;
	}
	public void setConfig(CmAttributeConfig config) {
		this.config = config;
	}
	
	@Column(name="node_order")
	public int getNodeOrder(){
		return this.nodeOrder;
	}
	public void setNodeOrder(int nodeOrder){
		this.nodeOrder = nodeOrder;
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
	public boolean hasCustomImage() {
		if (this.extension == null) return false;
		if (this.extension.isEmpty()) return false;
		return true;
	}
	
	@Transient
	@Override
	public Path getImageFile() {
		if (imageFile != null) return imageFile;
		return getImagePersistenceLocation();
	}


	@Column(name="imagetype")
	public String getExtension() {
		return this.extension;
	}
	public void setExtension(String extension) {
		this.extension = extension;
	}
	
	@Transient
	@Override
	public void resetImageFile() {
		this.imageFile = null;
	}
	
	@Transient
	@Override
	public void setImageFile(Path file) {
		imageFile = file;
		if (imageFile == null) {
			setExtension(null);
		}else {
			String fileName = imageFile.getFileName().toString();
			int index = fileName.lastIndexOf('.');
			if (index >= 0) {
				setExtension(fileName.substring(index+1));
			}else {
				setExtension(""); //$NON-NLS-1$
			}
		}
	}
	
	@Transient
	@Override
	public Path getImagePersistenceLocation() {
		Path cmroot = getConfig().getModel().getFileDataStoreLocation();
		if (cmroot == null) return null;
		return cmroot.resolve(getDefaultImageFileName());
	}
	
	@Override
	@Transient
	public String getDefaultImageFileName() {
		//filename
		StringBuilder sb = new StringBuilder();
		sb.append("tn_img1_"); //$NON-NLS-1$
		sb.append(UuidUtils.getDirectoryPath(getUuid()));
		if (getExtension() == null) {
			sb.append(".jpg"); // for backwards compatibility; prior to 6.1 it was assumed the image format was jpg //$NON-NLS-1$
		}else if (getExtension().isEmpty()) {
			//image was cleared
		}else {
			sb.append("."); //$NON-NLS-1$
			sb.append(getExtension());
		}
		return sb.toString();
	}
}

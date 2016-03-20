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

import org.hibernate.annotations.OrderBy;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Category;

/**
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.cm_node")
public class CmNode extends NamedItem {

	private ConfigurableModel model; 
	private Category category; 
	private CmNode parent;
	private int nodeOrder;
	private List<CmNode> children;
	private List<CmAttribute> cmAttributes;
	private boolean photoAllowed = false;
	private boolean photoRequired = true;
	private boolean collectMultipleObservations = false;
	private boolean useSingleGpsPoint = false;
	private DisplayMode displayMode;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
	public ConfigurableModel getModel() {
        return model;
    }
    public void setModel(ConfigurableModel model) {
        this.model = model;
    }

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="category_uuid", referencedColumnName="uuid")
	public Category getCategory() {
        return category;
    }
    public void setCategory(Category category) {
        this.category = category;
    }
    
	/**
	 * 
	 * @return parent node;  <code>null</code> if root node
	 */
	@ManyToOne(fetch = FetchType.LAZY, cascade={CascadeType.ALL})
	@JoinColumn(name="parent_node_uuid", referencedColumnName="uuid")
	public CmNode getParent() {
		return this.parent;
	}
	public void setParent(CmNode parent) {
		this.parent = parent;
	}

	@Column(name = "node_order")
	public int getNodeOrder() {
		return nodeOrder;
	}
	public void setNodeOrder(int nodeOrder) {
		this.nodeOrder = nodeOrder;
	}

	/**
	 * @return all children nodes; <code>null</code> if leaf node
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="parent", cascade={CascadeType.ALL}, orphanRemoval = true)
	@OrderBy(clause = "node_order")
	public List<CmNode> getChildren() {
		if (this.children == null)
			this.children = new ArrayList<CmNode>();
		return this.children;
	}
	public void setChildren(List<CmNode> children) {
		this.children = children;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="node", cascade={CascadeType.ALL}, orphanRemoval = true)
	@OrderBy(clause = "attribute_order")
	public List<CmAttribute> getCmAttributes() {
		if (cmAttributes == null)
			cmAttributes = new ArrayList<CmAttribute>();
		return cmAttributes;
	}
	public void setCmAttributes(List<CmAttribute> cmAttributes) {
		this.cmAttributes = cmAttributes;
	}

	@Column(name = "photo_allowed")
	public boolean isPhotoAllowed() {
		return photoAllowed;
	}
	public void setPhotoAllowed(Boolean photoAllowed) {
		this.photoAllowed = Boolean.TRUE.equals(photoAllowed); //null <==> false
	}
	
	@Column(name = "photo_required")
	public boolean isPhotoRequired() {
		return photoRequired;
	}
	public void setPhotoRequired(Boolean photoRequired) {
		this.photoRequired = !Boolean.FALSE.equals(photoRequired); //null <==> true
	}
	
	@Column(name = "collect_multiple_obs")
	public boolean isCollectMultipleObservations() {
		return collectMultipleObservations;
	}
	public void setCollectMultipleObservations(Boolean collectMultipleObservations) {
		this.collectMultipleObservations = Boolean.TRUE.equals(collectMultipleObservations); //null <==> false
	}

	@Column(name = "use_single_gps_point")
	public boolean isUseSingleGpsPoint() {
		return useSingleGpsPoint;
	}	
	public void setUseSingleGpsPoint(Boolean useSingleGpsPoint) {
		this.useSingleGpsPoint = Boolean.TRUE.equals(useSingleGpsPoint); //null <==> false
	}

	/**
	 * {@link DisplayMode} makes sense only for Groups
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
	public File getImageFile() {
		//TODO: implement real logic
		return new File("D:\\SMART\\_test_img\\fish4.jpg");
	}
	
	@Transient
	public boolean isGroup() {
		return category == null;
	}
}

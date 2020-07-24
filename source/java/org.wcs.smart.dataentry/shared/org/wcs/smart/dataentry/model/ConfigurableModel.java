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

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.annotations.OrderBy;
import org.hibernate.annotations.Where;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.icon.IconSet;
import org.wcs.smart.util.UuidUtils;


/**
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.configurable_model")
public class ConfigurableModel extends NamedItem {

	private static final long serialVersionUID = 1L;
	
    private ConservationArea conservationArea;
	private DisplayMode displayMode; //display mode for the root nodes
    private List<CmNode> nodes; //the root nodes for the data model
    private IconSet iconSet;
    
	private Map<Attribute, CmAttributeConfig> defaultConfigs;
	
	private boolean instantGps = false;
	private boolean photoFirst = false;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
        return conservationArea;
    }

    public void setConservationArea(ConservationArea conservationArea) {
        this.conservationArea = conservationArea;
    }
    
    @ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="iconset_uuid", referencedColumnName="uuid")
	public IconSet getIconSet() {
        return iconSet;
    }

    public void setIconSet(IconSet iconSet) {
        this.iconSet = iconSet;
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

	@Column(name="display_mode")
	@Enumerated(EnumType.STRING)
	public DisplayMode getDisplayMode() {
		return displayMode;
	}
	public void setDisplayMode(DisplayMode displayMode) {
		this.displayMode = displayMode;
	}

	@Column(name="instant_gps")
	public boolean isInstantGps() {
		return instantGps;
	}
	public void setInstantGps(Boolean instantGps) {
		this.instantGps = Boolean.TRUE.equals(instantGps); //null <==> false
	}

	@Column(name="photo_first")	
	public boolean isPhotoFirst() {
		return photoFirst;
	}
	public void setPhotoFirst(Boolean photoFirst) {
		this.photoFirst = Boolean.TRUE.equals(photoFirst); //null <==> false
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

	@OneToMany(fetch = FetchType.LAZY, mappedBy="model")
	@MapKey(name="attribute")
	@Where(clause = "is_default")
	public Map<Attribute, CmAttributeConfig> getDefaultConfigs() {
		if (defaultConfigs == null)
			defaultConfigs = new HashMap<>();
		return defaultConfigs;
	}
	public void setDefaultConfigs(Map<Attribute, CmAttributeConfig> defaultConfigs) {
		this.defaultConfigs = defaultConfigs;
	}
	
	/**
	 * @return the filestore location for the given configurable model
	 */
	@Transient
	public String getFileDataStoreLocation() {
		if (getUuid() == null) return ""; //$NON-NLS-1$
		
		return Paths.get(getConservationArea().getFileDataStoreLocation())
				.resolve("dataentry")  //$NON-NLS-1$
				.resolve(UuidUtils.getDirectoryPath(getUuid())).toString();
	}
	
}

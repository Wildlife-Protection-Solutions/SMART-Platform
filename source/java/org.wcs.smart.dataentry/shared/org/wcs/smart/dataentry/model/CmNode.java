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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.SQLOrder;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.SignatureType;
import org.wcs.smart.ca.datamodel.Category;
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
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "cm_node", schema="smart")
public class CmNode extends NamedItem implements IImageAssociatedObject {

	/*
	 * these strings match the smartDataType expected by the JSON 
	 * data processor used by the Conenct data api for adding data to 
	 * Connect
	 */
	public enum IntegrateIncidentType{
		INTEGRATEINCIDENT, //create normal incident in smart   
		INTEGRATEPATROL, //create incident to move to patrol in smart
		INTEGRATEPLLINK //create incidnet to link to patrol in smart
	}
	
	private static final String SIGNATURE_SPACER = ","; //$NON-NLS-1$

	private static final long serialVersionUID = 1L;
	
	private ConfigurableModel model; 
	
	private Category category; 
	private List<CmAttribute> cmAttributes;
	
	private CmNode parent;
	private int nodeOrder;
	private List<CmNode> children;
	private IntegrateIncidentType integrateType;
	
	private boolean photoAllowed = false;
	private boolean photoRequired = true;
	private boolean collectMultipleObservations = false;
	private boolean useSingleGpsPoint = false;
	private DisplayMode displayMode;
	private Path imageFile;
	private String extension; //image name extension
	private String signatures; //comma delimiter list of signature uuids valid for this node (if category)
	
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

	@Column(name = "integrate_incident_type")
	@Enumerated(EnumType.STRING)
	public IntegrateIncidentType getIntegrateIncidentType() {
		return integrateType;
	}
	public void setIntegrateIncidentType(IntegrateIncidentType type) {
		this.integrateType = type;
	}
	
	/**
	 * @return all children nodes; empty list if leaf node
	 */
	@OneToMany(fetch = FetchType.LAZY, mappedBy="parent", cascade={CascadeType.ALL}, orphanRemoval = true)
	@SQLOrder("node_order")
	public List<CmNode> getChildren() {
		if (this.children == null)
			this.children = new ArrayList<CmNode>();
		return this.children;
	}
	public void setChildren(List<CmNode> children) {
		this.children = children;
	}
	
	@OneToMany(fetch = FetchType.LAZY, mappedBy="node", cascade={CascadeType.ALL}, orphanRemoval = true)
	@SQLOrder("attribute_order")
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

	@Column(name="imagetype")
	public String getExtension() {
		return this.extension;
	}
	public void setExtension(String extension) {
		this.extension = extension;
	}
	
	@Column(name="signatures")
	public String getSignatures() {
		return this.signatures;
	}
	public void setSignatures(String signatures) {
		this.signatures = signatures;
	}
	
	@Transient
	public Set<UUID> getSignatureUuids(){
		if (getSignatures() == null) return Collections.emptySet();
		Set<UUID> items = new HashSet<>();
		for (String uuid : getSignatures().split(SIGNATURE_SPACER)) {
			try {
				items.add(UuidUtils.stringToUuid(uuid));
			}catch (Exception ex) {}
		}
		return items;
	}

	@Transient
	public void setSignatures(Set<SignatureType> types) {
		StringBuilder sb = new StringBuilder();
		for (SignatureType t : types) {
			sb.append(UuidUtils.uuidToString(t.getUuid()));
			sb.append(SIGNATURE_SPACER);
		}
		if (sb.length() == 0) {
			setSignatures((String)null);
			return;
		}
		sb.deleteCharAt(sb.length() - 1);
		setSignatures(sb.toString());
	}
	
	@Transient
	public boolean isGroup() {
		return category == null;
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
	
	@Transient
	@Override
	public void resetImageFile() {
		this.imageFile = null;
	}
	
	@Transient
	@Override
	public void setImageFile(Path file) {
		imageFile = file;
		//TODO: figure out how to delete old files if extension has change - if
		//extension is the same then the file will be overwritten and this
		//is not a problem
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
		Path cmroot = getModel().getFileDataStoreLocation();
		if (cmroot == null) return null;
		return cmroot.resolve(getDefaultImageFileName());
	}
	
	@Override
	@Transient
	public String getDefaultImageFileName() {
		//filename
		StringBuilder sb = new StringBuilder();
		sb.append("cn_img1_"); //$NON-NLS-1$
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
	
	
	
	/**
	 * 
	 * @return the names associated with the list element in the
	 * language the platform is running in.
	 */
	@Override
	@Transient
	public String getName() {
		String n = super.getName();
		if (n == null || n.length() == 0){
			if (getCategory() == null) return null;
			return getCategory().getName();
		}
		return n;
	}
	
	@Transient
	public String findDisplayName(Language language, Language defaultl) {
		//search for custom translations
		String l = super.findNameNull(language);
		if (l != null) return l;
		
		if (getCategory() != null) l = getCategory().findNameNull(language);
		if (l != null) return l;
		
		l = super.findNameNull(defaultl);
		if (l != null) return l;
		
		if (getCategory() != null) return getCategory().findName(defaultl);
		
		return "";	 //$NON-NLS-1$
		
	}
}

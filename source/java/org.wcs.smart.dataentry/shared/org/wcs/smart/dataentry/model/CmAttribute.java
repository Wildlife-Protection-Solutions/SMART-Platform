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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.model.CmAttributeOption.EnterOnceType;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.cm_attribute")
public class CmAttribute extends NamedItem implements IImageAssociatedObject{

	private static final long serialVersionUID = 1L;
	
	public enum HelpImageLocation{
		BEFORE,
		AFTER
	}
	
	private CmNode node;
	private Attribute attribute;
	private Map<String, CmAttributeOption> cmAttributeOptions;
	private int order;
	
	private CmAttributeConfig config = null;
	
	@Transient
	private Path importHelpFile;
	@Transient
	private Path imageFile;
	
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

	@ManyToOne(fetch = FetchType.LAZY)
	@Cascade(value = {org.hibernate.annotations.CascadeType.PERSIST, org.hibernate.annotations.CascadeType.MERGE, org.hibernate.annotations.CascadeType.REFRESH, org.hibernate.annotations.CascadeType.SAVE_UPDATE, org.hibernate.annotations.CascadeType.REPLICATE})
	@JoinColumn(name="config_uuid", referencedColumnName="uuid")
	public CmAttributeConfig getConfig() {
		return config;
	}
	public void setConfig(CmAttributeConfig config) {
		this.config = config;
	}
	

	@Transient
	private String getStringOption(String optionid) {
		CmAttributeOption op = getCmAttributeOptions().get(optionid);
		if (op == null) return null;
		return op.getStringValue();
	}
	@Transient
	public void setStringOption(String optionid, String value) {
		if (value == null || value.isEmpty()) {
			getCmAttributeOptions().remove(optionid);
			return;
		}
		CmAttributeOption op = getCmAttributeOptions().get(optionid);
		if (op == null) {
			op = new CmAttributeOption();
			op.setOptionId(optionid);
			op.setCmAttribute(this);
			getCmAttributeOptions().put(optionid, op);
		}
		op.setStringValue(value);
	}

	@Transient
	public String getHelpTextAsHtml(boolean fullpath) {
		if ( (getHelpText() == null || getHelpText().isEmpty()) &&
				getHelpImage() == null) return ""; //$NON-NLS-1$
				
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head>"); //$NON-NLS-1$
		sb.append("<title>"); //$NON-NLS-1$
		sb.append(getName());
		sb.append("</title>"); //$NON-NLS-1$
		sb.append("</head>"); //$NON-NLS-1$
		sb.append("<body>"); //$NON-NLS-1$
		
		Path p = null;
		if (getHelpFormat() != null) {
			if (getImportHelpFile() != null) {
				p = getImportHelpFile();
			}else {
				p = getHelpImage();
			}
			if (fullpath) {
				p = p.toAbsolutePath().normalize();
			}else {
				p = p.getFileName();
			}
		}
		if (getHelpImageLocation() == CmAttribute.HelpImageLocation.AFTER) {
			// after
			sb.append(getHelpText());
			if (p != null) sb.append("<br><img src='" + p.toString() + "'/>"); //$NON-NLS-1$ //$NON-NLS-2$
		} else {
			// before
			if (p != null) sb.append("<br><img src='" + p.toString() + "'/>"); //$NON-NLS-1$ //$NON-NLS-2$
			sb.append(getHelpText());
		}
		sb.append("</body></html>"); //$NON-NLS-1$
		return sb.toString();
	}
			
	@Transient
	public String getHelpText() {
		return getStringOption(CmAttributeOption.ID_HELP_TEXT);
	}
	@Transient
	public void setHelpText(String helpText) {
		setStringOption(CmAttributeOption.ID_HELP_TEXT, helpText);
	}
	@Transient	
	public String getHelpFormat() {
		return getStringOption(CmAttributeOption.ID_HELP_IMAGE_FORMAT);
	}
	@Transient
	public void setHelpFormat(String helpImageFormat) {
		setStringOption(CmAttributeOption.ID_HELP_IMAGE_FORMAT, helpImageFormat);
	}
	@Transient	
	public HelpImageLocation getHelpImageLocation() {
		String v = getStringOption(CmAttributeOption.ID_HELP_IMAGE_LOCATION);
		if (v == null) return null;
		return HelpImageLocation.valueOf(v);
	}
	
	@Transient
	public void setHelpImageLocation(HelpImageLocation location) {
		setStringOption(CmAttributeOption.ID_HELP_IMAGE_LOCATION, location == null ? null : location.name());
	}
	
	@Transient
	public Path getHelpImage() {
		if (getHelpFormat() == null) return null;
		if (getNode() == null) return Paths.get( getHelpImageFileRootName() + "." + getHelpFormat() ); //$NON-NLS-1$
		
		Path cmroot = getNode().getModel().getFileDataStoreLocation();
		if (cmroot == null) return null;
		
		Path p = cmroot.resolve(getHelpImageFileRootName() + "." + getHelpFormat()); //$NON-NLS-1$
		if (!Files.exists(p)) return null;
		return p;
	}
	
	@Transient
	public Path getImportHelpFile() {
		return this.importHelpFile;
	}
	@Transient
	public void setImportHelpFile(Path importHelpFile) {
		if (importHelpFile != null) {
			getCmAttributeOptions().remove(CmAttributeOption.ID_HELP_IMAGE_FORMAT);
			setHelpFormat(SharedUtils.getFilenameExtension(importHelpFile.getFileName().toString()));
		}
		this.importHelpFile = importHelpFile;
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
		return attribute.getType().isList() ? getConfig().getList() : Collections.emptyList();
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
	
	/**
	 * 
	 * @return the extension value of the file name representing the image
	 */
	@Transient
	public String getImageFileExtension() {
		CmAttributeOption option = getCmAttributeOptions().get(CmAttributeOption.ID_IMAGE_EXT);
		if (option == null) return null;
		return option.getStringValue();
	}
	
	@Transient
	@Override
	public boolean hasCustomImage() {
		if (getImageFileExtension() == null || getImageFileExtension().isEmpty()) return false;
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
	public void setImageFile(Path file) {
		this.imageFile = file;
		//TODO: figure out how to delete old files if extension has change - if
		//extension is the same then the file will be overwritten and this
		//is not a problem
		if (file == null) {
			getCmAttributeOptions().put(CmAttributeOption.ID_IMAGE_EXT, null);
		}else {
			String fileName = file.getFileName().toString();
			int index = fileName.lastIndexOf('.');
			if (index >= 0) {
				CmAttributeOption op = getCmAttributeOptions().get(CmAttributeOption.ID_IMAGE_EXT);
				if (op == null) {
					op = new CmAttributeOption();
					op.setOptionId(CmAttributeOption.ID_IMAGE_EXT);
					op.setCmAttribute(this);
					getCmAttributeOptions().put(CmAttributeOption.ID_IMAGE_EXT, op);
				}	
				op.setStringValue(fileName.substring(index+1));
			}else {
				getCmAttributeOptions().remove(CmAttributeOption.ID_IMAGE_EXT);
			}
		}
	}
	
	@Override
	@Transient
	public Path getImagePersistenceLocation() {
		Path cmroot = getNode().getModel().getFileDataStoreLocation();
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
		if (getImageFileExtension() == null) {
			sb.append(".jpg"); // for backwards compatibility; prior to 6.1 it was assumed the image format was jpg //$NON-NLS-1$
		}else if (getImageFileExtension().isEmpty()) {
			//image was cleared
		}else {
			sb.append("."); //$NON-NLS-1$
			sb.append(getImageFileExtension());
		}
		return sb.toString();
	}
	
	@Override
	public void resetImageFile() {
		this.imageFile = null;
	}

	/**
	 * The root filename for the help image associated 
	 * with this node.  This will be cm_help_<uuid>.  The image
	 * extension is stored as an attribute see getHelpImageFormat.
	 * @return
	 */
	@Transient
	public String getHelpImageFileRootName() {
		return "cm_help_" + UuidUtils.uuidToString(getUuid()); //$NON-NLS-1$
	}
	
	@Transient
	public boolean isGrouped() {
		if (getCmAttributeOptions().get(CmAttributeOption.ID_INPUTGROUP) == null) return false;
		return getCmAttributeOptions().get(CmAttributeOption.ID_INPUTGROUP).getBooleanValue();
	}
	
	@Transient
	public void setGrouped(boolean isgrouped) {
		if (getCmAttributeOptions().get(CmAttributeOption.ID_INPUTGROUP) == null) {
			if (!isgrouped)  return;
			CmAttributeOption op = new CmAttributeOption();
			op.setOptionId(CmAttributeOption.ID_INPUTGROUP);
			op.setCmAttribute(this);
			getCmAttributeOptions().put(CmAttributeOption.ID_INPUTGROUP, op);
		}
		getCmAttributeOptions().get(CmAttributeOption.ID_INPUTGROUP).setBooleanValue(isgrouped);
	}
	
	
	/**
	 * 
	 * @return the names associated with the list element in the
	 * language the platform is running in.
	 */
	@Override
	@Type(type="org.wcs.smart.ca.LabelUserType")
	@Column(name="uuid", insertable=false, updatable=false)
	public String getName() {
		String n = super.getName();
		if (n == null || n.length() == 0){
			if (getAttribute() == null) return null;
			return getAttribute().getName();
		}
		return n;
	}
	
	public String findDisplayName(Language language, Language defaultl) {
		//search for custom translations
		String l = super.findNameNull(language);
		if (l != null) return l;
		
		if (getAttribute() != null) l = getAttribute().findNameNull(language);
		if (l != null) return l;
		
		l = super.findNameNull(defaultl);
		if (l != null) return l;
		
		if (getAttribute() != null) return getAttribute().findName(defaultl);
		
		return "";	
	}
}

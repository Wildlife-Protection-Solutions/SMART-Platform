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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.util.UuidUtils;

/**
 * List Attributes items configuration data.
 * 
 * @author elitvin
 * @since 2.0.0
 */
@Entity
@Table(name = "smart.cm_attribute_list")
public class CmAttributeListItem extends NamedItem implements IImageAssociatedObject {

	private static final long serialVersionUID = 1L;
	
	private boolean isActive;
	
	private AttributeListItem listItem;
	
	private CmAttributeConfig config;
	private int listOrder;
	private Path imageFile;
	private String extension; //image name extension
	
	@Column(name="is_active")
	public boolean getIsActive() {
		return isActive;
	}
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="list_element_uuid", referencedColumnName="uuid")
	public AttributeListItem getListItem() {
		return listItem;
	}
	public void setListItem(AttributeListItem listItem) {
		this.listItem = listItem;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="config_uuid", referencedColumnName="uuid")
	public CmAttributeConfig getConfig() {
		return config;
	}
	public void setConfig(CmAttributeConfig config) {
		this.config = config;
	}
	
	@Column(name="list_order")
	public int getListOrder() {
		return listOrder;
	}
	public void setListOrder(int listOrder) {
		this.listOrder = listOrder;
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
		sb.append("li_img1_"); //$NON-NLS-1$
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
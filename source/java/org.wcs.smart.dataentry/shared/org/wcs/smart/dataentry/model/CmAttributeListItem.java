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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.NamedItem;
import org.wcs.smart.ca.datamodel.Attribute;
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

	private ConfigurableModel configurableModel;
	private boolean isActive;
	
	private AttributeListItem listItem;
	
	private CmAttribute attribute;
	private Attribute dmAttribute;
	private int listOrder;
	private File imageFile;

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
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="list_element_uuid", referencedColumnName="uuid")
	public AttributeListItem getListItem() {
		return listItem;
	}
	public void setListItem(AttributeListItem listItem) {
		this.listItem = listItem;
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
	
	@Column(name="list_order")
	public int getListOrder() {
		return listOrder;
	}
	public void setListOrder(int listOrder) {
		this.listOrder = listOrder;
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
		return getConfigurableModel().getFileDataStoreLocation() + File.separator + "li_img1_" + UuidUtils.getDirectoryPath(getUuid()); //$NON-NLS-1$
	}
}
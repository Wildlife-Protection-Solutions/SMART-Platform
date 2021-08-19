/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.smartcollect.model;

import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICmProvider;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldUuidValue;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * SMART Collect package 
 * 
 * @author Emily
 *
 */
@Entity
@Table(name="smart.smartcollect_package")
public class SmartCollectPackage extends AbstractCtPackage implements ICmProvider {

	private static final long serialVersionUID = 1L;
	
	public static final String PACKAGE_TYPENAME = "SMARTCOLLECT"; //$NON-NLS-1$
	
	//configurable model
	private ConfigurableModel cm;
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="cm_uuid", referencedColumnName="uuid")
	public ConfigurableModel getConfigurableModel() {
		return this.cm;
	}
	public void setConfigurableModel(ConfigurableModel cm) {
		this.cm = cm;
	}
	
	@Transient
	public boolean isDataModel() {
		if (getConfigurableModel() == null) return true;
		return false;
	}
	
	@Transient
	public String getTypeIdentifier() {
		return PACKAGE_TYPENAME;
	}
	
	@Transient
	@Override
	public ICtPackage copy() {
		SmartCollectPackage copy = new SmartCollectPackage();
		copy.ca = this.ca;
		copy.cm = this.cm;
		copy.ctprofile = this.ctprofile;
		copy.name = name;
		copy.basemapdef = this.basemapdef;
		
		if (getMetadataValues() != null) {
			copy.metadataValues = new ArrayList<>();
			for (MetadataFieldValue v : getMetadataValues()) {
				MetadataFieldValue mcopy = new MetadataFieldValue();
				mcopy.setBooleanValue(v.getBooleanValue());
				mcopy.setConservationArea(v.getConservationArea());
				mcopy.setCtPackage(copy);
				mcopy.setMetadataKey(v.getMetadataKey());
				mcopy.setStringValue(v.getStringValue());
				mcopy.setUuidValue(v.getUuidValue());
				mcopy.setVisible(v.isVisible());
				
				if (v.getUuidList() != null) {
					mcopy.setUuidList(new ArrayList<>());
					for (MetadataFieldUuidValue uuidValue : v.getUuidList()) {
						MetadataFieldUuidValue uuidcopy = new MetadataFieldUuidValue();
						uuidcopy.setMetadata(mcopy);
						uuidcopy.setUuidValue(uuidValue.getUuidValue());
						mcopy.getUuidList().add(uuidcopy);
					}
				}
				copy.getMetadataValues().add(mcopy);
			}
		}
		return copy;
	}
	
	
	@Transient
	public static String generateSmartMobileAppLink(URL url, UUID ctPackageUuid) {
		StringBuilder sb = new StringBuilder();
		sb.append("http://clientapp.cybertracker.org/install/"); //$NON-NLS-1$
		sb.append(url.getProtocol());
		sb.append("/"); //$NON-NLS-1$
		sb.append(url.getHost());
		if (url.getPort() != -1) {
			sb.append(":"); //$NON-NLS-1$
			sb.append(url.getPort());
		}
		sb.append(url.getPath().replaceAll("/", "_")); //$NON-NLS-1$ //$NON-NLS-2$
		sb.append("_noa_smartcollect_packages_"); //$NON-NLS-1$
		if (ctPackageUuid != null) sb.append(ctPackageUuid.toString());
		return sb.toString();
	}
	
}

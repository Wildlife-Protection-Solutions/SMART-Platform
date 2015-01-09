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
package org.wcs.smart.intelligence.model;

import java.io.File;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.informant.PersistentManager;
import org.wcs.smart.intelligence.informant.aes.EncryptedData;
import org.wcs.smart.intelligence.informant.aes.InformantAesManager;
import org.wcs.smart.util.SmartUtils;

/**
 * @author elitvin
 * @since 3.2.0
 */
@Entity
@Table(name = "smart.informant")
public final class Informant extends UuidItem {
	
	private static final String DIR_NAME = "aes";

    private ConservationArea conservationArea;
	public boolean isActive;
    private String id;
    
    private EncryptedData encryptedData;
    
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="ca_uuid", referencedColumnName="uuid")
	public ConservationArea getConservationArea() {
		return conservationArea;
	}
	public void setConservationArea(ConservationArea conservationArea) {
		this.conservationArea = conservationArea;
	}
	
	@Column(name="is_active")
	public boolean getIsActive() {
		return this.isActive;
	}
	public void setIsActive(boolean isActive) {
		this.isActive = isActive;
	}

	@Column(name="id")
    public String getId() {
		return id;
	}
    public void setId(String id) {
		this.id = id;
	}
	
    @Transient
    public EncryptedData getEncryptedData() {
    	if (encryptedData == null) {
    		File file = getDataFile();
			encryptedData = new EncryptedData();
    		if (file != null && file.exists()) {
    			Object obj = PersistentManager.fromFile(file);
    			if (obj instanceof EncryptedData) {
    				encryptedData = (EncryptedData) obj;
    			}
    		}
    	}
		return encryptedData;
	}
    @Transient
    public void setEncryptedData(EncryptedData encryptedData) {
		this.encryptedData = encryptedData;
	}
    
    @Transient
    public final Object get(InformantDataKey key) {
    	return InformantAesManager.getInstance().get(this, key);
    }
    @Transient
    public final void set(InformantDataKey key, Object value) {
    	InformantAesManager.getInstance().set(this, key, value);
    }

    @Transient
	public final File getDataFile() {
		if (getUuid() != null) {
    		String fn = SmartUtils.encodeHex(getUuid());
    		File file = new File(getDatastoreFolderPath() + File.separator + fn + ".dat"); //$NON-NLS-1$
			return file;
		} else {
			return null;
		}
	}
	
    @Transient
	public final String getDatastoreFolderPath() {
		return SmartDB.getCurrentConservationArea().getFileDataStoreLocation() + File.separator
				+ IntelligencePlugIn.INTELLIGENCE_DIR + File.separator + DIR_NAME;
	}
    
}

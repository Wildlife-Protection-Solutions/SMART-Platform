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
package org.wcs.smart.ca.icon;

import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.common.attachment.ISmartAttachment;
import org.wcs.smart.util.SharedUtils;
import org.wcs.smart.util.UuidUtils;

/**
 * Represents an icon implementation for a given icon and icon set
 * @author Emily
 *
 */
@Entity
@Table(name="smart.iconfile")
public class IconFile extends ISmartAttachment{

	private static final long serialVersionUID = 1L;
	
	private static final String PLATFORM_IMAGE = "platform:/plugin"; //$NON-NLS-1$

	public static final String ICON_DIR = "icons"; //$NON-NLS-1$
	
	private IconSet iconSet;
	private Icon icon;
	

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="iconset_uuid", referencedColumnName="uuid")
	public IconSet getIconSet() {
		return this.iconSet;
	}
	
	public void setIconSet(IconSet iconSet){
		this.iconSet = iconSet;
	}
	
	
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="icon_uuid", referencedColumnName="uuid")
	public Icon getIcon() {
		return this.icon;
	}
	
	public void setIcon(Icon icon){
		this.icon = icon;
	}
	
	/**
	 * Getter for file name
	 * 
	 * @return
	 */
	@Column(name="filename")
	@Override
	public String getFilename() {
		return super.getFilename();
	}

	/**
	 * Setter for file name
	 * 
	 * @param filename
	 */
	@Override
	public void setFilename(String filename) {
		super.setFilename(filename);
	}

	@Transient
	@Override
	protected String getDatastoreFolderPath(Session session) throws Exception {
		Path p = Paths.get(getConservationArea().getFileDataStoreLocation()).resolve(ICON_DIR);
		return p.toString();
	}

	@Transient
	@Override
	public ConservationArea getConservationArea() {
		return icon.getConservationArea();
	}
	
	@Transient
	public boolean isEncrypted() {
		return false;
	}
	
	@Transient
	@Override
	public void computeFileLocation(Session session) {
		if (attachmentFile != null) return;
		if (isSystemIcon()) return;
		attachmentFile = Paths.get(getConservationArea().getFileDataStoreLocation()).resolve(ICON_DIR).resolve(getFilename());
	}
	
	@Transient
	public boolean isSystemIcon() {
		if (getCopyFromLocation() != null) return false;
		return getFilename().startsWith(PLATFORM_IMAGE);
	}
	
	@Transient
	@Override
	public Path getAttachmentFile(){
		if (isSystemIcon()) {
			try {
				//extract to a local temp directory
				URL url = new URL(getFilename());
				Path temp = null;
				String ext = SharedUtils.getFilenameExtension(getFilename());
				if (getUuid() != null) {
					temp = SmartContext.INSTANCE.getTempFilestoreLocation().resolve(ICON_DIR).resolve(UuidUtils.uuidToString(getUuid()) + "." + ext); //$NON-NLS-1$
					if (Files.exists(temp)) return temp;
					if (!Files.exists(temp.getParent())) Files.createDirectories(temp.getParent());
				}else {
					temp = Files.createTempFile("smart", "." + ext); //$NON-NLS-1$ //$NON-NLS-2$
				}
				temp.toFile().deleteOnExit();
				
			
				try(InputStream inputStream = url.openConnection().getInputStream()){
					Files.copy(inputStream, temp, StandardCopyOption.REPLACE_EXISTING);
					return temp;
				}
				
			} catch (Exception e) {
				throw new IllegalStateException("Invalid URL.", e); //$NON-NLS-1$
			}
		}
		if (attachmentFile == null){
			throw new IllegalStateException("Attachment file not set.  You must first call computeFileLocaion."); //$NON-NLS-1$
		}
		return attachmentFile;
	}
	
}

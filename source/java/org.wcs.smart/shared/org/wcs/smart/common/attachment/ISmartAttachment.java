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
package org.wcs.smart.common.attachment;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.UuidItem;

/**
 * Interface that represents attachments.
 * Classes that implement this interface will also be handled by {@link AttachmentInterceptor}
 * if one is attached to hibernate session in order to save related files
 * is datastore.
 * 
 * @author elitvin
 * @since 1.0.0
 */
@MappedSuperclass
public abstract class ISmartAttachment extends UuidItem{
	
	private static final long serialVersionUID = 1L;
	
	private String filename;
	
	@Transient
	protected Path copyFromLocation;
	@Transient
	protected Path attachmentFile;
	
	/**
	 * Getter for file name
	 * 
	 * @return
	 */
	@Column(name="filename")
	public String getFilename() {
		return this.filename;
	}

	/**
	 * Setter for file name
	 * 
	 * @param filename
	 */
	public void setFilename(String filename) {
		this.filename = filename;
	}
	
	/**
	 * Location of the file to copy.  Temporarily set until
	 * saved.  Will return null if file already in datastore.
	 * @return File
	 */
	@Transient
	public Path getCopyFromLocation() {
		return copyFromLocation;
	}

	/**
	 * Location to copy files from.  Temporarily set
	 * for newly added attachments until saved. 
	 */
	public void setCopyFromLocation(Path newFile) {
		this.copyFromLocation = newFile;
	}

	/**
	 * Returns the full file
	 * 
	 * @return File
	 */
	@Transient
	public void computeFileLocation(Session session) throws Exception{
		if (attachmentFile != null) return;
		attachmentFile = Paths.get(getDatastoreFolderPath(session), getFilename()); 
	}
	/**
	 * To be used with care.  This should only be used when it is not possible to
	 * compute the location from the database. (When saving an object where the 
	 * location depends on a parent object that is not in the database yet but will
	 * be as a part of the transaction).
	 * @param location
	 * @throws Exception
	 */
	@Transient
	public void computeFileLocation(Path location) {
		this.attachmentFile = location; 
	}
	
	@Transient
	public Path getAttachmentFile(){
		if (attachmentFile == null){
			throw new IllegalStateException("Attachment file not set.  You must first call computeFileLocaion."); //$NON-NLS-1$
		}
		return attachmentFile;
	}
	
	/**
	 * Getter for datastore folder path
	 * 
	 * @return path to folder where file is saved
	 */
	@Transient
	protected abstract String getDatastoreFolderPath(Session session) throws Exception;

	/**
	 * The conservation area associated with the attachment
	 * @param session
	 * @return
	 * @throws Exception
	 */
	@Transient
	public abstract ConservationArea getConservationArea();
	
	/**
	 * If this attachment is encrypted.  By default this is true but for a few small
	 * cases this might not be true.
	 * 
	 * @return true
	 */
	@Transient
	public boolean isEncrypted() {
		return true;
	}
}

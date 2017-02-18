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

import java.io.File;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Transient;

import org.hibernate.Session;
import org.wcs.smart.ca.UuidItem;

/**
 * Interface that represents attachments used within {@link AttachmentComposite}.
 * Classes that implement this interface will also be handled by {@link AttachmentInterceptor}
 * if one is attached to hibernate session in order to save related files
 * is datastore.
 * 
 * @author elitvin
 * @since 1.0.0
 */
@Entity
@Inheritance(strategy= InheritanceType.TABLE_PER_CLASS)
public abstract class ISmartAttachment extends UuidItem{
	
	private String filename;
	
	@Transient
	protected File copyFromLocation;
	@Transient
	protected File attachmentFile;
	
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
	public File getCopyFromLocation() {
		return copyFromLocation;
	}

	/**
	 * Location to copy files from.  Temporarily set
	 * for newly added attachments until saved. 
	 */
	public void setCopyFromLocation(File newFile) {
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
		attachmentFile = new File(getDatastoreFolderPath(session), getFilename()); 
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
	public void computeFileLocation(File location) {
		this.attachmentFile = location; 
	}
	
	@Transient
	public File getAttachmentFile(){
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
}

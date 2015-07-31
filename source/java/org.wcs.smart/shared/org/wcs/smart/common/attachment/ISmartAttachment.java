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

/**
 * Interface that represents attachments used within {@link AttachmentComposite}.
 * Classes that implement this interface will also be handled by {@link AttachmentInterceptor}
 * if one is attached to hibernate session in order to save related files
 * is datastore.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public interface ISmartAttachment {
	
	/**
	 * Getter for file name
	 * 
	 * @return
	 */
	public String getFilename();
	
	/**
	 * Setter for file name
	 * 
	 * @param filename
	 */
	public void setFilename(String filename);

	/**
	 * Location of the file to copy.  Temporarily set until
	 * saved.  Will return null if file already in datastore.
	 * @return File
	 */
	public File getCopyFromLocation();
	
	/**
	 * Location to copy files from.  Temporarily set
	 * for newly added attachments until saved. 
	 */
	public void setCopyFromLocation(File newFile);
	
	/**
	 * Returns the full file
	 * 
	 * @return File
	 */
	public File getFullFile() throws Exception;
	
	/**
	 * Getter for datastore folder path
	 * 
	 * @return path to folder where file is saved
	 */
	public String getDatastoreFolderPath() throws Exception;
}

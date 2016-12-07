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
import java.util.UUID;

/**
 * Interface indicates that an object has an image associated with it.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public interface IImageAssociatedObject {
	
	public static final File NULL_FILE = new File("NO-SUCH-FILE"); //$NON-NLS-1$
	
	/**
	 * Item UUID
	 * @return
	 */
	public UUID getUuid();
	
	/**
	 * The image file name.  If setImageFile is called this will return
	 * the file set; otherwise it will return the file described 
	 * in getImagePersistenceLocation.  setImageFile(null) will cause this to return
	 * the file described in getImagePersistenceLocation();
	 * 
	 * @return
	 */
	public File getImageFile();
	
	/**
	 * Sets the image file.  This is the location to copy the image from.
	 * 
	 * @param file
	 */
	public void setImageFile(File file);
	
	/**
	 * This is the location where the image file should be saved to.
	 * 
	 * @return
	 */
	public String getImagePersistenceLocation();
	
}

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
package org.wcs.smart.intelligence.ui.editor;

import java.util.Arrays;
import java.util.Date;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

/**
 * Intelligence EditorInput
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class IntelligenceEditorInput implements IEditorInput {

	private byte[] uuid;
	private String shortName;
	private Date receivedDate;
	
	/**
	 * Constructor
	 */
	public IntelligenceEditorInput(byte[] uuid, String shortName, Date receivedDate) {
		this.uuid = uuid;
		this.shortName = shortName;
		this.receivedDate = receivedDate;
	}

	/**
	 * @return uuid
	 */
	public byte[] getUuid(){
		return this.uuid;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		if (shortName == null) return ""; //$NON-NLS-1$
		return shortName;
	}

	public Date getReceivedDate() {
		return receivedDate;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		return shortName;
	}
	
	@Override
	public int hashCode() {
		if (uuid != null) {
			return Arrays.hashCode(uuid);
		}
		return super.hashCode();
	}
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof IntelligenceEditorInput) {
			IntelligenceEditorInput i = (IntelligenceEditorInput) other;
			if (i.getUuid() == null && this.getUuid() == null) {
				return super.equals(i);
			} else if (i.getUuid() != null && this.getUuid() != null) {
				return Arrays.equals(i.getUuid(), this.getUuid());
			}
		}
		return false;
	}

}

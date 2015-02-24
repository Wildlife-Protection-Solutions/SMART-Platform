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
package org.wcs.smart.patrol.ui;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.patrol.PatrolUtils;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.PatrolType;

/**
 * The patrol editor input.
 * <p>The patrol input consists of the patrol uuid, id, type 
 * and start date & end date.</p>
 * 
 * @author Emily
 * @since 1.0.0
 */
/**
 * @author Emily
 *
 */
public class PatrolEditorInput implements IEditorInput {

	private byte[] patrolUuid;
	private String id;
	private PatrolType.Type type;
	private Date startDate;
	private Date endDate;
	
	/**
	 * Creates a new input from the uuid only.
	 * @param uuid
	 */
	public PatrolEditorInput(byte[] uuid){
		this.patrolUuid = uuid;
	}
	
	/**
	 * Create new input.
	 * 
	 * @param uuid
	 * @param id
	 * @param type
	 * @param startDate
	 */
	public PatrolEditorInput(byte[] uuid, String id, PatrolType.Type type, Date startDate, Date endDate){
		this.patrolUuid = uuid;
		this.id = id;
		this.type = type;
		this.startDate = startDate;
		this.endDate = endDate;
	}
	
	/**
	 * @param id
	 */
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * @return start date
	 */ 
	public Date getStartDate(){
		return startDate;
	}
	
	/**
	 * @return start date
	 */ 
	public Date getEndDate(){
		return endDate;
	}
	
	/**
	 * @return patrol uuid
	 */
	public byte[] getUuid(){
		return this.patrolUuid;
	}
	
	/**
	 * @return patrol id
	 */
	public String getPatrolId(){
		return this.id;
	}
	
	/**
	 * @return patrol type
	 */
	public PatrolType.Type getType(){
		return this.type;
	}
	
	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		return false;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {		
		return PatrolUtils.getImageDescriptor(this.type);
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {	
		return Messages.PatrolEditorInput_EditorNamePrefix + "_" + id; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		return MessageFormat.format(Messages.PatrolEditorInput_Editor_Tooltip, new Object[]{ id});
	}

	public int hashCode() {
		return Arrays.hashCode(patrolUuid);
	}

	public boolean equals(Object obj) {
		if (obj != null && obj instanceof PatrolEditorInput){
			return Arrays.equals(this.patrolUuid, ((PatrolEditorInput)obj).patrolUuid);
		}
		return false;
	}
}

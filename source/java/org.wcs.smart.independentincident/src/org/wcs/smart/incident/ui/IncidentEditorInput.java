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
package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.util.Date;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.internal.Messages;

/**
 * Editor input for incident objects.  Tracks
 * the uuid, id and datetime.
 * 
 * @author Emily
 *
 */
public class IncidentEditorInput implements IEditorInput {

	private int id;
	private UUID uuid;
	private Date dateTime;
	
	public IncidentEditorInput(UUID uuid){
		this.uuid = uuid;
	}
	
	public IncidentEditorInput(UUID uuid, int id, Date dateTime){
		this.id = id;
		this.uuid = uuid;
		this.dateTime = dateTime;
	}
	
	/**
	 * update id value
	 * @param id
	 */
	public void setId(int id){
		this.id = id;
	}
	
	/**
	 * update date time value
	 * @param dateTime
	 */
	public void setDateTime(Date dateTime){
		this.dateTime = dateTime;
	}
	/**
	 * 
	 * @return datetime of incident
	 */
	public Date getDateTime(){
		return this.dateTime;
	}
	/**
	 * @return the incident uuid
	 */
	public UUID getUuid(){
		return this.uuid;
	}
	
	/**
	 * @return the incident id
	 */
	public int getId(){
		return this.id;
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

	@Override
	public ImageDescriptor getImageDescriptor() {
		return IncidentPlugIn.getDefault().getImageRegistry().getDescriptor(IncidentPlugIn.INCIDENT_ICON);
	}

	/**
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		return MessageFormat.format(Messages.IncidentEditorInput_EditorName, new Object[]{String.valueOf(id)});
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
		return MessageFormat.format(Messages.IncidentEditorInput_EditorTooltip, new Object[]{ id});
	}

	public int hashCode() {
		return uuid.hashCode();
	}

	public boolean equals(Object obj) {
		if (obj != null && obj instanceof IncidentEditorInput){
			return this.uuid.equals(((IncidentEditorInput)obj).uuid);
		}
		return false;
	}
}

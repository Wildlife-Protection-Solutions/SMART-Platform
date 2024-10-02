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
import java.time.LocalDateTime;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.incident.IncidentManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.incident.model.IncidentType;

/**
 * Editor input for incident objects.  Tracks
 * the uuid, id and datetime.
 * 
 * @author Emily
 *
 */
public class IncidentEditorInput implements IEditorInput {

	private String id;
	private UUID uuid;
	private LocalDateTime dateTime;
	private String sourceKey;
	private IncidentType type;
	
	private IncidentEditorInput(UUID uuid, String sourceKey, IncidentType type){
		this.uuid = uuid;
		this.sourceKey = sourceKey;
		this.type = type;
	}
	
	public IncidentEditorInput(UUID uuid, String id, LocalDateTime dateTime, String sourceKey, IncidentType type){
		this(uuid, sourceKey, type);
		this.id = id;
		this.dateTime = dateTime;
	}
	
	public IncidentType getType() {
		return this.type;
	}
	
	/**
	 * @return the incident source key
	 */
	public String getSourceKey() {
		return this.sourceKey;
	}
	
	/**
	 * update id value
	 * @param id
	 */
	public void setId(String id){
		this.id = id;
	}
	
	/**
	 * update date time value
	 * @param dateTime
	 */
	public void setDateTime(LocalDateTime dateTime){
		this.dateTime = dateTime;
	}
	/**
	 * 
	 * @return datetime of incident
	 */
	public LocalDateTime getDateTime(){
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
	public String getId(){
		return this.id;
	}
	
	/**
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
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
		return null;
	}

	public Image getImage() {
		return IncidentManager.getInstance().getIncidentProvider(sourceKey).getImage();
	}
	/**
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		return MessageFormat.format("{0} {1}", IncidentManager.getInstance().getIncidentProvider(sourceKey).getName(), String.valueOf(id)); //$NON-NLS-1$
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

package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.incident.IncidentPlugIn;

public class IncidentEditorInput implements IEditorInput {

	private int id;
	private byte[] uuid;
	private Date dateTime;
	
	public IncidentEditorInput(byte[] uuid, int id, Date dateTime){
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
	public byte[] getUuid(){
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
		return MessageFormat.format("Independent Incident {0}", new Object[]{String.valueOf(id)});
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
		return MessageFormat.format("edit incident data for id {0}", new Object[]{ id});
	}

	public int hashCode() {
		return Arrays.hashCode(uuid);
	}

	public boolean equals(Object obj) {
		if (obj != null && obj instanceof IncidentEditorInput){
			return Arrays.equals(this.uuid, ((IncidentEditorInput)obj).uuid);
		}
		return false;
	}
}

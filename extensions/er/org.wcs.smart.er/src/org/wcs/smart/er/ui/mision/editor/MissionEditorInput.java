package org.wcs.smart.er.ui.mision.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.er.EcologicalRecordsPlugIn;

public class MissionEditorInput implements IEditorInput {

	private byte[] uuid;
	private String name;
	
	/**
	 * Constructor
	 */
	public MissionEditorInput(String name, byte[] uuid) {
		this.uuid = uuid;
		this.name = name;
	}
	
	public byte[] getUuid() {
		return uuid;
	}
	
	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter) {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return EcologicalRecordsPlugIn.getDefault().getImageRegistry().getDescriptor(EcologicalRecordsPlugIn.MISSION_ICON);
	}

	@Override
	public String getName() {
		return this.name; 
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return ""; //$NON-NLS-1$
	}

}

package org.wcs.smart.entity.ui.typelist.editor;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.entity.EntityPlugIn;

public class EntityTypeEditorInput implements IEditorInput {

	private byte[] uuid;
	private String id;
	private String name;
	
	
	public EntityTypeEditorInput(byte[] entityType, String id, String name){
		this.uuid = entityType;
		this.id = id;
		this.name = name;
	}
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
		// TODO Auto-generated method stub
		return EntityPlugIn.getDefault().getImageRegistry().getDescriptor(EntityPlugIn.ENTITY_TYPE_ICON);
	}

	@Override
	public String getName() {
		return name + " [" + id + "]";
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return "Entity types configure the information captured about entities.";
	}

}

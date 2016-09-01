package org.wcs.smart.i2.ui.editors;

import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class EntityEditorInput implements IEditorInput{

	private String name;
	private UUID uuid;
	
	public EntityEditorInput(String name, UUID uuid){
		this.name = name;
		this.uuid = uuid;
	}
	
	public UUID getUuid(){
		return this.uuid;
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
		return null;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return null;
	}

}

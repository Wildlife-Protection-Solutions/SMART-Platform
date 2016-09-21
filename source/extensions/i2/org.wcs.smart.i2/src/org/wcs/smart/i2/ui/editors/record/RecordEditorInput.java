package org.wcs.smart.i2.ui.editors.record;

import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.locationtech.udig.ui.graphics.AWTSWTImageUtils;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.model.IntelRecord;

public class RecordEditorInput implements IEditorInput{

	private String name;
	private UUID uuid;
	
	private IntelRecord record;
	
	public RecordEditorInput(String name, UUID uuid){
		this.name = name;
		this.uuid = uuid;
	}
	
	public RecordEditorInput(IntelRecord record){
		this.record = record;
		this.name = record.getTitle();
	}
	
	public IntelRecord getRecord(){
		return this.record;
	}
	
	public UUID getUuid(){
		if (record != null){
			return record.getUuid();
		}
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
		//TODO: record icon
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
	
	@Override
	public boolean equals(Object other){
		if (other != null && other instanceof RecordEditorInput){
			if (record != null) return record.equals(((RecordEditorInput)other).record);
			return uuid.equals(((RecordEditorInput)other).uuid);
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return uuid.hashCode();
	}

}

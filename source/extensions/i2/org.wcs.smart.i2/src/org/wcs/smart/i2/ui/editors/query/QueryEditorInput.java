package org.wcs.smart.i2.ui.editors.query;

import java.util.Date;
import java.util.Objects;
import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordQuery;
import org.wcs.smart.i2.ui.editors.record.RecordEditorInput;

public class QueryEditorInput implements IEditorInput{

	private String name;
	private UUID uuid;
	
	private boolean isNew;
	
	public QueryEditorInput(String name, UUID uuid){
		this.name = name;
		this.uuid = uuid;
		if (this.uuid == null){
			isNew = true;
			//generate temporary random uuid
			this.uuid = UUID.randomUUID();
		}
	}
	
	public QueryEditorInput(IntelRecordQuery record){
		this(record.getName(), record.getUuid());
	}
	
	public boolean isNew(){
		return this.isNew;
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
	
	@Override
	public boolean equals(Object other){
		if (this == other) return true;
		if (other == null) return false;
		if (getClass() != other.getClass()) return false;
		QueryEditorInput ie = (QueryEditorInput) other;
		if (getUuid() != null){
			return Objects.equals(getUuid(), ie.getUuid());
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		if (uuid != null) return uuid.hashCode();
		return super.hashCode();
	}

}
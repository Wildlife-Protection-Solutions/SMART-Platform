package org.wcs.smart.query.ui;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class QueryResultsInput implements IEditorInput {

	private byte[] uuid = null;
	
	public QueryResultsInput(){
		
	}
	public QueryResultsInput(byte[] uuid){
		this.uuid = uuid;
	}
	
	public byte[] getUuid(){
		return this.uuid;
	}
	@Override
	public Object getAdapter(Class adapter) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean exists() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getName() {
		return "Smart Query Editor";
	}

	@Override
	public IPersistableElement getPersistable() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getToolTipText() {
		return "Smart Query Editor.";
	}

}
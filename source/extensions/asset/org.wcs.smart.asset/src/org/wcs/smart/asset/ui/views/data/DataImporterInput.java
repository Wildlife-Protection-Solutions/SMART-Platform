package org.wcs.smart.asset.ui.views.data;

import java.nio.file.Path;
import java.util.List;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

public class DataImporterInput implements IEditorInput{

	public DataImporterInput() {
	}
	
	@Override
	public <T> T getAdapter(Class<T> adapter) {
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
		return "Import Asset Files";
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
	public boolean equals(Object other) {
		if (other.getClass() == getClass()) return true;
		return false;
	}

}

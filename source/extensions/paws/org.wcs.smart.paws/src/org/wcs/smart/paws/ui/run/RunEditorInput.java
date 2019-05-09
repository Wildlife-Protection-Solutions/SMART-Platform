package org.wcs.smart.paws.ui.run;

import java.util.UUID;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.wcs.smart.paws.model.PawsConfiguration;
import org.wcs.smart.paws.model.PawsRun;
import org.wcs.smart.paws.ui.config.ConfigEditorInput;

public class RunEditorInput implements IEditorInput {

	private UUID uuid;
	private String label;

	/**
	 * Constructor
	 */
	public RunEditorInput(UUID uuid, String label) {
		this.uuid = uuid;
		this.label = label;
	}
	
	public RunEditorInput(PawsRun pw) {
		this(pw.getUuid(), pw.getId());
	}

	/**
	 * @return uuid
	 */
	public UUID getUuid(){
		return this.uuid;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IAdaptable#getAdapter(java.lang.Class)
	 */
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#exists()
	 */
	@Override
	public boolean exists() {
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getImageDescriptor()
	 */
	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getName()
	 */
	@Override
	public String getName() {
		if (label == null) return ""; //$NON-NLS-1$
		return label;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getPersistable()
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.IEditorInput#getToolTipText()
	 */
	@Override
	public String getToolTipText() {
		return label;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uuid.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		if (uuid == null || ((RunEditorInput)obj).uuid == null)
			return false;
		return uuid.equals( ((RunEditorInput)obj).uuid);
	}
}

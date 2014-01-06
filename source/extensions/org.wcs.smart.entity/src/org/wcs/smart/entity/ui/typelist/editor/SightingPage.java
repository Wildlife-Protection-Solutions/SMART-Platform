package org.wcs.smart.entity.ui.typelist.editor;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.EditorPart;
import org.hibernate.Session;

public class SightingPage extends EditorPart implements IEntityTypeEditorPage {

	@Override
	public void updatePage(Session currentSession, boolean typeModified) {

	}

	@Override
	public void doSave(IProgressMonitor monitor) {
	
	}

	@Override
	public void doSaveAs() {

	}

	@Override
	public void init(IEditorSite site, IEditorInput input)
			throws PartInitException {

	}

	@Override
	public boolean isDirty() {
		return false;
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}

	@Override
	public void createPartControl(Composite parent) {
		
	}

	@Override
	public void setFocus() {
		
	}

}

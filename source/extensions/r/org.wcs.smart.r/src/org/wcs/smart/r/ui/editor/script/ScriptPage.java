package org.wcs.smart.r.ui.editor.script;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;

public class ScriptPage extends EditorPart {

	private RScriptEditor parent;
	
	private Text txtScript;
	private FormToolkit toolkit;

	
	public ScriptPage(RScriptEditor parent) {
		this.parent = parent;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		toolkit.dispose();
	}
	
	@Override
	public void doSave(IProgressMonitor monitor) {}

	@Override
	public void doSaveAs() {}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
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
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		txtScript = new Text(main, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		txtScript.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
	}

	@Override
	public void setFocus() {
		txtScript.setFocus();
	}
	
	public void update() {
		String fileName = parent.getScript().getFilename();
		txtScript.setText("TODO: show contents for " + fileName + " here. ");
	}

}

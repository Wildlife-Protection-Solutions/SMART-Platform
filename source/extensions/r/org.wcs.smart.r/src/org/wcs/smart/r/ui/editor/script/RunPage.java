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
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.r.model.RScript;
import org.wcs.smart.r.model.RScriptParameter;

public class RunPage extends EditorPart {

	private FormToolkit toolkit;
	
	private Form mainForm; 
	private Text txtParameters;
	
	private RScriptEditor parent;
	
	public RunPage(RScriptEditor parent) {
		this.parent = parent;
	}
	
	@Override
	public void dispose() {
		super.dispose();
		toolkit.dispose();
	}
	@Override
	public void doSave(IProgressMonitor monitor) {
	}

	@Override
	public void doSaveAs() {
	}

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
		toolkit = new FormToolkit(parent.getDisplay());
		
		mainForm = toolkit.createForm(parent);
		mainForm.getBody().setLayout(new GridLayout());
		mainForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite main = toolkit.createComposite(mainForm.getBody(), SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.createLabel(main, "R Script Parameter:");
		txtParameters = toolkit.createText(main, "", SWT.V_SCROLL | SWT.WRAP);
	}

	public void update() {
		RScript script = parent.getScript();
		mainForm.setText(script.getName());
		boolean found = false;
		if (script.getParameters() != null) {
			for (RScriptParameter p : script.getParameters()) {
				if (p.getKey().equalsIgnoreCase(RScriptParameter.R_PARAMETER)) {
					txtParameters.setText(p.getValue());
					found = true;
					break;
				}
			}
		}
		if (!found && script.getDefaultParameters() != null) {
			txtParameters.setText(script.getDefaultParameters());
		}
	}
	
	@Override
	public void setFocus() {

	}

}

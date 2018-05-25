package org.wcs.smart.r.ui.editor.script;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

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
import org.wcs.smart.r.RPlugIn;
import org.wcs.smart.r.RScriptManager;

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
		toolkit = new FormToolkit(parent.getDisplay());
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout());
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		txtScript = new Text(main, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		txtScript.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtScript.setEditable(false);
		
	}

	@Override
	public void setFocus() {
		txtScript.setFocus();
	}
	
	public void update() {
		String fileName = parent.getScript().getFilename();
		txtScript.setText("TODO: show contents for " + fileName + " here. ");
		
		Path fileToRead = RScriptManager.INSTANCE.getScriptPath(parent.getScript());
		if (fileToRead == null || !Files.exists(fileToRead)) {
			txtScript.setText("ERROR - No R Script file found");
		}else {
			try(BufferedReader io = Files.newBufferedReader(fileToRead)){
				String line = null;
				StringBuilder sb = new StringBuilder();
				while ((line = io.readLine()) != null) {
					sb.append(line);
					sb.append("\n");
				}
				txtScript.setText(sb.toString());
			} catch (IOException ex) {
				txtScript.setText("Error Reading RScript file" + "\n" + ex.getMessage());
				RPlugIn.log(ex.getMessage(), ex);
			}
		}
	}

}

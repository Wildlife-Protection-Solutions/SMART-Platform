package org.wcs.smart.r.ui.editor.script;

import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.events.IHyperlinkListener;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.Hyperlink;
import org.eclipse.ui.part.EditorPart;

public class ResultsPage extends EditorPart {

	private FormToolkit toolkit;
	private RScriptEditor parent;
	
	private Text txtOutput;
	
	public ResultsPage(RScriptEditor parent) {
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
		super.setSite(site);
		super.setInput(input);
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

		Composite header = toolkit.createComposite(main);
		header.setLayout(new GridLayout(3, false));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		
		Label l = toolkit.createLabel(header, "R Script Output:");
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Hyperlink rerun = toolkit.createHyperlink(header, "run", SWT.NONE);
		rerun.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		rerun.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkExited(HyperlinkEvent e) {
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				ResultsPage.this.parent.executeScript();
			}
		});
		
		Hyperlink clear = toolkit.createHyperlink(header, "clear", SWT.NONE);
		clear.setLayoutData(new GridData(SWT.RIGHT, SWT.FILL, false, false));
		clear.addHyperlinkListener(new IHyperlinkListener() {
			@Override
			public void linkExited(HyperlinkEvent e) {
			}
			
			@Override
			public void linkEntered(HyperlinkEvent e) {
			}
			
			@Override
			public void linkActivated(HyperlinkEvent e) {
				txtOutput.setText("");
			}
		});

		txtOutput = new Text(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		txtOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtOutput.setEditable(false);
	}

	@Override
	public void setFocus() {
		txtOutput.setFocus();
	}
	
	public void update() {
		
	}
	
	public OutputStream createPage2OutputStream() {

		return new OutputStream() {
			StringWriter sb = new StringWriter();	
			private Job j = new Job("refreshJob") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Display.getDefault().syncExec(()->txtOutput.setText(sb.toString()));
					return Status.OK_STATUS;
				}
				
			};
			@Override
			public void close() throws IOException {
				final String lastString = sb.toString();
				Display.getDefault().asyncExec(()->txtOutput.setText(lastString));
				super.close();
				j.cancel();
				j = null;
			}
			
			@Override
			public void write(int b) throws IOException {
				sb.append((char)b);
				j.schedule(500);
			}
		};

	}
}

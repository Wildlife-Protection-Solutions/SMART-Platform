/*
 * Copyright (C) 2012 Wildlife Conservation Society
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
 * of the Software, and to permit persons to whom the Software is furnished to do
 * so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.wcs.smart.r.ui.editor.script;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.r.internal.Messages;

/**
 * R Editor results page
 * 
 * @author Emily
 *
 */
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
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Label l = toolkit.createLabel(main, Messages.ResultsPage_OutputLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		ToolBar tb = new ToolBar(main, SWT.FLAT);
		
		ToolItem btnRun = new ToolItem(tb, SWT.PUSH);
		btnRun.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.RUN_ICON));
		btnRun.setToolTipText(Messages.ResultsPage_rereuntooltip);
		btnRun.addListener(SWT.Selection, e-> ResultsPage.this.parent.executeScript());
			
		ToolItem btnClear = new ToolItem(tb, SWT.PUSH);
		btnClear.setImage(QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.CLEAR_ICON));
		btnClear.setToolTipText(Messages.ResultsPage_cleartooltip);
		btnClear.addListener(SWT.Selection, e->txtOutput.setText("")); //$NON-NLS-1$
		
		txtOutput = new Text(main, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
		txtOutput.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1));
		txtOutput.setEditable(false);
		
		main.setTabList(new Control[] {txtOutput, tb});
		
	}

	@Override
	public void setFocus() {
		txtOutput.setFocus();
	}
	
	public void update() {
		
	}
	
	public IRScriptOutputStream createPage2OutputStream() {
		IRScriptOutputStream test = new IRScriptOutputStream() {
			private Job j = new Job("refreshJob") { //$NON-NLS-1$
				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Display.getDefault().syncExec(()->txtOutput.setText(sb.toString()));
					return Status.OK_STATUS;
				}
								
			};
			@Override
			public void close() {
				final String lastString = sb.toString();
				Display.getDefault().asyncExec(()->txtOutput.setText(lastString));
				j.cancel();
				j = null;
			}
			
			@Override
			public void write(String string) {
				super.write(string);
				j.schedule(500);
			}
		};
		return test;
	}
}

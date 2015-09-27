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
package org.wcs.smart.cybertracker.importer;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISaveablePart2;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.cybertracker.internal.Messages;

/**
 * Dialog for importing CyberTracker application data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerImportEditor extends EditorPart implements ISaveablePart2 {

	public static final String ID = "org.wcs.smart.cybertracker.CyberTrackerImportEditor"; //$NON-NLS-1$

	private FormToolkit toolkit;
	private CyberTrackerImportComposite tableContainer;
	
	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		setSite(site);
		setInput(input);
	}

	@Override
	public void createPartControl(Composite parent) {
		toolkit = new FormToolkit(parent.getDisplay());
		Form form = toolkit.createForm(parent);
		form.setText(Messages.CyberTrackerImportEditor_ImportDataFormTitle);
		GridLayout layout = new GridLayout();
		form.getBody().setLayout(layout);
		
		tableContainer = new CyberTrackerImportComposite(form.getBody(), SWT.NONE, toolkit);
		tableContainer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}
	
	@Override
	public void dispose(){
		super.dispose();
		if (toolkit != null){
			toolkit.dispose();
		}
	}
		
	@Override
	public int promptToSaveOnClose() {
		boolean result = MessageDialog.openQuestion(getSite().getShell(), Messages.CyberTrackerImportEditor_ConfirmClose_Title, Messages.CyberTrackerImportEditor_ConfirmClose_Message);
		return result ? ISaveablePart2.NO : ISaveablePart2.CANCEL;
	}
	
	@Override
	public boolean isDirty() {
		return tableContainer.getViewer().getTable().getItemCount() > 0;
	}

	@Override
	public void doSave(IProgressMonitor monitor) {
		// nothing
	}

	@Override
	public void setFocus() {
		tableContainer.setFocus();
	}

	@Override
	public void doSaveAs() {
		//not allowed
	}

	@Override
	public boolean isSaveAsAllowed() {
		return false;
	}
	
}

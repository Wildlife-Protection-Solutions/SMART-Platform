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
package org.wcs.smart.patrol.xml.in;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.common.control.XmlImportDialog;
import org.wcs.smart.patrol.internal.Messages;

/**
 * Dialog for selecting file to import patrols into SMART
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class PatrolXmlImportDialog extends XmlImportDialog {
	
	private boolean keepIDs = false;

	public PatrolXmlImportDialog() {
		super(Display.getCurrent().getActiveShell(),
				Messages.ImportPatrolDialog_DialogTitle,
				Messages.ImportPatrolDialog_DialogText,
				Messages.ImportPatrolDialog_DialogMessage);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		Composite container = (Composite) super.createDialogArea(parent);
		
		Composite optionCmp = new Composite(container, SWT.NONE);
		optionCmp.setLayout(new GridLayout());
		optionCmp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Button btnAssign = new Button(optionCmp, SWT.RADIO);
		btnAssign.setText(Messages.PatrolXmlImportDialog_NewId);
		btnAssign.setToolTipText(Messages.PatrolXmlImportDialog_AutoGenerateIdsTooltip);
		btnAssign.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnAssign.setSelection(true);
		btnAssign.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				keepIDs = false;
			}
		});
		
		Button btnKeep = new Button(optionCmp, SWT.RADIO);
		btnKeep.setText(Messages.PatrolXmlImportDialog_KeepId);
		btnKeep.setToolTipText(Messages.PatrolXmlImportDialog_KeepIdsTooltip);
		btnKeep.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		btnKeep.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				keepIDs = true;
			}
		});

		return container;
	}
	
	public boolean isKeepIDs() {
		return keepIDs;
	}
}

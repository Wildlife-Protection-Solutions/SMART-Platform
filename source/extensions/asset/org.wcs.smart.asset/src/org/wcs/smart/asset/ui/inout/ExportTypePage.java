/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.asset.ui.inout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.asset.internal.Messages;

/**
 * Wizard page for determining which types of data to exprot. 
 * 
 * @author Emily
 *
 */
public class ExportTypePage extends WizardPage{

	private static final String OPTION_KEY = "OPTION"; //$NON-NLS-1$
	private List<Button> buttons;
	
	protected ExportTypePage() {
		super("EXPORT_TYPE"); //$NON-NLS-1$
	}

    @Override
	public boolean isPageComplete() {
        for (Button b : buttons) {
        	if (b.getSelection()) return true;
        }
        return false;
    }
	@Override
	public void createControl(Composite parent) {
		Composite panel = new Composite(parent, SWT.NONE);
		panel.setLayout(new GridLayout());
		panel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Composite inner = new Composite(panel, SWT.NONE);
		inner.setLayout(new GridLayout());
		inner.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		buttons = new ArrayList<>();
		for (AssetDataExportWizard.Type type : AssetDataExportWizard.Type.values()) {
			Button btnOp = new Button(inner, SWT.CHECK);
			btnOp.setText(type.guiName);
			buttons.add(btnOp);
			btnOp.setData(OPTION_KEY,  type);
			btnOp.addListener(SWT.Selection, e->{
				getWizard().getContainer().updateButtons();
			});
		}
		
		setTitle(Messages.ExportTypePage_Title);
		setMessage(Messages.ExportTypePage_Message);
		setControl(panel);
	}
	
	public Collection<AssetDataExportWizard.Type> getTypes(){
		List<AssetDataExportWizard.Type> types = new ArrayList<>();
		for (Button b : buttons) {
			if (b.getSelection()) {
				types.add( (AssetDataExportWizard.Type)b.getData(OPTION_KEY) );
			}
		}
		return types;
	}
}

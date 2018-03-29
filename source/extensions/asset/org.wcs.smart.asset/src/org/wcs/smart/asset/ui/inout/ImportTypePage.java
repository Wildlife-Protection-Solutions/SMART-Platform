/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
import java.util.List;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.asset.internal.Messages;

/**
 * Wizard page for determining which type of data to import. 
 * 
 * @author Emily
 *
 */
public class ImportTypePage extends WizardPage{

	private static final String OPTION_KEY = "OPTION"; //$NON-NLS-1$
	private List<Button> buttons;
	
	protected ImportTypePage() {
		super("IMPORT_TYPE"); //$NON-NLS-1$
	}

	
    @Override
	public IWizardPage getNextPage() {
        switch(getType()) {
		case ASSET_CSV:
		case STATION_CSV:
		case LOCATION_CSV:
			return ((AssetDataImportWizard)getWizard()).filePage;
		case XML:
			return ((AssetDataImportWizard)getWizard()).xmlPage;
        }
        return null;
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
		for (AssetDataImportWizard.Type type : AssetDataImportWizard.Type.values()) {
			Button btnOp = new Button(inner, SWT.RADIO);
			btnOp.setText(type.guiName);
			buttons.add(btnOp);
			btnOp.setData(OPTION_KEY,  type);
			btnOp.addListener(SWT.Selection, e->{
				getWizard().getContainer().updateButtons();
			});
		}
		buttons.get(0).setSelection(true);
		
		setTitle(Messages.ImportTypePage_Title);
		setMessage(Messages.ImportTypePage_Message);
		setControl(panel);
	}
	
	public AssetDataImportWizard.Type getType(){
		for (Button b : buttons) {
			if (b.getSelection()) {
				return (AssetDataImportWizard.Type)b.getData(OPTION_KEY);
			}
		}
		return null;
	}
}

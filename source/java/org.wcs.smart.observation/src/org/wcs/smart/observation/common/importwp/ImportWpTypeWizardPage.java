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
package org.wcs.smart.observation.common.importwp;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.observation.internal.Messages;

/**
 * Wizard page to select waypoint import location option.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class ImportWpTypeWizardPage extends WizardPage implements IImportWizardPage{

	/**
	 * 
	 */
	public static final String PAGE_NAME = Messages.ImportWpTypeWizardPage_PageName;

	private List<Button> options;
	private List<IImportEngine> supportedEngines;
	
	/**
	 * @param pageName
	 */
	protected ImportWpTypeWizardPage(List<IImportEngine> engines ) {
		super(PAGE_NAME);
		this.supportedEngines = engines;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.dialogs.IDialogPage#createControl(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	public void createControl(Composite parent) {
		
		Composite comp = new Composite(parent, SWT.NONE);
		comp.setLayout(new GridLayout(1, false));
		comp.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Composite center = new Composite(comp, SWT.NONE);
		center.setLayout(new GridLayout(1, false));
		center.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(MessageFormat.format(
				Messages.ImportWpTypeWizardPage_ImportFromLabel, new Object[]{((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase() }));
		
		Composite ops = new Composite(center, SWT.NONE);
		ops.setLayout(new GridLayout(1, false));
		((GridLayout)ops.getLayout()).marginLeft = 20;
		
		options = new ArrayList<Button>();
		for(IImportEngine engine : supportedEngines){
			Button op = new Button(ops, SWT.RADIO);
			op.setText(engine.getName());
			options.add(op);
		}
		options.get(0).setSelection(true);
	
		super.setTitle(Messages.ImportWpTypeWizardPage_PageTitle + ((ImportGpsDataWizard)getWizard()).getType().guiName);
		super.setMessage(MessageFormat.format(Messages.ImportWpTypeWizardPage_PageMessage, new Object[]{((ImportGpsDataWizard)getWizard()).getType().guiName.toLowerCase()}));
		super.setControl(comp);
	}
	
	@Override
    public IWizardPage getNextPage() {
		for (int i = 0; i < options.size(); i ++){
			if (options.get(i).getSelection()){
				return (IWizardPage) supportedEngines.get(i).getFirstWizardPage((ImportGpsDataWizard)getWizard());
			}
		}
		return null;
		
    }


	@Override
	public boolean beforeMoveNext(WizardPage nextPage) {
		for (int i = 0; i < options.size(); i ++){
			if (options.get(i).getSelection()){
				((ImportGpsDataWizard)getWizard()).setImportEngine(supportedEngines.get(i));
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean init() {
		return true;
	}
}

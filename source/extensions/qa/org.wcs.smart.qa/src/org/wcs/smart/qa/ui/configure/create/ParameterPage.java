/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.ui.configure.create;

import java.util.Locale;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.qa.InternalExtensionManager;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.ui.configure.IParameterCollector;

/**
 * Wizard page for collecting parameters from
 * the user for a given qa routine type.
 * 
 * @author Emily
 *
 */
public class ParameterPage extends WizardPage{

	public static final String ID = "parametercollector"; //$NON-NLS-1$

	private Composite all;
	private IParameterCollector collector;
	
	
	public ParameterPage() {
		super(ID);
	}

	@Override
	public void createControl(Composite parent) {
		
		all = new Composite(parent,  SWT.NONE);
		all.setLayout(new GridLayout());
		all.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		setTitle("Quality Assurance Routine Parameters");
		setMessage("Configure parameters for quality assurance routine");
		
		setControl(all);
	}
	
	@Override
	public boolean isPageComplete(){
		if (collector == null) return true;
		return collector.isValid();
	}

	public void initPage(QaRoutine r){
		for (Control c : all.getChildren()){
			c.dispose();
		}
		IQaRoutineType type = r.getRoutineType();
		if (type == null) return;
		setTitle(type.getName(Locale.getDefault()));
		setMessage(type.getDescription(Locale.getDefault()));
		
		collector = InternalExtensionManager.INSTANCE.newParameterCollector(r.getRoutineTypeId());
		if (collector == null){
			Label l = new Label(all, SWT.NONE);
			l.setText("No parameters are required for this QA routine.");
		}else{
			collector.createUi(all);
			collector.initUi(r);
			collector.addListener(e->getWizard().getContainer().updateButtons());
		}
		all.layout(true);
	}
	
	
	public void updateRoutine(QaRoutine r){
		if (collector == null) return;
		collector.updateParameters(r);
	}

}

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

import java.util.Collection;
import java.util.Locale;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.RoutineExtensionManager;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.ui.configure.RoutinesListDialog;

/**
 * Wizard page for collecting the routine type, name 
 * and other details from the user.
 * 
 * @author Emily
 *
 */
public class TypeNamePage extends WizardPage{

	public static final String ID = "typenamepage"; //$NON-NLS-1$
	
	private Text txtName ;
	private Text txtDescription ;
	private ComboViewer cmbType;
	private Button btnAuto;
	
	private ControlDecoration cdType;
	private ControlDecoration cdName;
	
	private boolean canFlip = false;
	
	public TypeNamePage() {
		super(ID);
	}

	@Override
	public boolean canFlipToNextPage(){
		return canFlip;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite all = new Composite(parent,  SWT.NONE);
		all.setLayout(new GridLayout(2, false));
		all.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label lblType = new Label(all, SWT.NONE);
		lblType.setText(RoutinesListDialog.RoutineColumn.TYPE.guiName + ":"); //$NON-NLS-1$
		lblType.setToolTipText(RoutinesListDialog.RoutineColumn.TYPE.tooltip);
		
		cmbType = new ComboViewer(all, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbType.setContentProvider(ArrayContentProvider.getInstance());
		cmbType.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				if (element instanceof IQaRoutineType) return ((IQaRoutineType) element).getName(Locale.getDefault());
				return super.getText(element);
			}
		});
		cmbType.getControl().addListener(SWT.Selection, e->{
			if (txtName.getText().length() == 0 && getSelectedType() != null) 
				txtName.setText(getSelectedType().getName(Locale.getDefault()));
			validate();
		});
		Collection<IQaRoutineType> input = RoutineExtensionManager.INSTANCE.getDefinedRoutineTypes();
		cmbType.setInput(input);
		if (!input.isEmpty()) cmbType.setSelection(new StructuredSelection(input.iterator().next()));
		
		cdType = new ControlDecoration(cmbType.getControl(), SWT.TOP | SWT.LEFT);
		cdType.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdType.hide();
		
		Label lblName = new Label(all, SWT.NONE);
		lblName.setText(RoutinesListDialog.RoutineColumn.NAME.guiName + ":"); //$NON-NLS-1$
		lblName.setToolTipText(RoutinesListDialog.RoutineColumn.NAME.tooltip);
		
		txtName = new Text(all, SWT.BORDER);
		txtName.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txtName.addListener(SWT.Modify, e->validate());
		txtName.setTextLimit(org.wcs.smart.ca.Label.MAX_LENGTH);
		
		cdName = new ControlDecoration(txtName, SWT.TOP | SWT.LEFT);
		cdName.setImage(FieldDecorationRegistry.getDefault().getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cdName.hide();
		
		Label lblAuto = new Label(all, SWT.NONE);
		lblAuto.setText(RoutinesListDialog.RoutineColumn.AUTO.guiName + ":"); //$NON-NLS-1$
		lblAuto.setToolTipText(RoutinesListDialog.RoutineColumn.AUTO.tooltip);

		btnAuto = new Button(all, SWT.CHECK);
		
		Label lblDesc = new Label(all, SWT.NONE);
		lblDesc.setLayoutData(new GridData(SWT.TOP, SWT.LEFT, false, false));
		lblDesc.setText(RoutinesListDialog.RoutineColumn.DESC.guiName + ":"); //$NON-NLS-1$
		lblDesc.setToolTipText(RoutinesListDialog.RoutineColumn.DESC.tooltip);
		
		txtDescription = new Text(all, SWT.MULTI | SWT.BORDER);
		txtDescription.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		txtDescription.setTextLimit(QaRoutine.MAX_DESC_LENGTH);
		
		setControl(all);
		
		setTitle("Quality Assurance Routine Details");
		setMessage("Configure a new quality assurance routine.");
		
		validate(false);
	}
	
	private void validate(boolean buttons){
		canFlip = true;
		if (getSelectedType() == null){
			canFlip = false;
			cdType.setDescriptionText("Quality assurance routine required.");
			cdType.show();
		}else{
			cdType.hide();
		}
		if (txtName.getText().trim().length() == 0){
			canFlip = false;
			cdName.setDescriptionText("Name required");
			cdName.show();
		}else{
			cdName.hide();
		}
		if (buttons) getWizard().getContainer().updateButtons();
	}
	
	private void validate(){
		validate(true);
	}

	/**
	 * Creates a new qa routine from the values in the controls and
	 * returns it.  Will return null if a QA Routine cannot be generated
	 * @return
	 */
	public QaRoutine getRoutine(){
		if (getSelectedType() == null) return null;
		
		QaRoutine r = new QaRoutine();
		r.setAutoCheck(btnAuto.getSelection());
		String name = txtName.getText().trim();
		r.setName(name);
		r.updateName(SmartDB.getCurrentLanguage(), name);
		r.updateName(SmartDB.getCurrentConservationArea().getDefaultLanguage(), name);
		r.setConservationArea(SmartDB.getCurrentConservationArea());
		if (txtDescription.getText().trim().length() > 0){
			r.setDescription(txtDescription.getText().trim());
		}
		r.setRoutineTypeId(getSelectedType().getId());
		
		return r;
	}
	
	
	private IQaRoutineType getSelectedType(){
		if (cmbType.getSelection().isEmpty()) return null;
		Object x = ((IStructuredSelection)cmbType.getSelection()).getFirstElement();
		if (x instanceof IQaRoutineType) return (IQaRoutineType)x;
		return null;
	}

}

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
package org.wcs.smart.plan.ui.panel;

import java.util.Locale;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.Plan.PlanType;

/**
 * Composite for collecting the plan description information
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanTypeEmployeesComposite extends PlanComposite {

	private static final String UNAVAILABLE_EMPLOYEES_DEFAULT_VALUE = "0"; //$NON-NLS-1$
	
	private ComboViewer planType = null;
	private Text unavailableEmployees;
	private Label activeEmployees;
	
	private int iActiveEmployees;
	
	private ControlDecoration cdUnavailEmployee;
	
	/**
	 * @param parent
	 * @param style
	 */
	public PlanTypeEmployeesComposite(Composite parent, int style) {
		super(parent, style);
		setMessage(Messages.PlanTypeEmployeesComposite_Message);
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Label lbl = new Label(this, SWT.NONE);
		lbl.setText(Messages.PlanTypeEmployeesComposite_Type_Label);
		GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd.horizontalIndent = 8;
		lbl.setLayoutData(gd);
		
		planType = new ComboViewer(this, SWT.READ_ONLY);
		planType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		planType.setContentProvider(ArrayContentProvider.getInstance());
		planType.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((Plan.PlanType)element).getGuiName(Locale.getDefault());
			}
		});
		planType.setInput(Plan.PlanType.values());
		planType.setSelection(new StructuredSelection(Plan.PlanType.PATROL));
		
        planType.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				fireInputChangeListeners();	
			}
		});
				
		Label lbl2 = new Label(this, SWT.NONE);
		lbl2.setText(Messages.PlanTypeEmployeesComposite_ActiveRangers_Label);
		GridData gd2 = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd2.horizontalIndent = 8;
		lbl2.setLayoutData(gd2);
		
		activeEmployees = new Label(this, SWT.NONE);
		activeEmployees.setText(Messages.PlanTypeEmployeesComposite_Unknown_Label);
		GridData gda = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gda.horizontalIndent = 8;
		activeEmployees.setLayoutData(gda);

		Label lbl4 = new Label(this, SWT.NONE);
		lbl4.setText(Messages.PlanTypeEmployeesComposite_UnavailableRangers_Label);
		GridData gd4 = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd4.horizontalIndent = 8;
		lbl4.setLayoutData(gd4);

		unavailableEmployees = new Text(this, SWT.BORDER);
		unavailableEmployees.setTextLimit(5);
		unavailableEmployees.setText(UNAVAILABLE_EMPLOYEES_DEFAULT_VALUE);
		GridData gdun = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gdun.horizontalIndent = 8;
		unavailableEmployees.setLayoutData(gdun);

		unavailableEmployees.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				fireInputChangeListeners();
			}
		});
		Label lbl5 = new Label(this, SWT.NONE);
		lbl5.setText(Messages.PlanTypeEmployeesComposite_VacationSickness_Label);
		GridData gd5 = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd5.horizontalIndent = 8;
		lbl5.setLayoutData(gd5);

        cdUnavailEmployee = new ControlDecoration(unavailableEmployees, SWT.LEFT);
        cdUnavailEmployee.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        cdUnavailEmployee.setShowHover(true);
        cdUnavailEmployee.setDescriptionText(Messages.PlanTypeEmployeesComposite_InvalidName_Error);
	}
	
	@Override
	protected boolean updateModelInternal(Plan plan) {
		plan.setType((PlanType) ((IStructuredSelection)planType.getSelection()).getFirstElement());
		Integer act = Integer.valueOf(activeEmployees.getText());
		Integer un = Integer.valueOf(this.unavailableEmployees.getText());
		plan.setUnavailableEmployees(un);
		plan.setActiveEmployees(act);
		return true;
	}

	@Override
	public void initFromModel(Plan plan) {
		if(plan.getUnavailableEmployees() != null){
			unavailableEmployees.setText(plan.getUnavailableEmployees().toString());
		}else{
			unavailableEmployees.setText(UNAVAILABLE_EMPLOYEES_DEFAULT_VALUE);
		}
		if(plan.getType() != null){
				planType.setSelection(new StructuredSelection(plan.getType()));
		}
		if(plan.getActiveEmployees() != null){
			iActiveEmployees = plan.getActiveEmployees();
			activeEmployees.setText(plan.getActiveEmployees().toString());
		}else{
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			try{
				iActiveEmployees = HibernateManager.getActiveEmployees(plan.getConservationArea(), session).size();
			}finally{
				session.getTransaction().rollback();
			}
			
			activeEmployees.setText(String.valueOf(iActiveEmployees));
		}
		fireInputChangeListeners();
	}

	@Override
	protected void validate() {
		setErrorMessage(null);
		if (!validatedEmployees()) {
			setErrorMessage(Messages.PlanTypeEmployeesComposite_InvalidName_Error);
			cdUnavailEmployee.show();
		}else{
			cdUnavailEmployee.hide();
		}
	}
	
	private boolean validatedEmployees() {
		try{
			Integer x = Integer.parseInt(unavailableEmployees.getText());
			if (x < 0 || x> iActiveEmployees){
				return false;
			}
		}catch (Exception e){
			return false;
		}
		return true;
	}

	@Override
	public String getTitle() {
		return Messages.PlanTypeEmployeesComposite_Titlle;
	}
}

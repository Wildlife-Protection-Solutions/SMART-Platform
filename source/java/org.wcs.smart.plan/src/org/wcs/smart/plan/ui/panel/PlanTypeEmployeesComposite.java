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
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.model.Plan.PlanType;

/**
 * Composite for collecting the plan description information
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class PlanTypeEmployeesComposite extends PlanComposite {

	private ComboViewer planType = null;
	private Text unavailableEmployees;
	private Label activeEmployees;
	
	private ControlDecoration UeDecoration;
	
	/**
	 * @param parent
	 * @param style
	 */
	public PlanTypeEmployeesComposite(Composite parent, int style) {
		super(parent, style);
		setMessage("Edit Plan type and unavailable Resources");
		createControls();
	}

	private void createControls() {
        this.setLayout(new GridLayout(2, false));
        this.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));

		Label lbl = new Label(this, SWT.NONE);
		lbl.setText("Plan Type:");
		GridData gd = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd.horizontalIndent = 8;
		lbl.setLayoutData(gd);
		
		planType = new ComboViewer(this, SWT.READ_ONLY);
		planType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		planType.setContentProvider(ArrayContentProvider.getInstance());
		planType.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((Plan.PlanType)element).guiName;
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
		lbl2.setText("Active Rangers:");
		GridData gd2 = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd2.horizontalIndent = 8;
		lbl2.setLayoutData(gd2);
		
		activeEmployees = new Label(this, SWT.NONE);
		activeEmployees.setText("unknown");
		GridData gda = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gda.horizontalIndent = 8;
		activeEmployees.setLayoutData(gda);

		Label lbl4 = new Label(this, SWT.NONE);
		lbl4.setText("Unavailable Rangers:");
		GridData gd4 = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd4.horizontalIndent = 8;
		lbl4.setLayoutData(gd4);

		unavailableEmployees = new Text(this, SWT.BORDER);
		unavailableEmployees.setTextLimit(5);
		unavailableEmployees.setText("0");
		GridData gdun = new GridData(SWT.FILL, SWT.CENTER, true, false);
		gdun.horizontalIndent = 8;
		unavailableEmployees.setLayoutData(gdun);

		unavailableEmployees.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (!isIdValid()) {
					UeDecoration.show();
				} else {
					UeDecoration.hide();
				}
				fireDataValidStateListeners();

				fireInputChangeListeners();
			}
		});
		Label lbl5 = new Label(this, SWT.NONE);
		lbl5.setText("(vacation, sickness, etc)");
		GridData gd5 = new GridData(SWT.RIGHT, SWT.CENTER, false, false);
		gd5.horizontalIndent = 8;
		lbl5.setLayoutData(gd5);

        UeDecoration = new ControlDecoration(unavailableEmployees, SWT.LEFT);
        UeDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
        UeDecoration.setShowHover(true);
        UeDecoration.setDescriptionText("Invalid Name");

		
	}
	
	@Override
	public boolean updateModel(Plan plan) {
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
			unavailableEmployees.setText("0");
		}
		if(plan.getType() != null){
				planType.setSelection(new StructuredSelection(plan.getType()));
		}
		if(plan.getActiveEmployees() != null){
			activeEmployees.setText(plan.getActiveEmployees().toString());
		}else{
			Session session = HibernateManager.openSession();
			session.beginTransaction();
			Integer act = HibernateManager.getActiveEmployees(plan.getConservationArea(), session).size();
			session.getTransaction().rollback();
			
			activeEmployees.setText(act.toString());
		}
	}

	@Override
	public boolean isDataValid() {
		return isIdValid();
	}
	
	private boolean isIdValid() {
		try{
			Integer.parseInt(unavailableEmployees.getText());
		}catch (Exception e){
			return false;
		}
		return true;
	}

}

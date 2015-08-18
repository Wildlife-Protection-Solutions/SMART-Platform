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

package org.wcs.smart.plan.ui.targets;


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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.internal.PlanLabelProvider;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget.TargetType;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.model.SpatialPlanTarget;
import org.wcs.smart.plan.ui.newPlanWizard.ITargetPage;

/**
 * Page for collecting numeric plan target information.
 * 
 * @author Emily
 * @author jeffloun
 *
 */
public class NumericPlanTargetPropertyPage implements ITargetPage {

	
	private TargetPropertyDialog parentWindow;
	
	private Text targetValue;
	private Text targetName;
	private ComboViewer targetType = null;
	private ComboViewer targetOp = null;
	private Text targetDesc;
	
	private ControlDecoration cdTargetValue;
	private ControlDecoration cdTargetName;
	private PlanTarget planTarget;
	private Label lblUnits; 
	
	private boolean isInit = false;	
	private Listener changeListener = new Listener() {
		@Override
		public void handleEvent(Event event) {
			if (!isInit){
				parentWindow.setDirty();
			}
		}
	};
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public NumericPlanTargetPropertyPage(TargetPropertyDialog parentWindow) {
		this.parentWindow = parentWindow;
	}
	
	@Override
	public String getPageName(){
		return PlanLabelProvider.NUMERIC_TARGET_GUI_NAME;
	}
	
	private GridData createGridDataWithIndent(int colspan){
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false, colspan, 1);
		gd.horizontalIndent = 8;
		return gd;
	}
	
	@Override
	public Composite createComponent(Composite parent, int style) {
		TargetType defaultTt = NumericPlanTarget.TargetType.DISTANCE;
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(3, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
	
		Label lbl2 = new Label(center, SWT.NONE);
		lbl2.setText(Messages.NumericPlanTargetPropertyPage_TargetType_Label);
		lbl2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		targetType= new ComboViewer(center, SWT.READ_ONLY);
		targetType.getControl().setLayoutData(createGridDataWithIndent(2));
		targetType.setContentProvider(ArrayContentProvider.getInstance());
		targetType.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((NumericPlanTarget.TargetType)element).getGuiName(Locale.getDefault());
			}
		});
		targetType.setInput(NumericPlanTarget.TargetType.values());
		targetType.setSelection(new StructuredSelection(defaultTt));
		targetType.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if(planTarget == null || planTarget.getName() == null || planTarget.getName().isEmpty()){
					targetName.setText( getTargetType().getGuiName(Locale.getDefault()));
				}
				lblUnits.setText(getTargetType().getUnits().getGuiName(Locale.getDefault()));
			}
		});
		targetType.getControl().addListener(SWT.Selection, changeListener);
		targetType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Label lbl3 = new Label(center, SWT.NONE);
		lbl3.setText(Messages.NumericPlanTargetPropertyPage_Operator_Label);
		lbl3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		targetOp= new ComboViewer(center, SWT.READ_ONLY);
		targetOp.getControl().setLayoutData(createGridDataWithIndent(2));
		targetOp.setContentProvider(ArrayContentProvider.getInstance());
		targetOp.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((NumericPlanTarget.Operator)element).getSmartValue();
			}
		});
		targetOp.setInput(NumericPlanTarget.Operator.values());
		targetOp.setSelection(new StructuredSelection(NumericPlanTarget.Operator.GREATER));
		targetOp.getControl().addListener(SWT.Selection, changeListener);
		targetOp.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 2, 1));
		
		Label lbl4 = new Label(center, SWT.NONE);
		lbl4.setText(Messages.NumericPlanTargetPropertyPage_TargetValue_Label);
		lbl4.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetValue = new Text(center, SWT.BORDER | SWT.LEFT);
		targetValue.setTextLimit(32);
		targetValue.setLayoutData(createGridDataWithIndent(1));
		targetValue.addListener(SWT.Modify, changeListener);
		
		lblUnits = new Label(center, SWT.NONE);
		lblUnits.setText(getTargetType().getUnits().getGuiName(Locale.getDefault()));
		lblUnits.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText(Messages.NumericPlanTargetPropertyPage_TargetName_Label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetName = new Text(center, SWT.BORDER | SWT.LEFT);
		targetName.setTextLimit(SpatialPlanTarget.MAX_NAME_LENGTH);
		targetName.setLayoutData(createGridDataWithIndent(2));
		if(planTarget == null){
			targetName.setText( getTargetType().getGuiName(Locale.getDefault()) );
		}
		targetName.addListener(SWT.Modify, changeListener);
		
		
		cdTargetValue = createDecoration(targetValue);
		cdTargetName = createDecoration(targetName);

		Label descLabel = new Label(center, SWT.NONE);
		descLabel.setText(Messages.NumericPlanTargetPropertyPage_Description_Label);
		descLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

		targetDesc = new Text(center, SWT.BORDER  | SWT.WRAP | SWT.V_SCROLL);
		targetDesc.setTextLimit(AdministrativePlanTarget.MAX_DESC_LENGTH);
		targetDesc.setLayoutData(createGridDataWithIndent(2));
		((GridData)targetDesc.getLayoutData()).verticalSpan = 2;
		((GridData)targetDesc.getLayoutData()).widthHint = 100;
		((GridData)targetDesc.getLayoutData()).heightHint = 50;
		((GridData)targetDesc.getLayoutData()).grabExcessVerticalSpace = true;
		
		targetDesc.addListener(SWT.Modify, changeListener);
		
		return center;
	}
	
	protected ControlDecoration createDecoration(Control control){
		ControlDecoration cd = new ControlDecoration(control, SWT.LEFT);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	/**
	 * Validate the input fields
	 * 
	 * @return <code>false</code> if not complete, <code>true</code> otherwise
	 */
	@Override
	public boolean validate() {

		boolean isComplete = true;
		if (targetName.getText().trim().isEmpty()
				|| targetName.getText().length() > PlanTarget.MAX_NAME_LENGTH)  {
			cdTargetName.show();
			cdTargetName.setDescriptionText(Messages.NumericPlanTargetPropertyPage_InvalidName_Error1);
			isComplete = false;
		}else{
			cdTargetName.hide();
		}
		
		try{
			Double.parseDouble(targetValue.getText());
			//exception will kick us out here if it's not valid
			cdTargetValue.hide();
		}catch(Exception e){
			cdTargetValue.show();
			cdTargetValue.setDescriptionText(Messages.NumericPlanTargetPropertyPage_InvalidValue_Error);
			isComplete = false;
		}

		
		if (getTargetOp() == null){
			isComplete = false;
		}
		if (getTargetType() == null){
			isComplete = false;
		}
		return isComplete;
	}

	/*
	 * return the numeric value entered by the user
	 */
	private Double getTargetValue(){
		try{
			return Double.valueOf(targetValue.getText());
		}catch (Exception ex){
			return null;
		}
	}
	
	/*
	 * return the target name
	 */
	private String getTargetName(){
		return targetName.getText();
	}
	
	/*
	 * return target type
	 */
	private NumericPlanTarget.TargetType getTargetType(){
		if (targetType.getSelection().isEmpty()){
			return null;
		}
		return (TargetType) ((IStructuredSelection)targetType.getSelection()).getFirstElement();
	}
	
	/*
	 * return operator
	 */
	private NumericPlanTarget.Operator getTargetOp(){
		if (targetOp.getSelection().isEmpty()){
			return null;
		}
		return (NumericPlanTarget.Operator) ((IStructuredSelection)targetOp.getSelection()).getFirstElement();
	}

	/**
	 * @see org.wcs.smart.plan.ui.newPlanWizard.ITargetPage#createTarget()
	 */
	@Override
	public PlanTarget createTarget() {
		return new NumericPlanTarget();
	}
	
	/**
	 * @see org.wcs.smart.plan.ui.newPlanWizard.ITargetPage#updateTarget(org.wcs.smart.plan.model.PlanTarget)
	 */
	@Override
	public void updateTarget(PlanTarget pt){
		if (!(pt instanceof NumericPlanTarget)){
			return;
		}
		NumericPlanTarget target = (NumericPlanTarget)pt;
		target.setValue(getTargetValue());
		target.setName(getTargetName());
		target.setType(getTargetType());
		target.setOp(getTargetOp());
		if (targetDesc.getText().trim().length() > 0){
			target.setDescription(targetDesc.getText());
		}else{
			target.setDescription(null);
		}
	}
	
	/**
	 * @see org.wcs.smart.plan.ui.newPlanWizard.ITargetPage#initPage(org.wcs.smart.plan.model.PlanTarget)
	 */
	@Override
	public void initPage(PlanTarget p) {
		this.isInit = true;
		try{
			if (!(p instanceof NumericPlanTarget)){
				return;
			}
			NumericPlanTarget pt = (NumericPlanTarget) p;

			this.targetOp.setSelection(new StructuredSelection(pt.getOp()));
			this.targetType.setSelection(new StructuredSelection(pt.getType()));
			this.targetValue.setText(pt.getValue().toString());
			this.planTarget = p;
			this.targetName.setText(pt.getName());
			lblUnits.setText(pt.getType().getUnits().getGuiName(Locale.getDefault()));
			String descr = pt.getDescription();
			this.targetDesc.setText(descr != null ? descr : ""); //$NON-NLS-1$
		
			validate();
		}finally{
			this.isInit = false;
		}
	}
	
}


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


import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.plan.model.NumericPlanTarget;
import org.wcs.smart.plan.model.NumericPlanTarget.TargetType;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.newPlanWizard.ITargetPage;
import org.wcs.smart.util.SmartUtils;

/**
 * Page for collecting numeric plan target information.
 * 
 * @author Emily
 * @author jeffloun
 *
 */
public class NumericPlanTargetPropertyPage implements ITargetPage {

	
	private TargetPropertyPage parentWindow;
	
	private Text targetValue;
	private Text targetName;
	private ComboViewer targetType = null;
	private ComboViewer targetOp = null;
	
	private ControlDecoration cdTargetValue;
	private ControlDecoration cdTargetName;
	
	
	/**
	 * Creates new editor page
	 * @param parent
	 */
	public NumericPlanTargetPropertyPage(TargetPropertyPage parentWindow) {
		this.parentWindow = parentWindow;
	}
	
	@Override
	public String getPageName(){
		return NumericPlanTarget.TARGET_GUI_NAME;
	}
	
	private GridData createGridDataWithIndent(){
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 8;
		return gd;
	}
	
	@Override
	public Composite createComponent(Composite parent, int style) {
		Composite center = new Composite(parent, SWT.NONE);
		center.setLayout(new GridLayout(2, false));
		center.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
	
		Label lbl = new Label(center, SWT.NONE);
		lbl.setText("Target Name:");
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetName = new Text(center, SWT.BORDER | SWT.LEFT);
		targetName.setTextLimit(PlanTarget.MAX_NAME_LENGTH);
		targetName.setLayoutData(createGridDataWithIndent());
		
		Label lbl2 = new Label(center, SWT.NONE);
		lbl2.setText("Target Type:");
		lbl2.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		targetType= new ComboViewer(center, SWT.READ_ONLY);
		targetType.getControl().setLayoutData(createGridDataWithIndent());
		targetType.setContentProvider(ArrayContentProvider.getInstance());
		targetType.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((NumericPlanTarget.TargetType)element).guiName;
			}
		});
		targetType.setInput(NumericPlanTarget.TargetType.values());
		targetType.setSelection(new StructuredSelection(NumericPlanTarget.TargetType.DISTANCE));
		
		
		Label lbl3 = new Label(center, SWT.NONE);
		lbl3.setText("Operator:");
		lbl3.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		targetOp= new ComboViewer(center, SWT.READ_ONLY);
		targetOp.getControl().setLayoutData(createGridDataWithIndent());
		targetOp.setContentProvider(ArrayContentProvider.getInstance());
		targetOp.setLabelProvider(new LabelProvider(){
			public String getText(Object element){
				return ((NumericPlanTarget.Operator)element).guiName;
			}
		});
		targetOp.setInput(NumericPlanTarget.Operator.values());
		targetOp.setSelection(new StructuredSelection(NumericPlanTarget.Operator.GREATER));
		
		Label lbl4 = new Label(center, SWT.NONE);
		lbl4.setText("Target Value:");
		lbl4.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetValue = new Text(center, SWT.BORDER | SWT.LEFT);
		targetValue.setTextLimit(32);
		targetValue.setLayoutData(createGridDataWithIndent());
		
		
		
		KeyListener validate = new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				validate();
			}
		};
		targetValue.addKeyListener(validate);
		targetName.addKeyListener(validate);
		
		cdTargetValue = createDecoration(targetValue);
		cdTargetName = createDecoration(targetName);

		validate();
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
				|| ! SmartUtils.isSimpleString(targetName.getText(), SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, Employee.MAX_NAME_LENGTH) ) {
			cdTargetName.show();
			cdTargetName.setDescriptionText("Name cannot be empty and must only contain " + SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
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
			cdTargetValue.setDescriptionText("Value must be a numeric value");
			isComplete = false;
		}

		
		if (getTargetOp() == null){
			isComplete = false;
		}
		if (getTargetType() == null){
			isComplete = false;
		}
		
		if(isComplete){
			parentWindow.enableOK(true);
		}else{
			parentWindow.enableOK(false);
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
		NumericPlanTarget target = (NumericPlanTarget)pt;
		target.setValue(getTargetValue());
		target.setName(getTargetName());
		target.setType(getTargetType());
		target.setOp(getTargetOp());
	}
	
	/**
	 * @see org.wcs.smart.plan.ui.newPlanWizard.ITargetPage#initPage(org.wcs.smart.plan.model.PlanTarget)
	 */
	@Override
	public void initPage(PlanTarget p) {
		NumericPlanTarget pt = (NumericPlanTarget) p;
		this.targetName.setText(pt.getName());
		this.targetOp.setSelection(new StructuredSelection(pt.getOp()));
		this.targetType.setSelection(new StructuredSelection(pt.getType()));
		this.targetValue.setText(pt.getValue().toString());
		
		validate();
	}
	
}


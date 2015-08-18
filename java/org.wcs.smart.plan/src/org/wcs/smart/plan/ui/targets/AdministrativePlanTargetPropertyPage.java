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
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.internal.PlanLabelProvider;
import org.wcs.smart.plan.model.AdministrativePlanTarget;
import org.wcs.smart.plan.model.PlanTarget;
import org.wcs.smart.plan.ui.newPlanWizard.ITargetPage;
/**
 * Page for collecting administrative plan target properties
 * @author Emily
 *
 */
public class AdministrativePlanTargetPropertyPage implements ITargetPage{

	private TargetPropertyDialog parentWindow;
	
	private Text targetDesc;
	private Text targetName;
	private Button targetIsComplete;
	private ControlDecoration cdTargetName;
	
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
	public AdministrativePlanTargetPropertyPage(TargetPropertyDialog parentWindow) {
		this.parentWindow = parentWindow;
	}
	
	/**
	 * @return page name
	 */
	public String getPageName(){
		return PlanLabelProvider.ADMIN_TARGET_GUI_NAME;
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
		lbl.setText(Messages.AdministrativePlanTargetPropertyPage_Name_Label);
		lbl.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetName = new Text(center, SWT.BORDER );
		targetName.setTextLimit(PlanTarget.MAX_NAME_LENGTH);
		targetName.setLayoutData( createGridDataWithIndent());
		targetName.addListener(SWT.Modify, changeListener);
		
		
		Label lbl4 = new Label(center, SWT.NONE);
		lbl4.setText(Messages.AdministrativePlanTargetPropertyPage_Description_Label);
		lbl4.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));

		targetDesc= new Text(center, SWT.BORDER  | SWT.WRAP | SWT.V_SCROLL);
		targetDesc.setTextLimit(AdministrativePlanTarget.MAX_DESC_LENGTH);
		targetDesc.setLayoutData(createGridDataWithIndent());
		((GridData)targetDesc.getLayoutData()).widthHint = 100;
		((GridData)targetDesc.getLayoutData()).heightHint = 50;
		((GridData)targetDesc.getLayoutData()).grabExcessVerticalSpace = true;
		targetDesc.addListener(SWT.Modify, changeListener);
		
		Label lblt = new Label(center, SWT.NONE);
		lblt.setText(Messages.AdministrativePlanTargetPropertyPage_Achieved_Label);
		lblt.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		targetIsComplete = new Button(center, SWT.CHECK);
		targetIsComplete.addListener(SWT.Selection, changeListener);

		cdTargetName = createDecoration(targetName);

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
	public boolean validate() {

		boolean isComplete = true;
		if (targetName.getText().trim().isEmpty()
				||
				targetName.getText().length() > PlanTarget.MAX_NAME_LENGTH){
			cdTargetName.show();
			cdTargetName.setDescriptionText(Messages.AdministrativePlanTargetPropertyPage_EmptyName_Error1);
			isComplete = false;
		}else{
			cdTargetName.hide();
		}
		return isComplete;
	}


	/*
	 * return target description
	 */
	private String getTargetDesc() {
		if (targetDesc.getText().trim().length() == 0){
			return null;
		}
		return targetDesc.getText();
	}
	
	/*
	 * return target name
	 */
	private String getTargetName() {
		return targetName.getText();
	}
	
	/**
	 * @see org.wcs.smart.plan.ui.newPlanWizard.ITargetPage#initPage(org.wcs.smart.plan.model.PlanTarget)
	 */
	@Override
	public void initPage(PlanTarget p) {
		this.isInit = true;
		try{
			if (!(p instanceof AdministrativePlanTarget)){
				return;
			}
			AdministrativePlanTarget pt = (AdministrativePlanTarget) p;
			this.targetName.setText(pt.getName());
			if (pt.getTargetDesc() != null){
				this.targetDesc.setText(pt.getTargetDesc());
			}else{
				this.targetDesc.setText(""); //$NON-NLS-1$
			}
			this.targetIsComplete.setSelection(pt.getStatus());
			validate();
		}finally{
			this.isInit = false;
		}
	}
	
	/**
	 * @see org.wcs.smart.plan.ui.newPlanWizard.ITargetPage#createTarget()
	 */
	@Override
	public PlanTarget createTarget(){
		return new AdministrativePlanTarget();
	}
	
	/**
	 * @see org.wcs.smart.plan.ui.newPlanWizard.ITargetPage#updateTarget(org.wcs.smart.plan.model.PlanTarget)
	 */
	@Override
	public void updateTarget(PlanTarget pt){
		if (!(pt instanceof AdministrativePlanTarget)){
			return;
		}
		AdministrativePlanTarget target = (AdministrativePlanTarget)pt;
		target.setName(getTargetName());
		target.setTargetDesc(getTargetDesc());
		target.setStatus(targetIsComplete.getSelection());
	}

}


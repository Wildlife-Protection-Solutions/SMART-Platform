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
package org.wcs.smart.plan.internal.preference;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;

/**
 * SMART Plan Configuration Preference Page
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PlanConfigurationPreferencePage extends PreferencePage implements IWorkbenchPreferencePage {

	public static final String ID = "org.wcs.smart.preference.PlanConfiguration"; //$NON-NLS-1$
	
	
	private Text txtDistanceToComplete;
    private ControlDecoration distancDecoration;

	/**
	 * Default constructor
	 */
	public PlanConfigurationPreferencePage() {}

	/**
	 * @param title
	 */
	public PlanConfigurationPreferencePage(String title) {
		super(title);
	}

	/**
	 * @param title
	 * @param image
	 */
	public PlanConfigurationPreferencePage(String title, ImageDescriptor image) {
		super(title, image);
	}

	@Override
	protected Control createContents(Composite parent) {
		final Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout(3, false));

		Label distanceLabel = new Label(main, SWT.NONE);
		distanceLabel.setText(Messages.PlanConfigurationPreferencePage_DistanceToComplete_Label);
		distanceLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

		txtDistanceToComplete = new Text(main, SWT.BORDER);
		txtDistanceToComplete.setTextLimit(32);
		GridData gd = new GridData(SWT.FILL, SWT.FILL, true, false);
		gd.horizontalIndent = 5;
		txtDistanceToComplete.setLayoutData(gd);
		
		
		distancDecoration = new ControlDecoration(txtDistanceToComplete, SWT.LEFT);
		distancDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		distancDecoration.setShowHover(true);
		distancDecoration.setDescriptionText(Messages.PlanConfigurationPreferencePage_DistanceToComplete_DecorationMessage);
		//distancDecoration.hide();

		txtDistanceToComplete.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isDistanceToCompleteValid()) {
					distancDecoration.hide();
				} else {
					distancDecoration.show();
				}
			}
		});
		
		//init with values
		int propValue = SmartPlanPlugIn.getDefault().getPreferenceStore().getInt(SmartPlanPlugIn.SYSPROP_PLAN_DISTANCE_TO_COMPLETE);
		txtDistanceToComplete.setText(String.valueOf(propValue));
		
		Label lblMeter = new Label(main, SWT.NONE);
		lblMeter.setText(Messages.PlanConfigurationPreferencePage_METER_LABEL);
		
		return main;
	}

	@Override
	public void init(IWorkbench workbench) {
		//nothing
	}

	@Override
	protected void performDefaults() {
		super.performDefaults();
		txtDistanceToComplete.setText(String.valueOf(SmartPlanPlugIn.getDefault().getDefaultPreferenceInt(SmartPlanPlugIn.SYSPROP_PLAN_DISTANCE_TO_COMPLETE)));
		performApply();
	}

	@Override
	public boolean performOk() {
		if (!isDistanceToCompleteValid()) {
			MessageDialog.openError(getShell(), Messages.PlanConfigurationPreferencePage_ErrorDialog_Title, Messages.PlanConfigurationPreferencePage_ErrorDialog_Message);
			return false;
		}

		int newValue = Integer.parseInt(txtDistanceToComplete.getText());
		
		int oldValue = SmartPlanPlugIn.getDefault().getPreferenceStore().getInt(SmartPlanPlugIn.SYSPROP_PLAN_DISTANCE_TO_COMPLETE);
		if (oldValue == newValue) {
			return true;
		}

		try {
			SmartPlanPlugIn.getDefault().getPreferenceStore().setValue(SmartPlanPlugIn.SYSPROP_PLAN_DISTANCE_TO_COMPLETE, newValue);
		} catch (Exception ex) {
			SmartPlugIn.displayLog(Messages.PlanConfigurationPreferencePage_CannotUpdate_Error + ex.getMessage(), ex);
			return false;
		}
		
		return true;
	}

	private boolean isDistanceToCompleteValid() {
		try {
			int number = Integer.parseInt(txtDistanceToComplete.getText());
			if (number < 0){
				return false;
			}
		} catch (Exception e) {
			return false;
		}
		return true;
	}
	
}

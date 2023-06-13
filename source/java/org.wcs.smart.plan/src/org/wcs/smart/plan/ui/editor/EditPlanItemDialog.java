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
package org.wcs.smart.plan.ui.editor;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.PlanEventManager;
import org.wcs.smart.plan.PlanHibernateManager;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.panel.IInputChangeListener;
import org.wcs.smart.plan.ui.panel.PlanComposite;
import org.wcs.smart.plan.ui.panel.PlanCompositeFactory;
import org.wcs.smart.plan.ui.panel.PlanCompositeFactory.PanelType;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * EditPlanItemDialog
 * 
 * @author jeffloun
 * @since 1.0.0
 */
public class EditPlanItemDialog extends AbstractPropertyJHeaderDialog {

	protected PlanComposite content;
	
	private PanelType panelType;
	private Plan plan;

	IInputChangeListener inputChangeListener = new IInputChangeListener() {			
		@Override
		public void inputChanged() {
			setChangesMade(true);
			EditPlanItemDialog.this.setErrorMessage(content.getErrorMessage());
			if (getButton(IDialogConstants.OK_ID) != null){
				getButton(IDialogConstants.OK_ID).setEnabled(content.isDataValid());
			}
		}
	};
	
	/**
	 * @param parent
	 * @param panelType
	 * @param plan
	 */
	public EditPlanItemDialog(Shell parent, PanelType panelType, Plan plan) {
		super(parent, PlanCompositeFactory.getInstance().getTitle(panelType));
		this.panelType = panelType;
		this.plan = plan;
	}

	@Override
	protected Composite createContent(Composite parent) {
		content = PlanCompositeFactory.getInstance().createComposite(parent, SWT.NONE, panelType);
		content.initFromModel(plan);
		content.addInputChangeListener(inputChangeListener);
		setTitle(content.getTitle());
		setMessage(content.getMessage());
		return content;
	}

	@Override
	protected boolean performSave() {
		content.updateModel(plan);
		boolean saved = false;
		try(Session session = HibernateManager.openSession()){
			saved = PlanHibernateManager.savePlan(plan, session);
		}
		if (saved) {
			setChangesMade(false);
			PlanEventManager.getInstance().planChanged(0, plan);
			return true;
		}
		return false;
	}
	
}

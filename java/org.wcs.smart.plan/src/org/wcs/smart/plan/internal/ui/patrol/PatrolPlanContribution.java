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
package org.wcs.smart.plan.internal.ui.patrol;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;

/**
 * Contribution item for the patrol editor.
 * 
 * @author Emily
 *
 */
public class PatrolPlanContribution implements IPatrolEditorContribution {

	private Label lblPlanId;
	private Label lblPlanName;
	private Label lblPlanDescription;
	
	public PatrolPlanContribution() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public String getName() {
		return "Plan";
	}

	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent) {
		Composite main = toolkit.createComposite(parent);
		
		main.setLayout(new GridLayout(2, false));
		toolkit.createLabel(main, "Plan ID:");
		lblPlanId = toolkit.createLabel(main, "");
		
		toolkit.createLabel(main, "Plan Name:");
		lblPlanName = toolkit.createLabel(main, "");
		
		toolkit.createLabel(main, "Plan Description:");
		lblPlanDescription = toolkit.createLabel(main, "");
		return main;
	}

	@Override
	public void setPatrol(Patrol patrol) {
		//TODO: find patrol and put info in there
		
		lblPlanId.setText("NONE");
//		lblPlanId.setText(patrol.getId());
//		lblPlanName.setText(patrol.get)
		
	}

}

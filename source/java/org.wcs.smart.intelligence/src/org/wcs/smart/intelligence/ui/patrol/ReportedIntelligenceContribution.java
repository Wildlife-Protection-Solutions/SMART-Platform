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
package org.wcs.smart.intelligence.ui.patrol;

import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.IPatrolEditorContribution;

/**
 * Intelligence contribution item for the patrol editor.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class ReportedIntelligenceContribution implements IPatrolEditorContribution {

	private Label lblName;
	private Label lblDescription;

	public ReportedIntelligenceContribution() {}

	@Override
	public Composite createControl(FormToolkit toolkit, Composite parent) {
		Composite main = toolkit.createComposite(parent);
		main.setLayout(new GridLayout(2, false));
		
		toolkit.createLabel(main, "Intelligence name:");
		lblName = toolkit.createLabel(main, "");
		
		toolkit.createLabel(main, "Intelligence Description:");
		lblDescription = toolkit.createLabel(main, "");
		return main;
	}

	@Override
	public String getName() {
		return "Reported Intelligence";
	}

	@Override
	public void setPatrol(Patrol patrol) {
		return;
		// TODO Auto-generated method stub
	}

}

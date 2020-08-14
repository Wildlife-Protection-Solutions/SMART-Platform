/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.plan.xml.patrol;

import java.util.HashMap;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.patrol.xml.external.IPatrolExportContribution;
import org.wcs.smart.plan.internal.Messages;

/**
 * Export plan definition option to add to the patrol
 * export dialog
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class IncludeDefOp implements IPatrolExportContribution {

	private Button btnDef;
	private boolean selection = true;
	
	@Override
	public void createControls(Composite comp) {
		Label l = new Label(comp, SWT.NONE);
		l.setText(Messages.IncludeDefOp_IncludeOp);
		l.setToolTipText(Messages.IncludeDefOp_Tooltip);
		
		btnDef = new Button(comp, SWT.CHECK);
		btnDef.setSelection(selection);
		btnDef.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		btnDef.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				selection = btnDef.getSelection();		
			}
		});
	}

	@Override
	public HashMap<Object, Object> getOptions() {
		HashMap<Object,Object> map = new HashMap<>();
		map.put(PatrolPlanXmlExtraDataContribution.INCLUDE_PLAN_DEF, selection);
		return map;
	}

	@Override
	public String validate() {
		return null;
	}

}

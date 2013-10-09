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
package org.wcs.smart.patrol.meta;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.wcs.smart.ca.ScreenOption;

/**
 * Screen armed option configuration panel.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ArmedScreenOptionComposite extends ScreenOptionComposite {

	public ArmedScreenOptionComposite(Composite parent, ScreenOption model) {
		super(parent);
		new BooleanScreenOptionGroup(this, model);
	}

	private class BooleanScreenOptionGroup extends ScreenOptionGroup {

		private Button btnArmed;
		
		public BooleanScreenOptionGroup(Composite parent, ScreenOption option) {
			super(parent, option);
		}

		@Override
		protected void createDefaultControl(Group group) {
			btnArmed = new Button(group, SWT.CHECK);
			btnArmed.setText("Armed?");
			btnArmed.setEnabled(!getModel().isVisible());
			btnArmed.setSelection(Boolean.TRUE.equals(getModel().getBooleanValue()));
			btnArmed.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					getModel().setBooleanValue(btnArmed.getSelection());
					fireScreenOptionListeners();
				}
			});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean visible = getBtnDisplayPage().getSelection();
			getModel().setVisible(visible);
			btnArmed.setEnabled(!visible);
			fireScreenOptionListeners();
		}
		
	}
}

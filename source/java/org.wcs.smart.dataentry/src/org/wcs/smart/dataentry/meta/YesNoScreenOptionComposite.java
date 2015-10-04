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
package org.wcs.smart.dataentry.meta;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.ScreenOption;

/**
 * Screen armed option configuration panel.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class YesNoScreenOptionComposite extends ScreenOptionComposite {

	public YesNoScreenOptionComposite(Composite parent, ScreenOption model, String title) {
		super(parent);
		new BooleanScreenOptionGroup(this, model, title);
	}

	private class BooleanScreenOptionGroup extends ScreenOptionGroup {

		private Button btnYes;
		private Button btnNo;
		
		public BooleanScreenOptionGroup(Composite parent, ScreenOption option, String title) {
			super(parent, option, title);
		}

		@Override
		protected void createDefaultControl(Group group) {
			Composite container = new Composite(group, SWT.NONE);
			GridLayout gd = new GridLayout(2, false);
			gd.marginBottom=0;
			gd.marginHeight = 0;
			gd.marginLeft = 0;
			gd.marginRight = 0;
			gd.marginTop = 0;
			gd.marginWidth = 0;
			container.setLayout(gd);
			container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			
			btnYes = new Button(container, SWT.RADIO);
			btnYes.setText(Messages.YesNoScreenOptionComposite_Yes);
			btnYes.setEnabled(!getModel().isVisible());
			btnYes.setSelection(Boolean.TRUE.equals(getModel().getBooleanValue()));
			btnYes.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					getModel().setBooleanValue(true);
					fireScreenOptionListeners();
				}
			});
			
			btnNo = new Button(container, SWT.RADIO);
			btnNo.setText(Messages.YesNoScreenOptionComposite_No);
			btnNo.setEnabled(!getModel().isVisible());
			btnNo.setSelection(!Boolean.TRUE.equals(getModel().getBooleanValue()));
			btnNo.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					getModel().setBooleanValue(false);
					fireScreenOptionListeners();
				}
			});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean visible = getBtnDisplayPage().getSelection();
			getModel().setVisible(visible);
			btnYes.setEnabled(!visible);
			btnNo.setEnabled(!visible);
			fireScreenOptionListeners();
		}
		
	}
}

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
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.dataentry.meta.ScreenOptionComposite;
import org.wcs.smart.dataentry.model.ScreenOption;

/**
 * Screen option with text control for default value.
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class TextScreenOptionComposite extends ScreenOptionComposite {

	/**
	 * @param parent
	 */
	public TextScreenOptionComposite(Composite parent, ScreenOption model, String title) {
		super(parent);
		TextScreenOptionGroup grp = new TextScreenOptionGroup(this, model, title);
		grp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
	}

	private class TextScreenOptionGroup extends ScreenOptionGroup {

		private Text text;

		public TextScreenOptionGroup(Composite parent, ScreenOption option, String title) {
			super(parent, option, title);
		}

		@Override
		protected void createDefaultControl(Group group) {
			new Label(group, SWT.NONE);
			text = new Text(group, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
			GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true, 2, 1);
//			gd.widthHint = 150;
//			gd.heightHint = 80;
			String value = getModel().getStringValue();
			text.setText(value != null ? value : ""); //$NON-NLS-1$
			text.setLayoutData(gd);
			text.setEnabled(!getModel().isVisible());
			text.addModifyListener(new ModifyListener() {
				@Override
				public void modifyText(ModifyEvent e) {
					getModel().setStringValue(text.getText());
					fireScreenOptionListeners();
				}
			});
		}

		@Override
		protected void onBtnDisplayPageClick() {
			boolean visible = getBtnDisplayPage().getSelection();
			getModel().setVisible(visible);
			text.setEnabled(!visible);
			fireScreenOptionListeners();
		}

	}
}

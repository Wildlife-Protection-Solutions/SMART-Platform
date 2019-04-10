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
package org.wcs.smart.observation.ui.input;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ui.SmartWizardDialog;

/**
 * Extension to the wizard dialog and disables
 * the default ESC function and provides
 * access to the next button for focus.
 * 
 * @author egouge
 *
 */
public class ObservationWizardDialog extends SmartWizardDialog {

	public ObservationWizardDialog(Shell parentShell, IWizard newWizard) {
		super(parentShell, newWizard);

	}

	/**
	 * Removes the ESC traverse functionality
	 */
	@Override
	public void create() {
		super.create();
		getShell().addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE) {
					e.doit = false;
				}
			}
		});
	}

	/**
	 * Sets a maximum width of 700
	 */
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		
		p.x = Math.min(p.x, 800);
		p.y = Math.min(p.y, 700);
		return p;
	}
	
	/**
	 * Attempts to set focus on the next button of the 
	 * wizard dialog.
	 */
	public void setNextFocus() {
		Button btn = getButton(IDialogConstants.NEXT_ID);
		if (btn != null) {
			btn.setFocus();
		}
	}
	
	/**
	 * Overwrite to keep default button setting
	 */
	@Override
	public void updateButtons() {
		Button btn = getShell().getDefaultButton();
		super.updateButtons();
		getShell().setDefaultButton(btn);
	}
}

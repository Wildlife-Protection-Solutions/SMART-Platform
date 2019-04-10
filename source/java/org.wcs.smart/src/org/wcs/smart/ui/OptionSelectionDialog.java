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
package org.wcs.smart.ui;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;

/**
 * 
 * Dialog for selecting an option from provided list.
 * Contains blank title and message.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class OptionSelectionDialog extends SmartStyledDialog {
	private TableViewer fTableViewer;
	private String[] options;
	private IStructuredSelection selection;
	private String title;
	private String message;
	
	/**
	 * @param parentShell
	 */
	public OptionSelectionDialog(Shell shell, String[] options, String title, String message) {
		super(shell);
		this.options = options;
		this.title = title;
		this.message = message;
	}

	@Override
	protected void configureShell(Shell shell) {
		super.configureShell(shell);
		shell.setText(title);
	}
	
	@Override
	protected void okPressed() {
		this.selection = (IStructuredSelection) fTableViewer.getSelection();
		super.okPressed();
	}
	
	public ISelection getSelection(){
		return selection;
	}

	public String getSelectedOption() {
		return (String) ((IStructuredSelection)getSelection()).getFirstElement();
	}
	
	@Override
	protected Point getInitialSize() {
		Point p = super.getInitialSize();
		if (p.x > 500) {
			p.x = 500;
		}
		if (p.y < 400) {
			p.y = 400;
		}
		return p;
	}

	@Override
	protected void createButtonsForButtonBar(Composite parent) {
		// create OK and Cancel buttons by default
		createButton(parent, IDialogConstants.OK_ID,
				IDialogConstants.OK_LABEL, true);
		createButton(parent, IDialogConstants.CANCEL_ID,
				IDialogConstants.CANCEL_LABEL, false);
	}

	private Image getWarningIcon() {
		Shell shell = getShell();
		final Display display;
		if (shell == null || shell.isDisposed()) {
			shell = getParentShell();
		}
		if (shell == null || shell.isDisposed()) {
			display = Display.getCurrent();
			Assert.isNotNull(display, "The dialog should be created in UI thread"); //$NON-NLS-1$
		} else {
			display = shell.getDisplay();
		}

		final Image[] image = new Image[1];
		display.syncExec(new Runnable() {
			public void run() {
				image[0] = display.getSystemImage(SWT.ICON_WARNING);
			}
		});

		return image[0];
	}

	@Override
	protected Control createDialogArea(Composite container) {

		Composite parent = (Composite) super.createDialogArea(container);
		GridLayout gl = new GridLayout(1, false);
		int margin = 15;
		gl.marginHeight = margin;
		gl.marginWidth = margin;

		parent.setLayout(gl);

		Composite header = new Composite(parent, SWT.NONE);
		header.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false));
		header.setLayout(new GridLayout(2, false));
		Label imageLabel = new Label(header, SWT.NULL);
		imageLabel.setImage(getWarningIcon());
		imageLabel.setLayoutData(new GridData(SWT.LEFT,
				SWT.CENTER, false, false));

		Label messageLabel = new Label(header, SWT.WRAP);
		messageLabel.setText(message);
		messageLabel.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

		fTableViewer = new TableViewer(parent,
				SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		fTableViewer.setContentProvider(ArrayContentProvider.getInstance());
		fTableViewer.setLabelProvider(new LabelProvider());
		
		fTableViewer.setInput(options);
		fTableViewer.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		fTableViewer.setSelection(new StructuredSelection(options[0]));
		return parent;
	}

};

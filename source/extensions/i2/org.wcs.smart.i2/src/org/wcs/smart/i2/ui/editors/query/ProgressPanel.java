/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.query;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.wcs.smart.i2.internal.Messages;

/**
 * Query results progress panel that contains a progress bar and label.
 * 
 * @author Emily
 *
 */
public class ProgressPanel extends Composite {

	private ProgressBar pbar;
	private Label progressLabel;
	private Button btnCancel;
	
	private IProgressMonitor currentMonitor;
	
	public ProgressPanel(Composite parent) {
		super(parent, SWT.NONE);
		
		setLayout(new GridLayout(2, false));
		
		pbar = new ProgressBar(this, SWT.HORIZONTAL | SWT.SMOOTH);
		pbar.setMinimum(0);
		pbar.setMaximum(100);
		pbar.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		btnCancel = new Button(this, SWT.PUSH);
		btnCancel.setText(Messages.ProgressPanel_CancelBtn);
		btnCancel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnCancel.addListener(SWT.Selection, event->{
			if (currentMonitor != null){
				currentMonitor.setCanceled(true);
			}
		});
		
		progressLabel = new Label(this, SWT.NONE);
		progressLabel.setText(""); //$NON-NLS-1$
		progressLabel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		
		
	}

	/**
	 * Set the current progress monitor; required for cancel feature
	 * @param currentMonitor
	 */
	public void setProgressMonitor(IProgressMonitor currentMonitor){
		this.currentMonitor = currentMonitor;
	}
	
	public void setLabel(String label){
		this.progressLabel.setText(label);
	}
	
	/**
	 * 
	 * @param value value between 0 and 100 representing the progress
	 */
	public void setProgress(int value){
		pbar.setSelection(value);
	}
	
	/**
	 * clears message and sets progres to 0
	 */
	public void clear(){
		setProgress(0);
		setLabel(""); //$NON-NLS-1$
		setProgressMonitor(null);
		layout(true);
	}
	
	/**
	 * clears messages and set progress to 100%
	 */
	public void done(){
		setProgress(pbar.getMaximum());
		setLabel(""); //$NON-NLS-1$
		setProgressMonitor(null);
		layout(true);
	}
}

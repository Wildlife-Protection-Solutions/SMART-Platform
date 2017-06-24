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
package org.wcs.smart.common.control;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.ui.forms.widgets.FormToolkit;


/**
 * A composite that contains a progress area for
 * display query progress when executing a query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class ProgressAreaComposite extends Composite {

	private Composite outer;
	private Label lblStatus;
	private ProgressBar progresBar;
	private Button btnCancel;
	
	private IProgressMonitor internalMonitor = null;

	private String cancelledMessage;
	private String cancellingMessage;
	
	/**
	 * @param parent
	 * @param style
	 */
	public ProgressAreaComposite(Composite parent) {
		super(parent, SWT.NONE);
		createComposite();
		this.cancelledMessage="Task Cancelled";
		this.cancellingMessage = "Stopping Task";
	}
	
	/**
	 * 
	 * @param cancelledMessage the message displayed to the user once the task has been cancelled
	 * @param cancellingMessage the message displayed to the user after the cancel request but before the task finished cancelled
	 */
	public void setCancelMessages(String cancelledMessage, String cancellingMessage){
		this.cancelledMessage = cancelledMessage;
		this.cancellingMessage = cancellingMessage;
	}
	public void adapt(FormToolkit toolkit){
		toolkit.adapt(this);
		toolkit.adapt(outer);
		toolkit.adapt(lblStatus, false, true);
		toolkit.adapt(progresBar, false, false);
		toolkit.adapt(btnCancel, false, true );
	}
	
	private void createComposite(){
		GridLayout gl = new GridLayout(1, false);
		gl.marginLeft = 100;
		gl.marginRight = 100;
//		gl.marginBottom = 50;
		this.setLayout(gl);
		this.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
		outer = new Composite(this, SWT.NONE);
		gl = new GridLayout(2, false);
//		gl.verticalSpacing = 15;
		outer.setLayout(gl);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
		lblStatus = new Label(outer, SWT.NONE);
		lblStatus.setText(""); //$NON-NLS-1$
		lblStatus.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
			
		// main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		progresBar = new ProgressBar(outer, SWT.SMOOTH | SWT.HORIZONTAL);
		progresBar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
		btnCancel = new Button(outer, SWT.NONE);
		btnCancel.setText(IDialogConstants.CANCEL_LABEL);
		btnCancel.setEnabled(true);
		btnCancel.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				lblStatus.setText(cancellingMessage);
				internalMonitor.setCanceled(true);
			}
		});
			
	}
	/**
	 * @see org.eclipse.swt.widgets.Control#setEnabled(boolean)
	 */
	public void setEnabled(boolean enabled){
		progresBar.setEnabled(enabled);
		btnCancel.setEnabled(enabled);
		lblStatus.setEnabled(enabled);
	}

	/**
	 * Updates the state of the progress area to a cancelled state.
	 */
	public void showCancelled(){
		lblStatus.setText(cancelledMessage);
		progresBar.setEnabled(false);
		progresBar.setSelection(0);
		btnCancel.setEnabled(false);
	}

	/**
	 * @return creates a new progress monitor
	 * for displaying the progress in the progress
	 * area.
	 * 
	 * Only one progress monitor should be created
	 * at a time.  
	 * 
	 */
	public IProgressMonitor createProgressMonitor(){
		
		internalMonitor =  new IProgressMonitor() {
				private String taskName = null;
				private boolean isCancelled = false;
				
				@Override
				public void worked(int work) {
					internalWorked(work);
				}

				@Override
				public void subTask(final String name) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (lblStatus.isDisposed()){
								return;
							}
							lblStatus.setText(taskName + " - " + name); //$NON-NLS-1$
						}
					});
				}

				@Override
				public void setTaskName(String name) {
					this.taskName = name;
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (lblStatus.isDisposed()){
								return;
							}
							lblStatus.setText(taskName);
						}
					});
				}

				@Override
				public void setCanceled(boolean value) {
					this.isCancelled = value;
				}

				@Override
				public boolean isCanceled() {
					return isCancelled;
				}

				@Override
				public void internalWorked(final double work) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (progresBar.isDisposed()){
								return;
							}
							int newValue = progresBar.getSelection() + (int)work;
							if (newValue > progresBar.getMaximum()) {
								newValue = progresBar.getMaximum();
							}
							if (newValue < progresBar.getMinimum()) {
								newValue = progresBar.getMinimum();
							}
							progresBar.setSelection(newValue);
						}
					});
				}

				@Override
				public void done() {
				}

				@Override
				public void beginTask(final String name, final int totalWork) {
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							if (progresBar.isDisposed()){
								return;
							}
							setEnabled(true);
							progresBar.setMinimum(0);
							progresBar.setMaximum(totalWork);
							progresBar.setSelection(0);
							setTaskName(name);

						}
					});

				}
			};
			
			return internalMonitor;
	}
	
}

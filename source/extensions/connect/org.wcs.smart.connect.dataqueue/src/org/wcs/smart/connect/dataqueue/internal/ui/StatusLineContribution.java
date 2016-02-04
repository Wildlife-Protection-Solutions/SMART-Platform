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
package org.wcs.smart.connect.dataqueue.internal.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.wcs.smart.connect.dataqueue.internal.process.AutoProcessingManager;
import org.wcs.smart.connect.dataqueue.internal.process.AutoProcessingStatus;
import org.wcs.smart.connect.ui.IConnectStatusContribution;

/**
 * Status line contribution for displaying the status of the data queue processor.
 * @author Emily
 *
 */
public class StatusLineContribution implements IConnectStatusContribution {

	private Label statusLabel;

	private AutoProcessingManager.IProcessingStatusListener listener = new AutoProcessingManager.IProcessingStatusListener() {
		
		@Override
		public void statusModified(AutoProcessingStatus lastStatus) {
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					updateControl(lastStatus);		
				}});
			
		}
	};
	
	@Override
	public void refresh(){
		updateControl(AutoProcessingManager.INSTANCE.getLastStatus());
	}
	
	@Override
	public Control createControl(Composite parent){
		statusLabel = new Label(parent, SWT.NONE);
		
		AutoProcessingManager.INSTANCE.addStatusListener(listener);
		statusLabel.addDisposeListener(new DisposeListener() {
			@Override
			public void widgetDisposed(DisposeEvent e) {
				AutoProcessingManager.INSTANCE.removeStatusListener(listener);
			}
		});
		refresh();
		return statusLabel;
	}
	
	private void updateControl(AutoProcessingStatus lastStatus){
		statusLabel.setImage(lastStatus.getStatus().getImage());
		String message = lastStatus.getMessage();
		if (message == null) message = "";
		statusLabel.setToolTipText(message);
	}
	
}

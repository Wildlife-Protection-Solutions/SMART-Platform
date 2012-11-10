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
package org.wcs.smart.patrol.internal.ui.editor;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.ui.observation.ObservationWizard;
import org.wcs.smart.patrol.internal.ui.observation.ObservationWizardDialog;
import org.wcs.smart.patrol.model.Waypoint;

/**
 * Editor for editing the patrol observation table cell.
 * <p>Opens the patrol observation entry wizard.</p>
 * @author Emily
 * @since 1.0.0
 */
public class ObservationCellEditor extends DialogCellEditor{

	private ObservationWizardDialog dialog;
	
	public ObservationCellEditor(Composite parent){
		super(parent);
	}

	@Override
	protected Button createButton(final Composite parent) {
		Button result = super.createButton(parent);
		result.addListener(SWT.Traverse, new Listener() {
			public void handleEvent(Event e) {
				getControl().notifyListeners(SWT.Traverse, e);
			}
		});
		return result;
	}
	
	@Override
	protected void fireApplyEditorValue() {
		super.fireApplyEditorValue();
		getControl().getParent().setFocus();
		getControl().getShell().setDefaultButton(null);
	}
	
	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#openDialogBox(org.eclipse.swt.widgets.Control)
	 */
	@Override
	protected Object openDialogBox(Control cellEditorWindow) {
		Waypoint wp = (Waypoint)super.getValue();

		final ObservationWizard wizard = new ObservationWizard(wp);
		final Shell shell = super.getControl().getShell();
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
			pmd.run(false, false, new IRunnableWithProgress() {

				@Override
				public void run(IProgressMonitor monitor)
						throws InvocationTargetException, InterruptedException {
					monitor.setTaskName("Loading Wizard");
					dialog = new ObservationWizardDialog(shell, wizard);
					wizard.setWizardDialog(dialog);

				}
			});
		} catch (Exception ex) {
			dialog = null;
			SmartPatrolPlugIn.displayLog("Error loading new patrol wizard. "
					+ ex.getMessage(), ex);
		}
		if (dialog != null) {
			if (dialog.open() == Window.CANCEL){
				return null;
			}
		}
		
		return wp;
	}
	
	/**
	 * Updates the size of the widget
	 */
	@Override
	public LayoutData getLayoutData() {
		LayoutData data = super.getLayoutData();
		data.minimumHeight = getDefaultLabel().computeSize(SWT.DEFAULT, SWT.DEFAULT).y;
		return data;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.DialogCellEditor#updateContents(java.lang.Object)
	 */
	@Override
	 protected void updateContents(Object value) {
		super.updateContents(value);
		if (value == null){
			return;
		}

		String text = "(None)";
		if ( ((Waypoint)value).getObservations() != null && ((Waypoint)value).getObservations().size() > 0){
			if ( ((Waypoint)value).getObservations().size() == 1){
				text = ((Waypoint)value).getObservations().get(0).getCategory().getName();
			}else{
				text = ((Waypoint)value).getObservations().size() + " Observations";
			}
		}
		getDefaultLabel().setText( text);  
    }
	
}

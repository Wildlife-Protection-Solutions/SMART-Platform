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
package org.wcs.smart.dataentry.dialog.composite;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.dataentry.DisplayModeLabelProvider;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.DisplayMode;

/**
 * {@link ComboViewer} that is intended to provide selection for {@link DisplayMode}
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class DisplayModeComboViewer extends Composite {
	
	private ComboViewer cmbViewer;
	private Button btnCascade;
	
	public DisplayModeComboViewer(Composite parent) {
		this(parent, true);
	}
	
	public DisplayModeComboViewer(Composite parent, boolean includeCascade) {
		super(parent, SWT.READ_ONLY);
		
		setLayout(new GridLayout(includeCascade ? 2 : 1, false));
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		cmbViewer = new ComboViewer(this, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbViewer.setContentProvider(ArrayContentProvider.getInstance());
		cmbViewer.setLabelProvider(new DisplayModeLabelProvider());
		cmbViewer.setInput(DisplayMode.values());
		cmbViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (includeCascade) {
			btnCascade = new Button(this, SWT.PUSH);
			btnCascade.setBackground(getDisplay().getSystemColor(SWT.COLOR_TRANSPARENT));
			btnCascade.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
			btnCascade.setText(Messages.DisplayModeComboViewer_cascadeButton);
			btnCascade.setToolTipText(Messages.DisplayModeComboViewer_cascadeTooltip);	
		}
		
	}
	
	public void addDisplayModeChangeListener(Listener onChange) {
		cmbViewer.getControl().addListener(SWT.Selection, onChange);
	}
	public void addCascadeListener(Listener onCascade) {
		btnCascade.addListener(SWT.Selection, onCascade);
	}
	public void setSelection(IStructuredSelection selection) {
		cmbViewer.setSelection(selection);
	}
	
	public DisplayMode getSelectedDisplayMode() {
		IStructuredSelection selection = cmbViewer.getStructuredSelection();
		if (!selection.isEmpty() && selection.getFirstElement() instanceof DisplayMode) {
			return (DisplayMode) selection.getFirstElement();
		}
		
		return DisplayMode.DEFAULT_DISPLAY_MODE;
	}
}

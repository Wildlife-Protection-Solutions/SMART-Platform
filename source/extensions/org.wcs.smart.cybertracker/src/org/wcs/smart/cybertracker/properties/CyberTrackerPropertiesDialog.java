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
package org.wcs.smart.cybertracker.properties;

import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The CyberTracker property dialog for managing 
 * CyberTracker application default properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerPropertiesDialog extends AbstractPropertyJHeaderDialog {
	
	private static Integer[] CT_GTM_VALUES = {
		-1200,
		-1100,
		-1000,
		-900,
		-800,
		-700,
		-600,
		-500,
		-430,
		-400,
		-330,
		-300,
		-200,
		-100,
		0,
		100,
		200,
		300,
		400,
		430,
		500,
		530,
		545,
		600,
		630,
		700,
		800,
		900,
		930,
		1000,
		1030,
		1100,
		1130,
		1200,
		1300,		
	};

	private Button btnKioskMode;
	private Text txtTrackTimer;
    private ComboViewer timeOffset;
	
	
	public CyberTrackerPropertiesDialog() {
		super(Display.getCurrent().getActiveShell(), "CyberTracker Default Properties");
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		Label lblKioskMode = new Label(container, SWT.NONE);
		lblKioskMode.setText("Kiosk Mode:");

		btnKioskMode = new Button(container, SWT.CHECK);
		
		Label lblTrackTimer = new Label(container, SWT.NONE);
		lblTrackTimer.setText("Track Timer:");

		txtTrackTimer = new Text(container, SWT.BORDER);

		Label lblTimeOffset = new Label(container, SWT.NONE);
		lblTimeOffset.setText("GMT/UTC time offset:");

		timeOffset = new ComboViewer(container, SWT.READ_ONLY);
		timeOffset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		timeOffset.setContentProvider(ArrayContentProvider.getInstance());
		timeOffset.setLabelProvider(new CyberTrackerGTMLabelProvider());
 		timeOffset.setInput(CT_GTM_VALUES);
		timeOffset.setSelection(new StructuredSelection(0));
		
		setTitle("CyberTracker Default Properties");
		setMessage("Default properties that will be applied to all created CyberTracker applications");
		
		return container;
	}

	@Override
	protected boolean performSave() {
		// TODO Auto-generated method stub
		return true;
	}


	private class CyberTrackerGTMLabelProvider extends LabelProvider {
		@Override
		public String getText(Object element) {
			if (element instanceof Integer) {
				int x = (Integer) element;
				int val = Math.abs(x);
				boolean positive = x >= 0;
				String s = "GTM"; //$NON-NLS-1$
				int hour = val/100;
				int min = val%100;
				if (val != 0) {
					s += positive ? " + " : " - "; //$NON-NLS-1$ //$NON-NLS-2$
					if (hour < 10)
						s += "0"; //$NON-NLS-1$
					s += String.valueOf(hour);
					if (min == 0)
						s += ":00"; //$NON-NLS-1$
					else
						s += ":"+min; //$NON-NLS-1$
				}
				return s;
			}
			return super.getText(element);
		}
	}
}

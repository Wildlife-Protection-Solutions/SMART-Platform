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
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.model.CyberTrackerProperties;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.properties.AbstractPropertyJHeaderDialog;

/**
 * The CyberTracker property dialog for managing 
 * CyberTracker application default properties
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerPropertiesDialog extends AbstractPropertyJHeaderDialog {

	private static final Integer CT_TIME_TRACK_MIN_VALUE = 0;
	private static final Integer CT_TIME_TRACK_MAX_VALUE = 1000;
	
	private static final Integer[] CT_GTM_VALUES = {
		-1200,
		-1100,
		-1000,
		-900,
		-800,
		-700,
		-600,
		-500,
		-450,
		-400,
		-350,
		-300,
		-200,
		-100,
		0,
		100,
		200,
		300,
		400,
		450,
		500,
		550,
		575,
		600,
		650,
		700,
		800,
		900,
		950,
		1000,
		1050,
		1100,
		1150,
		1200,
		1300,		
	};

	private CyberTrackerProperties ctProperties;
	
	private Text txtAppName;
	private Button btnKioskMode;
	private Text txtTrackTimer;
    private ComboViewer timeOffset;
	
	
	public CyberTrackerPropertiesDialog() {
		super(Display.getCurrent().getActiveShell(), "CyberTracker Default Properties");
		Session session = HibernateManager.openSession();
		try {
			ctProperties = CyberTrackerHibernateManager.getProperties(session);
		} finally {
			session.close();
		}
		if (ctProperties == null)
			ctProperties = new CyberTrackerProperties();
	}

	@Override
	protected Composite createContent(Composite parent) {
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(2, false));

		Label lblAppName = new Label(container, SWT.NONE);
		lblAppName.setText("Application Name:");

		txtAppName = new Text(container, SWT.BORDER);
		txtAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (ctProperties.getApplicationName() != null)
			txtAppName.setText(ctProperties.getApplicationName());
		txtAppName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
			}
		});
		
		Label lblKioskMode = new Label(container, SWT.NONE);
		lblKioskMode.setText("Kiosk Mode:");

		btnKioskMode = new Button(container, SWT.CHECK);
		btnKioskMode.setSelection(Boolean.TRUE.equals(ctProperties.getKioskMode()));
		btnKioskMode.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		Label lblTrackTimer = new Label(container, SWT.NONE);
		lblTrackTimer.setText("Track Timer:");

		txtTrackTimer = new Text(container, SWT.BORDER);
		txtTrackTimer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (ctProperties.getWaypointTimer() != null)
			txtTrackTimer.setText(String.valueOf(ctProperties.getWaypointTimer()));
		txtTrackTimer.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				setChangesMade(true);
			}
		});

		Label lblTimeOffset = new Label(container, SWT.NONE);
		lblTimeOffset.setText("GMT/UTC time offset:");

		timeOffset = new ComboViewer(container, SWT.READ_ONLY);
		timeOffset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		timeOffset.setContentProvider(ArrayContentProvider.getInstance());
		timeOffset.setLabelProvider(new CyberTrackerGTMLabelProvider());
 		timeOffset.setInput(CT_GTM_VALUES);
		if (ctProperties.getGpsTimeZone() != null)
			timeOffset.setSelection(new StructuredSelection(ctProperties.getGpsTimeZone()));
		timeOffset.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setChangesMade(true);
			}
		});
		
		setTitle("CyberTracker Default Properties");
		setMessage("Default properties that will be applied to all created CyberTracker applications");
		
		return container;
	}

	boolean validateTrackTimer(String value) {
		try {
			Integer result = Integer.valueOf(value);
			return result >= CT_TIME_TRACK_MIN_VALUE && result <= CT_TIME_TRACK_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
		
	}
	
	@Override
	protected boolean performSave() {
		if (!validateTrackTimer(txtTrackTimer.getText())) {
			return false;
		}
		ctProperties.setApplicationName(txtAppName.getText());
		ctProperties.setKioskMode(btnKioskMode.getSelection());
		ctProperties.setWaypointTimer(Integer.valueOf(txtTrackTimer.getText()));
		StructuredSelection selection = (StructuredSelection) timeOffset.getSelection();
		ctProperties.setGpsTimeZone((Integer)selection.getFirstElement());
		
		Session session = HibernateManager.openSession();
		try {
			CyberTrackerHibernateManager.saveProperties(ctProperties, session);
			setChangesMade(false);
			return true;
		} catch (Exception e) {
			SmartPlugIn.displayLog(getShell(), "Error occured while trying to save CyberTracker properties", e);
			return false;
		}finally {
			session.close();
		}
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
					switch (min) {
					case 0:
						s += ":00"; //$NON-NLS-1$
						break;
					case 50:
						s += ":30"; //$NON-NLS-1$
						break;
					case 75:
						s += ":45"; //$NON-NLS-1$
						break;
					default:
						break;
					}
				}
				return s;
			}
			return super.getText(element);
		}
	}
}

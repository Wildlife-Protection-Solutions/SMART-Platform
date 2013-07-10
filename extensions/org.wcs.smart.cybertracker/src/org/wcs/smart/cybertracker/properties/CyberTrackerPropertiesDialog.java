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

import java.text.MessageFormat;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
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
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
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

	private CyberTrackerProperties ctProperties;
	
	private Text txtAppName;
	private Button btnAutoNext;
	private Button btnKioskMode;
	private Text txtTrackTimer;
    private ComboViewer timeOffset;
    private Text txtStorageTime;
	
    private ControlDecoration appNameDecoration;
    private ControlDecoration trackTimerDecoration;
    private ControlDecoration storageTimeDecoration;
	
	public CyberTrackerPropertiesDialog() {
		super(Display.getCurrent().getActiveShell(), Messages.CyberTrackerPropertiesDialog_Title);
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
		lblAppName.setText(Messages.CyberTrackerPropertiesDialog_AppName);

		txtAppName = new Text(container, SWT.BORDER);
		txtAppName.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (ctProperties.getApplicationName() != null)
			txtAppName.setText(ctProperties.getApplicationName());
		txtAppName.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isAppNameValid()) {
					appNameDecoration.hide();
				} else {
					appNameDecoration.show();
				}
				setChangesMade(true);
			}
		});

		appNameDecoration = new ControlDecoration(txtAppName, SWT.LEFT);
		appNameDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		appNameDecoration.setShowHover(true);
		appNameDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_NameInvalid, CyberTrackerProperties.APPLICATION_NAME_MAX_LENTH));
		appNameDecoration.hide();

		Label lblAutoNext = new Label(container, SWT.NONE);
		lblAutoNext.setText(Messages.CyberTrackerPropertiesDialog_AutoNext);

		btnAutoNext = new Button(container, SWT.CHECK);
		btnAutoNext.setSelection(ctProperties.isAutoNext());
		btnAutoNext.addSelectionListener(new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setChangesMade(true);
			}
			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing
			}
		});
		
		Label lblKioskMode = new Label(container, SWT.NONE);
		lblKioskMode.setText(Messages.CyberTrackerPropertiesDialog_KioskMode);

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
		lblTrackTimer.setText(Messages.CyberTrackerPropertiesDialog_TrackTimer);

		txtTrackTimer = new Text(container, SWT.BORDER);
		txtTrackTimer.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (ctProperties.getWaypointTimer() != null)
			txtTrackTimer.setText(String.valueOf(ctProperties.getWaypointTimer()));
		txtTrackTimer.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isTrackTimerValid()) {
					trackTimerDecoration.hide();
				} else {
					trackTimerDecoration.show();
				}
				setChangesMade(true);
			}
		});

		trackTimerDecoration = new ControlDecoration(txtTrackTimer, SWT.LEFT);
		trackTimerDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		trackTimerDecoration.setShowHover(true);
		trackTimerDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_TrackTimerInvalid, CyberTrackerProperties.TIME_TRACK_MIN_VALUE, CyberTrackerProperties.TIME_TRACK_MAX_VALUE));
		trackTimerDecoration.hide();

		Label lblTimeOffset = new Label(container, SWT.NONE);
		lblTimeOffset.setText(Messages.CyberTrackerPropertiesDialog_TimeOffset);

		timeOffset = new ComboViewer(container, SWT.READ_ONLY);
		timeOffset.getControl().setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		timeOffset.setContentProvider(ArrayContentProvider.getInstance());
		timeOffset.setLabelProvider(new CyberTrackerGTMLabelProvider());
 		timeOffset.setInput(CyberTrackerProperties.GTM_VALUES);
		if (ctProperties.getGpsTimeZone() != null)
			timeOffset.setSelection(new StructuredSelection(ctProperties.getGpsTimeZone()));
		timeOffset.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				setChangesMade(true);
			}
		});

		
		Label lblStorageTime = new Label(container, SWT.NONE);
		lblStorageTime.setText(Messages.CyberTrackerPropertiesDialog_StorageTime);

		txtStorageTime = new Text(container, SWT.BORDER);
		txtStorageTime.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		txtStorageTime.setText(String.valueOf(ctProperties.getStorageTime()));
		txtStorageTime.addModifyListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				if (isStorageTimeValid()) {
					storageTimeDecoration.hide();
				} else {
					storageTimeDecoration.show();
				}
				setChangesMade(true);
			}

		});

		storageTimeDecoration = new ControlDecoration(txtStorageTime, SWT.LEFT);
		storageTimeDecoration.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		storageTimeDecoration.setShowHover(true);
		storageTimeDecoration.setDescriptionText(MessageFormat.format(Messages.CyberTrackerPropertiesDialog_StorageTimeInvalid, CyberTrackerProperties.STORAGE_TIME_MIN_VALUE, CyberTrackerProperties.STORAGE_TIME_MAX_VALUE));
		storageTimeDecoration.hide();
		
		setTitle(Messages.CyberTrackerPropertiesDialog_Title);
		setMessage(Messages.CyberTrackerPropertiesDialog_Message);
		super.setTitleImage(CyberTrackerPlugIn.getDefault().getImageRegistry().get(CyberTrackerPlugIn.CT_WIZARD_BANNER));
		
		return container;
	}

	private boolean isTrackTimerValid() {
		if (txtTrackTimer == null || txtTrackTimer.getText() == null || txtTrackTimer.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtTrackTimer.getText());
			return result >= CyberTrackerProperties.TIME_TRACK_MIN_VALUE && result <= CyberTrackerProperties.TIME_TRACK_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	private boolean isStorageTimeValid() {
		if (txtStorageTime == null || txtStorageTime.getText() == null || txtStorageTime.getText().isEmpty())
			return false;
		try {
			Integer result = Integer.valueOf(txtStorageTime.getText());
			return result >= CyberTrackerProperties.STORAGE_TIME_MIN_VALUE && result <= CyberTrackerProperties.STORAGE_TIME_MAX_VALUE;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	private boolean isAppNameValid() {
    	return txtAppName != null && txtAppName.getText() != null && !txtAppName.getText().isEmpty() && txtAppName.getText().length() <= CyberTrackerProperties.APPLICATION_NAME_MAX_LENTH;
	}

	private boolean validate() {
		return isTrackTimerValid() && isAppNameValid() && isStorageTimeValid();
	}
	
	@Override
	protected boolean performSave() {
		if (!validate()) {
			MessageDialog.openError(getShell(), Messages.CyberTrackerPropertiesDialog_Error, Messages.CyberTrackerPropertiesDialog_DataNotValid);
			return false;
		}
		ctProperties.setApplicationName(txtAppName.getText());
		ctProperties.setAutoNext(btnAutoNext.getSelection());
		ctProperties.setKioskMode(btnKioskMode.getSelection());
		ctProperties.setWaypointTimer(Integer.valueOf(txtTrackTimer.getText()));
		StructuredSelection selection = (StructuredSelection) timeOffset.getSelection();
		ctProperties.setGpsTimeZone((Integer)selection.getFirstElement());
		ctProperties.setStorageTime(Integer.valueOf(txtStorageTime.getText()));
		
		Session session = HibernateManager.openSession();
		try {
			CyberTrackerHibernateManager.saveProperties(ctProperties, session);
			setChangesMade(false);
			return true;
		} catch (Exception e) {
			SmartPlugIn.displayLog(getShell(), Messages.CyberTrackerPropertiesDialog_Save_Error, e);
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

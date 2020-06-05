/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
package org.wcs.smart.connect.cybertracker.ctpackage;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.css.swt.dom.WidgetElement;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CtConnectPackageMetadata;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;

/**
 * Ui Controller for adding connect alerts/upload details to package
 * @author Emily
 *
 */
public class ConnectDataUiController implements IPackageUiContribution{

	private ICtPackage ctpackage;

	private Listener onModified;
	
	private Button btnUploadData;
	private Label lblUp1, lblUp2;
	private Text txtDataPeriod;
	private Button btnPositionUpdates;
	private Label lblPos1, lblPos2;
	private Text txtPositionPeriod;
	private ComboViewer cmbPositionType;
	
	private List<AlertType> types = null;
	
	@Inject private IEclipseContext context;

	private boolean fireEvents = true;
	private boolean canDisableUpload = true;
	
	public ConnectDataUiController() {
		this(true);
	}
	public ConnectDataUiController(boolean canConfigureUpload) {
		this.canDisableUpload = canConfigureUpload;
	}
	public boolean isTab() { 
		return true; 
	}
	
	public String getTabName() { 
		return Messages.ConnectDataUiController_TabName; 
	}
	
	private void validate() {
		if (!fireEvents) return;
		if (onModified == null) return;
		onModified.handleEvent(new Event());
	}
	
	@Override
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		Composite upDataComp = new Composite(main, SWT.FLAT);
		upDataComp.setLayout(new GridLayout());
		upDataComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)upDataComp.getLayout()).marginWidth = 0;
		((GridLayout)upDataComp.getLayout()).marginHeight = 0;
		
		Composite header = new Composite(upDataComp, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		WidgetElement.setCSSClass(header, SmartUiUtils.HEADER_CLASS);
		
		Label l = new Label(header, SWT.NONE);
		l.setText(Messages.ConnectDataUiController_DataUploadsLabel);
		Composite core = new Composite(upDataComp, SWT.FLAT);
		core.setLayout(new GridLayout(4, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label msg = new Label(core, SWT.WRAP);
		msg.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false, 4, 1));
		msg.setText(Messages.ConnectDataUiController_dataUploadMsg);
		((GridData)msg.getLayoutData()).widthHint = 140;
		
		btnUploadData = new Button(core, SWT.CHECK);
		btnUploadData.setSelection(true);
		btnUploadData.setEnabled(canDisableUpload);
		btnUploadData.addListener(SWT.Selection, e->{
			txtDataPeriod.setEnabled(btnUploadData.getSelection());
			lblUp1.setEnabled(btnUploadData.getSelection());
			lblUp2.setEnabled(btnUploadData.getSelection());
			validate();
		});
		
		lblUp1 = new Label(core, SWT.NONE);
		lblUp1.setText(Messages.ConnectDataUiController_Upload1);
		if (canDisableUpload) {
			lblUp1.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseUp(MouseEvent e) {
					btnUploadData.setSelection(!btnUploadData.getSelection());
					btnUploadData.notifyListeners(SWT.Selection, new Event());
				}
			});
		}
		
		txtDataPeriod = new Text(core, SWT.BORDER);
		txtDataPeriod.setText("20"); //$NON-NLS-1$
		txtDataPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtDataPeriod.getLayoutData()).widthHint = 30;
		txtDataPeriod.addListener(SWT.Modify, e->validate());
		
		lblUp2 = new Label(core, SWT.NONE);
		lblUp2.setText(Messages.ConnectDataUiController_Upload2);
		lblUp2.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		Composite posComp = new Composite(main, SWT.FLAT);
		posComp.setLayout(new GridLayout());
		posComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)posComp.getLayout()).marginWidth = 0;
		((GridLayout)posComp.getLayout()).marginHeight = 0;
		
		header = new Composite(posComp, SWT.NONE);
		header.setLayout(new GridLayout());
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		WidgetElement.setCSSClass(header, SmartUiUtils.HEADER_CLASS);
		
		l = new Label(header, SWT.NONE);
		l.setText(Messages.ConnectDataUiController_PositionLabel);
		
		core = new Composite(posComp, SWT.FLAT);
		core.setLayout(new GridLayout(5, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		msg = new Label(core, SWT.WRAP);
		msg.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		msg.setText(Messages.ConnectDataUiController_PositiongMessage);
		((GridData)msg.getLayoutData()).widthHint = 140;
		
		btnPositionUpdates = new Button(core, SWT.CHECK);
		btnPositionUpdates.setSelection(false);
		btnPositionUpdates.addListener(SWT.Selection, e->{
			lblPos1.setEnabled(btnPositionUpdates.getSelection());
			lblPos2.setEnabled(btnPositionUpdates.getSelection());
			txtPositionPeriod.setEnabled(btnPositionUpdates.getSelection());
			cmbPositionType.getControl().setEnabled(btnPositionUpdates.getSelection());
			validate();
		});
		lblPos1 = new Label(core, SWT.NONE);
		lblPos1.setText(Messages.ConnectDataUiController_Position1);
		lblPos1.setEnabled(false);
		lblPos1.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseUp(MouseEvent e) {
				btnPositionUpdates.setSelection(!btnPositionUpdates.getSelection());
				btnPositionUpdates.notifyListeners(SWT.Selection, new Event());
			}
		});
		
		txtPositionPeriod = new Text(core, SWT.BORDER);
		txtPositionPeriod.setText("10"); //$NON-NLS-1$
		txtPositionPeriod.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)txtDataPeriod.getLayoutData()).widthHint = 30;
		txtPositionPeriod.addListener(SWT.Modify, e->validate());
		txtPositionPeriod.setEnabled(false);
		
		lblPos2 = new Label(core, SWT.NONE);
		lblPos2.setText(Messages.ConnectDataUiController_Position2);
		lblPos2.setEnabled(false);
		
		cmbPositionType = new ComboViewer(core, SWT.DROP_DOWN | SWT.READ_ONLY);
		cmbPositionType.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		cmbPositionType.getControl().setEnabled(false);
		cmbPositionType.setContentProvider(ArrayContentProvider.getInstance());
		cmbPositionType.setLabelProvider(new LabelProvider() {
			public String getText(Object element) {
				if (element instanceof AlertType) return ((AlertType) element).getLabel();
				return super.getText(element);
			}
		});
		cmbPositionType.setInput(Messages.ConnectDataUiController_NotLoaded);
		cmbPositionType.addSelectionChangedListener(e->validate());

		Link link = new Link(main, SWT.NONE);
		link.setText("<a>" + Messages.ConnectDataUiController_RefreshTypes + "</a>"); //$NON-NLS-1$ //$NON-NLS-2$
		link.addListener(SWT.Selection, e->{
			refreshAlertTypes(true);
		});
		
		refreshAlertTypes(false);		
		
		if (ctpackage instanceof AbstractCtPackage) {
			try {
				fireEvents = false;
				MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name(), (AbstractCtPackage)ctpackage);
				if (data != null && data.getBooleanValue() != null && data.getBooleanValue()) {
					btnUploadData.setSelection(true);
					txtDataPeriod.setText(data.getStringValue());
				}else {
					if (canDisableUpload) {
						btnUploadData.setSelection(false);
					}else {
						btnUploadData.setSelection(true);	
					}
				}
				data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage);
				if (data != null && data.getBooleanValue() != null && data.getBooleanValue()) {
					btnPositionUpdates.setSelection(true);
					txtPositionPeriod.setText(data.getStringValue());
				}else {
					btnPositionUpdates.setSelection(false);
				}
				btnUploadData.notifyListeners(SWT.Selection, new Event());
				btnPositionUpdates.notifyListeners(SWT.Selection, new Event());
			}finally {
				fireEvents = true;
			}
			
		}

		
		return main;
	}

	private void refreshAlertTypes(boolean force) {
		(new LoadAlertTypesJob(context, force) {
			@Override
			public void typesLoaded(List<AlertType> atypes) {
				final boolean sendevents = (types == null);
				types = atypes;
				cmbPositionType.getControl().getDisplay().asyncExec(()->{
					cmbPositionType.setInput(types);
					
					if (ctpackage instanceof AbstractCtPackage) {
						MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage);
						if (data.getUuidValue() != null) {
							AlertType temp = new AlertType();
							temp.setUuid(data.getUuidValue());
							if (!sendevents) {
								cmbPositionType.setSelection(new StructuredSelection(temp));
							}else {
								fireEvents = false;
								try {
									cmbPositionType.setSelection(new StructuredSelection(temp));	
								}finally {
									fireEvents = true;
								}
							}
						}
					}

				});
			}
		}).schedule();
	}
	
	@Override
	public String isValid() {
		if (btnUploadData.getSelection()) {
			String min = txtDataPeriod.getText();
			
			try {
				int imin = Integer.parseInt(min);
				if (imin <= 0) {
					throw new Exception();
				}
			}catch (Exception ex) {
				return Messages.ConnectDataUiController_InvalidUploadPeriod;
			}
		}
		if (btnPositionUpdates.getSelection()) {
			String time = txtPositionPeriod.getText();
			try {
				int imin = Integer.parseInt(time);
				if (imin <= 0) {
					throw new Exception();
				}
			}catch (Exception ex) {
				return Messages.ConnectDataUiController_InvalidPositionPeriod;
			}
			if (cmbPositionType.getStructuredSelection().isEmpty()) {
				return Messages.ConnectDataUiController_AertTypeRequired;
			}
			if (!(cmbPositionType.getStructuredSelection().getFirstElement() instanceof AlertType )){
				return Messages.ConnectDataUiController_AertTypeRequired;
			}
		}
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage) {
		if (!(ctpackage instanceof AbstractCtPackage)) return;
		
		MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name(), (AbstractCtPackage)ctpackage);
		if (btnUploadData.getSelection()) {
			data.setBooleanValue(true);
			data.setStringValue(txtDataPeriod.getText());
		}else {
			data.setBooleanValue(false);
			data.setStringValue(null);
		}
		
		data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage);
		if (btnPositionUpdates.getSelection()) {
			data.setBooleanValue(true);
			data.setStringValue(txtPositionPeriod.getText());
			data.setUuidValue(  ((AlertType)cmbPositionType.getStructuredSelection().getFirstElement()).getUuid() );
		}else {
			data.setBooleanValue(false);
			data.setStringValue(null);
			data.setUuidValue(null);
		}
		
	}
	
	private MetadataFieldValue findCreateMetadataField(String key, AbstractCtPackage ctpackage) {
		if (ctpackage.getMetadataValues() != null) {
			for (MetadataFieldValue v : ctpackage.getMetadataValues()) {
				if (v.getMetadataKey().equals(key)) return v;
			}
		}else {
			ctpackage.setMetadataValues(new ArrayList<>());
		}
		MetadataFieldValue v = new MetadataFieldValue();
		v.setMetadataKey(key);
		v.setConservationArea(ctpackage.getConservationArea());
		v.setCtPackage(ctpackage);
		ctpackage.getMetadataValues().add(v);
		return v;
	}

}

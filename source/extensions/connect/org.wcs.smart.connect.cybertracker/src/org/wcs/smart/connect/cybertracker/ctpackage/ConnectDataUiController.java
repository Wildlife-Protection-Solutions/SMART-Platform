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

import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.jface.dialogs.MessageDialog;
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
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.common.control.SmartUiUtils;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.api.model.AlertType;
import org.wcs.smart.connect.cybertracker.ConnectCtPlugIn;
import org.wcs.smart.connect.cybertracker.internal.Messages;
import org.wcs.smart.connect.cybertracker.model.CtConnectPackageMetadata;
import org.wcs.smart.cybertracker.export.IPackageUiContribution;
import org.wcs.smart.cybertracker.model.AbstractCtPackage;
import org.wcs.smart.cybertracker.model.ICtPackage;
import org.wcs.smart.cybertracker.model.MetadataFieldValue;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.ui.QrCodeLabel;
import org.wcs.smart.util.SmartUtils;

/**
 * Ui Controller for adding connect alerts/upload details to package
 * @author Emily
 *
 */
public class ConnectDataUiController implements IPackageUiContribution{

	private ICtPackage ctpackage;

	private Listener onModified;
	private Runnable onInitilized;

	private Button btnUploadData;
	private Button btnPositionUpdates;
	private Label lblPos1, lblPos2;
	private Text txtPositionPeriod;
	private ComboViewer cmbPositionType;
	
	private Text txtFieldIdStr;
	
	@Inject private IEclipseContext context;

	private boolean fireEvents = true;
	private boolean canDisableUpload = true;
	
	private Text txtUrl;
	private QrCodeLabel lblQr;
	private Button btnPublic;
	
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
	public Composite createUi(Composite parent, ICtPackage ctpackage, Listener onModified, Runnable onInitilized) {
		this.ctpackage = ctpackage;
		this.onModified = onModified;
		this.onInitilized = onInitilized;
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		((GridLayout)main.getLayout()).marginWidth = 0;
		((GridLayout)main.getLayout()).marginHeight = 0;
		
		Composite upDataComp = new Composite(main, SWT.FLAT);
		upDataComp.setLayout(new GridLayout());
		upDataComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)upDataComp.getLayout()).marginWidth = 0;
		((GridLayout)upDataComp.getLayout()).marginHeight = 0;
		
		SmartUiUtils.createHeaderLabel(upDataComp, Messages.ConnectDataUiController_DataUploadsLabel);
		
		Composite core = new Composite(upDataComp, SWT.FLAT);
		core.setLayout(new GridLayout());
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		Label msg = new Label(core, SWT.WRAP);
		msg.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false));
		
		((GridData)msg.getLayoutData()).widthHint = 600;
		
		if (canDisableUpload) {
			msg.setText(Messages.ConnectDataUiController_dataUploadMsg);	
			btnUploadData = new Button(core, SWT.CHECK);
			btnUploadData.setSelection(false);
			btnUploadData.setText(Messages.ConnectDataUiController_UploadOp);
			
			btnUploadData.addListener(SWT.Selection, e->{
				validate();
			});
		}else {
			msg.setText(Messages.ConnectDataUiController_MobileExportsAutomaticUpload);
		}
		
		Composite part = new Composite(core, SWT.NONE);
		part.setLayout(new GridLayout(4, false));
		part.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		if (canDisableUpload) ((GridData)part.getLayoutData()).horizontalIndent = 20;
		((GridLayout)part.getLayout()).marginWidth = 0;
		((GridLayout)part.getLayout()).marginHeight = 0;

		Composite posComp = new Composite(main, SWT.FLAT);
		posComp.setLayout(new GridLayout());
		posComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridLayout)posComp.getLayout()).marginWidth = 0;
		((GridLayout)posComp.getLayout()).marginHeight = 0;
		
		SmartUiUtils.createHeaderLabel(posComp, Messages.ConnectDataUiController_PositionLabel);
		
		core = new Composite(posComp, SWT.FLAT);
		core.setLayout(new GridLayout(5, false));
		core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		msg = new Label(core, SWT.WRAP);
		msg.setLayoutData( new GridData(SWT.FILL, SWT.FILL, true, false, 5, 1));
		msg.setText(Messages.ConnectDataUiController_PositiongMessage);
		((GridData)msg.getLayoutData()).widthHint = 600;
		
		btnPositionUpdates = new Button(core, SWT.CHECK);
		btnPositionUpdates.setSelection(false);
		btnPositionUpdates.addListener(SWT.Selection, e->{
//			lblPos1.setEnabled(btnPositionUpdates.getSelection());
//			lblPos2.setEnabled(btnPositionUpdates.getSelection());
			txtPositionPeriod.setEnabled(btnPositionUpdates.getSelection());
			cmbPositionType.getControl().setEnabled(btnPositionUpdates.getSelection());
			validate();
		});
		lblPos1 = new Label(core, SWT.NONE);
		lblPos1.setText(Messages.ConnectDataUiController_Position1);
//		lblPos1.setEnabled(false);
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
		txtPositionPeriod.addListener(SWT.Modify, e->validate());
		txtPositionPeriod.setEnabled(false);
		
		lblPos2 = new Label(core, SWT.NONE);
		lblPos2.setText(Messages.ConnectDataUiController_Position2);
//		lblPos2.setEnabled(false);
		
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
		
		
		if (ctpackage.getFieldIdentifierKeys() != null && ctpackage.getFieldIdentifierKeys().length > 0) {
			Composite idComp = new Composite(main, SWT.FLAT);
			idComp.setLayout(new GridLayout());
			idComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)idComp.getLayout()).marginWidth = 0;
			((GridLayout)idComp.getLayout()).marginHeight = 0;
			
			SmartUiUtils.createHeaderLabel(idComp, Messages.ConnectDataUiController_AlertFieldIdSection);
			
			core = new Composite(idComp, SWT.FLAT);
			core.setLayout(new GridLayout(1, false));
			core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Label lblInfo = new Label(core, SWT.WRAP);
			lblInfo.setText(Messages.ConnectDataUiController_Message);
			lblInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)lblInfo.getLayoutData()).widthHint = 600;
			
			StringJoiner sj = new StringJoiner(", "); //$NON-NLS-1$
			for (String s : ctpackage.getFieldIdentifierKeys()) sj.add("{" + s + "}"); //$NON-NLS-1$ //$NON-NLS-2$
			
			Text validMetadataField= new Text(core, SWT.WRAP);
			validMetadataField.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)validMetadataField.getLayoutData()).widthHint = 600;
			validMetadataField.setText(Messages.ConnectDataUiController_ValidTokensMessage + sj.toString());
			validMetadataField.setEditable(false);
			
			txtFieldIdStr = new Text(core, SWT.BORDER);
			txtFieldIdStr.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			txtFieldIdStr.addListener(SWT.Modify, e->validate());
//			((GridData)txtId.getLayoutData()).heightHint = 50;
//			((GridData)txtId.getLayoutData()).widthHint = 600;
			
			
		}
		
		
		if (ctpackage.showConnectUrlConfiguration()) {
		
			Composite urlComp = new Composite(main, SWT.FLAT);
			urlComp.setLayout(new GridLayout());
			urlComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridLayout)urlComp.getLayout()).marginWidth = 0;
			((GridLayout)urlComp.getLayout()).marginHeight = 0;
			
			SmartUiUtils.createHeaderLabel(urlComp, Messages.ConnectDataUiController_PackageUrlHeader);
			
			core = new Composite(urlComp, SWT.FLAT);
			core.setLayout(new GridLayout(1, false));
			core.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			Label l = new Label(core, SWT.WRAP);
			l.setText(Messages.ConnectDataUiController_LinkInfo);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 600;
			
			Composite temp = new Composite(core, SWT.NONE);
			temp.setLayout(new GridLayout(3, false));
			((GridLayout)temp.getLayout()).marginWidth = 0;
			((GridLayout)temp.getLayout()).marginHeight = 0;
			temp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			
			lblQr = new QrCodeLabel(temp, SWT.NONE);
			lblQr.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			txtUrl = new Text(temp, SWT.WRAP | SWT.READ_ONLY | SWT.BORDER | SWT.V_SCROLL);
			txtUrl.setEditable(false);
			txtUrl.setEnabled(true);
			txtUrl.setText(""); //$NON-NLS-1$
			txtUrl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)txtUrl.getLayoutData()).heightHint = 50;
			((GridData)txtUrl.getLayoutData()).widthHint = 600;
			
			ToolBar privateTb = new ToolBar(temp, SWT.FLAT);
			privateTb.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			
			ToolItem btnCopy = new ToolItem(privateTb, SWT.PUSH);
			btnCopy.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.COPY_ICON));
			btnCopy.setToolTipText(Messages.ConnectDataUiController_copytooltip);
			btnCopy.addListener(SWT.Selection,e->{
				txtUrl.setRedraw(false);
				txtUrl.selectAll();
				txtUrl.copy();
				txtUrl.clearSelection();
				txtUrl.setRedraw(true);
				
			});
			
			l = new Label(core, SWT.WRAP);
			l.setText(Messages.ConnectDataUiController_SecurityMessage);
			l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
			((GridData)l.getLayoutData()).widthHint = 600;
	
			btnPublic = new Button(core, SWT.CHECK);
			btnPublic.setText(Messages.ConnectDataUiController_MakePublic);
			btnPublic.addListener(SWT.Selection, e->{
				if (btnPublic.getSelection()) {
					MessageDialog.openWarning(btnPublic.getShell(), Messages.ConnectDataUiController_WarningTitle, Messages.ConnectDataUiController_WarningMsg);
				}
				validate();
			});
			
			updateUrl();
		}
				
		
		if (ctpackage instanceof AbstractCtPackage) {
			try {
				fireEvents = false;
				
				MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.USE_CONNECT.name(), (AbstractCtPackage)ctpackage, null);
				if (data != null && data.getBooleanValue() != null && data.getBooleanValue()) {
					if (btnUploadData != null) btnUploadData.setSelection(true);
				}else {
					if (btnUploadData != null) btnUploadData.setSelection(false);
				}
				
				data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage, null);
				if (data != null && data.getBooleanValue() != null && data.getBooleanValue()) {
					btnPositionUpdates.setSelection(true);
					txtPositionPeriod.setText(data.getStringValue());
				}else {
					btnPositionUpdates.setSelection(false);
				}
				
				if (btnPublic != null) {
					data = findCreateMetadataField(ICtPackage.PRIVATE_PROP_KEY, (AbstractCtPackage)ctpackage, null);
					if ((data == null || data.getBooleanValue() == null) || data.getBooleanValue()) {
						btnPublic.setSelection(false);
					}else {
						btnPublic.setSelection(true);
					}
				}
				if (btnUploadData != null) btnUploadData.notifyListeners(SWT.Selection,new Event());
				btnPositionUpdates.notifyListeners(SWT.Selection, new Event());
				
				
				if (txtFieldIdStr != null) {
					data = findCreateMetadataField(ICtPackage.FIELD_IDENTIFIER_KEY, (AbstractCtPackage)ctpackage, null);
					if (data == null || data.getStringValue() == null) {
						txtFieldIdStr.setText(""); //$NON-NLS-1$
					}else {
						txtFieldIdStr.setText(data.getStringValue());
					}
				}
			}finally {
				fireEvents = true;
			}			
		}
		
		refreshAlertTypes(false);		
		return main;
	}

	private void refreshAlertTypes(boolean force) {
		//TODO: FIX THIS
		(new LoadAlertTypesJob(context, force) {
			@Override
			public void typesLoaded(List<AlertType> atypes) {
				
				List<AlertType> types = new ArrayList<>(atypes);
				
				cmbPositionType.getControl().getDisplay().asyncExec(()->{
					
					fireEvents = false;
					try {
						cmbPositionType.setInput(types);
							
						if (ctpackage instanceof AbstractCtPackage) {
							MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage, null);
							if (data.getUuidValue() != null) {
								
								AlertType temp = null;
								for (AlertType t : types) {
									if (t.getUuid().equals(data.getUuidValue())){
										temp = t;
										break;
									}
								}
								if (temp == null) {
									temp = new AlertType();
									temp.setUuid(data.getUuidValue());
									temp.setLabel(data.getUuid().toString());
									types.add(temp);
									cmbPositionType.refresh();
								}
								cmbPositionType.setSelection(new StructuredSelection(temp));
							}
						}
						onInitilized.run();
					}finally {
						fireEvents = true;		
					}
				});
			
			}
		}).schedule();
	}
	
	@Override
	public String isValid() {
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
		if (txtFieldIdStr != null) {
			String id = txtFieldIdStr.getText();
			if (!id.isBlank()) {
				for (String key : this.ctpackage.getFieldIdentifierKeys()) {
					id = id.replace("{" + key + "}", ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				}
				
				if (!id.isBlank() && !SmartUtils.isSimpleString(id.trim(), 
						SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX, MetadataFieldValue.MAX_STRING_LENGTH) ) {
					return MessageFormat.format(Messages.ConnectDataUiController_InvalidPattern, MetadataFieldValue.MAX_STRING_LENGTH, SmartUtils.RegExLevel.ALLOWED_CHARS_COMPLEX_REGEX.textDesc);
				}
				
			}
		}
		return null;
	}

	@Override
	public void updatePackage(ICtPackage ctpackage, Session session) {
		if (!(ctpackage instanceof AbstractCtPackage)) return;
		
		boolean useconnect = true;
		if (btnUploadData != null) {
			if (!btnUploadData.getSelection()) {
				useconnect = false;
			}
		}
		MetadataFieldValue data = findCreateMetadataField(CtConnectPackageMetadata.Properties.USE_CONNECT.name(), (AbstractCtPackage)ctpackage, session);
		data.setBooleanValue(useconnect);
		
		data = findCreateMetadataField(CtConnectPackageMetadata.Properties.POSITION_UPLOAD.name(), (AbstractCtPackage)ctpackage, session);
		if (btnPositionUpdates.getSelection()) {
			data.setBooleanValue(true);
			data.setStringValue(txtPositionPeriod.getText());
			data.setUuidValue(  ((AlertType)cmbPositionType.getStructuredSelection().getFirstElement()).getUuid() );
		}else {
			data.setBooleanValue(false);
			data.setStringValue(null);
			data.setUuidValue(null);
		}
		
		if (btnPublic != null) {
			data = findCreateMetadataField(ICtPackage.PRIVATE_PROP_KEY, (AbstractCtPackage)ctpackage, session);
			if (btnPublic.getSelection()) {
				data.setBooleanValue(false);
			}else {
				data.setBooleanValue(true);
			}
			updateUrl();
		}
		
		if (txtFieldIdStr != null) {
			data = findCreateMetadataField(ICtPackage.FIELD_IDENTIFIER_KEY, (AbstractCtPackage)ctpackage, session);
			if (txtFieldIdStr.getText().isBlank()) {
				data.setStringValue(null);
			}else {
				data.setStringValue(txtFieldIdStr.getText());
			}
		}
		
	}
	
	private MetadataFieldValue findCreateMetadataField(String key, AbstractCtPackage ctpackage, Session session) {
		if (ctpackage.getMetadataValues() != null) {
			for (MetadataFieldValue v : ctpackage.getMetadataValues()) {
				if (v.getMetadataKey().equals(key)) {
					if (session != null && v.getUuid() == null) session.persist(v);
					return v;
				}
			}
		}else {
			ctpackage.setMetadataValues(new ArrayList<>());
		}
		
		MetadataFieldValue v = new MetadataFieldValue();
		v.setMetadataKey(key);
		v.setConservationArea(ctpackage.getConservationArea());
		v.setCtPackage(ctpackage);
		
		if (session != null) session.persist(v);
		ctpackage.getMetadataValues().add(v);
		
		
		//if there is no use_connect metadata field then base the value of this field on the 
		//data_upload metadata field to support backwards compatibility
		if (key.equalsIgnoreCase(CtConnectPackageMetadata.Properties.USE_CONNECT.name()) ) {
			for (MetadataFieldValue t : ctpackage.getMetadataValues()) {
				if (t.getMetadataKey().equals( CtConnectPackageMetadata.Properties.DATA_UPLOAD.name() )) {
					v.setBooleanValue(t.getBooleanValue());
					break;
				}
			}
		}
		
		if (key.equalsIgnoreCase(CtConnectPackageMetadata.Properties.DATA_UPLOAD.name())) {
			v.setBooleanValue(true);
		}
		return v;
	}

	private void updateUrl() {
		if (ctpackage.getUuid() == null) {
			txtUrl.setText(Messages.ConnectDataUiController_PackageMustBeSaved);	
		}else {
			lblQr.setUrl(null);
			
			URL url = null;
			
			try(Session s = HibernateManager.openSession()){
				url = ConnectCtPlugIn.generagePackageConnectUrl(s, ctpackage);
			}
			
			if (url == null) {
				txtUrl.setText(Messages.ConnectDataUiController_NoConnectServer);
			}else {
				try {
					MetadataFieldValue privatemd = findCreateMetadataField(ICtPackage.PRIVATE_PROP_KEY, (AbstractCtPackage)ctpackage, null);
					boolean isprivate = true;
					if (privatemd.getBooleanValue() != null) isprivate = privatemd.getBooleanValue();
					String link = ICtPackage.generateSmartMobileAppLink(url, isprivate);
					txtUrl.setText(link);					
					lblQr.setUrl(link);
				}catch (Exception ex) {
					ConnectPlugIn.log(ex.getMessage(), ex);
					txtUrl.setText(Messages.ConnectDataUiController_PackageConfigError +ex.getMessage());
				}
			}
			
			lblQr.getParent().layout(true);
		}
	}
}
 
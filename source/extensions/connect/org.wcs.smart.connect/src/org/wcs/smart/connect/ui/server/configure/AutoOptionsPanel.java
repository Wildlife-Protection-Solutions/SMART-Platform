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
package org.wcs.smart.connect.ui.server.configure;

import java.util.ArrayList;
import java.util.Collection;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectServerOption.ConnectionOption;
import org.wcs.smart.connect.server.replication.AutoReplicationStartUp;

/**
 * Composite that contains all the options
 * for automatic replication
 * 
 * @author Emily
 *
 */
public class AutoOptionsPanel implements IServerOptionsPanel {

	private Collection<ModifyListener> listeners;
	
	private boolean isEditable = true;
	
	private Button btnDownStartUp;
	private Button btnUploadStartUp;
	private Button btnDownShutDown;
	private Button btnUploadShutDown;
	private Button btnAutoCheck;
	private Button btnPrompt;
	private Button btnUpload;
	private Button btnDownload;
	private Button btnPackageSize;
	private Label lblMinutes, lblMinutes2, lblPackageSize;
	private Text txtMinutes, txtPackageSize;
	private ControlDecoration cdMinutes, cdPackageSize;
	
	private boolean isAutoReplicationPrev;
	
	@Override
	public String getName(){
		return "Automatic Sync Options";
	}
	
	@Override
	public Composite createComposite(Composite parent, boolean isEditable){
		listeners = new ArrayList<ModifyListener>();
		
		Composite main = new Composite(parent, SWT.NONE);
		main.setLayout(new GridLayout());
		
		Group g1 = new Group(main, SWT.DEFAULT);
		g1.setText(Messages.AutoOptionsPanel_StartUpOptionLabel);
		g1.setLayout(new GridLayout());
		g1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnDownStartUp = new Button(g1, SWT.CHECK);
		btnDownStartUp.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.DOWNLOAD_ON_STARTUP));
		btnDownStartUp.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.DOWNLOAD_ON_STARTUP));
		
		btnUploadStartUp = new Button(g1, SWT.CHECK);
		btnUploadStartUp.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.UPLOAD_ON_STARTUP));
		btnUploadStartUp.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.UPLOAD_ON_STARTUP));
		btnUploadStartUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnUploadStartUp.getLayoutData()).horizontalIndent = 10;
		
		Group g2 = new Group(main, SWT.DEFAULT);
		g2.setText(Messages.AutoOptionsPanel_ShutdownOpLabel);
		g2.setLayout(new GridLayout());
		g2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnDownShutDown = new Button(g2, SWT.CHECK);
		btnDownShutDown.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.DOWNLOAD_ON_SHUTDOWN));
		btnDownShutDown.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.DOWNLOAD_ON_SHUTDOWN));
		
		btnUploadShutDown = new Button(g2, SWT.CHECK);
		btnUploadShutDown.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.UPLOAD_ON_SHUTDOWN));
		btnUploadShutDown.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.UPLOAD_ON_SHUTDOWN));
		btnUploadShutDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnUploadShutDown.getLayoutData()).horizontalIndent = 10;
		
		Group g3 = new Group(main, SWT.DEFAULT);
		g3.setText(Messages.AutoOptionsPanel_AutoOpLabel);
		g3.setLayout(new GridLayout());
		g3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnAutoCheck = new Button(g3, SWT.CHECK);
		btnAutoCheck.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.SYNC_AUTOMATICALLY));
		btnAutoCheck.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.SYNC_AUTOMATICALLY));
		
		Composite minComp = new Composite(g3, SWT.NONE);
		minComp.setLayout(new GridLayout(3, false));
		lblMinutes = new Label(minComp, SWT.NONE);
		lblMinutes.setText(Messages.AutoOptionsPanel_CheckChangesLabel);
		txtMinutes = new Text(minComp, SWT.BORDER);
		txtMinutes.setText("0000"); //$NON-NLS-1$
		txtMinutes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtMinutes.getLayoutData()).widthHint = 50;
		lblMinutes2 = new Label(minComp, SWT.NONE);
		lblMinutes2.setText(Messages.AutoOptionsPanel_MinutesLabel);
		minComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)minComp.getLayoutData()).horizontalIndent = 10;
		cdMinutes = createControlDecoration(txtMinutes);
		cdMinutes.setDescriptionText(Messages.AutoOptionsPanel_InvalidMinutes);
		txtMinutes.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e) {
				try{
					int x = Integer.parseInt(txtMinutes.getText());
					if (x < 0){
						throw new Exception(Messages.AutoOptionsPanel_InvalidMinutes2);
					}
					cdMinutes.hide();
				}catch (Exception ex){
					cdMinutes.show();
				}
				fireChange(e);
			}
			
		});

		btnPrompt = new Button(g3, SWT.CHECK);
		btnPrompt.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.SYNC_PROMPT_PASSWORD));
		btnPrompt.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.SYNC_PROMPT_PASSWORD));
		btnPrompt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnPrompt.getLayoutData()).horizontalIndent = 10;
				
		btnDownload = new Button(g3, SWT.CHECK);
		btnDownload.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.SYNC_DOWNLOAD));
		btnDownload.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.SYNC_DOWNLOAD));
		btnDownload.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnDownload.getLayoutData()).horizontalIndent = 10;
		
		btnUpload = new Button(g3, SWT.CHECK);
		btnUpload.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.SYNC_AUTO_UPLOAD));
		btnUpload.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.SYNC_AUTO_UPLOAD));
		btnUpload.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnUpload.getLayoutData()).horizontalIndent = 20;
		
		Group g4 = new Group(main, SWT.DEFAULT);
		g4.setText(Messages.AutoOptionsPanel_PackageOpLable);
		g4.setLayout(new GridLayout(3, false));
		g4.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnPackageSize = new Button(g4, SWT.CHECK);
		btnPackageSize.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.PACKAGE_PROMPT));
		btnPackageSize.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.PACKAGE_PROMPT));
		btnPackageSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnPackageSize.getLayoutData()).horizontalIndent = 0;
		
		txtPackageSize = new Text(g4, SWT.BORDER);
		txtPackageSize.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtPackageSize.getLayoutData()).widthHint = 50;
		cdPackageSize = createControlDecoration(txtPackageSize);
		cdPackageSize.setDescriptionText(Messages.AutoOptionsPanel_InvalidSize);
		txtPackageSize.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e) {
				try{
					int x = Integer.parseInt(txtPackageSize.getText());
					if (x < 0){
						throw new Exception(Messages.AutoOptionsPanel_InvalidSize2);
					}
					cdPackageSize.hide();
				}catch (Exception ex){
					cdPackageSize.show();
				}
				fireChange(e);
			}
			
		});
		
		lblPackageSize = new Label(g4, SWT.NONE);
		lblPackageSize.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(ConnectionOption.PACKAGE_PROMPT_SIZE));
		lblPackageSize.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(ConnectionOption.PACKAGE_PROMPT_SIZE));
		
		if (isEditable){
			SelectionListener ml = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateEnabled();
					fireChange(null);
				}
			};
			btnAutoCheck.addSelectionListener(ml);
			btnDownload.addSelectionListener(ml);
			btnDownShutDown.addSelectionListener(ml);
			btnDownStartUp.addSelectionListener(ml);
			btnPackageSize.addSelectionListener(ml);
			
			ml = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					fireChange(null);
				}
			};
			btnPrompt.addSelectionListener(ml);
			btnUpload.addSelectionListener(ml);
			btnUploadShutDown.addSelectionListener(ml);
			btnUploadStartUp.addSelectionListener(ml);
		}else{
			btnDownShutDown.setEnabled(false);
			btnDownStartUp.setEnabled(false);
			btnUploadShutDown.setEnabled(false);
			btnUploadStartUp.setEnabled(false);
			btnAutoCheck.setEnabled(false);
			btnDownload.setEnabled(false);
			btnUpload.setEnabled(false);
			btnPrompt.setEnabled(false);
			txtMinutes.setEnabled(false);
			lblMinutes.setEnabled(false);
			lblMinutes2.setEnabled(false);
			lblPackageSize.setEnabled(false);
			txtPackageSize.setEnabled(false);
			btnPackageSize.setEnabled(false);
		}
		
		return main;
	}
	
	public boolean isValid(){
		return !cdMinutes.isVisible() && !cdPackageSize.isVisible();
	}
	
	private void fireChange(ModifyEvent e){
		for(ModifyListener m : listeners){
			m.modifyText(e);
		}
	}
	
	@Override
	public void addChangeListener(ModifyListener listener){
		listeners.add(listener);
	}
	
	protected ControlDecoration createControlDecoration(Control widget){
		ControlDecoration cd = new ControlDecoration(widget, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		return cd;
	}
	
	protected void updateEnabled(){
		if (!isEditable) return;
		
		boolean enabled = btnAutoCheck.getSelection();
		
		lblMinutes.setEnabled(enabled);
		txtMinutes.setEnabled(enabled);
		lblMinutes2.setEnabled(enabled);
		btnPrompt.setEnabled(enabled);
		btnDownload.setEnabled(enabled);
		btnUpload.setEnabled(enabled);
		
		if (btnDownload.isEnabled()){
			if (btnDownload.getSelection()){
				btnUpload.setEnabled(true);
			}else{
				btnUpload.setEnabled(false);
			}
		}
		
		
		btnDownShutDown.setEnabled(true);
		btnDownStartUp.setEnabled(true);
		if (btnDownShutDown.getSelection()){
			btnUploadShutDown.setEnabled(true);
		}else{
			btnUploadShutDown.setEnabled(false);
		}
		if (btnDownStartUp.getSelection()){
			btnUploadStartUp.setEnabled(true);
		}else{
			btnUploadStartUp.setEnabled(false);
		}
		txtPackageSize.setEnabled(btnPackageSize.getSelection());
	}
	
	@Override
	public void initValues(ConnectServer server){
		if (server == null){
			btnDownShutDown.setSelection(ConnectServerOption.ConnectionOption.DOWNLOAD_ON_SHUTDOWN.getDefaultValueAsBoolean());
			btnDownStartUp.setSelection(ConnectServerOption.ConnectionOption.DOWNLOAD_ON_STARTUP.getDefaultValueAsBoolean());
			btnUploadShutDown.setSelection(ConnectServerOption.ConnectionOption.UPLOAD_ON_SHUTDOWN.getDefaultValueAsBoolean());
			btnUploadStartUp.setSelection(ConnectServerOption.ConnectionOption.UPLOAD_ON_STARTUP.getDefaultValueAsBoolean());
			btnAutoCheck.setSelection(ConnectServerOption.ConnectionOption.SYNC_AUTOMATICALLY.getDefaultValueAsBoolean());
			btnDownload.setSelection(ConnectServerOption.ConnectionOption.SYNC_DOWNLOAD.getDefaultValueAsBoolean());
			btnUpload.setSelection(ConnectServerOption.ConnectionOption.SYNC_AUTO_UPLOAD.getDefaultValueAsBoolean());
			btnPrompt.setSelection(ConnectServerOption.ConnectionOption.SYNC_PROMPT_PASSWORD.getDefaultValueAsBoolean());
			txtMinutes.setText(ConnectServerOption.ConnectionOption.SYNC_MINUTE.getDefaultValueAsString());
			
			btnPackageSize.setSelection(ConnectServerOption.ConnectionOption.SYNC_PROMPT_PASSWORD.getDefaultValueAsBoolean());
			txtPackageSize.setText(String.valueOf(ConnectServerOption.ConnectionOption.PACKAGE_PROMPT_SIZE.getDefaultValueAsInt()));

			updateEnabled();
			return;
		}
		
		btnDownShutDown.setSelection(ConnectServerOption.ConnectionOption.DOWNLOAD_ON_SHUTDOWN.getBooleanValue(server));
		btnDownStartUp.setSelection(ConnectServerOption.ConnectionOption.DOWNLOAD_ON_STARTUP.getBooleanValue(server));
		btnUploadShutDown.setSelection(ConnectServerOption.ConnectionOption.UPLOAD_ON_SHUTDOWN.getBooleanValue(server));
		btnUploadStartUp.setSelection(ConnectServerOption.ConnectionOption.UPLOAD_ON_STARTUP.getBooleanValue(server));
		btnAutoCheck.setSelection(ConnectServerOption.ConnectionOption.SYNC_AUTOMATICALLY.getBooleanValue(server));
		btnDownload.setSelection(ConnectServerOption.ConnectionOption.SYNC_DOWNLOAD.getBooleanValue(server));
		btnUpload.setSelection(ConnectServerOption.ConnectionOption.SYNC_AUTO_UPLOAD.getBooleanValue(server));
		btnPrompt.setSelection(ConnectServerOption.ConnectionOption.SYNC_PROMPT_PASSWORD.getBooleanValue(server));
		txtMinutes.setText(String.valueOf(ConnectServerOption.ConnectionOption.SYNC_MINUTE.getIntegerValue(server)));
		btnPackageSize.setSelection(ConnectServerOption.ConnectionOption.PACKAGE_PROMPT.getBooleanValue(server));
		txtPackageSize.setText(String.valueOf(ConnectServerOption.ConnectionOption.PACKAGE_PROMPT_SIZE.getIntegerValue(server)));
		
		updateEnabled();
	}
	
	@Override
	public void updateServer(ConnectServer server){
		isAutoReplicationPrev = ConnectServerOption.ConnectionOption.SYNC_AUTOMATICALLY.getBooleanValue(server);
		
		server.setOption(ConnectServerOption.ConnectionOption.DOWNLOAD_ON_SHUTDOWN.name(), ((Boolean)btnDownShutDown.getSelection()).toString());
		server.setOption(ConnectServerOption.ConnectionOption.DOWNLOAD_ON_STARTUP.name(), ((Boolean)btnDownStartUp.getSelection()).toString());
		server.setOption(ConnectServerOption.ConnectionOption.UPLOAD_ON_SHUTDOWN.name(), ((Boolean)btnUploadShutDown.getSelection()).toString());
		server.setOption(ConnectServerOption.ConnectionOption.UPLOAD_ON_STARTUP.name(), ((Boolean)btnUploadStartUp.getSelection()).toString());
		server.setOption(ConnectServerOption.ConnectionOption.SYNC_AUTOMATICALLY.name(), ((Boolean)btnAutoCheck.getSelection()).toString());
		server.setOption(ConnectServerOption.ConnectionOption.SYNC_PROMPT_PASSWORD.name(), ((Boolean)btnPrompt.getSelection()).toString());
		server.setOption(ConnectServerOption.ConnectionOption.SYNC_DOWNLOAD.name(), ((Boolean)btnDownload.getSelection()).toString());
		server.setOption(ConnectServerOption.ConnectionOption.SYNC_AUTO_UPLOAD.name(), ((Boolean)btnUpload.getSelection()).toString());
		server.setOption(ConnectServerOption.ConnectionOption.PACKAGE_PROMPT.name(), ((Boolean)btnPackageSize.getSelection()).toString());

		int packageSize = ConnectServerOption.ConnectionOption.PACKAGE_PROMPT_SIZE.getDefaultValueAsInt();
		try{
			int tmp = Integer.parseInt(txtPackageSize.getText());
			if (tmp >= 0) packageSize = tmp;
		}catch(Exception ex){
			
		}
		server.setOption(ConnectServerOption.ConnectionOption.PACKAGE_PROMPT_SIZE.name(), String.valueOf(packageSize));
		
		String minutes = ConnectServerOption.ConnectionOption.SYNC_MINUTE.getDefaultValueAsString();
		try{
			int tmp = Integer.parseInt(txtMinutes.getText());
			if (tmp >= 0){
				minutes = String.valueOf(tmp);
			}
		}catch(Exception ex){
			
		}
		server.setOption(ConnectServerOption.ConnectionOption.SYNC_MINUTE.name(), minutes);	
	}

	@Override
	public void afterSave(ConnectServer server){
		boolean isAutoReplication = ConnectServerOption.ConnectionOption.SYNC_AUTOMATICALLY.getBooleanValue(server);
		if (!isAutoReplicationPrev && isAutoReplication){
			//auto replication state has been updated; we need to initiate auto replication
			int delay = ConnectServerOption.ConnectionOption.SYNC_MINUTE.getIntegerValue(server);
			AutoReplicationStartUp.INSTANCE.enableAutoReplication(delay);
		}
	}
}


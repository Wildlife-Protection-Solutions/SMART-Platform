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
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectServerOption.Option;

/**
 * Composite that contains all the options
 * for automatic replication
 * 
 * @author Emily
 *
 */
public class AutoOptionsPanel extends Composite {

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
	private Label lblMinutes, lblMinutes2;
	private Text txtMinutes;
	private ControlDecoration cdMinutes;
	
	public AutoOptionsPanel(Composite parent) {
		this(parent, true);
	}
	
	public AutoOptionsPanel(Composite parent, boolean isEditable) {
		super(parent, SWT.NONE);
		this.isEditable = isEditable;
		listeners = new ArrayList<ModifyListener>();
		createControl();
	}
	
	protected void createControl(){
		setLayout(new GridLayout());
		
		Group g1 = new Group(this, SWT.DEFAULT);
		g1.setText("Start Up Options");
		g1.setLayout(new GridLayout());
		g1.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnDownStartUp = new Button(g1, SWT.CHECK);
		btnDownStartUp.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(Option.DOWNLOAD_ON_STARTUP));
		btnDownStartUp.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(Option.DOWNLOAD_ON_STARTUP));
		
		btnUploadStartUp = new Button(g1, SWT.CHECK);
		btnUploadStartUp.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(Option.UPLOAD_ON_STARTUP));
		btnUploadStartUp.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(Option.UPLOAD_ON_STARTUP));
		btnUploadStartUp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnUploadStartUp.getLayoutData()).horizontalIndent = 10;
		
		Group g2 = new Group(this, SWT.DEFAULT);
		g2.setText("Shut Down Options");
		g2.setLayout(new GridLayout());
		g2.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnDownShutDown = new Button(g2, SWT.CHECK);
		btnDownShutDown.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(Option.DOWNLOAD_ON_SHUTDOWN));
		btnDownShutDown.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(Option.DOWNLOAD_ON_SHUTDOWN));
		
		btnUploadShutDown = new Button(g2, SWT.CHECK);
		btnUploadShutDown.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(Option.UPLOAD_ON_SHUTDOWN));
		btnUploadShutDown.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(Option.UPLOAD_ON_SHUTDOWN));
		btnUploadShutDown.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnUploadShutDown.getLayoutData()).horizontalIndent = 10;
		
		Group g3 = new Group(this, SWT.DEFAULT);
		g3.setText("Automatic Replication Options");
		g3.setLayout(new GridLayout());
		g3.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		btnAutoCheck = new Button(g3, SWT.CHECK);
		btnAutoCheck.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(Option.SYNC_AUTOMATICALLY));
		btnAutoCheck.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(Option.SYNC_AUTOMATICALLY));
		
		Composite minComp = new Composite(g3, SWT.NONE);
		minComp.setLayout(new GridLayout(3, false));
		lblMinutes = new Label(minComp, SWT.NONE);
		lblMinutes.setText("Check for changes every");
		txtMinutes = new Text(minComp, SWT.BORDER);
		txtMinutes.setText("0000");
		txtMinutes.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)txtMinutes.getLayoutData()).widthHint = 50;
		lblMinutes2 = new Label(minComp, SWT.NONE);
		lblMinutes2.setText("minutes");
		minComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)minComp.getLayoutData()).horizontalIndent = 10;
		cdMinutes = createControlDecoration(txtMinutes);
		cdMinutes.setDescriptionText("Invalid minutes.  Must be a valid integer.");
		txtMinutes.addModifyListener(new ModifyListener(){
			@Override
			public void modifyText(ModifyEvent e) {
				try{
					Integer.parseInt(txtMinutes.getText());
					cdMinutes.hide();
				}catch (Exception ex){
					cdMinutes.show();
				}
				fireChange(e);
			}
			
		});

		btnPrompt = new Button(g3, SWT.CHECK);
		btnPrompt.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(Option.SYNC_PROMPT_PASSWORD));
		btnPrompt.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(Option.SYNC_PROMPT_PASSWORD));
		btnPrompt.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnPrompt.getLayoutData()).horizontalIndent = 10;
				
		btnDownload = new Button(g3, SWT.CHECK);
		btnDownload.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(Option.SYNC_DOWNLOAD));
		btnDownload.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(Option.SYNC_DOWNLOAD));
		btnDownload.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnDownload.getLayoutData()).horizontalIndent = 10;
		
		btnUpload = new Button(g3, SWT.CHECK);
		btnUpload.setText(ServerOptionLabelProvider.INSTANCE.getOptionLabel(Option.SYNC_AUTO_UPLOAD));
		btnUpload.setToolTipText(ServerOptionLabelProvider.INSTANCE.getOptionTooltip(Option.SYNC_AUTO_UPLOAD));
		btnUpload.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridData)btnUpload.getLayoutData()).horizontalIndent = 20;
		
		if (isEditable){
			SelectionListener ml = new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					updateEnabled();
				}
			};
			btnAutoCheck.addSelectionListener(ml);
			btnDownload.addSelectionListener(ml);
			btnDownShutDown.addSelectionListener(ml);
			btnDownStartUp.addSelectionListener(ml);
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
		}
	}
	
	public boolean isValid(){
		return !cdMinutes.isVisible();
	}
	
	private void fireChange(ModifyEvent e){
		for(ModifyListener m : listeners){
			m.modifyText(e);
		}
	}
	
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
	}
	

	public void initValues(ConnectServer server){
		if (server == null){
			btnDownShutDown.setSelection(ConnectServerOption.Option.DOWNLOAD_ON_SHUTDOWN.getDefaultValueAsBoolean());
			btnDownStartUp.setSelection(ConnectServerOption.Option.DOWNLOAD_ON_STARTUP.getDefaultValueAsBoolean());
			btnUploadShutDown.setSelection(ConnectServerOption.Option.UPLOAD_ON_SHUTDOWN.getDefaultValueAsBoolean());
			btnUploadStartUp.setSelection(ConnectServerOption.Option.UPLOAD_ON_STARTUP.getDefaultValueAsBoolean());
			btnAutoCheck.setSelection(ConnectServerOption.Option.SYNC_AUTOMATICALLY.getDefaultValueAsBoolean());
			btnDownload.setSelection(ConnectServerOption.Option.SYNC_DOWNLOAD.getDefaultValueAsBoolean());
			btnUpload.setSelection(ConnectServerOption.Option.SYNC_AUTO_UPLOAD.getDefaultValueAsBoolean());
			btnPrompt.setSelection(ConnectServerOption.Option.SYNC_PROMPT_PASSWORD.getDefaultValueAsBoolean());
			txtMinutes.setText(ConnectServerOption.Option.SYNC_MINUTE.getDefaultValueAsString());
			updateEnabled();
			return;
		}
		
		btnDownShutDown.setSelection(server.getOptionAsBoolean(ConnectServerOption.Option.DOWNLOAD_ON_SHUTDOWN));
		btnDownStartUp.setSelection(server.getOptionAsBoolean(ConnectServerOption.Option.DOWNLOAD_ON_STARTUP));
		btnUploadShutDown.setSelection(server.getOptionAsBoolean(ConnectServerOption.Option.UPLOAD_ON_SHUTDOWN));
		btnUploadStartUp.setSelection(server.getOptionAsBoolean(ConnectServerOption.Option.UPLOAD_ON_STARTUP));
		btnAutoCheck.setSelection(server.getOptionAsBoolean(ConnectServerOption.Option.SYNC_AUTOMATICALLY));
		btnDownload.setSelection(server.getOptionAsBoolean(ConnectServerOption.Option.SYNC_DOWNLOAD));
		btnUpload.setSelection(server.getOptionAsBoolean(ConnectServerOption.Option.SYNC_AUTO_UPLOAD));
		btnPrompt.setSelection(server.getOptionAsBoolean(ConnectServerOption.Option.SYNC_PROMPT_PASSWORD));
		txtMinutes.setText(String.valueOf(server.getOptionAsInt(ConnectServerOption.Option.SYNC_MINUTE)));
		if (isEditable) updateEnabled();
	}
	
	public void updateServer(ConnectServer server){
		server.setOption(ConnectServerOption.Option.DOWNLOAD_ON_SHUTDOWN, ((Boolean)btnDownShutDown.getSelection()).toString());
		server.setOption(ConnectServerOption.Option.DOWNLOAD_ON_STARTUP, ((Boolean)btnDownStartUp.getSelection()).toString());
		server.setOption(ConnectServerOption.Option.UPLOAD_ON_SHUTDOWN, ((Boolean)btnUploadShutDown.getSelection()).toString());
		server.setOption(ConnectServerOption.Option.UPLOAD_ON_STARTUP, ((Boolean)btnUploadStartUp.getSelection()).toString());
		server.setOption(ConnectServerOption.Option.SYNC_AUTOMATICALLY, ((Boolean)btnAutoCheck.getSelection()).toString());
		server.setOption(ConnectServerOption.Option.SYNC_PROMPT_PASSWORD, ((Boolean)btnPrompt.getSelection()).toString());
		server.setOption(ConnectServerOption.Option.SYNC_DOWNLOAD, ((Boolean)btnDownload.getSelection()).toString());
		server.setOption(ConnectServerOption.Option.SYNC_AUTO_UPLOAD, ((Boolean)btnUpload.getSelection()).toString());
	
		String minutes = ConnectServerOption.Option.SYNC_MINUTE.getDefaultValueAsString();
		try{
			minutes = String.valueOf(Integer.parseInt(txtMinutes.getText()));
		}catch(Exception ex){
			
		}
		server.setOption(ConnectServerOption.Option.SYNC_MINUTE, minutes);	
	}

}


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

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.fieldassist.ControlDecoration;
import org.eclipse.jface.fieldassist.FieldDecorationRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;

/**
 * Composite for collecting server connect information.  Provides option
 * for inputting url and associated certificate.
 * 
 * @author Emily
 *
 */
public class ServerPanel extends Composite implements ModifyListener {

	private static final String USE_EXISTING = Messages.ServerPanel_UseExistingCert;
	private static final String CD_KEY = "cd"; //$NON-NLS-1$
	private static final String VALID_KEY = "valid"; //$NON-NLS-1$
	
	
	private Text txtServer;
	private Text txtCertificate;
	private List<ModifyListener> listeners;
	
	public ServerPanel(Composite parent) {
		super(parent, SWT.NONE);
		
		listeners = new ArrayList<ModifyListener>();
		createControl();
	}
	
	private void createControl(){
		setLayout(new GridLayout(3, false));
		
		Label l = new Label(this, SWT.NONE);
		l.setText(Messages.ServerPanel_UrlLabel);
		
		txtServer = new Text(this, SWT.BORDER);
		txtServer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 2, 1));
		txtServer.addModifyListener(this);
		ControlDecoration cd = createControlDecoration(txtServer);
		txtServer.setData(CD_KEY, cd);
		txtServer.setData(VALID_KEY, false);
		
		l = new Label(this, SWT.NONE);
		l.setText(Messages.ServerPanel_CertificateLabel);
		
		txtCertificate = new Text(this, SWT.BORDER);
		txtCertificate.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		txtCertificate.addModifyListener(this);
		txtCertificate.setData(VALID_KEY, false);
		
		cd = createControlDecoration(txtCertificate);
		txtCertificate.setData(CD_KEY, cd);
		
		Button btnSelect = new Button(this, SWT.PUSH);
		btnSelect.setText("..."); //$NON-NLS-1$
		btnSelect.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				FileDialog fd = new FileDialog(getShell());
				fd.setText(txtCertificate.getText());
				fd.setFilterExtensions(new String[]{"*.pem;*.cer;*.crt;*.def;*.p7b;*.p7c;*.p12;*.pfx", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
				fd.setFilterNames(new String[]{Messages.ServerPanel_CertificateFilterName, Messages.ServerPanel_AllFiles});
				
				String f = fd.open();
				if (f != null){
					txtCertificate.setText(f);
				}
			}
		});
		
		l = new Label(this, SWT.WRAP);
		l.setText(Messages.ServerPanel_CertificateMessage);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 3, 1));
		((GridData)l.getLayoutData()).widthHint = 300;
		
	}
	
	protected ControlDecoration createControlDecoration(Control widget){
		ControlDecoration cd = new ControlDecoration(widget, SWT.LEFT | SWT.TOP);
		cd.setImage(FieldDecorationRegistry.getDefault()
				.getFieldDecoration(FieldDecorationRegistry.DEC_ERROR).getImage());
		cd.setShowHover(true);
		cd.hide();
		return cd;
	}
	
	public boolean isValid(){
		if (! (boolean)txtServer.getData(VALID_KEY)) return false;
		if (! (boolean)txtCertificate.getData(VALID_KEY)) return false;
		return true;
	}
	
	@Override
	public void modifyText(ModifyEvent e) {
		if (e.getSource() == txtServer){
			ControlDecoration scd = (ControlDecoration) txtServer.getData(CD_KEY);
			if (txtServer.getText().isEmpty()){
				scd.show();
				scd.setDescriptionText(Messages.ServerPanel_ServerRequired);
				txtServer.setData(VALID_KEY, false);
			}else{
				scd.hide();
				txtServer.setData(VALID_KEY, true);
			}
		}
		
		if (e.getSource() == txtCertificate){
			ControlDecoration ccd = (ControlDecoration) txtCertificate.getData(CD_KEY);
			ccd.hide();
			txtCertificate.setData(VALID_KEY, true);
			if (!txtCertificate.getText().equals(USE_EXISTING)){
				if (!txtCertificate.getText().trim().isEmpty()){
					boolean err = false;
					try{
						Path p = FileSystems.getDefault().getPath(txtCertificate.getText());
						if (!Files.exists(p)){
							err = true;
						}
					}catch (Exception ex){
						err = true;
					}
					if (err){
						ccd.setDescriptionText(Messages.ServerPanel_InvalidCertificate);
						txtCertificate.setData(VALID_KEY, false);
						ccd.show();
					}
				}
			}
		}
		
		fireChange(e);
	}
	
	private void fireChange(ModifyEvent e){
		for(ModifyListener m : listeners){
			m.modifyText(e);
		}
	}
	public void addChangeListener(ModifyListener listener){
		listeners.add(listener);
	}
	
	public void initValues(ConnectServer server){
		if (server.getServerUrl() != null){
			txtServer.setText(server.getServerUrl());
		}else{
			txtServer.setText("https://localhost:8443/server");
		}
		if (server.getCertificateFileName() != null){
			txtCertificate.setText(USE_EXISTING);
		}else{
			txtCertificate.setText(""); //$NON-NLS-1$
		}
	}
	
	public String getServerUrl(){
		return txtServer.getText();
	}
	/**
	 * blank resets to default;
	 * null makes no changes (should use current)
	 * value - new file
	 * @return
	 */
	public String getCertificateFile(){
		if (txtCertificate.getText().equals(USE_EXISTING)){
			return null;
		}
		return txtCertificate.getText();
	}

}
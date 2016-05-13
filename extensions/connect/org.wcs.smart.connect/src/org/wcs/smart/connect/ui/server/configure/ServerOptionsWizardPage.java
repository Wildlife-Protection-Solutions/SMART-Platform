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

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;

/**
 * Wizard page to enter SMART Connect connection options.
 * 
 * @author Emily
 *
 */
public class ServerOptionsWizardPage extends WizardPage  {

	private IServerOptionsPanel panel;
	
	public ServerOptionsWizardPage(IServerOptionsPanel panel){
		super(panel.getClass().getCanonicalName());
		this.panel = panel;
	}
	@Override
	public void createControl(Composite parent) {
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new GridLayout());
		
		Composite inner = new Composite(outer, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
		
		panel.createComposite(inner, true);
		
		setTitle(Messages.ServerOptionsWizardPage_Title);
		setTitle(panel.getName());
		setMessage(panel.getDescription());
		
		setControl(outer);
	
		ConnectServer tmp = new ConnectServer();
		panel.initValues(tmp);
		panel.addChangeListener(new ModifyListener() {
			@Override
			public void modifyText(ModifyEvent e) {
				getWizard().getContainer().updateButtons();
			}
		});
	}
	
	public boolean isPageComplete(){
		if (!super.isPageComplete()){
			return false;
		}
		return panel.isValid();
	}
	
	public void updateServer(ConnectServer server){
		panel.updateServer(server);
	}
	
	public IServerOptionsPanel getPanel(){
		return this.panel;
	}
}

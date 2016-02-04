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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

/**
 * Configure SMART Connect server wizard.
 * 
 * @author Emily
 *
 */
public class ConnectServerWizard extends Wizard {

	protected ServerWizardPage page1 = null;
	protected UserWizardPage page4 = null; 
	
	protected ServerOptionsWizardPage[] opPages = null;
	
	public ConnectServerWizard(){
		this(true);
	}
	
	public ConnectServerWizard(boolean includeOptionalConfigurations){
		super();
		
		setWindowTitle(Messages.ConnectServerWizard_WizardTitle);
		
		addPage(page1 = new ServerWizardPage());
		
		IServerOptionsPanel[] panels = null;
		if (includeOptionalConfigurations){
			panels = OptionPanelManager.createOptionPanels();
		}else{
			panels = new IServerOptionsPanel[]{new ServerOptionsPanel()};
		}
		opPages = new ServerOptionsWizardPage[panels.length];
		for (int i = 0; i < panels.length; i ++){
			opPages[i] = new ServerOptionsWizardPage(panels[i]);
			addPage(opPages[i]);
		}
		
		addPage(page4 = new UserWizardPage());
	}
	
	@Override
	public boolean performFinish() {
		String url = page1.getServerName();
		String username = page4.getUsername();
		String password = page4.getPassword();
		boolean savePassword = page4.getSavePassword();
		String certificateFile = page1.getCertificateFile();
		
		String error = null;
		
		ConnectServer server = new ConnectServer();
		server.setConservationArea(SmartDB.getCurrentConservationArea());
		server.setServerUrl(url);
		server.setOptions(new HashMap<String, ConnectServerOption>());
		
		for (ServerOptionsWizardPage p : opPages){
			p.updateServer(server);
		}
		
		if (!certificateFile.trim().isEmpty()){
			try{
				server.setCertificateFile(Paths.get(certificateFile));
			}catch (Exception ex){
				ConnectPlugIn.displayLog(Messages.ConnectServerWizard_CertificateError + "\n\n" + ex.getMessage(), ex); //$NON-NLS-1$
				return false;
			}
		}
		
		SmartConnect cs = SmartConnect.findInstance(server, username, password);
		error = cs.validateUser();
		
		if (error != null){
			try{
				//try to delete certificate file
				Files.deleteIfExists(server.getLocalCertificateFile());
			}catch (Exception ex){}
			MessageDialog.openError(getShell(), Messages.ConnectServerWizard_ErrorDialog, error);
			return false;
		}
		
		ConnectUser user = new ConnectUser();
		
		if (savePassword){
			String encryptedPass = null;
			try{
				encryptedPass = ConnectPlugIn.encryptPassword(password);
			}catch (Exception ex){
				ConnectPlugIn.log(Messages.ConnectServerWizard_EncryptPasswordError + ex.getMessage(), ex);
			}
			user.setConnectPassword(encryptedPass);
		}
		user.setConnectUsername(username);
		user.setSmartUser(SmartDB.getCurrentEmployee());
		user.setServer(server);

		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			s.save(server);
			s.save(user);
			s.getTransaction().commit();
		}catch (Exception ex){			
			ConnectPlugIn.displayLog(Messages.ConnectServerWizard_SaveError + ex.getMessage(), ex);
		}finally{
			s.close();
		}
		return true;
	}
}

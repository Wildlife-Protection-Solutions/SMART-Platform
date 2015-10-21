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

import java.nio.file.Paths;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.hibernate.Session;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
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

	protected ServerWizardPage page1;
	protected UserWizardPage page3;
	protected ServerOptionsWizardPage page2;
	
	public ConnectServerWizard(){
		super();
		
		setWindowTitle("Configure SMART Connect Server");
		
		addPage(page1 = new ServerWizardPage());
		addPage(page2 = new ServerOptionsWizardPage());
		addPage(page3 = new UserWizardPage());
	}

	@Override
	public boolean performFinish() {
		String url = page1.getServerName();
		String username = page3.getUsername();
		String password = page3.getPassword();
		String certificateFile = page1.getCertificateFile();
		
		String error = null;
		
		ConnectServer server = new ConnectServer();
		server.setConservationArea(SmartDB.getCurrentConservationArea());
		server.setServerUrl(url);
		server.initalizeOptions();
		page2.updateServer(server);
		
		if (!certificateFile.trim().isEmpty()){
			try{
				server.setCertificateFile(Paths.get(certificateFile));
			}catch (Exception ex){
				ConnectPlugIn.displayLog("Could not copy certificate file to filestore." + "\n\n" + ex.getMessage(), ex);
				return false;
			}
		}
		
		SmartConnect cs = SmartConnect.findInstance(server, username, password);
		error = cs.validateUser();
		
		
		if (error != null){
			//TODO on error; remove copied certificate file
			MessageDialog.openError(getShell(), "Error", error);
			return false;
		}
		
		ConnectUser user = new ConnectUser();
		user.setConnectPassword(password);
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
			ConnectPlugIn.displayLog("Could not save connect server information." + ex.getMessage(), ex);
		}finally{
			s.close();
		}
		return true;
	}
}

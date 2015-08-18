package org.wcs.smart.connect.ui.server.configure;

import java.text.MessageFormat;

import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.wizard.Wizard;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;

public class ConnectServerWizard extends Wizard {

	private ServerWizardPage page1;
	private UserWizardPage page2;
	
	public ConnectServerWizard(){
		super();
		
		setWindowTitle("Configure SMART Connect Server");
		
		addPage(page1 = new ServerWizardPage());
		addPage(page2 = new UserWizardPage());
	}

	@Override
	public boolean performFinish() {
		String url = page1.getServerName();
		String username = page2.getUsername();
		String password = page2.getPassword();
		
		SmartConnect cs = new SmartConnect(url, username, password);
		String error = cs.validateUser();
		if (error != null){
			MessageDialog.openError(getShell(), "Error", error);
			return false;
		}
		
		ConnectServer server = new ConnectServer();
		server.setConservationArea(SmartDB.getCurrentConservationArea());
		server.setServerUrl(url);
		server.setTimeout(120);
			
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

/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.smartcollect.json;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.connect.ui.server.ConnectDialog;
import org.wcs.smart.cybertracker.importer.json.IDesktopJsonProcessor;
import org.wcs.smart.cybertracker.json.JsonImportWarning;
import org.wcs.smart.cybertracker.json.UserCancelledException;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.smartcollect.SmartCollectPlugIn;
import org.wcs.smart.smartcollect.connect.SmartCollectConnectClient;
import org.wcs.smart.smartcollect.internal.Messages;
import org.wcs.smart.smartcollect.model.SmartCollectUser;
import org.wcs.smart.smartcollect.model.SmartCollectUser.State;
import org.wcs.smart.smartcollection.json.SmartCollectJsonProcessor;

import jakarta.ws.rs.core.Response;

/**
 * JSON processor for SMARTCollect data.  Assumes all data for an individual
 * waypoint is included in the same file in order.  More than one waypoint can 
 * be in the file but observations must be in order.
 * 
 * @author Emily
 *
 */
public class DesktopSmartCollectJsonProcessor extends SmartCollectJsonProcessor implements IDesktopJsonProcessor {

	private SmartCollectConnectClient client;

	
	public DesktopSmartCollectJsonProcessor() {
		super(SmartDB.getCurrentConservationArea());
	}
	
	@Override
	protected void logException(String message, Exception ex) {
		SmartCollectPlugIn.log(message, ex);
	}

	
	@Override
	public void processWarnings(List<JsonImportWarning> warnings) throws UserCancelledException {
		displayWarnings(warnings);
	}

	@Override
	public void afterSave() {
		for (Waypoint p : waypoints){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_MODIFIED, p);
			}catch (Exception ex){
				SmartCollectPlugIn.displayLog(Messages.SmartCollectDataProcessor_EventError + ex.getMessage(), ex);
			}
		}
	}

	
	/*
	 * displays warning dialog to user allowing them to cancel the processing
	 */
	private void displayWarnings(List<JsonImportWarning> warnings) throws UserCancelledException{
		 if (!warnings.isEmpty()){
			 	final boolean[] cont = {false};
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						List<String> all = warnings.stream().map(e->e.getMessage()).toList();
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), 
								Messages.SmartCollectDataProcessor_WarningsTitle, 
								Messages.SmartCollectDataProcessor_WarningsMessage,
								all,
								new String[]{IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL}, 0);
						if (wd.open() == 0){
							cont[0] = true;
						}else{
							cont[0] = false;
						}
					}	
				});
				if (!cont[0]){
					throw new UserCancelledException(Messages.SmartCollectDataProcessor_Cancelled);
				}
		 }
	}
	
	@Override
	protected Boolean processNotOkUsers(Set<SmartCollectUser> notok) throws Exception {
		final StringBuilder sb = new StringBuilder();
		for (SmartCollectUser u : notok) {
			sb.append(u.getSource() + " (" + u.getDeviceId() + ") [" + u.getState() + "]"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			sb.append(", "); //$NON-NLS-1$
		}
		sb.delete(sb.length() - 2, sb.length());
		
		ProcessingOption[] cancel = {ProcessingOption.CANCEL};
		Display.getDefault().syncExec(()->{
			ValidationDialog dialog = new ValidationDialog(Display.getDefault().getActiveShell(), sb.toString());
			dialog.open();
			cancel[0] = dialog.getSelectedOption();
		});
		
		if (cancel[0] == ProcessingOption.CANCEL) {
			throw new UserCancelledException(Messages.SmartCollectDataProcessor_Cancelled);
		}else if (cancel[0] == ProcessingOption.LOADDATA) {
			//temporarily set to validated for the purposes of processing this dataset
			for (SmartCollectUser u : notok) u.setState(State.VALIDATED);
		}else if (cancel[0] == ProcessingOption.DISCARD) {
			return true;
		}else if (cancel[0] == ProcessingOption.ACCEPTANDLOAD) {			
			for (SmartCollectUser u : notok) {
				try {
					validateUser(u);
				}catch (Exception ex) {
					throw new Exception(MessageFormat.format(Messages.SmartCollectDataProcessor_UserValidationFailed,u.getSource(),ex.getMessage()), ex);
				}
			}
		}else if (cancel[0] == ProcessingOption.BLACKLISTANDDISCARD) {
			for (SmartCollectUser u : notok) {
				try {
					blacklistUser(u);
				}catch (Exception ex) {
					throw new Exception(MessageFormat.format(Messages.SmartCollectDataProcessor_UserBlackListFailed,u.getSource(),ex.getMessage()), ex);
				}
			}
		}else if (cancel[0] == ProcessingOption.VERIFYREQUEUE) {
			for (SmartCollectUser u : notok) {
				try {
					sendEmailRequest(u);
				}catch (Exception ex) {
					throw new Exception(MessageFormat.format(Messages.SmartCollectDataProcessor_EmailSendFailed,u.getSource(),ex.getMessage()), ex);
				}
			}
			return null;
		}
		
		return false;
	}

	
	protected Map<SmartCollectJsonProcessor.DeviceUser, SmartCollectUser> getUserState(Set<DeviceUser> users) {
		
		SmartConnect[] connect = {null};

		try {
			ConnectServer cs = null;
			ConnectUser user = null;
			try(Session session = HibernateManager.openSession()){
				cs = ConnectHibernateManager.getConnectServer(session);
				user = ConnectHibernateManager.getConnectUser(SmartDB.getCurrentEmployee(), session);			
			}catch (Exception ex) {
				throw ex;
			}
			if (user.getConnectPassword().isBlank()) throw new Exception();
		
			SmartConnect temp = SmartConnect.findInstance(cs, user.getConnectUsername(), ConnectPlugIn.decryptPassword(user));
			if (temp == null) throw new Exception();
			
			String error = temp.validateUser();
			if (error != null) throw new Exception();
			
			connect[0] = temp;
		}catch (Exception ex){
			
		}
		
		if (connect[0] == null) {
			//prompt for server details
			Display.getDefault().syncExec(()->{
				ConnectDialog cd = new ConnectDialog(Display.getCurrent().getActiveShell(), true) {
					@Override
					protected Control createDialogArea(Composite parent) {
						setTitle(Messages.SmartCollectDataProcessor_Title);
						getShell().setText(Messages.SmartCollectDataProcessor_Title);
						setMessage(Messages.SmartCollectDataProcessor_Message);	
						return super.createDialogArea(parent);
					}
				};
				if (cd.open() == Window.OK) {
					connect[0] = cd.getConnection();
				}
			});
		}
		
		if (connect[0] == null) return null;
		
		
		ResteasyClient rclient = connect[0].getClient();
		ResteasyWebTarget target = rclient.target(connect[0].getServer().getServerUrl() + SmartConnect.API_URL);
		client = target.proxy(SmartCollectConnectClient.class);
		
		Map<DeviceUser, SmartCollectUser> cusers = new HashMap<>();
		for (DeviceUser user : users) {
			List<SmartCollectUser> cuser = client.getUsers(user.getUser(), user.getDeviceId());
			for (SmartCollectUser i : cuser) cusers.put(user,i);
		}
		
		return cusers;
	}
	
	private void sendEmailRequest(SmartCollectUser user) {
		try(Response r = client.updateUserState(user.getUuid().toString(), SmartCollectUser.State.VALIDATED.name(), Boolean.TRUE.toString())){}
	}
	
	private void validateUser(SmartCollectUser user) {
		try(Response r = client.updateUserState(user.getUuid().toString(), SmartCollectUser.State.VALIDATED.name(), Boolean.FALSE.toString())){
			user.setState(State.VALIDATED);
		}
	}
	
	private void blacklistUser(SmartCollectUser user) {
		try(Response r = client.updateUserState(user.getUuid().toString(), SmartCollectUser.State.BLACKLISTED.name(), Boolean.FALSE.toString())){
			user.setState(State.BLACKLISTED);
		}
	}
	
	
}

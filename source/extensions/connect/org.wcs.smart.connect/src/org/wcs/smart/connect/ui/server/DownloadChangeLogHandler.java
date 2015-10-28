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
package org.wcs.smart.connect.ui.server;

import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;

import javax.inject.Named;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.internal.ErrorEditorPart;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jdbc.Work;
import org.wcs.smart.LogoutHandler;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.SmartConnect;
import org.wcs.smart.connect.server.replication.DownloadChangeLogEngine;
import org.wcs.smart.hibernate.ConservationAreaConfiguration;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.UserNamePasswordDialog;
import org.wcs.smart.util.E3Utils;

/**
 * Download change log handler.
 * 
 * @author Emily
 *
 */
public class DownloadChangeLogHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell, 
			final EPartService pService, IEventBroker events) {
		DownloadChangeLogDialog dialog = new DownloadChangeLogDialog(activeShell);
		if (dialog.open() == Window.OK){
			downloadChangeLog(activeShell, pService, dialog.getConnection(), events);
		}
	}

	private void downloadChangeLog(Shell activeShell, final EPartService pService, 
			final SmartConnect connect, final IEventBroker events){
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(activeShell);
		try {
			pmd.run(true, false, new IRunnableWithProgress() {
				@Override
				public void run(final IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					monitor.beginTask("Download and Apply Changelog", 3);
					DownloadChangeLogEngine engine = new DownloadChangeLogEngine(connect);
					try{
						if (engine.downloadInstall(pService, new SubProgressMonitor(monitor, 1))){
							//refresh ui
							monitor.subTask("Updating Current User");
							if (!checkUser()){
								return;
							}
							monitor.worked(1);
							monitor.subTask("Refreshing UI");
							refreshUi(pService, events);
							monitor.worked(1);
							
						}
					}catch (Exception ex){
						ConnectPlugIn.displayLog(ex.getMessage(), ex);
					}
					monitor.done();
				}
			});
		} catch (InvocationTargetException | InterruptedException e) {
			ConnectPlugIn.displayLog(e.getMessage(), e);
		}
	}

	/*
	 * validates that the current user still has same username/password
	 * and permissions after download sync.  If not then the
	 * application must restart or ask user to re-enter credentials.
	 */
	private boolean checkUser(){
		Session s = HibernateManager.openSession();
		
		final Employee afterDownload = (Employee) s.createCriteria(Employee.class)
			.add(Restrictions.eq("uuid", SmartDB.getCurrentEmployee().getUuid()))
			.uniqueResult();
		final boolean[] cont = new boolean[]{true};
		if (afterDownload == null ||
				afterDownload.getSmartUserLevel() == null ||
				!afterDownload.getSmartUserLevel().equals(SmartDB.getCurrentEmployee().getSmartUserLevel())){
			//current employee no longer exists; 
			//or no longer a smart user
			//or user level has changed
			//logout
			Display.getDefault().syncExec(new Runnable(){
				@Override
				public void run() {
					MessageDialog.openError(Display.getDefault().getActiveShell(), "Restart", "Your SMART user account has been removed or modified.  The system will restart and you must log back in.");
					(new LogoutHandler()).execute();
					cont[0] = false;
				}
			});
		}
		if (!cont[0]) return false;
		
		//ensure usernames & passwords are the same
		//we are comparing the hashed values here; not the unhashed values
		//this is the best we can do but will not be perfect
		if (!afterDownload.getSmartUserId().equals(SmartDB.getCurrentEmployee().getSmartUserId()) ||
			!afterDownload.getSmartPassword().equals(SmartDB.getCurrentEmployee().getSmartPassword())){
			//the username or password are different we need to prompt for the new password
			//if they cannot provide the necessary info, we need to logout
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					boolean ok = false;
					String password = null;
					while(!ok){
						UserNamePasswordDialog dialog = new UserNamePasswordDialog(Display.getCurrent().getActiveShell(), 
							"Re-Enter Credentials", "Your SMART account was changed, you need to re-enter your credentials before you can continue", "OK");
						if (dialog.open() == Window.CANCEL){
							//logout
							(new LogoutHandler()).execute();
							cont[0] = false;
							return;
						}else{
							//validate
							String username = dialog.getUserName();
							password = dialog.getPassword();
							
							String error = null;
							try{
								Employee validated = HibernateManager.validateUser(username, password, afterDownload.getConservationArea());
								if(validated == null || 
									!validated.getUuid().equals(afterDownload.getUuid())){
									error = "Invalid username/password.";
								}
							}catch (Exception ex){
								ConnectPlugIn.log(ex.getMessage(), ex);
								error = ex.getMessage();
							}
							
							if (error != null){
								MessageDialog.openError(Display.getCurrent().getActiveShell(), "Login Error", error);
							}else{
								ok = true;
							}
						}
					}

					//update configuration to use new employee
					ConservationAreaConfiguration cc = new ConservationAreaConfiguration(Collections.singleton(SmartDB.getCurrentConservationArea()), 
							Collections.singleton(afterDownload), s);
					SmartDB.setConservationAreaConfiguration(afterDownload, 
							password, SmartDB.getCurrentConservationArea(), cc);
				}
			});
		}
		if (!cont[0]) return false;
		return true;
	}
	private void refreshUi(final EPartService pService,
			final IEventBroker events) {
		Display.getDefault().syncExec(new Runnable(){

			@Override
			public void run() {
				
				MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Download Complete", "Download complete.");
				
				events.post(SmartPlugIn.E4_DATABASE_CHANGED_EVENT, null);
				
				//find all editors, close and reopen
				Collection<MPart> parts = pService.getParts();
				for (MPart part : parts){
					if (E3Utils.isCompatibilityEditor(part)){
						PartState state = PartState.CREATE;
						if (pService.isPartVisible(part)){
							state = PartState.VISIBLE;
						}
						pService.hidePart(part, false);
						try{
							pService.showPart(part, state);
							Object e3part = E3Utils.getSourceObject(part);
//							if (e3part instanceof EditorPart && )
							if (e3part instanceof ErrorEditorPart){
//								System.out.println(((EditorPart) e3part).getEditorSite().getId());
								pService.hidePart(part, true);
							}
						}catch (Throwable ex){
							//eat me; likely the input behind the part was removed
							pService.hidePart(part, true);
						}
					}
				}
			}
			
		});
	}
	
	public static class DownloadChangeLogHandlerWrapper extends DIHandler<DownloadChangeLogHandler>{
		public DownloadChangeLogHandlerWrapper() {
			super(DownloadChangeLogHandler.class);
		}
		
	}
}

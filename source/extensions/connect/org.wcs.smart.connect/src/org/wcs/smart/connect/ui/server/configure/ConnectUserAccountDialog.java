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

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectUser;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Dialog for collecting a connect username and password for a given server.
 * Will default to the stored username, password.
 * 
 * @author Emily
 *
 */
public class ConnectUserAccountDialog extends TitleAreaDialog{

	private Text txtUser;
	private List<ConnectUser> toUpdate;

	private CheckboxTableViewer lstUsers;
	
	private ConnectServer server;
	
	public ConnectUserAccountDialog(Shell parentShell, ConnectServer server, List<ConnectUser> toUpdate) {
		super(parentShell);
		
		this.toUpdate = toUpdate;
		this.server = server;
	}

	protected void okPressed() {
		String username = txtUser.getText().trim();
		if (username.isEmpty()){
			MessageDialog.openError(getParentShell(), Messages.ConnectUserAccountDialog_ErrorDialog, Messages.ConnectUserAccountDialog_InvalidUsername);
			return;
		}

		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			if (toUpdate != null){
				for (ConnectUser cu : toUpdate){
					s.saveOrUpdate(cu);
					cu.setConnectUsername(username);
				}
			}else{
				for (Object e : lstUsers.getCheckedElements()){
					if (! (e instanceof Employee) ) continue;
					ConnectUser user = new ConnectUser();
					user.setConnectUsername(username);
					user.setSmartUser((Employee)e);
					user.setServer(server);
					s.save(user);
				}
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			ConnectPlugIn.displayLog(Messages.ConnectUserAccountDialog_UpdateError + ex.getMessage(), ex);
			return;
		}finally{
			s.close();
		}
		super.okPressed();
	}
	
	
	@Override
	protected Control createDialogArea(Composite parent) {
		parent = (Composite) super.createDialogArea(parent);
		
		Composite outer = new Composite(parent, SWT.NONE);
		outer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		outer.setLayout(new GridLayout());
		
		Composite inner = new Composite(outer, SWT.NONE);
		inner.setLayout(new GridLayout(2, false));
		inner.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, true));
		
		Label l = new Label(inner, SWT.NONE);
		l.setText(Messages.ConnectUserAccountDialog_UsernameLabel);
	
		txtUser = new Text(inner, SWT.BORDER);
		txtUser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		if (toUpdate == null){
			inner.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			
			l = new Label(inner, SWT.NONE);
			l.setText(Messages.ConnectUserAccountDialog_DesktopAccountLabel);
			l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
			lstUsers = CheckboxTableViewer.newCheckList(inner, SWT.BORDER | SWT.V_SCROLL);
			lstUsers.getTable().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			lstUsers.setContentProvider(ArrayContentProvider.getInstance());
			lstUsers.setInput(new String[]{Messages.ConnectUserAccountDialog_LoadingLabel});
			lstUsers.setLabelProvider(new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof Employee){
						Employee e = (Employee) element;
						return e.getSmartUserId() + " (" + SmartLabelProvider.getShortLabel(e) + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return super.getText(element);
				}
			});
			
			loadUsers.setSystem(true);
			loadUsers.schedule();
		}
		
		setTitle(Messages.ConnectUserAccountDialog_Title);
		getShell().setText(Messages.ConnectUserAccountDialog_ShellTitle);
		if (toUpdate != null){
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.ConnectUserAccountDialog_Message);
			for (ConnectUser u : toUpdate){
				sb.append(u.getSmartUser().getSmartUserId() + ", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			setMessage(sb.toString());
			
		}else{
			setMessage(Messages.ConnectUserAccountDialog_LinkMessage);
		}
		
		return outer;
	}
	
	@Override
	public boolean isResizable(){
		return true;
	}
	
	Job loadUsers = new Job(Messages.ConnectUserAccountDialog_loadusersjobname){
		@Override
		@SuppressWarnings("unchecked")
		protected IStatus run(IProgressMonitor monitor) {
			List<Employee> e = null;
			Session s = HibernateManager.openSession();
			s.beginTransaction();
			try{
				String q = "FROM Employee e WHERE e.conservationArea = :ca and smartUserId is not null and e.uuid not in (SELECT uuid FROM ConnectUser)"; //$NON-NLS-1$
				Query query = s.createQuery(q);
				query.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
				e = query.list();
				s.getTransaction().commit();
			}catch (Exception ex){
				if (s.getTransaction().isActive()) s.getTransaction().rollback();
				ConnectPlugIn.log("Could not load SMART Desktop user:" + ex.getMessage(), ex); //$NON-NLS-1$
				return Status.OK_STATUS;
			}finally{
				s.close();
			}
			
			final List<Employee> emp = e;
			Display.getDefault().syncExec(new Runnable(){

				@Override
				public void run() {
					lstUsers.setInput(emp);
				}
				
			});
			return Status.OK_STATUS;
		}
		
	};
}

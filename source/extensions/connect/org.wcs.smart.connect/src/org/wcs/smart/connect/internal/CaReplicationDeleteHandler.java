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
package org.wcs.smart.connect.internal;

import javax.transaction.Status;
import javax.transaction.Synchronization;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ICaDeleteHandler;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.replication.DerbyReplicationManager;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Delete handler that turns off replication.  Should be run before any
 * other delete handlers.
 * 
 * @author Emily
 *
 */
public class CaReplicationDeleteHandler implements ICaDeleteHandler {

	/**
	 * To be executed first.
	 */
	public static final int EXECUTE_ORDER = 1000;
	
	private Exception resetException;
	
	public CaReplicationDeleteHandler() {}

	@Override
	public void beforeDelete(ConservationArea ca, final Session session, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.CaReplicationDeleteHandler_TaskName);
		
		//register a listener to re-enable replication if
		//the delete cannot be committed.
		session.getTransaction().registerSynchronization(new Synchronization() {
			@Override
			public void beforeCompletion() {
			}
			
			@Override
			public void afterCompletion(int status) {
				if (status != Status.STATUS_COMMITTED){
					//transaction not committed we need to re-enable replication
					//TODO: we should only do this if replication was enabled in the first place
					resetException = null;
					Job restart = new Job(Messages.CaReplicationDeleteHandler_JobName) {
						@Override
						protected IStatus run(IProgressMonitor monitor) {
							//run in job to get a new session as cannot reuse the existing one
							Session s = HibernateManager.openSession();
							try {
								s.beginTransaction();
								DerbyReplicationManager.INSTANCE.enableReplication(session);
								s.getTransaction().commit();
							}catch (Exception ex){
								resetException = ex;
							}finally{
								s.close();
							}
							return org.eclipse.core.runtime.Status.OK_STATUS;
						}
					};
					restart.schedule();
					try{
						restart.join();
					}catch (InterruptedException ex){
						resetException = ex;
					}
					if (resetException != null){
						ConnectPlugIn.displayLog(Messages.CaReplicationDeleteHandler_DeleteError, resetException);
						//restart
						Display.getDefault().syncExec(new Runnable(){
							@Override
							public void run() {
								PlatformUI.getWorkbench().restart();		
							}	
						});
					}
				}
			}
		});
		
		DerbyReplicationManager.INSTANCE.disableReplication(session);
	}
}

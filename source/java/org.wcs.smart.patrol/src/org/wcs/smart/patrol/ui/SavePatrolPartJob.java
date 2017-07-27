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
package org.wcs.smart.patrol.ui;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.PatrolEventManager;
import org.wcs.smart.patrol.PatrolHibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.WaypointAttachmentInterceptor;
/**
 * Job for saving a patrol part (patrol leg, leg day etc)
 * to the database.
 * 
 * @author Emily
 *
 */
public class SavePatrolPartJob extends Job {

	private Object patrolPart;
	private Patrol patrol;
	
	/**
	 * 
	 * @param p the patrol the part belongs to 
	 * @param patrolPart the part to save
	 */
	public SavePatrolPartJob(Patrol p, Object patrolPart){
		super(Messages.SavePatrolPartJob_Name);
		this.patrol = p;
		this.patrolPart = patrolPart;
		
	}
	@Override
	protected IStatus run(IProgressMonitor monitor) {
		try(Session saveSession = HibernateManager
				.openSession(new WaypointAttachmentInterceptor())){
			saveSession.beginTransaction();
		
			try{
			
				if (patrolPart instanceof Patrol) {
					if (((Patrol) patrolPart).getId() == null) {
						String id = PatrolHibernateManager.generatePatrolId(
							((Patrol) patrolPart), saveSession);
						((Patrol) patrolPart).setId(id);
					}
				}
			
				saveSession.saveOrUpdate(patrolPart);
				saveSession.getTransaction().commit();
			}catch (Exception ex){
				if (saveSession.getTransaction().isActive()){
					saveSession.getTransaction().rollback();
				}
				SmartPatrolPlugIn.displayLog(Messages.PatrolEditor_Error_SavingPatrol + ex.getLocalizedMessage(), ex);
				return Status.OK_STATUS;
			}
		}				
		PatrolEventManager.getInstance().patrolSaved(patrol, false);
		return Status.OK_STATUS;
	}

}

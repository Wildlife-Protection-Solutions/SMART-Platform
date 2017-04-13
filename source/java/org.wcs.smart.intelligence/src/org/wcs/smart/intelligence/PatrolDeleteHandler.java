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
package org.wcs.smart.intelligence;

import java.text.MessageFormat;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.intelligence.internal.Messages;
import org.wcs.smart.intelligence.model.Intelligence;
import org.wcs.smart.patrol.IPatrolDeleteHandler;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Delete handler for deleting all intelligence information attached to the patrol.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class PatrolDeleteHandler implements IPatrolDeleteHandler {
	
	public static final int EXECUTE_ORDER = 1;

	private List<Intelligence> removedList;

	public PatrolDeleteHandler() {}

	@Override
	public boolean beforeDelete(Patrol patrol, Session session, IProgressMonitor monitor) throws Exception {
		monitor.subTask(Messages.PatrolDeleteHandler_Task_FetchIds);
		removedList = getIntelligenceIds(patrol, session);
		
		if (removedList.size() > 0){
			//confirm delete
			final String message = 
					MessageFormat.format(Messages.PatrolDeleteHandler_DeleteWarningMessage,
							new Object[]{patrol.getId(), removedList.size()});
			final boolean info[] = {false};
			Display.getDefault().syncExec(new Runnable() {
				
				@Override
				public void run() {
					info[0] = MessageDialog.openQuestion(Display.getDefault().getActiveShell(), 
							Messages.PatrolDeleteHandler_DeleteWarningTitle,
							message);
					
				}
			});
			
			if (!info[0]){
				return false;
			}
		}
		
		monitor.subTask(Messages.PatrolDeleteHandler_Task_DeleteIntelligence);
		deleteIntelligences(patrol, session);
		return true;
		
		
	}

	@Override
	public void afterDelete(Patrol patrol, IProgressMonitor monitor) {
		if (removedList != null) {
			monitor.subTask(Messages.PatrolDeleteHandler_Task_DeleteFilestore);
			for (Intelligence intelligence : removedList) {
				IntelligenceHibernateManager.deleteFilestore(intelligence);
				IntelligenceEventManager.getInstance().intelligenceDeleted(intelligence);
			}
		}
		
	}
	
	@SuppressWarnings("unchecked")
	private List<Intelligence> getIntelligenceIds(Patrol patrol, Session session) throws Exception{
		Query q = session.createQuery("from Intelligence i where i.patrol = :p"); //$NON-NLS-1$
		q.setParameter("p", patrol); //$NON-NLS-1$
		return q.list();
	}

	private void deleteIntelligences(Patrol patrol, Session session) throws Exception{
		Query q = session.createQuery("delete from Intelligence where patrol = :p"); //$NON-NLS-1$
		q.setParameter("p", patrol); //$NON-NLS-1$
		q.executeUpdate();
	}

	
}

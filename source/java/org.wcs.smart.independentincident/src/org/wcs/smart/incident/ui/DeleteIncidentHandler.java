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
package org.wcs.smart.incident.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.IncidentPlugIn;
import org.wcs.smart.incident.event.IncidentEventManager;
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.events.WaypointEventManager;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Delete incident handler.
 * @author Emily
 *
 */
public class DeleteIncidentHandler extends AbstractHandler{

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection selection = HandlerUtil.getCurrentSelection(event);
		
		if (!(selection instanceof IStructuredSelection)){
			return null;
		}
		IStructuredSelection sel = (IStructuredSelection) selection;
		List<IncidentEditorInput> toDelete = new ArrayList<IncidentEditorInput>();
		
		for (Iterator<?> iterator = sel.iterator(); iterator.hasNext();) {
			Object waypoint = (Object) iterator.next();
			if (waypoint instanceof Waypoint){
				Waypoint wp = (Waypoint)waypoint;
				toDelete.add(new IncidentEditorInput(wp.getUuid(), wp.getId(),wp.getDateTime()));
			}else if (waypoint instanceof IncidentEditorInput){
				toDelete.add(((IncidentEditorInput)waypoint));
			}
		}
		if (toDelete.size() == 0){
			//nothing to delete
			return null;
		}
		if (!MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), Messages.DeleteIncidentHandler_ConfirmLabel, MessageFormat.format(Messages.DeleteIncidentHandler_ConfirmMessage, new Object[]{toDelete.size()}))){
			return null;
		}
				
		Session s = HibernateManager.openSession();
		try{
			s.beginTransaction();
			Query q=s.createQuery("delete Waypoint w where w.uuid = :wp"); //$NON-NLS-1$
			for (IncidentEditorInput w : toDelete){
				q.setParameter("wp", w.getUuid()); //$NON-NLS-1$
				q.executeUpdate();
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			IncidentPlugIn.displayLog(Messages.DeleteIncidentHandler_Error1 + ex.getMessage(), ex);
		}
		
		//fire events
		for (IncidentEditorInput w : toDelete){
			try{
				IncidentEventManager.getInstance().fireEvent(IncidentEventManager.INCIDENT_DELETED, w);
				
				Waypoint tmp = new Waypoint();
				tmp.setUuid(w.getUuid());
				WaypointEventManager.getInstance().waypointDeleted(tmp);
			}catch (Exception ex){
				IncidentPlugIn.displayLog(Messages.DeleteIncidentHandler_Error2 +ex.getMessage(), ex);
			}
		}
	
		return null;
	}
}

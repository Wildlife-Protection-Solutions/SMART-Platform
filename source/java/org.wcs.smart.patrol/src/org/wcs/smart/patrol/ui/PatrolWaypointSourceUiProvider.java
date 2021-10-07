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

import java.util.UUID;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.IWaypointSourceProvider;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;

/**
 * Source provider for patrol waypoints that opens
 * the associated patrol editor and selects
 * the waypoint 
 * 
 * @author Emily
 *
 */
public class PatrolWaypointSourceUiProvider implements
		IWaypointSourceProvider {

	@Override
	public void findAndShow(UUID waypointUuid) {
		PatrolWaypoint pw = null;
		
		try(Session s = HibernateManager.openSession()){
			
			CriteriaBuilder cb = s.getCriteriaBuilder();
			CriteriaQuery<PatrolWaypoint> c = cb.createQuery(PatrolWaypoint.class);
			Root<PatrolWaypoint> from = c.from(PatrolWaypoint.class);
			c.where(cb.equal(from.get("id").get("waypoint").get("uuid"), waypointUuid)); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			pw = s.createQuery(c).uniqueResult();
			
			if (pw == null){
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						ERROR_STR, 
						Messages.PatrolWaypointSourceUiProvider_WaypointNotFound);
				return;
			}
			Patrol p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
			p.getPatrolType();
			pw.getWaypoint().getUuid();
		}
		
		
		PatrolEditorInput in = new PatrolEditorInput(pw.getPatrolLegDay().getPatrolLeg().getPatrol());
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ctx = ctx.getActiveLeaf().createChild();
		ctx.set(OpenPatrolHandler.PATROL_PARAM, in);
		ctx.set(OpenPatrolHandler.INIT_SELECTION_WP_UUID, waypointUuid);
		
		ContextInjectionFactory.invoke(new OpenPatrolHandler(),
					Execute.class, ctx.getActiveLeaf());

	}

}

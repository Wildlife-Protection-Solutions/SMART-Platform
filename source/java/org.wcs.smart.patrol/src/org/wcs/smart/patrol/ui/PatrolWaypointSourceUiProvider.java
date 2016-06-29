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

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.model.IWaypointSourceUiProvider;
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
		IWaypointSourceUiProvider {

	@Override
	public void findAndShow(UUID waypointUuid) {
		PatrolWaypoint pw = null;
		Session s = HibernateManager.openSession();
		try{
			pw = (PatrolWaypoint)s.createCriteria(PatrolWaypoint.class)
					.add(Restrictions.eq("id.waypoint.uuid", waypointUuid)) //$NON-NLS-1$
					.uniqueResult();
			if (pw == null){
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						ERROR_STR, 
						Messages.PatrolWaypointSourceUiProvider_WaypointNotFound);
				return;
			}
			Patrol p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
			p.getPatrolType();
			pw.getWaypoint().getUuid();
		}finally{
			s.close();
		}
		
		
		PatrolEditorInput in = new PatrolEditorInput(pw.getPatrolLegDay().getPatrolLeg().getPatrol());
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ctx.set(OpenPatrolHandler.PATROL_PARAM, in);
		ctx.set(OpenPatrolHandler.INIT_SELECTION_WP_UUID, waypointUuid);
		
		ContextInjectionFactory.invoke(new OpenPatrolHandler(),
					Execute.class, ctx.getActiveLeaf());

	}

}

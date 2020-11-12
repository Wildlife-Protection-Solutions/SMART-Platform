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
package org.wcs.smart.patrol.query.model.types;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.query.model.types.ShowItemInfoProvider;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.model.PatrolWaypoint;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.model.IPatrolQueryResultItem;
import org.wcs.smart.patrol.ui.OpenPatrolHandler;
import org.wcs.smart.patrol.ui.PatrolEditorInput;
import org.wcs.smart.query.common.engine.IAttachmentResultItem;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.common.engine.WaypointQueryResultItem;

/**
 * Patrol info provider for opening up original patrol
 * 
 * @author Emily
 *
 */
public class PatrolResultInfoProvider extends ShowItemInfoProvider{
	
	@Override
	public boolean supportsMap(){
		return true;
	}
	
	private void showItem(PatrolEditorInput in, UUID waypointUuid) {
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ctx.set(OpenPatrolHandler.PATROL_PARAM, in);
		if (waypointUuid != null){
			ctx.set(OpenPatrolHandler.INIT_SELECTION_WP_UUID, waypointUuid);
		}
		ContextInjectionFactory.invoke(new OpenPatrolHandler(),
				Execute.class, ctx.getActiveLeaf());
	}
	
	@Override
	public void doWork(IResultItem resultItem) {
		if (resultItem instanceof IPatrolQueryResultItem) {
			IPatrolQueryResultItem it = (IPatrolQueryResultItem)resultItem;
			PatrolEditorInput in = new PatrolEditorInput(it.getPatrolUuid(), it.getPatrolId(), it.getPatrolType(), it.getPatrolStartDate(), it.getPatrolEndDate());
			UUID wpUuid = null;
			if (resultItem instanceof WaypointQueryResultItem) {
				wpUuid =((WaypointQueryResultItem) resultItem).getWaypointUuid();
			}
			showItem(in, wpUuid);
			return;
		}else if (resultItem instanceof IAttachmentResultItem) {
			PatrolEditorInput input = null;
			PatrolWaypoint pw = null;
			try(Session s = HibernateManager.openSession()){
				pw = PatrolQueryPlugIn.findWaypoint(s, (IAttachmentResultItem)resultItem);
				if (pw != null) {
					Patrol p = pw.getPatrolLegDay().getPatrolLeg().getPatrol();
					input = new PatrolEditorInput(p);
				}
			}
			if (input != null) {
				showItem(input, pw.getWaypoint().getUuid());
				return;
			}
			
		}
		MessageDialog.openError(Display.getDefault().getActiveShell(),ERROR_STR,
						MessageFormat .format(OP_NOT_SUPPORTED_STR, resultItem.getClass().getName()));
	}


}

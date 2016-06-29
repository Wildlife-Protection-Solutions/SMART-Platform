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
import org.wcs.smart.incident.internal.Messages;
import org.wcs.smart.observation.model.IWaypointSourceUiProvider;
import org.wcs.smart.observation.model.Waypoint;

/**
 * Source provider for independent incidents that opens
 * the incident editor.
 * 
 * @author Emily
 *
 */
public class IndIncidentSourceUiProvider implements
		IWaypointSourceUiProvider {

	@Override
	public void findAndShow(UUID waypointUuid) {
		Waypoint pw = null;
		Session s = HibernateManager.openSession();
		try{
			pw = (Waypoint)s.createCriteria(Waypoint.class)
					.add(Restrictions.eq("uuid", waypointUuid)) //$NON-NLS-1$
					.uniqueResult();
			if (pw == null){
				MessageDialog.openError(Display.getDefault().getActiveShell(),
						ERROR_STR, 
						Messages.IndIncidentSourceUiProvider_WaypointNotFound);
				return;
			}
			
		}finally{
			s.close();
		}
		
			
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		ctx.set(OpenIncidentHandler.UUID_PARAM, waypointUuid);
		ContextInjectionFactory.invoke(new OpenIncidentHandler(),
					Execute.class, ctx.getActiveLeaf());

	}

}

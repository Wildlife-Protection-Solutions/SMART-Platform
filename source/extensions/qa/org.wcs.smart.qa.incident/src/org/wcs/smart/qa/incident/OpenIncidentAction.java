/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa.incident;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.ui.OpenIncidentHandler;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.incident.internal.Messages;
import org.wcs.smart.qa.model.IQaAction;
import org.wcs.smart.qa.model.QaError;

/**
 * Opens the source patrol editor.  Works for
 * results from PatrolWaypointDataProvider or PatrolTrackDataProvider
 * 
 * @author Emily
 *
 */
public class OpenIncidentAction implements IQaAction {

	@Inject
	private IEclipseContext context;
	
	public OpenIncidentAction(){
	}

	@Override
	public boolean doAction(List<QaError> items) {
		if (items.isEmpty()) return false;
		QaError item = items.get(0);
		if (item.getDataProviderId().equals(IncidentDataProvider.ID) ||
				item.getDataProviderId().equals(IntegrateIncidentDataProvider.ID)){
			Waypoint pw = null;
			
			try(Session s = HibernateManager.openSession()){
				pw = (Waypoint) s.get(Waypoint.class, item.getSourceId());
			}
			if (pw == null){
				//not found
				MessageDialog.openError(Display.getDefault().getActiveShell(), Messages.OpenIncidentAction_NotFoundTitle, MessageFormat.format(Messages.OpenIncidentAction_NotFoundMsg, item.getErrorId()));
			}else{
				(new OpenIncidentHandler()).openIncident(pw.getUuid(), pw.getSourceId(), context.get(MWindow.class));	
			}
		}
		return false;
	}

	@Override
	public boolean supportsMultiple() {
		return false;
	}

	@Override
	public String getId() {
		return GOTO_ACTION_ID;
	}

	@Override
	public String getName(Locale l) {
		return Messages.OpenIncidentAction_ActionName;
	}
}

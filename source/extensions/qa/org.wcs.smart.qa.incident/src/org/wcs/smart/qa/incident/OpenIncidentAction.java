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
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.incident.ui.OpenIncidentHandler;
import org.wcs.smart.observation.model.Waypoint;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.routine.IQaAction;

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
		if (item.getDataProviderId().equals(IncidentDataProvider.ID)){
			Waypoint pw = null;
			Session s = HibernateManager.openSession();
			try{
				pw = (Waypoint) s.get(Waypoint.class, item.getSourceId());
			}finally{
				s.close();
			}
			if (pw == null){
				//not found
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Not Found", MessageFormat.format("Independent Incident {0} not found", item.getErrorId()));
			}else{
				(new OpenIncidentHandler()).openIncident(pw.getUuid(), context.get(MWindow.class));	
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
		return "org.wcs.smart.qa.incident.goto"; //$NON-NLS-1$
	}

	@Override
	public String getName(Locale l) {
		return "Goto Source";
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON);
	}
}

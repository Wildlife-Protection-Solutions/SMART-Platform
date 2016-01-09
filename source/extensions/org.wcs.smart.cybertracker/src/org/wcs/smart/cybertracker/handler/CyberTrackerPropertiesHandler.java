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
package org.wcs.smart.cybertracker.handler;

import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.cybertracker.CyberTrackerHibernateManager;
import org.wcs.smart.cybertracker.model.CyberTrackerPropertiesProfile;
import org.wcs.smart.cybertracker.properties.CyberTrackerPropertiesDialog;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * Handler for opening dialog to edit properties used be default for CyberTracker application.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class CyberTrackerPropertiesHandler {

	@Execute
	public void execute (Shell shell) {
		//TODO: launch profile manager dialog
		
		//temp code start
		Session session = HibernateManager.openSession();
		List<CyberTrackerPropertiesProfile> list = CyberTrackerHibernateManager.getPropertiesProfiles(session);
		session.close();
		//temp code end
		
		Dialog dialog = new CyberTrackerPropertiesDialog(shell, list.get(0));
		dialog.open();
	}

	public static class CyberTrackerPropertiesHandlerWrapper extends DIHandler<CyberTrackerPropertiesHandler>{
		public CyberTrackerPropertiesHandlerWrapper(){
			super (CyberTrackerPropertiesHandler.class);
		}
	}
}

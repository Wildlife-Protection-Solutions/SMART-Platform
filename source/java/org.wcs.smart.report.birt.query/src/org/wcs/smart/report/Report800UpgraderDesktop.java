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
package org.wcs.smart.report;

import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.common.control.WarningDialog;
import org.wcs.smart.data.oda.smart.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.upgrade.AbstractInteralDatabaseUpgrader;

/**
 * Script to upgrade report files from the "old" map report structure to the new
 * map report structure implemented in 4.0
 * 
 * @author Emily
 *
 */
public class Report800UpgraderDesktop extends AbstractInteralDatabaseUpgrader {
	
	@Override
	public void upgrade(IProgressMonitor monitor) throws Exception {
		try (Session session = HibernateManager.openSession()){
			session.beginTransaction();
			
			List<String> warnings = (new Report800Upgrader()).upgrade(session);
			if (warnings.size() > 0){
				Display.getDefault().syncExec(new Runnable(){
					@Override
					public void run() {
						WarningDialog wd = new WarningDialog(Display.getDefault().getActiveShell(), Messages.Report400Upgrader_ErrorTitle, Messages.Report400Upgrader_ErrorMessage, warnings);
						wd.open();
					}});
			}
			
			session.getTransaction().commit();
		}
	}

}

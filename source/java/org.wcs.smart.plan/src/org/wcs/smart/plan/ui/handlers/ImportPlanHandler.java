/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.plan.ui.handlers;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.xml.PlanFromXml;

/**
 * Handler for importing plans from xml files.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@SuppressWarnings("restriction")
public class ImportPlanHandler {
	
	
	@Execute
	public void execute(Shell activeShell){
		
		FileDialog fd = new FileDialog(activeShell, SWT.OPEN);
		fd.setFilterNames(new String[] {Messages.ImportPlanHandler_XmlFiles, Messages.ImportPlanHandler_AllFiles});
		fd.setFilterExtensions(new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		
		String filename = fd.open();
		if (filename == null) return;
		
		Path xmlFile = Paths.get(filename);
		
		PlanFromXml importer = new PlanFromXml();
		try{
			if (importer.convertPlan(xmlFile)) {
				try(Session session = HibernateManager.openSession()){
					session.beginTransaction();
					try {
						importer.doSave(session);
						session.getTransaction().commit();
					}catch (Exception ex) {
						session.getTransaction().rollback();
						throw ex;
					}
				}
				importer.fireEvents();
			}
		}catch (Exception ex) {
			SmartPlanPlugIn.displayLog(MessageFormat.format(Messages.ImportPlanHandler_ImportError,xmlFile.toString(), ex.getMessage()), ex);
		}
	}

	
	public static class ImportPlanHandlerWrapper extends DIHandler<ImportPlanHandler>{
		public ImportPlanHandlerWrapper(){
			super(ImportPlanHandler.class);
		}
	}
}

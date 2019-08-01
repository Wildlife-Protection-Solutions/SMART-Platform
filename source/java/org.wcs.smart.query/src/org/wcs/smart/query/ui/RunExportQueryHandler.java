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
package org.wcs.smart.query.ui;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.importexport.RunExportWizard;
import org.wcs.smart.ui.SmartWizardDialog;

/**
 * Handler for the run query command that run the query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@SuppressWarnings("restriction")
public class RunExportQueryHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, IEclipseContext context) {
		List<QueryEditorInput> toexport = new ArrayList<>();
		
		if (thisSelection instanceof QueryEditorInput) toexport.add((QueryEditorInput)thisSelection);
		if (thisSelection instanceof Query) toexport.add(new QueryEditorInput((Query)thisSelection));
		
		if (thisSelection instanceof IStructuredSelection) {
			for (Iterator<?> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
				Object o = (Object)iterator.next();
				if (o instanceof QueryEditorInput) toexport.add((QueryEditorInput)o);
				if (o instanceof Query) toexport.add(new QueryEditorInput((Query)o));
			}
		}
		
		if (toexport.isEmpty()) return;
		
		List<Query> items = new ArrayList<>();
		try(Session session = HibernateManager.openSession()){
			for (QueryEditorInput i : toexport) {
				Query q = QueryHibernateManager.getInstance().findQuery(session, i.getUuid(), i.getType());
				if (q != null) items.add(q);
			}
		}

		WizardDialog wizard = new SmartWizardDialog(context.get(Shell.class), new RunExportWizard(items));
		wizard.open();
		
	}
	
	public static class RunExportQueryHandlerWrapper extends DIHandler<RunExportQueryHandler>{
		public RunExportQueryHandlerWrapper(){
			super(RunExportQueryHandler.class);
		}
	}

}

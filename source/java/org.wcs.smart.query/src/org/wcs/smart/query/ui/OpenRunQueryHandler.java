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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.filter.date.IDateFieldFilter;
import org.wcs.smart.query.ui.editor.QueryEditorInput;
import org.wcs.smart.query.ui.querylist.OpenQueryHandler;

/**
 * Handler for the run query command that run the query.
 * 
 * @author Emily
 * @since 1.0.0
 */
@SuppressWarnings("restriction")
public class OpenRunQueryHandler {

	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection, EPartService pService, IEclipseContext context) {
		
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
		
		List<IDateFieldFilter> fields = new ArrayList<>();
		for (IDateFieldFilter i : toexport.get(0).getType().getDateFilterOptions()) {
			fields.add(i);
		}
		for (QueryEditorInput q : toexport) {
			Set<IDateFieldFilter> test = new HashSet<>();
			for (IDateFieldFilter i : q.getType().getDateFilterOptions()) test.add(i);
				
			for (Iterator<IDateFieldFilter> iterator = fields.iterator(); iterator.hasNext();) {
				IDateFieldFilter iDateFieldFilter = iterator.next();
				if (!test.contains(iDateFieldFilter)) iterator.remove();
			}
			if (fields.isEmpty()) break;
		}
		if (fields.isEmpty()) {
			MessageDialog.openError(context.get(Shell.class), Messages.OpenRunQueryHandler_MsgTitle, Messages.OpenRunQueryHandler_Msg);
			return;
		}
		
		RunDateDialog d = new RunDateDialog(context.get(Shell.class), fields);
		if (d.open() != Window.OK) return;
		for (QueryEditorInput q : toexport) {
			QueryEditorInput copy = new QueryEditorInput(q.getUuid(), q.getName(), q.getId(), q.isShared(), q.getType());
			copy.setDateFilter(d.getDateFilter());
			IEditorPart p = (new OpenQueryHandler()).execute(new StructuredSelection(copy));
			if (p == null) return;
			(new RunQueryHandler()).execute(pService);
		}
		
	}
	
	public static class OpenRunQueryHandlerWrapper extends DIHandler<OpenRunQueryHandler>{
		public OpenRunQueryHandlerWrapper(){
			super(OpenRunQueryHandler.class);
		}
	}

}

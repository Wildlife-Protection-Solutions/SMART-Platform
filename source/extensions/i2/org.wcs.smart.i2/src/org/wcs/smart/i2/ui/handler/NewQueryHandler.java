/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntityRecordQuery;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.editors.query.QueryEditorInput;
import org.wcs.smart.ui.ShowPerspectiveHandler;

import com.ibm.icu.text.MessageFormat;

/**
 * Open dialog handler
 * 
 * @author Emily
 *
 */
public class NewQueryHandler {
	
	public static final String QUERY_TYPE_KEY = "org.wcs.smart.i2.query.new.type"; //$NON-NLS-1$
	
	@Execute
	public void createNewRecord(IEclipseContext context){
		//open perspective
		IEclipseContext kid = context.createChild();
		kid.set( org.wcs.smart.ui.ShowPerspectiveHandler.PERSPECTIVE_ID_PARAM, IntelDataAnalysisPerspective.ID);
		ContextInjectionFactory.invoke(new ShowPerspectiveHandler(), Execute.class, kid);

		String typeKey = (String) context.get(QUERY_TYPE_KEY);
		if (typeKey == null) {
			MessageDialog.openError(context.get(Shell.class), Messages.NewQueryHandler_ErrorTitle, Messages.NewQueryHandler_NoQueryType);
			return;
		}
		typeKey = typeKey.toUpperCase();
		if (!typeKey.equals(IntelRecordObservationQuery.KEY) && !typeKey.equals(IntelEntitySummaryQuery.KEY)
				&& !typeKey.equals(IntelEntityRecordQuery.KEY)) {
			MessageDialog.openError(context.get(Shell.class), Messages.NewQueryHandler_ErrorTitle, MessageFormat.format(Messages.NewQueryHandler_InvalidQueryType, typeKey));
			return;
		}
		//open editor
		QueryEditorInput input = new QueryEditorInput(Messages.NewQueryHandler_DefaultQueryName, null, typeKey);
		(new OpenQueryHandler()).openQuery(input, true);
	}
	
	public static class NewQueryHandlerWrapper extends AbstractHandler {

		private NewQueryHandler component;

		public NewQueryHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(NewQueryHandler.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(QUERY_TYPE_KEY, event.getParameter(QUERY_TYPE_KEY));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}

	}
	
}

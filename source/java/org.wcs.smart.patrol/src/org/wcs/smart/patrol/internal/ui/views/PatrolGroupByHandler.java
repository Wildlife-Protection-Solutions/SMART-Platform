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
package org.wcs.smart.patrol.internal.ui.views;

import javax.inject.Named;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

public class PatrolGroupByHandler  {

	public static final String GROUPBY_OP_PARAM = "org.eclipse.ui.commands.radioStateParameter"; //$NON-NLS-1$
	
	@Execute
	public void execute(@Optional @Named(GROUPBY_OP_PARAM) String groupByOption, IEventBroker broker){
		if (groupByOption == null) return;
		broker.send(PatrolListView.GROUP_BY_EVENT, groupByOption);
	}

	//E3
	public static class PatrolGroupByHandlerWrapper extends AbstractHandler {

		private PatrolGroupByHandler component;

		public PatrolGroupByHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(PatrolGroupByHandler.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			if (HandlerUtil.matchesRadioState(event)) return null;
			 HandlerUtil.updateRadioState(event.getCommand(),  event.getParameter(GROUPBY_OP_PARAM));
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(GROUPBY_OP_PARAM, event.getParameter(GROUPBY_OP_PARAM));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}
	}
}


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
package org.wcs.smart.ui;

import javax.inject.Named;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.WorkbenchException;
import org.wcs.smart.SmartPlugIn;

/**
 * Show perspective handler with a single parameter identifying the perspective
 * to show.
 * @author Emily
 *
 */
public class ShowPerspectiveHandler {

	public static final String PERSPECTIVE_ID_PARAM = "org.wcs.smart.perspectiveid"; //$NON-NLS-1$
	@Execute
	public void execute(@Optional @Named(PERSPECTIVE_ID_PARAM) String perspectiveId, MWindow window){
		if (perspectiveId == null) return;
		
		EModelService mService = window.getContext().get(EModelService.class);
		String activeId = mService.getActivePerspective(window).getElementId();		
		if (!activeId.equals(perspectiveId)){
			try {
				PlatformUI.getWorkbench().showPerspective(perspectiveId, PlatformUI.getWorkbench().getActiveWorkbenchWindow());
			} catch (WorkbenchException e) {
				SmartPlugIn.displayLog(e.getMessage(), e);
			}
		}
	}
	
	public static class ShowPerspectiveHandlerWrapper extends AbstractHandler {

		private ShowPerspectiveHandler component;

		public ShowPerspectiveHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(ShowPerspectiveHandler.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(PERSPECTIVE_ID_PARAM, event.getParameter(PERSPECTIVE_ID_PARAM));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}

	}
}

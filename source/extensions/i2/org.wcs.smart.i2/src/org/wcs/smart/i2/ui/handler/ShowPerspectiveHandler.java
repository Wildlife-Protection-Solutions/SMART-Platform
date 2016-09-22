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
package org.wcs.smart.i2.ui.handler;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.ui.editors.IntelligenceMapEditor;

/**
 * Show perspective handler with a single parameter identifying the perspective
 * to show.
 * @author Emily
 *
 */
public class ShowPerspectiveHandler {

	@Execute
	public void execute(IEclipseContext context){
		ContextInjectionFactory.invoke(new org.wcs.smart.ui.ShowPerspectiveHandler(), Execute.class, context);
		
		
		try {
			//ensure map editor is open
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(IntelligenceMapEditor.MAPINPUT, IntelligenceMapEditor.ID);
		} catch (PartInitException e) {
			e.printStackTrace();
		}
		
	}
	
	public static class ShowPerspectiveHandlerWrapper extends AbstractHandler{
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
			ctx.set(org.wcs.smart.ui.ShowPerspectiveHandler.PERSPECTIVE_ID_PARAM, event.getParameter(org.wcs.smart.ui.ShowPerspectiveHandler.PERSPECTIVE_ID_PARAM));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}
	}
}

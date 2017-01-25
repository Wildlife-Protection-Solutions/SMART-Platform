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

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.wcs.smart.i2.ui.IntelDataAnalysisPerspective;
import org.wcs.smart.i2.ui.editors.query.QueryEditorInput;

/**
 * Open dialog handler
 * 
 * @author Emily
 *
 */
public class NewQueryHandler {
	
	@Execute
	public void createNewRecord(IEclipseContext context){
		//open perspective
		IEclipseContext kid = context.createChild();
		kid.set( org.wcs.smart.ui.ShowPerspectiveHandler.PERSPECTIVE_ID_PARAM, IntelDataAnalysisPerspective.ID);
		ContextInjectionFactory.invoke(new ShowPerspectiveHandler(), Execute.class, kid);

		//open editor
		QueryEditorInput input = new QueryEditorInput("New Query", null);
		(new OpenQueryHandler()).openQuery(input, true);
	}
	
	// E3
	public static class NewQueryHandlerWrapper extends DIHandler<NewQueryHandler> {
		public NewQueryHandlerWrapper() {
			super(NewQueryHandler.class);
		}
	}
	
}

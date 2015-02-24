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
package org.wcs.smart.intelligence.ui.handlers;

import javax.inject.Named;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.intelligence.report.ReportIntelligence;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditor;
import org.wcs.smart.intelligence.ui.editor.IntelligenceEditorInput;
import org.wcs.smart.util.E3Utils;

/**
 * Handler for exporting intelligence to pdf files.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public class ExportIntelligencePdf {

	private final static String ISEDITOR_PARAM = "org.wcs.smart.intelligence.exportpdf.editor"; //$NON-NLS-1$
	
	@Execute
	public void execute(MPart activePart, 
			@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection,
			@Optional @Named(ISEDITOR_PARAM) String isEditor){
		
		IntelligenceEditorInput in = null;
		
		if (isEditor != null && isEditor.equalsIgnoreCase("true")){ //$NON-NLS-1$
			Object x = E3Utils.getSourceObject(activePart);
			if (x instanceof IntelligenceEditor){
				in = (IntelligenceEditorInput) ((IntelligenceEditor)x).getEditorInput();
			}
		}else{
			if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((IStructuredSelection)thisSelection).isEmpty()) return;
			final IStructuredSelection lastSelection = (IStructuredSelection) thisSelection;
			if (lastSelection.getFirstElement() instanceof IntelligenceEditorInput){
				in = (IntelligenceEditorInput) lastSelection.getFirstElement();	
			}			
		}
		if (in != null){
			ReportIntelligence.export(in.getUuid());
		}
	}

	public static class ExportIntelligencePdffWrapper extends AbstractHandler {

		private ExportIntelligencePdf component;

		public ExportIntelligencePdffWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(ExportIntelligencePdf.class, context);
		}

		private static IEclipseContext getActiveContext() {
			IEclipseContext parentContext = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			return parentContext.getActiveLeaf();
		}
		
		@Override
		public Object execute(ExecutionEvent event) throws ExecutionException {
			//Default di handler does not add parameters into context
			IEclipseContext ctx = getActiveContext();
			ctx.set(ISEDITOR_PARAM, event.getParameter(ISEDITOR_PARAM));
			return ContextInjectionFactory.invoke(component, Execute.class, ctx);
		}

	}
}

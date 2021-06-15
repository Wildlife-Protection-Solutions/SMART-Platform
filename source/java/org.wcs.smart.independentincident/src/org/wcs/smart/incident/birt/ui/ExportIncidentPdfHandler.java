/*
 * Copyright (C) 2021 Wildlife Conservation Society
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
package org.wcs.smart.incident.birt.ui;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;

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
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.incident.birt.IncidentBirtManager;
import org.wcs.smart.incident.ui.IncidentEditorInput;
import org.wcs.smart.util.E3Utils;

/**
 * Handler for exporting incidents to pdf files.
 * 
 * @author Emily
 *
 */
public class ExportIncidentPdfHandler {

	private final static String ISEDITOR_PARAM = "org.wcs.smart.incident.exportpdf.editor"; //$NON-NLS-1$
	
	@SuppressWarnings("unchecked")
	@Execute
	public void execute(Shell activeShell, EPartService pService, 
			@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection,
			@Optional @Named(ISEDITOR_PARAM) String isEditor){
		
		
		
		if (isEditor != null && isEditor.equalsIgnoreCase("true")){ //$NON-NLS-1$
			UUID uuid = null;
			for (MPart p : pService.getParts()){
				if (E3Utils.isCompatibilityEditor(p) &&
						E3Utils.getSourceObject(p) instanceof IEditorPart &&
						pService.isPartVisible(p)){
					
					IEditorPart ie = (IEditorPart)E3Utils.getSourceObject(p);
					if (ie.getEditorInput() instanceof IncidentEditorInput) {
						uuid = ((IncidentEditorInput) ie.getEditorInput()  ).getUuid();
					}
					break;
				}
			}
			if (uuid == null) return;
			IncidentBirtManager.INSTANCE.exportToPdf(uuid);
		}else{
			Set<UUID> in = new HashSet<>();
			if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((IStructuredSelection)thisSelection).isEmpty()) return;
			for (Iterator<Object> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
				Object item = (Object) iterator.next();
				if (item instanceof IncidentEditorInput){
					in.add( ((IncidentEditorInput) item).getUuid() );	
				}	
			}
			if (in.isEmpty()) return;
			
			DirectoryDialog dir = new DirectoryDialog( activeShell );
			String loc = dir.open();
			if (loc == null) return;
			
			IncidentBirtManager.INSTANCE.exportToPdf(in, Paths.get(loc));				
		}
		
		
	}

	public static class ExportIncidentPdfHandlerWrapper extends AbstractHandler {

		private ExportIncidentPdfHandler component;

		public ExportIncidentPdfHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(ExportIncidentPdfHandler.class, context);
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

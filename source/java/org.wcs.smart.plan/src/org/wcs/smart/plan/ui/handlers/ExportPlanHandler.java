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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.locationtech.udig.catalog.URLUtils;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.plan.SmartPlanPlugIn;
import org.wcs.smart.plan.internal.Messages;
import org.wcs.smart.plan.model.Plan;
import org.wcs.smart.plan.ui.editor.PlanEditor;
import org.wcs.smart.plan.ui.editor.PlanEditorInput;
import org.wcs.smart.plan.xml.PlanToXml;
import org.wcs.smart.util.E3Utils;
import org.wcs.smart.util.SmartUtils;

/**
 * Handler for exporting plans to xml files.
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
public class ExportPlanHandler {
	
	private final static String ISEDITOR_PARAM = "org.wcs.smart.plan.export.editor"; //$NON-NLS-1$
	
	@SuppressWarnings("unchecked")
	@Execute
	public void execute(EPartService pService, 
			Shell activeShell,
			@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection,
			@Optional @Named(ISEDITOR_PARAM) String isEditor){
		
		List<PlanEditorInput> in = new ArrayList<>();
		
		if (isEditor != null && isEditor.equalsIgnoreCase("true")){ //$NON-NLS-1$
			for (MPart p : pService.getParts()){
				if (E3Utils.isCompatibilityEditor(p) &&
						E3Utils.getSourceObject(p) instanceof PlanEditor &&
						pService.isPartVisible(p)){
					in.add ( (PlanEditorInput) ((PlanEditor)E3Utils.getSourceObject(p)).getEditorInput() );
					break;
				}
			}
		}else{
			if (thisSelection == null || !(thisSelection instanceof IStructuredSelection) || ((IStructuredSelection)thisSelection).isEmpty()) return;
			for (Iterator<Object> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
				Object item = (Object) iterator.next();
				if (item instanceof PlanEditorInput){
					in.add( (PlanEditorInput)item );	
				}	
			}
						
		}
		
		if (in.isEmpty()) return;
		
		if (in.size() == 1) {
			exportSingle(in.get(0), activeShell);
		}	else {
			exportMulti(in, activeShell);
		}
	}

	private void exportMulti(List<PlanEditorInput> toexport, Shell activeShell) {
		DirectoryDialog fd = new DirectoryDialog(activeShell, SWT.OPEN);
		String sdir = fd.open();
		if (sdir == null) return;
		
		Path dir = Paths.get(sdir);
		SmartUtils.createDirectory(dir);
	
		int cnt = 0;
		try(Session session = HibernateManager.openSession()){
			for (PlanEditorInput i : toexport) {
				Plan p = session.get(Plan.class, i.getUuid());
				
				try {
					Path xmlFile = dir.resolve(getFilename(p));
					PlanToXml xx = new PlanToXml();
					xx.convertPlan(p, xmlFile);
					cnt++;
				}catch (Exception ex) {
					SmartPlanPlugIn.displayLog(MessageFormat.format(Messages.ExportPlanHandler_ErrorMsg2, p.getName() + " [" + p.getId() + "]", ex.getMessage()),ex); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		
		MessageDialog.openInformation(activeShell, Messages.ExportPlanHandler_OkTitle,
				MessageFormat.format(Messages.ExportPlanHandler_OkMessage, cnt, toexport.size(), dir.toString()));
	}
	
	
	private void exportSingle(PlanEditorInput toexport, Shell activeShell) {
		Plan p = null;
		try(Session session = HibernateManager.openSession()){
			p = session.get(Plan.class, toexport.getUuid());
			if (p != null) {	
				p.getId();
			}
		}
		if (p == null) {
			SmartPlanPlugIn.displayLog(Messages.ExportPlanHandler_PlanNotFound, null);
			return;
		}
		
		FileDialog fd = new FileDialog(activeShell, SWT.SAVE);
		fd.setFilterNames(new String[] {Messages.ExportPlanHandler_XmlFiles, Messages.ExportPlanHandler_AllFiles});
		fd.setFilterExtensions(new String[] {"*.xml", "*.*"}); //$NON-NLS-1$ //$NON-NLS-2$
		fd.setFileName(getFilename(p));
		
		String filename = fd.open();
		if (filename == null) return;
		
		Path xmlFile = Paths.get(filename);
		if (Files.exists(xmlFile)) {
			if (!MessageDialog.openQuestion(activeShell, Messages.ExportPlanHandler_OverwriteTitle, 
					MessageFormat.format(Messages.ExportPlanHandler_OverwriteMessage, xmlFile.toString()))) {
				return;
			}
			
		}
		SmartUtils.createDirectory(xmlFile.getParent());
		
		
		try(Session session = HibernateManager.openSession()){
			p = session.get(Plan.class, toexport.getUuid());
			if (p == null) {
					SmartPlanPlugIn.displayLog(Messages.ExportPlanHandler_PlanNotFound, null);
			}else {
				PlanToXml xx = new PlanToXml();
				xx.convertPlan(p, xmlFile);
			}
			
		}catch (Exception ex) {
			SmartPlanPlugIn.displayLog(Messages.ExportPlanHandler_ExportError + p.getId(), ex);
			return;
		}
		MessageDialog.openInformation(activeShell, Messages.ExportPlanHandler_OkTitle, MessageFormat.format(Messages.ExportPlanHandler_SingleOkMsg, p.getId(), xmlFile.toString()) );
	}
	
	private String getFilename(Plan p) {
		StringBuilder sb = new StringBuilder();
		if (!p.getName().isEmpty()) sb.append(p.getName() + "_"); //$NON-NLS-1$
		sb.append(p.getId());
		String fname = URLUtils.cleanFilename(sb.toString()) + ".plan.xml"; //$NON-NLS-1$
		return fname;
	}
	
	public static class ExportPlanHandlerWrapper extends AbstractHandler {

		private ExportPlanHandler component;

		public ExportPlanHandlerWrapper() {
			IEclipseContext context = getActiveContext();
			component = ContextInjectionFactory.make(ExportPlanHandler.class, context);
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

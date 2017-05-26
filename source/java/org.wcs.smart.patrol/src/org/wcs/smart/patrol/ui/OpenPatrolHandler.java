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
package org.wcs.smart.patrol.ui;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.observation.ObservationHibernateManager;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;
import org.wcs.smart.patrol.PatrolManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.internal.ui.views.PatrolListView;
import org.wcs.smart.patrol.model.Patrol;

/**
 * Simple POJO handler for opening patrol editor.
 * @author Emily
 *
 */
public class OpenPatrolHandler {

	public static final String PATROL_PARAM = "patrolinput"; //$NON-NLS-1$
	public static final String INIT_SELECTION_WP_UUID = "waypointuuid"; //$NON-NLS-1$
	
	@Execute
	public void openPatrol(@Optional @Named(PATROL_PARAM) PatrolEditorInput patrolInput,
			@Optional @Named(INIT_SELECTION_WP_UUID) UUID initSelection,
			MWindow activeWindow){
		List<PatrolEditorInput> patrols = new ArrayList<>();
		if (patrolInput != null){
			patrols.add(patrolInput);
		}else{
			Object selobj = activeWindow.getContext().get(ESelectionService.class).getSelection();
			if (!(selobj instanceof IStructuredSelection)) return;
			for (Iterator<?> iterator = ((IStructuredSelection)selobj).iterator(); iterator.hasNext();) {
				Object sel = (Object) iterator.next();
				if (sel instanceof PatrolEditorInput){
					patrols.add((PatrolEditorInput)sel);
				}else if (sel instanceof Patrol){
					patrols.add(new PatrolEditorInput((Patrol)sel));
				}	
			}
		}
		if (patrols.isEmpty()) return;
		
		(new ShowFieldDataPerspective()).execute(PatrolListView.ID,activeWindow);
		
		try {
			for(PatrolEditorInput pi: patrols){
				//validate patrol
				
				Session s = HibernateManager.openSession();
				Patrol patrol = null;
				boolean canEdit = false;
				try{
					patrol = (Patrol)s.get(Patrol.class, pi.getUuid());
					if (patrol == null) continue; //patrol not found so we cannot open it
					canEdit = null == PatrolManager.getInstance().canEdit(patrol, ObservationHibernateManager.getPatrolOptions(SmartDB.getCurrentConservationArea(), s));	
				}finally{
					s.close();
				}
				
				long patrolLengthMills = pi.getEndDate().getTime() - pi.getStartDate().getTime();
				long patrolLengthDays = (long)Math.ceil(patrolLengthMills / (24 * 60 * 60 * 1000l));
				if (patrolLengthDays > Patrol.MAX_PATROL_LENGTH_DAYS){
					//warning with an option to edit dates
					String[] buttons = new String[]{Messages.OpenPatrolHandler_CancelBtn, Messages.OpenPatrolHandler_ProceedBtn};
					String message = MessageFormat.format(Messages.OpenPatrolHandler_TooLongProceed, pi.getPatrolId(), patrolLengthDays, Patrol.MAX_PATROL_LENGTH_DAYS);
					if (canEdit){
						buttons = new String[]{Messages.OpenPatrolHandler_CancelBtn, Messages.OpenPatrolHandler_ProceedBtn, Messages.OpenPatrolHandler_EditButtons};
						message = MessageFormat.format(Messages.OpenPatrolHandler_TooLongError, pi.getPatrolId(), patrolLengthDays, Patrol.MAX_PATROL_LENGTH_DAYS);
					}
					Shell activeShell = activeWindow.getContext().get(Shell.class);
					MessageDialog md = new MessageDialog(activeShell,
							Messages.OpenPatrolHandler_DialogTitle, null, message, MessageDialog.WARNING,
							buttons, buttons.length - 1);
					int ret = md.open();
					if (ret == 0){
						//cancel
						return;
					}
					if (ret == 2){
						//show edit dates dialog
						EditPatrolDatesDialog pdd = new EditPatrolDatesDialog(activeShell, pi);
						if (pdd.open() == Window.CANCEL) return;
					}
				}
				
				
				IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
				IWorkbenchPage page = window.getActivePage();
				IEditorPart part = page.openEditor(pi, PatrolEditor.ID);
				if (part instanceof PatrolEditor && initSelection != null){
					((PatrolEditor)part).findAndShow(initSelection);
				}
			}
		} catch (PartInitException e) {
			SmartPatrolPlugIn.displayLog(Messages.OpenPatrolHandler_OpenPatrolError + e.getLocalizedMessage(), e);
		}
	}
	
	public void openPatrol(@Optional @Named(PATROL_PARAM) PatrolEditorInput patrolInput,
			MWindow activeWindow){
		openPatrol(patrolInput, null, activeWindow);
	}
	
	
	//E3
	public static class OpenPatrolHandlerWrapper extends DIHandler<OpenPatrolHandler>{
		public OpenPatrolHandlerWrapper(){
			super(OpenPatrolHandler.class);
		}
	}
}

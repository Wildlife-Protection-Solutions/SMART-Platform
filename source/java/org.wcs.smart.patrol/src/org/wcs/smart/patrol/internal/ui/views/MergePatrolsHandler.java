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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.workbench.modeling.ESelectionService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.model.Patrol;
import org.wcs.smart.patrol.ui.PatrolEditorInput;


/**
 * Merge patrol handler
 * @author Jeff
 *
 */
public class MergePatrolsHandler {
	
	@Execute
	public void execute(ESelectionService selectionService, final Shell activeShell){
		
		Object selobj = selectionService.getSelection();
		if (!(selobj instanceof IStructuredSelection)) return;
		
		final IStructuredSelection lastSelection = (IStructuredSelection) selobj;
		if (lastSelection.size() == 0){
			return;	//nothing to merge
		}
		
		final List<PatrolEditorInput> toMerge = new ArrayList<PatrolEditorInput>();
		for (Iterator<?> iterator = lastSelection.iterator(); iterator.hasNext();) {
			Object x = iterator.next();
			if (x instanceof PatrolEditorInput){
				toMerge.add((PatrolEditorInput)x);
			}
		}
		
		if (toMerge.size() == 1){
			return;//Shouldn't be able to run this from the UI when only 1 patrol is selected.
		}else if (toMerge.size() > 1){
			StringBuilder sb = new StringBuilder();
			for (PatrolEditorInput in : toMerge){
				sb.append(in.getPatrolId());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			if (sb.length() > 1000){
				sb.delete(1000, sb.length()-1);
				sb.append(" ..."); //$NON-NLS-1$
			} 
		}else{
			return;
		}
		ArrayList<Patrol> patrols = new ArrayList<Patrol>();
		
		Session session = HibernateManager.openSession();

		try {
		
			session.beginTransaction();
			for(PatrolEditorInput pei : toMerge){
				patrols.add((Patrol)session.load(Patrol.class, pei.getUuid()));
			}
			session.getTransaction().commit();
			
			MergePatrolsDialog mergePatrolDialog = new MergePatrolsDialog(activeShell, patrols, session);
			if (mergePatrolDialog.open() != Window.OK){
				return;
			}
		}catch (Exception ex){
			session.getTransaction().rollback();
			SmartPatrolPlugIn.displayLog("There was a problem loading the Patrols, merge failed." + "\n\n" + ex.getLocalizedMessage(),ex); //$NON-NLS-1$ //$NON-NLS-2$
		}finally{
			
		}
		
		return;
	}

	//E3
	public static class MergePatrolsHandlerWrapper extends DIHandler<MergePatrolsHandler>{
		public MergePatrolsHandlerWrapper(){
			super(MergePatrolsHandler.class);
		}
	}
}

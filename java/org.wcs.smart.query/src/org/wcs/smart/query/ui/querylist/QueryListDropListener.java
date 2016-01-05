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
package org.wcs.smart.query.ui.querylist;

import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Drop handler for moving queries betweeen folders
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryListDropListener extends ViewerDropAdapter {

	
	/**
	 * @param viewer
	 */
	protected QueryListDropListener(TreeViewer viewer) {
		super(viewer);
	}
	
	@Override
	 public void dragOver(DropTargetEvent event) {
		super.dragOver(event);
		event.feedback = DND.FEEDBACK_SELECT | DND.FEEDBACK_SCROLL  | DND.FEEDBACK_EXPAND;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@Override
	public boolean performDrop(Object data) {
		
		StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null){
			return false;
		}
		
		Object x = getCurrentTarget();
		if (!(x instanceof QueryFolder)) return false;
		
		final QueryFolder targetFolder = (QueryFolder) x;
		
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object select = (Object) iterator.next();
			if (select instanceof QueryEditorInput){
				final QueryEditorInput query = (QueryEditorInput) select;
				//want to close any editors associated with given input
				final IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findEditor(query);
				if (editor != null){
					if (!PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeEditor(editor, true)){
						continue;
					}
				}
				//run job to update query folder
				Job internalUpdate = new Job("Update Query Folder"){ //$NON-NLS-1$

					@Override
					protected IStatus run(IProgressMonitor monitor) {
						Session s = HibernateManager.openSession();
						Query q = null;
						boolean isChanged = false;
						try{
							s.beginTransaction();
							q = (Query) s.load(query.getType().getHibernateClass(), query.getUuid());
							if (!((targetFolder == null && q.getFolder() == null) ||
									(targetFolder != null && targetFolder.equals(q.getFolder())))){
								isChanged = true;
								//folder was changed; update it
								q.setIsShared(targetFolder.getEmployee() == null);
								if (targetFolder.isRootFolder()){	
									q.setFolder(null);
								}else{
									q.setFolder(targetFolder);
								}
							}
							s.getTransaction().commit();
						}catch (Exception ex){
							if (s.getTransaction().isActive()) s.getTransaction().rollback();
							QueryPlugIn.log(ex.getMessage(), ex);
							return Status.OK_STATUS;
						}finally{
							s.close();
						}
						// fire changed event if folder was changed
						if (isChanged){
							final Query lq = q;
							Display.getDefault().syncExec(new Runnable(){
								@Override
								public void run() {
									QueryEventManager.getInstance().fireQuerySaved(lq);
									if (editor != null){
										//reopen the editor
										try {
											PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(editor.getEditorInput(), editor.getSite().getId());
										} catch (PartInitException e) {
											QueryPlugIn.log(e.getMessage(), e);
										}
									}
								}
							});
						}
						return Status.OK_STATUS;
					}
					
				};
				internalUpdate.setSystem(true);
				internalUpdate.schedule();
			}
		}
		
		return true;
	}

	/**
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation,
			TransferData transferType) {

		StructuredSelection selection = (StructuredSelection)LocalSelectionTransfer.getTransfer().getSelection();
		if (selection == null){
			return false;
		}
		
		if (!(target instanceof QueryFolder)) return false;
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object type = (Object) iterator.next();
			//at least one item is a query editor input so we can move it
			if (type instanceof QueryEditorInput) return true;
		}
		return false;
	}

}

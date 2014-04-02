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

import java.text.MessageFormat;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Command handler for deleting a folder
 * or query from the query list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class DeleteItemHandler extends AbstractHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		ISelection thisSelection = HandlerUtil.getCurrentSelection(event);
		if (thisSelection == null || thisSelection.isEmpty() 
				|| !(thisSelection instanceof IStructuredSelection) ){
			return null;
		}
		
		//iterator in reverse order to try to remove leaf items before parent items
		@SuppressWarnings("unchecked")
		final List<Object> selection= ((IStructuredSelection)thisSelection).toList();
		Collections.reverse(selection);
		
		
		if (selection.size() == 1 && (selection.get(0) instanceof QueryEditorInput) ){
			QueryEditorInput query = ((QueryEditorInput)selection.get(0)) ;
			if (!MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), 
					Messages.DeleteItemHandler_Confirm_DialogTitle, 
					MessageFormat.format(
							Messages.DeleteItemHandler_ConfirmMessage,  new Object[]{ query.getName() + " [" + query.getId() + "]"}))){ //$NON-NLS-1$ //$NON-NLS-2$
			
				return null;
			}
		}else{
	
			String message = Messages.DeleteItemHandler_ConfirmDelete;
			message = MessageFormat.format(message, new Object[]{selection.size()});
			if (!MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), Messages.DeleteItemHandler_ConfirmDeleteTitle, message )){
				return null;
			}	
			
		}
		
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object o = (Object) iterator.next();			
			if (o instanceof QueryFolder){
				QueryListContentProvider contentProvider = ((QueryListView)HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(QueryListView.ID)).getQueryListContentProvider();
				deleteFolder((QueryFolder)o, contentProvider);
			}else if (o instanceof Query){
				deleteQuery((Query)o, event);
			}else if (o instanceof QueryEditorInput){
				deleteQuery((QueryEditorInput)o, event);
			}	
		}
		return null;
	}

	private void deleteQuery(Query o, ExecutionEvent event) {
		deleteQuery(new QueryEditorInput(o), event);
	}
	
	
	private void deleteQuery(QueryEditorInput o, ExecutionEvent event) {
		/**
		 * Get the current window here for linux bug:
		 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=242246
		 */
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
		
		//need to save and refresh query list view
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			Query query = QueryHibernateManager.getInstance().findQuery(s, o.getUuid(), o.getType());
			if (query == null) throw new Exception(Messages.DeleteItemHandler_ErrorQueryNotFound);
			
			if (!QueryEventManager.getInstance().fireBeforeDelete(query, s)){
				return;
			}
			
			org.hibernate.Query q = s.createQuery("DELETE from " + o.getType().getHibernateClass().getSimpleName() + " WHERE uuid = :uuid"); //$NON-NLS-1$ //$NON-NLS-2$
			q.setParameter("uuid", o.getUuid()); //$NON-NLS-1$
			int deleted = q.executeUpdate();
			if (deleted != 1){
				QueryPlugIn.log(MessageFormat.format(Messages.DeleteItemHandler_ErrorDeletingQuery, new Object[]{ o.getName()}), null);
				s.getTransaction().rollback();
				return;
			}else{
				s.getTransaction().commit();
			}
		}catch (Exception ex){
			s.getTransaction().rollback();
			QueryPlugIn.log(MessageFormat.format(
				Messages.DeleteItemHandler_ErrorDeletingQueryB, new Object[]{ o.getName()}) + ex.getLocalizedMessage(), ex);
			return;
		}finally{
			s.close();
		}
		QueryEventManager.getInstance().fireQueryDeleted(o);
		
		// close the editor
		try {
			if (window != null) {
				final IWorkbenchPage page = window.getActivePage();
				if (page != null) {
					IEditorReference[] refs = page.getEditorReferences();
					for (int i = 0; i < refs.length; i++) {
						if (refs[i].getEditorInput().equals(o)) {
							page.closeEditor(refs[i].getEditor(false), false);
						}
					}
				}
			}
		} catch (Exception ex) {
			QueryPlugIn.displayLog(
					Messages.DeleteItemHandler_ErrorClosingEditor + ex.getLocalizedMessage(), ex);
		}
		
	}
	private void deleteFolder(QueryFolder folder, ITreeContentProvider provider) {
		
		if (folder.getChildren() != null && folder.getChildren().size() > 0){
			QueryPlugIn.displayLog(MessageFormat.format(Messages.DeleteItemHandler_CannotDeleteItemWithKids1, new Object[]{folder.getName()}), null);
			return;
		}
		if (provider.hasChildren(folder) ){
			QueryPlugIn.displayLog(MessageFormat.format(Messages.DeleteItemHandler_CannotDeleteItemWithKids1, new Object[]{folder.getName()}), null);
			return;
		}
			
		
		QueryFolder parent = folder.getParentFolder();
		//need to save and refresh query list view
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			if (parent == null){
				s.delete(folder);
			}else{
				s.update(folder);
				parent.getChildren().remove(folder);
				folder.setParentFolder(null);
			}
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			QueryPlugIn.log(
					MessageFormat.format(
							Messages.DeleteItemHandler_ErrorDeleteFolder, new Object[]{folder.getName()}), ex);
		}finally{
			s.close();
		}
		QueryEventManager.getInstance().fireFolderDeleted(folder);
	}

}

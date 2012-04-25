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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.handlers.HandlerUtil;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.IQueryFolderListener;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;

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
		
		Object o = ((IStructuredSelection)thisSelection).getFirstElement();
		
		QueryListViewContentProvider contentProvider = ((QueryListView)HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().findView(QueryListView.ID)).getQueryListContentProvider();
		
		
		if (o instanceof QueryFolder){
			deleteFolder((QueryFolder)o, contentProvider);
		}else if (o instanceof Query){
			deleteQuery((Query)o, event);
		}else if (o instanceof QueryInput){
			deleteQuery((QueryInput)o, event);
		}
		
		return null;
	}

	private void deleteQuery(Query o, ExecutionEvent event) {
		deleteQuery(new QueryInput(o), event);
	}
	
	
	private void deleteQuery(QueryInput o, ExecutionEvent event) {
		if (!MessageDialog.openConfirm(HandlerUtil.getActiveShell(event), "Delete Query", "Are you sure you want to delete the query '" + o.getName() + " [" + o.getId() + "]'?  This action cannot be undone.")){
			return;
		}
		//need to save and refresh query list view
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			org.hibernate.Query q = s.createQuery("DELETE from WaypointQuery where uuid = :uuid");
			q.setParameter("uuid", o.getUuid());
			q.executeUpdate();
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			QueryPlugIn.log("Could not delete query: '" + o.getName() + "'", ex);
		}finally{
			s.close();
		}
		QueryEventManager.getInstance().fireFolderChangedListeners(IQueryFolderListener.QUERY_DELETED, o);
		
		// close the editor
		try {
			IEditorReference[] refs = HandlerUtil
					.getActiveWorkbenchWindow(event).getActivePage()
					.getEditorReferences();
			for (int i = 0; i < refs.length; i++) {
				if (refs[i].getEditorInput().equals(o)) {
					HandlerUtil.getActiveWorkbenchWindow(event).getActivePage()
							.closeEditor(refs[i].getEditor(false), false);
				}
			}
		} catch (Exception ex) {
			QueryPlugIn.displayLog(
					"Could not close editor. " + ex.getMessage(), ex);
		}
		
	}
	private void deleteFolder(QueryFolder folder, ITreeContentProvider provider) {
		
		if (folder.getChildren() != null && folder.getChildren().size() > 0){
			QueryPlugIn.displayLog("Cannot delete a folder with children elements.  Please remove the children elements first.", null);
			return;
		}
		if (provider.hasChildren(folder) ){
			QueryPlugIn.displayLog("Cannot delete a folder with children elements.  Please remove the children elements first.", null);
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
			QueryPlugIn.log("Could not delete folder: '" + folder.getName() + "'", ex);
		}finally{
			s.close();
		}
		QueryEventManager.getInstance().fireFolderChangedListeners(IQueryFolderListener.FOLDER_DELETED, folder);
	}

}

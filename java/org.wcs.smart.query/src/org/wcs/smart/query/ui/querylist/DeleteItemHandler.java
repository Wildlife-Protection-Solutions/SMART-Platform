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
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
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
public class DeleteItemHandler {

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object activeSelection, Shell activeShell) {
		if (activeSelection == null || !(activeSelection instanceof IStructuredSelection)){
			return;
		}
		//iterator in reverse order to try to remove leaf items before parent items
		@SuppressWarnings("unchecked")
		final List<Object> selection= ((IStructuredSelection)activeSelection).toList();
		Collections.reverse(selection);
		
		
		if (selection.size() == 1 && (selection.get(0) instanceof QueryEditorInput) ){
			QueryEditorInput query = ((QueryEditorInput)selection.get(0)) ;
			if (!MessageDialog.openConfirm(activeShell, 
					Messages.DeleteItemHandler_Confirm_DialogTitle, 
					MessageFormat.format(
							Messages.DeleteItemHandler_ConfirmMessage,  new Object[]{ query.getName() + " [" + query.getId() + "]"}))){ //$NON-NLS-1$ //$NON-NLS-2$
			
				return;
			}
		}else{
	
			String message = Messages.DeleteItemHandler_ConfirmDelete;
			message = MessageFormat.format(message, new Object[]{selection.size()});
			if (!MessageDialog.openConfirm(activeShell, Messages.DeleteItemHandler_ConfirmDeleteTitle, message )){
				return ;
			}	
			
		}
		
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object o = (Object) iterator.next();			
			if (o instanceof QueryFolder){
				deleteFolder((QueryFolder)o);
			}else if (o instanceof Query){
				deleteQuery((Query)o, activeShell);
			}else if (o instanceof QueryEditorInput){
				deleteQuery((QueryEditorInput)o, activeShell);
			}	
		}
	}

	private void deleteQuery(Query o, Shell shell) {
		deleteQuery(new QueryEditorInput(o), shell);
	}
	
	
	private void deleteQuery(QueryEditorInput o, Shell activeShell) {	
		//need to save and refresh query list view
		Session s = HibernateManager.openSession();
		s.beginTransaction();
		try{
			Query query = QueryHibernateManager.getInstance().findQuery(s, o.getUuid(), o.getType());
			if (query == null) throw new Exception(Messages.DeleteItemHandler_ErrorQueryNotFound);
			
			if (!QueryEventManager.getInstance().fireBeforeDelete(query, s)){
				s.getTransaction().rollback();
				return;
			}
			s.delete(query);
			s.getTransaction().commit();
		}catch (Exception ex){
			s.getTransaction().rollback();
			QueryPlugIn.log(MessageFormat.format(
				Messages.DeleteItemHandler_ErrorDeletingQueryB, new Object[]{ o.getName()}) + ex.getLocalizedMessage(), ex);
			return;
		}finally{
			s.close();
		}
		QueryEventManager.getInstance().fireQueryDeleted(o);
	}
	
	private void deleteFolder(QueryFolder folder) {
		if (folder.isRootFolder()){
			//cannot delete root folders
			return;
		}
		if (folder.getChildren() != null && folder.getChildren().size() > 0){
			QueryPlugIn.displayLog(MessageFormat.format(Messages.DeleteItemHandler_CannotDeleteItemWithKids1, new Object[]{folder.getName()}), null);
			return;
		}
		
		List<?> items = SavedQueryTree.getInstance().getQueries().get(folder.getUuid());
		if (items != null && items.size() > 0){
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
	
	public static class DeleteItemHandlerWrapper extends DIHandler<DeleteItemHandler>{
		public DeleteItemHandlerWrapper(){
			super(DeleteItemHandler.class);
		}
	}

}

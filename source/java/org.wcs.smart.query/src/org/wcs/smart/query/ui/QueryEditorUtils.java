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
package org.wcs.smart.query.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorPart;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.IQueryHibernateManager;
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.editor.IQueryEditor;
import org.wcs.smart.query.ui.querylist.SaveQueryDialog;

/**
 * Query editor utils.  Includes options for saving queries.
 * @author egouge
 *
 */
public class QueryEditorUtils {

	
	private static final String SAVE_DIALOGTITLE = Messages.QueryEditorUtils_SaveDialotTitle;


	/**
	 * 
	 * @param editor must implement the IQueryEditor interface
	 * @param addNamePrefix
	 * @return
	 */
	public static Query doSave(final IEditorPart editor,  
			IProgressMonitor monitor){
		
		if (!(editor instanceof IQueryEditor)){
			throw new IllegalStateException("invalid editor"); //$NON-NLS-1$
		}
		Query query = ((IQueryEditor)editor).getQueryProxy().getQuery();
		Shell shell = editor.getSite().getShell();
		
		//validate if user can save the current query
		if (query.getIsShared() && !QueryHibernateManager.getInstance().canModifyCaQueries()){			
			boolean ret = MessageDialog.openQuestion(shell, SAVE_DIALOGTITLE, Messages.QueryEditorUtils_PermissionError);
			if (ret){
				return doSaveAs(editor,true).getQuery();
			}
			return null;
		}
				
		//ensure query is valid
		if (!((IQueryEditor)editor).getQueryProxy().isValid()){
			MessageDialog.openError(shell, SAVE_DIALOGTITLE, Messages.QueryEditorUtils_InvalidQueryError);
			monitor.setCanceled(true);
			return null;
		}else if (query.getName().trim().length() == 0){
			MessageDialog.openError(shell, SAVE_DIALOGTITLE, Messages.QueryEditorUtils_BlankNameError);
			monitor.setCanceled(true);
			return null;
		}
				
		if (query.getUuid() != null && !query.getName().equals(((IQueryEditor)editor).getInputInternal().getName())){
			MessageDialog md = new MessageDialog(shell, SAVE_DIALOGTITLE,
					null, 
					Messages.QueryEditorUtils_OverwirteMessageDialog,
					MessageDialog.QUESTION, 
					new String[]{Messages.QueryEditorUtils_CreateNewButton, Messages.QueryEditorUtils_OverwriteButton, IDialogConstants.CANCEL_LABEL}, 0);
			int index = md.open();
			if (index == 2){
				monitor.setCanceled(true);
				return null;
			}else if (index == 0){
				return doSaveAs(editor, false).getQuery();
			}
			//otherwise continue with save
		}
				
		boolean newQuery = false;
		if (query.getUuid() == null){
			newQuery = true;
			//new query; we need to get folder location
			SaveQueryDialog dialog = new SaveQueryDialog(shell, query, false);
			if (dialog.open() != IDialogConstants.OK_ID){
				monitor.setCanceled(true);
					return null;
				}
					
				QueryFolder qf = dialog.getQueryFolder() ; 
				if (qf == null){
					QueryPlugIn.displayLog(Messages.QueryEditorUtils_QueryNotSavedError, null);
					monitor.setCanceled(true);
					return null;
				}
					
				if (!qf.isRootFolder()){
					query.setFolder(qf);
					query.setIsShared(qf.getEmployee() == null);
				
				}else if (qf.getUuid().equals(IQueryHibernateManager.CA_QUERY_KEY)){
					query.setIsShared(true);
				}
				//set the owner
				if (query.getIsShared() && SmartDB.isMultipleAnalysis()){
					//shared queries in the cross-ca analysis do not have a user
					query.setOwner(SmartDB.getSharedEmployee());
				}else{
					query.setOwner(SmartDB.getCurrentEmployee());
				}
				
				query.setConservationArea(SmartDB.getCurrentConservationArea());			
		}
				
		if (!QueryHibernateManager.getInstance().saveQuery(query, null)){
			monitor.setCanceled(true);
			return null;
		}
		
		if (newQuery){
			((IQueryEditor)editor).getInputInternal().setUuid(query.getUuid());
			((IQueryEditor)editor).getInputInternal().setId(query.getId());
		}
		((IQueryEditor)editor).getInputInternal().setQueryName(query.getName());
		return query;	
	}
	
	
	/**
	 * 
	 * @param editor must implement the IQueryEditor interface
	 * @param addNamePrefix
	 * @return
	 */
	public static QueryProxy doSaveAs(final IEditorPart ieditor, 
			final boolean addNamePrefix) {
		
		if (!(ieditor instanceof IQueryEditor)){
			throw new IllegalStateException("invalid editor"); //$NON-NLS-1$
		}
		
		final QueryProxy[] result = {null};
		final Shell shell = ieditor.getEditorSite().getShell();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					IQueryEditor editor = (IQueryEditor)ieditor;
					Query query = editor.getQueryProxy().getQuery();
					
					//ensure query is valid
					if (!editor.getQueryProxy().isValid()){
						MessageDialog.openError(shell, SAVE_DIALOGTITLE, Messages.QueryEditorUtils_SaveasInvalidQueryError);
						return;
					}
					
					monitor.beginTask(Messages.QueryEditorUtils_Progress_SaveAs, 3);
					monitor.subTask(Messages.QueryEditorUtils_Progress_Cloning);
					
					Query newQuery = (Query) query.clone(SmartDB.getCurrentEmployee());
					if (addNamePrefix){
						newQuery.setName(Messages.QueryEditorUtils_CopyOfLabel + newQuery.getName());
					}
					monitor.worked(1);
					
					monitor.subTask(Messages.QueryEditorUtils_Progress_SaveLocation);
					SaveQueryDialog dialog = new SaveQueryDialog(shell, newQuery, true);
					if (dialog.open() != IDialogConstants.OK_ID){
						return;
					}
					
					newQuery.setName(dialog.getQueryName());
					if (newQuery.getName().trim().length() == 0){
						MessageDialog.openError(shell, SAVE_DIALOGTITLE, Messages.QueryEditorUtils_SaveasBlankNameError);
						monitor.setCanceled(true);
						return;
					}
					
					QueryFolder qf = dialog.getQueryFolder();
					if (!qf.isRootFolder()){
						newQuery.setFolder(qf);
						newQuery.setIsShared(qf.getEmployee() == null);
					}else if (qf.getUuid().equals(IQueryHibernateManager.CA_QUERY_KEY)){
						newQuery.setFolder(null);
						newQuery.setIsShared(true);
					}else{
						newQuery.setFolder(null);
						newQuery.setIsShared(false);
					}
					//set the owner
					if (newQuery.getIsShared() && SmartDB.isMultipleAnalysis()){
						newQuery.setOwner(SmartDB.getSharedEmployee());
					}else{
						newQuery.setOwner(SmartDB.getCurrentEmployee());
					}
					newQuery.setConservationArea(SmartDB.getCurrentConservationArea());
					
					
					Query oldQuery = query;
					
					monitor.subTask(Messages.QueryEditorUtils_Progress_SavingQuery);
					
					QueryProxy newProxy = new QueryProxy(newQuery);
					if (!QueryHibernateManager.getInstance().saveQuery(newQuery, newProxy)){
						//not saved
						return;
					}
					result[0] = newProxy;

					QueryDefView view = ((QueryDefView.QueryDefViewWrapper)ieditor.getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID)).getComponent();
					//update the Query Def View; see if there is a better way to do this
					if(view != null){
						if (view.getQuery().equals(oldQuery)){
							view.setQuery(newProxy);
						}
					}
				}
			});
		} catch (Exception ex) {
			QueryPlugIn.displayLog(Messages.QueryEditorUtils_SaveQueryError + ex.getLocalizedMessage(), ex);
		}
		return result[0];
	}
}

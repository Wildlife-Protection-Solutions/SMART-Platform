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
import org.wcs.smart.query.QueryHibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.querylist.SaveQueryDialog;

public class QueryEditorUtils {

	
	/**
	 * 
	 * @param editor must implement the IQueryEditor interface
	 * @param addNamePrefix
	 * @return
	 */
	public static Query doSave(final IEditorPart editor,  
			IProgressMonitor monitor){
		
		if (!(editor instanceof IQueryEditor)){
			throw new IllegalStateException("invalid editor");
		}
		Query query = ((IQueryEditor)editor).getQuery();
//		Query query = ((IQueryEditor)editor).getQuery();
		Shell shell = editor.getSite().getShell();
		
		//validate if user can save the current query
		if (query.getIsShared() && !QueryHibernateManager.canModifyCaQueries()){			
			boolean ret = MessageDialog.openQuestion(shell, "Save", "You do not have permission to overwrite this query.  Would you like to save it as a new query?");
			if (ret){
				return doSaveAs(editor,true);
			}
			return null;
		}
				
		//ensure query is valid
		if (!query.isValid()){
			MessageDialog.openError(shell, "Save", "You cannot save an invalid query.  Please ensure fix the errors in the query and try saving again.");
			monitor.setCanceled(true);
			return null;
		}else if (query.getName().trim().length() == 0){
			MessageDialog.openError(shell, "Save", "Query name must not be blank.");
			monitor.setCanceled(true);
			return null;
		}
				
		if (query.getUuid() != null && !query.getName().equals(((IQueryEditor)editor).getInputInternal().getName())){
			MessageDialog md = new MessageDialog(shell, "Save Query",
					null, 
					"You have changed the name of this query do you want to overwrite the existing query or save as a new query?",
					MessageDialog.QUESTION, 
					new String[]{"Create New", "Overwrite", "Cancel"}, 0);
			int index = md.open();
			if (index == 2){
				monitor.setCanceled(true);
				return null;
			}else if (index == 0){
				return doSaveAs(editor, false);
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
					QueryPlugIn.displayLog("Query not saved.  Could not determine folder.", null);
					monitor.setCanceled(true);
					return null;
				}
					
				if (!qf.isRootFolder()){
					query.setFolder(qf);
					query.setIsShared(qf.getEmployee() == null);
				
				}else if (qf.getUuid().equals(QueryHibernateManager.CA_QUERY_KEY)){
					query.setIsShared(true);
				}
				query.setOwner(SmartDB.getCurrentEmployee());
				query.setConservationArea(SmartDB.getCurrentConservationArea());			
		}
				
		if (!QueryHibernateManager.saveQuery(query, false)){
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
	public static Query doSaveAs(final IEditorPart ieditor, 
			final boolean addNamePrefix) {
		
		if (!(ieditor instanceof IQueryEditor)){
			throw new IllegalStateException("invalid editor");
		}
		
		final Query[] result = {null};
		final Shell shell = ieditor.getEditorSite().getShell();
		
		ProgressMonitorDialog pmd = new ProgressMonitorDialog(shell);
		try {
			pmd.run(false, false, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					IQueryEditor editor = (IQueryEditor)ieditor;
					Query query = editor.getQuery();
					
					//ensure query is valid
					if (!query.isValid()){
						MessageDialog.openError(shell, "Save", "You cannot save an invalid query.  Please ensure fix the errors in the query and try saving again.");
						return;
					}
					
					monitor.beginTask("Save As...", 3);
					monitor.subTask("Cloning query...");
					
					Query newQuery = (Query) query.clone();
					if (addNamePrefix){
						newQuery.setName("Copy of " + newQuery.getName());
					}
					monitor.worked(1);
					
					monitor.subTask("Getting save location...");
					SaveQueryDialog dialog = new SaveQueryDialog(shell, newQuery, true);
					if (dialog.open() != IDialogConstants.OK_ID){
						return;
					}
					
					newQuery.setName(dialog.getQueryName());
					if (newQuery.getName().trim().length() == 0){
						MessageDialog.openError(shell, "Save", "Query name must not be blank.");
						monitor.setCanceled(true);
						return;
					}
					
					QueryFolder qf = dialog.getQueryFolder();
					if (!qf.isRootFolder()){
						newQuery.setFolder(qf);
						newQuery.setIsShared(qf.getEmployee() == null);
					
					}else if (qf.getUuid().equals(QueryHibernateManager.CA_QUERY_KEY)){
						newQuery.setIsShared(true);
					}
					newQuery.setOwner(SmartDB.getCurrentEmployee());
					newQuery.setConservationArea(SmartDB.getCurrentConservationArea());
					
					
					Query oldQuery = query;
					
					monitor.subTask("Saving query...");
					
					if (!QueryHibernateManager.saveQuery(newQuery, true)){
						//not saved
						return;
					}
					result[0] = newQuery;

					QueryDefView view = (QueryDefView)ieditor.getSite().getWorkbenchWindow().getActivePage().findView(QueryDefView.ID);
					//TODO: update the Query Def View; see if there is a better way to do this
					if(view != null){
						if (view.getQuery().equals(oldQuery)){
							view.setQuery(newQuery);
						}
					}
				}
			});
		} catch (Exception ex) {
			QueryPlugIn.displayLog("Error saving query: " + ex.getMessage(), ex);
		}
		return result[0];
	}
}

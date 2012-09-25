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


import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ICellModifier;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Item;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryEventManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.model.QueryInput;
import org.wcs.smart.query.querylist.QueryFolderEditablePropertyTester;

/**
 * Cell editor for modifying a QueryFolder name in a
 * tree.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class NameCellEditor implements ICellModifier {

	private TreeViewer viewer;
	
	public NameCellEditor(TreeViewer viewer){
		this.viewer = viewer;
	}
	
	@Override
	public void modify(Object element, String property,
			final Object value) {
		element = ((Item) element).getData();
		if (element instanceof QueryFolder) {
			updateFolder((QueryFolder) element, value.toString());
		}else if (element instanceof QueryInput){
			updateQuery((QueryInput)element, value.toString());
		}
	}

	@Override
	public Object getValue(Object element, String property) {
		if (element instanceof QueryInput){
			return ((QueryInput) element).getName();
		}
		return ((LabelProvider) viewer.getLabelProvider()).getText(element);
	}

	@Override
	public boolean canModify(Object element, String property) {
		if (element instanceof QueryFolder) {
			return QueryFolderEditablePropertyTester.canModify((QueryFolder)element, QueryFolderEditablePropertyTester.RENAME_OP);			
		}else if (element instanceof QueryInput){
			return QueryFolderEditablePropertyTester.canModify((QueryInput)element, QueryFolderEditablePropertyTester.RENAME_OP);
		}
		return false;
	}
	
	
	/*
	 * updates the report name in the database
	 */
	private void updateQuery(final QueryInput query, final String newName){

		if (newName.equals(query.getName())) {
			// nothing to update
			return;
		}
		
		Job j = new Job("Update Query Name") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					Query thisquery = (Query) session.load(query.getType().getHibernateClass(), query.getUuid());
					thisquery.setName(newName);
					query.setQueryName(newName);
					session.getTransaction().commit();
					fireChangeListener(thisquery);
					updateViewer(query);
				} catch (Exception ex) {
					if (session.getTransaction().isActive()){
						session.getTransaction().rollback();
					}
					QueryPlugIn.displayLog(
							"Could not save changes to query name. "
									+ ex.getMessage(), ex);
				} finally {
					session.close();
				}

				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	/*
	 * updates the report name in the database
	 */
	private void updateFolder(final QueryFolder folder, final String newName){

		if (newName.equals(folder.getName())) {
			// nothing to update
			return;
		}
		
		Job j = new Job("Update Query Folder Name") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					session.saveOrUpdate(folder);
					folder.updateName(newName, SmartDB.getCurrentLanguage());
					folder.setName(newName);
					session.getTransaction().commit();
					updateViewer(folder);
				} catch (Exception ex) {
					if (session.getTransaction().isActive()){
						session.getTransaction().rollback();
					}
					QueryPlugIn.displayLog(
							"Could not save changes to folder name. "
									+ ex.getMessage(), ex);
				} finally {
					session.close();
				}

				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	private void fireChangeListener(final Query query){
		viewer.getTree().getDisplay().syncExec(new Runnable(){
			@Override
			public void run() {
				QueryEventManager.getInstance().fireQueryNameChangedListeners(query);
			}});
		
	}
	
	private void updateViewer(final Object obj){
		viewer.getTree().getDisplay().asyncExec(new Runnable(){
			@Override
			public void run() {
				viewer.refresh(obj);
			}
		});
	}
}
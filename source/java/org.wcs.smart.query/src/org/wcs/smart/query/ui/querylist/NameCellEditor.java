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
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.hibernate.Session;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryFolderEditablePropertyTester;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

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
		}else if (element instanceof QueryEditorInput){
			updateQuery((QueryEditorInput)element, value.toString());
		}
	}

	@Override
	public Object getValue(Object element, String property) {
		if (element instanceof QueryEditorInput){
			return ((QueryEditorInput) element).getName();
		}
		return ((LabelProvider) viewer.getLabelProvider()).getText(element);
	}

	@Override
	public boolean canModify(Object element, String property) {
		if (element instanceof QueryFolder) {
			return QueryFolderEditablePropertyTester.canModify((QueryFolder)element, QueryFolderEditablePropertyTester.RENAME_OP);			
		}else if (element instanceof QueryEditorInput){
			return QueryFolderEditablePropertyTester.canModify((QueryEditorInput)element, QueryFolderEditablePropertyTester.RENAME_OP);
		}
		return false;
	}
	
	
	/*
	 * updates the report name in the database
	 */
	private void updateQuery(final QueryEditorInput query, final String newName){

		if (newName.equals(query.getName())) {
			// nothing to update
			return;
		}
		
		Job j = new Job(Messages.NameCellEditor_UpdateQueryNameJob) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					Query thisquery = (Query) session.load(query.getType().getHibernateClass(), query.getUuid());
					thisquery.updateName(SmartDB.getCurrentLanguage(), newName);
					thisquery.setName(newName);
					query.setQueryName(newName);
					session.getTransaction().commit();
					fireQueryNameChangeListener(thisquery);
				} catch (Exception ex) {
					if (session.getTransaction().isActive()){
						session.getTransaction().rollback();
					}
					QueryPlugIn.displayLog(
							Messages.NameCellEditor_CouldNotSaveQueryName
									+ ex.getLocalizedMessage(), ex);
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
		
		Job j = new Job(Messages.NameCellEditor_UpdateFolderNameJob) {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					session.saveOrUpdate(folder);
					folder.updateName(SmartDB.getCurrentLanguage(), newName);
					folder.setName(newName);
					session.getTransaction().commit();
					fireFolderNameChangeListener(folder);
				} catch (Exception ex) {
					if (session.getTransaction().isActive()){
						session.getTransaction().rollback();
					}
					QueryPlugIn.displayLog(
							Messages.NameCellEditor_CouldNotSaveFolderNameChange
									+ ex.getLocalizedMessage(), ex);
				} finally {
					session.close();
				}

				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}
	
	private void fireQueryNameChangeListener(final Query query){
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				QueryEventManager.getInstance().fireQueryNameModified(query);
			}});
		
	}
	
	private void fireFolderNameChangeListener(final QueryFolder folder){
		Display.getDefault().syncExec(new Runnable(){
			@Override
			public void run() {
				QueryEventManager.getInstance().fireFolderRenamed(folder);
			}});
		
	}

}
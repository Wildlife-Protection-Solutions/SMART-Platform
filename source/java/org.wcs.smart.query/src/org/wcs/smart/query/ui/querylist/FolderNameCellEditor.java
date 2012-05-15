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
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.QueryFolder;

/**
 * Cell editor for modifying a QueryFolder name in a
 * tree.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class FolderNameCellEditor implements ICellModifier {

	private TreeViewer viewer;
	
	public FolderNameCellEditor(TreeViewer viewer){
		this.viewer = viewer;
	}
	
	@Override
	public void modify(Object element, String property,
			final Object value) {
		element = ((Item) element).getData();
		if (element instanceof QueryFolder) {
			final QueryFolder folder = (QueryFolder) element;
			if (value.toString().equals(folder.getName())) {
				// nothing to update
				return;
			}
			folder.setName(value.toString());

			viewer.refresh();

			Job j = new Job("Update Name") {

				@Override
				protected IStatus run(IProgressMonitor monitor) {
					Session session = HibernateManager.openSession();
					session.beginTransaction();
					try {
						session.saveOrUpdate((QueryFolder) folder);
						folder.updateName(value.toString(),
								SmartDB.getCurrentLanguage());
						session.getTransaction().commit();
					} catch (Exception ex) {
						session.getTransaction().rollback();
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

	}

	@Override
	public Object getValue(Object element, String property) {
		return ((LabelProvider) viewer.getLabelProvider()).getText(element);
	}

	@Override
	public boolean canModify(Object element, String property) {
		if (element instanceof QueryFolder) {
			return !((QueryFolder) element).isRootFolder();
		}
		return false;
	}
}
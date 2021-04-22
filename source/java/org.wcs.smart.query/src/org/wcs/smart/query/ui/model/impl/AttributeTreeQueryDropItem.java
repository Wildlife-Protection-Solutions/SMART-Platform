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
package org.wcs.smart.query.ui.model.impl;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryFilterConfigManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeTreeDropItem;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;

/**
 * Attribute tree drop item
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTreeQueryDropItem extends AttributeTreeDropItem {

	/*
	 * Job to load the attribute list options
	 */
	private Job queryloadItemsJobs = new Job(Messages.AttributeTreeDropItem_LoadingListItemJobName) {

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			boolean showInactive = QueryFilterConfigManager.getInstance().isShowInactiveItems();
			try (Session s = HibernateManager.openSession()) {
				s.beginTransaction();
				try {
					roots = showInactive ? QueryDataModelManager.getInstance().getAllAttributeTreeNodes(attribute, s)
							: QueryDataModelManager.getInstance().getActiveAttributeTreeNodes(attribute, s);
				} catch (Exception ex) {
					QueryPlugIn.log("Could not initialize attribute tree items", ex); //$NON-NLS-1$
				} finally {
					s.getTransaction().rollback();
				}
			}

			input = roots;
			if (treeviewer == null)
				return Status.OK_STATUS;
			Display d = treeviewer.getTreeViewer().getTree().getDisplay();
			if (d != null && !d.isDisposed()) {
				d.asyncExec(new Runnable() {
					@Override
					public void run() {
						if (treeviewer == null || treeviewer.getTreeViewer().getControl().isDisposed()) {
							return;
						}
						treeviewer.getTreeViewer().setContentProvider(getContentProvider());
						treeviewer.getTreeViewer().setInput(roots);
						treeviewer.getTreeViewer().refresh();
					}

				});
			}
			return Status.OK_STATUS;
		}
	};

	protected AttributeTreeContentProvider getContentProvider(){
		return new AttributeTreeContentProvider(!QueryFilterConfigManager.getInstance().isShowInactiveItems(), false);
	}
	/**
	 * Creates a new attribute list drop item
	 * 
	 * @param parent parent composite
	 * @param panel  drop target
	 * @param att    the category attribute to make up the drop item
	 */
	public AttributeTreeQueryDropItem(CategoryAttribute att) {
		super(att);
		super.loadItemsJobs = queryloadItemsJobs;
	}

	/**
	 * Creates a new attribute list drop item
	 * 
	 * @param parent parent composite
	 * @param panel  drop target
	 * @param att    the attribute to make up the drop item
	 */
	public AttributeTreeQueryDropItem(Attribute att) {
		super(att);
		super.loadItemsJobs = queryloadItemsJobs;
	}

}
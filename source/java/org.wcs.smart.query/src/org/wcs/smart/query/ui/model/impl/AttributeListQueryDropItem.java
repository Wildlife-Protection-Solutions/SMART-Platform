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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryFilterConfigManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

/**
 * Attribute list type drop item.
 * 
 * 
 * @author Emily
 * @since 1.0.0
 */
public class AttributeListQueryDropItem extends org.wcs.smart.ui.ca.datamodel.dropitem.AttributeListDropItem{
	
	/*
	 * Job to load the attribute list options
	 */
	protected Job queryloadItemsJobs = new Job(Messages.AttributeListDropItem_LoadingJobName){

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			final ArrayList<ListItem> items = new ArrayList<ListItem>();
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try{
					boolean showInactive = QueryFilterConfigManager.getInstance().isShowInactiveItems();
					List<AttributeListItem> litems = QueryDataModelManager.getInstance().getAttributeListItems(attribute, s, !showInactive);
					for (AttributeListItem item : litems){
						items.add(new ListItem(item.getUuid(), item.getName(), item.getKeyId(), item.getIsActive()));
					}
					//add the any item
					items.add(0, BasicDropItemFactory.ANY_OPTION);				
					if (currentSelection != null && !items.contains(currentSelection)){
						//item is not longer active; but still in query
						items.add(currentSelection);
					}
				}finally{
					s.getTransaction().rollback();
				}
			}
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer == null || listViewer.getCombo().isDisposed()){
						return;
					}
					if (currentSelection != null && !items.contains(currentSelection)){
						items.add(currentSelection);
					}
					listViewer.setInput(items.toArray(new ListItem[items.size()]));
					if (currentSelection != null){
						listViewer.setSelection(new StructuredSelection(currentSelection));
					}else{
						listViewer.setSelection(new StructuredSelection(BasicDropItemFactory.ANY_OPTION));
					}
					getTargetPanel().redraw();
				}});
			return Status.OK_STATUS;
		}
	};

		
	/**
	 * Creates a new attribute list drop item
	 * 
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the category attribute to make up the drop item
	 */
	public AttributeListQueryDropItem(CategoryAttribute att) {
		super(att);
		super.loadItemsJobs = queryloadItemsJobs;
	}

	
	/**
	 * Creates a new attribute list drop item
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the attribute to make up the drop item
	 */
	public AttributeListQueryDropItem(Attribute att) {
		super(att);
		super.loadItemsJobs = queryloadItemsJobs;
	}
	

}

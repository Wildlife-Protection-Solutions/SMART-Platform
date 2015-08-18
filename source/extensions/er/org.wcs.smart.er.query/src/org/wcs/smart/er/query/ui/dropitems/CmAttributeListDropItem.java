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
package org.wcs.smart.er.query.ui.dropitems;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.er.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.AttributeListDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.util.UuidUtils;

/**
 * Configurable model attribute list drop item.  Only shows
 *  the attribute list items from the configured model.
 *  
 * @author Emily
 *
 */
public class CmAttributeListDropItem extends AttributeListDropItem {

	private CmAttribute cmAttribute;

	public CmAttributeListDropItem(CmAttribute cmAttribute, CategoryAttribute att) {
		super(att);
		this.cmAttribute = cmAttribute;
		this.key = "category:" + att.getCategory().getHkey() + ":cmattribute:" + att.getAttribute().getType().typeKey + ":" + UuidUtils.uuidToString(cmAttribute.getUuid()) + ":" + att.getAttribute().getKeyId();  //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
	
		loadItemsJobs = new Job(Messages.CmAttributeListDropItem_loadModelAttributeJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				
				final ArrayList<ListItem> items = new ArrayList<ListItem>();
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					List<AttributeListItem> litems = QueryDataModelManager.getInstance().getActiveAttributeListItems(CmAttributeListDropItem.this.attribute, s);
					for (AttributeListItem item : litems){
						CmAttributeListItem cmItem = null;
						if (CmAttributeListDropItem.this.cmAttribute.isUseCustomConfig()){
							cmItem = (CmAttributeListItem) s.createCriteria(CmAttributeListItem.class)
								.add(Restrictions.eq("listItem", item))  //$NON-NLS-1$
								.add(Restrictions.eq("configurableModel", CmAttributeListDropItem.this.cmAttribute.getNode().getModel())) //$NON-NLS-1$
								.add(Restrictions.eq("attribute", CmAttributeListDropItem.this.cmAttribute)) //$NON-NLS-1$
								.uniqueResult();
						}else{
							cmItem = (CmAttributeListItem) s.createCriteria(CmAttributeListItem.class)
									.add(Restrictions.eq("listItem", item))  //$NON-NLS-1$
									.add(Restrictions.eq("configurableModel", CmAttributeListDropItem.this.cmAttribute.getNode().getModel())) //$NON-NLS-1$
									.add(Restrictions.eq("dmAttribute", CmAttributeListDropItem.this.cmAttribute.getAttribute())) //$NON-NLS-1$
									.uniqueResult();
						}
						
						if (cmItem == null){
							items.add(new ListItem(item.getUuid(), item.getName(), item.getKeyId()));
						}else if (cmItem.getIsActive()){
							String cmName = cmItem.findNameNull(SmartDB.getCurrentLanguage());
							if (cmName != null){
								items.add(new ListItem(item.getUuid(), cmName, item.getKeyId()));
							}else{
								items.add(new ListItem(item.getUuid(), item.getName(), item.getKeyId()));	
							}
						}
					}
					//add the any item
					items.add(0, BasicDropItemFactory.ANY_OPTION);				
					if (currentSelection != null && !items.contains(currentSelection)){
						//item is not longer active; but still in query
						items.add(currentSelection);
					}

				}finally{
					s.getTransaction().rollback();
					s.close();
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
			}};
	}
	
	
	

}

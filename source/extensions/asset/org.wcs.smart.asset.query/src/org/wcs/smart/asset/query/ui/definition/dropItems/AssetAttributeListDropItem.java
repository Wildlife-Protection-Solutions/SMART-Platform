/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.asset.query.ui.definition.dropItems;

import java.util.ArrayList;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.asset.model.AssetAttribute;
import org.wcs.smart.asset.model.AssetAttributeListItem;
import org.wcs.smart.asset.query.ui.itempanel.AttributeWrapper;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.ui.model.ListItem;
import org.wcs.smart.query.ui.model.impl.AttributeListDropItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;

/**
 * Attribute list drop item
 * 
 * @author Emily
 *
 */
public class AssetAttributeListDropItem extends AttributeListDropItem {

	protected AssetAttribute attribute = null;
	
	public AssetAttributeListDropItem(AttributeWrapper att) {
		super(att.getAttribute().getName(), "assetattribute:" + att.getType().key + ":" + att.getAttribute().getType().key + ":" + att.getAttribute().getKeyId()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		this.attribute = att.getAttribute();
	}

	
	protected Job getLoadJob() {
		return loadItemsJobs;
	}
	
	
	/*
	 * Job to load the attribute list options
	 */
	protected Job loadItemsJobs = new Job("Loading list items"){ //$NON-NLS-1$

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			
			final ArrayList<ListItem> items = new ArrayList<ListItem>();
			try(Session s = HibernateManager.openSession()){
				s.beginTransaction();
				try{

					AssetAttribute assetattribute = s.get(AssetAttribute.class, attribute.getUuid());
					if (assetattribute != null && assetattribute.getAttributeList() != null) {
						for (AssetAttributeListItem list : assetattribute.getAttributeList()) {
							items.add(new ListItem(list.getUuid(), list.getName(), list.getKeyId(), true));
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
}

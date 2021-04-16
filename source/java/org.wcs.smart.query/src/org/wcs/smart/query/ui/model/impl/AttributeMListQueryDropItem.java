package org.wcs.smart.query.ui.model.impl;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryFilterConfigManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.ui.ca.datamodel.dropitem.AttributeMListDropItem;
import org.wcs.smart.ui.ca.datamodel.dropitem.ListItem;

public class AttributeMListQueryDropItem extends AttributeMListDropItem {
	/*
	 * Job to load the attribute list options
	 */
	protected Job localLoadJob = new Job(Messages.AttributeListDropItem_LoadingJobName){

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
					//items.add(0, BasicDropItemFactory.ANY_OPTION);				
					
				}finally{
					s.getTransaction().rollback();
				}
			}
			Display.getDefault().asyncExec(new Runnable(){
				@Override
				public void run() {
					if (listViewer == null || listViewer.isDisposed()) return;
					
					if (currentSelection != null) {
						for (ListItem current: currentSelection) {
							if (!items.contains(current)) items.add(current);
						}
					}
					
					listViewer.setInput(items);
					if (currentSelection != null) listViewer.setValue(currentSelection);
					
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
	public AttributeMListQueryDropItem(CategoryAttribute att) {
		super(att);
		super.loadItemsJobs = localLoadJob;
	}

	
	/**
	 * Creates a new attribute list drop item
	 * @param parent parent composite
	 * @param panel drop target
	 * @param att the attribute to make up the drop item
	 */
	public AttributeMListQueryDropItem(Attribute att) {
		super(att);
		super.loadItemsJobs = localLoadJob;
	}
}

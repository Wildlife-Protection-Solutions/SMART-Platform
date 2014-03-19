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
package org.wcs.smart.entity.query.ui.itempanel;


import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Query;
import org.hibernate.Session;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.entity.EntityPlugIn;
import org.wcs.smart.entity.model.EntityAttribute;
import org.wcs.smart.entity.model.EntityType;
import org.wcs.smart.entity.query.internal.Messages;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.common.ui.itempanel.SummaryDmObject;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;

/**
 * Data model content provider for summary queries.  Supports both
 * group by options and value options.  The group by option displays
 * the data model items that are relevant for group by options and the
 * values displays the item relevant for value options.
 *  
 * @author Emily
 *
 */
public class EntityTypeSummaryContentProvider implements ITreeContentProvider{

	//data model 
	private DataModel dataModel = null;
	private DataModelContentProvider provider;
	
	//data model 
	private List<EntityType> types = null;
	private Viewer viewer = null;
	
	/**
	 * Creates a new content provider 
	 * @type if this should return group by or value related items
	 */
	public EntityTypeSummaryContentProvider(){
		provider = new DataModelContentProvider(false, true, true);
	}

	/**
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
		provider.dispose();
	}

	
	/**
	 * 
	 * @param newInput must be a map that contains the keys
	 * QueryFilterContentProvider.ROOT_NODES - array of RootNodeType which should appear as 
	 * the root nodes in the tree
	 * RootNodeType.DATA_MODEL_FILTERS whose value is the current data model
	 * and 
	 * RootNodeType.PATROL_FILTERS whose value is the patrol filter options
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		
		if (newInput == null){
			provider.inputChanged(viewer, oldInput, null);
		}else if (newInput instanceof String){
			dataModel = null;
		}else{
			this.dataModel = (DataModel)newInput;
			provider.inputChanged(viewer, oldInput, this.dataModel);
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (types == null){
			loadTypes();
			return new String[]{Messages.EntityTypeSummaryContentProvider_LoadingLabel};
		}
		return types.toArray();
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof SummaryDmObject){
			SummaryDmObject parent = ((SummaryDmObject)parentElement);
			return getChildren(parent);
			
		}else if (parentElement instanceof EntityType){
			List<EntityAttribute> eas = new ArrayList<EntityAttribute>();
			eas.addAll(((EntityType) parentElement).getAttributes());
			//filter out numeric only
			for (Iterator<EntityAttribute> iterator = eas.iterator(); iterator.hasNext();) {
				EntityAttribute attribute = (EntityAttribute) iterator.next();
				if (attribute.getDmAttribute().getType() != AttributeType.LIST 
						&& attribute.getDmAttribute().getType() != AttributeType.TREE){
					iterator.remove();
				}	
			}
			//sort
			Collections.sort(eas, new Comparator<EntityAttribute>() {
				@Override
				public int compare(EntityAttribute o1, EntityAttribute o2) {
					return Collator.getInstance().compare(o1.getName(),o2.getName());
				}
			});
			//create required summary objects
			Object[] results = new Object[eas.size()];
			int cnt = 0;
			for (EntityAttribute att: eas){
				results[cnt++] = new SummaryDmObject(att, false);
			}
			return Arrays.copyOf(results, cnt);			
		}else{
			//assume data model
			return provider.getChildren(parentElement);
		}
	}


	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof EntityAttribute){
			return ((EntityAttribute) element).getEntityType();
		}else if (element instanceof EntityType){
			return null;
		}else{
			//assume data model
			return provider.getParent(element);	
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof EntityType){
			return true;
		}else if (element instanceof SummaryDmObject){
				if (!((SummaryDmObject) element).isValue()){
					if (((SummaryDmObject) element).getObject() instanceof AttributeTreeNode){
						return getAttributeTreeChildren((SummaryDmObject)element) != null;
					}else if (((SummaryDmObject) element).getObject() instanceof Attribute){
						if (((Attribute)((SummaryDmObject) element).getObject()).getType() == AttributeType.TREE){
							return true;
						}else{
							return false;
						}
					}else if (((SummaryDmObject) element).getObject() instanceof CategoryAttribute){
						if (((CategoryAttribute)((SummaryDmObject) element).getObject()).getAttribute().getType() == AttributeType.TREE){
							return true;
						}else{
							return false;
						}
					}
				}
				Object[] kids = getChildren(element);
				if (kids == null || kids.length == 0){
					return false;
				}
				return true;
		}else{
			//assume data model
			return provider.hasChildren(element);
			
		}
	}

	
	public Object[] getChildren(SummaryDmObject parent){
		Object raw = parent.getObject();
		if (raw instanceof EntityAttribute){
			raw = ((EntityAttribute) raw).getDmAttribute();
		}
		
		if (raw instanceof Attribute &&
				((Attribute)raw).getType() == AttributeType.TREE){
			return getAttributeTreeChildren(parent);		
		}else if (raw instanceof AttributeTreeNode){
			return getAttributeTreeChildren(parent);
		}
		return null;
	}
	
	private Object[] getAttributeTreeChildren(final SummaryDmObject parent){
		
		final List<AttributeTreeNode> kids = new ArrayList<AttributeTreeNode>();
		Job j = new Job(Messages.EntityTypeSummaryContentProvider_DataModelJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try{
					
					List<AttributeTreeNode> nodes = null;
					
					if (parent.getObject() instanceof EntityAttribute){
						Attribute att = ((EntityAttribute)parent.getObject()).getDmAttribute();
						
						att = QueryDataModelManager.getInstance().getAttribute(session, att);
						nodes = QueryDataModelManager.getInstance().getActiveAttributeTreeNodes(att, session);
						
					}else if (parent.getObject() instanceof AttributeTreeNode){
						AttributeTreeNode node = (AttributeTreeNode)parent.getObject();
						node.getAttribute().getName();
						nodes = node.getActiveChildren();
					}
					kids.addAll(nodes);					
					session.getTransaction().rollback();
				}catch (Exception ex){
					QueryPlugIn.log(Messages.EntityTypeSummaryContentProvider_ErrorLoadingTree + ex.getLocalizedMessage(), ex);
				}finally{
					session.close();
				}
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
		try{
			j.join();
		}catch (Exception ex){
			QueryPlugIn.log(Messages.EntityTypeSummaryContentProvider_ErrorLoadingTree  + ex.getLocalizedMessage(), ex);
			return null;
		}
		
		if (kids == null || kids.size() == 0){
			return null;
		}
		Object[] results = new Object[kids.size()];
		int index = 0;
		for (AttributeTreeNode kid : kids){
			if (parent.getObject() instanceof AttributeTreeNode){
				results[index++] = new SummaryDmObject(kid, parent.getObject2(), parent.isValue());
			}else{
				results[index++] = new SummaryDmObject(kid, parent.getObject(), parent.isValue());
			}
		}
		return results;
	}
	
	private Object[] getAttributeListChildren(final SummaryDmObject parent){
		
		final List<AttributeListItem> kids = new ArrayList<AttributeListItem>();
		Job j = new Job(Messages.EntityTypeSummaryContentProvider_LoadingListJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try{
					List<AttributeListItem> nodes = null;
					
					if (parent.getObject() instanceof EntityAttribute){
						Attribute att = ((EntityAttribute)parent.getObject()).getDmAttribute();
						nodes = QueryDataModelManager.getInstance().getActiveAttributeListItems(att, session);
						for(AttributeListItem it : nodes){
							it.getAttribute().getName();
						}
					}
					kids.addAll(nodes);					
					session.getTransaction().rollback();
				}catch (Exception ex){
					QueryPlugIn.log(Messages.EntityTypeSummaryContentProvider_ErrorLoadingList + ex.getLocalizedMessage(), ex);
				}finally{
					session.close();
				}
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
		try{
			j.join();
		}catch (Exception ex){
			QueryPlugIn.log(Messages.EntityTypeSummaryContentProvider_ErrorLoadingList + ex.getLocalizedMessage(), ex);
			return null;
		}
		
		if (kids == null || kids.size() == 0){
			return null;
		}
		Object[] results = new Object[kids.size()];
		int index = 0;
		
		for (AttributeListItem kid : kids){
			results[index++] = new SummaryDmObject(kid, parent.getObject(), parent.isValue());
		}
		return results;
	}
	
	private DataModelLabelProvider dmLabelProvider;
	private LabelProvider lp = null;
	public LabelProvider getLabelProvider(){
		if (lp == null) {
			dmLabelProvider = new DataModelLabelProvider();
			
			lp = new LabelProvider() {
				@Override
				public String getText(Object element) {
					if(element instanceof EntityType){
						return ((EntityType) element).getName();
					}else if (element instanceof SummaryDmObject){
						SummaryDmObject obj = (SummaryDmObject)element;
						if (obj.getObject() instanceof Attribute){
							return ((Attribute)obj.getObject()).getName();
						}else if (obj.getObject() instanceof EntityAttribute){
								return ((EntityAttribute)obj.getObject()).getName();
						}else if (obj.getObject() instanceof AttributeTreeNode){
							String name = ((AttributeTreeNode)obj.getObject()).getName();
							if (obj.isValue()){
								return MessageFormat.format(Messages.EntityTypeSummaryContentProvider_CountLabel, new Object[]{name});
							}else{
								return name;
							}
						}else if (obj.getObject() instanceof AttributeListItem){
							String name = ((AttributeListItem)obj.getObject()).getName();
							if (obj.isValue()){
								return MessageFormat.format(Messages.EntityTypeSummaryContentProvider_CountLabel, new Object[]{name});
							}else{
								return name;
							}
						}						
					} 
					return dmLabelProvider.getText(element);
				}
				@Override
				public Image getImage(Object element){
					if (element instanceof EntityType) {
						return EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.ENTITY_TYPE_ICON);
					}else if (element instanceof SummaryDmObject){
						if (((SummaryDmObject) element).getObject() instanceof EntityAttribute){
							return dmLabelProvider.getImage( ((EntityAttribute) ((SummaryDmObject) element).getObject()).getDmAttribute());	
						}
						return dmLabelProvider.getImage(((SummaryDmObject) element).getObject());
					} else {
						return dmLabelProvider.getImage(element);
					}
				}
			};
		}
		return lp;
		
	}
	
	
	private void loadTypes(){
		Job j = new Job(Messages.EntityTypeSummaryContentProvider_LoadEntityTypesJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					Query q = session.createQuery("FROM EntityType WHERE conservationArea = :ca and status = :stat"); //$NON-NLS-1$
					q.setParameter("ca", SmartDB.getCurrentConservationArea()); //$NON-NLS-1$
					q.setParameter("stat", EntityType.Status.ACTIVE); //$NON-NLS-1$
					
					List<EntityType> items = q.list();
					List<EntityType> tmp = new ArrayList<EntityType>();
					for (EntityType t : items){
						tmp.add(t);
						t.getDmAttribute().getName();
						for (EntityAttribute ea : t.getAttributes()){
							ea.getName();
							ea.getDmAttribute().getType();
							ea.getDmAttribute().getAggregations().size();
						}
					}
					types = tmp;
				} finally {
					session.getTransaction().rollback();
					session.close();
				}
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						//TODO: figure out how to only refresh this node which is a wrapped object
						//viewer.refresh(at);
						viewer.refresh();
					}
				});
				return Status.OK_STATUS;
			}
			
		};
		j.setSystem(true);
		j.schedule();
	}
	
	public static final LabelProvider lblProvider = new LabelProvider(){
		@Override
		public String getText(Object element){
			if (element instanceof EntityType){
				return ((EntityType) element).getName();
			}else if (element instanceof EntityAttribute){
				return ((EntityAttribute) element).getName();
			}else if (element instanceof Attribute){
				return ((Attribute) element).getName();
			}
			return super.getText(element);
		}
		
		@Override
		public Image getImage(Object element){
			if (element instanceof EntityType){
				return EntityPlugIn.getDefault().getImageRegistry().get(EntityPlugIn.ENTITY_TYPE_ICON);
			}else if (element instanceof EntityAttribute){
				return ((EntityAttribute) element).getDmAttribute().getType().getImage();
			}else if (element instanceof Attribute){
				return ((Attribute) element).getType().getImage();
			}
			return super.getImage(element);
		}
		
	};
}


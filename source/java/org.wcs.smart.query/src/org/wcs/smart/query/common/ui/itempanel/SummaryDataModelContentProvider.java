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
package org.wcs.smart.query.common.ui.itempanel;

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
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
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
public class SummaryDataModelContentProvider implements ITreeContentProvider{

	//data model 
	private DataModel dataModel = null;
	private DataModelContentProvider provider;
	private DataModelLabelProvider dmLabelProvider;
	private LabelProvider lp = null;
	private Type type;
	public enum Type{GROUPBY, VALUE};
	
	/**
	 * Various Root Nodes
	 */
	public enum DataModelItem{
		CATEGORIES_GROUPBY(Messages.SummaryDataModelContentProvider_CategoryGroupByLabel, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON)),
		CATEGORIES_VALUE(Messages.SummaryDataModelContentProvider_CategoryValueLabel, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON)),
		ATTRIBUTES_GROUPBY(Messages.SummaryDataModelContentProvider_AttributesCategoryLabel, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON)),
		ATTRIBUTES_VALUE(Messages.SummaryDataModelContentProvider_AttributesValueLabel, SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON));
		
		String guiName;
		Image image;
		DataModelItem(String guiName, Image image){
			this.guiName = guiName;
			this.image = image;
		}
	}
	
	/**
	 * Creates a new content provider 
	 * @type if this should return group by or value related items
	 */
	public SummaryDataModelContentProvider(Type type){
		this.type = type;
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
		if (dataModel == null ){
			return new String[]{Messages.SummaryDataModelContentProvider_LoadingText};
		}else if (type == Type.GROUPBY){
			return new Object[]{DataModelItem.CATEGORIES_GROUPBY, DataModelItem.ATTRIBUTES_GROUPBY};
		}else if (type == Type.VALUE){
			return new Object[]{DataModelItem.CATEGORIES_VALUE, DataModelItem.ATTRIBUTES_VALUE};
		}
		return new Object[]{};
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof SummaryDmObject){
			SummaryDmObject parent = ((SummaryDmObject)parentElement);
			return getChildren(parent);
			
		}else if (parentElement instanceof DataModelItem){
			if (parentElement == DataModelItem.CATEGORIES_GROUPBY || 
					parentElement == DataModelItem.CATEGORIES_VALUE ){
				Object[] kids = provider.getChildren(provider.getElements(null)[0]);
				Object[] results = new Object[kids.length];
				for (int i = 0; i < kids.length; i ++){
					results[i] = new SummaryDmObject((DmObject)kids[i], parentElement == DataModelItem.CATEGORIES_VALUE);
				}
				//assume data model
				return results;
			}else if (parentElement == DataModelItem.ATTRIBUTES_GROUPBY){
				//get all active attributes
				List<Attribute> atts = QueryDataModelManager.getInstance().getActiveAttributes(dataModel);
				
				//filter out numeric only
				for (Iterator<Attribute> iterator = atts.iterator(); iterator.hasNext();) {
					Attribute attribute = (Attribute) iterator.next();
					if (attribute.getType() != AttributeType.LIST && attribute.getType() != AttributeType.TREE){
						iterator.remove();
					}
					
				}
				//sort
				Collections.sort(atts, new Comparator<Attribute>() {
					@Override
					public int compare(Attribute o1, Attribute o2) {
						return Collator.getInstance().compare(o1.getName(),o2.getName());
					}
				});
				//create required summary objects
				Object[] results = new Object[atts.size()];
				int cnt = 0;
				for (Attribute att: atts){
					results[cnt++] = new SummaryDmObject(att, false);
				}
				return Arrays.copyOf(results, cnt);			
			}else if (parentElement == DataModelItem.ATTRIBUTES_VALUE){	
				List<Attribute> atts = QueryDataModelManager.getInstance().getActiveAttributes(dataModel);
				
				//filter out numeric only
				for (Iterator<Attribute> iterator = atts.iterator(); iterator.hasNext();) {
					Attribute attribute = (Attribute) iterator.next();
					if (attribute.getType() != AttributeType.NUMERIC && 
							attribute.getType() != AttributeType.LIST &&
							attribute.getType() != AttributeType.TREE){
						iterator.remove();
					}
					
				}
				//sort
				Collections.sort(atts, new Comparator<Attribute>() {
					@Override
					public int compare(Attribute o1, Attribute o2) {
						return Collator.getInstance().compare(o1.getName(),o2.getName());
					}
				});
				//create required summary objects
				Object[] results = new Object[atts.size()];
				int cnt = 0;
				for (Attribute att: atts){
					results[cnt++] = new SummaryDmObject(att, true);
				}
				return Arrays.copyOf(results, cnt);
			}
			return new Object[]{};
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
		if (element instanceof DataModelItem){
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
		if (element instanceof DataModelItem){
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

		if (parent.getObject() instanceof Attribute && 
				((Attribute)parent.getObject()).getType() == AttributeType.TREE){
			return getAttributeTreeChildren(parent);
		}else if (parent.getObject() instanceof CategoryAttribute && 
			((CategoryAttribute)parent.getObject()).getAttribute().getType() == AttributeType.TREE){
			return getAttributeTreeChildren(parent);
		}else if (parent.getObject() instanceof AttributeTreeNode){
			return getAttributeTreeChildren(parent);
		}else if (parent.getObject() instanceof Attribute && ((Attribute)parent.getObject()).getType() == AttributeType.LIST){
			return getAttributeListChildren(parent);
		}else if (parent.getObject() instanceof CategoryAttribute && ((CategoryAttribute)parent.getObject()).getAttribute().getType() == AttributeType.LIST){
			return getAttributeListChildren(parent);
		}
		
		Object[] kids = provider.getChildren(  parent.getObject() );
		
		if (kids == null){ return null; }
		
		Object[] results = new Object[kids.length];
		
		int cnt = 0;
		
		for (int i = 0; i < kids.length; i ++){
			boolean add = false;
			if (parent.isValue()){
				if (kids[i] instanceof Attribute){
					if (((Attribute)kids[i]).getType() == AttributeType.NUMERIC ||
						((Attribute)kids[i]).getType() == AttributeType.TREE ||
						((Attribute)kids[i]).getType() == AttributeType.LIST){
						add = true;
					}
				}else if (kids[i] instanceof CategoryAttribute){
					if (((CategoryAttribute)kids[i]).getAttribute().getType() == AttributeType.NUMERIC ||
							((CategoryAttribute)kids[i]).getAttribute().getType() == AttributeType.TREE ||
							((CategoryAttribute)kids[i]).getAttribute().getType() == AttributeType.LIST){
						add = true;
					}
				}else if (kids[i] instanceof Category){
					add = true;
				}
			}else{
				if (kids[i] instanceof Attribute){
					if (( (Attribute)kids[i]).getType() == AttributeType.LIST ||
							( (Attribute)kids[i]).getType() == AttributeType.TREE){
						add = true;
					}
				}else if (kids[i] instanceof CategoryAttribute){
					if (( (CategoryAttribute)kids[i]).getAttribute().getType() == AttributeType.LIST ||
							( (CategoryAttribute)kids[i]).getAttribute().getType() == AttributeType.TREE){
						add = true;
					}
				}else if (kids[i] instanceof Category){
					add = true;
				}
			}
			if (add){
				results[cnt++] = new SummaryDmObject(kids[i], parent.isValue());
			}
		}
		//assume data model
		if (cnt == 0){
			return null;
		}else{
			return Arrays.copyOf(results, cnt);
		}
	}
	
	private Object[] getAttributeTreeChildren(final SummaryDmObject parent){
		
		final List<AttributeTreeNode> kids = new ArrayList<AttributeTreeNode>();
		Job j = new Job(Messages.SummaryDataModelContentProvider_LoadingTreeJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try{
					
					List<AttributeTreeNode> nodes = null;
					
					if (parent.getObject() instanceof Attribute){
						Attribute attribute = (Attribute)parent.getObject();
						attribute = QueryDataModelManager.getInstance().getAttribute(session, attribute);
//						attribute.getName();
						nodes = QueryDataModelManager.getInstance().getActiveAttributeTreeNodes(attribute, session);
					}else if (parent.getObject() instanceof CategoryAttribute ){
						Attribute attribute = ((CategoryAttribute)parent.getObject()).getAttribute();
						attribute = QueryDataModelManager.getInstance().getAttribute(session, attribute);
//						attribute.getName();
						nodes = QueryDataModelManager.getInstance().getActiveAttributeTreeNodes(attribute, session);
					}else if (parent.getObject() instanceof AttributeTreeNode){
						AttributeTreeNode node = (AttributeTreeNode)parent.getObject();
						node.getAttribute().getName();
						nodes = node.getActiveChildren();
					}
					kids.addAll(nodes);					
					session.getTransaction().rollback();
				}catch (Exception ex){
					QueryPlugIn.log(Messages.SummaryDataModelContentProvider_CouldNotLoadTree + ex.getLocalizedMessage(), ex);
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
			QueryPlugIn.log(Messages.SummaryDataModelContentProvider_CouldNotLoadTree + ex.getLocalizedMessage(), ex);
			return null;
		}
		
		if (kids == null || kids.size() == 0){
			return null;
		}
		Object[] results = new Object[kids.size()];
		int index = 0;
		Object obj2 = parent.getObject2();
		if (parent.getObject() instanceof CategoryAttribute){
			obj2 = ((CategoryAttribute) parent.getObject()).getCategory();
		}
		
		for (AttributeTreeNode kid : kids){
			results[index++] = new SummaryDmObject(kid, obj2, parent.isValue());
		}
		return results;
	}
	
	private Object[] getAttributeListChildren(final SummaryDmObject parent){
		
		final List<AttributeListItem> kids = new ArrayList<AttributeListItem>();
		Job j = new Job(Messages.SummaryDataModelContentProvider_LoadingListItemsJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try{
					List<AttributeListItem> nodes = null;
					if (parent.getObject() instanceof Attribute){
						Attribute attribute = (Attribute)parent.getObject();
						nodes = QueryDataModelManager.getInstance().getActiveAttributeListItems(attribute, session);
						for(AttributeListItem it : nodes){
							it.getAttribute().getName();
						}
					}else if (parent.getObject() instanceof CategoryAttribute ){
						Attribute attribute = ((CategoryAttribute)parent.getObject()).getAttribute();
						nodes = QueryDataModelManager.getInstance().getActiveAttributeListItems(attribute, session);
						for(AttributeListItem it : nodes){
							it.getAttribute().getName();
						}
					}
					kids.addAll(nodes);					
					session.getTransaction().rollback();
				}catch (Exception ex){
					QueryPlugIn.log(Messages.SummaryDataModelContentProvider_CouldNotLoadItems + ex.getLocalizedMessage(), ex);
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
			QueryPlugIn.log(Messages.SummaryDataModelContentProvider_CouldNotLoadItems + ex.getLocalizedMessage(), ex);
			return null;
		}
		
		if (kids == null || kids.size() == 0){
			return null;
		}
		Object[] results = new Object[kids.size()];
		int index = 0;
		Object obj2 = parent.getObject2();
		if (parent.getObject() instanceof CategoryAttribute){
			obj2 = ((CategoryAttribute) parent.getObject()).getCategory();
		}
		
		for (AttributeListItem kid : kids){
			results[index++] = new SummaryDmObject(kid, obj2, parent.isValue());
		}
		return results;
	}
	

	public LabelProvider getLabelProvider(){
		if (lp == null) {
			dmLabelProvider = new DataModelLabelProvider();
			
			lp = new LabelProvider() {
				@Override
				public String getText(Object element) {
					if (element instanceof DataModelItem) {
						return ((DataModelItem) element).guiName;
					}else if (element instanceof SummaryDmObject){
						SummaryDmObject obj = (SummaryDmObject)element;
						if (obj.getObject() instanceof Attribute){
							return ((Attribute)obj.getObject()).getName();
						}else if (obj.getObject() instanceof AttributeTreeNode){
							String name = ((AttributeTreeNode)obj.getObject()).getName();
							if (obj.isValue()){
								return MessageFormat.format(Messages.SummaryDataModelContentProvider_CountLabel, new Object[]{name});
							}else{
								return name;
							}
						}else if (obj.getObject() instanceof AttributeListItem){
							String name = ((AttributeListItem)obj.getObject()).getName();
							if (obj.isValue()){
								return MessageFormat.format(Messages.SummaryDataModelContentProvider_CountLabel, new Object[]{name});
							}else{
								return name;
							}
						}else if (obj.getObject() instanceof Category){
							if (obj.isValue()){
								return MessageFormat.format(Messages.SummaryDataModelContentProvider_CountLabel, new Object[]{((Category)obj.getObject()).getName()});
							}else{
								return ((Category)obj.getObject()).getName();
							}
						}else if (obj.getObject() instanceof CategoryAttribute){
							return ((CategoryAttribute)obj.getObject()).getAttribute().getName();
						}						
					} 
					return dmLabelProvider.getText(element);
				}
				@Override
				public Image getImage(Object element){
					if (element instanceof DataModelItem) {
						return ((DataModelItem) element).image;
					}else if (element instanceof SummaryDmObject){
						return dmLabelProvider.getImage(((SummaryDmObject) element).getObject());
					} else {
						return dmLabelProvider.getImage(element);
					}
				}
			};
		}
		return lp;
		
	}
}

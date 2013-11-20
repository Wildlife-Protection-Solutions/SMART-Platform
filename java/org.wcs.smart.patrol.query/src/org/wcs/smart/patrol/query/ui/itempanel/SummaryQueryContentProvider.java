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
package org.wcs.smart.patrol.query.ui.itempanel;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.query.PatrolQueryPlugIn;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.model.AllCategory;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;
import org.wcs.smart.ui.properties.DataModelContentProvider;

/**
 * Content provider for summary query options.  Includes
 * value and group by options.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SummaryQueryContentProvider  implements ITreeContentProvider {

	private static final String LOADING_TEXT = Messages.SummaryQueryContentProvider_LoadingText;
	//data model 
	private DataModel dataModel = null;
	private DataModelContentProvider provider;
	
	//root nodes
	private RootNode valueNode = new RootNode(NodeType.VALUE_NODE);
	private RootNode groupByNode = new RootNode(NodeType.GROUP_BY_NODE);
	
	//patrol nodes
	private RootNode patrolValueNode = new RootNode(NodeType.PATROL_VALUES);
	private RootNode patrolGroupByNode = new RootNode(NodeType.PATROL_GROUPBYS);
	
	//date group by node 
	private RootNode dateGroupByNode = new RootNode(NodeType.PATROL_DATE_GROUPBYS);
	
	//area group bys
	private RootNode areaGroupByNode = new RootNode(NodeType.AREA_GROUPBYS); 
	
	//datamodel nodes
	private RootNode dataModelValueNode = new RootNode(NodeType.DATAMODEL_VALUES);
	private RootNode dataModelGroupByNode = new RootNode(NodeType.DATAMODEL_GROUPBYS);
	
	private AllCategory dataModelValueCategory = AllCategory.INSTANCE;
	private RootNode dataModelValueAttribute = new RootNode(NodeType.DATAMODEL_VALUE_ATTRIBUTES);

	private RootNode dataModelGroupByCategory = new RootNode(NodeType.DATAMODEL_GROUPBY_CATEGORY);
	private RootNode dataModelGroupByAttribute = new RootNode(NodeType.DATAMODEL_GROUPBY_ATTRIBUTES);
	
	private PatrolValueOption[] patrolValueOptions = null;
	private PatrolQueryOption[] patrolGroupByOption = null;
	private IDateGroupBy[] dateGroupByOptions = null;

	private HashMap<Area.AreaType, Area[]> areas = new HashMap<Area.AreaType, Area[]>();
	private TreeViewer viewer;
	
	/**
	 * Root node children
	 */
	public enum NodeType  {
		VALUE_NODE(Messages.SummaryQueryContentProvider_ValueOpsLabel),
		GROUP_BY_NODE(Messages.SummaryQueryContentProvider_GroupByOpLabel),
		PATROL_VALUES(Messages.SummaryQueryContentProvider_PatrolValuesLabel),
		PATROL_GROUPBYS(Messages.SummaryQueryContentProvider_PatrolGroupByLabel),
		PATROL_DATE_GROUPBYS(Messages.SummaryQueryContentProvider_DateLabel),
		AREA_GROUPBYS(Messages.SummaryQueryContentProvider_AreaGroupByLabel),
		DATAMODEL_VALUES(Messages.SummaryQueryContentProvider_DataModelValuesLabel),
		DATAMODEL_GROUPBYS(Messages.SummaryQueryContentProvider_DataModelGroupByLabel),
		DATAMODEL_VALUE_ATTRIBUTES(Messages.SummaryQueryContentProvider_DataModelAttributeLabel),
		DATAMODEL_GROUPBY_CATEGORY(Messages.SummaryQueryContentProvider_GroupByCategoryAttributeLabel),
		DATAMODEL_GROUPBY_ATTRIBUTES(Messages.SummaryQueryContentProvider_DataModelGroupByAttributesLabel);		
		
		private String name;
		
		private NodeType( String name){
			this.name = name;
		}
	}
	
	/**
	 * Creates a new content provider 
	 */
	public SummaryQueryContentProvider(){
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
	 * Clears the areas loaded into the content provider
	 */
	public void clearAreas(){
		this.areas.clear();
	}
	
	
	/**
	 * 
	 * @param newInput must be a map that contains the keys
	 * RootNodeType.DATA_MODEL_ITEM whose value is the current data model
	 * and 
	 * RootNodeType.PATROL_VALUES
	 * RootNodeType.PATROL_GROUPBYS
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = (TreeViewer)viewer;
		if (newInput == null){
			patrolValueOptions = null;
			patrolGroupByOption = null;
			provider.inputChanged(viewer, oldInput, null);
		}else if (newInput instanceof String){
			patrolValueOptions = null;
			patrolGroupByOption = null;
			dataModel = null;
			dateGroupByOptions = null;
		}else{
			if (newInput != null && !(newInput instanceof Map)){
				throw new IllegalArgumentException("new input must be map"); //$NON-NLS-1$
			}
			Map<?, ?> in = (Map<?, ?>)newInput;
			this.dataModel = (DataModel)in.get(NodeType.DATAMODEL_VALUES);
			patrolValueOptions = (PatrolValueOption[])in.get(NodeType.PATROL_VALUES);
			patrolGroupByOption = (PatrolQueryOption[])in.get(NodeType.PATROL_GROUPBYS);
			dateGroupByOptions = (IDateGroupBy[]) in.get(SummaryQueryContentProvider.NodeType.PATROL_DATE_GROUPBYS);
			provider.inputChanged(viewer, oldInput, this.dataModel);	
		}
		clearAreas();
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (patrolValueOptions == null &&
			patrolGroupByOption == null &&
			dataModel == null &&
			dateGroupByOptions == null){
			return new String[]{LOADING_TEXT};
		}
		
		return new Object[]{groupByNode, valueNode};
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RootNode){
			return ((RootNode)parentElement).getChildren();
		}else if (parentElement instanceof Area.AreaType){
			final Area.AreaType at = (Area.AreaType) parentElement;
			if (areas.get(at) != null) {
				return areas.get(at);
			} else {
				loadAreas(at);
				return new String[] { LOADING_TEXT };
			}
		}else if (parentElement instanceof AllCategory){
			Object[] kids = provider.getChildren(provider.getElements(null)[0]);
			Object[] results = new Object[kids.length];
			for (int i = 0; i < kids.length; i ++){
				results[i] = new SummaryDmObject((DmObject)kids[i], true);
			}
			//assume data model
			return results;
		}else if (parentElement instanceof SummaryDmObject){
			SummaryDmObject parent = ((SummaryDmObject)parentElement);
			return getChildren(parent);
			
		}
		return new Object[]{};
	}
	
	/**
	 * Loads the areas for a given area type
	 * @param at
	 */
	private void loadAreas(final Area.AreaType at) {
		Job j = new Job(MessageFormat.format(Messages.QueryFilterContentProvider_LoadingItemsJobName, new Object[]{at.getGuiName()})) {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				session.beginTransaction();
				try {
					List<Area> items = HibernateManager.loadAreas(at, session);
					areas.put(at, items.toArray(new Area[items.size()]));
				} finally {
					session.getTransaction().rollback();
					session.close();
				}
				Display.getDefault().asyncExec(new Runnable() {
					@Override
					public void run() {
						viewer.refresh(at);
					}
				});
				return Status.OK_STATUS;
			}
		};
		j.schedule();
	}

	private Object[] getAttributeTreeChildren(final SummaryDmObject parent){
	
		final List<AttributeTreeNode> kids = new ArrayList<AttributeTreeNode>();
		Job j = new Job(Messages.SummaryQueryContentProvider_LoadingTreeJobName){

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
					PatrolQueryPlugIn.log(Messages.SummaryQueryContentProvider_ErrorLoadingTreeItemsA + ex.getLocalizedMessage(), ex);
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
			PatrolQueryPlugIn.log(Messages.SummaryQueryContentProvider_ErrorLoadingTreeItemsB + ex.getLocalizedMessage(), ex);
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
		Job j = new Job(Messages.SummaryQueryContentProvider_LoadListItemJob){

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
					PatrolQueryPlugIn.log(Messages.SummaryQueryContentProvider_ErrorLoadListItem + ex.getLocalizedMessage(), ex);
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
			PatrolQueryPlugIn.log(Messages.SummaryQueryContentProvider_ErrorLoadingTreeItemsB + ex.getLocalizedMessage(), ex);
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
	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof RootNode){
			switch (((RootNode)element).type){
				case VALUE_NODE:	
				case GROUP_BY_NODE:
					return null;
					
				case PATROL_DATE_GROUPBYS:
					return patrolGroupByNode;
				case PATROL_GROUPBYS:
				case DATAMODEL_GROUPBYS:
					return groupByNode;
				case DATAMODEL_VALUES:
				case PATROL_VALUES:
					return valueNode;
			}
			return null;
		}else if (element instanceof AllCategory){
			return valueNode;
		}else if (element instanceof Area.AreaType){
			return areaGroupByNode;
		}else if (element instanceof PatrolQueryOption){
			return patrolGroupByNode;
		}else if (element instanceof PatrolValueOption){
			return patrolValueNode;
		//}else if (parentElement instanceof AREA FITLER){
		}else if (element instanceof SummaryDmObject){
			//assume data model
			return provider.getParent( ((SummaryDmObject)element).getObject() );	
		}else{
			return provider.getParent(element);
		}

	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof RootNode){
			return true;
		}else if (element instanceof PatrolQueryOption){
			return false;
		}else if (element instanceof PatrolValueOption){
			return false;
		}else if (element instanceof Area.AreaType){
			return true;
		}else if (element instanceof AllCategory){
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
			//return provider.hasChildren(((SummaryDmObject) element).getObject());		
		}else{
			//assume data model
			return provider.hasChildren(element);
			
		}
	}

	/*
	 * A root node of the content provider
	 */
	class RootNode{
		private NodeType type;
		
		/**
		 * Creates a new root node of a give type
		 * @param type
		 */
		public RootNode(NodeType type){
			this.type = type;
		}
		
		/**
		 * @return the children of the given root node
		 */
		public Object[] getChildren(){
			if (type == NodeType.VALUE_NODE){
				return new Object[]{patrolValueNode, dataModelValueNode};
			}else if (type == NodeType.PATROL_VALUES){
				return patrolValueOptions;
			}else if (type == NodeType.PATROL_GROUPBYS){
				Object[] kids = new Object[patrolGroupByOption.length + 1];
				for (int i = 0; i < patrolGroupByOption.length; i++){
					kids[i] = patrolGroupByOption[i];
				}
				kids[kids.length-1] = dateGroupByNode;
				return kids;
			}else if (type == NodeType.GROUP_BY_NODE){				
				if (SmartDB.isMultipleAnalysis()){
					//areas are not applicable for cross-ca analysis
					return new Object[]{patrolGroupByNode, dataModelGroupByNode};
				}else{
					return new Object[]{patrolGroupByNode, areaGroupByNode, dataModelGroupByNode};
				}
			}else if (type == NodeType.AREA_GROUPBYS){
				return Area.AreaType.values();
			}else if (type == NodeType.PATROL_DATE_GROUPBYS){
				return dateGroupByOptions;
			}else if (type == NodeType.DATAMODEL_VALUES){
				return new Object[]{dataModelValueCategory, dataModelValueAttribute};
			}else if (type == NodeType.DATAMODEL_VALUE_ATTRIBUTES){
				//get all active attributes
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
				
			}else if (type == NodeType.DATAMODEL_GROUPBYS){
				return new Object[]{dataModelGroupByCategory, dataModelGroupByAttribute};
			}else if (type == NodeType.DATAMODEL_GROUPBY_CATEGORY){
				Object[] kids = provider.getChildren(provider.getElements(null)[0]);
				Object[] results = new Object[kids.length];
				for (int i = 0; i < kids.length; i ++){
					results[i] = new SummaryDmObject((DmObject)kids[i], false);
				}
				//assume data model
				return results;
			}else if (type == NodeType.DATAMODEL_GROUPBY_ATTRIBUTES){
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
			}
			return null;
		}
		
		/**
		 * @return root node name
		 */
		public String getName(){
			return type.name;
		}
		
		/**
		 * @return root node image
		 */
		public Image getImage(){
			if (type == NodeType.VALUE_NODE){
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.VALUE_ICON);
			}else if (type == NodeType.GROUP_BY_NODE){
				 return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.GROUPBY_ICON);
			}else if (type == NodeType.PATROL_DATE_GROUPBYS){
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.CALENDAR_ICON);
			}else if (type == NodeType.DATAMODEL_VALUES ||
					type == NodeType.DATAMODEL_GROUPBYS){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
			}else if (type == NodeType.PATROL_GROUPBYS ||
					type == NodeType.PATROL_VALUES){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
			}else if (type == NodeType.DATAMODEL_VALUE_ATTRIBUTES){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
			}else if (type == NodeType.DATAMODEL_GROUPBY_ATTRIBUTES){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_LIST_ICON);
			}else if (type == NodeType.DATAMODEL_GROUPBY_CATEGORY){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
			}else if (type == NodeType.AREA_GROUPBYS){
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.AREA_FILTER_ICON);
			}
			return null;
		}
	}
}

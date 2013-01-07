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
package org.wcs.smart.query.ui.queryfilter;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.ca.datamodel.DmObject;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.PatrolQueryOptions.DateGroupByOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.ui.properties.DataModelContentProvider;

/**
 * Content provider for gridded query options.  Includes
 * value and group by options.
 * 
 * @author jeff
 * @since 1.0.0
 */
public class GriddedQueryContentProvider  implements ITreeContentProvider {

	//data model 
	private DataModel dataModel = null;
	private DataModelContentProvider provider;
	
	//root nodes
	private RootNode valueNode = new RootNode(NodeType.VALUE_NODE);

	//patrol nodes
	private RootNode patrolValueNode = new RootNode(NodeType.PATROL_VALUES);

	

	//datamodel nodes
	private RootNode dataModelValueNode = new RootNode(NodeType.DATAMODEL_VALUES);

	
	private RootNode dataModelValueCategory = new RootNode(NodeType.DATAMODEL_VALUE_CATEGORY);
	private RootNode dataModelValueAttribute = new RootNode(NodeType.DATAMODEL_VALUE_ATTRIBUTES);

	
	private PatrolValueOption[] gridValueOptions = null;
	private PatrolValueOption[] patrolRatioOptions = null;
	private DateGroupByOption[] dateGroupByOptions = null;
	
	
//	/**
//	 * Data model children items
//	 */
//	enum DataModelItem{
//		CATEGORIES("Categories"),
//		ATTRIBUTES("Attributes");
//		
//		String guiName;
//		
//		DataModelItem(String guiName){
//			this.guiName = guiName;
//		}
//	}
	
	
	
	/**
	 * Root node children
	 */
	enum NodeType  {
		VALUE_NODE(Messages.GriddedQueryContentProvider_ValueOptionsLabel),
		PATROL_VALUES(Messages.GriddedQueryContentProvider_PatrolValuesLabel),
		PATROL_DATE_GROUPBYS(Messages.GriddedQueryContentProvider_DateLabel),
		DATAMODEL_VALUES(Messages.GriddedQueryContentProvider_DataModelValuesLabel),
		DATAMODEL_VALUE_CATEGORY(Messages.GriddedQueryContentProvider_CategoriesLabel),
		DATAMODEL_VALUE_ATTRIBUTES(Messages.GriddedQueryContentProvider_AttributesLabel);		
		
		private String name;
		
		private NodeType( String name){
			this.name = name;
		}
	}
	
	/**
	 * Creates a new content provider 
	 */
	public GriddedQueryContentProvider(){
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
	 * RootNodeType.DATA_MODEL_ITEM whose value is the current data model
	 * and 
	 * RootNodeType.PATROL_VALUES
	 * RootNodeType.PATROL_GROUPBYS
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null){
			gridValueOptions = null;
			patrolRatioOptions = null;
			provider.inputChanged(viewer, oldInput, null);
		}else if (newInput instanceof String){
			gridValueOptions = null;
			patrolRatioOptions = null;
			dataModel = null;
			dateGroupByOptions = null;
		}else{
			if (newInput != null && !(newInput instanceof Map)){
				throw new IllegalArgumentException("new input must be map"); //$NON-NLS-1$
			}
			Map<?, ?> in = (Map<?, ?>)newInput;
			gridValueOptions = (PatrolValueOption[])in.get(NodeType.PATROL_VALUES);
			patrolRatioOptions = (PatrolValueOption[])in.get(NodeType.PATROL_VALUES);
			this.dataModel = (DataModel)in.get(NodeType.DATAMODEL_VALUES);
			
			dateGroupByOptions = (DateGroupByOption[]) in.get(GriddedQueryContentProvider.NodeType.PATROL_DATE_GROUPBYS);
			provider.inputChanged(viewer, oldInput, this.dataModel);	
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (gridValueOptions == null &&
			patrolRatioOptions == null &&
			dataModel == null &&
			dateGroupByOptions == null){
			return new String[]{Messages.GriddedQueryContentProvider_LoadingText};
		}
		return new Object[]{valueNode};
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RootNode){
			return ((RootNode)parentElement).getChildren();
		//}else if (parentElement instanceof AREA FITLER){
		}else if (parentElement instanceof SummaryDmObject){
			SummaryDmObject parent = ((SummaryDmObject)parentElement);
			return getChildren(parent);
		}
		return new Object[]{};
	}

	private Object[] getAttributeTreeChildren(final SummaryDmObject parent){
	
		final List<AttributeTreeNode> kids = new ArrayList<AttributeTreeNode>();
		Job j = new Job(Messages.GriddedQueryContentProvider_loadingtreeJobName){

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session s = HibernateManager.openSession();
				s.beginTransaction();
				try{
					
					List<AttributeTreeNode> nodes = null;
					if (parent.getObject() instanceof Attribute){
						s.update(parent.getObject());
						nodes = ((Attribute)parent.getObject()).getTree() ;
					}else if (parent.getObject() instanceof CategoryAttribute ){
						s.update(((CategoryAttribute)parent.getObject()).getAttribute() );
						nodes = ((CategoryAttribute)parent.getObject()).getAttribute().getTree() ;
					}else if (parent.getObject() instanceof AttributeTreeNode){
						s.update(parent.getObject());
						nodes =  ((AttributeTreeNode)parent.getObject()).getChildren() ;
					}else{
						throw new IllegalStateException("Parent node not an attribute tree."); //$NON-NLS-1$
					}
					for (AttributeTreeNode node : nodes){
						if (node.getIsActive()){
							kids.add( node );								
						}
					}
					s.getTransaction().rollback();
				}catch (Exception ex){
					QueryPlugIn.log(Messages.GriddedQueryContentProvider_ErrorLoadingTree + ex.getLocalizedMessage(), ex);
				}finally{
					s.close();
				}
				return Status.OK_STATUS;
			}
			
		};
		j.schedule();
		try{
			j.join();
		}catch (Exception ex){
			QueryPlugIn.log(Messages.GriddedQueryContentProvider_ErrorLoadingTree + ex.getLocalizedMessage(), ex);
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
	
	
	public Object[] getChildren(SummaryDmObject parent){

		if (parent.getObject() instanceof Attribute && 
				((Attribute)parent.getObject()).getType() == AttributeType.TREE &&
				!parent.isValue()
				){
			return getAttributeTreeChildren(parent);
		}else if (parent.getObject() instanceof CategoryAttribute && 
			((CategoryAttribute)parent.getObject()).getAttribute().getType() == AttributeType.TREE &&
			!parent.isValue()){
			
			return getAttributeTreeChildren(parent);
		}else if (parent.getObject() instanceof AttributeTreeNode && !parent.isValue()){
			return getAttributeTreeChildren(parent);
		}
		
		Object[] kids = provider.getChildren(  parent.getObject() );
		
		if (kids == null){ return null; }
		
		Object[] results = new Object[kids.length];
		
		int cnt = 0;
		
		for (int i = 0; i < kids.length; i ++){
			boolean add = false;
			if (parent.isValue()){
				if (kids[i] instanceof Attribute){
					if (( (Attribute)kids[i]).getType() == AttributeType.NUMERIC){
						add = true;
					}
				}else if (kids[i] instanceof CategoryAttribute){
					if (( (CategoryAttribute)kids[i]).getAttribute().getType() == AttributeType.NUMERIC){
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
					return null;
				case DATAMODEL_VALUES:
				case PATROL_VALUES:
					return valueNode;
			}
			return null;
		}else if (element instanceof PatrolValueOption){
			return patrolValueNode;
		//}else if (parentElement instanceof AREA FITLER){
//		}else if (element instanceof DataModelItem){
//			Object parent = getParent(element);
//			if (parent instanceof RootNode){
//				return dataModelValueNode;
//			}
//			return null;
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
//		}else if (element instanceof DataModelItem){
//			return false;
		//}else if (parentElement instanceof AREA FITLER){
		}else if (element instanceof SummaryDmObject){
			if (!((SummaryDmObject) element).isValue()){
				if (((SummaryDmObject) element).getObject() instanceof AttributeTreeNode){
					return ((AttributeTreeNode)((SummaryDmObject) element).getObject()).getChildren().size() > 0;
				}else if (((SummaryDmObject) element).getObject() instanceof Attribute){
					if (((Attribute)((SummaryDmObject) element).getObject()).getType() == AttributeType.TREE){
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
				return gridValueOptions;
			}else if (type == NodeType.PATROL_DATE_GROUPBYS){
				return dateGroupByOptions;
			}else if (type == NodeType.DATAMODEL_VALUES){
				return new Object[]{dataModelValueCategory, dataModelValueAttribute};
			}else if (type == NodeType.DATAMODEL_VALUE_CATEGORY){
				Object[] kids = provider.getChildren(provider.getElements(null)[0]);
				Object[] results = new Object[kids.length];
				for (int i = 0; i < kids.length; i ++){
					results[i] = new SummaryDmObject((DmObject)kids[i], true);
				}
				//assume data model
				return results;
			}else if (type == NodeType.DATAMODEL_VALUE_ATTRIBUTES){
				List<Attribute> atts = dataModel.getAttributes();
				Collections.sort(atts, new Comparator<Attribute>() {
					@Override
					public int compare(Attribute o1, Attribute o2) {
						return Collator.getInstance().compare(o1.getName(),o2.getName());
					}
				});
				Object[] results = new Object[atts.size()];
				int cnt = 0;
				for (Attribute att: atts){
					if (att.getType() == AttributeType.NUMERIC){
						results[cnt++] = new SummaryDmObject(att, true);
					}
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
			}else if (type == NodeType.PATROL_DATE_GROUPBYS){
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.CALENDAR_ICON);
			}else if (type == NodeType.DATAMODEL_VALUES){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
			}else if (type == NodeType.PATROL_VALUES){
				return SmartPatrolPlugIn.getDefault().getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
			}else if (type == NodeType.DATAMODEL_VALUE_ATTRIBUTES){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
			}else if (type == NodeType.DATAMODEL_VALUE_CATEGORY){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
			}
			return null;
		}
	}
}

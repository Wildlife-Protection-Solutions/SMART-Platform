package org.wcs.smart.query.ui.queyfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.DateGroupByOption;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;

public class SummaryQueryContentProvider  implements ITreeContentProvider {

	//data model 
	private DataModel dataModel = null;
	private DataModelContentProvider provider;
	
	//root nodes
	private RootNode patrolItemNode = new RootNode(NodeType.PATROL_ITEM);
	private RootNode dataModelItemNode = new RootNode(NodeType.DATA_MODEL_ITEM);
	//patrol nodes
	private RootNode patrolValueNode = new RootNode(NodeType.PATROL_VALUES);
	private RootNode patrolGroupByNode = new RootNode(NodeType.PATROL_GROUPBYS);
	
	//date group by node 
	private Object dateGroupByNode = new RootNode(NodeType.PATROL_DATE_GROUPBYS);
	
	private Object[] roots = new Object[]{patrolItemNode, dataModelItemNode};
	private Object[] patrolRoots = new Object[]{patrolValueNode,patrolGroupByNode};
	
	private PatrolValueOption[] patrolValueOptions = null;
	private PatrolQueryOption[] patrolGroupByOption = null;
	private DateGroupByOption[] dateGroupByOptions = null;
	
	
	/**
	 * Data model children items
	 */
	enum DataModelItem{
		CATEGORIES("Categories"),
		ATTRIBUTES("Attributes");
		
		String guiName;
		DataModelItem(String guiName){
			this.guiName = guiName;
		}
	}
	
	
	
	/**
	 * Root node children
	 */
	enum NodeType  {
		PATROL_ITEM("Patrol"),
		DATA_MODEL_ITEM("Data Model"),
		PATROL_VALUES("Values"),
		PATROL_GROUPBYS("Group Bys"),
		PATROL_DATE_GROUPBYS("Date");
		
		private String name;
		
		private NodeType( String name){
			this.name = name;
		}
	}
	
	/**
	 * Creates a new content provider 
	 */
	public SummaryQueryContentProvider(){
		provider = new DataModelContentProvider(false, true);
		
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
		if (newInput != null && !(newInput instanceof Map)){
			throw new IllegalArgumentException("new input must be map");
		}
		if (newInput == null){
			patrolValueOptions = null;
			patrolGroupByOption = null;
			provider.inputChanged(viewer, oldInput, null);
		}else{
			Map<?, ?> in = (Map<?, ?>)newInput;
			this.dataModel = (DataModel)in.get(NodeType.DATA_MODEL_ITEM);
			patrolValueOptions = (PatrolValueOption[])in.get(NodeType.PATROL_VALUES);
			patrolGroupByOption = (PatrolQueryOption[])in.get(NodeType.PATROL_GROUPBYS);
			dateGroupByOptions = (DateGroupByOption[]) in.get(SummaryQueryContentProvider.NodeType.PATROL_DATE_GROUPBYS);
			provider.inputChanged(viewer, oldInput, this.dataModel);	
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		return roots;
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof RootNode){
			return ((RootNode)parentElement).getChildren();
		//}else if (parentElement instanceof AREA FITLER){
		}else if (parentElement instanceof DataModelItem){
			if (parentElement == DataModelItem.CATEGORIES){
					return provider.getChildren(provider.getElements(null)[0]);	
			}else if (parentElement == DataModelItem.ATTRIBUTES){
//				DataModel dm = provider.
				List<Attribute> atts = new ArrayList<Attribute>();
				for (Attribute a : this.dataModel.getAttributes()){
					//TODO: check categories and only include attributes with at least one active 
					//category
					atts.add(a);
				}
				return atts.toArray(new Attribute[atts.size()]);
				
			}
			return null;
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
		if (element instanceof RootNode){
			switch (((RootNode)element).type){
				case PATROL_ITEM:	
				case DATA_MODEL_ITEM:
					return null;
					
				case PATROL_DATE_GROUPBYS:
					return patrolGroupByNode;
				case PATROL_GROUPBYS:
				case PATROL_VALUES:
					return patrolItemNode;
			}
			return null;
		}else if (element instanceof PatrolQueryOption){
			return patrolGroupByNode;
		}else if (element instanceof PatrolValueOption){
			return patrolValueNode;
		//}else if (parentElement instanceof AREA FITLER){
		}else if (element instanceof DataModelItem){
			return dataModelItemNode;
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
		if (element instanceof RootNode){
			return true;
		}else if (element instanceof PatrolQueryOption){
			return false;
		}else if (element instanceof PatrolValueOption){
			return false;
		}else if (element instanceof DataModelItem){
			return false;
		//}else if (parentElement instanceof AREA FITLER){
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
			if (type == NodeType.PATROL_ITEM){
				return patrolRoots;
			}else if (type == NodeType.PATROL_VALUES){
				return patrolValueOptions;
			}else if (type == NodeType.PATROL_GROUPBYS){
				Object[] kids = new Object[patrolGroupByOption.length + 1];
				for (int i = 0; i < patrolGroupByOption.length; i++){
					kids[i] = patrolGroupByOption[i];
				}
				kids[kids.length-1] = dateGroupByNode;
				return kids;
			}else if (type == NodeType.DATA_MODEL_ITEM){				

			}else if (type == NodeType.PATROL_DATE_GROUPBYS){
				return dateGroupByOptions;
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
			if (type == NodeType.PATROL_ITEM){
				return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
			}else if (type == NodeType.DATA_MODEL_ITEM){
				 return JFaceResources.getImageRegistry().get(DataModelLabelProvider.DATA_MODEL_ICON);
			}else if (type == NodeType.PATROL_DATE_GROUPBYS){
				return JFaceResources.getImageRegistry().get(QueryPlugIn.CALENDAR_ICON);
			}else if (type == NodeType.PATROL_GROUPBYS){
				return JFaceResources.getImageRegistry().get(QueryPlugIn.GROUPBY_ICON);
			}else if (type == NodeType.PATROL_VALUES){
				return JFaceResources.getImageRegistry().get(QueryPlugIn.VALUE_ICON);
			}
			return null;
		}
	}
}

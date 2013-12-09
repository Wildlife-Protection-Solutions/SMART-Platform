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
package org.wcs.smart.observation.query.ui.itempanel;

import java.text.Collator;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
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
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.observation.query.internal.Messages;
import org.wcs.smart.query.QueryDataModelManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.ui.properties.DataModelContentProvider;

/**
 * A content provider that provides the query
 * filter options.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryFilterContentProvider implements ITreeContentProvider {

	private static final String LOADING_TEXT = Messages.QueryFilterContentProvider_LoadingLabel;
	//data model 
	private DataModel dataModel = null;
	private DataModelContentProvider provider;
	
	//root nodes
	public static final String ROOT_NODES = "RootNodes"; //$NON-NLS-1$
	private RootNode generalFiltersRoot =new RootNode(RootNodeType.GENERAL_FILTERS);
	private RootNode dataModelFiltersRoot =new RootNode(RootNodeType.DATA_MODEL_FILTERS);
	private RootNode areaFilterRoot = new RootNode(RootNodeType.AREA_FILTERS);
	private RootNode otherItemRoot = new RootNode(RootNodeType.OTHER_ITEMS);
	
	private Object[] roots = new Object[]{generalFiltersRoot, dataModelFiltersRoot, areaFilterRoot, otherItemRoot};
		
	private HashMap<Area.AreaType, Area[]> areas = new HashMap<Area.AreaType, Area[]>();
	
	/**
	 * Data model children items
	 */
	enum DataModelItem{
		CATEGORIES(Messages.QueryFilterContentProvider_CategoriesLabel),
		ATTRIBUTES(Messages.QueryFilterContentProvider_AttributeLabel);
		
		String guiName;
		DataModelItem(String guiName){
			this.guiName = guiName;
		}
	}
	
	/**
	 * Other item children
	 */
	public enum GeneralItems{
		WAYPOINT_SOURCE(Messages.QueryFilterContentProvider_WaypointSourceName);
		
		public String guiName;
		GeneralItems(String guiName){
			this.guiName = guiName;
		}
	}
	
	/**
	 * Other item children
	 */
	public enum OtherItems{
		BRACKETS(Messages.QueryFilterContentProvider_BracketsLabel),
		NOT (Messages.QueryFilterContentProvider_NotLabel);
		
		String guiName;
		OtherItems(String guiName){
			this.guiName = guiName;
		}
	}
	
	/**
	 * Root node children
	 */
	public enum RootNodeType  {
		GENERAL_FILTERS(Messages.QueryFilterContentProvider_GeneralFiltersName),
		DATA_MODEL_FILTERS(Messages.QueryFilterContentProvider_DataModelFiltersLabel),
		AREA_FILTERS(Messages.QueryFilterContentProvider_AreaFiltersLabel),
		OTHER_ITEMS(Messages.QueryFilterContentProvider_OperatoresLabel);
		
		private String name;
		private RootNodeType( String name){
			this.name = name;
		}
	}
	
	private TreeViewer viewer;
	/**
	 * Creates a new content provider 
	 */
	public QueryFilterContentProvider(TreeViewer viewer){
		provider = new DataModelContentProvider(false, true, true);
		this.viewer = viewer;
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
			if (newInput != null && !(newInput instanceof Map)){
				throw new IllegalArgumentException("new input must be map"); //$NON-NLS-1$
			}
			
			Map<?, ?> in = (Map<?, ?>)newInput;
			
			//create root nodes
			RootNodeType[] rootnodes = (RootNodeType[])in.get(QueryFilterContentProvider.ROOT_NODES);
			roots = new Object[rootnodes.length];
			for (int i = 0; i < roots.length; i ++){
				roots[i] = new RootNode(rootnodes[i]);
			}
			
			this.dataModel = (DataModel)in.get(RootNodeType.DATA_MODEL_FILTERS); 
			
			provider.inputChanged(viewer, oldInput, this.dataModel);
			clearAreas();
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (dataModel == null ){
			return new String[]{LOADING_TEXT};
		}
		return roots;
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
		}else if (parentElement instanceof DataModelItem){
			if (parentElement == DataModelItem.CATEGORIES){
					return provider.getChildren(provider.getElements(null)[0]);	
			}else if (parentElement == DataModelItem.ATTRIBUTES){
				List<Attribute> atts = QueryDataModelManager.getInstance().getActiveAttributes(this.dataModel);
				
				Collections.sort(atts, new Comparator<Attribute>() {
					@Override
					public int compare(Attribute o1, Attribute o2) {
						return Collator.getInstance().compare(o1.getName(),o2.getName());
					}
				});
				return atts.toArray(new Attribute[atts.size()]);
				
			}
			return new Object[]{};
		}else{
			//assume data model
			return provider.getChildren(parentElement);
			
		}
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

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		if (element instanceof RootNode){
			return null;
		}else if (element instanceof OtherItems){
			return otherItemRoot;
		}else if (element instanceof DataModelItem){
			return dataModelFiltersRoot;
		}else if (element instanceof Area.AreaType){
			return areaFilterRoot;
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
		}else if (element instanceof DataModelItem){
			return true;
		}else if (element instanceof OtherItems){
			return false;
		}else if (element instanceof Area.AreaType){
			return true;
		}else{
			//assume data model
			return provider.hasChildren(element);
			
		}
	}

	
	/*
	 * A root node of the content provider
	 */
	class RootNode{
		private RootNodeType type;
		
		/**
		 * Creates a new root node of a give type
		 * @param type
		 */
		public RootNode(RootNodeType type){
			this.type = type;
		}
		
		/**
		 * @return the children of the given root node
		 */
		public Object[] getChildren(){
			if (type == RootNodeType.AREA_FILTERS){
				return Area.AreaType.values();
			}else if (type == RootNodeType.DATA_MODEL_FILTERS){				
				return DataModelItem.values();
			}else if (type == RootNodeType.OTHER_ITEMS){
				return OtherItems.values();
			}else if (type == RootNodeType.GENERAL_FILTERS){
				return GeneralItems.values();
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
			if (type == RootNodeType.DATA_MODEL_FILTERS){
				 return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.DATA_MODEL_ICON);
			}else if (type == RootNodeType.AREA_FILTERS){
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.AREA_FILTER_ICON);
			}
			return null;
		}
	}
}

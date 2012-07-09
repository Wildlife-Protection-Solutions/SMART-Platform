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
package org.wcs.smart.query.ui.queyfilter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.DataModel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.parser.internal.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.ui.properties.DataModelContentProvider;
import org.wcs.smart.ui.properties.DataModelLabelProvider;

/**
 * A content provider that provides the query
 * filter options.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryFilterContentProvider implements ITreeContentProvider {

	//data model 
	private DataModel dataModel = null;
	private DataModelContentProvider provider;
	
	//root nodes
	private RootNode patrolFiltersRoot = new RootNode(RootNodeType.PATROL_FILTERS);
	private RootNode dataModelFiltersRoot =new RootNode(RootNodeType.DATA_MODEL_FILTERS);
	private RootNode areaFilterRoot = new RootNode(RootNodeType.AREA_FILTERS);
	private RootNode otherItemRoot = new RootNode(RootNodeType.OTHER_ITEMS);
	private Object[] roots = new Object[]{patrolFiltersRoot, dataModelFiltersRoot, areaFilterRoot, otherItemRoot};
	
	private PatrolQueryOption[] patrolOptions = null;
	
	private HashMap<Area.AreaType, Area[]> areas = new HashMap<Area.AreaType, Area[]>();
	
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
	 * Other item children
	 */
	public enum OtherItems{
		BRACKETS(" (   ) "),
		NOT (" NOT ");
		
		String guiName;
		OtherItems(String guiName){
			this.guiName = guiName;
		}
	}
	
	/**
	 * Root node children
	 */
	enum RootNodeType  {
		PATROL_FILTERS("Patrol Filters"),
		DATA_MODEL_FILTERS("Data Model Filters"),
		AREA_FILTERS("Area Filters"),
		OTHER_ITEMS("Operators");
		
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
	 * RootNodeType.DATA_MODEL_FILTERS whose value is the current data model
	 * and 
	 * RootNodeType.PATROL_FILTERS whose value is the patrol filter options
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput == null){
			patrolOptions = null;
			provider.inputChanged(viewer, oldInput, null);
		}else if (newInput instanceof String){
			patrolOptions = null;
			dataModel = null;
		}else{
			if (newInput != null && !(newInput instanceof Map)){
				throw new IllegalArgumentException("new input must be map");
			}
			Map<?, ?> in = (Map<?, ?>)newInput;
			this.dataModel = (DataModel)in.get(RootNodeType.DATA_MODEL_FILTERS); 
			patrolOptions = (PatrolQueryOption[]) in.get(RootNodeType.PATROL_FILTERS);		
			provider.inputChanged(viewer, oldInput, this.dataModel);
			clearAreas();
		}
	}

	/**
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (dataModel == null && patrolOptions == null){
			return new String[]{"Loading"};
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
		}else if (parentElement instanceof PatrolQueryOption){
			return null;
		}else if (parentElement instanceof Area.AreaType){
			final Area.AreaType at = (Area.AreaType) parentElement;
			if (areas.get(at) != null) {
				return areas.get(at);
			} else {
				loadAreas(at);
				return new String[] { "Loading..." };
			}
		}else if (parentElement instanceof DataModelItem){
			if (parentElement == DataModelItem.CATEGORIES){
					return provider.getChildren(provider.getElements(null)[0]);	
			}else if (parentElement == DataModelItem.ATTRIBUTES){
				List<Attribute> atts = new ArrayList<Attribute>();
				for (Attribute a : this.dataModel.getAttributes()){
					//TODO: check categories and only include attributes with at least one active category
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
	 * Loads the areas for a given area type
	 * @param at
	 */
	private void loadAreas(final Area.AreaType at) {
		Job j = new Job("Loading " + at.getGuiName() + " items") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				Session session = HibernateManager.openSession();
				try {
					session.beginTransaction();
					@SuppressWarnings("unchecked")
					List<Area> items = session
							.createCriteria(Area.class)
							.add(Restrictions.eq(
									"conservationArea",
									SmartDB.getCurrentConservationArea()))
							.add(Restrictions.eq("type", at)).list();
					areas.put(at, items.toArray(new Area[items.size()]));
					session.getTransaction().commit();
					Display.getDefault().asyncExec(new Runnable() {
						@Override
						public void run() {
							viewer.refresh(at);
						}
					});
				} finally {
					session.close();
				}
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
		}else if (element instanceof PatrolQueryOption){
			return patrolFiltersRoot;
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
		}else if (element instanceof PatrolQueryOption){
			return false;
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
			if (type == RootNodeType.PATROL_FILTERS){
				return QueryFilterContentProvider.this.patrolOptions;
			}else if (type == RootNodeType.AREA_FILTERS){
				return Area.AreaType.values();
			}else if (type == RootNodeType.DATA_MODEL_FILTERS){				
				return DataModelItem.values();
			}else if (type == RootNodeType.OTHER_ITEMS){
				return OtherItems.values();
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
			if (type == RootNodeType.PATROL_FILTERS){
				return JFaceResources.getImageRegistry().get(SmartPatrolPlugIn.PATROL_ICON);
			}else if (type == RootNodeType.DATA_MODEL_FILTERS){
				 return JFaceResources.getImageRegistry().get(DataModelLabelProvider.DATA_MODEL_ICON);
			}else if (type == RootNodeType.AREA_FILTERS){
				return JFaceResources.getImageRegistry().get(QueryPlugIn.AREA_FILTER_ICON);
			}
			return null;
		}
	}
}

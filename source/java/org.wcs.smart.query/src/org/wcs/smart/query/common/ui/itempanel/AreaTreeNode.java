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

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.hibernate.Session;
import org.wcs.smart.ca.Area;
import org.wcs.smart.ca.Area.AreaType;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.ui.SmartLabelProvider;

/**
 * Area tree node item.
 * 
 * @author Emily
 *
 */
public class AreaTreeNode implements IItemTreeNode{

	private static final String KEY = "area"; //$NON-NLS-1$

	private String name;
	private AreaContentProvider provider;
	
	private final static LabelProvider lblProvider =  new LabelProvider(){
		
		@Override 
		public Image getImage(Object element) {
			if (element instanceof Area.AreaType || element instanceof Area){
				return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.AREA_POLYGON_FILTER_ICON);
			}
			return null;
		}
		
		@Override
		public String getText(Object element) {
			if (element instanceof Area.AreaType) {
				return SmartLabelProvider.getAreaTypeName((AreaType) element);				
			} else if (element instanceof Area) {
				return ((Area) element).getName();
			}
			return super.getText(element);
			
		}
	};
	
	public AreaTreeNode(String name){
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Image getImage() {
		return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.AREA_FILTER_ICON);
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		if (provider == null){
			provider = new AreaContentProvider();
		}
		return provider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return lblProvider;
	}

	@Override
	public String getKey() {
		return KEY;
	}
	
	public void clearAreas(){
		provider.clearAreas();
	}
	
	private class AreaContentProvider implements ITreeContentProvider{

		private HashMap<Area.AreaType, Area[]> areas = new HashMap<Area.AreaType, Area[]>();
		private TreeViewer viewer;
		
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			this.viewer = (TreeViewer) viewer;
			this.areas.clear();

		}

		@Override
		public Object[] getElements(Object inputElement) {
			return Area.AreaType.values();
		}
		
		public void clearAreas(){
			areas.clear();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			 if (parentElement instanceof Area.AreaType){
				final Area.AreaType at = (Area.AreaType) parentElement;
				if (areas.get(at) != null) {
					return areas.get(at);
				} else {
					loadAreas(at);
					return new String[] { Messages.AreaTreeNode_LoadingTest };
				}
			 }
			return null;
		}

		@Override
		public Object getParent(Object element) {
			if (element instanceof Area){
				return ((Area) element).getType();
			}
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof Area.AreaType){
				return true;
			}
			return false;
		}
		
		

		/**
		 * Loads the areas for a given area type
		 * @param at
		 */
		private void loadAreas(final Area.AreaType at) {
			Job j = new Job(MessageFormat.format(Messages.AreaTreeNode_jobname, 
					new Object[]{SmartLabelProvider.getAreaTypeName(at)})) {
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
					viewer.getControl().getDisplay().asyncExec(new Runnable() {
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
			j.schedule();
		}


	}
}

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
package org.wcs.smart.query.ui.querylist;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.query.IQueryHibernateManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.Query;
import org.wcs.smart.query.model.QueryFolder;
import org.wcs.smart.query.ui.editor.QueryEditorInput;

/**
 * Content provider for the query list view.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryListContentProvider implements ITreeContentProvider{

	public static final int FOLDER_KEY = 1;
	public static final int QUERY_KEY = 2;
	
	private List<QueryFolder> rootFolders = null;
	private HashMap<UUID, QueryFolder> folders = null;
	private HashMap<UUID, List<QueryEditorInput>> queries = null;
	
	private boolean includeQueries = false;
	
	public QueryListContentProvider(boolean includeQueries){
		this.includeQueries = includeQueries;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
	 */
	@Override
	public void dispose() {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		folders = new HashMap<UUID, QueryFolder>();
		if (newInput == null || !(newInput instanceof HashMap)){
			rootFolders = null;
			queries = null;
			return;
		}
		if (newInput instanceof HashMap){
			HashMap<Object, Object> data = (HashMap<Object, Object>)newInput;
			rootFolders = (List<QueryFolder>) data.get(FOLDER_KEY);
			queries = (HashMap<UUID, List<QueryEditorInput>>) data.get(QUERY_KEY);
		}
	
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getElements(java.lang.Object)
	 */
	@Override
	public Object[] getElements(Object inputElement) {
		if (rootFolders == null){
			return new String[]{Messages.QueryListViewContentProvider_LoadingLabel};
		}
		return rootFolders.toArray();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getChildren(java.lang.Object)
	 */
	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof QueryFolder){
			QueryFolder folder = (QueryFolder)parentElement;
			List<Object> children = new ArrayList<Object>();
			if (folder.getChildren() != null){
				for (QueryFolder kid: folder.getChildren()){
					children.add(kid);
					folders.put(kid.getUuid(), kid);
				}
			}
			if (includeQueries){
				List<QueryEditorInput> qs = queries.get( folder.getUuid() );
				if (qs != null){
					children.addAll(qs);
				}
			}
			Collections.sort(children, new Comparator<Object>() {

				@Override
				public int compare(Object o1, Object o2) {
					boolean q1 = o1 instanceof QueryFolder;
					boolean q2 = o2 instanceof QueryFolder;
					if (q1 & !q2) return -1;
					if (!q1 & q2) return 1;
					if (q1 && q2){
						return Collator.getInstance().compare(((QueryFolder)o1).getName().toLowerCase(), ((QueryFolder)o2).getName().toLowerCase());
					}else{
						return Collator.getInstance().compare(((QueryEditorInput)o1).getName().toLowerCase(), ((QueryEditorInput)o2).getName().toLowerCase());
					}
				}
			});
			return children.toArray();
		}
		return new Object[]{};
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#getParent(java.lang.Object)
	 */
	@Override
	public Object getParent(Object element) {
		
		if (element instanceof QueryEditorInput && queries != null){
			QueryFolder parent = null;
			for (Iterator<Entry<UUID, List<QueryEditorInput>>> iterator = queries.entrySet().iterator(); iterator.hasNext();) {
				Entry<UUID, List<QueryEditorInput>> type = (Entry<UUID, List<QueryEditorInput>>) iterator.next();
				if (type.getValue().contains(element)){
					parent = folders.get(type.getKey());
					if (parent == null){
						for (QueryFolder f : rootFolders){
							if (f.getUuid().equals(type.getKey())){
								return f;
							}
						}
					}else{
						return parent;
					}
				}
			}
			return null;
		}else if (element instanceof QueryFolder){
			if (((QueryFolder) element).isRootFolder()){
				return null;
			}
			QueryFolder parent = ((QueryFolder) element).getParentFolder();
			if (parent == null){
				for (QueryFolder f : rootFolders){
					if (f.isRootFolder()){
						if (((QueryFolder) element).getEmployee() == null 
								&& (f.getUuid().equals(IQueryHibernateManager.CA_QUERY_KEY))){
							return f;
						}else if (((QueryFolder) element).getEmployee() != null && f.getUuid().equals(IQueryHibernateManager.USER_QUERY_KEY)){
							return f;
						}
					}
				}
			}
			return parent;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ITreeContentProvider#hasChildren(java.lang.Object)
	 */
	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof Query){
			return false;
		}else if (element instanceof QueryFolder){
			QueryFolder folder = (QueryFolder)element;
			if (folder.getChildren() != null && folder.getChildren().size() > 0){
				return true;
			}
			if (includeQueries){
				List<QueryEditorInput> q = queries.get( folder.getUuid() );
				if ( q != null && q.size() > 0){
					return true;
				}
			}
		}
		return false;
	}
	
}

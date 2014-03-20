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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
/**
 * Content provider for a list of tree nodes
 * @author Emily
 *
 */
public class ItemTreeNodeContentProvider implements ITreeContentProvider {

	private List<IItemTreeNode> roots;
	private String msg = null;
	
	public ItemTreeNodeContentProvider(List<IItemTreeNode> roots){
		this.roots = roots;
	}
	
	@Override
	public void dispose() {
		for (IItemTreeNode n : roots){
			n.getContentProvider().dispose();
		}
	}

	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		msg = null;
		if (newInput == null){
			for (IItemTreeNode n : roots){
				n.getContentProvider().inputChanged(viewer, oldInput, newInput);
			}
		}else if (newInput instanceof String){
			msg = (String) newInput;
		}else{
			Map<String, Object> keys = (Map<String, Object>) newInput;
			
			for (IItemTreeNode n : roots){
				n.getContentProvider().inputChanged(viewer, oldInput, keys.get(n.getKey()));
			}
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		if (msg == null){
			return roots.toArray(new IItemTreeNode[roots.size()]);
		}else{
			return new String[]{msg};
		}
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		Object[] kids = null;
		IItemTreeNode parent = null;
		if (parentElement instanceof IItemTreeNode){
			kids = ((IItemTreeNode)parentElement).getContentProvider().getElements(null);
			parent = (IItemTreeNode) parentElement;
		}else if (parentElement instanceof WrappedTreeNode){
			kids = ((WrappedTreeNode)parentElement).parent.getContentProvider().getChildren(((WrappedTreeNode)parentElement).item);
			parent = ((WrappedTreeNode)parentElement).parent;
		}
		
		if (kids != null){
			Object[] wrappedkids = new Object[kids.length];
			for (int i = 0; i < kids.length; i ++){
				wrappedkids[i] = new WrappedTreeNode(parent, kids[i]);
			}
			return wrappedkids;
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IItemTreeNode){
			return null;
		}else if (element instanceof WrappedTreeNode){
			Object p = ((WrappedTreeNode) element).parent.getContentProvider().getParent(element);
			if (p == null){
				return ((WrappedTreeNode) element).parent;
			}else{
				return new WrappedTreeNode(((WrappedTreeNode) element).parent, p);
			}			
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (msg != null) return false;
		if (element instanceof IItemTreeNode){
			return true;
		}else if (element instanceof WrappedTreeNode){
			return ((WrappedTreeNode) element).parent.getContentProvider().hasChildren(((WrappedTreeNode) element).item);
		}
		return getChildren(element).length > 0;
	}
		
	
	public LabelProvider getLabelProvider(){
		return LBLPROVIDER;
	}
		
	public static IStructuredSelection unwrapSelection(IStructuredSelection selection){
		List<Object> items = new ArrayList<Object>();
		
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object object = (Object) iterator.next();
			if (object instanceof WrappedTreeNode){
				items.add( ((WrappedTreeNode) object).item );
			}else{
				items.add(object);
			}
		}
		
		return new StructuredSelection(items);
		
	}

	
	private static final LabelProvider LBLPROVIDER =  new LabelProvider(){
		
		/**
		 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
		 */
		@Override 
		public Image getImage(Object element) {
			if (element instanceof IItemTreeNode){
				return ((IItemTreeNode) element).getImage();
			}else if (element instanceof WrappedTreeNode){
				return ((WrappedTreeNode) element).parent.getLabelProvider().getImage(((WrappedTreeNode) element).item);
			}
			return super.getImage(element);
			
		}
		
		
		/**
		 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
		 */
		@Override
		public String getText(Object element) {
			if (element instanceof IItemTreeNode){
				return ((IItemTreeNode) element).getName();
			}else if (element instanceof WrappedTreeNode){
				return ((WrappedTreeNode) element).parent.getLabelProvider().getText(((WrappedTreeNode) element).item);
			}
			return super.getText(element);
			
		}
	};
}

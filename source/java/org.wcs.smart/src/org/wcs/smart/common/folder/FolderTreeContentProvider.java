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
package org.wcs.smart.common.folder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.ui.properties.DialogConstants;

/**
 * Generic content provider for folder tree like structures.
 * It allows to create a tree structure having various elements at each level
 * of the tree by wrapping elements in containers that covers all the logic required
 * to maintain dependencies required for the tree.
 *
 * @author elitvin
 * @since 6.0.0
 */
public class FolderTreeContentProvider implements ITreeContentProvider {
	
	//original input
	private Object input;
	//root tree elements
	private List<ITreeElement> treeRoot;
	private Map<Object, ITreeElement> obj2TreeElement = new HashMap<>();
	
	private Viewer viewer;
	private IGroupContentBuilder groupContentBuilder = new NoGroupingContentBuilder();
	
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		this.viewer = viewer;
		input = newInput;		
		applyCurrentGrouping(true);
	}
	
	public void applyCurrentGrouping() {
		applyGrouping(groupContentBuilder, false);
	}

	private void applyCurrentGrouping(boolean expend) {
		applyGrouping(groupContentBuilder, expend);
	}

	private synchronized void applyGrouping(IGroupContentBuilder gcb, boolean expend) {
		treeRoot = null;
		if (gcb != null) {
			List<ITreeElement> roots = gcb.applyGrouping(input);
			Map<Object, ITreeElement> mapping = new HashMap<>();
			fillMapping(mapping, roots);
			obj2TreeElement = mapping;
			treeRoot = roots;
			if (input == null) return;
			
			Display.getDefault().syncExec(()->{
				if (viewer != null) {
					viewer.refresh();
					if (expend) {
						((TreeViewer)viewer).expandAll();
					}
				}
			});
		}
	}
	
	public void updateGroupContentBuilder(IGroupContentBuilder groupContentBuilder) {
		Assert.isNotNull(groupContentBuilder);
		this.groupContentBuilder = groupContentBuilder;
		applyCurrentGrouping(true);
	}
	
	private void fillMapping(Map<Object, ITreeElement> mapping, List<ITreeElement> elements) {
		if (elements == null) {
			return;
		}
		for (ITreeElement te : elements) {
			mapping.put(te.getObject(), te);
			fillMapping(mapping, te.getChildren());
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		List<ITreeElement> currentRoot = treeRoot;
		if (currentRoot == null) {
			return new Object[]{DialogConstants.LOADING_TEXT};
		}
		return currentRoot.stream().map(e -> e.getObject()).toArray();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		ITreeElement te = obj2TreeElement.get(parentElement);
		return te != null && te.getChildren() != null ? te.getChildren().stream().map(e -> e.getObject()).toArray() : null;
	}

	@Override
	public Object getParent(Object element) {
		ITreeElement te = obj2TreeElement.get(element);
		return te != null ? te.getParent() : null;
	}

	@Override
	public boolean hasChildren(Object element) {
		ITreeElement te = obj2TreeElement.get(element);
		return te != null && te.getChildren() != null && te.getChildren().size() > 0;
	}

}

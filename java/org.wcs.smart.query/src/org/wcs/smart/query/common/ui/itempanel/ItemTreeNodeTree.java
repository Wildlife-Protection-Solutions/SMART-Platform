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

import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.query.internal.Messages;

/**
 * Tree Viewer for query item panel that displays a set of
 * ItemTreeNodes.
 * 
 * @author Emily
 *
 */
public class ItemTreeNodeTree extends Composite {

	private List<IItemTreeNode> nodes;
	private List<IItemTreeNode> values;
	
	private TreeViewer filterTreeViewer;

	/**
	 * Creates a new tree node with a single set of filters.
	 * 
	 * @param parent
	 * @param style
	 * @param nodes
	 */
	public ItemTreeNodeTree(Composite parent, int style,
			List<IItemTreeNode> nodes) {
		super(parent, style);
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.nodes = nodes;

		createTree();
	}
	
	/**
	 * Creates a new tree with two sets of filter; one for
	 * groupby elements and one for value elements
	 * 
	 * @param parent
	 * @param style
	 * @param groupbys
	 * @param values
	 */
	public ItemTreeNodeTree(Composite parent, int style,
			List<IItemTreeNode> groupbys, List<IItemTreeNode> values) {
		super(parent, style);
		setLayout(new GridLayout());
		((GridLayout)getLayout()).marginWidth = 0;
		((GridLayout)getLayout()).marginHeight = 0;
		
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		this.nodes = groupbys;
		this.values = values;

		createTree();
	}
	
	public TreeViewer getTreeViewer(){
		return this.filterTreeViewer;
	}

	private void createTree() {

		// search tree
		final PatternFilter patternFilter = new PatternFilter() {
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((ITreeContentProvider) ((TreeViewer) viewer)
						.getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(
							viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((LabelProvider) ((TreeViewer) viewer)
						.getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,
						element));
			}
		};

		FilteredTree fTree = new FilteredTree(this, SWT.H_SCROLL | SWT.V_SCROLL
				| SWT.MULTI, patternFilter, true);
		fTree.setBackground(fTree.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		filterTreeViewer = fTree.getViewer();
		filterTreeViewer.getTree().setLayoutData(
				new GridData(SWT.FILL, SWT.FILL, true, true));
		
		if (this.values == null){
			ItemTreeNodeContentProvider provider = new ItemTreeNodeContentProvider(this.nodes);
			filterTreeViewer.setLabelProvider(provider.getLabelProvider());
			filterTreeViewer.setContentProvider(provider);
		}else{
			TreeNodeGroupValueContentProvider provider = new TreeNodeGroupValueContentProvider(this.nodes, this.values);
			filterTreeViewer.setLabelProvider(provider.getLabelProvider());
			filterTreeViewer.setContentProvider(provider);
		}

		filterTreeViewer.setAutoExpandLevel(2);
		filterTreeViewer.setInput(Messages.ItemTreeNodeTree_Loading);

	}

}

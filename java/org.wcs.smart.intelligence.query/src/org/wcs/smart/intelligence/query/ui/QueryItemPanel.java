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
package org.wcs.smart.intelligence.query.ui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.intelligence.IntelligencePlugIn;
import org.wcs.smart.intelligence.query.filter.IntelligenceFilterOption;
import org.wcs.smart.intelligence.query.internal.Messages;
import org.wcs.smart.query.common.ui.itempanel.IItemTreeNode;
import org.wcs.smart.query.common.ui.itempanel.ItemTreeNodeContentProvider;
import org.wcs.smart.query.common.ui.itempanel.ItemTreeNodeTree;
import org.wcs.smart.query.common.ui.itempanel.OperatorsTreeNode;
import org.wcs.smart.query.model.filter.Operator;
import org.wcs.smart.query.ui.itempanel.AbstractQueryItemPanel;

/**
 * Item panel for displaying options for intelligence filters.
 * 
 * @author Emily
 *
 */
public class QueryItemPanel extends AbstractQueryItemPanel {

	public static final String ID = "org.wcs.smart.intelligence.query.record.items"; //$NON-NLS-1$
	
	private static final String INTEL_NODE_KEY = "intelligence"; //$NON-NLS-1$

	private TreeViewer filterTreeViewer;
	
	
	public QueryItemPanel() {
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public Composite getComposite(Composite parent) {
		//search tree
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout();
		gl.marginWidth = gl.marginHeight = 0;
		main.setLayout(gl);
		
	
		List<IItemTreeNode> nodes = new ArrayList<IItemTreeNode>();
		
		IItemTreeNode intellNode = new IItemTreeNode() {
			
			LabelProvider labelProvider = new LabelProvider(){
				@Override
				public String getText(Object element){
					if (element instanceof String){
						return (String)element;
					}else if (element instanceof IntelligenceFilterOption){
						return ((IntelligenceFilterOption) element).getGuiName();
					}
					return super.getText(element);
				}
				@Override
				public Image getImage(Object element){
					if (element instanceof IntelligenceFilterOption){
						return ((IntelligenceFilterOption)element).getImage();
					}
					return super.getImage(element);
				}
			};
			
			ITreeContentProvider dataProvider = new ITreeContentProvider() {
				
				Object[] data = null;
				@Override
				public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
					if (newInput instanceof Object[]){
						data = (Object[]) newInput;
					}
				}
				
				@Override
				public void dispose() {	
				}
				
				@Override
				public boolean hasChildren(Object element) {
					return false;
				}
				
				@Override
				public Object getParent(Object element) {
					return null;
				}
				
				@Override
				public Object[] getElements(Object inputElement) {
					return data;
				}
				
				@Override
				public Object[] getChildren(Object parentElement) {
					return null;
				}
			};
			
			@Override
			public String getName() {
				return Messages.QueryItemPanel_IntelligenceNodeName;
			}
			
			@Override
			public ILabelProvider getLabelProvider() {
				return labelProvider;
			}
			
			@Override
			public String getKey() {
				return INTEL_NODE_KEY;
			}
			
			@Override
			public Image getImage() {
				return IntelligencePlugIn.getDefault().getImageRegistry().get(IntelligencePlugIn.INTELLIGENCE_ICON);
			}
			
			@Override
			public ITreeContentProvider getContentProvider() {
				return dataProvider;
			}
		};
		nodes.add(intellNode);
		nodes.add(new OperatorsTreeNode());
		ItemTreeNodeTree tree = new ItemTreeNodeTree(main, SWT.NONE, nodes);
		
		filterTreeViewer = tree.getTreeViewer();

		filterTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addItem();
			}
		});
		filterTreeViewer.setAutoExpandLevel(2);
		
		final HashMap<Object, Object> input = new HashMap<Object, Object> ();
		input.put(INTEL_NODE_KEY, IntelligenceFilterOption.values());
		input.put(OperatorsTreeNode.KEY, new Operator[]{Operator.NOT, Operator.BRACKETS});
		filterTreeViewer.setInput(input);
		
		Button btnAdd = new Button(main, SWT.PUSH);
		btnAdd.setText(Messages.QueryItemPanel_AddToQueryButton);
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addItem();
			}
		});
		refreshPanel();
		
		return main;
	}


	@Override
	public void refreshPanel() {
	}

	private void addItem(){
		addQueryItem( ItemTreeNodeContentProvider.unwrapSelection((IStructuredSelection) filterTreeViewer.getSelection()));
	}
}

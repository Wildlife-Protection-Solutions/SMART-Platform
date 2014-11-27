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
package org.wcs.smart.query.ui;

import java.util.List;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;

/**
 * Tree viewer that displays a tree in a box similar
 * to a combo box.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class TreeDropDownViewer {

	private TreeViewer attributeTreeViewer;
	private Composite main;
	private ISelectionListener onSelected;
	private PatternFilter patternFilter;
	private LocalFilteredTree fTree;
	
	
	/**
	 * Creates a new tree drop down viewer 
	 * @param parent outer shell
	 */
	public TreeDropDownViewer(Shell parent){
		
		main = new Shell(parent, SWT.SINGLE |SWT.BORDER | SWT.RESIZE);
		
		// close dialog if user selects outside of the shell
		main.addListener(SWT.Deactivate, new Listener() {
			public void handleEvent(Event e){
				hide();
			}
		});
		
		main.addListener(SWT.Traverse, new Listener(){
			@Override
			public void handleEvent(Event event) {
				if (event.detail == SWT.TRAVERSE_ESCAPE){
					event.doit = false;
					hide();
				}
			}});
		
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		main.setLayout(gl);
		createComposite(main);
	}
	
	
	/**
	 * Sets the current datamodel attribute to display
	 * in the tree.
	 * 
	 * @param att
	 */
	public void setAttribute(List<AttributeTreeNode> rootNodes){
		attributeTreeViewer.setInput(rootNodes);
		attributeTreeViewer.expandToLevel(1);
		attributeTreeViewer.refresh();
	}
	
	private void createComposite(Composite parent){
		
		patternFilter = new PatternFilter() {
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((ITreeContentProvider) ((TreeViewer) viewer).getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(
							viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((LabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
			}

		};
		fTree = new LocalFilteredTree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, patternFilter, true);
		
		fTree.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		attributeTreeViewer = fTree.getViewer();
		//attributeTreeViewer = new TreeViewer(main, SWT.H_SCROLL | SWT.V_SCROLL);
		attributeTreeViewer.setContentProvider(new AttributeTreeContentProvider(true, false));
		attributeTreeViewer.setLabelProvider(new AttributeTreeLabelProvider());
		
		attributeTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		attributeTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				fireSelection();
				hide();
			}
		});
	}
	
	/**
	 * Moves the shell to the required location and displays the tree.
	 * 
	 * @param obj the parent object (used for positioning)
	 * @param onSelected selection listener to fire when item from tree is selected
	 */
	public void positionAndShow(Composite obj, ISelectionListener onSelected){
		fTree.clearText();
		this.onSelected = onSelected;
		Rectangle r = obj.getBounds();
		Point pnt = obj.getParent().toDisplay(r.x, r.y);
		
		main.setBounds(pnt.x + 25, pnt.y + r.height, 200, 150);
		main.setVisible(true);
		
		attributeTreeViewer.getTree().setFocus();
	}
	
	/**
	 * Fires selection listener
	 */
	private void fireSelection(){
		onSelected.selectionChanged(null, attributeTreeViewer.getSelection());
	}
	
	/**
	 * Hides the dialog
	 */
	public void hide(){
		main.setVisible(false);
	}
	
	class LocalFilteredTree extends FilteredTree{
		/**
		 * @param parent
		 */
		protected LocalFilteredTree(Composite parent, int style, PatternFilter patternFilter, boolean newLook) {
			super(parent, style, patternFilter, newLook);
		}

		public void clearText(){
			super.clearText();
		}
	}
	
}

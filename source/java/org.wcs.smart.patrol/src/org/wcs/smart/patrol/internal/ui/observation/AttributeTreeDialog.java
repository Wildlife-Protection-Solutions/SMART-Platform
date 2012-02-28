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
package org.wcs.smart.patrol.internal.ui.observation;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.FilteredTree;
import org.eclipse.ui.dialogs.PatternFilter;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;

/**
 * Dialog for display attribute tree.
 * @author Emily
 * @since 1.0.0
 */
public class AttributeTreeDialog extends TitleAreaDialog {

	private TreeViewer tblTree;
	private Attribute attribute;
	
	private Object currentSelection;
	
	/**
	 * @param parentShell
	 */
	public AttributeTreeDialog(Shell parentShell, Attribute attribute) {
		super(parentShell);
		this.attribute = attribute;
		
		
	}
	
	@Override
	public Control createDialogArea(Composite parent){
		Composite composite = (Composite)super.createDialogArea(parent);
		
		Composite main = new Composite(composite, SWT.NONE);
		main.setLayout(new GridLayout(2, false));
		main.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		PatternFilter patternFilter = new PatternFilter(){			
			protected boolean isChildMatch(Viewer viewer, Object element) {
				Object parent = ((AttributeTreeContentProvider)((TreeViewer)viewer).getContentProvider()).getParent(element);
				if (parent != null) {
					return (isLeafMatch(viewer, parent) ? true : isChildMatch(viewer, parent));
				}
				return false;
			}

			@Override
			protected boolean isLeafMatch(Viewer viewer, Object element) {
				String labelText = ((AttributeTreeLabelProvider) ((TreeViewer) viewer).getLabelProvider()).getText(element);
				if (labelText == null) {
					return false;
				}
				return (wordMatches(labelText) ? true : isChildMatch(viewer,element));
			}
			
		};
		
		
		FilteredTree fTree = new FilteredTree(main, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL, patternFilter, true);
		tblTree = fTree.getViewer();
		tblTree.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		tblTree.setContentProvider(new AttributeTreeContentProvider());
		tblTree.setLabelProvider(new AttributeTreeLabelProvider());
		tblTree.setAutoExpandLevel(1);
		tblTree.setInput(attribute);
		tblTree.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				currentSelection = ((IStructuredSelection)tblTree.getSelection()).getFirstElement();
				
			}
		});
		
		setMessage("Select attribute value from tree.");
		return composite; 
	}

	public AttributeTreeNode getSelection(){
		if (currentSelection instanceof AttributeTreeNode){
			return (AttributeTreeNode)currentSelection;
		}
		return null;
	}

	protected boolean isResizable() {
		return true;
	}
}

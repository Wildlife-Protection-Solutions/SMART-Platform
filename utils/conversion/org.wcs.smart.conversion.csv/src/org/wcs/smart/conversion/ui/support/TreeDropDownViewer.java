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
package org.wcs.smart.conversion.ui.support;

import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IContentProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.TreeViewer;
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

/**
 * @author elitvin
 * @since 3.0.0
 */
public abstract class TreeDropDownViewer {

	private TreeViewer dmTreeViewer;
	private Composite main;
	private ISelectionListener onSelected;
	private DmTreePatternFilter patternFilter;
	private DmFilteredTree fTree;
	
	private IContentProvider contentProvider;
	private LangColumnLabelProvider labelProvider;
	
	/**
	 * Creates a new tree drop down viewer 
	 * @param parent outer shell
	 */
	public TreeDropDownViewer(Shell parent, IContentProvider contentProvider, LangColumnLabelProvider labelProvider) {
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
		
		main = new Shell(parent, SWT.SINGLE |SWT.BORDER | SWT.RESIZE);
		
		// close dialog if user selects outside of the shell
		main.addListener(SWT.Deactivate, new Listener() {
			public void handleEvent(Event e){
				hide();
			}
		});
		
		GridLayout gl = new GridLayout(1, false);
		gl.horizontalSpacing = 0;
		gl.verticalSpacing = 0;
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		main.setLayout(gl);
		createComposite(main);
	}
	
	
	
	private void createComposite(Composite parent) {
		
		patternFilter = new DmTreePatternFilter();
//		fTree = new LocalFilteredTree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, patternFilter, true);
		fTree = new DmFilteredTree(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI, patternFilter, true);
		
		fTree.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		dmTreeViewer = fTree.getViewer();
		//attributeTreeViewer = new TreeViewer(main, SWT.H_SCROLL | SWT.V_SCROLL);
		dmTreeViewer.setContentProvider(contentProvider);
		dmTreeViewer.setLabelProvider(labelProvider);
		
		dmTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		dmTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
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
		
		dmTreeViewer.getTree().setFocus();
	}
	
	/**
	 * Fires selection listener
	 */
	private void fireSelection(){
		onSelected.selectionChanged(null, dmTreeViewer.getSelection());
	}
	
	/**
	 * Hides the dialog
	 */
	public void hide(){
		main.setVisible(false);
	}

	protected TreeViewer getDmTreeViewer() {
		return dmTreeViewer;
	}
	
	public LangColumnLabelProvider getLabelProvider() {
		return labelProvider;
	}
}

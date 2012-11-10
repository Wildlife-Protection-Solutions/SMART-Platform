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
package org.wcs.smart.patrol.internal.ui.observation.field;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ui.properties.AttributeTreeContentProvider;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;

/**
 * Drop-down tree to support the tree attribute field.
 * <p>
 * This is adapted from the FilteredTree class.
 * </p>
 * @author egouge
 *
 */
public class TreeDropDown{

	private TreeViewer attributeTreeViewer;
	private Composite main;
	
	private ISelectionListener onSelected; //called when item in tree selected

	private PatternFilter patternFilter; 	//tree filter patter
	private String filterText = null;	//current filter text
	private boolean narrowingDown = false;	//if narrowing search
	private WorkbenchJob refreshJob = null;	//job to search tree itmes

	/**
	 * Creates a new tree drop down viewer 
	 * @param parent outer shell
	 */
	public TreeDropDown(Shell parent){
		
		main = new Shell(parent, SWT.BORDER  );
		
		// close dialog if user selects outside of the shell
		main.addListener(SWT.Deactivate, new Listener() {
			public void handleEvent(Event e){
//				if (isVisible()){
//					System.out.println("hide here");
//					hide();
//				}
			}
		});
		
//		// resize shell when list resizes
//		main.addControlListener(new ControlListener() {
//			public void controlMoved(ControlEvent e){}
//			public void controlResized(ControlEvent e){
//			}
//		});
		
		main.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_ESCAPE){
					//select nothing
					onSelected.selectionChanged(null, null);
				}
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
	
	public TreeViewer getTreeViewer(){
		return this.attributeTreeViewer;
	}
	/**
	 * Sets the current datamodel attribute to display
	 * in the tree.
	 * 
	 * @param att
	 */
	public void setAttribute(Attribute att){
		attributeTreeViewer.setInput(att);
		attributeTreeViewer.expandToLevel(2);
		attributeTreeViewer.refresh();
	}
	
	/*
	 * Creates the drop down tree control
	 * @param parent
	 */
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

		attributeTreeViewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		attributeTreeViewer.setFilters(new ViewerFilter[]{patternFilter});
		attributeTreeViewer.setContentProvider(new AttributeTreeContentProvider(true, false));
		attributeTreeViewer.setLabelProvider(new AttributeTreeLabelProvider());
		attributeTreeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		attributeTreeViewer.getTree().addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.keyCode == SWT.CR) {
					fireSelection();
					hide();
				}
			}
		});
//		attributeTreeViewer.getTree().addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseUp(MouseEvent e) {
//				fireSelection();
//				hide();	
//			}
//		});
		attributeTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
			@Override
			public void doubleClick(DoubleClickEvent event) {
				fireSelection();
				hide();
			}
		});
	}
	
	/**
	 * 
	 * @return true if drop down is visible
	 */
	public boolean isVisible(){
		if (!main.isDisposed()){
			return main.getVisible();
		}
		return false;
	}
	
	/**
	 * Moves the shell to the required location and displays the tree.
	 * 
	 * @param obj the parent object (used for positioning)
	 * @param onSelected selection listener to fire when item from tree is selected
	 */
	public void positionAndShow(Control obj, ISelectionListener onSelected){
		this.onSelected = onSelected;
		Rectangle r = obj.getBounds();
		Point pnt = obj.getParent().toDisplay(r.x, r.y);
		main.setBounds(pnt.x, pnt.y + r.height, r.width, 200);
		main.setVisible(true);
	}
	
	/**
	 * Updates the filter text and schedules the
	 * job to update the tree based on the new text.
	 * 
	 * @param text
	 */
	public void setText(String text){
		narrowingDown = filterText == null
				|| text.startsWith(filterText);
		this.filterText = text;
		if (refreshJob == null){
			refreshJob = doCreateRefreshJob();
			refreshJob.setSystem(true);
		}
		refreshJob.cancel();
		refreshJob.schedule(200);
	}
	
	/**
	 * 
	 * @return the current filter text
	 */
	public String getText(){
		if (filterText == null){
			return "";
		}
		return this.filterText;
	}
	
	/**
	 * Fires selection listener
	 */
	private void fireSelection(){
		onSelected.selectionChanged(null, attributeTreeViewer.getSelection());
	}
	
	/**
	 * 
	 * @return the current selection
	 */
	public IStructuredSelection getSelection(){
		return (IStructuredSelection) attributeTreeViewer.getSelection();
	}
	/**
	 * Hides the dialog
	 */
	public void hide(){
		main.setVisible(false);
	}
	
	/**
	 * Sets current focus on attribute tree.
	 */
	public void setFocus(){
		System.out.println("tree set focus");
		attributeTreeViewer.getTree().setFocus();
	}

	/**
	 * 
	 * @return <code>true</code> if at least one tree item
	 * is visible (matched the filter); false otherwise.
	 */
	public boolean treeItemsVisible(){
		return attributeTreeViewer.getTree().getItemCount() > 0;
	}
	/**
	 * Creates a job that refreshes the
	 * tree filter based on the filter text.
	 *  
	 * @return
	 */
	private WorkbenchJob doCreateRefreshJob() {
		return new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				if (attributeTreeViewer.getControl().isDisposed()) {
					return Status.CANCEL_STATUS;
				}

				
				if (filterText == null) {
					return Status.OK_STATUS;
				}
				patternFilter.setPattern(filterText);

				Control redrawFalseControl = attributeTreeViewer.getControl();
				try {
					// don't want the user to see updates that will be made to
					// the tree
					// we are setting redraw(false) on the composite to avoid
					// dancing scrollbar
					redrawFalseControl.setRedraw(false);
					if (!narrowingDown) {
						// collapse all
						TreeItem[] is = attributeTreeViewer.getTree().getItems();
						for (int i = 0; i < is.length; i++) {
							TreeItem item = is[i];
							if (item.getExpanded()) {
								attributeTreeViewer.setExpandedState(item.getData(),
										false);
							}
						}
					}
					attributeTreeViewer.refresh(true);

					if (filterText.length() > 0 ) {
						/*
						 * Expand elements one at a time. After each is
						 * expanded, check to see if the filter text has been
						 * modified. If it has, then cancel the refresh job so
						 * the user doesn't have to endure expansion of all the
						 * nodes.
						 */
						TreeItem[] items = attributeTreeViewer.getTree().getItems();
						int treeHeight = attributeTreeViewer.getTree().getBounds().height;
						int numVisibleItems = treeHeight
								/ attributeTreeViewer.getTree().getItemHeight();
						long stopTime = 200
								+ System.currentTimeMillis();
						boolean cancel = false;
						attributeTreeViewer.getTree().deselectAll();
						if (items.length > 0
								&& recursiveExpand(items, monitor, stopTime,
										new int[] { numVisibleItems })) {
							cancel = true;
						}

						if (cancel) {
							return Status.CANCEL_STATUS;
						}
					}
				} finally {
					// done updating the tree - set redraw back to true
					TreeItem[] items = attributeTreeViewer.getTree().getItems();
					if (items.length > 0
							&& attributeTreeViewer.getTree().getSelectionCount() == 0) {
						attributeTreeViewer.getTree().setTopItem(items[0]);
					}
					redrawFalseControl.setRedraw(true);
				}
				return Status.OK_STATUS;
			}

			/**
			 * Returns true if the job should be canceled (because of timeout or
			 * actual cancellation).
			 * 
			 * @param items
			 * @param monitor
			 * @param cancelTime
			 * @param numItemsLeft
			 * @return true if canceled
			 */
			private boolean recursiveExpand(TreeItem[] items,
					IProgressMonitor monitor, long cancelTime,
					int[] numItemsLeft) {
				boolean canceled = false;
				for (int i = 0; !canceled && i < items.length; i++) {
					TreeItem item = items[i];
					boolean visible = numItemsLeft[0]-- >= 0;
					if (monitor.isCanceled()
							|| (!visible && System.currentTimeMillis() > cancelTime)) {
						canceled = true;
					} else {
						Object itemData = item.getData();
						if (itemData != null) {
							if (!item.getExpanded()) {
								// do the expansion through the viewer so that
								// it can refresh children appropriately.
								attributeTreeViewer.setExpandedState(itemData, true);
							}
							TreeItem[] children = item.getItems();
							if (items.length > 0) {
								canceled = recursiveExpand(children, monitor,
										cancelTime, numItemsLeft);
							}
							if (children.length == 0){
								if (attributeTreeViewer.getTree().getSelectionCount() == 0){
									//select this item
									attributeTreeViewer.getTree().setSelection(item);
								}
							}
						}
					}
				}
				return canceled;
			}

		};
	}
	
}

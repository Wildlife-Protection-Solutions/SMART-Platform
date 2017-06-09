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
package org.wcs.smart.ui.properties;

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
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.progress.WorkbenchJob;

/**
 * A treeviewer wrapped in its own shell that can 
 * be displayed as a drop-down below another
 * ui field.
 * <p>
 * This is adapted from the FilteredTree class.
 * </p>
 * @author egouge
 *
 */
public class TreeDropDown{

	private TreeViewer treeViewer;
	private Composite main;
	private Control positionControl;
	private ISelectionListener onSelected; //called when item in tree selected

	private PatternFilter patternFilter; 	//tree filter patter
	private String filterText = null;	//current filter text
	private boolean narrowingDown = false;	//if narrowing search
	private WorkbenchJob refreshJob = null;	//job to search tree itmes

	private Listener moveListener = null;
	private Text filterTextBox = null;
	
	/**
	 * Creates a new tree drop down viewer 
	 * @param parent outer shell
	 */
	public TreeDropDown(Shell parent){
		main = new Shell(parent, SWT.SINGLE | SWT.BORDER | SWT.NO_FOCUS | SWT.NO_TRIM);

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
		
		moveListener = createMoveListener();
		parent.getShell().addListener(SWT.Move, moveListener);
	}
	
	private Listener createMoveListener() {
		Listener moveListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				if (isVisible()) {
					position(null, null);
				}
			}
		};
		return moveListener;
	}
	
	/**
	 * Disposes of the tree viewer
	 */
	public void dispose(){
		if (main.isDisposed()) return;
		if (moveListener != null && main != null){
			main.removeListener(SWT.Move, moveListener);
		}
		main.dispose();
		main = null;
	}
	
	public TreeViewer getTreeViewer(){
		return this.treeViewer;
	}

	public void setFilterTextBox(Text textBox){
		this.filterTextBox = textBox;
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

		treeViewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.SINGLE);
		treeViewer.setFilters(new ViewerFilter[]{patternFilter});
		
		treeViewer.getTree().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.getTree().addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.keyCode == SWT.CR) {
					fireSelection();
					hide();
				}else if (filterTextBox != null && (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS)){
					filterTextBox.setFocus();
					e.doit = false;
				}
			}
		});

		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
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
		if (main == null) return false;
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
	 * @param width <code>null</code> if width to be the same as the control object otherwise drop down width
	 * @param height <code>null</code> if height to be default
	 */
	public void positionAndShow(Control obj, Integer width, Integer height, ISelectionListener onSelected){
		this.positionControl = obj;
		this.onSelected = onSelected;
		Rectangle r = obj.getBounds();
		if (width == null){
			width = r.width;
		}
		if (height == null){
			height = 200;
		}
		position(width, height);
		
		main.setVisible(true);
	}
	
	private void position(Integer width, Integer height){
		if (width == null){
			width = main.getBounds().width;
		}
		if (height == null){
			height = main.getBounds().height;
		}
		Rectangle r = positionControl.getBounds();
		Point pnt = positionControl.getParent().toDisplay(r.x, r.y);
		main.setBounds(pnt.x, pnt.y + r.height, width, height);
		
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
			return ""; //$NON-NLS-1$
		}
		return this.filterText;
	}
	
	/**
	 * Fires selection listener
	 */
	private void fireSelection(){
		onSelected.selectionChanged(null, treeViewer.getSelection());
	}
	
	/**
	 * 
	 * @return the current selection
	 */
	public IStructuredSelection getSelection(){
		return (IStructuredSelection) treeViewer.getSelection();
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
		treeViewer.getTree().setFocus();
	}

	/**
	 * 
	 * @return <code>true</code> if at least one tree item
	 * is visible (matched the filter); false otherwise.
	 */
	public boolean treeItemsVisible(){
		return treeViewer.getTree().getItemCount() > 0;
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
				if (treeViewer.getControl().isDisposed()) {
					return Status.CANCEL_STATUS;
				}

				
				if (filterText == null) {
					return Status.OK_STATUS;
				}
				patternFilter.setPattern(filterText);

				Control redrawFalseControl = treeViewer.getControl();
				try {
					// don't want the user to see updates that will be made to
					// the tree
					// we are setting redraw(false) on the composite to avoid
					// dancing scrollbar
					redrawFalseControl.setRedraw(false);
					if (!narrowingDown) {
						// collapse all
						TreeItem[] is = treeViewer.getTree().getItems();
						for (int i = 0; i < is.length; i++) {
							TreeItem item = is[i];
							if (item.getExpanded()) {
								treeViewer.setExpandedState(item.getData(),
										false);
							}
						}
					}
					treeViewer.refresh(true);

					if (filterText.length() > 0 ) {
						/*
						 * Expand elements one at a time. After each is
						 * expanded, check to see if the filter text has been
						 * modified. If it has, then cancel the refresh job so
						 * the user doesn't have to endure expansion of all the
						 * nodes.
						 */
						TreeItem[] items = treeViewer.getTree().getItems();
						int treeHeight = treeViewer.getTree().getBounds().height;
						int numVisibleItems = treeHeight
								/ treeViewer.getTree().getItemHeight();
						long stopTime = 200
								+ System.currentTimeMillis();
						boolean cancel = false;
						treeViewer.getTree().deselectAll();
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
					TreeItem[] items = treeViewer.getTree().getItems();
					if (items.length > 0
							&& treeViewer.getTree().getSelectionCount() == 0) {
						treeViewer.getTree().setTopItem(items[0]);
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
								treeViewer.setExpandedState(itemData, true);
							}
							TreeItem[] children = item.getItems();
							if (items.length > 0) {
								canceled = recursiveExpand(children, monitor,
										cancelTime, numItemsLeft);
							}
							if (children.length == 0){
								if (treeViewer.getTree().getSelectionCount() == 0){
									//select this item
									treeViewer.getTree().setSelection(item);
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

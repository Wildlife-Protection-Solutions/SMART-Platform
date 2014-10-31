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
package org.wcs.smart.observation.ui.input;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PatternFilter;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.progress.WorkbenchJob;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.dataentry.model.CmNode;
import org.wcs.smart.observation.ObservationPlugIn;
import org.wcs.smart.observation.internal.Messages;

/**
 * This is a customized search tree for entering observation 
 * values.
 * <p>
 * It is based on a heavily modified version of the FilteredTree provided
 * by the eclipse framework.
 * </p>
 *   
 * 
 */
public class SearchTree extends Composite {

	private static final String INITIAL_TEXT = Messages.SearchTree_DefaultText;

	/**
	 * The filter text widget to be used by this tree. This value may be
	 * <code>null</code> if there is no filter widget, or if the controls have
	 * not yet been created.
	 */
	protected Text filterText;
	
	/**
	 * The control representing the clear button for the filter text entry. This
	 * value may be <code>null</code> if no such button exists, or if the
	 * controls have not yet been created.
	 */
	protected Control clearButtonControl;
	
	/**
	 * The viewer for the filtered tree. This value should never be
	 * <code>null</code> after the widget creation methods are complete.
	 */
	protected TreeViewer treeViewer;
	protected TreeViewer cmTreeViewer;

	/**
	 * The list viewer for the selected items. 
	 */
	protected ListViewer listViewer;
	
	/**
	 * A list to support the list viewer for selected itmes
	 */
	protected ArrayList<Category> selectedList;
	
	/**
	 * The Composite on which the filter controls are created. This is used to
	 * set the background color of the filter controls to match the surrounding
	 * controls.
	 */
	protected Composite filterComposite;

	/**
	 * The pattern filter for the tree. This value must not be <code>null</code>.
	 */
	private PatternFilter patternFilter;

	/**
	 * The text to initially show in the filter text control.
	 */
	protected String initialText = ""; //$NON-NLS-1$
	/**
	 * Maximum time spent expanding the tree after the filter text has been
	 * updated (this is only used if we were able to at least expand the visible
	 * nodes)
	 */
	private static final long SOFT_MAX_EXPAND_TIME = 200;
	/**
	 * The job used to refresh the tree.
	 */
	private Job refreshJob;

	/**
	 * The parent composite of the filtered tree.
	 * 
	 * @since 3.3
	 */
	protected Composite parent;


	/**
	 * @since 3.3
	 */
	protected Composite treeComposite;	
	
	private static Boolean useNativeSearchField = null;
	private String previousFilterText;
	private boolean narrowingDown;
	private List<IChangeListener> changeListener = new ArrayList<IChangeListener>();
	private TabFolder bits;
	
	/**
	 * Image descriptor for enabled clear button.
	 */
	private static final String CLEAR_ICON = "org.eclipse.ui.internal.dialogs.CLEAR_ICON"; //$NON-NLS-1$

	/**
	 * Image descriptor for disabled clear button.
	 */
	private static final String DISABLED_CLEAR_ICON= "org.eclipse.ui.internal.dialogs.DCLEAR_ICON"; //$NON-NLS-1$


	/**
	 * Get image descriptors for the clear button.
	 */
	static {
		ImageDescriptor descriptor = AbstractUIPlugin
				.imageDescriptorFromPlugin(PlatformUI.PLUGIN_ID,
						"$nl$/icons/full/etool16/clear_co.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			ObservationPlugIn.getDefault().getImageRegistry().put(CLEAR_ICON, descriptor);
		}
		descriptor = AbstractUIPlugin.imageDescriptorFromPlugin(
				PlatformUI.PLUGIN_ID, "$nl$/icons/full/dtool16/clear_co.gif"); //$NON-NLS-1$
		if (descriptor != null) {
			ObservationPlugIn.getDefault().getImageRegistry().put(DISABLED_CLEAR_ICON, descriptor);
		}
	}

	private boolean hasCmOption;
	
	/**
	 * Create a new instance of the receiver.
	 * 
	 * @param parent
	 *            the parent <code>Composite</code>
	 * @param treeStyle
	 *            the style bits for the <code>Tree</code>
	 * @param filter
	 *            the filter to be used
	 * @param useNewLook
	 *            <code>true</code> if the new 3.5 look should be used
	 * @param hasCmOption <code>true</code> if the search tree should have
	 * the ability to show a configurable model as well as the default model
	 * 
	 * @since 3.5
	 */
	public SearchTree(Composite parent, int treeStyle, PatternFilter filter, 
			boolean hasCmOption) {
		super(parent, SWT.NONE);
		this.parent = parent;
		this.hasCmOption = hasCmOption;
		if (!hasCmOption){
			treeStyle = treeStyle | SWT.BORDER;
		}
		init(treeStyle, filter);
	}
	
	/**
	 * Create the filtered tree.
	 * 
	 * @param treeStyle
	 *            the style bits for the <code>Tree</code>
	 * @param filter
	 *            the filter to be used
	 * 
	 * @since 3.3
	 */
	protected void init(int treeStyle, PatternFilter filter) {
		patternFilter = filter;
	
		createControl(parent, treeStyle);
		createRefreshJob();
		setInitialText(INITIAL_TEXT);
		setFont(parent.getFont());
	}

	/**
	 * Create the filtered tree's controls. Subclasses should override.
	 * 
	 * @param parent
	 * @param treeStyle
	 */
	protected void createControl(Composite parent, int treeStyle) {
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (useNativeSearchField(parent)) {
			filterComposite= new Composite(this, SWT.NONE);
		} else {
			filterComposite= new Composite(this, SWT.BORDER);
			filterComposite.setBackground(getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
		}
		
		GridLayout filterLayout= new GridLayout(2, false);
		filterLayout.marginHeight= 0;
		filterLayout.marginWidth= 0;
		filterComposite.setLayout(filterLayout);
		filterComposite.setFont(parent.getFont());

		createFilterControls(filterComposite);
		filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));

		treeComposite = new Composite(this, SWT.NONE);
		GridLayout treeCompositeLayout = new GridLayout();
		treeCompositeLayout.marginHeight = 0;
		treeCompositeLayout.marginWidth = 0;
		treeComposite.setLayout(treeCompositeLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeComposite.setLayoutData(data);
		createTreeControl(treeComposite, treeStyle);
	}
	

	private static boolean useNativeSearchField(Composite composite) {
		if (useNativeSearchField == null) {
			useNativeSearchField = Boolean.FALSE;
			Text testText = null;
			try {
				testText = new Text(composite, SWT.SEARCH | SWT.ICON_CANCEL);
				useNativeSearchField = new Boolean((testText.getStyle() & SWT.ICON_CANCEL) != 0);
			} finally {
				if (testText != null) {
					testText.dispose();
				}
			}
				
		}
		return useNativeSearchField.booleanValue();
	}
	

	/**
	 * Create the filter controls. By default, a text and corresponding tool bar
	 * button that clears the contents of the text is created. Subclasses may
	 * override.
	 * 
	 * @param parent
	 *            parent <code>Composite</code> of the filter controls
	 * @return the <code>Composite</code> that contains the filter controls
	 */
	protected Composite createFilterControls(Composite parent) {
		createFilterText(parent);
		createClearTextNew(parent);
		if (clearButtonControl != null) {
			// initially there is no text to clear
			clearButtonControl.setVisible(false);
		}
		
		return parent;
	}

	/**
	 * Creates and set up the tree and tree viewer. This method calls
	 * {@link #doCreateTreeViewer(Composite, int)} to create the tree viewer.
	 * Subclasses should override {@link #doCreateTreeViewer(Composite, int)}
	 * instead of overriding this method.
	 * 
	 * @param parent
	 *            parent <code>Composite</code>
	 * @param style
	 *            SWT style bits used to create the tree
	 * @return the tree
	 */
	protected Control createTreeControl(Composite parent, int style) {
		final Composite treeComp = new Composite(parent, SWT.NONE);
		treeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite dmTreeComp = treeComp;
		Composite cmTreeComp = treeComp;
		
		if (hasCmOption){
			bits = new TabFolder(treeComp, SWT.NONE);
		
			TabItem dmItem = new TabItem(bits, SWT.NONE);
			dmItem.setText(Messages.CmSearchTree_DataModelOption);
		
			dmTreeComp = new Composite(bits, SWT.NONE);
			GridLayout gl = new GridLayout();
			gl.marginWidth = gl.marginHeight = 0;
			dmTreeComp.setLayout(gl);
			dmItem.setControl(dmTreeComp);
		
			TabItem cmItem = new TabItem(bits, SWT.NONE);
			cmItem.setText(Messages.CmSearchTree_ConfigurableModelOption);
			bits.setSelection(1);
			cmTreeComp = new Composite(bits, SWT.NONE);
			gl = new GridLayout();
			gl.marginWidth = gl.marginHeight = 0;
			cmTreeComp.setLayout(gl);
			cmItem.setControl(cmTreeComp);
		}
		
		treeViewer = doCreateTreeViewer(dmTreeComp, style);
		treeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		treeViewer.getControl().addDisposeListener(new DisposeListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			 */
			public void widgetDisposed(DisposeEvent e) {
				refreshJob.cancel();
			}
		});
		treeViewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(DoubleClickEvent event) {
				addSelectionToList();
			}
		});
		treeViewer.getTree().addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if (e.keyCode == SWT.CR){
					addSelectionToList();
					e.doit = false;
				}
				
			}
		});
		treeViewer.addFilter(patternFilter);
		
		if (hasCmOption){
			cmTreeViewer = doCreateTreeViewer(cmTreeComp, style);
			cmTreeViewer.getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			cmTreeViewer.getControl().addDisposeListener(new DisposeListener() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
				 */
				public void widgetDisposed(DisposeEvent e) {
					refreshJob.cancel();
				}
			});
			cmTreeViewer.addDoubleClickListener(new IDoubleClickListener() {
				
				@Override
				public void doubleClick(DoubleClickEvent event) {
					addSelectionToList();
				}
			});
			cmTreeViewer.getTree().addTraverseListener(new TraverseListener() {
				@Override
				public void keyTraversed(TraverseEvent e) {
					if (e.keyCode == SWT.CR){
						addSelectionToList();
						e.doit = false;
					}
					
				}
			});
			cmTreeViewer.addFilter(patternFilter);
		}
		createButtonPanel(treeComp);
		
		selectedList = new ArrayList<Category>();
		listViewer = doCreateListViewer(treeComp, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);

		treeComp.addListener(SWT.Resize, new Listener(){

			@Override
			public void handleEvent(Event event) {
				Rectangle compSize = treeComp.getClientArea();
				int width = compSize.width;
				int height = compSize.height;
				Point btnSize = treeComp.getChildren()[1].computeSize(SWT.DEFAULT, SWT.DEFAULT);
				
				int columnWidth = (width - btnSize.x) / 2;
				
				treeComp.getChildren()[0].setBounds(0, 0, columnWidth, height);
				treeComp.getChildren()[1].setBounds(columnWidth, (height - btnSize.y) / 2, btnSize.x, height);
				treeComp.getChildren()[2].setBounds(columnWidth + btnSize.x, 0, columnWidth, height);
				
			}});
		
		return treeViewer.getControl();
	}
	
	private void createButtonPanel(Composite parent){	
		Composite buttonPnl = new Composite(parent, SWT.NONE);
		buttonPnl.setLayout(new GridLayout(1, false));
//		buttonPnl.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, true));
		
		Button btnAdd = new Button(buttonPnl, SWT.PUSH);
		btnAdd.setText(" > "); //$NON-NLS-1$
		btnAdd.setToolTipText(Messages.SearchTree_AddCategories_ToolTip);
		btnAdd.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnAdd.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				addSelectionToList();
			}
		});

		Button btnRemove = new Button(buttonPnl, SWT.PUSH);
		btnRemove.setText(" < "); //$NON-NLS-1$
		btnRemove.setToolTipText(Messages.SearchTree_RemoveCategories_ToolTip);
		btnRemove.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnRemove.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				IStructuredSelection toRemove = (IStructuredSelection) listViewer.getSelection();
				for (Iterator<?> iterator = toRemove.iterator(); iterator.hasNext();) {
					Object type = (Object) iterator.next();
					selectedList.remove(type);
				}
				listViewer.refresh();
				fireListChanged();
			}
		});
		Button btnRemoveAll = new Button(buttonPnl, SWT.PUSH);
		btnRemoveAll.setText(" << "); //$NON-NLS-1$
		btnRemoveAll.setToolTipText(Messages.SearchTree_RemoveAllCategories_Tooltip);
		btnRemoveAll.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false));
		btnRemoveAll.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				selectedList.clear();
				
				listViewer.refresh();
				fireListChanged();
			}
		});
		
	}
	
	/**
	 * Creates the list viewer and sets up label
	 * and content provider
	 * 
	 * @param parent
	 * @param style
	 * @return
	 */
	private ListViewer doCreateListViewer(Composite parent, int style){
		ListViewer listViewer  = new ListViewer(parent, style);
		listViewer.setLabelProvider(new LabelProvider(){
			@Override
			public String getText(Object element){
				if (element instanceof Category){
					String text = ((Category)element).getName();
					if (((Category) element).getParent() != null){
						text = text + "  (" + ((Category)element).getParent().getFullCategoryName() + ")"; //$NON-NLS-1$ //$NON-NLS-2$
					}
					return text;
				}
				return super.getText(element);
			}
		});
		
		listViewer.setContentProvider(ArrayContentProvider.getInstance());
		
		listViewer.setInput(selectedList);
		return listViewer;
	}

	/**
	 * Creates the tree viewer. 
	 * @since 3.3
	 */
	protected TreeViewer doCreateTreeViewer(Composite parent, int style) {
		return new TreeViewer(parent, style);
	}


	/**
	 * Create the refresh job for the receiver.
	 * 
	 */
	private void createRefreshJob() {
		refreshJob = doCreateRefreshJob();
		refreshJob.setSystem(true);
	}

	/**
	 * Creates a workbench job that will refresh the tree based on the current filter text.
	 * Subclasses may override.
	 * 
	 * @return a workbench job that can be scheduled to refresh the tree
	 * 
	 * @since 3.4
	 */
	protected WorkbenchJob doCreateRefreshJob() {
		return new WorkbenchJob("Refresh Filter") {//$NON-NLS-1$
			public IStatus runInUIThread(IProgressMonitor monitor) {
				TreeViewer[] viewers = new TreeViewer[]{treeViewer, cmTreeViewer};
				
				if (treeViewer.getControl().isDisposed()) {
					return Status.CANCEL_STATUS;
				}

				String text = getFilterString();
				if (text == null) {
					return Status.OK_STATUS;
				}

				boolean initial = initialText != null && initialText.equals(text);
				if (initial) {
					patternFilter.setPattern(null);
				} else if (text != null) {
					patternFilter.setPattern(text);
				}
				if (text.length() == 0){
					for (TreeViewer viewer: viewers){
						if (viewer == null) continue;
						viewer.getControl().setRedraw(false);
						if (!narrowingDown) {
							// collapse all
							TreeItem[] is = viewer.getTree().getItems();
							for (int i = 0; i < is.length; i++) {
								TreeItem item = is[i];
								if (item.getExpanded()) {
									viewer.setExpandedState(item.getData(),false);
								}
							}
						}
						viewer.refresh(true);
						viewer.getControl().setRedraw(true);
						viewer.expandToLevel(3);
					}
					
					return Status.OK_STATUS;
				}

				for (TreeViewer viewer : viewers){
					if (viewer == null) continue;
					
					List<Object> matched = new ArrayList<Object>();
					recursiveFindMatched(viewer, viewer.getInput(), matched);
	
					if (monitor.isCanceled()){
						return Status.CANCEL_STATUS;
					}
					if (matched.size() > 0){
						if (monitor.isCanceled()){
							return Status.CANCEL_STATUS;
						}
						IStructuredSelection sel = new StructuredSelection(matched);
						viewer.setSelection(sel, true);
					}else{
						viewer.setSelection(null);
					}
					
					Control redrawFalseControl = treeComposite != null ? treeComposite : viewer.getControl();
					try {
						// don't want the user to see updates that will be made to
						// the tree
						// we are setting redraw(false) on the composite to avoid
						// dancing scrollbar
						redrawFalseControl.setRedraw(false);
						if (!narrowingDown) {
							// collapse all
							TreeItem[] is = viewer.getTree().getItems();
							for (int i = 0; i < is.length; i++) {
								TreeItem item = is[i];
								if (item.getExpanded()) {
									viewer.setExpandedState(item.getData(),false);
								}
							}
						}
						viewer.refresh(true);
	
						if (text.length() > 0 && !initial) {
							/*
							 * Expand elements one at a time. After each is
							 * expanded, check to see if the filter text has been
							 * modified. If it has, then cancel the refresh job so
							 * the user doesn't have to endure expansion of all the
							 * nodes.
							 */
							TreeItem[] items = viewer.getTree().getItems();
							int treeHeight = viewer.getTree().getBounds().height;
							int numVisibleItems = treeHeight / viewer.getTree().getItemHeight();
							long stopTime = SOFT_MAX_EXPAND_TIME + System.currentTimeMillis();
							boolean cancel = false;
							if (items.length > 0
									&& recursiveExpand(viewer, items, monitor, stopTime, new int[] { numVisibleItems })) {
								cancel = true;
							}
	
							// enabled toolbar - there is text to clear
							// and the list is currently being filtered
							updateToolbar(true);
							
							if (cancel) {
								return Status.CANCEL_STATUS;
							}
						} else {
							// disabled toolbar - there is no text to clear
							// and the list is currently not filtered
							updateToolbar(false);
						}
					} finally {
						// done updating the tree - set redraw back to true
						TreeItem[] items = viewer.getTree().getItems();
						if (items.length > 0
								&& viewer.getTree().getSelectionCount() == 0) {
							viewer.getTree().setTopItem(items[0]);
						}
						redrawFalseControl.setRedraw(true);
					}
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
			private boolean recursiveExpand(TreeViewer viewer, TreeItem[] items,
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
								viewer.setExpandedState(itemData, true);
							}
							TreeItem[] children = item.getItems();
							if (items.length > 0) {
								canceled = recursiveExpand(viewer, children, monitor,
										cancelTime, numItemsLeft);
							}
						}
					}
				}
				return canceled;
			}

		};
	}

	private void recursiveFindMatched(TreeViewer viewer, Object root, List<Object> items){
		if (getFilterString() == null || getFilterString().length() == 0 || getFilterString().equals(getInitialText())) return;
		
		if (root == viewer.getInput()){
			Object[] kids = ((ITreeContentProvider)viewer.getContentProvider()).getElements(root);
			for (int i = 0; i < kids.length; i ++){
				recursiveFindMatched(viewer, kids[i], items);
			}
			return;
		}
		
		boolean amIVisisble = patternFilter.isElementVisible(viewer, root);
		if (amIVisisble){
			Object[] kids = ((ITreeContentProvider)viewer.getContentProvider()).getChildren(root);
			boolean isKidVisible = false;
			if (kids != null){
				for (int i = 0; i < kids.length; i ++){
					if (patternFilter.isElementVisible(viewer, kids[i])){
						isKidVisible = true;
						recursiveFindMatched(viewer, kids[i], items);
					}
				}
			}
			if (!isKidVisible){
				items.add(root);
			}
		}

	}
	protected void updateToolbar(boolean visible) {
		if (clearButtonControl != null) {
			clearButtonControl.setVisible(visible);
		}
	}

	/**
	 * Creates the filter text and adds listeners. This method calls
	 * {@link #doCreateFilterText(Composite)} to create the text control.
	 * Subclasses should override {@link #doCreateFilterText(Composite)} instead
	 * of overriding this method.
	 * 
	 * @param parent
	 *            <code>Composite</code> of the filter text
	 */
	protected void createFilterText(Composite parent) {
		filterText = doCreateFilterText(parent);
		
		filterText.addFocusListener(new FocusAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.FocusListener#focusLost(org.eclipse.swt.events.FocusEvent)
			 */
			public void focusGained(FocusEvent e) {
			}

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.FocusAdapter#focusLost(org.eclipse.swt.events.FocusEvent)
			 */
			public void focusLost(FocusEvent e) {
				if (filterText.getText().equals(initialText)) {
					setFilterText(""); //$NON-NLS-1$
					textChanged();
				}
			}
		});

		
		filterText.addMouseListener(new MouseAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see
				 * org.eclipse.swt.events.MouseAdapter#mouseDown(org.eclipse.swt.events.MouseEvent)
				 */
				public void mouseDown(MouseEvent e) {
					if (filterText.getText().equals(initialText)) {
						setFilterText(""); //$NON-NLS-1$
						textChanged();
					}
				}
			});
		

		filterText.addKeyListener(new KeyAdapter() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.KeyAdapter#keyReleased(org.eclipse.swt.events.KeyEvent)
			 */
			public void keyPressed(KeyEvent e) {
				// on a CR we want to transfer focus to the list
				boolean hasItems = getActiveViewer().getTree().getItemCount() > 0;
				if (hasItems && e.keyCode == SWT.ARROW_DOWN) {
					getActiveViewer().getTree().setFocus();
					return;
				}
			}
		});

		// enter key set focus to tree
		filterText.addTraverseListener(new TraverseListener() {
			public void keyTraversed(TraverseEvent e) {
				if (e.detail == SWT.TRAVERSE_RETURN) {
					e.doit = false;
					if (getActiveViewer().getTree().getItemCount() == 0) {
						Display.getCurrent().beep();
					} else {
						addSelectionToList();

					}
					//clear filter text
					filterText.setText(""); //$NON-NLS-1$
				}
			}
		});

		filterText.addModifyListener(new ModifyListener() {
			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.swt.events.ModifyListener#modifyText(org.eclipse.swt.events.ModifyEvent)
			 */
			public void modifyText(ModifyEvent e) {
				textChanged();
			}
		});

		// if we're using a field with built in cancel we need to listen for
		// default selection changes (which tell us the cancel button has been
		// pressed)
		if ((filterText.getStyle() & SWT.ICON_CANCEL) != 0) {
			filterText.addSelectionListener(new SelectionAdapter() {
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.swt.events.SelectionAdapter#widgetDefaultSelected(org.eclipse.swt.events.SelectionEvent)
				 */
				public void widgetDefaultSelected(SelectionEvent e) {
					if (e.detail == SWT.ICON_CANCEL)
						clearText();
				}
			});
		}

		GridData gridData= new GridData(SWT.FILL, SWT.CENTER, true, false);
		// if the text widget supported cancel then it will have it's own
		// integrated button. We can take all of the space.
		if ((filterText.getStyle() & SWT.ICON_CANCEL) != 0)
			gridData.horizontalSpan = 2;
		filterText.setLayoutData(gridData);
		
	}
	
	private TreeViewer getActiveViewer(){
		TreeViewer sourceViewer = null;
		if (bits == null || bits.getSelectionIndex() == 0){
			sourceViewer = treeViewer;
		}else{
			sourceViewer = cmTreeViewer;
		}
		return sourceViewer;
	}
	/*
	 * adds the selection from the treeviewer to the list viewer
	 */
	private void addSelectionToList(){
		TreeViewer sourceViewer = getActiveViewer();
		IStructuredSelection selection = (IStructuredSelection)sourceViewer.getSelection();
		for (Iterator<?> iterator = selection.iterator(); iterator.hasNext();) {
			Object x = (Object) iterator.next();
			if (x instanceof Category && !selectedList.contains(x)){
				selectedList.add((Category)x);
			}else if (x instanceof CmNode){
				if (((CmNode)x).getCategory() != null){
					Category c = ((CmNode)x).getCategory();
					if (!selectedList.contains(c)){
						selectedList.add(c);
					}
				}
			}
		}
		sourceViewer.setSelection(null);
		listViewer.refresh();
		
		fireListChanged();
	}

	/**
	 * Creates the text control for entering the filter text. Subclasses may
	 * override.
	 * 
	 * @param parent
	 *            the parent composite
	 * @return the text widget
	 * 
	 * @since 3.3
	 */
	protected Text doCreateFilterText(Composite parent) {
		if (useNativeSearchField(parent)) {
			return new Text(parent, SWT.SINGLE | SWT.BORDER | SWT.SEARCH
					| SWT.ICON_CANCEL);
		}
		return new Text(parent, SWT.SINGLE);
	}



	/**
	 * Update the receiver after the text has changed.
	 */
	protected void textChanged() {
		narrowingDown = previousFilterText == null
				|| previousFilterText.equals(INITIAL_TEXT)
				|| getFilterString().startsWith(previousFilterText);
		previousFilterText = getFilterString();
		// cancel currently running job first, to prevent unnecessary redraw
		refreshJob.cancel();
		refreshJob.schedule(getRefreshJobDelay());
	}
	
	/**
	 * Return the time delay that should be used when scheduling the
	 * filter refresh job.  Subclasses may override.
	 * 
	 * @return a time delay in milliseconds before the job should run
	 * 
	 * @since 3.5
	 */
	protected long getRefreshJobDelay() {
		return 200;
	}

	/**
	 * Set the background for the widgets that support the filter text area.
	 * 
	 * @param background
	 *            background <code>Color</code> to set
	 */
	public void setBackground(Color background) {
		super.setBackground(background);
		if (filterComposite != null && (useNativeSearchField(filterComposite))) {
			filterComposite.setBackground(background);
		}
	}
	/**
	 * @return items selected in the list
	 */
	public List<Category> getSelectedItems(){
		List<Category> selection = new ArrayList<Category>();
		for (Iterator<?> iterator = this.selectedList.iterator(); iterator.hasNext();) {
			Object category = (Object) iterator.next();
			if (category instanceof Category){
				selection.add((Category) category);
			}
		}
		return selection;
	}

	/**
	 * Create the button that clears the text.
	 * 
	 * @param parent parent <code>Composite</code> of toolbar button
	 */
	private void createClearTextNew(Composite parent) {
		// only create the button if the text widget doesn't support one
		// natively
		if ((filterText.getStyle() & SWT.ICON_CANCEL) == 0) {
			final Image inactiveImage= ObservationPlugIn.getDefault().getImageRegistry().getDescriptor(DISABLED_CLEAR_ICON).createImage();
			final Image activeImage= ObservationPlugIn.getDefault().getImageRegistry().getDescriptor(CLEAR_ICON).createImage();
			final Image pressedImage= new Image(getDisplay(), activeImage, SWT.IMAGE_GRAY);
			
			final Label clearButton= new Label(parent, SWT.NONE);
			clearButton.setLayoutData(new GridData(SWT.BEGINNING, SWT.CENTER, false, false));
			clearButton.setImage(inactiveImage);
			clearButton.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
			clearButton.setToolTipText(Messages.SearchTree_ClearSelection_Tooltip);
			clearButton.addMouseListener(new MouseAdapter() {
				private MouseMoveListener fMoveListener;

				public void mouseDown(MouseEvent e) {
					clearButton.setImage(pressedImage);
					fMoveListener= new MouseMoveListener() {
						private boolean fMouseInButton= true;

						public void mouseMove(MouseEvent e) {
							boolean mouseInButton= isMouseInButton(e);
							if (mouseInButton != fMouseInButton) {
								fMouseInButton= mouseInButton;
								clearButton.setImage(mouseInButton ? pressedImage : inactiveImage);
							}
						}
					};
					clearButton.addMouseMoveListener(fMoveListener);
				}

				public void mouseUp(MouseEvent e) {
					if (fMoveListener != null) {
						clearButton.removeMouseMoveListener(fMoveListener);
						fMoveListener= null;
						boolean mouseInButton= isMouseInButton(e);
						clearButton.setImage(mouseInButton ? activeImage : inactiveImage);
						if (mouseInButton) {
							clearText();
							filterText.setFocus();
						}
					}
				}
				
				private boolean isMouseInButton(MouseEvent e) {
					Point buttonSize = clearButton.getSize();
					return 0 <= e.x && e.x < buttonSize.x && 0 <= e.y && e.y < buttonSize.y;
				}
			});
			clearButton.addMouseTrackListener(new MouseTrackListener() {
				public void mouseEnter(MouseEvent e) {
					clearButton.setImage(activeImage);
				}

				public void mouseExit(MouseEvent e) {
					clearButton.setImage(inactiveImage);
				}

				public void mouseHover(MouseEvent e) {
				}
			});
			clearButton.addDisposeListener(new DisposeListener() {
				public void widgetDisposed(DisposeEvent e) {
					inactiveImage.dispose();
					activeImage.dispose();
					pressedImage.dispose();
				}
			});
			
			clearButton.getAccessible().addAccessibleControlListener(
				new AccessibleControlAdapter() {
					public void getRole(AccessibleControlEvent e) {
						e.detail= ACC.ROLE_PUSHBUTTON;
					}
			});
			this.clearButtonControl= clearButton;
		}
	}

	/**
	 * Clears the text in the filter text widget.
	 */
	protected void clearText() {
		setFilterText(""); //$NON-NLS-1$
		textChanged();
	}

	/**
	 * Set the text in the filter control.
	 * 
	 * @param string
	 */
	protected void setFilterText(String string) {
		if (filterText != null) {
			filterText.setText(string);
			selectAll();
		}
	}

	/**
	 * Returns the pattern filter used by this tree.
	 * 
	 * @return The pattern filter; never <code>null</code>.
	 */
	public final PatternFilter getPatternFilter() {
		return patternFilter;
	}

	/**
	 * Get the tree viewer of the receiver.
	 * 
	 * @return the tree viewer
	 */
	public TreeViewer getViewer() {
		return treeViewer;
	}

	public TreeViewer getCmViewer(){
		return cmTreeViewer;
	}
	/**
	 * Get the filter text for the receiver, if it was created. Otherwise return
	 * <code>null</code>.
	 * 
	 * @return the filter Text, or null if it was not created
	 */
	public Text getFilterControl() {
		return filterText;
	}

	/**
	 * Convenience method to return the text of the filter control. If the text
	 * widget is not created, then null is returned.
	 * 
	 * @return String in the text, or null if the text does not exist
	 */
	protected String getFilterString() {
		return filterText != null ? filterText.getText() : null;
	}

	/**
	 * Set the text that will be shown until the first focus. A default value is
	 * provided, so this method only need be called if overriding the default
	 * initial text is desired.
	 * 
	 * @param text
	 *            initial text to appear in text field
	 */
	public void setInitialText(String text) {
		initialText = text;
		if (filterText != null) {
			filterText.setMessage(text);
			if (filterText.isFocusControl()) {
				setFilterText(initialText);
				textChanged();
			} else {
				getDisplay().asyncExec(new Runnable() {
					public void run() {
						if (!filterText.isDisposed() && filterText.isFocusControl()) {
							setFilterText(initialText);
							textChanged();
						}
					}
				});
			}
		} else {
			setFilterText(initialText);
			textChanged();
		}
	}

	/**
	 * Select all text in the filter text field.
	 * 
	 */
	protected void selectAll() {
		if (filterText != null) {
			filterText.selectAll();
		}
	}

	/**
	 * Get the initial text for the receiver.
	 * 
	 * @return String
	 */
	protected String getInitialText() {
		return initialText;
	}

	public void addChangeListener(IChangeListener listener){
		this.changeListener.add(listener);
	}
	
	private void fireListChanged(){
		for (IChangeListener listener : this.changeListener){
			listener.listModified();
		}
	}
	

	
	public interface IChangeListener{
		public void listModified();
	}

}

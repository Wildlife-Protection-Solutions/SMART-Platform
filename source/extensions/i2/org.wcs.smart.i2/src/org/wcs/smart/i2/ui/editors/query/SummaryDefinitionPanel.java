/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.editors.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.SectionTabHeader;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.ICombinableDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.IDefinitionPanel;
import org.wcs.smart.i2.ui.views.query.dropitem.IGroupByDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.IValueDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.ProxyItem;

/**
 * Query definition panel for summary queries
 * 
 * @author Emily
 *
 */
public class SummaryDefinitionPanel implements IDefinitionPanel {

	private IntelEntitySummaryQueryEditor editor;
	
	private ListDefinitionPanel rowGroupByPanel;
	private ListDefinitionPanel columnGroupByPanel;
	private ListDefinitionPanel valuePanel;
	
	private FilterDefinitionPanel filterPanel;
	
	private ToolItem runItem;
	private ToolItem saveItem;
	
	private Composite infoPanel;
	private SectionTabHeader header;
	
	private List<Runnable> queryListeners = new ArrayList<>();
	
	public SummaryDefinitionPanel(IntelEntitySummaryQueryEditor editor) {
		this.editor = editor;
	}
	
	@Override
	public Composite createComposite(Composite parent) {
		FormToolkit toolkit = new FormToolkit(parent.getDisplay());
		
		Composite container = toolkit.createComposite(parent,  SWT.NONE);
		container.setLayout(new GridLayout());
		((GridLayout)container.getLayout()).marginWidth = 0;
		((GridLayout)container.getLayout()).marginHeight = 0;
		
		Composite headerPart = toolkit.createComposite(container, SWT.NONE);
		headerPart.setLayout(new GridLayout(3, false));
		((GridLayout)headerPart.getLayout()).marginWidth = 0;
		((GridLayout)headerPart.getLayout()).marginHeight = 0;
		headerPart.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		header = new SectionTabHeader(new String[] {Messages.SummaryDefinitionPanel_GroupByOpHeader, Messages.SummaryDefinitionPanel_FilterOpHeader}, headerPart, toolkit, Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		
		createToolbar(headerPart);
		
		Composite definitionStack = toolkit.createComposite(container, SWT.NONE);
		definitionStack.setLayout(new StackLayout());
		definitionStack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		((StackLayout)definitionStack.getLayout()).marginWidth = 0;
		((StackLayout)definitionStack.getLayout()).marginHeight = 0;
		
		Composite groupByComp = toolkit.createComposite(definitionStack, SWT.NONE);
		groupByComp.setLayout(new GridLayout());
		((GridLayout)groupByComp.getLayout()).marginWidth = 0;
		((GridLayout)groupByComp.getLayout()).marginHeight = 0;
		
		Composite filterComposite = toolkit.createComposite(definitionStack, SWT.NONE);
		filterComposite.setLayout(new GridLayout());
		((GridLayout)filterComposite.getLayout()).marginWidth = 0;
		((GridLayout)filterComposite.getLayout()).marginHeight = 0;
		
		header.setContent(new Composite[] {groupByComp, filterComposite}, definitionStack);
		header.selectTab(0);
		
		createGroupBySection(groupByComp);
		
		filterPanel = new FilterDefinitionPanel(false, false){
			public void runQuery(){
				editor.runQuery();
			}
			public void saveQuery(){
				editor.getSite().getPage().saveEditor(editor, false);
			}
			
			@Override
			protected void createRightArea(Composite parent){
				if (IntelSecurityManager.INSTANCE.canEditQuery()) {
					ToolBar toolbar = new ToolBar(parent, SWT.FLAT | SWT.NONE);
					toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.TOP, false, false));
					
					ToolItem clear = new ToolItem(toolbar, SWT.PUSH);
					clear.setToolTipText(Messages.SummaryDefinitionPanel_CleanFiltertooltip);
					clear.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CLEAR));
					clear.addSelectionListener(new SelectionAdapter() {
						
						@Override
						public void widgetSelected(SelectionEvent e) {
							clear();
							fireQueryChangedListeners();
						}
					});
				}
			}
		};
		filterPanel.addQueryChangedListener(()->fireQueryChangedListeners());
		
		Composite definitionPanel = filterPanel.createComposite(filterComposite);
		definitionPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		toolkit.dispose();
		return container;
	}

	private void createToolbar(Composite parent) {
		infoPanel = new Composite(parent, SWT.NONE);
		infoPanel.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		((GridData)infoPanel.getLayoutData()).heightHint = 10;
		
		ToolBar toolbar = new ToolBar(parent, SWT.FLAT);
		toolbar.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));
		
		if (IntelSecurityManager.INSTANCE.canEditQuery()) {
			saveItem = new ToolItem(toolbar, SWT.PUSH);
			saveItem.setToolTipText(Messages.FilterDefinitionPanel_savetooltip);
			saveItem.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_SAVE_EDIT));
			saveItem.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					editor.getSite().getPage().saveEditor(editor, false);
				}
			});
			
			ToolItem clear = new ToolItem(toolbar, SWT.PUSH);
			clear.setToolTipText(Messages.FilterDefinitionPanel_clearTooltip);
			clear.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CLEAR));
			clear.addSelectionListener(new SelectionAdapter() {
				
				@Override
				public void widgetSelected(SelectionEvent e) {
					clear();
					fireQueryChangedListeners();
				}
			});
		}
		
		runItem = new ToolItem(toolbar, SWT.PUSH);
		runItem.setToolTipText(Messages.FilterDefinitionPanel_runtooltip);
		runItem.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RUN));
		runItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				editor.runQuery();
			}
		});
		
	}
	private void createGroupBySection(Composite parent) {
		SashForm areas = new SashForm(parent, SWT.NONE);
		areas.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
		Composite rowComp = new Composite(areas, SWT.BORDER);
		rowComp.setLayout(new GridLayout());
		((GridLayout)rowComp.getLayout()).marginWidth = 0;
		((GridLayout)rowComp.getLayout()).marginHeight = 0;
		
		Composite header = new Composite(rowComp, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		
		Label l = new Label(header, SWT.NONE);
		l.setText(Messages.SummaryDefinitionPanel_RowGroupByHeader);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		if (IntelSecurityManager.INSTANCE.canEditQuery()) {
			ToolBar t = new ToolBar(header, SWT.FLAT);
			ToolItem rgbClear = new ToolItem(t, SWT.PUSH);
			rgbClear.setToolTipText(Messages.SummaryDefinitionPanel_ClearPanelTooltip);
			rgbClear.addListener(SWT.Selection, e-> rowGroupByPanel.clear());
			rgbClear.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CLEAR));
		}
		rowGroupByPanel = new ListDefinitionPanel();
		rowGroupByPanel.createComposite(rowComp);
		
		
		Composite colComp = new Composite(areas, SWT.BORDER);
		colComp.setLayout(new GridLayout());
		((GridLayout)colComp.getLayout()).marginWidth = 0;
		((GridLayout)colComp.getLayout()).marginHeight = 0;
		
		header = new Composite(colComp, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(header, SWT.NONE);
		l.setText(Messages.SummaryDefinitionPanel_ColumnGroupbyHeader);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		if (IntelSecurityManager.INSTANCE.canEditQuery()) {
			ToolBar t = new ToolBar(header, SWT.FLAT);
			ToolItem cgbClear = new ToolItem(t, SWT.PUSH);
			cgbClear.setToolTipText(Messages.SummaryDefinitionPanel_ClearPanelTooltip);
			cgbClear.addListener(SWT.Selection, e-> columnGroupByPanel.clear());
			cgbClear.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CLEAR));
		}		
		columnGroupByPanel = new ListDefinitionPanel();
		columnGroupByPanel.createComposite(colComp);
		
		Composite valueComp = new Composite(areas, SWT.BORDER);
		valueComp.setLayout(new GridLayout());
		((GridLayout)valueComp.getLayout()).marginWidth = 0;
		((GridLayout)valueComp.getLayout()).marginHeight = 0;
		
		header = new Composite(valueComp, SWT.NONE);
		header.setLayout(new GridLayout(2, false));
		((GridLayout)header.getLayout()).marginWidth = 0;
		((GridLayout)header.getLayout()).marginHeight = 0;
		header.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
		l = new Label(header, SWT.NONE);
		l.setText(Messages.SummaryDefinitionPanel_ValuesHeader);
		l.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		
		if (IntelSecurityManager.INSTANCE.canEditQuery()) {
			ToolBar t = new ToolBar(header, SWT.FLAT);
			ToolItem vClear = new ToolItem(t, SWT.PUSH);
			vClear.setToolTipText(Messages.SummaryDefinitionPanel_ClearPanelTooltip);
			vClear.addListener(SWT.Selection, e-> valuePanel.clear());
			vClear.setImage(Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_CLEAR));
		}		
		valuePanel = new ListDefinitionPanel();
		valuePanel.createComposite(valueComp);
		
		
		rowGroupByPanel.addTargetPanel(columnGroupByPanel);
		columnGroupByPanel.addTargetPanel(rowGroupByPanel);
	}

	@Override
	public void dispose() {
		rowGroupByPanel.dispose();
		columnGroupByPanel.dispose();
		valuePanel.dispose();
	}

	@Override
	public void redraw() {
		rowGroupByPanel.redraw();
		columnGroupByPanel.redraw();
		valuePanel.redraw();
	}

	public void initGroupByItems(List<DropItem> items, boolean isRow) {
		if (isRow) {
			rowGroupByPanel.addItems(items);
		}else {
			columnGroupByPanel.addItems(items);
		}
	}
	
	public void initValueItems(List<DropItem> items) {
		valuePanel.addItems(items);
	}
	public void initFilterItems(List<DropItem> items) {
		filterPanel.addItems(items);
	}
	
	public void setQueryState(boolean isDirty){
		if (this.saveItem != null) this.saveItem.setEnabled(isDirty);
	}
	
	@Override
	public void addItem(DropItem item) {
		if (item instanceof IGroupByDropItem) {
			rowGroupByPanel.addItem(item);
			header.selectTab(0);
		}else if (item instanceof IValueDropItem) {
			valuePanel.addItem(item);
			header.selectTab(0);
		}else {
			filterPanel.addItem(item);
			
			header.selectTab(1);
		}
		fireQueryChangedListeners();
		
	}


	@Override
	public void removeItem(DropItem item) {
		if (item instanceof IGroupByDropItem) {
			rowGroupByPanel.removeItem(item);
			columnGroupByPanel.removeItem(item);
			fireQueryChangedListeners();
		}else if (item instanceof IValueDropItem) {
			valuePanel.removeItem(item);
			fireQueryChangedListeners();
		}else {
			filterPanel.removeItem(item);
		}
	}


	@Override
	public String validate() {
		return null;
	}

	@Override
	public Composite getDropTargetComposite() {
		return null;
	}


	@Override
	public String getQueryPart() {
		StringBuilder sb = new StringBuilder();
		sb.append(rowGroupByPanel.getQueryPart());
		sb.append(IntelEntitySummaryQuery.PART_SEPERATOR);
		sb.append(columnGroupByPanel.getQueryPart());
		sb.append(IntelEntitySummaryQuery.PART_SEPERATOR);
		sb.append(valuePanel.getQueryPart());
		sb.append(IntelEntitySummaryQuery.PART_SEPERATOR);
		sb.append(filterPanel.getQueryPart());
		return sb.toString();
	}


	@Override
	public void clear() {
		rowGroupByPanel.clear();
		columnGroupByPanel.clear();
		valuePanel.clear();
		filterPanel.clear();
	}


	@Override
	public void finishDrag(DropItem item) {
		item.getTargetPanel().finishDrag(item);
	}


	public void addQueryChangedListener(Runnable r){
		queryListeners.add(r);
	}
	
	@Override
	public void fireQueryChangedListeners() {
		for (Runnable r : queryListeners){
			r.run();
		}
	}
	
	public void setErrorMessage(String message, Exception fullMessage){
		if (infoPanel == null) return;
		
		for (Control c : infoPanel.getChildren()){
			c.dispose();
		}
		if (message == null){
			runItem.setEnabled(true);
			infoPanel.getParent().layout(true,true);
			return;
		}
		
		runItem.setEnabled(false);
		
		infoPanel.setLayout(new GridLayout(2, false));
		Label l = new Label(infoPanel, SWT.NONE);
		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
		
		l = new Label(infoPanel, SWT.NONE);
		l.setText(message);
		l.setToolTipText(fullMessage.getMessage());
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		infoPanel.getParent().layout(true,true);
	}
	
	
	private class ListDefinitionPanel implements IDefinitionPanel{

		private ScrolledComposite dropTarget = null;
		private Composite dropTargetContent; 
		private ProxyItem proxy = null;	//drag proxy item	
		protected ArrayList<DropItem> items = new ArrayList<DropItem>();	//list of controls in formula
		
		private Set<ListDefinitionPanel> targetPanels;

		private DropItem dragItem;
		
		/**
		 * Creates a new drop target panel.
		 * 
		 * @param view
		 * @param  unique - if the list should only contain unique items
		 */
		public ListDefinitionPanel(){
			targetPanels = new HashSet<ListDefinitionPanel>();
			targetPanels.add(this);
		}
		
		/**
		 * Adds another list drop target panel as an option for
		 * a drop target.  This allows you to move items between lists
		 * 
		 * @param panel
		 */
		public void addTargetPanel(ListDefinitionPanel panel){
			targetPanels.add(panel);
		}
		

		/**
		 * Clears all items from the query and hides the 
		 * proxy.
		 * 
		 */
		public void clear(){
			for (DropItem item: items){
				if (item == proxy){
					continue;
				}
				if (item != null){
					item.dispose();
				}
			}
			proxy.getWidget().setVisible(false);
			items.clear();
			dropTarget.redraw();
		}
		
		/**
		 * Converts the items to a comma-delimited list
		 * of items
		 * 
		 * @return the query string represented by the items in the query panel
		 */
		public String getQueryPart(){
			StringBuilder query = new StringBuilder();
			
			for (Object item : items){
				if (item instanceof DropItem){
					DropItem it = (DropItem)item;
					query.append(it.asQueryPart());
					query.append(","); //$NON-NLS-1$
				}
			}
			if (query.length() > 0){
				query.deleteCharAt(query.length() -1);
			}
			return query.toString();
		}
		
		
		/**
		 * Re-draw the drop items
		 */
		public void redraw(){
			orderElements();
		}
		
		
		/**
		 * Adds all items to the current query.  
		 * <p>This does not fire the query changed event.
		 * </p>
		 * 
		 * @param items
		 */
		public void addItems(Collection<DropItem> items) {
			if (items != null) {
				for (DropItem item : items) {
					item.createWidget(this, dropTargetContent);
					this.items.add(item);
				}
			}
			orderElements();
		}
		
		/**
		 * Adds a drop item to the query formula.  Fires a query 
		 * change event after item added.
		 * @param item drop item to add
		 */
		public void addItem(DropItem item) {
			for (DropItem it: items){
				if (it.asQueryPart().equals(item.asQueryPart()))
					return;
			}
			if (addCombinedItem(item)){
				//added to existing item so we don't need to add to list
				return;
			}
			item.createWidget(this, dropTargetContent);
			items.add(item);
			orderElements();
			fireQueryChangedListeners();
		}
		
		/**
		 * if the drop item is a combinable item it searches the other
		 * drop items in the list to determine if the item can
		 * be added to one of the existing items
		 * 
		 * @param combinableItem
		 * @return
		 */
		private boolean addCombinedItem(DropItem combinableItem){
			if (combinableItem instanceof ICombinableDropItem){
				for (DropItem it : items){
					if (it instanceof ICombinableDropItem){
						if (((ICombinableDropItem)it).addItem(combinableItem)){
							orderElements();
							fireQueryChangedListeners();
							return true;
						}
					}
				}
			}
			return false;
		}

		/**
		 * Remove an element from the drop item
		 * 
		 * @param item item to remove
		 */
		public void removeItem(DropItem item){
			if (items.remove(item)){
				item.dispose();
			}
			orderElements();
		}
		
		/**
		 * Redraws the items in the query formula in the correct order
		 */
		public void orderElements() {
			int curry = 0;
			int maxWidth = 0;
			
			for (int i = 0; i < items.size(); i++) {
				DropItem di = items.get(i);
				Point pnt = di.getWidget().computeSize(SWT.DEFAULT, SWT.DEFAULT);
				int height = Math.max(30, pnt.y);
				maxWidth = Math.max(maxWidth, pnt.x);			
				di.getWidget().setBounds(0, curry, pnt.x, height);
				if (di.getWidget() != null) {
					di.getWidget().layout();
				}
				curry += height;
			}
			dropTargetContent.setSize(maxWidth, curry);
			dropTarget.redraw();

		}

		/**
		 * @return the drop target composite
		 */
		public Composite getDropTargetComposite() {
			return dropTargetContent;
		}

		
		/**
		 * @see org.wcs.smart.query.ui.IDefinitionPanel.IDropPanel#finishDrag(org.wcs.smart.query.ui.formulaDnd.DropItem)
		 */
		@Override
		public void finishDrag(DropItem di){
			if (di.getTargetPanel() != this){
				items.remove(di);
			}
			
			if (di.getTargetPanel() == this && !items.contains(di)){
				int index= items.indexOf(proxy);
				if (index >= 0){
					items.add(index, di);
				}else{
					items.add(di);
				}
				di.getWidget().setVisible(true);
			}
			items.remove(proxy);
			proxy.getWidget().setVisible(false);
			this.dragItem = null;
			
			orderElements();
			fireQueryChangedListeners();
		}
		
		/**
		 * Creates the drop target composite
		 * @param parent
		 * @return
		 */
		public Composite createComposite(Composite parent) {

			dropTarget = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL );
			dropTarget.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
			dropTarget.addListener(SWT.Resize, new Listener() {
				@Override
				public void handleEvent(Event event) {
					orderElements();
				}
			});		
			
			dropTargetContent = new Composite(dropTarget, SWT.NONE);
			dropTargetContent.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			dropTarget.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_WHITE));
			dropTarget.setContent(dropTargetContent);

			DropTarget dtarget = new DropTarget(dropTarget, DND.DROP_MOVE);
			dtarget.setTransfer(DND_TYPES);
			dtarget.addDropListener(new DropTargetAdapter() {

				
				@Override
				public void dragEnter(DropTargetEvent event) {
					
					StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
					if (selection == null) {
						return;
					}
					//hide drop item and setup proxy
					dragItem = (DropItem)selection.getFirstElement();
					

					if (!targetPanels.contains(dragItem.getTargetPanel())){
						event.detail  = DND.DROP_NONE;
						dragItem = null;
						return;
					}
					
					if (dragItem.getWidget().isVisible()){
						dragItem.getWidget().setVisible(false);
						
						int i = items.indexOf(dragItem);
						if (!proxy.getWidget().isVisible()){
							if ( i < 0 ){
								items.add(proxy);
							}else{
								items.add(i, proxy);
								items.remove(dragItem);
							}
							proxy.setLabelText(dragItem.getText());
							proxy.getWidget().setVisible(true);
						}
					}else if (!proxy.getWidget().isVisible()){
						//a different panel from the source
						proxy.setLabelText(dragItem.getText());
						proxy.getWidget().setVisible(true);
						moveElements(event.x, event.y);
					}

					orderElements();
				}

				public void dragLeave(DropTargetEvent event) {
					if (proxy.getWidget().isVisible() && dragItem.getTargetPanel() != ListDefinitionPanel.this){
						items.remove(proxy);
						proxy.getWidget().setVisible(false);
					}
					orderElements();
				}
				
				@Override
				public void dragOperationChanged(DropTargetEvent event) {
				}

				@Override
				public void dragOver(DropTargetEvent event) {
					if (event.detail != DND.DROP_NONE){
						if (dragItem == null) return;
						moveElements(event.x, event.y);
						orderElements();
					}
				}

				@Override
				public void dropAccept(DropTargetEvent event) {
				}

				@Override
				public void drop(DropTargetEvent event) {
					if (event.detail == DND.DROP_NONE || dragItem == null){
						return;
					}
					if (dragItem.getTargetPanel() != ListDefinitionPanel.this){
						IDefinitionPanel target =  dragItem.getTargetPanel();
						dragItem.moveParent(ListDefinitionPanel.this);
						target.finishDrag(dragItem);
						
						//move this this panel
						if (ListDefinitionPanel.this.addCombinedItem(dragItem)){
							//combined with existing drop item
							ListDefinitionPanel.this.removeItem(dragItem);
							dragItem.setTargetPanel(null);
							dragItem = null;
						}
					}

					if (dragItem != null){
						moveElements(event.x, event.y);
						//remove proxy and put back the drop item
						int i = items.indexOf(proxy);
						if (!items.contains(dragItem)){
							items.add(i, dragItem);	
						}
						
						dragItem.getWidget().setVisible(true);
					}
					items.remove(proxy);
					proxy.getWidget().setVisible(false);
					
					orderElements();
					dragItem = null;
				}

				private void moveElements(int x, int y) {
					DropItem target = null;
					boolean before = false;
					for (DropItem children : items) {
						Rectangle childBounds = children.getWidget().getBounds();
						
						Point p = children.getWidget().getParent().toDisplay(childBounds.x, childBounds.y);
						Rectangle r = new Rectangle(p.x, p.y, childBounds.width,childBounds.height);
						
						if (r.contains(x, y)) {
							target = children;
							before = y < p.y + (childBounds.height / 2.0);
							break;
						}
					}
					if (target == null) {
						items.remove(proxy);
						items.add(proxy);
					} else if (target == proxy) {
						return;
					} else {
						items.remove(proxy);
						int toIndex = items.indexOf(target);
						if (!before) {
							toIndex++;
						}
						if (toIndex < 0) {
							toIndex = 0;
						}
						items.add(toIndex, proxy);
					}
				}
			});
		
			dropTarget.setSize(dropTarget.computeSize(SWT.DEFAULT, SWT.DEFAULT));
			dropTargetContent.setSize(dropTarget.computeSize(SWT.DEFAULT,
					SWT.DEFAULT));
			
			
			//create proxy item
			proxy = new ProxyItem();
			proxy.createWidget(this, dropTargetContent);
			proxy.getWidget().setVisible(false);
			
			return dropTarget;
		}


		@Override
		public void fireQueryChangedListeners() {
			SummaryDefinitionPanel.this.fireQueryChangedListeners();
		}
		
		@Override
		public void dispose() {
			dropTarget.dispose();
		}
		
		@Override
		public String validate() {
			return SummaryDefinitionPanel.this.editor.validateQuery();
			
		}
	}

}

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
package org.wcs.smart.query.ui.definition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.ICombinableDropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.ProxyItem;

/**
 * Drop panel that represents a simple list of drop items.
 * @author egouge
 * @since 1.0.0
 */
public abstract class ListDefinitionPanel implements IDefinitionPanel{

	final static Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
	
	private ScrolledComposite dropTarget = null;
	private Composite dropTargetContent; 
	
	private ProxyItem proxy = null;	//drag proxy item
	protected QueryProxy currentQuery = null;
	
	protected ArrayList<DropItem> items = new ArrayList<DropItem>();	//list of controls in formula
	
	private boolean isUnique = false;
	
	private Set<ListDefinitionPanel> targetPanels;
	

	private DropItem dragItem;
	
	public ListDefinitionPanel(){
		this(false);
	}
	/**
	 * Creates a new drop target panel.
	 * 
	 * @param view
	 * @param  unique - if the list should only contain unique items
	 */
	public ListDefinitionPanel(boolean unique){
		this.isUnique = unique;
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
	 * @return 
	 */
	public static Transfer[] getTransferTypes() {
		return types;
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
	 * @return the current set of drop items associated
	 * with this query
	 */
	public List<DropItem> getItems(){
		return items;
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
	 * Validates the current query.
	 */
	public abstract String validate();
	
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
		if (this.isUnique){
			for (DropItem it: items){
				if (it.asQueryPart().equals(item.asQueryPart()))
					return;
			}
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

		dropTarget = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NONE);
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
		dtarget.setTransfer(types);
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
		QueryEventManager.getInstance().fireQueryDefinitionModified(currentQuery.getQuery());
	}
	
	@Override
	public void dispose() {
		dropTarget.dispose();
	}
	
	/**
	 * @see org.wcs.smart.query.ui.IDefinitionPanel.IDropPanel#layout()
	 */
	public void layout() {
		orderElements();
	}

	public void clearDragItem() {
		this.dragItem = null;
	}

	@Override
	public void saveItems(QueryProxy q){
		List<DropItem> duplicate = new ArrayList<DropItem>();
		duplicate.addAll(items);
		q.setDropItems(getId(), duplicate);
	}


	@Override
	public abstract String getId();

	@Override
	public abstract String getGuiName();
	
	@Override
	public void initItems(QueryProxy q){
		this.currentQuery = q;
		clear();
		this.addItems(q.getDropItems(getId()));
		
	}
}

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
package org.wcs.smart.query.ui.formulaDnd;

import java.util.ArrayList;
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
import org.wcs.smart.query.ui.definition.QueryDefView;

/**
 * Drop panel that represents a simple list of drop items.
 * @author egouge
 * @since 1.0.0
 */
public class ListDropTargetPanel implements IDropPanel{

	final static Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
	
	private ScrolledComposite dropTarget = null;
	private Composite dropTargetContent; 
	
	private ProxyItem proxy = null;	//drag proxy item

	private ArrayList<DropItem> items = new ArrayList<DropItem>();	//list of controls in formula
	private QueryDefView parentView = null;
	
	private boolean isUnique = false;
	
	private Set<ListDropTargetPanel> targetPanels;

	private DropItem dragItem;
	
	/**
	 * Creates a new drop target panel.
	 * 
	 * @param view
	 * @param  unique - if the list should only contain unique items
	 */
	public ListDropTargetPanel(QueryDefView view, boolean unique){
		this.parentView = view;
		this.isUnique = unique;
		targetPanels = new HashSet<ListDropTargetPanel>();
		targetPanels.add(this);
	}
	
	/**
	 * Adds another list drop target panel as an option for
	 * a drop target.  This allows you to move items between lists
	 * 
	 * @param panel
	 */
	public void addTargetPanel(ListDropTargetPanel panel){
		targetPanels.add(panel);
	}
	
	/**
	 * @return parent veiw
	 */
	public QueryDefView getParentView(){
		return this.parentView;
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
		validate();
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
	public String getQueryString(){
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
	public void validate(){
		parentView.validate();
	}
	
	/**
	 * Adds all items to the current query.  
	 * <p>This does not fire the query changed event.
	 * </p>
	 * 
	 * @param items
	 */
	public void addElements(List<DropItem> items) {
		if (items != null) {
			for (DropItem item : items) {
				item.createWidget(this);
				this.items.add(item);
			}
		}
		orderElements();
		validate();
	}
	
	/**
	 * Adds a drop item to the query formula.
	 * @param item drop item to add
	 */
	public void addElement(DropItem item) {
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
		item.createWidget(this);
		items.add(item);
		orderElements();
		validate();
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
						validate();
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
	public void removeElement(DropItem item){
		if (items.remove(item)){
			item.dispose();
		}
		orderElements();
		validate();
	}
	
	/**
	 * Redraws the items in the query formula in the correct order
	 */
	public void orderElements() {
		int curry = 0;
		int maxWidth = 0;
		
		for (int i = 0; i < items.size(); i++) {
			Point pnt = items.get(i).getWidget().computeSize(SWT.DEFAULT, SWT.DEFAULT);
			int height = Math.max(30, pnt.y);
			maxWidth = Math.max(maxWidth, pnt.x);			
			items.get(i).getWidget().setBounds(0, curry, pnt.x, height);
			if (items.get(i).getWidget() != null) {
				items.get(i).getWidget().layout();
			}
			curry += height;
		}
		dropTargetContent.setSize(maxWidth, curry);
		dropTarget.redraw();

	}

	/**
	 * @return the drop target composite
	 */
	public Composite getComposite() {
		return dropTargetContent;
	}

	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.IDropPanel#finishDrag(org.wcs.smart.query.ui.formulaDnd.DropItem)
	 */
	@Override
	public void finishDrag(DropItem di){
		if (di.targetPanel != this){
			items.remove(di);
		}
		
		if (di.targetPanel == this && !items.contains(di)){
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
				

				if (!targetPanels.contains(dragItem.targetPanel)){
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
				if (proxy.getWidget().isVisible() && dragItem.targetPanel != ListDropTargetPanel.this){
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
				if (dragItem.targetPanel != ListDropTargetPanel.this){
					IDropPanel target =  dragItem.targetPanel;
					dragItem.moveParent(ListDropTargetPanel.this);
					target.finishDrag(dragItem);
					
					//move this this panel
					if (ListDropTargetPanel.this.addCombinedItem(dragItem)){
						//combined with existing drop item
						ListDropTargetPanel.this.removeElement(dragItem);
						dragItem.targetPanel = null;
						dragItem = null;
					}
				}

				if (dragItem != null){
					moveElements(event.x, event.y);
					//remove proxy and put back the drop item
					int i = items.indexOf(proxy);
					items.add(i, dragItem);
					dragItem.getWidget().setVisible(true);
				}
				items.remove(proxy);
				proxy.getWidget().setVisible(false);
				
				orderElements();
				dragItem = null;
				validate();
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
						before = x < p.x + (childBounds.width / 2.0);
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
		proxy.createWidget(this);
		proxy.getWidget().setVisible(false);
		
		return dropTarget;
	}


	@Override
	public void fireQueryChangedListeners() {
		getParentView().fireQueryModifiedListeners();
	}
	
	/**
	 * @see org.wcs.smart.query.ui.formulaDnd.IDropPanel#layout()
	 */
	@Override
	public void layout() {
		orderElements();
	}
}

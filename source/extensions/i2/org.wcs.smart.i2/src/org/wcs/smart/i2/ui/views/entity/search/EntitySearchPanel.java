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
package org.wcs.smart.i2.ui.views.entity.search;

import java.util.ArrayList;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.i2.query.Operator;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.IDefinitionPanel;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.ProxyItem;
import org.wcs.smart.i2.ui.views.query.dropitem.TextOperatorDropItem;

/**
 * Simple definition panel for creating filter queries
 * 
 * @author Emily
 *
 */
public class EntitySearchPanel implements IDefinitionPanel {
	
	private Composite mainComposite;
	private ScrolledComposite dropTarget = null;
	private Composite dropTargetContent;
	
	private ProxyItem proxy = null;	//drag proxy item
	protected ArrayList<DropItem> items = new ArrayList<DropItem>();	//list of controls in formula
	private DropItem dragItem;
	
	
	/**
	 * Creates a new drop target panel.
	 * 
	 */
	public EntitySearchPanel(){
	}


	@Override
	public void dispose(){
		mainComposite.dispose();
	}

	/**
	 * Clears all items from the query and hides the 
	 * proxy.
	 * 
	 */
	@Override
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
		if (this.dragItem != null){
			this.dragItem.dispose();
			this.dragItem = null;
		}		
	}
	
	
	/**
	 * Converts the items that make up the query to 
	 * a query string.
	 * 
	 * @return the query string represented by the items in the query panel
	 */
	@Override
	public String getQueryPart(){
		StringBuilder query = new StringBuilder();

		for (Object item : items){
			if (item instanceof DropItem){
				DropItem it = (DropItem)item;
				query.append(it.asQueryPart());
				query.append("|"); //$NON-NLS-1$
			}
		}
		
		if (query.length() > 0 && query.charAt(query.length() - 1) == '|'){
			query.deleteCharAt(query.length() - 1);
		}
		
		return query.toString();
	}
	
	
	/**
	 * Validates the items in the filter panel 
	 */
	@Override
	public String validate(){
		return null;
	}
	
//	/**
//	 * Adds all items to the current query.  
//	 * <p>This does not fire the query changed event.
//	 * </p>
//	 * 
//	 * @param items
//	 */
//	
//	public void addItems(Collection<DropItem> items) {
//		if (items != null) {
//			for (DropItem item : items) {
//				item.createWidget(this, dropTargetContent);
//				this.items.add(item);
//			}
//		}
//		orderElements();
//	}
	
	/**
	 * Adds a drop item to the query formula; fires a query changed
	 *  event after item added
	 * @param item drop item to add
	 */
	@Override
	public void addItem(DropItem item) {
		item.createWidget(this, dropTargetContent);
		
		if (items.size() > 0){
			boolean addBoolean = true;
			if (item instanceof TextOperatorDropItem){
				Operator op = ((TextOperatorDropItem)item).getOperator();
				if (op.equals(Operator.NOT) || op.equals(Operator.BRACKET_CLOSE) || 
						op.equals(Operator.BRACKET_OPEN) || op.equals(Operator.BRACKETS)){ 
					addBoolean = false;
				}
			}
			if (addBoolean){
				DropItem it = OptionDropItem.createAndOrDropItem();
				it.createWidget(this, dropTargetContent);
				items.add(it);	
			}
		}
		items.add(item);
		orderElements();
		fireQueryChangedListeners();
	}

	/**
	 * Remove an element from the drop item
	 * 
	 * @param item item to remove
	 */
	@Override
	public void removeItem(DropItem item){
		if (items.remove(item)){
			item.dispose();
		}
		orderElements();
	}
	
	/**
	 * Redraws the items in the query formula in the correct order
	 */
	private void orderElements() {
		int currx = 0;
		int curry = 0;
		int maxWidth = dropTarget.getBounds().width;
		int height = 10;
		int width = 0;
		for (int i = 0; i < items.size(); i++) {
			Point pnt = items.get(i).getWidget().computeSize(SWT.DEFAULT, SWT.DEFAULT);
			height = Math.max(33,pnt.y);
			if (currx + pnt.x > maxWidth && currx != 0) {
				// move to next line
				width = Math.max(width, currx);
				curry += height;
				currx = 0;
			}
			items.get(i).getWidget().setBounds(currx, curry, pnt.x, height);
			currx += pnt.x;
			if (items.get(i).getWidget() != null) {
				items.get(i).getWidget().layout();
			}
		}
		width = Math.max(width, currx);
		dropTargetContent.setSize(width, curry + height);
		dropTarget.redraw();
	}


	
	/**
	 * @see org.wcs.smart.query.ui.IDefinitionPanel.IDropPanel#finishDrag(org.wcs.smart.query.ui.formulaDnd.DropItem)
	 */
	@Override
	public void finishDrag(DropItem di){
		if (di.getTargetPanel() != this){
			items.remove(di);
		}
		if (di.getTargetPanel() == null && !items.contains(di)){
			int index= items.indexOf(proxy);
			
			proxy.getWidget().setVisible(false);
			if (index >= 0){
				items.add(index, di);
			}else{
				items.add(di);
			}
			di.getWidget().setVisible(true);
		}
		items.remove(proxy);
		proxy.getWidget().setVisible(false);
		dragItem = null;
		
		orderElements();
		fireQueryChangedListeners();

	}
	
	@Override
	public void redraw(){
		orderElements();
	}
//	
//	public void setErrorMessage(String message, Exception fullMessage){
//		for (Control c : infoPanel.getChildren()){
//			c.dispose();
//		}
//		if (message == null){
//			runItem.setEnabled(true);
//			infoPanel.getParent().layout(true,true);
//			return;
//		}
//		
//		runItem.setEnabled(false);
//		
//		infoPanel.setLayout(new GridLayout(2, false));
//		Label l = new Label(infoPanel, SWT.NONE);
//		l.setImage(SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ERROR_ICON));
//		l.setLayoutData(new GridData(SWT.FILL, SWT.TOP, false, false));
//		
//		l = new Label(infoPanel, SWT.NONE);
//		l.setText(message);
//		l.setToolTipText(fullMessage.getMessage());
//		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		infoPanel.getParent().layout(true,true);
//	}
	
	
	
	/**
	 * Creates the drop target composite
	 * @param parent
	 * @return
	 */
	@Override
	public Composite createComposite(Composite parent) {
		
		mainComposite = new Composite(parent, SWT.BORDER);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		mainComposite.setLayout(layout);
		
		dropTarget = new ScrolledComposite(mainComposite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.NONE);
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
				if (dragItem == null){
					StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
					if (selection == null) {
						return;
					}
					//hide drop item and setup proxy
					dragItem = (DropItem)selection.getFirstElement();
				}
				if (dragItem.getTargetPanel() != EntitySearchPanel.this){
					event.detail  = DND.DROP_NONE;
					dragItem = null;
					return;
				}
				
				if (dragItem.getWidget().isVisible()){
					dragItem.getWidget().setVisible(false);
					int i = items.indexOf(dragItem);
					if ( i < 0){
						items.add(proxy);
					}else{
						items.add(i, proxy);
						items.remove(dragItem);
					}
				}else if (!proxy.getWidget().isVisible()){
					moveElements(event.x, event.y);
					
				}
				proxy.setLabelText(dragItem.getText());
				proxy.getWidget().setVisible(true);
				orderElements();
			}

			public void dragLeave(DropTargetEvent event) {
				if (proxy.getWidget().isVisible() && dragItem.getTargetPanel() != EntitySearchPanel.this){
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
				moveElements(event.x, event.y);
				//remove proxy and put back the drop item
				int i = items.indexOf(proxy);
				items.add(i, dragItem);
				dragItem.getWidget().setVisible(true);
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
		proxy.createWidget(this, dropTargetContent);
		proxy.getWidget().setVisible(false);
		
		return mainComposite;
	}

	@Override
	public Composite getDropTargetComposite() {
		return dropTargetContent;
	}


	@Override
	public void fireQueryChangedListeners() {

	}
}

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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.filter.IFilter;
import org.wcs.smart.query.parser.filter.IFilter.FilterType;
import org.wcs.smart.query.ui.definition.QueryDefView;

/**
 * A drop target area for creating query formulas
 * @author Emily
 * @since 1.0.0
 */
public class FilterDropTargetPanel implements IDropPanel {

	public static final String PANEL_TITLE = Messages.FilterDropTargetPanel_QueryFilterPanelTitle1;
	
	final static Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
	
	private ScrolledComposite dropTarget = null;
	private Composite dropTargetContent; 
	
	private ProxyItem proxy = null;	//drag proxy item

	private ArrayList<DropItem> items = new ArrayList<DropItem>();	//list of controls in formula
	private QueryDefView parentView = null;
	
	private TreeDropDownViewer treeEditor = null;
	private Set<FilterDropTargetPanel> targetPanels;
	
	private DropItem dragItem;
	
	private Button btnWaypoint;
	private Button btnObservation;
	/**
	 * Creates a new drop target panel.
	 * 
	 */
	public FilterDropTargetPanel(QueryDefView view){
		this.parentView = view;
		this.targetPanels = new HashSet<FilterDropTargetPanel>();
		targetPanels.add(this);
	}
	
	
	public void addDropTargetPanel(FilterDropTargetPanel panel){
		this.targetPanels.add(panel);
	}
	
	public QueryDefView getParentView(){
		return this.parentView;
	}
	/**
	 * @return 
	 */
	public static Transfer[] getTransferTypes() {
		return types;
	}

	public void setFilterType(IFilter.FilterType filterType){
		if (filterType.equals(FilterType.WAYPOINT)){
			btnWaypoint.setSelection(true);
			btnObservation.setSelection(false);
		}else{
			btnObservation.setSelection(true);
			btnWaypoint.setSelection(false);
		}
	}

	public void dispose(){		
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
		if (this.dragItem != null){
			this.dragItem.dispose();
			this.dragItem = null;
		}		
	}
	
	/**
	 * @return the current set of drop items associated
	 * with this query
	 */
	public List<DropItem> getItems(){
		return items;
	}
	
	/**
	 * Converts the items that make up the query to 
	 * a query string.
	 * 
	 * @return the query string represented by the items in the query panel
	 */
	public String getQueryString(){
		StringBuilder query = new StringBuilder();
		
		if (items.size() > 0){
			//if non-empty filter then include filter type
			if (btnWaypoint.getSelection()){
				query.append(IFilter.FilterType.WAYPOINT.getKey());
			}else{
				query.append(IFilter.FilterType.OBSERVATION.getKey());
			}
			query.append("|"); //$NON-NLS-1$
		}
		
		for (Object item : items){
			if (item instanceof DropItem){
				DropItem it = (DropItem)item;
				query.append(it.asQueryPart());
				query.append(" "); //$NON-NLS-1$
			}
		}
		return query.toString();
	}
	
	/**
	 * 
	 * @return attribute tree viewer for drop down tree attributes
	 */
	public TreeDropDownViewer getTreeEditor(){
		if (treeEditor == null){
			treeEditor = new TreeDropDownViewer(parentView.getSite().getShell());
		}
		return this.treeEditor;
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
	 * Adds a drop item to the query formula
	 * @param item drop item to add
	 */
	public void addElement(DropItem item) {
		item.createWidget(this);
		
		if (items.size() > 0){
			if (!(item instanceof NotDropItem || item instanceof BracketDropItem)){
				DropItem it = DropItemFactory.INSTANCE.createBooleanOpDropItem();
				it.createWidget(this);
				items.add(it);	
			}
		}
		items.add(item);
		orderElements();
		validate();
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
		if (di.targetPanel == null && !items.contains(di)){
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
	
	/**
	 * Creates the drop target composite
	 * @param parent
	 * @return
	 */
	public Composite createComposite(Composite parent) {
		parent = new Composite(parent, SWT.NONE);
		parent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		parent.setLayout(layout);
		
		Composite filterTypeComp = new Composite(parent, SWT.NONE);
		filterTypeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		layout = new GridLayout(4, false);
		layout.horizontalSpacing = 5;
		layout.verticalSpacing = 0;
		layout.marginWidth = 5;
		layout.marginHeight = 3;
		
		filterTypeComp.setLayout(layout);
		Label l = new Label(filterTypeComp, SWT.NONE);
		l.setText(Messages.FilterDropTargetPanel_FilterTypeLabel);
		l.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false));
		
		Listener selectListener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				validate();
				fireQueryChangedListeners();
			}
		};
		btnWaypoint = new Button(filterTypeComp, SWT.RADIO);
		btnWaypoint.setText(IFilter.FilterType.WAYPOINT.getGuiName());
		btnWaypoint.setToolTipText(Messages.FilterDropTargetPanel_waypointtooltip);
		btnWaypoint.setSelection(true);
		btnWaypoint.addListener(SWT.Selection, selectListener);
		
		btnObservation = new Button(filterTypeComp, SWT.RADIO);
		btnObservation.setText(IFilter.FilterType.OBSERVATION.getGuiName());
		btnObservation.addListener(SWT.Selection, selectListener);
		btnObservation.setToolTipText(Messages.FilterDropTargetPanel_observationtooltip);
		Label lspacer = new Label(filterTypeComp, SWT.NONE);
		lspacer.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		
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
				if (dragItem == null){
					StructuredSelection selection = (StructuredSelection) LocalSelectionTransfer.getTransfer().getSelection();
					if (selection == null) {
						return;
					}
					//hide drop item and setup proxy
					dragItem = (DropItem)selection.getFirstElement();
				}
				if (!targetPanels.contains(dragItem.targetPanel)){
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
				if (proxy.getWidget().isVisible() && dragItem.targetPanel != FilterDropTargetPanel.this){
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

				if (dragItem.targetPanel != FilterDropTargetPanel.this){
					IDropPanel target =  dragItem.targetPanel;
					dragItem.moveParent(FilterDropTargetPanel.this);
					target.finishDrag(dragItem);
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
				validate();
				for(FilterDropTargetPanel target : targetPanels){
					target.dragItem = null;
				}

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
		
		return parent;
	}


	@Override
	public void fireQueryChangedListeners() {
		getParentView().fireQueryModifiedListeners();
	}


	/* (non-Javadoc)
	 * @see org.wcs.smart.query.ui.formulaDnd.IDropPanel#layout()
	 */
	@Override
	public void layout() {
		orderElements();
	}
}

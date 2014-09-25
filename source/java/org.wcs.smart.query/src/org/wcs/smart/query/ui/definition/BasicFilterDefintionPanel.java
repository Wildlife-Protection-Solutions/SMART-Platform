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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.wcs.smart.query.common.model.GriddedQuery;
import org.wcs.smart.query.common.model.SimpleQuery;
import org.wcs.smart.query.common.model.SummaryQuery;
import org.wcs.smart.query.event.QueryEventManager;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.QueryProxy;
import org.wcs.smart.query.model.filter.IFilter;
import org.wcs.smart.query.model.filter.IFilter.FilterType;
import org.wcs.smart.query.ui.TreeDropDownViewer;
import org.wcs.smart.query.ui.model.DropItem;
import org.wcs.smart.query.ui.model.IDefinitionPanel;
import org.wcs.smart.query.ui.model.ProxyItem;
import org.wcs.smart.query.ui.model.impl.BasicDropItemFactory;
import org.wcs.smart.query.ui.model.impl.BracketDropItem;
import org.wcs.smart.query.ui.model.impl.NotDropItem;

/**
 * Simple filter definition panel.
 * @author Emily
 *
 */
public class BasicFilterDefintionPanel implements IDefinitionPanel {

	public static final String ID = "org.wcs.smart.query.common.definition.filter"; //$NON-NLS-1$
	final static Transfer[] types = new Transfer[] { LocalSelectionTransfer.getTransfer() };
	
	private Composite mainComposite;
	private ScrolledComposite dropTarget = null;
	private Composite dropTargetContent;
	
	private ProxyItem proxy = null;	//drag proxy item

	protected ArrayList<DropItem> items = new ArrayList<DropItem>();	//list of controls in formula
	protected QueryProxy currentQuery = null;
	
	private TreeDropDownViewer treeEditor = null;
	private Set<IDefinitionPanel> targetPanels;
	
	private DropItem dragItem;
	
	private Button btnWaypoint;
	private Button btnObservation;
	
	/**
	 * Creates a new drop target panel.
	 * 
	 */
	public BasicFilterDefintionPanel(){
		this.targetPanels = new HashSet<IDefinitionPanel>();
		targetPanels.add(this);
	}
	
	/**
	 * Return the unique identifier for the panel
	 */
	public String getId(){
		return ID;
	}
	
	/**
	 * Return the gui name
	 */
	public String getGuiName(){
		return Messages.BasicFilterDefintionPanel_FiltersPanelName;
	}
	
	/**
	 * adds addition panels as possible drop targets
	 * @param panel
	 */
	public void addDropTargetPanel(IDefinitionPanel panel){
		this.targetPanels.add(panel);
	}
	
	/**
	 * @return 
	 */
	public static Transfer[] getTransferTypes() {
		return types;
	}

	/**
	 * Sets the filter type
	 * @param filterType
	 */
	public void setFilterType(IFilter.FilterType filterType){
		if (filterType.equals(FilterType.WAYPOINT)){
			btnWaypoint.setSelection(true);
			btnObservation.setSelection(false);
		}else{
			btnObservation.setSelection(true);
			btnWaypoint.setSelection(false);
		}
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
	public String getQueryPart(){
		StringBuilder query = new StringBuilder();
		
		if (btnWaypoint != null){
			if (items.size() > 0){
				//if non-empty filter then include filter type
				if (btnWaypoint.getSelection()){
					query.append(IFilter.FilterType.WAYPOINT.getKey());
				}else{
					query.append(IFilter.FilterType.OBSERVATION.getKey());
				}
				query.append("|"); //$NON-NLS-1$
			}
		}
		
		for (Object item : items){
			if (item instanceof DropItem){
				DropItem it = (DropItem)item;
				query.append(it.asQueryPart());
				query.append(" "); //$NON-NLS-1$
			}
		}
		return query.toString().trim();
	}
	
	/**
	 * 
	 * @return attribute tree viewer for drop down tree attributes
	 */
	public TreeDropDownViewer getTreeEditor(){
		if (treeEditor == null){
			treeEditor = new TreeDropDownViewer(getDropTargetComposite().getShell());
		}
		return this.treeEditor;
	}
	
	/**
	 * Validates the items in the filter panel 
	 */
	public String validate(){
		return null;
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
	 * Adds a drop item to the query formula; fires a query changed
	 *  event after item added
	 * @param item drop item to add
	 */
	public void addItem(DropItem item) {
		item.createWidget(this, dropTargetContent);
		
		if (items.size() > 0){
			if (!(item instanceof NotDropItem || item instanceof BracketDropItem)){
				DropItem it = BasicDropItemFactory.createBooleanOpDropItem();
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
	
	public void redraw(){
		orderElements();
	}
	
	/**
	 * Adds the filter type options to the parent composite.
	 * @param parent
	 * @return
	 */
	protected void createFilterTypeComposite(Composite parent){
		Composite filterTypeComp = new Composite(parent, SWT.NONE);
		filterTypeComp.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		GridLayout layout = new GridLayout(4, false);
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
		
	}
	
	/**
	 * Creates the drop target composite
	 * @param parent
	 * @return
	 */
	@Override
	public Composite createComposite(Composite parent) {
		
		mainComposite = new Composite(parent, SWT.NONE);
		mainComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout();
		layout.horizontalSpacing = 0;
		layout.verticalSpacing = 0;
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		mainComposite.setLayout(layout);
		
		createFilterTypeComposite(mainComposite);
		
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
				if (!targetPanels.contains(dragItem.getTargetPanel())){
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
				if (proxy.getWidget().isVisible() && dragItem.getTargetPanel() != BasicFilterDefintionPanel.this){
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

				if (dragItem.getTargetPanel() != BasicFilterDefintionPanel.this){
					BasicFilterDefintionPanel target =  (BasicFilterDefintionPanel) dragItem.getTargetPanel();
					dragItem.moveParent(BasicFilterDefintionPanel.this);
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
				for(IDefinitionPanel target : targetPanels){
					((BasicFilterDefintionPanel)target).dragItem = null;
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
		proxy.createWidget(this, dropTargetContent);
		proxy.getWidget().setVisible(false);
		
		return mainComposite;
	}



	public void fireQueryChangedListeners() {
		QueryEventManager.getInstance().fireQueryDefinitionModified(currentQuery.getQuery());
	}


	@Override
	public void saveItems(QueryProxy q){
		List<DropItem> duplicate = new ArrayList<DropItem>();
		duplicate.addAll(items);
		q.setDropItems(getId(), duplicate);
	}

	@Override
	public void initItems(QueryProxy q){
		this.currentQuery = q;
		addItems(q.getDropItems(getId()));
		
		if (q.getQuery() instanceof SimpleQuery){
			setFilterType( ((SimpleQuery)q.getQuery()).getFilter().getFilterType() );
		}else if (q.getQuery() instanceof GriddedQuery){
			if ( ((GriddedQuery)q.getQuery()).getQueryDefinition() != null &&
					((GriddedQuery)q.getQuery()).getQueryDefinition().getValueFilter() != null){
				setFilterType( ((GriddedQuery)q.getQuery()).getQueryDefinition().getValueFilter().getFilterType() );
			}else{
				setFilterType(FilterType.WAYPOINT);
			}
		}else if (q.getQuery() instanceof SummaryQuery){
			if ( ((SummaryQuery)q.getQuery()).getQueryDefinition() != null && 
					((SummaryQuery)q.getQuery()).getQueryDefinition().getValueFilter() != null){
				setFilterType( ((SummaryQuery)q.getQuery()).getQueryDefinition().getValueFilter().getFilterType() );
			}else{
				setFilterType(FilterType.WAYPOINT);
			}
		}
	}

	@Override
	public Composite getDropTargetComposite() {
//		return mainComposite;
		return dropTargetContent;
	}


}

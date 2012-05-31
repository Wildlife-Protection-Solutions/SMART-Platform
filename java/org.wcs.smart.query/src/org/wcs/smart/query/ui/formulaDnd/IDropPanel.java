package org.wcs.smart.query.ui.formulaDnd;

import org.eclipse.swt.widgets.Composite;

/**
 * Drop panel for placing drop items
 * @author egouge
 * @since 1.0.0
 */
public interface IDropPanel {

	
	/**
	 * Remove an element from the drop panel
	 * @param item
	 */
	public void removeElement(DropItem item);
	
	/**
	 * Validate the items in the drop panel
	 */
	public void validate();
	
	/**
	 * Called when an item is finished being dragged.
	 * 
	 * @param item the item being dragged
	 */
	public void finishDrag(DropItem item);
	
	/**
	 * fires any listeners associated with the drop panel
	 */
	public void fireQueryChangedListeners();
	
	/**
	 * re-layouts items in the drop panel
	 */
	public void layout();
	
	/**
	 * @return the drop panel widget
	 */
	public Composite getComposite();
	
	/**
	 * @return the part of the query string represented by the drop panel
	 */
	public String getQueryString();
	
	
}

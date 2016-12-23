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
package org.wcs.smart.i2.ui.views.query.dropitem;

import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Composite;

/**
 * Drop panel for placing drop items
 * 
 * @author egouge
 * @since 1.0.0
 */
public interface IDefinitionPanel {
	
	public final static Transfer[] DND_TYPES = new Transfer[] { LocalSelectionTransfer.getTransfer() };
	
	/**
	 * Add a drop item to the definition panel
	 * @param item the drop item to add
	 */
	public abstract void addItem(DropItem item);
	
	/**
	 * Remove an element from the definition panel
	 * @param item
	 */
	public void removeItem(DropItem item);
	
	/**
	 * Validate the items in the drop panel.
	 * This validates specific items in the panel; it
	 * does not have to validate the entire query
	 * 
	 * @return null if no error otherwise error string
	 */
	public String validate();
	
	/**
	 * Creates the composite for the drop panel.
	 * @param parent
	 * @return
	 */
	public Composite createComposite(Composite parent);
	
	/**
	 * @return the drop target widget - where drop items
	 * should be added
	 */
	public Composite getDropTargetComposite();
	
	/**
	 * @return the drop panel widget
	 */
	public void dispose();
	
	/**
	 * @return the part of the query string represented by the drop panel
	 */
	public String getQueryPart();
		
	/**
	 * Clears all items in the panels. 
	 */
	public void clear();
	
	/**
	 * Re-draws all elements in the panel 
	 */
	public void redraw();

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
		

	
}

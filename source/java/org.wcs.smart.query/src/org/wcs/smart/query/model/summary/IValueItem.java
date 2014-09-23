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
package org.wcs.smart.query.model.summary;

import org.hibernate.Session;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.IValueVisitor;
import org.wcs.smart.query.ui.model.DropItem;


/**
 * Interface that represents a value item
 * in a summary query.
 * 
 * @author egouge
 * @since 1.0.0
 */
public interface IValueItem {
	
	public enum ValueType{
		OBSERVATION("obs", Messages.CategoryValueItem_CountObservationLabel), //$NON-NLS-1$
		WAYPOINT("wp", Messages.CategoryValueItem_CountIncidentLabel); //$NON-NLS-1$
		
		public String key;
		public String guiLabel;
		
		private ValueType(String key, String guiLabel){
			this.key = key;
			this.guiLabel = guiLabel;
		}
	}	
	
	/**
	 * @return the string representation of the item
	 */
	public String asString();

	/**
	 * Given a hibernate connection returns the 
	 * human readable representation of the name
	 * for the value item.  This is displayed in the
	 * results table.
	 * 
	 * @param session hibernate session
	 * @return 
	 */
	public String getName(Session session);
	
	/**
	 * Given a hibernate connection returns the 
	 * full human readable representation of the name
	 * for the value item. 
	 * <p>This may be the same as the name or it may contain
	 * more information for tooltips</p>
	 * 
	 * @param session hibernate session
	 * @return 
	 */
	public String getFullName(Session session);
	
	/**
	 * Converts the value item to a drop item
	 * @param session
	 * @return
	 */
	public DropItem asDropItem(Session session) throws Exception;

	/**
	 * @return the object used to initailize the drop item
	 */
	public Object getDropItemInitializeData();

	/**
	 * process the given visitor
	 * @param visitor
	 */
	public void accept(IValueVisitor visitor);
}

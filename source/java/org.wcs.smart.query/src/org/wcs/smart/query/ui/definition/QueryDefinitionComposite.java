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

import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.ui.ca.datamodel.dropitem.DropItem;

/**
 * Abstract class for a query definition 
 * composite to be used in the query
 * area.
 * 
 * @author egouge
 * @since 1.0.0
 */
public abstract class QueryDefinitionComposite extends Composite{

	/**
	 * @param parent
	 * @param style
	 */
	public QueryDefinitionComposite(Composite parent, int style) {
		super(parent, style);
	}

	/**
	 * Clears the query
	 */
	public abstract void clear();
	
	/**
	 * Validates the query
	 * @return <code>null</code> if validates okay or a message 
	 * describing query error
	 */
	public abstract String validate();
	

	
	/**
	 * Add a drop item to the query
	 * @param item the drop item to add
	 */
	public abstract void addItem(DropItem item);
	
	/**
	 * Called when the panel is made visible. 
	 */
	protected abstract void visible();

}

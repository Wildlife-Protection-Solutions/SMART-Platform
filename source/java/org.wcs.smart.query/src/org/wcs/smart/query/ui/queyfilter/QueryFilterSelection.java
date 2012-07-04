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
package org.wcs.smart.query.ui.queyfilter;

import java.util.Iterator;
import java.util.List;

import org.eclipse.jface.viewers.IStructuredSelection;

/**
 * Represents an item selected in the 
 * the query filter tree.  Identified which item was selected
 * and if the item is part of a query filter or a summary item.
 * 
 * @author egouge
 * @since 1.0.0
 */
public class QueryFilterSelection implements IStructuredSelection{

	/**
	 * Types of items. Items are either
	 * filter items (used to filter results) or summary items (used
	 * to build summary queries)
	 * @author Emily
	 *
	 */
	public enum FilterType{
		SUMMARY, FILTER
	};
	
	private FilterType type;
	private IStructuredSelection selection;
	
	/**
	 * Creates a new selection
	 * @param selection the selected item
	 * @param type the type of item selected
	 */
	public QueryFilterSelection(IStructuredSelection selection, FilterType type){
		this.type = type;
		this.selection = selection;
	}
	
	/**
	 * @return the type of item selected
	 */
	public FilterType getType(){
		return this.type;
	}
	
	/**
	 * @see org.eclipse.jface.viewers.ISelection#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return selection.isEmpty();
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredSelection#getFirstElement()
	 */
	@Override
	public Object getFirstElement() {
		return selection.getFirstElement();
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredSelection#iterator()
	 */
	@Override
	public Iterator iterator() {
		return selection.iterator();
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredSelection#size()
	 */
	@Override
	public int size() {
		return selection.size();
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredSelection#toArray()
	 */
	@Override
	public Object[] toArray() {
		return selection.toArray();
	}

	/**
	 * @see org.eclipse.jface.viewers.IStructuredSelection#toList()
	 */
	@Override
	public List toList() {
		return selection.toList();
	}

	
}

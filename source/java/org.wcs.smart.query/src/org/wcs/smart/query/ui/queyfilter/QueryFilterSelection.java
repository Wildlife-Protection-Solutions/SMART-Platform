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
 * TODO Purpose of 
 * <p>
 * <ul>
 * <li></li>
 * </ul>
 * </p>
 * @author egouge
 * @since 1.0.0
 */
public class QueryFilterSelection implements IStructuredSelection{

	public enum FilterType{
		SUMMARY, FILTER
	};
	
	private FilterType type;
	private IStructuredSelection selection;
	
	public QueryFilterSelection(IStructuredSelection selection, FilterType type){
		this.type = type;
		this.selection = selection;
	}
	
	public FilterType getType(){
		return this.type;
	}
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.ISelection#isEmpty()
	 */
	@Override
	public boolean isEmpty() {
		return selection.isEmpty();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredSelection#getFirstElement()
	 */
	@Override
	public Object getFirstElement() {
		return selection.getFirstElement();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredSelection#iterator()
	 */
	@Override
	public Iterator iterator() {
		return selection.iterator();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredSelection#size()
	 */
	@Override
	public int size() {
		return selection.size();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredSelection#toArray()
	 */
	@Override
	public Object[] toArray() {
		return selection.toArray();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IStructuredSelection#toList()
	 */
	@Override
	public List toList() {
		return selection.toList();
	}

	
}

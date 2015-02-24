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
package org.wcs.smart.dataentry.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;

/**
 * List that is linked with original list but contains only elements 
 * that match given criteria. All operations performed on filtered list
 * will be performed on a source list as well.
 * 
 * @author elitvin
 * @since 3.0.0
 */
public abstract class FilteredSubList<T> implements List<T> {
	
	private List<T> source;
	private List<T> filteredList;
	
	public FilteredSubList(List<T> source) {
		this.source = source;
		filteredList = new ArrayList<T>();
		for (T t : source) {
			if (matches(t))
				filteredList.add(t);
		}
	}
	
	protected abstract boolean matches(T t);

	@Override
	public boolean add(T e) {
		filteredList.add(e);
		return source.add(e);
	}

	@Override
	public void add(int index, T element) {
		int sourceIndex = source.indexOf(filteredList.get(index));
		if (sourceIndex < 0) {
			throw new IllegalStateException("Item exists in filtered list but not in source list"); //$NON-NLS-1$
		}
		filteredList.add(index, element);
		source.add(sourceIndex, element);
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		filteredList.addAll(c);
		return source.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends T> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void clear() {
		source.removeAll(filteredList);
		filteredList.clear();
	}

	@Override
	public boolean contains(Object o) {
		return filteredList.contains(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return filteredList.containsAll(c);
	}

	@Override
	public T get(int index) {
		return filteredList.get(index);
	}

	@Override
	public int indexOf(Object o) {
		return filteredList.indexOf(o);
	}

	@Override
	public boolean isEmpty() {
		return filteredList.isEmpty();
	}

	@Override
	public Iterator<T> iterator() {
		return new Iterator<T>() {
			private int cursor = 0;

			@Override
			public boolean hasNext() {
				return cursor != filteredList.size();
			}

			@Override
			public T next() {
				int i = cursor;
				if (i >= filteredList.size())
					throw new NoSuchElementException();
				cursor = i + 1;
				return filteredList.get(i);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
	}

	@Override
	public int lastIndexOf(Object o) {
		return filteredList.lastIndexOf(o);
	}

	@Override
	public ListIterator<T> listIterator() {
		throw new UnsupportedOperationException();
	}

	@Override
	public ListIterator<T> listIterator(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean remove(Object o) {
		filteredList.remove(o);
		return source.remove(o);
	}

	@Override
	public T remove(int index) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		filteredList.removeAll(c);
		return source.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		throw new UnsupportedOperationException();
	}

	@Override
	public T set(int index, T element) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int size() {
		return filteredList.size();
	}

	@Override
	public List<T> subList(int fromIndex, int toIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Object[] toArray() {
		return filteredList.toArray();
	}

	@SuppressWarnings("hiding")
	@Override
	public <T> T[] toArray(T[] a) {
		return filteredList.toArray(a);
	}

}

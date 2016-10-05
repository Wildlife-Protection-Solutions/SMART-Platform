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
package org.wcs.smart.i2.ui.views;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetItem;

/**
 * Content provider for working set tree.
 * 
 * @author Emily
 *
 */
public class WorkingSetTreeContentProvider implements ITreeContentProvider {

	private Map<IntelWorkingSetCategory, List<IntelWorkingSetItem>> content = null;
	
	@Override
	public void dispose() {
	}

	@SuppressWarnings("unchecked")
	@Override
	public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		if (newInput instanceof List<?>){
			List<IntelWorkingSetItem> test = ((List<IntelWorkingSetItem>)newInput);
			content = test
						.stream()
						.collect(Collectors.groupingBy(IntelWorkingSetItem::getCategory,Collectors.toList()));
			
		}
	}

	@Override
	public Object[] getElements(Object inputElement) {
		return IntelWorkingSetCategory.values();
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof IntelWorkingSetCategory){
			if (content.get(parentElement) == null) return null;
			return content.get(parentElement).stream().toArray(IntelWorkingSetItem[]::new);
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof IntelWorkingSetCategory) return false;
		if (element instanceof IntelWorkingSetItem) return ((IntelWorkingSetItem) element).getCategory();
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof IntelWorkingSetCategory) return true;
		if (element instanceof IntelWorkingSetItem) return false;
		return false;
	}

}

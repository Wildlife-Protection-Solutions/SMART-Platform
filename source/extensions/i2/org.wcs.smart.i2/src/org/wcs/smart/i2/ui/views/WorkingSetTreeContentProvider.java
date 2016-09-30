package org.wcs.smart.i2.ui.views;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.wcs.smart.i2.model.IntelWorkingSetCategory;
import org.wcs.smart.i2.model.IntelWorkingSetItem;

public class WorkingSetTreeContentProvider implements ITreeContentProvider {

	private Map<IntelWorkingSetCategory, List<IntelWorkingSetItem>> content = null;
	
	@Override
	public void dispose() {
	}

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

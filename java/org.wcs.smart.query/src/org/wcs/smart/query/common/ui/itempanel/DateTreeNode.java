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
package org.wcs.smart.query.common.ui.itempanel;

import java.util.List;
import java.util.Locale;

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartContext;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.filter.date.DateGroupByViewer;
import org.wcs.smart.query.model.filter.date.IQueryDateLabelProvider;

/**
 * Tree node for displaying date options.  
 * @author Emily
 *
 */
public class DateTreeNode implements IItemTreeNode {

	public static final String KEY ="dategroupby"; //$NON-NLS-1$

	
	private final static LabelProvider lblProvider = new LabelProvider(){
		public String getText(Object element){
			if (element instanceof DateGroupByViewer){
				return SmartContext.INSTANCE.getClass(IQueryDateLabelProvider.class).getLabel(
						((DateGroupByViewer) element).getGroupBy().getOption(),
						Locale.getDefault());
			}
			return super.getText(element);
		}
		
		public Image getImage(Object element){
			if (element instanceof DateGroupByViewer){
				return ((DateGroupByViewer) element).getImage();
			}
			return super.getImage(element);
		}
		
	};
	
	private DateContentProider contentProvider;
	
	@Override
	public String getName() {
		return Messages.DateTreeNode_TreeNodeLabel;
	}

	@Override
	public Image getImage() {
		return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.CALENDAR_ICON);
	}

	@Override
	public ITreeContentProvider getContentProvider() {
		if (contentProvider == null){
			contentProvider = new DateContentProider();
		}
		return contentProvider;
	}

	@Override
	public ILabelProvider getLabelProvider() {
		return lblProvider;
	}

	@Override
	public String getKey() {
		return KEY;
	}

	
	class DateContentProider implements ITreeContentProvider{

		
		private List<DateGroupByViewer> groupbys;
		
		
		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			groupbys = (List<DateGroupByViewer>) newInput;

		}

		@Override
		public Object[] getElements(Object inputElement) {
			return groupbys.toArray(new Object[groupbys.size()]);
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return null;
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return false;
		}
		
	}
}

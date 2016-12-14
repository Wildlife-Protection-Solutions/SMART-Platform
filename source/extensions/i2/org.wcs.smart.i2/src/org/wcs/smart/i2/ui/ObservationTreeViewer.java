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
package org.wcs.smart.i2.ui;

import java.util.Locale;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.wcs.smart.i2.model.IntelObservation;
import org.wcs.smart.i2.model.IntelObservationAttribute;

/**
 * Tree table viewer for displaying observations.
 * 
 * @author Emily
 *
 */
public class ObservationTreeViewer {

	private TreeViewer viewer;
	
	public ObservationTreeViewer(Composite parent, int style){
		
		viewer = new TreeViewer(parent, style);
		viewer.setContentProvider(new ObservationContentProvider());
		viewer.getTree().setHeaderVisible(true);
		
		TreeViewerColumn categoryCol = new TreeViewerColumn(viewer, SWT.NONE);
		categoryCol.getColumn().setWidth(150);
		categoryCol.getColumn().setText("Category");
		categoryCol.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof IntelObservation){
					return ((IntelObservation)element).getCategory().getName();
				}else if (element instanceof IntelObservationAttribute){
					return ((IntelObservationAttribute)element).getAttribute().getName();
				}
				return super.getText(element);
			}
		});
		
		TreeViewerColumn attributeCol = new TreeViewerColumn(viewer, SWT.NONE);
		attributeCol.getColumn().setWidth(800);
		attributeCol.getColumn().setText("Attribute");
		attributeCol.setLabelProvider(new ColumnLabelProvider(){
			public String getText(Object element){
				if (element instanceof IntelObservationAttribute){
					return ((IntelObservationAttribute)element).getAttributeValueAsString(Locale.getDefault());
				}
				return "";
			}
		});
	}
	
	public void setInput(Object input){
		viewer.setInput(input);
	}
	
	public void expandToLevel(int level){
		viewer.expandToLevel(level);
	}
	
	public TreeViewer getViewer(){
		return this.viewer;
	}
}

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
package org.wcs.smart.query.common.ui;

import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.model.QueryColumn;

/**
 * Label provider for simple query column
 * @author Emily
 *
 */
public class QueryColumnLabelProvider extends ColumnLabelProvider{

	protected QueryColumn column;

	protected boolean isEditing;
	
	private Color lightRed;
	
	public QueryColumnLabelProvider(QueryColumn col){
		this.column = col;
	}

	@Override
	public void dispose(){
		super.dispose();
		if (lightRed != null){
			lightRed.dispose();
			lightRed = null;
		}
	}
	@Override
	public String getText(Object element){
		if (element == null) return "";
		if (element instanceof IResultItem){
			return column.getValueAsString( column.getValue((IResultItem)element));
		}
		return element.toString();
	}
	
	@Override
	public Color getForeground(Object element) {
		if (!isEditing) return null;
		if (column.canEdit()){
			return Display.getDefault().getSystemColor(SWT.COLOR_DARK_RED);
		}
		return Display.getDefault().getSystemColor(SWT.COLOR_GRAY);
	}
	
	@Override
	public Color getBackground(Object element) {
		if (!isEditing) return null;
		if (column.canEdit()){
			return lightRed;
		}
		return null;
	}
	
	
	public void setEditMode(boolean isEditing){
		if (isEditing && lightRed == null){
			lightRed = new Color(Display.getDefault(),255,230,230);
		}
		this.isEditing = isEditing;
	}
}
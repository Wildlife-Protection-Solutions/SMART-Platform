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

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.model.AbstractIntelQuery;
import org.wcs.smart.i2.model.IntelEntitySummaryQuery;
import org.wcs.smart.i2.model.IntelRecordObservationQuery;
import org.wcs.smart.i2.ui.views.QueryProxy;

/**
 * Label provider for profile queries 
 * 
 * @author Emily
 *
 */
public class IntelQueryLabelProvider extends LabelProvider {
	
	public String getText(Object element){
		if (element instanceof QueryProxy) return ((QueryProxy) element).getName();
		if (element instanceof AbstractIntelQuery) return ((AbstractIntelQuery)element).getName();
		return super.getText(element);
	}
	
	public Image getImage(Object element) {
		String queryType = null;
		if (element instanceof AbstractIntelQuery) {
			queryType = ((AbstractIntelQuery) element).getTypeKey();
		}else if (element instanceof QueryProxy) {
			queryType = ((QueryProxy)element).getTypeKey();
		}
		if (queryType == null) return null;
		
		
		if (queryType.equalsIgnoreCase(IntelRecordObservationQuery.KEY)) {
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_RECORD);
		}else if (queryType.equalsIgnoreCase(IntelEntitySummaryQuery.KEY)) {
			return Intelligence2PlugIn.getDefault().getImageRegistry().get(Intelligence2PlugIn.ICON_ENTITY);
		}
		return null;
	}
}

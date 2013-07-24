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
package org.wcs.smart.query.ui.queryfilter;

import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.Area;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.query.parser.IPatrolQueryOption;
import org.wcs.smart.query.ui.queryfilter.QueryFilterContentProvider.DataModelItem;
import org.wcs.smart.ui.properties.DataModelLabelProvider;

/**
 * Label provider for query filte tree.
 * @author Emily
 * @since 1.0.0
 */
public class QueryFilterLabelProvider extends LabelProvider {

	
	private DataModelLabelProvider dmLabelProvider = new DataModelLabelProvider();
	
	/**
	 * Creates a new label provider
	 */
	public QueryFilterLabelProvider(){
	}
	
	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getImage(java.lang.Object)
	 */
	@Override 
	public Image getImage(Object element) {
		if (element instanceof IPatrolQueryOption){
			return ((IPatrolQueryOption) element).getImage();
		}else if (element instanceof QueryFilterContentProvider.RootNode){
			return ((QueryFilterContentProvider.RootNode)element).getImage();
		}else if (element instanceof Area.AreaType || element instanceof Area){
			return QueryPlugIn.getDefault().getImageRegistry().get(QueryPlugIn.AREA_POLYGON_FILTER_ICON);
		}else if (element instanceof QueryFilterContentProvider.DataModelItem){
			if (element == DataModelItem.CATEGORIES){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
			}else if (element == DataModelItem.ATTRIBUTES){
				return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.ATTRIBUTE_NUMBER_ICON);
			}
			return null;
		}else{
			Image img =  dmLabelProvider.getImage(element);
			if (img != null){
				return img;
			}
		}
		return null;
	}
	
	
	/**
	 * @see org.eclipse.jface.viewers.LabelProvider#getText(java.lang.Object)
	 */
	@Override
	public String getText(Object element) {
		if (element instanceof IPatrolQueryOption){
			return ((IPatrolQueryOption)element).getGuiName();
		}else if (element instanceof QueryFilterContentProvider.RootNode){
			return ((QueryFilterContentProvider.RootNode)element).getName();
//			return getStyledText(element).getString();
		//}else if (area FILTER);
		}else if (element instanceof String){
			return (String) element;
		}else if (element instanceof Area.AreaType){
			return ((Area.AreaType) element).getGuiName();
		}else if (element instanceof Area){
			return ((Area) element).getName();
		}else if (element instanceof QueryFilterContentProvider.OtherItems){
			return ((QueryFilterContentProvider.OtherItems) element).guiName;
		}else if (element instanceof QueryFilterContentProvider.DataModelItem){
			return ((QueryFilterContentProvider.DataModelItem) element).guiName;
		}else {
			return dmLabelProvider.getText(element);
		}
	}


}

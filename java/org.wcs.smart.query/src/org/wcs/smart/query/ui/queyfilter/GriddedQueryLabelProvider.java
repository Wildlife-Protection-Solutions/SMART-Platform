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

import java.text.MessageFormat;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.parser.PatrolQueryOptions;
import org.wcs.smart.query.parser.PatrolQueryOptions.DateGroupByOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.query.parser.PatrolQueryOptions.PatrolValueOption;

/**
 *  Label provider for gridded queries
 * @author jeff
 * @since 1.0.0
 */
public class GriddedQueryLabelProvider extends QueryFilterLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof GriddedQueryContentProvider.RootNode) {
			return ((GriddedQueryContentProvider.RootNode) element).getImage();
		}else if (element instanceof DateGroupByOption){
			return PatrolQueryOptions.getImage((DateGroupByOption)element);
		}else if (element instanceof SummaryDmObject){
			return super.getImage(((SummaryDmObject) element).getObject());
		}else if (element instanceof PatrolValueOption){
			return ((PatrolValueOption) element).getIcon();
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof PatrolValueOption) {
			return ((PatrolValueOption) element).getGuiName();
		} else if (element instanceof PatrolQueryOption) {
			return ((PatrolQueryOption) element).getGuiName();
		} else if (element instanceof GriddedQueryContentProvider.RootNode) {
			return ((GriddedQueryContentProvider.RootNode) element).getName();
		} else if (element instanceof DateGroupByOption) {
			return ((DateGroupByOption) element).getGuiName();
		} else if (element instanceof SummaryDmObject){
			SummaryDmObject obj = (SummaryDmObject)element;
			if (obj.getObject() instanceof Attribute){
				return ((Attribute)obj.getObject()).getName();
			}else if (obj.getObject() instanceof AttributeTreeNode){
				return ((AttributeTreeNode)obj.getObject()).getName();
			}else if (obj.getObject() instanceof Category){
				if (obj.isValue()){
					return MessageFormat.format(Messages.GriddedQueryLabelProvider_CountCategoryLabel,new Object[]{((Category)obj.getObject()).getName()});
				}else{
					return ((Category)obj.getObject()).getName();
				}
			}else if (obj.getObject() instanceof CategoryAttribute){
				return ((CategoryAttribute)obj.getObject()).getAttribute().getName();
			}
			
		}
		return super.getText(element);

	}
}

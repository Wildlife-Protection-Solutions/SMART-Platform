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

package org.wcs.smart.patrol.query.ui.itempanel;

import java.text.MessageFormat;

import org.eclipse.swt.graphics.Image;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.ca.datamodel.Category;
import org.wcs.smart.ca.datamodel.CategoryAttribute;
import org.wcs.smart.patrol.query.internal.Messages;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolQueryOption;
import org.wcs.smart.patrol.query.parser.PatrolQueryOptions.PatrolValueOption;
import org.wcs.smart.query.model.AllCategory;
import org.wcs.smart.query.model.filter.date.IDateGroupBy;

public class SummaryQueryLabelProvider extends QueryFilterLabelProvider {

	@Override
	public Image getImage(Object element) {
		if (element instanceof SummaryQueryContentProvider.RootNode) {
			return ((SummaryQueryContentProvider.RootNode) element).getImage();
		}else if (element instanceof IDateGroupBy){
			return ((IDateGroupBy)element).getImage();
		}else if (element instanceof SummaryDmObject){
			return super.getImage(((SummaryDmObject) element).getObject());
		}else if (element instanceof PatrolValueOption){
			return ((PatrolValueOption) element).getIcon();
		}else if (element instanceof AllCategory){
			return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.CATEGORY_ICON);
		}
		return super.getImage(element);
	}

	@Override
	public String getText(Object element) {
		if (element instanceof PatrolValueOption) {
			return ((PatrolValueOption) element).getGuiName();
		} else if (element instanceof PatrolQueryOption) {
			return ((PatrolQueryOption) element).getGuiName();
		} else if (element instanceof SummaryQueryContentProvider.RootNode) {
			return ((SummaryQueryContentProvider.RootNode) element).getName();
		} else if (element instanceof IDateGroupBy) {
			return ((IDateGroupBy) element).getGuiName();
		}else if (element instanceof AllCategory){
			return MessageFormat.format(Messages.SummaryQueryLabelProvider_CountCatLabel, new Object[]{((AllCategory)element).getName()});
		} else if (element instanceof SummaryDmObject){
			SummaryDmObject obj = (SummaryDmObject)element;
			if (obj.getObject() instanceof Attribute){
				return ((Attribute)obj.getObject()).getName();
			}else if (obj.getObject() instanceof AttributeTreeNode){
				String name = ((AttributeTreeNode)obj.getObject()).getName();
				if (obj.isValue()){
					return MessageFormat.format(Messages.SummaryQueryLabelProvider_CountCatLabel, new Object[]{name});
				}else{
					return name;
				}
			}else if (obj.getObject() instanceof AttributeListItem){
				String name = ((AttributeListItem)obj.getObject()).getName();
				if (obj.isValue()){
					return MessageFormat.format(Messages.SummaryQueryLabelProvider_CountCatLabel, new Object[]{name});
				}else{
					return name;
				}
			}else if (obj.getObject() instanceof Category){
				if (obj.isValue()){
					return MessageFormat.format(Messages.SummaryQueryLabelProvider_CountCatLabel, new Object[]{((Category)obj.getObject()).getName()});
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

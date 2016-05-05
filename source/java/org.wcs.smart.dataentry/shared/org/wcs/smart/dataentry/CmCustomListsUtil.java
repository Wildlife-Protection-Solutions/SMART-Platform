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
package org.wcs.smart.dataentry;

import java.util.ArrayList;
import java.util.List;

import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Util class for configurable model custom lists manipulations
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class CmCustomListsUtil {

	public static List<CmAttributeListItem> buildCustomList(ConfigurableModel model, CmAttribute cmAttr, Attribute dmAttr) {
		List<AttributeListItem> source = dmAttr.getActiveListItems();
		List<CmAttributeListItem> result = new ArrayList<>(source.size());
		for (AttributeListItem dmNode : source) {
			CmAttributeListItem cmNode = new CmAttributeListItem();
			cmNode.setConfigurableModel(model);
			cmNode.setListItem(dmNode);
			cmNode.setIsActive(dmNode.getIsActive());
			cmNode.setListOrder(dmNode.getListOrder());
			cmNode.setAttribute(cmAttr);
			result.add(cmNode);
		}
		return result;
	}
	
}

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
package org.wcs.smart.connect.dataentry;

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.dialog.ConfigurableModelLabelProvider;
import org.wcs.smart.dataentry.dialog.composite.CmListItemLabelProvider;
import org.wcs.smart.dataentry.dialog.composite.CmTreeLabelProvider;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Label provider for configurable model tree used in connect tab.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCmTreeLabelProvider extends LabelProvider implements IColorProvider {
	
	private ConfigurableModelLabelProvider cmLabelProvider;
	private CmListItemLabelProvider liLabelProvider;
	private CmTreeLabelProvider tnLabelProvider;

	public ConnectCmTreeLabelProvider(ConfigurableModel model) {
		cmLabelProvider = new ConfigurableModelLabelProvider();
		liLabelProvider = new CmListItemLabelProvider(model);
		tnLabelProvider = new CmTreeLabelProvider(model);
	}
	
	private LabelProvider findLabelProvider(Object element) {
		if (element instanceof CmAttributeListItem || element instanceof AttributeListItem) {
			return liLabelProvider;
		}
		if (element instanceof CmAttributeTreeNode || element instanceof AttributeTreeNode) {
			return tnLabelProvider;
		}
		return cmLabelProvider;
	}

	@Override
	public String getText(Object element) {
		return findLabelProvider(element).getText(element);
	}
	
	@Override
	public Image getImage(Object element) {
		return findLabelProvider(element).getImage(element);
	}
	
	@Override
	public Color getForeground(Object element) {
		return ((IColorProvider)findLabelProvider(element)).getForeground(element);
	}
	
	@Override
	public Color getBackground(Object element) {
		return ((IColorProvider)findLabelProvider(element)).getBackground(element);
	}

}

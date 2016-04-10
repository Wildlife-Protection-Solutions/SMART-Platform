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

import org.wcs.smart.ca.datamodel.Attribute.AttributeType;
import org.wcs.smart.dataentry.dialog.ConfigurableModelTreeContentProvider;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;

/**
 * Tree content provider for configurable model used in connect tab.
 * It displays inner list items and tree node structures under attributes.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class ConnectCmTreeContentProvider extends ConfigurableModelTreeContentProvider {

	public ConnectCmTreeContentProvider(boolean showRoot) {
		super(showRoot, true);
	}

	@Override
	public Object[] getChildren(Object parentElement) {
		if (parentElement instanceof CmAttribute) {
			CmAttribute a = (CmAttribute) parentElement;
			AttributeType type = a.getAttribute().getType();
			if (AttributeType.LIST.equals(type)) {
				return a.getCurrentList().toArray();
			}
			if (AttributeType.TREE.equals(type)) {
				return a.getCurrentTree().toArray();
			}
		}
		if (parentElement instanceof CmAttributeTreeNode) {
			CmAttributeTreeNode tn = (CmAttributeTreeNode) parentElement;
			return tn.getChildren().toArray();
		}
		return super.getChildren(parentElement);
	}
	
	@Override
	public Object getParent(Object element) {
		if (element instanceof CmAttributeListItem) {
			CmAttributeListItem li = (CmAttributeListItem) element;
			return li.getAttribute();
		}
		if (element instanceof CmAttributeTreeNode) {
			CmAttributeTreeNode tn = (CmAttributeTreeNode) element;
			return tn.getParent() != null ? tn.getParent() : tn.getAttribute();
		}
		return super.getParent(element);
	}
	
	@Override
	public boolean hasChildren(Object element) {
		if (element instanceof CmAttribute) {
			CmAttribute a = (CmAttribute) element;
			AttributeType type = a.getAttribute().getType();
			if (AttributeType.LIST.equals(type)) {
				return !a.getCurrentList().isEmpty();
			}
			if (AttributeType.TREE.equals(type)) {
				return !a.getCurrentTree().isEmpty();
			}
			return false; //all other types of attributes do not have children
		}
		if (element instanceof CmAttributeTreeNode) {
			CmAttributeTreeNode tn = (CmAttributeTreeNode) element;
			return !tn.getChildren().isEmpty();
		}
		return super.hasChildren(element);
	}
}

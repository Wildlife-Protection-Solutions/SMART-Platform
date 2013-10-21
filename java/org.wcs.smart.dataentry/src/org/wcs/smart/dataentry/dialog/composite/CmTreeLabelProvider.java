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
package org.wcs.smart.dataentry.dialog.composite;

import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;

/**
 * Label provider for configurable model trees which looks for
 * alias first, then displays the default tree name.
 * 
 * @author Emily
 *
 */
public class CmTreeLabelProvider extends AttributeTreeLabelProvider{

	private Session session = null;
	public CmTreeLabelProvider(Session currentSession){
		this.session = currentSession;
	}
	
	
	@Override
	public String getText(Object element) {
		if (element instanceof AttributeTreeNode){
			@SuppressWarnings("rawtypes")
			List items = session.createCriteria(CmAttributeTreeNode.class).add(Restrictions.eq("dmTreeNode", ((AttributeTreeNode) element))).list();
			if (items.size() > 0){
				CmAttributeTreeNode node = (CmAttributeTreeNode) items.get(0);
				String label = node.findNameNull(SmartDB.getCurrentLanguage());
				if (label != null){
					return label;
				}
			}
		}
		return super.getText(element);
	}
}

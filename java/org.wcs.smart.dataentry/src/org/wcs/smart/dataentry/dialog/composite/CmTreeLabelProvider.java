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

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.AttributeTreeNode;
import org.wcs.smart.dataentry.dialog.CmAttributeTreeContentProvider.CmTreeRootNode;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttributeTreeNode;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.properties.AttributeTreeLabelProvider;

/**
 * Label provider for configurable model trees which looks for
 * alias first, then displays the default tree name.
 * 
 * @author Emily
 *
 */
public class CmTreeLabelProvider extends AttributeTreeLabelProvider {

	private Session session = null;
	private ConfigurableModel model;
	
	public CmTreeLabelProvider(Session currentSession, ConfigurableModel model) {
		this.session = currentSession;
		this.model = model;
	}
		
	@Override
	public String getText(Object element) {
		if (element instanceof CmTreeRootNode){
			return Messages.CmTreeLabelProvider_Root;
		}
		
		CmAttributeTreeNode node = getTreeNode(element);
		
		if (node != null){
			String label = null;
			if (getLanguage() == null){
				label = node.findNameNull(SmartDB.getCurrentLanguage());
			}else{
				label = node.findNameNull(getLanguage());
				if (label == null){
					label = node.findNameNull(SmartDB.getCurrentConservationArea().getDefaultLanguage());
				}
			}
			if (label != null){
				return label;
			}
			return super.getText(node.getDmTreeNode());
		}
		return super.getText(element);
	}
	

	private CmAttributeTreeNode getTreeNode(Object element){
		if (element instanceof CmAttributeTreeNode) {
			return (CmAttributeTreeNode) element;
		}
		
		if (element instanceof AttributeTreeNode){
			List<?> items = session.createCriteria(CmAttributeTreeNode.class)
					.add(Restrictions.eq("dmTreeNode", ((AttributeTreeNode) element)))  //$NON-NLS-1$
					.add(Restrictions.eq("configurableModel", model)).list();  //$NON-NLS-1$
			if (items.size() > 0){
				CmAttributeTreeNode node = (CmAttributeTreeNode) items.get(0);
				return node;
			}
		}
		return null;
	}
	
	@Override
	public Color getForeground(Object element) {
		CmAttributeTreeNode node = getTreeNode(element);
		if (node != null){
			if (!node.getIsActive()){
				return Display.getDefault().getSystemColor(SWT.COLOR_DARK_GRAY);
			}
		}
		return null;
	}


	@Override
	public Color getBackground(Object element) {
		return null;
	}
}

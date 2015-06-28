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

import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.ui.NamedItemLabelProvider;

/**
 * Label provider for configurable model list item which looks for
 * alias first, then displays the default tree name.
 * 
 * @author Emily
 *
 */
public class CmListItemLabelProvider extends NamedItemLabelProvider implements IColorProvider{
	private Session session = null;
	private ConfigurableModel model;
	
	public CmListItemLabelProvider(Session currentSession, ConfigurableModel model) {
		this.session = currentSession;
		this.model = model;
	}
	
	
	@Override
	public String getText(Object element) {
		CmAttributeListItem node = getListItem(element);
		if (node != null){
			String label = null;
			if (currentLanguage == null){
				label = node.findNameNull(SmartDB.getCurrentLanguage());
			}else{
				label = node.findNameNull(currentLanguage);
			}
			if (label != null){
				return label;
			}
			return super.getText(node.getListItem());
		}
		return super.getText(element);
	}


	private CmAttributeListItem getListItem(Object element){
		if (element instanceof CmAttributeListItem) {
			return (CmAttributeListItem) element;
		}
		
		if (element instanceof AttributeListItem){
			List<?> items = session.createCriteria(CmAttributeListItem.class)
					.add(Restrictions.eq("listItem", ((AttributeListItem) element)))  //$NON-NLS-1$
					.add(Restrictions.eq("configurableModel", model)).list();  //$NON-NLS-1$
			if (items.size() > 0){
				CmAttributeListItem node = (CmAttributeListItem) items.get(0);
				return node;
			}
		}
		return null;
	}
	
	@Override
	public Color getForeground(Object element) {
		CmAttributeListItem node = getListItem(element);
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

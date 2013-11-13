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
package org.wcs.smart.cybertracker.export.data;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.ca.Language;
import org.wcs.smart.ca.datamodel.Attribute;
import org.wcs.smart.ca.datamodel.AttributeListItem;
import org.wcs.smart.cybertracker.util.LanguageUtil;
import org.wcs.smart.dataentry.model.CmAttributeListItem;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Class provides data related to list attributes items for CT export engine
 * 
 * @author elitvin
 * @since 2.0.0
 */
public class ListItemsDataProvider {
	
	private List<IAttributeListItemProxy> data;
	private Language language;
	
	public ListItemsDataProvider(Attribute attribute, ConfigurableModel configurableModel, Language language, Session session) {
		this.language = language;
		data = new ArrayList<IAttributeListItemProxy>();
		for (AttributeListItem dmItem : attribute.getActiveListItems()) {
			CmAttributeListItem cmItem = getCmListItem(session, dmItem, configurableModel);
			if (cmItem != null) {
				if (cmItem.getIsActive()) {
					data.add(new CmListItemProxy(cmItem));
				}
			} else {
				data.add(new DmListItemProxy(dmItem));
			}
		}
	}
	
	private CmAttributeListItem getCmListItem(Session session, AttributeListItem element, ConfigurableModel configurableModel) {
		if (configurableModel.getUuid() == null)
			return null;
		List<?> items = session.createCriteria(CmAttributeListItem.class)
				.add(Restrictions.eq("listItem", element))  //$NON-NLS-1$
				.add(Restrictions.eq("configurableModel", configurableModel)).list();  //$NON-NLS-1$
		if (items.size() > 0) {
			return (CmAttributeListItem) items.get(0);
		}
		return null;
	}
	
	public List<IAttributeListItemProxy> getActiveListItems() {
		return data;
	}

	private class DmListItemProxy implements IAttributeListItemProxy {
		private AttributeListItem item;

		public DmListItemProxy(AttributeListItem item) {
			this.item = item;
		}

		@Override
		public String getName() {
			return LanguageUtil.getName(item, language);
		}

		@Override
		public byte[] getUuid() {
			return item.getUuid();
		}
	}
	
	private class CmListItemProxy implements IAttributeListItemProxy {
		private CmAttributeListItem item;

		public CmListItemProxy(CmAttributeListItem item) {
			this.item = item;
		}

		@Override
		public String getName() {
			String name = LanguageUtil.getName(item, language);
			if (name == null || name.isEmpty()) {
				return LanguageUtil.getName(item.getListItem(), language);
			}
			return name;
		}

		@Override
		public byte[] getUuid() {
			return item.getListItem().getUuid();
		}
	}
	
}

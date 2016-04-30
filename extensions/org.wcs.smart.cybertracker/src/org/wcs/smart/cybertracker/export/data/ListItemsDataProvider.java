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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.hibernate.Session;
import org.wcs.smart.ca.Language;
import org.wcs.smart.cybertracker.util.LanguageUtil;
import org.wcs.smart.dataentry.model.CmAttribute;
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
	
	public ListItemsDataProvider(CmAttribute attribute, ConfigurableModel configurableModel, Language language, Session session) {
		this.language = language;
		data = new ArrayList<IAttributeListItemProxy>();
		//wrapping list items
		for (CmAttributeListItem cmItem : attribute.getCurrentList()) {
			if (cmItem != null) {
				if (cmItem.getIsActive()) {
					data.add(new CmListItemProxy(cmItem));
				}
			}
		}
	}
	
	public List<IAttributeListItemProxy> getActiveListItems() {
		return data;
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
		public UUID getUuid() {
			return item.getListItem().getUuid();
		}

		@Override
		public File getImageFile() {
			return item.getImageFile();
		}

		@Override
		public CmAttributeListItem getListItem() {
			return item;
		}
	}
	
}

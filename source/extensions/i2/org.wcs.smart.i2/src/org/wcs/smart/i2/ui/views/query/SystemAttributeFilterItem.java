/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.views.query;

import java.text.MessageFormat;
import java.util.Locale;

import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;

/**
 * Tree filter item for system attributes
 * 
 * @author Emily
 *
 */
public class SystemAttributeFilterItem extends BasicTreeFilterItem {

	private SystemAttributeFilter.SystemAttribute attribute;
	private SystemAttributeFilter.Type type;
	
	public SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute attribute, SystemAttributeFilter.Type type) {
		super(getName(attribute));
		this.attribute = attribute;
		this.type = type;
		
		switch(attribute) {
			case DATE_CREATED:
			case DATE_MODIFIED:
				setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
		}
	}

	private static String getName(SystemAttributeFilter.SystemAttribute attribute) {
		return IntelligenceLabelProviderImpl.getName(attribute);
	}
	
	@Override
	public DropItem[] asDropItem() {
		boolean canEdit = IntelSecurityManager.INSTANCE.canEditQuery();
		
		if (attribute == SystemAttribute.DATE_CREATED || attribute == SystemAttribute.DATE_MODIFIED) {
			
			StringBuilder sb = new StringBuilder();
			sb.append(SystemAttributeFilter.SA_KEY);
			sb.append(SystemAttributeFilter.INTERNAL_SEPERATOR);
			sb.append(type.name().toLowerCase(Locale.ROOT));
			sb.append(SystemAttributeFilter.INTERNAL_SEPERATOR);
			sb.append(attribute.name().toLowerCase(Locale.ROOT));
			
			DateDropItem di = new DateDropItem(MessageFormat.format("{0} ({1})", getName(), IntelligenceLabelProviderImpl.getName(type)), sb.toString(), canEdit); //$NON-NLS-1$
			return new DropItem[] {di};
		}
		
		throw new IllegalStateException(Messages.RecordAttributeFilterItem_FilterNotSupported);
	}
	
}

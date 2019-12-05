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
import java.util.List;
import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.SmartContext;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.InternalQueryManager;
import org.wcs.smart.i2.ProfilesManager;
import org.wcs.smart.i2.internal.IntelligenceLabelProviderImpl;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.model.IntelRecordSource;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter;
import org.wcs.smart.i2.query.observation.filter.SystemAttributeFilter.SystemAttribute;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;

/**
 * Tree filter item for system attributes
 * 
 * @author Emily
 *
 */
public class SystemAttributeFilterItem extends BasicTreeFilterItem {

	private SystemAttributeFilter.SystemAttribute attribute;
	
	private String initKey = null;
	
	public SystemAttributeFilterItem(SystemAttributeFilter.SystemAttribute attribute) {
		super(getName(attribute));
		this.attribute = attribute;

		switch(attribute) {
			case RECORD_DATE:
			case RECORD_DATE_CREATED:
			case RECORD_DATE_MODIFIED:
			case ENTITY_DATE_CREATED:
			case ENTITY_DATE_MODIFIED:
				setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SYSTEM_DATEATTRIBUTE));
				break;
			case RECORD_SOURCE:
				setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_RECORD));
				break;
			case RECORD_STATUS:
				setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SRC_NEW));
				break;
		}
	}

	private static String getName(SystemAttributeFilter.SystemAttribute attribute) {
		return IntelligenceLabelProviderImpl.getName(attribute);
	}
	
	public void setInitialKey(String key) {
		this.initKey = key;
	}
	
	@Override
	public DropItem[] asDropItem() {
		boolean canEdit = IntelSecurityManager.INSTANCE.canEditQuery();
			
		StringBuilder sb = new StringBuilder();
		sb.append(SystemAttributeFilter.SA_KEY);
		sb.append(SystemAttributeFilter.INTERNAL_SEPERATOR);
		sb.append(attribute.name().toLowerCase(Locale.ROOT));
		
		if (attribute == SystemAttribute.RECORD_DATE_CREATED ||
				attribute == SystemAttribute.RECORD_DATE_MODIFIED ||
				attribute == SystemAttribute.RECORD_DATE) {	
			DateDropItem di = new DateDropItem(MessageFormat.format("{0} (Record)", getName()), sb.toString(), canEdit); //$NON-NLS-1$
			return new DropItem[] {di};
		}else if (attribute == SystemAttribute.ENTITY_DATE_CREATED ||
					attribute == SystemAttribute.ENTITY_DATE_MODIFIED) {	
				DateDropItem di = new DateDropItem(MessageFormat.format("{0} (Entity)", getName()), sb.toString(), canEdit); //$NON-NLS-1$
				return new DropItem[] {di};
		}else if (attribute == SystemAttribute.RECORD_SOURCE) {
			try(Session session = HibernateManager.openSession()){
				List<IntelRecordSource> srcs = InternalQueryManager.INSTANCE.getQueryItemProvider().getRecordSources(ProfilesManager.INSTANCE.getActiveProfileIds(), session);
				String[] ops = new String[srcs.size()];
				String[] keys = new String[ops.length];
				
				int i = 0;
				for (IntelRecordSource s : srcs) {
					ops[i] = s.getName();
					keys[i++] = s.getKeyId();
				}	
				OptionDropItem di = new OptionDropItem(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(SystemAttribute.RECORD_SOURCE, Locale.getDefault()), SystemAttribute.RECORD_SOURCE.getKey(), ops, keys, canEdit);
				if (initKey != null) di.setInitialValue(initKey);
				
				return new DropItem[] {di};
			}
		}else if (attribute == SystemAttribute.RECORD_STATUS) {
			String[] ops = new String[IntelRecord.Status.values().length];
			String[] keys = new String[ops.length];
			
			int i = 0;
			for (IntelRecord.Status ir : IntelRecord.Status.values()) {
				ops[i] = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(ir, Locale.getDefault());
				keys[i++] = ir.name();
			}
			OptionDropItem di = new OptionDropItem(SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(SystemAttribute.RECORD_STATUS, Locale.getDefault()), SystemAttribute.RECORD_STATUS.getKey(), ops, keys, canEdit);
			if (initKey != null) di.setInitialValue(initKey);
			return new DropItem[] {di};
		}
		
		throw new IllegalStateException(Messages.RecordAttributeFilterItem_FilterNotSupported);
	}
	
}

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

import java.util.Locale;

import org.wcs.smart.SmartContext;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.i2.IIntelligenceLabelProvider;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelRecord;
import org.wcs.smart.i2.query.observation.filter.RecordAttributeFilter;
import org.wcs.smart.i2.security.IntelSecurityManager;
import org.wcs.smart.i2.ui.views.query.dropitem.DateDropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.DropItem;
import org.wcs.smart.i2.ui.views.query.dropitem.OptionDropItem;

/**
 * Record attribute filter item for fixed record attributes
 * 
 * @author Emily
 *
 */
public class RecordAttributeFilterItem extends BasicTreeFilterItem {

	private RecordAttributeFilter.FixedAttribute type;
	
	public RecordAttributeFilterItem(RecordAttributeFilter.FixedAttribute type) {
		super(type == RecordAttributeFilter.FixedAttribute.STATUS ? Messages.RecordAttributeFilterItem_RecordStatusOp : Messages.RecordAttributeFilterItem_RecordDateOp);
		this.type = type;
		if (type == RecordAttributeFilter.FixedAttribute.STATUS) {
			setImageDescriptor(Intelligence2PlugIn.getDefault().getImageRegistry().getDescriptor(Intelligence2PlugIn.ICON_SRC_DONE));
		}else if (type == RecordAttributeFilter.FixedAttribute.DATE) {
			setImageDescriptor(SmartPlugIn.getDefault().getImageRegistry().getDescriptor(SmartPlugIn.ATTRIBUTE_DATE_ICON));
		}
	}

	@Override
	public DropItem[] asDropItem() {
		boolean canEdit = IntelSecurityManager.INSTANCE.canEditQuery();
		
		if (type == RecordAttributeFilter.FixedAttribute.STATUS) {
			String[] names = new String[IntelRecord.Status.values().length];
			String[] keys = new String[IntelRecord.Status.values().length];
			for (int i = 0; i < IntelRecord.Status.values().length; i ++) {
				names[i] = SmartContext.INSTANCE.getClass(IIntelligenceLabelProvider.class).getLabel(IntelRecord.Status.values()[i], Locale.getDefault());
				keys[i] = IntelRecord.Status.values()[i].name();
			}	
			OptionDropItem di = new OptionDropItem(getName(), type.getKey(), names, keys, canEdit);
			return new DropItem[] {di};
		}else if (type == RecordAttributeFilter.FixedAttribute.DATE) {
			DateDropItem di = new DateDropItem(getName(), type.getKey(), canEdit);
			return new DropItem[] {di};
		}
		
		throw new IllegalStateException(Messages.RecordAttributeFilterItem_FilterNotSupported);
	}
	
}

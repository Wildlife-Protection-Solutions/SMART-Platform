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
package org.wcs.smart.patrol.query.internal;

import java.util.Locale;

import org.hibernate.Session;
import org.wcs.smart.patrol.query.parser.internal.summary.PatrolValueItem;
import org.wcs.smart.query.model.summary.IValueItem;
import org.wcs.smart.query.model.summary.ValueItemLabelProvider;

/**
 * Label provider for patrol value items 
 * 
 * @author Emily
 *
 */
public class PatrolValueItemLabelProvider extends ValueItemLabelProvider {

	public static PatrolValueItemLabelProvider INSTANCE = new PatrolValueItemLabelProvider();
	
	protected PatrolValueItemLabelProvider() {
		super();
	}
	
	@Override
	public String getName(IValueItem item, Session session){
		if (item instanceof PatrolValueItem){
			PatrolValueItem it = (PatrolValueItem)item;
			
			String text = getName((PatrolValueItem)item, session);
			
			if (it.getPatrolValueOption().hasNoDataOption()) {
				if (it.includeNoData()) {
					text = text + Messages.PatrolValueItemLabelProvider_AllLabel;
				}else {
					text = text + Messages.PatrolValueItemLabelProvider_DataOnlyLabel;
				}
			}
			return text;
		}
		return super.getName(item, session);
	}
	@Override
	public String getFullName(IValueItem item, Session session){
		if (item instanceof PatrolValueItem){
			PatrolValueItem it = (PatrolValueItem)item;
			String text = getName((PatrolValueItem)item, session);
			if (it.getPatrolValueOption().hasNoDataOption()) {
				if (it.includeNoData()) {
					text = text + Messages.PatrolValueItemLabelProvider_AllLabel;
				}else {
					text = text + Messages.PatrolValueItemLabelProvider_DataOnlyLabel;
				}
			}
			return text;
		}
		return super.getFullName(item, session);
	}
	
	/**
	 * @see org.wcs.smart.query.parser.internal.summary.IValueItem#getName(org.hibernate.Session)
	 */
	public String getName(PatrolValueItem item, Session session){
		return item.getPatrolValueOption().getGuiName(Locale.getDefault());
	}


}

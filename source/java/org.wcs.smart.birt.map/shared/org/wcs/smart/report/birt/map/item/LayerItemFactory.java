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
package org.wcs.smart.report.birt.map.item;

import java.util.Locale;

import org.eclipse.birt.report.model.api.DesignElementHandle;
import org.eclipse.birt.report.model.api.ExtendedItemHandle;
import org.eclipse.birt.report.model.api.extension.IMessages;
import org.eclipse.birt.report.model.api.extension.IReportItem;
import org.eclipse.birt.report.model.api.extension.ReportItemFactory;

import com.ibm.icu.util.ULocale;

/**
 * Factory for smart map item.
 * 
 * @author Emily
 *
 */
public class LayerItemFactory extends ReportItemFactory implements IMessages{

	/**
	 * @see org.eclipse.birt.report.model.api.extension.IReportItemFactory#newReportItem(org.eclipse.birt.report.model.api.DesignElementHandle)
	 */
	@Override
	public IReportItem newReportItem(DesignElementHandle extendedItemHandle) {
		if ( extendedItemHandle instanceof ExtendedItemHandle &&
				LayerItem.EXTENSION_NAME.equals( ( (ExtendedItemHandle) extendedItemHandle ).getExtensionName( ) ) ){
			LayerItem item = new LayerItem( (ExtendedItemHandle) extendedItemHandle );
			return item;
		}
		return null;

	}

	@Override
	public IMessages getMessages() {
		return this;
	}

	@Override
	public String getMessage(String key, Locale locale) {
		return "Map Layer"; //$NON-NLS-1$
	}

	@Override
	public String getMessage(String key, ULocale locale) {
		return getMessage(key, (Locale)null);
	}

}

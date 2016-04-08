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
package org.wcs.smart.udig.catalog.smart.ui;

import java.util.Locale;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.udig.catalog.smart.ISmartMapLabelProvider;

/**
 * Desktop SMART Service label provider
 * @author Emily
 *
 */
public class DesktopSmartServiceLabelProvider implements ISmartMapLabelProvider {

	@Override
	public String getDataSourceConservationAreaPropName(Locale l) {
		return Messages.SmartDataSourceFactory_CA_ParameterName;
	}

	@Override
	public String getDataSourceSessionProviderPropName(Locale l) {
		return Messages.DesktopSmartServiceLabelProvider_ConnectionProvider;
	}

	
	@Override
	public String getDataSourceDisplayName(Locale l) {
		return Messages.SmartDataSourceFactory_SmartDataSourceName;
	}

	@Override
	public String getDataSourceDescription(Locale l) {
		return Messages.SmartDataSourceFactory_SmartDataSourceDescription;
	}

	@Override
	public String getDataSourceReadErrorMessage(Locale l) {
		return Messages.SmartDataSourceFactory_Error_ReadingSmartDataSource;
	}

	@Override
	public String getSmartServiceDescription(Locale l) {
		return Messages.SmartServiceInfo_Description;
	}

	@Override
	public String getSmartServiceTitle(Locale l) {
		return Messages.SmartServiceInfo_Title;
	}

	@Override
	public String[] getSmartServiceKeywords(Locale l) {
		return new String[]{Messages.SmartServiceInfo_Keyword1, Messages.SmartServiceInfo_Keyword2};
	}

	@Override
	public ImageDescriptor getSmartServiceImage(Locale l) {
		return AbstractUIPlugin.imageDescriptorFromPlugin(SmartPlugIn.PLUGIN_ID,"images/icons/smart16.gif"); //$NON-NLS-1$
	}
	
}

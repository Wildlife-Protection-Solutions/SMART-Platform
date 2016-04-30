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
package org.wcs.smart.cybertracker.export.alert;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.UuidItem;
import org.wcs.smart.cybertracker.CyberTrackerPlugIn;
import org.wcs.smart.cybertracker.internal.Messages;
import org.wcs.smart.dataentry.model.CmAttribute;
import org.wcs.smart.dataentry.model.ConfigurableModel;

/**
 * Class that combines all Alert extensions for CyberTracker and allows to obtain {@link AlertData}
 * for items from configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class AlertExportDataProvider {
	
	private List<IAlertProvider> providers = new ArrayList<>();
	
	public AlertExportDataProvider(ConfigurableModel model) {
		if (Platform.getExtensionRegistry() != null) {
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(CyberTrackerPlugIn.ALERT_EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					ICtAlertExtension ext = (ICtAlertExtension) e.createExecutableExtension("clazz"); //$NON-NLS-1$
					providers.add(ext.createAlertProvider(model));
				}
			}catch (Exception ex){
				SmartPlugIn.displayLog(Messages.AlertExportDataProvider_Error, ex);
			}
		}
	}

	public List<AlertData> getAlertData(UuidItem item, CmAttribute attribute) {
		List<AlertData> result = new ArrayList<>();
		for (IAlertProvider p : providers) {
			result.addAll(p.getAlertData(item, attribute));
		}
		return result;
	}
}

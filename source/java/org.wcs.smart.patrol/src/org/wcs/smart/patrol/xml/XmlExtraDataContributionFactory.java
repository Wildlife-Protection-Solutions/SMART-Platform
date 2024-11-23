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
package org.wcs.smart.patrol.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.patrol.SmartPatrolPlugIn;
import org.wcs.smart.patrol.internal.Messages;
import org.wcs.smart.patrol.xml.external.IPatrolExportContribution;
import org.wcs.smart.patrol.xml.external.IXmlExtraDataContribution;

/**
 * Factory for providing all contributed information added via extension point
 * for Export/Import patrol extra-data.
 * 
 * @author elitvin
 * @since 1.0.0
 */
public class XmlExtraDataContributionFactory {

	private static List<IXmlExtraDataContribution> contributions = null;
	
	public static List<IXmlExtraDataContribution> getContributions() {
		if (contributions == null) {
			if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
			List<IXmlExtraDataContribution> items = new ArrayList<>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IXmlExtraDataContribution.EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					IXmlExtraDataContribution contribution = (IXmlExtraDataContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
					items.add(contribution);
				}
				contributions = items;
			} catch (Exception ex) {
				SmartPatrolPlugIn.displayLog(Messages.XmlExtraDataContributionFactory_ErrorParsingExtraData2, ex);
				return Collections.emptyList();
			}
		}
		return contributions;
	}
	
	public static List<IPatrolExportContribution> getUiContributions() {
		if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
		List<IPatrolExportContribution> items = new ArrayList<>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IXmlExtraDataContribution.EXTENSION_ID);
		try {
			for (IConfigurationElement e : config) {
				IPatrolExportContribution contribution = (IPatrolExportContribution)e.createExecutableExtension("uiclass"); //$NON-NLS-1$
				items.add(contribution);
			}
		} catch (Exception ex) {
			SmartPatrolPlugIn.displayLog(Messages.XmlExtraDataContributionFactory_ErrorParsingExtraData2, ex);
			return Collections.emptyList();
		}
		return items;
	}
}

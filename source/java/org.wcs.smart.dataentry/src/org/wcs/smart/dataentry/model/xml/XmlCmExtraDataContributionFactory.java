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
package org.wcs.smart.dataentry.model.xml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.dataentry.internal.Messages;
import org.wcs.smart.dataentry.model.xml.external.IXmlCmExtraDataContribution;

/**
 * Factory for providing all contributed information added via extension point
 * for Export/Import extra-data for configurable model.
 * 
 * @author elitvin
 * @since 4.0.0
 */
public class XmlCmExtraDataContributionFactory {

	private static List<IXmlCmExtraDataContribution> contributions = null;
	
	public static List<IXmlCmExtraDataContribution> getContributions() {
		if (contributions == null) {
			if (Platform.getExtensionRegistry() == null) return Collections.emptyList();
			List<IXmlCmExtraDataContribution> items = new ArrayList<IXmlCmExtraDataContribution>();
			IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IXmlCmExtraDataContribution.EXTENSION_ID);
			try {
				for (IConfigurationElement e : config) {
					IXmlCmExtraDataContribution contribution = (IXmlCmExtraDataContribution)e.createExecutableExtension("class"); //$NON-NLS-1$
					items.add(contribution);
				}
				contributions = items;
			} catch (Exception ex) {
				SmartPlugIn.displayLog(Messages.XmlCmExtraDataContributionFactory_ErrorParseExtraData, ex);
				return Collections.emptyList();
			}
		}
		return contributions;
	}
	
}

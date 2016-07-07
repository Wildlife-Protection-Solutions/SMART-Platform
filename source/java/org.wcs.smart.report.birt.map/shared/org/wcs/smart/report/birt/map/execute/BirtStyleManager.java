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
package org.wcs.smart.report.birt.map.execute;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.hibernate.Session;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.wcs.smart.report.birt.map.IBirtLayerStyleProvider;

/**
 * Style manager for BIRT Map layers.
 * @author Emily
 *
 */
public enum BirtStyleManager {

	INSTANCE;
	
	private volatile List<IBirtLayerStyleProvider> styleProviders = null;
	
	public StyleBlackboard getStyle(String extensionId, String queryText, Session session) throws Exception{
		if (styleProviders == null){
			synchronized (INSTANCE) {
				styleProviders = getProviders();
			}
		}
		
		for (IBirtLayerStyleProvider p : styleProviders){
			StyleBlackboard style = p.getStyle(extensionId, queryText, session);
			if (style != null) return style;
		}
		return null;
	}
	
	
	private List<IBirtLayerStyleProvider> getProviders() throws CoreException{
		List<IBirtLayerStyleProvider> items = new ArrayList<IBirtLayerStyleProvider>();
		
		IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint = registry.getExtensionPoint("org.wcs.smart.report.birt.maplayer"); //$NON-NLS-1$
        if (extensionPoint == null)
            return items;
        IExtension[] extensions = extensionPoint.getExtensions();

        // For each extension ...
        for( int i = 0; i < extensions.length; i++ ) {
            IExtension extension = extensions[i];
            IConfigurationElement[] elements = extension.getConfigurationElements();
            for( int j = 0; j < elements.length; j++ ) {
                IConfigurationElement element = elements[j];
                if (element.getName().equals("StyleProvider")){ //$NON-NLS-1$
                	IBirtLayerStyleProvider manager = (IBirtLayerStyleProvider) element.createExecutableExtension("styleprovider"); //$NON-NLS-1$
                	if (manager != null){
                		items.add(manager);
                	}
                }
            }
        }
        return items;
	}
	
}

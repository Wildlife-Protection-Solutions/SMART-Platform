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
package org.wcs.smart.report.birt.map;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.OdaDataSetHandle;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.report.birt.map.properties.LayerDefinition;

/**
 * Birt Map Layer extension manager
 * @author Emily
 *
 */
public class ExtensionManager {
	
	public static final String EXTENSION_ID = "org.wcs.smart.report.birt.maplayer"; //$NON-NLS-1$
	
	public List<LayerDefinition> getLayerOptions(DataSetHandle[] handles) throws Exception{
		List<IBirtMapLayerManager> managers = getAllManagers();
		
		List<LayerDefinition> defs = new ArrayList<LayerDefinition>();
		
		for (DataSetHandle handle : handles){
			for (IBirtMapLayerManager manager : managers){
				if (manager.canAddToMap(handle)){
					List<MapLayerInfo> temp = manager.getGeometryOptions(handle);
					if (temp != null){
						for (MapLayerInfo t : temp){
							defs.add(new LayerDefinition((OdaDataSetHandle)handle, t));
						}
					}
				}
			}
		}
		return defs;
		
		
	}
	public List<IBirtMapLayerManager> getAllManagers() throws CoreException{
		List<IBirtMapLayerManager> items = new ArrayList<IBirtMapLayerManager>();
		
		IExtensionRegistry registry = Platform.getExtensionRegistry();
        IExtensionPoint extensionPoint = registry.getExtensionPoint(EXTENSION_ID);
        if (extensionPoint == null)
            return items;
        IExtension[] extensions = extensionPoint.getExtensions();

        // For each extension ...
        for( int i = 0; i < extensions.length; i++ ) {
            IExtension extension = extensions[i];
            IConfigurationElement[] elements = extension.getConfigurationElements();
            for( int j = 0; j < elements.length; j++ ) {
                IConfigurationElement element = elements[j];
                if (element.getName().equals("MapLayer")){ //$NON-NLS-1$
                	IBirtMapLayerManager manager = (IBirtMapLayerManager) element.createExecutableExtension("maplayer"); //$NON-NLS-1$
                	items.add(manager);
                }
            }
        }
        return items;
	}
}

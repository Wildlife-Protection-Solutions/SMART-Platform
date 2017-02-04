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
package org.wcs.smart.i2.ui.editors.query;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.project.ILayerListener;
import org.locationtech.udig.project.LayerEvent;
import org.locationtech.udig.project.LayerEvent.EventType;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.query.IPagedQueryResultSet;
import org.wcs.smart.i2.udig.query.QueryService;
import org.wcs.smart.i2.ui.editors.MapComposite;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Query results map panel.
 * 
 * @author Emily
 *
 */
public class QueryMapPanel extends MapComposite {

	
	public QueryMapPanel(Composite parent, IntelQueryEditor parentEditor) {
		super(parent, parentEditor);
	}

	private QueryService service = null;
	private List<Layer> mapLayers = null;
	
	public void updateQueryLayers(IPagedQueryResultSet results){
		if (isDisposed()) return;
		if (getMap() == null) return;
		
		int layerIndex = getMap().getLayersInternal().size();
		
		if (mapLayers != null){
			DeleteLayersCommand removeLayers = new DeleteLayersCommand(mapLayers.toArray(new Layer[mapLayers.size()]));
			getMap().sendCommandASync(removeLayers);
			layerIndex -= mapLayers.size();
			mapLayers = null;
		}
		
		if (service != null){
			service.dispose(new NullProgressMonitor());
			CatalogPlugin.getDefault().getLocalCatalog().remove(service);
			service = null;
		}
		if (results != null){
			
			service = new QueryService(results, ((IntelQueryEditor)editor).getQuery().getUuid(), ((IntelQueryEditor)editor).getQuery().getName());
			try {
				List<? extends IGeoResource> resources = service.resources(new NullProgressMonitor());
				AddLayersCommand addLayers = new AddLayersCommand(resources, layerIndex < 0 ? 0 : layerIndex){
					@Override
					public void run( IProgressMonitor monitor ) throws Exception {
						super.run(monitor);
						mapLayers = getLayers();
						
						String style = ((IntelQueryEditor)editor).getQuery().getStyle();
						Map<String, StyleBlackboard> styles = null;;
						if (style != null){
							styles = StyleManager.INSTANCE.fromStringMap(style);
						}
						for (Layer l : mapLayers){
							if (styles != null){
								StyleBlackboard sb = styles.get(l.getGeoResource().getIdentifier().getRef());
								if (sb != null) l.setStyleBlackboard(sb);
							}
							
							l.addListener(new ILayerListener() {	
								@Override
								public void refresh(LayerEvent event) {
									if (event.getType() == EventType.STYLE){
										updateQueryStyle();
									}
								}
							});
							
							
						}
					}
				};
				getMap().sendCommandASync(addLayers);
			} catch (IOException e) {
				Intelligence2PlugIn.log(e.getMessage(), e);
			}
		}
	}
	
	private void updateQueryStyle(){
		Map<String, StyleBlackboard> styles = new HashMap<>();
		
		for (Layer l : mapLayers){
			String key = l.getGeoResource().getIdentifier().getRef();
			styles.put(key, l.getStyleBlackboard());
		}
		try{
			String styleString = StyleManager.INSTANCE.asString(styles);
			((IntelQueryEditor)editor).getQuery().setStyle(styleString);
			
			Display.getDefault().syncExec(()->((IntelQueryEditor)editor).setDirty(true));
		}catch (Exception ex){
			Intelligence2PlugIn.displayLog("Error saving query layer styles.", ex);
		}
		
	}

	
	
}

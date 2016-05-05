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

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.catalog.IServiceFactory;
import org.locationtech.udig.catalog.URLUtils;
import org.locationtech.udig.catalog.internal.ServiceFactoryImpl;
import org.locationtech.udig.core.internal.CorePlugin;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.StyleContent;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.map.internal.settings.LayerRegister;
import org.wcs.smart.map.internal.settings.MapRegister;
import org.wcs.smart.map.internal.settings.StyleRegister;
import org.wcs.smart.map.internal.settings.SyleContentFactory;
import org.wcs.smart.udig.catalog.smart.IDatabaseConnectionProvider;
import org.wcs.smart.udig.catalog.smart.ISessionService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Restores a basemap onto a BIRT Smart Map
 * 
 * @author Emily
 *
 */
public class RestoreMapSettings {
	
	private static final String SMARTBM_FILE_PROTOCOL = "smartbm"; //$NON-NLS-1$
	private static final String MAP_DIRECTORY = "maps"; //$NON-NLS-1$
	
	private IDatabaseConnectionProvider dbProvider;
	
	/**
	 * Applies the custom settings to the map.
	 * <p>
	 * The provided map will be updated with those layers imported by the user 
	 * and the custom setting done in each layer.
	 * </p> 
	 * 
	 * @param currentMap the displayed map
	 */
	public synchronized void applyTo(Map currentMap, BasemapDefinition baseMap, 
			ConservationArea ca, IDatabaseConnectionProvider dbProvider) {
	
		this.dbProvider = dbProvider;
		
		// get map definition selected
		String jsonMap = baseMap.getMapDef();
		GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
		Gson gson = gsonBuilder.create();
		final MapRegister userMap = gson.fromJson(jsonMap, MapRegister.class);
					
		// new basemap layers
		List<ILayer> basemapLayers = new ArrayList<ILayer>();

		// determine which layers need to be added/removed
		List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
		final HashMap<URL, LayerRegister> definitionMap = new HashMap<URL, LayerRegister>();

		for (LayerRegister basemapLayer : userMap.getLayerList()) {
			if (!basemapLayer.getVisible()) continue; //we have no use for invisible layers in birt maps
			List<IGeoResource> resources = prepareLayers(currentMap, basemapLayer.getURI(),
					basemapLayer.getName(), ca);
			if (resources != null) {
				toAdd.addAll(resources);
				for (IGeoResource geo : resources) {
					definitionMap.put(geo.getIdentifier(), basemapLayer);
				}
			}
		}

		// add new basemap layers
		if (toAdd.size() > 0) {
			AddLayersCommand alCommand = new AddLayersCommand(toAdd, 0);
			alCommand.setMap(currentMap);
			try {
				alCommand.run(new NullProgressMonitor());
			} catch (Exception e) {
				Logger.getLogger(RestoreMapSettings.class.getName()).log(Level.SEVERE, "Error restoring basemap layers.", e); //$NON-NLS-1$
			}
			
			List<? extends ILayer> addedLayers = alCommand.getLayers();
			basemapLayers.addAll(addedLayers);
			for (ILayer l : addedLayers){
				definitionMap.put(l.getID(), definitionMap.get(l.getGeoResource().getIdentifier()));
			}
		}
			//sort basemap layers
		List<Layer> orderedLayers = new ArrayList<Layer>();
		for (ILayer l : basemapLayers) {
			orderedLayers.add((Layer) l);
		}
		Collections.sort(orderedLayers, new Comparator<Layer>() {
			@Override
			public int compare(Layer o1, Layer o2) {
				LayerRegister info1 = definitionMap.get(o1.getID());
				LayerRegister info2 = definitionMap.get(o2.getID());
				
				int index1 = userMap.getLayerList().indexOf(info1);
				int index2 = userMap.getLayerList().indexOf(info2);
				if (index1 < index2)
					return -1;
				if (index1 > index2)
					return 1;
				return 0;
			}
		});

		// order layers
		currentMap.getContextModel().eSetDeliver(false);
		currentMap.getLayersInternal().removeAll(orderedLayers);
		currentMap.getLayersInternal().addAll(0, orderedLayers);
		currentMap.getContextModel().eSetDeliver(true);

		// update basemap layer settings
		// updates the map with the user settings
		updateMap(currentMap, userMap);

		for (ILayer layer : basemapLayers) {
			LayerRegister info = definitionMap.get(layer.getID());
			((Layer) layer).setVisible(info.getVisible());
			updateLayer(((Layer) layer), info);
		}
	}
	
	private List<IGeoResource> prepareLayers( Map map, java.net.URI uri, String name, ConservationArea ca) {

    
    	try {
    		if (uri.getScheme().equals(SMARTBM_FILE_PROTOCOL)){
    			//smart basemap file; needs to re-create uri with absolute path
    			File fileStoreDirectory = new File(ca.getFileDataStoreLocation(), MAP_DIRECTORY);
    			File path = new File(fileStoreDirectory, uri.getSchemeSpecificPart());
    			String fragment = uri.getFragment();
    			uri = path.toURI();
    			uri = new java.net.URI(uri.getScheme(), uri.getSchemeSpecificPart(), fragment);
    		}
    		URL url  = new URL(null, uri.toString(), CorePlugin.RELAXED_HANDLER);
    		
        	List<IGeoResource> geoResources = null;
			
        	// it doesn't exist a service for the url then create one and try again
			// requires url without fragment
			URL urlWithoutFragment = new URL(url.getProtocol(), url.getHost(),url.getPort(), url.getFile(), CorePlugin.RELAXED_HANDLER );
			List<IService> newServices = constructServices(urlWithoutFragment);

			// the service for the url exist then gets the resource
			geoResources = new ArrayList<IGeoResource>();
	     	for( IResolve resolve : newServices ) {        
	     		//search fo georesource
            	List<IResolve> members = new ArrayList<IResolve>();
            	members.addAll(resolve.members(new NullProgressMonitor()));
            	while(members.size() > 0){
            		IResolve member = members.remove(0);
            		if (member.canResolve(IGeoResource.class)){
            			if (URLUtils.urlEquals(member.getIdentifier(), url, false)){
            				geoResources.add(member.resolve(IGeoResource.class, new NullProgressMonitor()));
            			}
            		}
            		members.addAll(member.members(new NullProgressMonitor()));
            	}
	        }
	        return geoResources;
    	} catch (Exception e){
    		Logger.getLogger(RestoreMapSettings.class.getName()).log(Level.WARNING, "Error preparing basemap layers.", e); //$NON-NLS-1$
    		return null;
    	}
    }
	
	/**
	 * Updates the saved map properties with the settings present in the map register.
	 * 
	 * @param map map to update
	 * @param savedMap register settings
	 */
	private Map updateMap(Map map, MapRegister savedMap) {
		map.setColorPalette(savedMap.getColorPalette());
		map.setColourScheme(savedMap.getColourScheme());
		return map;
	}
	
	/**
	 * Updates the layer with the value present in the settings.
	 * 
	 * @param layer a layer to update
	 * @param savedLayer custom settings for this layer
	 */
	private Layer updateLayer(Layer layer, LayerRegister savedLayer) {
		
		try {
			layer.setName(savedLayer.getName());
			layer.setColourScheme(savedLayer.getColourScheme());
			layer.setDefaultColor(savedLayer.getDefaultColor());
			layer.setFilter(ECQL.toFilter(savedLayer.getCQL()));
			layer.setMaxScaleDenominator(savedLayer.getMaxScaleDenominator());
			layer.setMinScaleDenominator(savedLayer.getMinScaleDenominator());

			CoordinateReferenceSystem crs = ReferencingFactoryFinder.getCRSFactory(null).createFromWKT(savedLayer.getCRS());
			layer.setCRS(crs);

			//create a new empty blackboard to add styles to
			StyleBlackboard updatedStyleBlackboard = ProjectFactory.eINSTANCE.createStyleBlackboard();
			
			for (StyleRegister styleRegister : savedLayer.getStyleRegisterList()) {
				Object style = mementoToStyle(styleRegister.getId(), styleRegister.getMemento());
				updatedStyleBlackboard.put(styleRegister.getId(), style);
			}
			// change the current by the cloned and updated
			layer.setStyleBlackboard(updatedStyleBlackboard);
		} catch (Exception e) {
			Logger.getLogger(RestoreMapSettings.class.getName()).log(Level.WARNING, "Error creating basemap layer.", e); //$NON-NLS-1$
		}
		
		return layer;
	}	
	
	private Object mementoToStyle(final String styleId, final String xmlMemento) throws WorkbenchException{
        XMLMemento memento = XMLMemento.createReadRoot(new StringReader(xmlMemento));
        StyleContent styleContent = SyleContentFactory.getStyleContentFor(styleId);
        Object style = styleContent.load(memento);
        return style;
	}

	private List<IService> constructServices( URL url) throws IOException {
		List<IService> availableServices = new ArrayList<IService>();//services already in catalog
		IServiceFactory factory = new ServiceFactoryImpl();
	        
	    // IOException used to report a problem connecting; usually we only have one
	    // Service willing to try with a given set of parameters so this works out okay
	    //
    	List<IService> possible = factory.createService(url);
        for( Iterator<IService> iterator = possible.iterator(); iterator.hasNext(); ) {
        	IService service = iterator.next();
        	if (service == null) continue;
            availableServices.add(service);
            if (service instanceof ISessionService){
            	((ISessionService) service).setConnectionProvider(dbProvider);
            }
        }
        return availableServices;
    }
}

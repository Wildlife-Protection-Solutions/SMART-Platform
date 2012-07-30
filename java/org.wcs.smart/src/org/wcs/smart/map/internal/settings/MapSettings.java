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
package org.wcs.smart.map.internal.settings;

import java.awt.Color;
import java.io.File;
import java.io.FileFilter;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ICatalog;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.internal.ui.UiPlugin;
import net.refractions.udig.libs.internal.Activator;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.StyleContent;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.StyleBlackboard;
import net.refractions.udig.project.internal.StyleEntry;
import net.refractions.udig.project.ui.ApplicationGIS;
import net.refractions.udig.ui.palette.ColourScheme;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.emf.common.util.URI;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.osgi.framework.Bundle;
import org.wcs.smart.SmartProperties;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.SmartDB;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Envelope;

/**
 * This class is responsible of maintaining the setting done for a user in the savedMap view.
 * <p>
 * This state object offers functions to load and save the map's settings from database. 
 * </p>
 * 
 * 
 * @author Mauricio Pazos
 *
 */
public class MapSettings {
	
	private static MapSettings THIS = new MapSettings();  
	
	private Employee user = null;

	/** maintains the map shared for all users*/
	private MapRegister sharedMap = null; 
	
	private MapSettings(){
		// singleton
	}
	
	/**
	 * Returns the instance of Map setting for the user.
	 * <p>
	 * It the user change a new instance of {@link MapSettings} is created. In this case you should provide a new savedMap.
	 * </p>
	 * @param userId	user id should be not Null and not  empty string "";
	 * 
	 * @return {@link MapSettings}
	 */
	public static synchronized MapSettings getInstance(final Employee employee){
		assert employee != null; 
//		TODO UNCOMMENT		
//		if ( (THIS.user != null) && (THIS.user.getId().equals(employee.getId()))) {
//			
//			return THIS;
//			
//		} else {

			THIS = new MapSettings();

			// restores the saved savedMap settings if they exists
			THIS.user = employee;
			THIS.sharedMap  = restoreSharedMap();

			return THIS;
//		}
	}

	/**
	 * Makes a {@link MapRegister} taking the present values in the map provided. 
	 * <p>
	 * If the map contains new layers added by the administrator, they are added 
	 * to the shared map (common map for all users). If the layer is associate to a file,
	 * it will be copied in the filestore directory.
	 * </p>
	 * 
	 * @param map 
	 * 
	 * @return a {@link MapRegister} instance. Null value when the method find error.
 	 */
	private MapRegister createMapRegister(final Map map, final SmartUserLevel userLevel){
	
		MapRegister mapRegister = null;
		try {
			// creates the map settings and the list of layers settings 
			List<LayerRegister> layerRegisterList = createLayerRegisterList(userLevel, map.getLayersInternal());

			URI id = map.getID();		
			java.net.URI uri = new java.net.URI(id.toString());
			String name = map.getName();
			BrewerPalette colorPalette = map.getColorPalette();
			
			Envelope envelope = map.getBounds(new NullProgressMonitor());
			String wktEnvelope = GeometryUtil.envelopToWKT(envelope);
			
			mapRegister = new MapRegister(uri, name, colorPalette, wktEnvelope , layerRegisterList);
			
		} catch (Exception e) {
			log(Status.ERROR, e.getMessage());
		}
		return mapRegister;
	}

	/**
	 * creates the layer settings using the values present in the layer list
	 * 
	 * @param userlevel
	 * @param layerList
	 * @return the list of {@link LayerRegister} 
	 */
	private List<LayerRegister> createLayerRegisterList(SmartUserLevel userLevel, List<Layer> layerList) throws Exception{
		
		List<LayerRegister> layerRegisterList= new LinkedList<LayerRegister>();  
		synchronized(layerList){
			
			for (Layer layer : layerList) {

				// saves the layers settings
		        List<StyleRegister> styleRegisterList = new LinkedList<StyleRegister>();

				final StyleBlackboard styleBlackboard = layer.getStyleBlackboard();
		        List<StyleEntry> stylelackboardContent = styleBlackboard.getContent();
		        synchronized(stylelackboardContent){
					for (StyleEntry styleEntry : stylelackboardContent) {
			        	 
			        	 String id = styleEntry.getID();
			        	 
			             String styleMemento = styleToMemento(styleEntry);

				         StyleRegister styleRegister = new StyleRegister(id, styleMemento);
						 styleRegisterList.add( styleRegister );
					}
		        }
				
				final String crs = layer.getCRS().toWKT();
				final String cql = ECQL.toCQL(layer.getFilter());
				
				IGeoResource geoResource = layer.getGeoResource();
				java.net.URI uri = geoResource.getID().toURI();

				// checks if the layer is present in the original map
				final String name = layer.getName();
				final ColourScheme colourSchema = layer.getColourScheme();
				final Color defaultColor = layer.getDefaultColor();
				
				final String envelop = GeometryUtil.envelopToWKT(layer.getBounds(null, layer.getCRS()) );

				final Double maxScaleDenominator = layer.getMaxScaleDenominator();
				final Double minScaleDenominator = layer.getMinScaleDenominator();
				
				if( isNewLayer(name) ){

					// only administrator level can add new layers
					if(userLevel == SmartUserLevel.ADMIN){
						if(uri != null){
							
							if(URIUtil.isFileURI(uri) ){
								uri = importFile(uri);
							}
						}
						LayerRegister layerRegister = new LayerRegister (
																name, colourSchema, uri, crs, cql ,defaultColor, 
																maxScaleDenominator, minScaleDenominator, envelop, styleRegisterList);
						
						layerRegisterList.add(layerRegister);
					}
				} else {
					LayerRegister layerRegister = new LayerRegister (
															name, colourSchema, uri, crs, cql, defaultColor,
															maxScaleDenominator, minScaleDenominator, envelop, styleRegisterList);
					layerRegisterList.add(layerRegister);
				}
			}
		}
		return layerRegisterList;
	}

	/**
	 * Return the string used to set the memento object
	 * @param styleId
	 * @return
	 */
	private String styleToMemento(final StyleEntry es) {

			return es.getMemento();
			
	}
	
	private Object mementoToStyle(final String styleId, final String xmlMemento) throws WorkbenchException{
		
        XMLMemento memento = XMLMemento.createReadRoot(new StringReader(xmlMemento));
        StyleContent styleContent = SyleContentFactory.getStyleContentFor(styleId);

        Object style = styleContent.load(memento);
        
        return style;
		
	}
	
	/**
	 * It is true if the layer is not present in the original map (the map shared by all the users).
	 *  
	 * @param layerName
	 * @return true if is a new layer, false in other case
	 */
	private boolean isNewLayer(final String layerName) {
		
		if(this.sharedMap == null ){
			return true;
		} 

		for (LayerRegister sharedLayer : sharedMap.getLayerList()) {
			if (layerName.equals(sharedLayer.getName())) {
				return false; // the layer exist
			}
		}
		return true;
		
	}

	/**
	 * Applies the custom settings to the map.
	 * <p>
	 * The provided map will be updated with those layers imported by the user 
	 * and the custom setting done in each layer.
	 * </p> 
	 * 
	 * @param currentMap the displayed map
	 */
	public synchronized void applyTo(Map currentMap) {

		if(	this.sharedMap == null ) {
			// there isn't settings saved, so nothing to do.
			return;
		}
		
		// adds the shared layers which are not present in the currentMap (displayed by smart)
		List<ILayer> layers = currentMap.getMapLayers();
		synchronized (layers) {
			
			for (LayerRegister sharedLayer : this.sharedMap.getLayerList()) {
				
				LayerRegister foundSharedLayer = null;
				for (ILayer layer : layers) {
					
					if(sharedLayer.getName().equals(layer.getName())){
						
						foundSharedLayer = sharedLayer;
						break;
					}
				}
				if(foundSharedLayer == null){
					// the register layer is not found in the map, then it must be added
					try {
						java.net.URI uri = sharedLayer.getURI();
						
						addLayer( currentMap, uri.toURL(), sharedLayer.getName());
						
					} catch (MalformedURLException e) {
						log(Status.ERROR, e.getMessage());
					}
				}
			}
		} // postcondition: the currentMap has got all the shared layer
		
		
		// retrieves the customization done for this user
		String jsonMap = MapSettingsStore.findById(this.user.getId());
	    
	    GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues(); 
		Gson gson = gsonBuilder.create();
	    
	    MapRegister userMap = gson.fromJson(jsonMap, MapRegister.class);
		
		// updates the map with the user settings
		updateMap(currentMap, userMap );
		
		// for each layer sets the user customization
		List<Layer> newLayerList = currentMap.getLayersInternal();
		synchronized (newLayerList) {
			for (Layer layer : newLayerList) {

				LayerRegister foundSettings = null;
				for (LayerRegister sharedLayer : userMap.getLayerList()) {

					if (layer.getName().equals(sharedLayer.getName())) {

						foundSettings = sharedLayer;
						break;
					}
				}
				if (foundSettings != null) {
					updateLayer(layer, foundSettings);
				}
				
			}
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

		
	  // TODO map.setColourScheme(savedMap.gettColourScheme());
		
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

			// set the envelop
			Envelope envelope = GeometryUtil.wktToEnvelop(savedLayer.getEnvelope());
			ReferencedEnvelope bounds = new ReferencedEnvelope(envelope, crs);
			layer.setBounds(bounds);
			
	
			// create a new style for the layer setting its values with those present in the registry
			StyleBlackboard updatedStyleBlackboard = (StyleBlackboard) layer.getStyleBlackboard().clone();
			
			// traverse the current style blackboard if the value is in the register then the clone is update
			// with these values
			List<StyleRegister> styleRegisterList = savedLayer.getStyleRegisterList();
			for (StyleRegister styleRegister : styleRegisterList) {
				
				// search the style to set the register value
				for (StyleEntry newStyleEntry : layer.getStyleBlackboard().getContent()) {

					if (newStyleEntry.getID().equals(styleRegister.getId())) {

						// set the register values in the current style
						Object style = mementoToStyle(styleRegister.getId(), styleRegister.getMemento());
						updatedStyleBlackboard.put(newStyleEntry.getID(), style);
					}
				}
			}
			// change the current by the cloned and updated
			layer.setStyleBlackboard(updatedStyleBlackboard);
			//layer.refresh(null);
			
		} catch (Exception e) {
			e.printStackTrace();
			log(IStatus.ERROR, "I cannot create a layer for "+ savedLayer.getURI().toString() +". Cause: "+ e.getMessage() );
		}
		
		return layer;
	}	

	/**
	 * Adds the layers associated to the URL.
	 * @param map map where the layers will be added
	 * @param url where the resource is
	 * @param name layer's name  
	 */
    private void addLayer( Map map, URL url, String name ) {

    	NullProgressMonitor monitor = new NullProgressMonitor();

    	ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
        
    	try {
    		// removes the fragment from the URL
        	URL urlWithoutFragment = new URL(url.getProtocol(), url.getHost(), url.getFile() );

        	List<IGeoResource> geoResources = null;
			List<IResolve> resolveList = catalog.find(urlWithoutFragment, monitor);
			// it doesn't exist a service for the url then create one and try again
			if(resolveList.isEmpty()){
				// requires url without fragment
				List<IService> newServices = catalog.constructServices(urlWithoutFragment, monitor);
				for (IService service : newServices) {
		            catalog.add(service);
				}
				resolveList = catalog.find(url, monitor);
				assert !resolveList.isEmpty();
			} 
			// the service for the url exist then gets the resource
	     	for( IResolve resolve : resolveList ) {

            	geoResources = new ArrayList<IGeoResource>();
            	List<IResolve> members = resolve.members(monitor);
                if (members.size() < 1 && resolve.canResolve(IGeoResource.class)) {
                	geoResources.add(resolve.resolve(IGeoResource.class, monitor));

                } else if (members.get(0).canResolve(IGeoResource.class)) {
                	
                    for( IResolve tmp : members ) {
                        IGeoResource finalResolve = tmp.resolve(IGeoResource.class, monitor);
                        geoResources.add(finalResolve);
                    }
                }
	        }
	        List< ? extends ILayer> addedLayers = ApplicationGIS.addLayersToMap(map, geoResources, -1);
	        assert addedLayers.size() != 0;
    	} catch (Exception e){
    		log(Status.ERROR, e.getMessage());
    	}
    }
	
	/**
	 * Restores the shared map. 
	 * @param savedMap
	 * @param MapSettings
	 * @return {@link MapRegister} the map chared for all users
	 */
	private static MapRegister restoreSharedMap(){
	
		String strMap = MapSettingsStore.findAllSharedLayers();
	    GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues(); 
		Gson gson = gsonBuilder.create();
	    
	    MapRegister map = gson.fromJson(strMap, MapRegister.class);
		
		return map;
	}


	/**
	 * Saves the lastSavedMap setting in the database
	 * 
	 * @param lastSavedMap lastSavedMap to be save
	 */
	public synchronized void save(final Map map) {
		
		try{
			MapRegister mapRegister = createMapRegister(map, this.user.getSmartUserLevel() ); 
			
			 GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues(); 
			 Gson gson = gsonBuilder.create();
			 String jsonMap = gson.toJson(mapRegister);

			// all users can save theirs settings
			MapSettingsStore.save(this.user.getId(), jsonMap);

			if(SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.ADMIN){
				
				MapSettingsStore.saveShared(jsonMap);
			}
			
		} catch (Exception e) {
			log(Status.ERROR, e.getMessage());
		} 
	}


	/**
	 * Import the file associated to the layer inthe ./data/filestore/ directory
	 *    
	 * @param srcUri file to import
	 * @param layerName 
	 * @return {@link java.net.URI} if the copy process was successful. null in other case.
	 */
	private java.net.URI importFile(final java.net.URI srcUri){
		
		assert srcUri != null;
		
		// creates the "filestore" folder if it is necessary
		
		String fileStorePath = SmartProperties.getInstance().getProperty(SmartProperties.FILESTORE_KEY);
		File fileStoreDirectory = new File(fileStorePath);
		if(!fileStoreDirectory.exists()){
			fileStoreDirectory.mkdir();
		}

		java.net.URI trgUri = null;
		try {

			String srcPath = srcUri.getRawPath();
			if (!containsFileStoreDirectory(srcPath, fileStorePath)) {

				// copies the file to filestore directory
				File[] srcFileList = createSourceFileList(srcPath);

				for (int i = 0; i < srcFileList.length; i++) {
					
					String targetName = srcFileList[i].getName();

					File trgFile = new File(fileStorePath + File.separator + targetName);
					
					FileUtils.copyFile(srcFileList[i], trgFile);
				}
			}	 
			String fileName = new File(srcPath).getName();
			String filePath = "file:"+ fileStoreDirectory.getCanonicalPath() +  File.separator +fileName;
			
			java.net.URI uri = URIUtil.fromString(filePath);
			trgUri = new java.net.URI(srcUri.getScheme(), srcUri.getHost(), uri.getPath(), srcUri.getFragment() );

		} catch (Exception e) {
			log(Status.ERROR, e.getMessage());
		}
		return trgUri;
	}
	
	
	/**
	 * Creates the list of source layers based on the srcPath
	 * 
	 * @param srcPath
	 * @return the list of path that match with the srcPath
	 */
	private File[] createSourceFileList(final String srcPath) {

		File srcFile = new File(srcPath);
		
		//creates a pattern like [path]/layerName.*, then retrieves all file that match
		//String fullFileName= pathFragments[pathFragments.length];
		String fullFileName= srcFile.getName();
		
		String layerName = fullFileName.split("[.]")[0];

		
		// makes the path
		int i  = srcPath.indexOf(fullFileName);
		assert i > -1;
		String path = srcPath.substring(0,i);
		
		File dir = new File(path);

		String pattern = layerName + ".*";
		FileFilter filter = new WildcardFileFilter(pattern);

		File[] fileList = dir.listFiles(filter);
		
		return fileList;
	}

	/**
	 * checks it the smart fileStore is in the file path
	 * @param filePath
	 * @param fileStorePath
	 * @return true if filestorePath is part of filePath
	 */
	private boolean containsFileStoreDirectory(final String filePath, final String fileStorePath) {

		String fileStorePathWithoutPoint  = fileStorePath.substring(1);// rempves the point
		
		
		return filePath.contains(fileStorePathWithoutPoint);
	}

	private void log(final int status, final String message) {

		final Bundle bundle = Platform.getBundle(Activator.ID);
		Platform.getLog(bundle).log(new Status(status, UiPlugin.ID, message));
	}



}

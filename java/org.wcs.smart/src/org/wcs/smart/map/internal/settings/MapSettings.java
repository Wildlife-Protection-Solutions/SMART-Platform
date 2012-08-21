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
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.refractions.udig.catalog.CatalogPlugin;
import net.refractions.udig.catalog.ICatalog;
import net.refractions.udig.catalog.ID;
import net.refractions.udig.catalog.IGeoResource;
import net.refractions.udig.catalog.IResolve;
import net.refractions.udig.catalog.IService;
import net.refractions.udig.core.internal.CorePlugin;
import net.refractions.udig.project.ILayer;
import net.refractions.udig.project.StyleContent;
import net.refractions.udig.project.internal.Layer;
import net.refractions.udig.project.internal.Map;
import net.refractions.udig.project.internal.ProjectPackage;
import net.refractions.udig.project.internal.StyleBlackboard;
import net.refractions.udig.project.internal.StyleEntry;
import net.refractions.udig.project.internal.commands.AddLayersCommand;
import net.refractions.udig.project.internal.commands.DeleteLayersCommand;
import net.refractions.udig.ui.palette.ColourScheme;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.hibernate.Session;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.util.GeometryUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.vividsolutions.jts.geom.Envelope;

/**
 * This class is responsible of maintaining the setting done for a user in the map.
 * <p>
 * This state object offers functions to load and save the map's settings from database.  It is a singleton object, to get the instance
 * you should send the {@link #getInstance(Employee)} message. The {@link Empolyee} object is the current user (or session user). 
 * If the user is changed (a new login action) a new singleton object will be created consistently with this user, 
 * thus the setting of the new user will be available.
 * </p>
 * <p> 
 * Those external files load to this map, by the administrator user,  will be imported to the ./data/filestore/<CAID>/maps directory.
 * </p>
 * 
 * <pre>
 * <b>Usage:</b>
 * The following examples show how to load and save the customization done for the current user.
 *   
 *  // loading and applying the custom settings to the map
 *	Map map = ...
 *	Employee user = ...
 *
 *	MapSettings settings = MapSettings.getInstance(user); 
 * 	settings.applyTo(map);
 *  
 *  ...
 *  
 *  // saving the map's custom settingS
 *    
 *	Map map = ...
 *	Employee user ...
 *
 *	MapSettings settings = MapSettings.getInstance(user);
 *	settings.save(map);
 * 
 * </pre>
 * 
 * 
 * @author Mauricio Pazos
 *
 */
public class MapSettings {
	
	/**
	 * 
	 */
	public static final String BASEMAP_BLACKBOARD_KEY = "org.wcs.smart.basemaplayers";

	public static final String MAP_DIRECTORY = "maps";
	
	/** settings for the current user */
	private static MapSettings THIS = new MapSettings();  

	/* the basemap definition being used */
	private BasemapDefinition baseMap = null;
	
	private MapSettings(){
		// singleton
	}
	
	/**
	 * Returns the instance of Map setting for the user.
	 * <p>
	 * If the user is changed, a new instance of {@link MapSettings} will be created. If the user had saved the map customization in 
	 * previous session, these settings will be restored.
	 * </p>
	 * @param userId	user id should be not Null and not  empty string "";
	 * 
	 * @return {@link MapSettings}
	 */
	public static synchronized MapSettings getInstance(final BasemapDefinition baseMap){
		assert baseMap != null;
		THIS = new MapSettings();
		THIS.baseMap = baseMap;
		return THIS;
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
	 * @param userLevel
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
			ColourScheme colourScheme = map.getColourScheme();
			
			mapRegister = new MapRegister(uri, name, colorPalette, colourScheme,  layerRegisterList);
			
		} catch (Exception e) {
			SmartPlugIn.log(Status.ERROR, e.getMessage(), e);
		}
		return mapRegister;
	}

	/**
	 * creates the layer settings using the values present in the layer list
	 * 
	 * @param userlevel
	 * @param layerList
	 * 
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
				
				String envelop = null;
				if (!layer.getBounds(null, layer.getCRS()).isNull()){
					envelop = GeometryUtils.envelopToWKT(layer.getBounds(null, layer.getCRS()) );
				}
				 

				final Double maxScaleDenominator = layer.getMaxScaleDenominator();
				final Double minScaleDenominator = layer.getMinScaleDenominator();
				
				// only administrator level can add new layers
				if(userLevel == SmartUserLevel.ADMIN){
					if(uri != null){
						if(URIUtil.isFileURI(uri) ){
							uri = importFile(uri);
						}
					}
					LayerRegister layerRegister = new LayerRegister (
									name, colourSchema, uri, crs, cql ,defaultColor, 
								maxScaleDenominator, minScaleDenominator, 
								envelop, styleRegisterList);
						
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
	 * Applies the custom settings to the map.
	 * <p>
	 * The provided map will be updated with those layers imported by the user 
	 * and the custom setting done in each layer.
	 * </p> 
	 * 
	 * @param currentMap the displayed map
	 */
	public synchronized void applyTo(Map currentMap) {
		
		//turn off map events
		currentMap.eSetDeliver(false);
		
		List<ILayer> layers = currentMap.getMapLayers();

		//keep track of current basemap layers
		List<ILayer> layersToRemove = (List<ILayer>) currentMap.getBlackboard().get(BASEMAP_BLACKBOARD_KEY);

		//new basemap layers
		List<ILayer> basemapLayers = new ArrayList<ILayer>();
		
		//get map definition selected
		String jsonMap = this.baseMap.getMapDef();
	    GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues(); 
		Gson gson = gsonBuilder.create();
	    
	    MapRegister userMap = gson.fromJson(jsonMap, MapRegister.class);
		
	    //determine which layers need to be added/removed
	    List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
	    synchronized(layers){
	    	for (LayerRegister sharedLayer : userMap.getLayerList()) {
				LayerRegister foundSharedLayer = null;
				for (ILayer layer : layers) {
					if(sharedLayer.getName().equals(layer.getName())){
						foundSharedLayer = sharedLayer;
						if(layersToRemove != null){
							layersToRemove.remove(layer);
						}
						basemapLayers.add(layer);
						break;
					}
				}
				if(foundSharedLayer == null){
					// the register layer is not found in the map, then it must be added
					List<IGeoResource> resources = prepareLayers( currentMap, sharedLayer.getURI(), sharedLayer.getName());
					if (resources != null){
						toAdd.addAll(resources);
					}
				}
			}
	    }
	    
	    //delete old basemaps layers that are not a part of the new basemap
	    if (layersToRemove != null && layersToRemove.size() > 0){
	    	for (Iterator<ILayer> iterator = layersToRemove.iterator(); iterator.hasNext();) {
	    		ILayer layer = (ILayer) iterator.next();
	    		if (layer.getMap() == null){
	    			iterator.remove();
	    		}
			}
	    	if (layersToRemove.size() > 0){
	    		currentMap.sendCommandSync( new DeleteLayersCommand(layersToRemove.toArray(new ILayer[layersToRemove.size()]) ));
	    	}
	    }
	    
	    //add new basemap layers
	    if (toAdd.size() > 0){
            AddLayersCommand alCommand = new AddLayersCommand(toAdd, 0);
            currentMap.sendCommandSync(alCommand);
            List< ? extends ILayer> addedLayers = alCommand.getLayers();
            
	    	assert addedLayers.size() != 0;
	    	basemapLayers.addAll(addedLayers);
	    }

	    List<Layer> orderedLayers = new ArrayList<Layer>();
	    for (LayerRegister sharedLayer : userMap.getLayerList()) {
	    	for (ILayer layer : basemapLayers) {	
	    		if (layer.getName().equals(sharedLayer.getName())) {
	    			orderedLayers.add((Layer)layer);
	    			break;
	    		}
	    	}
	    }
	    //order layers 
	    currentMap.getContextModel().eSetDeliver(false);
	    currentMap.getLayersInternal().removeAll(orderedLayers);
	    currentMap.getLayersInternal().addAll(0,orderedLayers);
	    currentMap.getContextModel().eSetDeliver(true);
	    
		//update basemap layer settings
	    ArrayList<ILayer> basemapCopy = new ArrayList<ILayer>(basemapLayers);
		currentMap.getBlackboard().put(BASEMAP_BLACKBOARD_KEY, basemapCopy);
		
		// updates the map with the user settings
		updateMap(currentMap, userMap );

		for (ILayer layer : basemapLayers) {
			LayerRegister foundSettings = null;
			for (LayerRegister sharedLayer : userMap.getLayerList()) {
				if (layer.getName().equals(sharedLayer.getName())) {
					foundSettings = sharedLayer;
					break;
				}
			}
			if (foundSettings != null && layer.getMap() != null) {
				((Layer)layer).eSetDeliver(false);
				updateLayer(((Layer)layer), foundSettings);
				((Layer)layer).eSetDeliver(true);
				((Layer)layer).eNotify(new ENotificationImpl((InternalEObject)layer, Notification.SET, ProjectPackage.LAYER__STYLE_BLACKBOARD,
		                layer.getStyleBlackboard(), layer.getStyleBlackboard()));
			}
		}

		//turn back on events
		currentMap.eSetDeliver(true);
		if (currentMap.getRenderManager() != null){
			currentMap.getRenderManager().refresh(null);
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

			// set the envelop
			if (savedLayer.getEnvelope() != null){
				Envelope envelope = GeometryUtils.wktToEnvelop(savedLayer.getEnvelope());
				ReferencedEnvelope bounds = new ReferencedEnvelope(envelope, crs);
				layer.setBounds(bounds);
			}
			
	
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
			SmartPlugIn.log(Status.ERROR, "Cannot create layer for " + savedLayer.getURI().toString() + ": " + e.getMessage(), e);
		}
		
		return layer;
	}	

	/**
	 * Adds the layers associated to the URL.
	 * @param map map where the layers will be added
	 * @param url where the resource is
	 * @param name layer's name  
	 */
    //private void addLayer( Map map, URL url, String name ) {
	private List<IGeoResource> prepareLayers( Map map, java.net.URI uri, String name ) {

    	NullProgressMonitor monitor = new NullProgressMonitor();

    	ICatalog catalog = CatalogPlugin.getDefault().getLocalCatalog();
        
    	try {
    		// removes the fragment from the URL
    		URL url  = new URL(null, uri.toString(), CorePlugin.RELAXED_HANDLER);

        	List<IGeoResource> geoResources = null;
			List<IResolve> resolveList = catalog.find(url, monitor);
			// it doesn't exist a service for the url then create one and try again
			if(resolveList.isEmpty()){
				// requires url without fragment
				URL urlWithoutFragment = new URL(url.getProtocol(), url.getHost(),url.getPort(), url.getFile(), CorePlugin.RELAXED_HANDLER );
				List<IService> newServices = catalog.constructServices(urlWithoutFragment, monitor);
				for (IService service : newServices) {
		            catalog.add(service);
				}
				resolveList = catalog.find(new ID(uri), monitor);
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
//	        List< ? extends ILayer> addedLayers = ApplicationGIS.addLayersToMap(map, geoResources, -1);
//	        assert addedLayers.size() != 0;
	        return geoResources;
    	} catch (Exception e){
    		SmartPlugIn.log(Status.ERROR, e.getMessage(), e);
    		return null;
    	}
    }

	/**
	 * Saves the lastSavedMap setting in the database
	 * 
	 * @param lastSavedMap lastSavedMap to be save
	 */
	public synchronized void save(final Map map) {
		
		try{
			MapRegister mapRegister = createMapRegister(map, SmartDB.getCurrentEmployee().getSmartUserLevel() ); 
			
			 GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues(); 
			 Gson gson = gsonBuilder.create();
			 String jsonMap = gson.toJson(mapRegister);
			 this.baseMap.setMapDef(jsonMap);
			 
			// all users can save theirs settings
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				s.saveOrUpdate(this.baseMap);
				s.getTransaction().commit();
			}catch (Exception ex){
				if (s.getTransaction().isActive()){
					s.getTransaction().rollback();
				}
				SmartPlugIn.displayLog(null, "Could not save basemap." + ex.getMessage(), ex);
			}finally{
				s.close();
			}
			 
//			MapSettingsStore.save(this.user.getId(), jsonMap);
//			if(SmartDB.getCurrentEmployee().getSmartUserLevel() == Employee.SmartUserLevel.ADMIN){
//				MapSettingsStore.saveShared(jsonMap);
//			}
			
		} catch (Exception e) {
			SmartPlugIn.log(Status.ERROR, e.getMessage(), e);
		} 
	}


	/**
	 * Imports the file associated to the layer inthe ./data/filestore/ directory
	 *    
	 * @param srcUri file to import
	 * @param layerName 
	 * @return {@link java.net.URI} if the copy process was successful. null in other case.
	 */
	private java.net.URI importFile(final java.net.URI srcUri){
		
		assert srcUri != null;
		
		// creates the "filestore" folder if it is necessary


		File fileStoreDirectory = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation(), MAP_DIRECTORY);
		if(!fileStoreDirectory.exists()){
			fileStoreDirectory.mkdir();
		}

		java.net.URI trgUri = null;
		try {

			String srcPath = srcUri.getRawPath();
			if (!containsFileStoreDirectory(srcPath, fileStoreDirectory.getAbsolutePath())) {

				// copies the file to filestore directory
				File[] srcFileList = createSourceFileList(srcPath);

				for (int i = 0; i < srcFileList.length; i++) {
					
					String targetName = srcFileList[i].getName();

					File trgFile = new File(fileStoreDirectory,targetName);
					
					FileUtils.copyFile(srcFileList[i], trgFile);
				}
			}	 
			String fileName = new File(srcPath).getName();
			StringBuilder pathBuilder = new StringBuilder(50);
			pathBuilder.append("file:")
						.append(fileStoreDirectory.getCanonicalPath())
						.append(File.separator )
						.append(fileName);
			
			java.net.URI uri = URIUtil.fromString(pathBuilder.toString());
			trgUri = new java.net.URI(srcUri.getScheme(), srcUri.getHost(), uri.getPath(), srcUri.getFragment() );

		} catch (Exception e) {
			SmartPlugIn.log(Status.ERROR, e.getMessage(), e);
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
	 * 
	 * @param filePath
	 * @param fileStorePath
	 * @return true if filestore Path is part of filePath
	 */
	private boolean containsFileStoreDirectory(final String filePath, final String fileStorePath) {

		String fileStorePathWithoutPoint  = fileStorePath.substring(1);// removes the point
		
		return filePath.contains(fileStorePathWithoutPoint);
	}
}

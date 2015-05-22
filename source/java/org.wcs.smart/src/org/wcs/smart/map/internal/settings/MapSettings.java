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
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.geotools.brewer.color.BrewerPalette;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.locationtech.udig.catalog.CatalogPlugin;
import org.locationtech.udig.catalog.ICatalog;
import org.locationtech.udig.catalog.ID;
import org.locationtech.udig.catalog.IGeoResource;
import org.locationtech.udig.catalog.IResolve;
import org.locationtech.udig.catalog.IService;
import org.locationtech.udig.core.internal.CorePlugin;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.StyleContent;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.Map;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.ProjectPackage;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.locationtech.udig.project.internal.commands.AddLayersCommand;
import org.locationtech.udig.project.internal.commands.ChangeCRSCommand;
import org.locationtech.udig.project.internal.commands.DeleteLayersCommand;
import org.locationtech.udig.project.internal.render.impl.RenderManagerImpl;
import org.locationtech.udig.project.internal.render.impl.ViewportModelImpl;
import org.locationtech.udig.ui.palette.ColourScheme;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.BasemapDefinition;
import org.wcs.smart.ca.Employee;
import org.wcs.smart.ca.Employee.SmartUserLevel;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.internal.Messages;
import org.wcs.smart.util.GeometryUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

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
	
	private static final String SMARTBM_FILE_PROTOCOL = "smartbm"; //$NON-NLS-1$

	/**
	 * 
	 */
	public static final String BASEMAP_BLACKBOARD_KEY = "org.wcs.smart.basemaplayers"; //$NON-NLS-1$

	public static final String MAP_DIRECTORY = "maps"; //$NON-NLS-1$
	
	/** settings for the current user */
	private static MapSettings THIS = new MapSettings();  

	/* the basemap definition being used */
	private BasemapDefinition baseMap = null;
	private List<File> tmpImportedFiles = null;
	
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

			java.net.URI uri = new java.net.URI(map.getID().toString());
			String name = map.getName();
			BrewerPalette colorPalette = map.getColorPalette();
			ColourScheme colourScheme = map.getColourScheme();
			String crs = map.getViewportModel().getCRS().toWKT();
			
			mapRegister = new MapRegister(uri, name, colorPalette, colourScheme,  layerRegisterList, crs);
			
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
					if (uri != null){
						LayerRegister layerRegister = new LayerRegister (
									name, colourSchema, uri, crs, cql ,defaultColor, 
								maxScaleDenominator, minScaleDenominator, 
								envelop, styleRegisterList, layer.isVisible());
						
						layerRegisterList.add(layerRegister);
					}
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
		if (((RenderManagerImpl)currentMap.getRenderManager()) != null){
			((RenderManagerImpl)currentMap.getRenderManager()).disableRendering();
		}
		
		// get map definition selected
		String jsonMap = this.baseMap.getMapDef();
		GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
		Gson gson = gsonBuilder.create();
		final MapRegister userMap = gson.fromJson(jsonMap, MapRegister.class);
					
		try {
			List<ILayer> currentMapLayers = currentMap.getMapLayers();

			// keep track of current basemap layers
			@SuppressWarnings("unchecked")
			List<ILayer> layersToRemove = (List<ILayer>) currentMap
					.getBlackboard().get(BASEMAP_BLACKBOARD_KEY);

			// new basemap layers
			List<ILayer> basemapLayers = new ArrayList<ILayer>();

			// determine which layers need to be added/removed
			List<IGeoResource> toAdd = new ArrayList<IGeoResource>();
			final HashMap<URL, LayerRegister> definitionMap = new HashMap<URL, LayerRegister>();

			synchronized (currentMapLayers) {
				for (LayerRegister basemapLayer : userMap.getLayerList()) {
					boolean found = false;
					for (ILayer mapLayer : currentMapLayers) {
						try {
							if (basemapLayer.getURI().equals(
									mapLayer.getID().toURI())) {
								found = true;
								if (layersToRemove != null) {
									layersToRemove.remove(mapLayer);
								}
								basemapLayers.add(mapLayer);
								definitionMap.put(mapLayer.getID(), basemapLayer);
								break;

							}
						} catch (Exception ex) {
							SmartPlugIn.log("restoring basemap", ex); //$NON-NLS-1$
						}
					}
					if (!found) {
						// the register layer is not found in the map, then it
						// must be added
						List<IGeoResource> resources = prepareLayers(
								currentMap, basemapLayer.getURI(),
								basemapLayer.getName());
						if (resources != null) {
							toAdd.addAll(resources);
							for (IGeoResource geo : resources) {
								definitionMap.put(geo.getIdentifier(), basemapLayer);
							}
						}
					}
				}
			}

			// delete old basemaps layers that are not a part of the new basemap
			if (layersToRemove != null && layersToRemove.size() > 0) {
				for (Iterator<ILayer> iterator = layersToRemove.iterator(); iterator
						.hasNext();) {
					ILayer layer = (ILayer) iterator.next();
					if (layer.getMap() == null) {
						iterator.remove();
					}
				}
				if (layersToRemove.size() > 0) {
					currentMap.sendCommandSync(new DeleteLayersCommand(
							layersToRemove.toArray(new ILayer[layersToRemove
									.size()])));
				}
			}

			// add new basemap layers
			if (toAdd.size() > 0) {
				AddLayersCommand alCommand = new AddLayersCommand(toAdd, 0);
				currentMap.sendCommandSync(alCommand);
				List<? extends ILayer> addedLayers = alCommand.getLayers();
				assert addedLayers.size() != 0;
				basemapLayers.addAll(addedLayers);
				
				//here we update our definitionmap as we use
				//layer.getResource to do lookups later
				//and layer.getResource returns a different resource
				//than the georesource used to create the layer
				//and currently in the definition map
				for (ILayer l : addedLayers){
					definitionMap.put(l.getID(), definitionMap.get(l.getGeoResource().getIdentifier()));
				}
			}

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
			ArrayList<ILayer> basemapCopy = new ArrayList<ILayer>(basemapLayers);
			currentMap.getBlackboard().put(BASEMAP_BLACKBOARD_KEY, basemapCopy);

			// updates the map with the user settings
			updateMap(currentMap, userMap);

			for (ILayer layer : basemapLayers) {
				LayerRegister info = definitionMap.get(layer.getID());

				((Layer) layer).setVisible(info.getVisible());
				((Layer) layer).eSetDeliver(false);
				updateLayer(((Layer) layer), info);
				((Layer) layer).eSetDeliver(true);
				((Layer) layer).eNotify(new ENotificationImpl(
						(InternalEObject) layer, Notification.SET,
						ProjectPackage.LAYER__STYLE_BLACKBOARD, layer
								.getStyleBlackboard(), layer
								.getStyleBlackboard()));

			}

			// turn back on events
		
		} finally {
			currentMap.eSetDeliver(true);
			if (((RenderManagerImpl) currentMap.getRenderManager()) != null) {
				((RenderManagerImpl) currentMap.getRenderManager())
						.enableRendering();
			}
		}
		try {
			if (userMap.getCrsWkt() != null) {
				CoordinateReferenceSystem crs = CRS.parseWKT(userMap
						.getCrsWkt());
				if (!CRS.equalsIgnoreMetadata(crs, currentMap
						.getViewportModel().getCRS())) {
					ChangeCRSCommand command = new ChangeCRSCommand(crs);
					currentMap.sendCommandSync(command);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		((ViewportModelImpl)currentMap.getViewportModel()).zoomToExtent();
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
			SmartPlugIn.log(Status.ERROR, Messages.MapSettings_Error_CreateMapLayer + savedLayer.getURI().toString() + ": " + e.getMessage(), e); //$NON-NLS-1$
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
    		if (uri.getScheme().equals(SMARTBM_FILE_PROTOCOL)){
    			//smart basemap file; needs to re-create uri with absolute path
    			File fileStoreDirectory = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation(), MAP_DIRECTORY);
    			File path = new File(fileStoreDirectory, uri.getSchemeSpecificPart());
    			String fragment = uri.getFragment();
    			uri = path.toURI();
    			uri = new java.net.URI(uri.getScheme(), uri.getSchemeSpecificPart(), fragment);
    		}
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
				resolveList = catalog.find(new ID(url), monitor);
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
	 * 
	 * @return list of layers in the saved basemap; only return layers
	 * saved in the basemap; null if error occurs
	 */
	public synchronized void save(final Map map) throws Exception{
		tmpImportedFiles = new ArrayList<File>();
		try{
			Session s = HibernateManager.openSession();
			try{
				s.beginTransaction();
				//this point is where files are copied into the filestore
				//so if an error happens after this point we get files in the filestore
				//that may never get references to a basemap and not removed.
				MapRegister mapRegister = createMapRegister(map, SmartDB.getCurrentEmployee().getSmartUserLevel() ); 
			
				//when overwriting and existing file; this gets the
				//files currently in the basemap; below we
				//get the new files then delete any that don't eixst
				//in the new map
				List<File> currentFiles = getFilesToDelete(s);
			
				//set the basemap property
				GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues(); 
				Gson gson = gsonBuilder.create();
				String jsonMap = gson.toJson(mapRegister);
				this.baseMap.setMapDef(jsonMap);
				List<File> newFiles = getFilesToDelete(s);
			
				for (File f : newFiles){
					currentFiles.remove(f);
				}
				
				s.saveOrUpdate(this.baseMap);
				s.getTransaction().commit();
				
				//need to delete remaining currentFiles
				for (File f : currentFiles){
					f.delete();
				}
				
				//these layers are now a part of the basemap and should be flagged as such
				List<ILayer> backgroundlayers = new ArrayList<ILayer>();
				backgroundlayers.addAll(map.getLayersInternal());
				map.getBlackboard().put(BASEMAP_BLACKBOARD_KEY, backgroundlayers);
			}catch (final Exception ex){
				if (s.getTransaction().isActive()){
					s.getTransaction().rollback();
				}
				//try to remove any imported files
				try{
					for (File f : tmpImportedFiles){
						f.delete();
					}
				}catch (Exception ex2){
					SmartPlugIn.log("Error cleaning up filestore after import error.", ex2); //$NON-NLS-1$
				}
				throw ex;
				
			}finally{
				s.close();
				tmpImportedFiles.clear();
			}
		} catch (Exception e) {
			throw e;		
		} 
	}


	/**
	 * Imports the file associated to the layer into ./data/filestore/ directory
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
		List<File> localImportFiles = new ArrayList<File>();
		try {
			String srcPath = srcUri.getPath();
			String fileName = new File(srcPath).getName();
			
			if (!containsFileStoreDirectory(srcPath, fileStoreDirectory.getAbsolutePath())) {

				// copies the file to filestore directory
				File[] srcFileList = createSourceFileList(srcPath);
			
				//ensure we are not going to overwrite any files
				int index = -1;
				for (int i = 0; i < srcFileList.length; i++) {
					String targetName = srcFileList[i].getName();
					
					String rootName = FilenameUtils.getBaseName(targetName);
					String extension = FilenameUtils.getExtension(targetName);
					
					File trgFile = null;
					if (index >= 0){
						trgFile = new File(fileStoreDirectory,createFileName(rootName, index, extension));
					}else{
						trgFile = new File(fileStoreDirectory,targetName);
					}
					
					while(trgFile.exists()){
						index++;
						trgFile = new File(fileStoreDirectory,createFileName(rootName, index, extension));
					}
				}

				for (int i = 0; i < srcFileList.length; i++) {
					String targetName = srcFileList[i].getName();
					File trgFile = null;
					if (index >= 0){
						String rootName = FilenameUtils.getBaseName(targetName);
						String extension = FilenameUtils.getExtension(targetName);
						trgFile = new File(fileStoreDirectory, createFileName(rootName, index, extension));
					}else{
						trgFile = new File(fileStoreDirectory,targetName);
					}
					
					if (!srcFileList[i].getCanonicalFile().equals(trgFile.getCanonicalFile())){
						FileUtils.copyFile(srcFileList[i], trgFile);
						tmpImportedFiles.add(trgFile);
						localImportFiles.add(trgFile);
					}
				}
				if (index >=0 ){
					//update uri name
					String rootName = FilenameUtils.getBaseName(fileName);
					String extension = FilenameUtils.getExtension(fileName);
					fileName = createFileName(rootName, index, extension);
				}
			}	 
			
			
			//create a custom uri that uses smartbm protocol 
			//this allows the file to be relative
			//so if the data is copied to a new location
			//the basemap files still work;
			java.net.URI uri = new java.net.URI( SMARTBM_FILE_PROTOCOL, "//" + fileName, srcUri.getFragment()); //$NON-NLS-1$
			trgUri = uri;
		} catch (Exception e) {
			SmartPlugIn.displayLog(MessageFormat.format(Messages.MapSettings_FileImportError + "\n\n" + e.getMessage(), new Object[]{srcUri.getPath()}), e); //$NON-NLS-1$
			
			for (File f : localImportFiles){
				try{
					f.delete();
					tmpImportedFiles.remove(f);
				}catch (Exception ex2){
					SmartPlugIn.log(ex2.getMessage(), ex2);
				}
			}
			

		}
		return trgUri;
	}
	
	private String createFileName(String rawFileName, int postFix, String extension){
		return rawFileName + "_" + postFix + (extension.length() > 0 ? "." + extension : ""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}
	
	/**
	 * Creates the list of source layers based on the srcPath
	 * 
	 * @param srcPath
	 * @return the list of path that match with the srcPath
	 */
	private File[] createSourceFileList(final String srcPath) {

		File srcFile = new File(srcPath);
		File dir = srcFile.getParentFile();
		
		//creates a pattern like [path]/layerName.*, then retrieves all file that match
		String fullFileName= srcFile.getName();
		int index = fullFileName.lastIndexOf('.');
		String layerName = fullFileName;
		if (index >= 0){
			layerName = layerName.substring(0, index);
		}
		
		String pattern = layerName + ".*"; //$NON-NLS-1$
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
	 * @throws IOException 
	 */
	private boolean containsFileStoreDirectory(final String filePath, final String fileStorePath) throws IOException {
		File file = new File(filePath);
		File store = new File(fileStorePath);
		return (file.getCanonicalPath().startsWith(store.getCanonicalPath()));		
	}
	
	/**
	 * Delete any files associated with the map settings
	 */
	public ArrayList<File> getFilesToDelete(Session activeSession){
		// get map definition selected
		if (this.baseMap.getMapDef() == null){
			return new ArrayList<File>();
		}
		String jsonMap = this.baseMap.getMapDef();
		GsonBuilder gsonBuilder = new GsonBuilder().serializeSpecialFloatingPointValues();
		Gson gson = gsonBuilder.create();
		final MapRegister userMap = gson.fromJson(jsonMap, MapRegister.class);
		
		ArrayList<File> toDelete = new ArrayList<File>();
		
		for (LayerRegister basemapLayer : userMap.getLayerList()) {
			URI uri = basemapLayer.getURI();
			if (uri.getScheme().equals(SMARTBM_FILE_PROTOCOL)){
				//verify that this layer is not used in any other basemaps
				List<?> others = activeSession.createCriteria(BasemapDefinition.class)
						.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
						.add(Restrictions.ilike("mapDef", "%" + uri.toString() + "%")) //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						.add(Restrictions.ne("uuid", baseMap.getUuid())).list(); //$NON-NLS-1$
				if(others.size() > 0){
					//this is used by another layer leave it
					continue;
				}
				
				File fileStoreDirectory = new File(SmartDB.getCurrentConservationArea().getFileDataStoreLocation(), MAP_DIRECTORY);
    			File path = new File(fileStoreDirectory, uri.getSchemeSpecificPart());
    			if (path.exists()){
    				//this is a file; we should remove it
    				File[] files = createSourceFileList(path.getAbsolutePath());
    				for (File ff : files){
    					toDelete.add(ff);
    				}
				}
			}
			
		}

		return toDelete;
	}
}

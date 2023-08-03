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
package org.wcs.smart.udig.style;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.function.Consumer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.geotools.styling.Style;
import org.hibernate.Session;
import org.locationtech.udig.core.internal.ExtensionPointProcessor;
import org.locationtech.udig.core.internal.ExtensionPointUtil;
import org.locationtech.udig.project.ILayer;
import org.locationtech.udig.project.StyleContent;
import org.locationtech.udig.project.internal.Layer;
import org.locationtech.udig.project.internal.ProjectFactory;
import org.locationtech.udig.project.internal.ProjectPlugin;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.locationtech.udig.style.sld.SLD;
import org.locationtech.udig.style.sld.SLDContent;
import org.opengis.coverage.grid.GridCoverage;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.ca.ConservationAreaProperty;
import org.wcs.smart.ca.SmartStyle;
import org.wcs.smart.hibernate.QueryFactory;
import org.wcs.smart.util.UuidUtils;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

/**
 * uDig style utilities.  For saving and loading style blackboard objects
 * as strings.
 * 
 * @author Emily
 *
 */
public class StyleManager {

	public static final StyleManager INSTANCE = new StyleManager();
	
	/**
	 * ConservationAreaProperty key for default map layer styles.
	 * Default map layer styles are stored as JSON key value pairs to 
	 * smart saved style.
	 */
	public static final String CA_PROPERTY_KEY = "smart.map.styles.default";  //$NON-NLS-1$
	/**
	 * Extension point ID for map configuration extension
	 */
	public static final String MAP_CONFIG_EXT = "org.wcs.smart.map.config";  //$NON-NLS-1$
	/**
	 * Default style extension point name
	 */
	public static final String DEFAULT_STYLE_PNT = "defaultmaplayerstyle"; //$NON-NLS-1$
	
	private static final String ID_KEY = "id"; //$NON-NLS-1$
	private static final String VALUE_KEY = "value"; //$NON-NLS-1$
	
	private static final String KEY_KEY = "key";  //$NON-NLS-1$
	private static final String STYLE_KEY = "style";  //$NON-NLS-1$
	
	private StyleManager(){
		
	}
	
	/**
	 * 
	 * @return List of map layers which can have default styles associated with them
	 * 
	 * @throws CoreException
	 */
	public List<MapLayerDefaultStyle> getDefaultStyleMapLayers() throws CoreException{
	
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(MAP_CONFIG_EXT);
		if (extensionPoint == null) return Collections.emptyList();
	            
		IExtension[] extensions = extensionPoint.getExtensions();

		List<MapLayerDefaultStyle> items = new ArrayList<>();
	        // For each extension ...
	    for( int i = 0; i < extensions.length; i++ ) {
	    	IExtension extension = extensions[i];
	        IConfigurationElement[] elements = extension.getConfigurationElements();

	        // For each member of the extension ...
	        for( int j = 0; j < elements.length; j++ ) {
	        	IConfigurationElement element = elements[j];
	        	if (element.getName().equalsIgnoreCase(DEFAULT_STYLE_PNT)) {
	        		MapLayerDefaultStyle p = (MapLayerDefaultStyle) element.createExecutableExtension("class"); //$NON-NLS-1$
					items.add(p);				
				}	
	        }
	    }
	    return items;
	                  
	}
	
	/**
	 * Load the default map styles from the database for the given Conservation Area and
	 * return a key value pair map linking the map layer key to the style uuid string  
	 * @param ca
	 * @param session
	 * @return
	 */
	public Map<String, String> getDefaultStyles(ConservationArea ca, Session session){
		ConservationAreaProperty currentProperty = QueryFactory.buildQuery(session, ConservationAreaProperty.class,
				new Object[] { "conservationArea", ca }, //$NON-NLS-1$
				new Object[] { "key", StyleManager.CA_PROPERTY_KEY }).uniqueResult(); //$NON-NLS-1$
		if (currentProperty == null || currentProperty.getValue() == null ||
				currentProperty.getValue().isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> properties = new HashMap<>();
		JsonElement json = JsonParser.parseString(currentProperty.getValue());
		if (json instanceof JsonArray) {
			((JsonArray) json).forEach(e -> {
				JsonObject jsone = (JsonObject) e;
				String key = jsone.get(KEY_KEY).getAsString();
				String value = null;
				if (jsone.get(STYLE_KEY) != null && !jsone.get(STYLE_KEY).isJsonNull()) {
					value = jsone.get(STYLE_KEY).getAsString();
				}
				properties.put(key, value);
			});
		}
		return properties;
		
	}
	/**
	 * Creates/updates the conservation area property storing the default map layer styles
	 * with the information in the provided map.
	 * 
	 * @param ca
	 * @param allstyles
	 * @param session
	 */
	public void setDefaultStyles(ConservationArea ca, Map<String,String> allstyles, Session session) {
		ConservationAreaProperty currentProperty = QueryFactory
				.buildQuery(session, ConservationAreaProperty.class,
						new Object[] { "conservationArea", ca }, //$NON-NLS-1$
						new Object[] { "key", StyleManager.CA_PROPERTY_KEY }) //$NON-NLS-1$
				.uniqueResult();

		if (currentProperty == null) {
			currentProperty = new ConservationAreaProperty();
			currentProperty.setConservationArea(ca);
			currentProperty.setKey(StyleManager.CA_PROPERTY_KEY);
			session.persist(currentProperty);
		}

		JsonArray json = new JsonArray();
		for (Entry<String, String> ss : allstyles.entrySet()) {
			JsonObject j = new JsonObject();
			j.addProperty(KEY_KEY, ss.getKey());
			j.addProperty(STYLE_KEY, ss.getValue());
			json.add(j);
		}
		currentProperty.setValue(json.toString());
	}
	
	
	/**
	 * Finds the default smart style associated with the given map key.
	 * 
	 * @param ca
	 * @param mapKey
	 * @param session
	 * @return
	 */
	public SmartStyle getMapLayerDefaultStyle(ConservationArea ca, String mapKey, Session session) {
		ConservationAreaProperty currentProperty = QueryFactory.buildQuery(session, ConservationAreaProperty.class,
				new Object[] { "conservationArea", ca }, //$NON-NLS-1$
				new Object[] { "key", StyleManager.CA_PROPERTY_KEY }).uniqueResult(); //$NON-NLS-1$
		if (currentProperty == null || currentProperty.getValue() == null ||
				currentProperty.getValue().isEmpty()) {
			return null;
		}
		JsonElement json = JsonParser.parseString(currentProperty.getValue());
		if (json instanceof JsonArray) {
			JsonArray ajson = (JsonArray)json;
			for (Object kid : ajson) {
				if (kid instanceof JsonObject) {
					JsonObject jkid = (JsonObject)kid;
					String key = jkid.get(KEY_KEY).getAsString();
					if (key.equals(mapKey)) {
						if (jkid.get(STYLE_KEY) == null ) return null;
						if (jkid.get(STYLE_KEY).isJsonNull()) return null;
						String value = jkid.get(STYLE_KEY).getAsString();
						UUID suuid = UuidUtils.stringToUuid(value);
						
						return session.get(SmartStyle.class, suuid);
					}
				}
			}
		}
		return null;
	}
	
	/**
	 * Converts a mapping of id to style blackboard to a json string
	 * 
	 * @param styles
	 * @return
	 * @throws IOException
	 */
	public String asString(Map<String, StyleBlackboard> styles) throws IOException{
		StringWriter sw = new StringWriter();
		
		try(JsonWriter writer = new JsonWriter(sw)){
			writer.beginArray();
			for (Entry<String, StyleBlackboard> entry : styles.entrySet()){
				writer.beginObject();
				writer.name("resourceId").value(entry.getKey()); //$NON-NLS-1$
				writer.endObject();
				writer.beginArray();
				for (StyleEntry se : entry.getValue().getContent()){
					if (se.getStyle() == null) continue;
					String key = se.getID();
					String value = se.getMemento();
					writer.beginObject();
					writer.name(ID_KEY).value(key);
					writer.name(VALUE_KEY).value(value); 
					writer.endObject();
				}
				writer.endArray();
			}
			writer.endArray();
		}
		String value = sw.toString();
		return value;
		
	}
	
	/**
	 * Converts a json string encoded using the asString(Map) function back into
	 * the original map.
	 * 
	 * @param string
	 * @return
	 * @throws IOException
	 * @throws WorkbenchException
	 */
	public Map<String,StyleBlackboard> fromStringMap(String string) throws IOException{
		
		HashMap<String, StyleBlackboard> maps = new HashMap<String, StyleBlackboard>();
		if (string == null){ 
			return maps;
		}
		try(JsonReader reader = new JsonReader(new StringReader(string))){
			reader.beginArray();
			while(reader.hasNext()){
				StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
				reader.beginObject();
				reader.hasNext();
				reader.nextName(); //will always be resourceId
				String resourceId = reader.nextString();
				reader.endObject();
			
				reader.beginArray();
				while(reader.hasNext()){
			
					String styleId = null;
					String value = null;
			
					reader.beginObject();
					while(reader.hasNext()){
						String name = reader.nextName();
						if (name.equals(ID_KEY)){ 
							styleId = reader.nextString();
						}else if (name.equals(VALUE_KEY)){ 
							if (reader.peek() == JsonToken.NULL) {
								reader.nextNull();	
							}else {
								value = reader.nextString();
							}
						}else{
							reader.skipValue();
						}
					}
					reader.endObject();
					if (styleId != null && value != null){
						StyleContent sc = loadStyleContent(styleId);
						XMLMemento memento = null;
						try{
							memento = XMLMemento.createReadRoot(new StringReader(value));
						}catch (WorkbenchException ex) {
							throw new IOException(ex.getMessage(), ex);
						}
						if (sc != null){
							Object style = sc.load(memento);
							if (style != null){
								sb.put(styleId, style);
							}
						}
					}
				}
				maps.put(resourceId, sb);
				reader.endArray();
			}
			reader.endArray();
			return maps;
		}
	}
	
	/**
	 * Converts a single style blackboard to a json string
	 * 
	 * @param style
	 * @return
	 * @throws IOException
	 */
	public String asString(StyleBlackboard style) throws IOException{
		StringWriter sw = new StringWriter();
		try(JsonWriter writer = new JsonWriter(sw)){
			writer.beginArray();
			for (StyleEntry se : style.getContent()){
				if (se.getStyle() == null) continue;
				String key = se.getID();
				String value = se.getMemento();
				if (value != null){
					writer.beginObject();
					writer.name(ID_KEY).value(key); 
					writer.name(VALUE_KEY).value(value); 
					writer.endObject();
				}
			}
			writer.endArray();
			String value = sw.toString();
	
			return value;
		}
	}
	
	/**
	 * Parses a json string encoded using the asString(styleBlackboard) back into
	 * the original style blackboard object.
	 * 
	 * @param string
	 * @return
	 * @throws IOException
	 * @throws WorkbenchException
	 */
	public StyleBlackboard fromString(String string) throws IOException, WorkbenchException{
		StyleBlackboard sb = ProjectFactory.eINSTANCE.createStyleBlackboard();
		try(JsonReader reader = new JsonReader(new StringReader(string))){
		
			reader.beginArray();
			while(reader.hasNext()){
				
				String styleId = null;
				String value = null;
				
				reader.beginObject();
				while(reader.hasNext()){
					String name = reader.nextName();
					if (name.equals(ID_KEY)){ 
						styleId = reader.nextString();
					}else if (name.equals(VALUE_KEY)){
						if (reader.peek() != JsonToken.NULL){
							value = reader.nextString();
						}else{
							reader.nextNull();
						}
					}else{
						reader.skipValue();
					}
				}
				reader.endObject();
				if (styleId != null && value != null){
					StyleContent sc = loadStyleContent(styleId);
					 XMLMemento memento = XMLMemento.createReadRoot(new StringReader(value));
					 if (sc != null){
						 Object style = sc.load(memento);
						 sb.put(styleId, style);
					 }
					
					 
				}
			}
			reader.endArray();
			return sb;
		}
	}
	
	/**
     * Creates the  {@link SyleContent} for the required style 
     * @param styleId
     */
    public StyleContent loadStyleContent( final String styleId ) {
    	final StyleContent[] st = new StyleContent[1];
        ExtensionPointProcessor p = new ExtensionPointProcessor(){
            boolean found = false;
            public void process( IExtension extension, IConfigurationElement element )
                    throws Exception {
                if (!found && element.getAttribute("id").equals(styleId)) { //$NON-NLS-1$
                    found = true;
                    StyleContent styleContent = (StyleContent) element.createExecutableExtension("class"); //$NON-NLS-1$
                    st[0] = styleContent;
                }
            }
        };
        ExtensionPointUtil.process(ProjectPlugin.getPlugin(), StyleContent.XPID, p);
        if (st[0] == null){
        	return StyleContent.DEFAULT;
        }
        return st[0];
    }
 
    
	
	
	/**
	 * Find the initial style page editor for a given style.
	 * 
	 * @param selectedLayer
	 * @return
	 */
	public String findInitialStylePageId(ILayer selectedLayer) {
		String pageId = "simple"; //$NON-NLS-1$
		try {
			if (SLD.POINT.supports(selectedLayer)) {
				pageId = "org.locationtech.udig.style.advanced.editorpages.SimplePointEditorPage"; //SimplePointEditorPage.ID; //$NON-NLS-1$
			} else if (SLD.LINE.supports(selectedLayer)) {
				pageId = "org.locationtech.udig.style.advanced.editorpages.SimpleLineEditorPage"; //SimpleLineEditorPage.ID; //$NON-NLS-1$
			} else if (SLD.POLYGON.supports(selectedLayer)) {
				pageId = "org.locationtech.udig.style.advanced.editorpages.SimplePolygonEditorPage"; //SimplePolygonEditorPage.ID; //$NON-NLS-1$
			} else if (selectedLayer.getGeoResource().canResolve(
					GridCoverage.class)) {
				pageId = "org.locationtech.udig.style.raster.SingleBandRasterPage"; //$NON-NLS-1$
			}
		} catch (Exception e) {
		}
		return pageId;
	}
	
	/**
	 * To the layer find the appropriate style and apply it. The geoIdToMapStyle links
	 * the georesource ID to the configured default map style key. The
	 * defaultStyles map is optional and is used if the configured default map style is not set or not found. It
	 * links the default map style key to the supplier which creates a style blackboard.
	 * If no style is found by either method, a style is resolved from the georesource.
	 * 
	 * @param l
	 * @param geoIdToMapStyle  (TRACK->org.wcs.smart.patrol.map.track)
	 * @param defaultStyles optional (can be null)  (org.wcs.smart.patrol.map.track->StyleBlackboardProducer)
	 * @param monitor
	 * @throws WorkbenchException
	 * @throws IOException
	 */
	public void applyDefaultStyleToMapLayer(ConservationArea ca, Layer l, 
			Map<String,String> geoIdToMapStyle, 
			Map<String,Consumer<Layer>> defaultStyles, 
			Session session, IProgressMonitor monitor) throws WorkbenchException, IOException {
		String styleKey = null;
		for (Entry<String,String> item: geoIdToMapStyle.entrySet()) {
			if (l.getGeoResource().getDisplayID().endsWith("#" + item.getKey())) { //$NON-NLS-1$
				styleKey =  item.getValue();
				break;
			}
		}
		if (styleKey != null) {
			SmartStyle style = StyleManager.INSTANCE.getMapLayerDefaultStyle(ca, styleKey, session);
			if (style != null) {
				StyleBlackboard sb = StyleManager.INSTANCE.fromString(style.getStyleString());
				l.setStyleBlackboard(sb);
				return;
			}
		}
		if (defaultStyles != null && defaultStyles.containsKey(styleKey)) {
			defaultStyles.get(styleKey).accept(l);
		}
		Style s = l.getGeoResource().resolve(Style.class, monitor);
		if (s != null) l.getStyleBlackboard().put(SLDContent.ID, s);			
	}
	
	public void applyDefaultStyleToMapLayer(ConservationArea ca, Layer l, Map<String,String> geoIdToMapStyle, Session session,
			IProgressMonitor monitor) throws WorkbenchException, IOException {
		this.applyDefaultStyleToMapLayer(ca, l, geoIdToMapStyle, Collections.emptyMap(), session, monitor);

		
	}
}

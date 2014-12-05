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
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.refractions.udig.core.internal.ExtensionPointProcessor;
import net.refractions.udig.core.internal.ExtensionPointUtil;
import net.refractions.udig.project.StyleContent;
import net.refractions.udig.project.internal.ProjectFactory;
import net.refractions.udig.project.internal.ProjectPlugin;
import net.refractions.udig.project.internal.StyleBlackboard;
import net.refractions.udig.project.internal.StyleEntry;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;

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
	
	private static final String ID_KEY = "id"; //$NON-NLS-1$
	private static final String VALUE_KEY = "value"; //$NON-NLS-1$
	
	private StyleManager(){
		
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
		JsonWriter writer = new JsonWriter(sw);
	
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
		
		String value = sw.toString();
		writer.close();
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
	public Map<String,StyleBlackboard> fromStringMap(String string) throws IOException, WorkbenchException{
		
		HashMap<String, StyleBlackboard> maps = new HashMap<String, StyleBlackboard>();
		
		JsonReader reader = new JsonReader(new StringReader(string));
		
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
						value = reader.nextString();
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
						if (style != null){
							sb.put(sc.getId(), style);
						}
					}
				}
			}
			maps.put(resourceId, sb);
			reader.endArray();
		}
		reader.endArray();
		reader.close();
		return maps;
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
		JsonWriter writer = new JsonWriter(sw);
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
		writer.close();
		return value;
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
		JsonReader reader = new JsonReader(new StringReader(string));
		
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
					 sb.put(sc.getId(), style);
				 }
				
				 
			}
		}
		reader.endArray();
		reader.close();
		return sb;
	}
	
	/**
     * Creates the  {@link SyleContent} for the required style 
     * @param styleId
     */
    private StyleContent loadStyleContent( final String styleId ) {
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
        return st[0];
    }
 
}

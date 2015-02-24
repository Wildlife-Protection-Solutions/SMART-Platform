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
package org.wcs.smart.query.model;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.Transient;

import org.eclipse.ui.WorkbenchException;
import org.wcs.smart.query.QueryPlugIn;
import org.wcs.smart.udig.style.StyleManager;

import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;

/**
 * Class for queries which can be styled.
 * 
 * @author Emily
 *
 */
@Entity
public abstract class StyledQuery extends Query {

	public static final String QUERY_STYLE_KEY = "queryStyle"; //$NON-NLS-1$
	
	private String styleMemento;
	
	// a single query can create multiple layers 
	// ex: survey observation queries (survey tracks and waypoints)
	// are returned as two separate layers
	//so we need to be able to map layer to a style
	//we use the ref section of the layer id as the query
	//uuid may not be set when this is set which will cause
	//problems if we use the entire layer id
	@Transient
	private Map<String, StyleBlackboard> queryStyles;
	

	/**
	 * The string representation of the layer style
	 * @return
	 */
	public String getStyle(){
		return this.styleMemento;
	}
	
	/**
	 * Sets the string representation of the layer style
	 * @param style
	 */
	public void setStyle(String style){
		this.styleMemento = style;
		queryStyles = null;
	}
	
	private void loadQueryStyleMap(){
		if (getStyle() == null){
			queryStyles = new HashMap<String, StyleBlackboard>();
			return;
		}
		try{
			queryStyles = StyleManager.INSTANCE.fromStringMap(getStyle());
		}catch (Exception ex){
			queryStyles = new HashMap<String, StyleBlackboard>();
			QueryPlugIn.log("Style parsing error", ex); //$NON-NLS-1$
		}
	}
	/**
	 * Updates the current query style from the contents of the blackboard
	 * 
	 * @param geoResourceID
	 * @param blackboard setting to null will remove style for the given layer
	 * @throws IOException
	 */
	@Transient
	public void updateStyle(String geoResourceKey, StyleBlackboard blackboard) throws IOException{
		if (queryStyles == null){
			loadQueryStyleMap();
		}
		if (blackboard == null){
			queryStyles.remove(geoResourceKey);
		}else{
			queryStyles.put(geoResourceKey, blackboard);
		}
		
		setStyle(StyleManager.INSTANCE.asString(queryStyles));
	}
	
	
	/**
	 * Applies the current query style to the given style blackboard
	 * @param toUpdate
	 */
	@Transient
	public void applyStyle(String geoResourceKey, StyleBlackboard toUpdate) throws IOException, WorkbenchException{
		if (queryStyles == null){
			loadQueryStyleMap();
		}
		StyleBlackboard local = queryStyles.get(geoResourceKey);
		if (local != null){
			toUpdate.clear();
			for (StyleEntry se : local.getContent()){
				toUpdate.put(se.getID(), se.getStyle());
			}
		}
	}
	
}

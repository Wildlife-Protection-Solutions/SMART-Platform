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

import org.eclipse.ui.WorkbenchException;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.wcs.smart.udig.style.StyleManager;

/**
 * Managing query layer styles
 * 
 * @author Emily
 *
 */
public enum QueryStyleParser {
	INSTANCE;
	
	/* a single query can create multiple layers 
	* ex: survey observation queries (survey tracks and waypoints)
	* are returned as two separate layers
	* so we need to be able to map layer to a style
	* we use the ref section of the layer id as the query
	* uuid may not be set when this is set which will cause
	* problems if we use the entire layer id
	**/
	/**
	 * Updates the current query style from the contents of the blackboard
	 * 
	 * @param geoResourceID
	 * @param blackboard setting to null will remove style for the given layer
	 * @throws IOException
	 */
	public void updateStyle(StyledQuery query, 
			String geoResourceKey, StyleBlackboard blackboard) throws IOException, WorkbenchException{
		
		Map<String, StyleBlackboard> queryStyles = StyleManager.INSTANCE.fromStringMap(query.getStyle());
		
		if (blackboard == null){
			queryStyles.remove(geoResourceKey);
		}else{
			queryStyles.put(geoResourceKey, blackboard);
		}
		query.setStyle(StyleManager.INSTANCE.asString(queryStyles));
	}
	
	/**
	 * Converts the blackboard to a style string that can be saved
	 * to the database and parsed using the applyStyle functions
	 * 
	 * @param geoResourceKey
	 * @param blackboard
	 * @throws IOException
	 * @throws WorkbenchException
	 */
	public String getStyle(String geoResourceKey, StyleBlackboard blackboard) throws IOException, WorkbenchException{
		Map<String, StyleBlackboard> queryStyles = new HashMap<String, StyleBlackboard>();
		if (blackboard == null){
			queryStyles.remove(geoResourceKey);
		}else{
			queryStyles.put(geoResourceKey, blackboard);
		}
		return StyleManager.INSTANCE.asString(queryStyles);
	}
	
	/**
	 * Applies the current query style to the given style blackboard
	 * @param toUpdate
	 */
	public void applyStyle(StyledQuery query, String geoResourceKey, StyleBlackboard toUpdate) throws IOException, WorkbenchException{
		applyStyle(query.getStyle(), geoResourceKey, toUpdate);
	}
	/**
	 * Applies the style represented by the style string to the given style blackboard
	 * @param toUpdate
	 */
	public void applyStyle(String styleString, String geoResourceKey, StyleBlackboard toUpdate) throws IOException, WorkbenchException{
		Map<String, StyleBlackboard> queryStyles = StyleManager.INSTANCE.fromStringMap(styleString);
		StyleBlackboard local = queryStyles.get(geoResourceKey);
		if (local != null){
			toUpdate.clear();
			for (StyleEntry se : local.getContent()){
				toUpdate.put(se.getID(), se.getStyle());
			}
		}
	}
}

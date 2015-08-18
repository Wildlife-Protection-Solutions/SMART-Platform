package org.wcs.smart.query.model;

import java.io.IOException;
import java.util.Map;

import org.eclipse.ui.WorkbenchException;
import org.locationtech.udig.project.internal.StyleBlackboard;
import org.locationtech.udig.project.internal.StyleEntry;
import org.wcs.smart.udig.style.StyleManager;

public enum QueryStyleParser {
	INSTANCE;
	
//	private void loadQueryStyleMap(){
//		if (getStyle() == null){
//			queryStyles = new HashMap<String, StyleBlackboard>();
//			return;
//		}
//		try{
//			queryStyles = StyleManager.INSTANCE.fromStringMap(getStyle());
//		}catch (Exception ex){
//			queryStyles = new HashMap<String, StyleBlackboard>();
//			QueryPlugIn.log("Style parsing error", ex); //$NON-NLS-1$
//		}
//	}
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
	 * Applies the current query style to the given style blackboard
	 * @param toUpdate
	 */
	public void applyStyle(StyledQuery query, String geoResourceKey, StyleBlackboard toUpdate) throws IOException, WorkbenchException{
		Map<String, StyleBlackboard> queryStyles = StyleManager.INSTANCE.fromStringMap(query.getStyle());
		StyleBlackboard local = queryStyles.get(geoResourceKey);
		if (local != null){
			toUpdate.clear();
			for (StyleEntry se : local.getContent()){
				toUpdate.put(se.getID(), se.getStyle());
			}
		}
	}
}

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

import net.refractions.udig.internal.ui.UiPlugin;
import net.refractions.udig.libs.internal.Activator;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.Bundle;

/**
 * This class is responsible of maintaining the map setting in the store (smart database)
 * 
 * TODO mock implementation 
 * 
 * @author Mauricio Pazos
 *
 */
final class MapSettingsStore {

	
	private static String CURRENT_USER_MAP = "";
	
	private static String SHARED_LAYERS = null; // required structure user=ALL map=json
	
	
	/**
	 * Save shared layers. 
	 * <pre>
	 * create a register with the following information:
	 * 
	 * user:ALL
	 * map:jsonMap
	 * </pre> 
	 * 
	 * @param jsonMap
	 */
	public static void saveShared(String jsonMap ){
		
		SHARED_LAYERS = jsonMap;
	}
	
	/**
	 * Retrieves the layers shared for all users
	 */
	public static String findAllSharedLayers(){
		
		return SHARED_LAYERS;
	}
	
	/**
	 * Saves the layers customized by the user
	 * @param userId
	 * @param jsonMap
	 */
	public static void save(String userId, String jsonMap) {

		//TODO save in the  database. The relation could be  CREATE TABLE USER_MAP_SETTINGS ( String USER_ID,  String MAP_SETTINGS)
		
		logInfo("SAVING Map: "+ jsonMap) ;
		
		CURRENT_USER_MAP = jsonMap;
		
	}
	
	/**
	 * Retrieves the layers customized by the user
	 * @param userId
	 * @return
	 */
	public static String findById(String userId){
		
		return CURRENT_USER_MAP;
	}
			
	private static void logInfo(String message) {
		
		final Bundle bundle = Platform.getBundle(Activator.ID);
        Platform.getLog(bundle).log( new Status(IStatus.INFO, UiPlugin.ID, message));
	}

}

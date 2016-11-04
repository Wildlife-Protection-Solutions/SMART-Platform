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
package org.wcs.smart.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.ca.SmartUserLevel;

/**
 * Tools for managing SMART User Levels
 * 
 * @author Emily
 *
 */
public enum UserLevelManager {

	INSTANCE;
	
	public final static String EXTENSION_ID = "org.wcs.smart.userlevel"; //$NON-NLS-1$
	
	
	public final static SmartUserLevel ADMIN = new SmartUserLevel("ADMIN"); //$NON-NLS-1$
	public final static SmartUserLevel DATA_ENTRY = new SmartUserLevel("DATAENTRY"); //$NON-NLS-1$
	public final static SmartUserLevel ANALYST = new SmartUserLevel("ANALYST"); //$NON-NLS-1$
	public final static SmartUserLevel MANAGER = new SmartUserLevel("MANAGER"); //$NON-NLS-1$
	
	private volatile Set<SmartUserLevel> levels = null;
	
	public SmartUserLevel getUserLevel(String key){
		for (SmartUserLevel l : getUserLevels()){
			if (l.getKey().equals(key)) return l;
		}
		return null;
	}
	
	public Collection<SmartUserLevel> getUserLevels(){
		if (levels == null){
			synchronized (UserLevelManager.class) {
				if (levels == null){
					levels = new HashSet<SmartUserLevel>();
					levels.add(ADMIN);
					levels.add(DATA_ENTRY);
					levels.add(ANALYST);
					levels.add(MANAGER);		
					levels.addAll(readExtensionUserLevels());		
				}
			}
		}
		return levels;
	}
	
	private List<SmartUserLevel> readExtensionUserLevels(){
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_ID);
		List<SmartUserLevel> levels = new ArrayList<>();
		for (IConfigurationElement e : config) {
			 if (e.getName().equals("SMARTUserLevel")){ //$NON-NLS-1$
				 try{
					 SmartUserLevel level = (SmartUserLevel) e.createExecutableExtension("class");
					 levels.add(level);
				 }catch (Exception ex){
					 SmartPlugIn.log(ex.getMessage(), ex);;
				 }
			 }
		}
		return levels;
	}
}

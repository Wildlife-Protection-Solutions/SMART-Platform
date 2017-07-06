/*
 * Copyright (C) 2017 Wildlife Conservation Society
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
package org.wcs.smart.qa;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.wcs.smart.qa.routine.IQaAction;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.routine.IgnoreAction;

/**
 * Extensions manager for loading QA Routine extensions.  This only loads details
 * of extensions for non desktop specific extensions.
 * @author Emily
 *
 */
public enum RoutineExtensionManager {
	
	INSTANCE;
	
	public static final String QA_ROUTINE_TYPE_EXTENSION_ID = "org.wcs.smart.qa"; //$NON-NLS-1$

	private List<IQaRoutineType> types = null;
	private List<IQaDataProvider> dataproviders = null;
	
	
	/**
	 * Find the qa routine type with the given id. 
	 * Will return null if none found
	 * 
	 * @param id
	 * @return
	 */
	public IQaRoutineType findRoutineType(String id){
		for(IQaRoutineType type : getDefinedRoutineTypes()){
			if (id.equals(type.getId())) return type;
		}
		return null;
	}
	
	/**
	 * Find the data provider routine type with the given id. 
	 * Will return null if none found
	 * 
	 * @param id
	 * @return
	 */
	public IQaDataProvider findDataProvider(String id){
		for(IQaDataProvider type : getDataProviders()){
			if (id.equals(type.getId())) return type;
		}
		return null;
	}
	
	/**
	 * Finds all defined QA Routine Types in the system
	 * @return
	 */
	public synchronized Collection<IQaRoutineType> getDefinedRoutineTypes(){
		if (types != null) return types;
		List<IQaRoutineType> temp = new ArrayList<>();
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint pnt = registry.getExtensionPoint(QA_ROUTINE_TYPE_EXTENSION_ID);
		IConfigurationElement[] config = pnt.getConfigurationElements();
		for (IConfigurationElement e : config) {
			if (e.getName().equals("qa_routine")){ //$NON-NLS-1$
				try{
					IQaRoutineType type = (IQaRoutineType)e.createExecutableExtension("class"); //$NON-NLS-1$
					temp.add(type);
					
				}catch (Exception ex){
					QaPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		this.types = temp;
		return types;
	}
	
	/**
	 * Finds all defined QA Routine Types in the system
	 * @return
	 */
	public synchronized Collection<IQaDataProvider> getDataProviders(){
		if (dataproviders != null) return dataproviders;
		List<IQaDataProvider> temp = new ArrayList<>();
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint pnt = registry.getExtensionPoint(QA_ROUTINE_TYPE_EXTENSION_ID);
		IConfigurationElement[] config = pnt.getConfigurationElements();
		for (IConfigurationElement e : config) {
			if (e.getName().equals("data_provider")){ //$NON-NLS-1$
				try{
					IQaDataProvider type = (IQaDataProvider)e.createExecutableExtension("class"); //$NON-NLS-1$
					temp.add(type);
					
				}catch (Exception ex){
					QaPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		this.dataproviders = temp;
		return dataproviders;
	}
	
	/**
	 * 
	 * @return list of actions applicable to all 
	 * data providers
	 */
	public List<IQaAction> getUniversalActions(){
		return Collections.singletonList(IgnoreAction.INSTANCE);
	}
}

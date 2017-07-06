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
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.hibernate.SmartDB;
import org.wcs.smart.qa.model.QaRoutine;
import org.wcs.smart.qa.routine.IQaAction;
import org.wcs.smart.qa.routine.IQaDataProvider;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.ui.configure.IParameterCollector;

/**
 * Utility functions for managing QA Routines, actions, and parameters.  
 * 
 * @author Emily
 *
 */
public enum InternalExtensionManager {
	INSTANCE;
	
	private HashMap<String, List<IQaAction>> providerActions = null;

	private List<QaRoutine> autoRoutines = null;
	
	private Boolean isAutoCleaned = Boolean.FALSE;
	
	/**
	 * 
	 * @return list of routines defined for automatic configuration
	 */
	@SuppressWarnings("unchecked")
	public synchronized List<QaRoutine> getAutoRoutines(){
		if (autoRoutines != null) return autoRoutines;
		List<QaRoutine> routines = new ArrayList<>();
		Session session = HibernateManager.openSession();
		try{
			routines.addAll(session.createCriteria(QaRoutine.class)
				.add(Restrictions.eq("conservationArea", SmartDB.getCurrentConservationArea())) //$NON-NLS-1$
				.add(Restrictions.eq("autoCheck", true)) //$NON-NLS-1$
				.list());
			
		}catch (Exception ex){
			QaPlugIn.log(ex.getMessage(), ex);
		}finally{
			session.close();
		}
		this.autoRoutines = routines;
		return this.autoRoutines;
	}
	
	public void clearAutoRoutines(){
		this.autoRoutines = null;
	}
	
	/**
	 * Finds the parameter collector for the qa routine type provided
	 * by typeId
	 * @param typeId the QA Routine type
	 * @return
	 */
	public IParameterCollector newParameterCollector(String typeId){
		IExtensionRegistry registry = RegistryFactory.getRegistry();
		IExtensionPoint pnt = registry.getExtensionPoint(RoutineExtensionManager.QA_ROUTINE_TYPE_EXTENSION_ID);
		IConfigurationElement[] config = pnt.getConfigurationElements();
		for (IConfigurationElement e : config) {
			if (e.getName().equals("qa_routine")){ //$NON-NLS-1$
				try{
					if (((IQaRoutineType)e.createExecutableExtension("class")).getId().equals(typeId)){ //$NON-NLS-1$
						IConfigurationElement[] kids = e.getChildren("parameter_collector"); //$NON-NLS-1$
						if (kids.length == 0) return null;
						return (IParameterCollector)kids[0].createExecutableExtension("class"); //$NON-NLS-1$
					}
				}catch (Exception ex){
					QaPlugIn.log(ex.getMessage(), ex);
				}
			}
		}
		return null;
	}

	
	public List<IQaAction> getQaActions(IQaDataProvider provider, IEclipseContext context){
		if (providerActions == null){
			synchronized (INSTANCE) {
				if (providerActions == null){
			
					providerActions = new HashMap<>();
					
					IExtensionRegistry registry = RegistryFactory.getRegistry();
					IExtensionPoint pnt = registry.getExtensionPoint(RoutineExtensionManager.QA_ROUTINE_TYPE_EXTENSION_ID);
					IConfigurationElement[] config = pnt.getConfigurationElements();
					for (IConfigurationElement e : config) {
						if (e.getName().equals("data_provider")){ //$NON-NLS-1$
							try{
								String id = ((IQaDataProvider)e.createExecutableExtension("class")).getId(); //$NON-NLS-1$
								IConfigurationElement[] kids = e.getChildren("qa_action"); //$NON-NLS-1$
								List<IQaAction> actions = new ArrayList<>();
								for (IConfigurationElement kid : kids){
									IQaAction action = (IQaAction)kid.createExecutableExtension("class"); //$NON-NLS-1$
									actions.add(action);
									ContextInjectionFactory.inject(action, context);
								}
								providerActions.put(id, actions);
							}catch (Exception ex){
								QaPlugIn.log(ex.getMessage(), ex);
							}
						}
					}
				}
			}
		}
		return providerActions.get(provider.getId());
	}
	
	/**
	 * Deletes all resolved items from the qa error tables. This is designed to be run
	 * only once per application run.
	 * 
	 */
	public void cleanAutoResults(){
		synchronized (isAutoCleaned) {
			if (isAutoCleaned) return;
			isAutoCleaned = true;
		}
		try {
			QaErrorCleaner.INSTANCE.cleanItems(SmartDB.getCurrentConservationArea());
		} catch (Exception e) {
			QaPlugIn.log(e.getMessage(), e);
		}
	}
}

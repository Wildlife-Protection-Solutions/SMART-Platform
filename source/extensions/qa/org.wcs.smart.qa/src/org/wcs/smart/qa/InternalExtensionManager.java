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

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.RegistryFactory;
import org.wcs.smart.qa.routine.IQaRoutineType;
import org.wcs.smart.qa.ui.configure.IParameterCollector;

public enum InternalExtensionManager {
	INSTANCE;
	

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

}

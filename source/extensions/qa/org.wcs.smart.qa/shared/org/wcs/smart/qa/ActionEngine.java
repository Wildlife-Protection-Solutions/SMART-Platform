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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.wcs.smart.qa.model.QaError;
import org.wcs.smart.qa.routine.IQaAction;
import org.wcs.smart.qa.routine.IQaDataProvider;

/**
 * Engine for performing qa actions
 */
public enum ActionEngine {

	INSTANCE;
	
	/*
	 * Find the associated action for each qa error item and perform 
	 * it.
	 * 
	 * return true if changes have been made to at least one item, false otherwise
	 */
	public boolean performActions(List<QaError> actionItems, String actionId, IEclipseContext context){
		
		Set<IQaDataProvider> providers = new HashSet<IQaDataProvider>();
		actionItems.forEach(i -> providers.add(i.getDataProvider()));
		boolean changes = false;
		boolean found = false;
		
		//universal actions
		for (IQaAction action : RoutineExtensionManager.INSTANCE.getUniversalActions()){
			if (action.getId().equals(actionId)){
				changes = changes || action.doAction(actionItems);
				found = true;
				break;
			}
		}
		
		//extension point actions
		if (!found){
			for (IQaDataProvider p : providers){
				for (IQaAction n : InternalExtensionManager.INSTANCE.getQaActions(p, context)){
					if (n.getId().equals(actionId)){
						changes = changes || n.doAction(actionItems);
						break;
					}
				}
			}
		}
		//update status and fix message of all linked items as they are all the same
		for (QaError item : actionItems){
			for (QaError link : item.getLinks()){
				link.setStatus(item.getStatus());
				link.setFixMessage(item.getFixMessage());
				link.setGeometryObject(item.getGeometryObject());
			}
		}
		return changes;
	}
}

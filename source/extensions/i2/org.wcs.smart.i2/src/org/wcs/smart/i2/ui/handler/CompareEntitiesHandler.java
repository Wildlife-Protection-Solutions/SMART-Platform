/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.handler;

import java.util.List;

import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.model.IntelEntityType;
import org.wcs.smart.i2.ui.editors.EntityComparisonEditor;
import org.wcs.smart.i2.ui.editors.EntityComparisonInput;
import org.wcs.smart.util.E3Utils;

/**
 * Handler for comparing entity types
 * 
 * @author Emily
 *
 */
public class CompareEntitiesHandler {

	/**
	 * All entities must be of the same type. Entities of different types
	 * cannot be compared.
	 * 
	 * @param entities
	 * @throws Exception
	 */
	public void compare(List<IntelEntity> entities, EPartService pService) throws Exception{
		IntelEntityType type = null;
		for (IntelEntity e : entities){
			if (type == null){
				type = e.getEntityType();
			}else{
				if (!e.getEntityType().equals(type)){
					throw new Exception(Messages.CompareEntitiesHandler_DifferentTypeError);
				}
			}
		}
		if (entities.isEmpty()) throw new Exception(Messages.CompareEntitiesHandler_EntityRequired);
		
		//add to existing comparison editor if of same type
		for (MPart p : pService.getParts()){
			Object source = E3Utils.getSourceObject(p);
			if (source instanceof EntityComparisonEditor){
				if (((EntityComparisonInput)(((EntityComparisonEditor)source).getEditorInput())).getType().equals(type)){
					((EntityComparisonEditor)source).addEntities(entities);
					pService.activate(p);
					return;
				}
			}
		}
		
		//create a new editor for the type
		EntityComparisonInput input = new EntityComparisonInput(type, entities);
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, EntityComparisonEditor.ID);
	}
}

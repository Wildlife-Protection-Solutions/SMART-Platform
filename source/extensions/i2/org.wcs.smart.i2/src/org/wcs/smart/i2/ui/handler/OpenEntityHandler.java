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

import java.text.MessageFormat;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.model.IntelEntity;
import org.wcs.smart.i2.ui.IntelDataAssessmentPerspective;
import org.wcs.smart.i2.ui.editors.EntityEditor;
import org.wcs.smart.i2.ui.editors.EntityEditorInput;

/**
 * Opens entity
 * 
 * @author Emily
 *
 */
public class OpenEntityHandler {

	public void openEntity(IntelEntity entity, IEclipseContext context){
		//open entity
		EntityEditorInput input = new EntityEditorInput(entity.getIdAttributeAsText(), entity.getUuid(), entity.getEntityType());
		try {
			String pId = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getPerspective().getId();
			IEditorPart editor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(input, EntityEditor.ID);
			if (editor instanceof EntityEditor){
				if (pId.equals(IntelDataAssessmentPerspective.ID)){
					((EntityEditor)editor).setEditMode(true);
				}
			}
		} catch (PartInitException e) {
			Intelligence2PlugIn.displayLog(MessageFormat.format(Messages.OpenEntityHandler_OpenError, e.getMessage()), e);
		}
		
	}
}

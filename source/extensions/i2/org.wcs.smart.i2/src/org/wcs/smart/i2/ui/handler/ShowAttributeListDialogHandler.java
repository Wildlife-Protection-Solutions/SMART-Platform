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
import java.util.ArrayList;
import java.util.List;

import javax.inject.Named;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.part.EditorPart;
import org.wcs.smart.i2.ui.dialogs.AttributeListDialog;
import org.wcs.smart.i2.ui.editors.EntityEditor;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.util.E3Utils;

/**
 * Open dialog handler
 * 
 * @author Emily
 *
 */
public class ShowAttributeListDialogHandler extends ShowDialogHandler {

	public ShowAttributeListDialogHandler(){
		super(AttributeListDialog.class);
	}
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell, IEclipseContext context) {
		EPartService partService = context.get(EPartService.class);
		//ensure no dirty entity editors
		List<EditorPart> dirtyeditors = new ArrayList<>();
		StringBuilder sb = new StringBuilder();
		for (MPart part : partService.getParts()){
			if (part.isDirty()){
				Object x = E3Utils.getSourceObject(part);
				if (x instanceof EntityEditor || x instanceof RecordEditor){
					dirtyeditors.add((EditorPart)x);
					sb.append(((EditorPart)x).getEditorInput().getName());
					sb.append(", ");
				}
			}
		}
		
		if (!dirtyeditors.isEmpty()){
			if (!MessageDialog.openQuestion(activeShell, "Close Editors", 
					MessageFormat.format("The following entities & records must be saved before you can modify attributes.  Do you want to save these? \n{0}",sb.substring(0, sb.length() - 2)))){
				return;
			}
			for (EditorPart e : dirtyeditors){
				e.getSite().getPage().saveEditor(e, false);
			}
		}
		
		super.execute(activeShell, context);
		
		//refresh all record editors
		//TODO: consider events instead;
		for (MPart part : partService.getParts()){
			Object x = E3Utils.getSourceObject(part);
			if (x instanceof RecordEditor){
				((RecordEditor) x).refresh();
			}
		}
	}
	
	// E3
	public static class ShowAttributeListDialogHandlerWrapper extends DIHandler<ShowAttributeListDialogHandler> {
		public ShowAttributeListDialogHandlerWrapper() {
			super(ShowAttributeListDialogHandler.class);
		}
	}
	
}

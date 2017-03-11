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

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.i2.internal.Messages;
import org.wcs.smart.i2.ui.dialogs.RecordSourceAttributeDialog;
import org.wcs.smart.i2.ui.editors.record.RecordEditor;
import org.wcs.smart.util.E3Utils;

/**
 * Opens dialog for configuring record attributes
 * @author Emily
 *
 */
public class ConfigureRecordAttributesHandler {

	@Execute
	public void execute(IEclipseContext context){
		
		//save or close all dirty RecordEditors
		List<MPart> parts = new ArrayList<>();
		StringBuilder names = new StringBuilder();
		for (MPart p : context.get(EPartService.class).getParts()){
			if (p.isDirty()){
				Object x = E3Utils.getSourceObject(p);
				if (x instanceof RecordEditor){
					//save or discard changes
					parts.add(p);
					names.append(((RecordEditor) x).getRecord().getTitle() + "\n"); //$NON-NLS-1$
				}
			}
		}
		if (!parts.isEmpty()){
			if(!MessageDialog.openQuestion(context.get(Shell.class),Messages.ConfigureRecordAttributesHandler_Title, MessageFormat.format(Messages.ConfigureRecordAttributesHandler_SaveRequired,  parts.size(), names.toString()))){
				return;
			}
			for (MPart p : parts){
				//save parts; part service doesn't work as it still prompt user
				RecordEditor x = (RecordEditor)E3Utils.getSourceObject(p);
				if (!x.getSite().getPage().saveEditor(x,  false)){
					MessageDialog.openError(context.get(Shell.class), Messages.ConfigureRecordAttributesHandler_Title, MessageFormat.format(Messages.ConfigureRecordAttributesHandler_SaveError, x.getRecord().getTitle()));
					return;
				}
			}
		}
		
		RecordSourceAttributeDialog dialog = new RecordSourceAttributeDialog(context.get(Shell.class));
		ContextInjectionFactory.inject(dialog,context);
		dialog.open();
	}
	
	// E3
	public static class ConfigureRecordAttributesHandlerWrapper extends DIHandler<ConfigureRecordAttributesHandler> {
		public ConfigureRecordAttributesHandlerWrapper() {
			super(ConfigureRecordAttributesHandler.class);
		}
	}
}

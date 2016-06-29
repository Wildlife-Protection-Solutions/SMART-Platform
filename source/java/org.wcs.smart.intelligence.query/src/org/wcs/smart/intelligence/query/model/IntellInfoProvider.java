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
package org.wcs.smart.intelligence.query.model;

import java.text.MessageFormat;
import java.util.UUID;

import org.eclipse.e4.core.contexts.ContextInjectionFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.intelligence.ui.handlers.OpenIntelligenceHandler;
import org.wcs.smart.query.model.IQueryResultInfoProvider;

/**
 * Intel info provider than opens up the intelligence record associated
 * with the result item.
 * 
 * @author Emily
 *
 */
public class IntellInfoProvider implements IQueryResultInfoProvider {

	@Override
	public String getName() {
		return GOTO_SOURCE_STR;
	}

	@Override
	public void doWork(Object resultItem) {
		if (resultItem instanceof IntelligenceRecordResultItem){
			UUID intellUuid = ((IntelligenceRecordResultItem)resultItem).getUuid();
			IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
			ctx.set(OpenIntelligenceHandler.INTELLUUID_PARAM, intellUuid);
			ContextInjectionFactory.invoke(new OpenIntelligenceHandler(), Execute.class, ctx.getActiveLeaf());
			
		}else{
			MessageDialog.openError(Display.getDefault().getActiveShell(), ERROR_STR, MessageFormat.format(OP_NOT_SUPPORTED_STR, resultItem.getClass().getName()));
		}
		
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON);
	}

}

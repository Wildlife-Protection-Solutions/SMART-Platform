/*
 * Copyright (C) 2020 Wildlife Conservation Society
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
package org.wcs.smart.report.internal.ui;

import java.util.Iterator;

import javax.inject.Named;

import org.eclipse.birt.report.designer.ui.newelement.DesignElementFactory;
import org.eclipse.birt.report.designer.util.DEUtil;
import org.eclipse.birt.report.model.api.ModuleHandle;
import org.eclipse.birt.report.model.api.ScalarParameterHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.birt.report.model.api.elements.DesignChoiceConstants;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.birt.parameter.ISmartBirtParameter;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.ui.viewer.parameter.ParameterSelectionDialog;

/**
 * Handler to add new SMART Birt Parameter
 * 
 * @author Emily
 * @since 7.0.0
 *
 */
@SuppressWarnings("restriction")
public class NewSmartParameterHandler {

	@Execute
	public void execute(@Optional @Named(IServiceConstants.ACTIVE_SELECTION) Object thisSelection,
		EPartService partService, final Shell activeShell){
		
		if (thisSelection == null || 
			!(thisSelection instanceof IStructuredSelection) || 
			((IStructuredSelection)thisSelection).isEmpty() ){		
			return;
		}
		
		
		ModuleHandle report = null;
		SlotHandle selection = null;
		for (Iterator<?> iterator = ((IStructuredSelection)thisSelection).iterator(); iterator.hasNext();) {
			Object sel = iterator.next();
			if (sel instanceof SlotHandle){
				selection = (SlotHandle) sel;
				report = ((SlotHandle)sel).getModule().getRoot().getModuleHandle();
				break;
			}
		}
		
		if (report == null) return;
		
		
		ParameterSelectionDialog dialog = new ParameterSelectionDialog(activeShell);
		if (dialog.open() != Window.OK) return;
		
		ISmartBirtParameter pp = dialog.getSelection();
		
		ScalarParameterHandle newParam = (ScalarParameterHandle) DesignElementFactory.getInstance(report)
				.newElement("ScalarParameter", null); //$NON-NLS-1$
		try {
			newParam.setDataType(DesignChoiceConstants.PARAM_TYPE_STRING);
			newParam.setName(dialog.getName());
			newParam.setDataType(DesignChoiceConstants.PARAM_TYPE_STRING);
			newParam.setIsRequired(dialog.isRequired());
			newParam.setControlType(DesignChoiceConstants.PARAM_CONTROL_LIST_BOX);
			newParam.setCustomXml(ISmartBirtParameter.KEY + pp.getKey());

			( (SlotHandle) selection ).add( newParam );
			DEUtil.setDefaultTheme( newParam );
			
		}catch (Exception ex) {
			ReportPlugIn.displayLog(ex.getMessage(), ex);
		}

	}
	
	public static class NewSmartParameterHandlerWrapper extends DIHandler<NewSmartParameterHandler>{
		public NewSmartParameterHandlerWrapper(){
			super(NewSmartParameterHandler.class);
		}
	}

}

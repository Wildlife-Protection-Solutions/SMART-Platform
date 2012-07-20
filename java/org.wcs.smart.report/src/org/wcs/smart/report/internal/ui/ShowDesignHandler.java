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
package org.wcs.smart.report.internal.ui;

import java.io.File;
import java.util.List;

import org.eclipse.birt.report.designer.core.model.SessionHandleAdapter;
import org.eclipse.birt.report.designer.data.ui.dataset.DataSetUIUtil;
import org.eclipse.birt.report.designer.internal.ui.editors.ReportEditorInput;
import org.eclipse.birt.report.designer.ui.editors.IReportEditorContants;
import org.eclipse.birt.report.model.api.DataSetHandle;
import org.eclipse.birt.report.model.api.ReportDesignHandle;
import org.eclipse.birt.report.model.api.SessionHandle;
import org.eclipse.birt.report.model.api.SlotHandle;
import org.eclipse.birt.report.model.core.DesignElement;
import org.eclipse.birt.report.model.elements.OdaDataSet;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;
import org.wcs.smart.report.ReportPlugIn;
import org.wcs.smart.report.internal.ui.designer.SmartReportPerspective;
import org.wcs.smart.report.library.SmartBirtLibrary;

//TODO: delete this file
public class ShowDesignHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		
		try {
			HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench().showPerspective(
					SmartReportPerspective.ID, 
					HandlerUtil.getActiveWorkbenchWindow(event));
			
			File smartLibrary = SmartBirtLibrary.getInstance().getLibraryFile();			
			if (!smartLibrary.exists()){
				//TODO: I don't think we need to throw ane xception here - perhaps just
				//an error dialog instead.
				throw new IllegalStateException("SMART Library does not exist.");
			}
			
			File reportFile = new File(ReportPlugIn.getReportDirectory(), "new_report.rpt");

			if (!reportFile.exists()){
				//creates new
				SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();
				ReportDesignHandle rdh = session.createDesign(reportFile.getAbsolutePath());
			
				rdh.includeLibrary(smartLibrary.getAbsolutePath(), "SMART Library");
				rdh.save();
				rdh.close();
			}else{
				
				SessionHandle session = SessionHandleAdapter.getInstance().getSessionHandle();
				
				ReportDesignHandle rdh = session.openDesign(reportFile.getAbsolutePath());
				
				SlotHandle datasets = rdh.getDataSets();
				List<DesignElement> elements = datasets.getSlot().getContents();
				for (DesignElement ds : elements){
					OdaDataSet dataset = (OdaDataSet)ds;
					//dataset.
					//TODO: figure out how to automatically update dataset
					
					DataSetHandle handle = (DataSetHandle)dataset.getHandle(rdh.getModule());
					DataSetUIUtil.updateColumnCache(handle);	
				}
				
				rdh.save();
				rdh.close();
				
			}
		
			
			
			ReportEditorInput ri = new ReportEditorInput(reportFile);
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(ri, IReportEditorContants.DESIGN_EDITOR_ID);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		
		
		
		return null;
	}

}

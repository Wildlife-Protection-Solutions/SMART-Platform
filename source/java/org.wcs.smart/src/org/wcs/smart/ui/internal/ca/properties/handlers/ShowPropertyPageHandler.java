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
package org.wcs.smart.ui.internal.ca.properties.handlers;

import javax.inject.Named;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.ui.internal.ca.properties.AgencyRankPropertyPage;
import org.wcs.smart.ui.internal.ca.properties.AreaPropertyPage;
import org.wcs.smart.ui.internal.ca.properties.BasemapPropertyPage;
import org.wcs.smart.ui.internal.ca.properties.CaPropertyPage;
import org.wcs.smart.ui.internal.ca.properties.EmployeePropertyPage;
import org.wcs.smart.ui.internal.ca.properties.ProjectionPropertyDialog;
import org.wcs.smart.ui.internal.ca.properties.StationListPropertyPage;

/**
 * Handler for displaying property page.
 * 
 * @since 1.0.0
 */
public class ShowPropertyPageHandler {

	private Class<? extends Dialog> page = null;
	
	public ShowPropertyPageHandler(Class<? extends Dialog> page){
		this.page = page;
	}
	
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell) throws ExecutionException {
		Dialog dialog = null;
		if (page.equals(CaPropertyPage.class)){
			dialog = new CaPropertyPage(activeShell);
		}else if (page.equals(StationListPropertyPage.class)){
			dialog = new StationListPropertyPage(activeShell);
		}else if (page.equals(AgencyRankPropertyPage.class)){
			dialog = new AgencyRankPropertyPage(activeShell);
		}else if (page.equals(EmployeePropertyPage.class)){
			dialog = new EmployeePropertyPage(activeShell);
		}else if (page.equals(AreaPropertyPage.class)){
			dialog = new AreaPropertyPage(activeShell);
		}else if (page.equals(BasemapPropertyPage.class)){
			dialog = new BasemapPropertyPage(activeShell);
		}else if (page.equals(ProjectionPropertyDialog.class)){
			dialog = new ProjectionPropertyDialog(activeShell);
		}
		if (dialog != null){
			dialog.open();
		}
	}
}

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
package org.wcs.smart.query.common.ui;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.graphics.Image;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.SmartPlugIn;
import org.wcs.smart.query.common.engine.IResultItem;
import org.wcs.smart.query.internal.Messages;
import org.wcs.smart.query.model.IQueryResultInfoProvider;
import org.wcs.smart.util.E3Utils;

/**
 * Results provider designed for map use that finds and selects
 * the given results item in the table.
 * 
 * @author Emily
 *
 */
public class ShowInTableInfoProvider implements IQueryResultInfoProvider {

	@Override
	public String getName() {
		return Messages.ShowInTableInfoProvider_Label;
	}

	@Override
	public boolean supportsCcaa() {
		return false;
	}
	
	/**
	 * Find the query result editor, show the table page and reveal
	 * the given resultItem
	 */
	@Override
	public void doWork(IResultItem resultItem) {
		IEclipseContext ctx = (IEclipseContext) PlatformUI.getWorkbench().getService(IEclipseContext.class);
		EPartService service = ctx.get(EPartService.class);
		for (MPart p : service.getParts()){
			if (p.isVisible() && p.getTags().contains("active")){ //$NON-NLS-1$
				Object src = null;
				if (E3Utils.isCompatibilityEditor(p)){
					src = E3Utils.getSourceObject(p);
				}else{
					src = p.getObject();
				}
				if (src instanceof QueryResultsEditor){
					QueryResultsEditor e = (QueryResultsEditor) src;
					e.showTablePage();
					e.getQueryResultsTable().revealSelection(resultItem);	
				}
			}
		}
	}

	@Override
	public Image getImage() {
		return SmartPlugIn.getDefault().getImageRegistry().get(SmartPlugIn.GOTO_ICON);
	}
	
	/**
	 * true
	 */
	@Override
	public boolean supportsMap(){
		return true;
	}

	/**
	 * false; this item is only meant for map pages
	 */
	@Override
	public boolean supportsTable(){
		return false;
	}
	
}
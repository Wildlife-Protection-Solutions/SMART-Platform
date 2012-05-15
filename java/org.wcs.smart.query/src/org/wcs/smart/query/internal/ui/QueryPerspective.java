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
package org.wcs.smart.query.internal.ui;

import net.refractions.udig.tool.info.internal.InfoView2;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.querylist.QueryListView;
import org.wcs.smart.query.ui.queyfilter.QueryFilterView;

/**
 * Default query perspective.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class QueryPerspective implements IPerspectiveFactory {

	/**
	 * Query Perspective identifier.
	 */
	public static final String ID = "org.wcs.smart.query.SmartQueryPerspective";
	
	/**
	 * @see org.eclipse.ui.IPerspectiveFactory#createInitialLayout(org.eclipse.ui.IPageLayout)
	 */
	@Override
	public void createInitialLayout(IPageLayout layout) {	
		layout.setEditorAreaVisible(true);

		layout.addView(QueryListView.ID, IPageLayout.LEFT, 0.2f, IPageLayout.ID_EDITOR_AREA);
		
		//right side - filters and layer manager
		IFolderLayout folder1 = layout.createFolder("org.wcs.smart.query.queryFolder1", IPageLayout.RIGHT, 0.8f, IPageLayout.ID_EDITOR_AREA);
		folder1.addView(QueryFilterView.ID);
		folder1.addView("net.refractions.udig.project.ui.layerManager");
		
		//bottom query and info view
		IFolderLayout folder2 = layout.createFolder("org.wcs.smart.query.queryFolder2", IPageLayout.BOTTOM, 0.8f, IPageLayout.ID_EDITOR_AREA);
		folder2.addView(QueryDefView.ID);
		folder2.addPlaceholder(InfoView2.VIEW_ID);

		
		
		layout.getViewLayout(QueryDefView.ID).setCloseable(false);
		layout.getViewLayout(QueryFilterView.ID).setCloseable(false);
		layout.getViewLayout(QueryListView.ID).setCloseable(false);
	}
}

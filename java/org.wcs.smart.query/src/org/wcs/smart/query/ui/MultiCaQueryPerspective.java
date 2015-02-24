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
package org.wcs.smart.query.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.itempanel.QueryItemView;
import org.wcs.smart.query.ui.querylist.QueryListView;
import org.wcs.smart.ui.ConservationAreaListView;

/**
 * Cross ca query perspective
 * 
 * @author Emily
 *
 */
public class MultiCaQueryPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.query.persepectives.multi"; //$NON-NLS-1$
	
	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);

		layout.addView(QueryListView.ID, IPageLayout.LEFT, 0.2f, IPageLayout.ID_EDITOR_AREA);
		
		//right side - filters and layer manager
		IFolderLayout folder1 = layout.createFolder("org.wcs.smart.query.queryFolder1", IPageLayout.RIGHT, 0.8f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		folder1.addView(QueryItemView.ID);
		folder1.addView("org.locationtech.udig.project.ui.layerManager"); //$NON-NLS-1$
		
		//bottom query and info view
		IFolderLayout folder2 = layout.createFolder("org.wcs.smart.query.queryFolder2", IPageLayout.BOTTOM, 0.7f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		folder2.addView(QueryDefView.ID);
		folder2.addPlaceholder("org.locationtech.udig.tool.info.infoView"); //$NON-NLS-1$

		layout.addView(ConservationAreaListView.ID, IPageLayout.TOP, .3f, QueryListView.ID);
		
		layout.getViewLayout(QueryDefView.ID).setCloseable(false);
		layout.getViewLayout(QueryItemView.ID).setCloseable(false);
		layout.getViewLayout(QueryListView.ID).setCloseable(false);
		layout.getViewLayout(ConservationAreaListView.ID).setCloseable(false);

	}

}

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
package org.wcs.smart;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.locationtech.udig.catalog.internal.ui.CatalogView;
import org.locationtech.udig.project.ui.internal.LayersView;
import org.wcs.smart.ui.map.MapView;

/**
 * Smart default map perspective
 * @author egouge
 *
 */
public class DefaultPerspective implements IPerspectiveFactory {

	public final static String ID = "org.wcs.smart.DefaultPerspective"; //$NON-NLS-1$
	
	
	@Override
	public void createInitialLayout(IPageLayout layout) {
		
		layout.setEditorAreaVisible(false);
		
		layout.addView(MapView.ID, IPageLayout.LEFT, 0.8f, IPageLayout.ID_EDITOR_AREA);
		IFolderLayout leftFolder = layout.createFolder("org.wcs.smart.DefaultPerspective.leftFolder", IPageLayout.LEFT, 0.2f, MapView.ID); //$NON-NLS-1$
		leftFolder.addView(LayersView.ID); 
		leftFolder.addPlaceholder(CatalogView.VIEW_ID);

		layout.getViewLayout(MapView.ID).setCloseable(false);
	}

}

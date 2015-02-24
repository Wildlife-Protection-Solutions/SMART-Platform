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
package org.wcs.smart.observation.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
/**
 * Field data perspective.  
 * 
 * @author Emily
 *
 */
public class FieldDataPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.observation.FieldDataPerspective"; //$NON-NLS-1$

	/**
	 * ID of placecholder folder for data lists
	 */
	public static final String DATA_ID = "org.wcs.smart.fielddata.datafolder"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		layout.createPlaceholderFolder(DATA_ID, IPageLayout.LEFT, 0.3f, IPageLayout.ID_EDITOR_AREA);

		IFolderLayout folder1 = layout.createFolder("org.wcs.smart.patrol.patrolMapFolder", IPageLayout.BOTTOM, 0.6f, DATA_ID); //$NON-NLS-1$
		folder1.addView("org.locationtech.udig.project.ui.layerManager"); //$NON-NLS-1$
		folder1.addView(WaypointInfoView.ID); 
		folder1.addPlaceholder("org.locationtech.udig.tool.info.infoView"); //$NON-NLS-1$
		
	}
}

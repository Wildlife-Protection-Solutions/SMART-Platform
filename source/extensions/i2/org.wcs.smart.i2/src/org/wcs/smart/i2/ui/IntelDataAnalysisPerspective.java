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
package org.wcs.smart.i2.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.locationtech.udig.project.ui.internal.LayersView;
import org.wcs.smart.i2.IntelSecurityManager;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.QueryView;
import org.wcs.smart.i2.ui.views.RecordNarrativeView;
import org.wcs.smart.i2.ui.views.RecordsView;
import org.wcs.smart.i2.ui.views.WorkingSetView;

/**
 * Data analysis perspective 
 * 
 * @author Emily
 *
 */
public class IntelDataAnalysisPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.i2.IntelDataAnalysisPerspective"; //$NON-NLS-1$

	/**
	 * ID of placecholder folder for data lists
	 */
	public static final String DATA_ID = "org.wcs.smart.fielddata.datafolder"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);

		IFolderLayout rightFolder = layout.createFolder("org.wcs.smart.i2.analysis.right", IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		rightFolder.addView(EntitySearchView.ID);
		layout.getViewLayout(EntitySearchView.ID).setCloseable(false);
		rightFolder.addView(RecordsView.ID);
		layout.getViewLayout(RecordsView.ID).setCloseable(false);
		if (IntelSecurityManager.INSTANCE.canViewQueries()){
			rightFolder.addView(QueryView.ID);
			layout.getViewLayout(QueryView.ID).setCloseable(false);
//			rightFolder.addView(QueryFilterView.ID);
//			layout.getViewLayout(QueryFilterView.ID).setCloseable(false);
		}
		rightFolder.addView(LayersView.ID);
		
		if (IntelSecurityManager.INSTANCE.canViewWorkingSets()){
			IFolderLayout bottomFolder = layout.createFolder("org.wcs.smart.i2.analysis.rightbottom", IPageLayout.BOTTOM, 0.7f, "org.wcs.smart.i2.analysis.right"); //$NON-NLS-1$ //$NON-NLS-2$
			bottomFolder.addView(WorkingSetView.ID);
			bottomFolder.addPlaceholder(RecordNarrativeView.ID);
			layout.getViewLayout(WorkingSetView.ID).setCloseable(false);
		}
	}
}
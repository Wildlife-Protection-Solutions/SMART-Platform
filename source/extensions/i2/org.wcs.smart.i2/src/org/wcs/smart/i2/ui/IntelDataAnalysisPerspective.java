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
import org.wcs.smart.i2.ui.editors.IntelligenceMapEditor;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.QueryView;
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

		IFolderLayout rightFolder = layout.createFolder("org.wcs.smart.i2.analysis.right", IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA);
		rightFolder.addView(EntitySearchView.ID);
		rightFolder.addView(RecordsView.ID);
		rightFolder.addView(QueryView.ID);
		
		IFolderLayout bottomFolder = layout.createFolder("org.wcs.smart.i2.analysis.rightbottom", IPageLayout.BOTTOM, 0.7f, "org.wcs.smart.i2.analysis.right");
		bottomFolder.addView(WorkingSetView.ID);
		
		
		layout.getViewLayout(RecordsView.ID).setCloseable(false);
		layout.getViewLayout(WorkingSetView.ID).setCloseable(false);
		layout.getViewLayout(EntitySearchView.ID).setCloseable(false);
		layout.getViewLayout(QueryView.ID).setCloseable(false);
	}
}
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
import org.wcs.smart.i2.ui.views.RecordNarrativeView;
import org.wcs.smart.i2.ui.views.RecordsView;
import org.wcs.smart.i2.ui.views.WorkingSetView;

/**
 * Data assesment perspective 
 * 
 * @author Emily
 *
 */
public class IntelDataAssessmentPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.i2.IntelDataAssessmentPerspective"; //$NON-NLS-1$


	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		
		layout.addView(RecordsView.ID, IPageLayout.LEFT, 0.2f, IPageLayout.ID_EDITOR_AREA);
		
		if (IntelSecurityManager.INSTANCE.canViewWorkingSets()){
			IFolderLayout bottomLeft = layout.createFolder("org.wcs.smart.i2.assessment.bottomleft", IPageLayout.BOTTOM,0.7f, RecordsView.ID);
			bottomLeft.addView(WorkingSetView.ID);
			layout.getViewLayout(WorkingSetView.ID).setCloseable(false);
		}
		
		IFolderLayout right = layout.createFolder("org.wcs.smart.i2.assessment.right", IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA);
		right.addView(EntitySearchView.ID);
		right.addView(LayersView.ID);
		right.addPlaceholder(RecordNarrativeView.ID);
		layout.getViewLayout(RecordsView.ID).setCloseable(false);
		layout.getViewLayout(EntitySearchView.ID).setCloseable(false);		
	}
}
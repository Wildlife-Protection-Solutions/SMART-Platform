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
package org.wcs.smart.plan.ui.perspective;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

/**
 * The planning perspective.
 * 
 * @author jeff
 *
 */
public class PlanPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.plan.planPerspective"; //$NON-NLS-1$
	
	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		layout.addView(PlanListView.ID, IPageLayout.LEFT, 0.25f, IPageLayout.ID_EDITOR_AREA);
		layout.getViewLayout(PlanListView.ID).setCloseable(false);

		IFolderLayout folder1 = layout.createFolder("org.wcs.smart.plan.planFolder", IPageLayout.BOTTOM, 0.6f, PlanListView.ID); //$NON-NLS-1$
		folder1.addView("org.locationtech.udig.project.ui.layerManager"); //$NON-NLS-1$
		folder1.addPlaceholder("org.locationtech.udig.tool.info.infoView"); //$NON-NLS-1$
	}

}
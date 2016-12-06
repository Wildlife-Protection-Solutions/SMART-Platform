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
package org.wcs.smart.report.internal.ui.designer;

import org.eclipse.birt.report.designer.ui.lib.explorer.LibraryExplorerView;
import org.eclipse.birt.report.designer.ui.views.attributes.AttributeView;
import org.eclipse.birt.report.designer.ui.views.data.DataView;
import org.eclipse.gef.ui.views.palette.PaletteView;
import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.wcs.smart.report.internal.ui.ReportListView;

/**
 * 
 * Smart Report Designer Perspective
 * 
 * @author egouge
 * @since 1.0.0
 */
public class SmartReportPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.report.ReportPerspective"; //$NON-NLS-1$

	public static final String NEW_REPORT_ID = "org.eclipse.birt.report.designer.ui.ide.wizards.NewReportWizard";//$NON-NLS-1$

	public static final String NEW_TEMPLATE_ID = "org.eclipse.birt.report.designer.ui.ide.wizards.NewTemplateWizard";//$NON-NLS-1$

	/**
	 * Constructs a new Default layout engine.
	 */

	public SmartReportPerspective() {
		super();
	}

	/**
	 * Defines the initial layout for a perspective.
	 * 
	 * Implementors of this method may add additional views to a perspective.
	 * The perspective already contains an editor folder with
	 * <code>ID = ILayoutFactory.ID_EDITORS</code>. Add additional views to the
	 * perspective in reference to the editor folder.
	 * 
	 * This method is only called when a new perspective is created. If an old
	 * perspective is restored from a persistence file then this method is not
	 * called.
	 * 
	 * @param layout
	 *            the factory used to add views to the perspective
	 */
	public void createInitialLayout(IPageLayout layout) {
		defineLayout(layout);
	}

	/**
	 * Defines the initial layout for a page.
	 */
	private void defineLayout(IPageLayout layout) {
		// Editors are placed for free.
		String editorArea = layout.getEditorArea();

		// Top left.
		IFolderLayout topLeft = layout.createFolder(
				"topLeft", IPageLayout.LEFT, (float) 0.26, editorArea);//$NON-NLS-1$
		topLeft.addView(PaletteView.ID);
		topLeft.addView(DataView.ID);
		topLeft.addView(LibraryExplorerView.ID);

		// Bottom left.
		IFolderLayout bottomLeft = layout.createFolder(
				"bottomLeft", IPageLayout.BOTTOM, (float) 0.50,//$NON-NLS-1$
				"topLeft");//$NON-NLS-1$
		bottomLeft.addView(IPageLayout.ID_OUTLINE);
		bottomLeft.addView(ReportListView.ID);

		// Bottom right.
		IFolderLayout bootomRight = layout.createFolder(
				"bootomRight", IPageLayout.BOTTOM, (float) 0.66, editorArea);//$NON-NLS-1$
		bootomRight.addView(AttributeView.ID);
	}
}
package org.wcs.smart.i2.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.IntelligenceMapView;
import org.wcs.smart.i2.ui.views.QueryView;
import org.wcs.smart.i2.ui.views.RecordsView;
import org.wcs.smart.i2.ui.views.WorkingSetView;


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
		
		if (layout instanceof org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout) {
			org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout layout4 = (org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout) layout;
			layout4.stackView(IntelligenceMapView.ID, layout.getEditorArea(), true);
			layout.getViewLayout(IntelligenceMapView.ID).setCloseable(false);
		}
		
		layout.getViewLayout(RecordsView.ID).setCloseable(false);
		layout.getViewLayout(WorkingSetView.ID).setCloseable(false);
		layout.getViewLayout(EntitySearchView.ID).setCloseable(false);
		layout.getViewLayout(QueryView.ID).setCloseable(false);
	}
}
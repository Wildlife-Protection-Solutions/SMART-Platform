package org.wcs.smart.i2.ui;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.wcs.smart.i2.Intelligence2PlugIn;
import org.wcs.smart.i2.ui.views.EntitySearchView;
import org.wcs.smart.i2.ui.views.IntelligenceMapView;
import org.wcs.smart.i2.ui.views.QueryView;
import org.wcs.smart.i2.ui.views.RecordsView;
import org.wcs.smart.i2.ui.views.WorkingSetView;
import org.wcs.smart.ui.map.MapView;
import org.wcs.smart.ui.map.SmartMapEditorPart;


public class IntelDataAssessmentPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.i2.IntelDataAssesmentPerspective"; //$NON-NLS-1$


	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		
		layout.addView(RecordsView.ID, IPageLayout.LEFT, 0.2f, IPageLayout.ID_EDITOR_AREA);
		layout.addView(WorkingSetView.ID, IPageLayout.BOTTOM, 0.7f, RecordsView.ID);
		layout.addView(EntitySearchView.ID, IPageLayout.RIGHT, 0.7f, IPageLayout.ID_EDITOR_AREA);
		
		layout.getViewLayout(RecordsView.ID).setCloseable(false);
		layout.getViewLayout(WorkingSetView.ID).setCloseable(false);
		layout.getViewLayout(EntitySearchView.ID).setCloseable(false);
		
		if (layout instanceof org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout) {
			org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout layout4 = (org.eclipse.ui.internal.e4.compatibility.ModeledPageLayout) layout;
			
			layout4.stackView(IntelligenceMapView.ID, layout.getEditorArea(), true);
			layout.getViewLayout(IntelligenceMapView.ID).setCloseable(false);
		}
	}
}
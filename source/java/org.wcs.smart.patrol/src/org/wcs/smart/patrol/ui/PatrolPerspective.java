package org.wcs.smart.patrol.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;

public class PatrolPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.patrol.PatrolPerspective";
	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);
		
		layout.addView(PatrolListView.ID, IPageLayout.LEFT, 0.3f, IPageLayout.ID_EDITOR_AREA);

		
		layout.getViewLayout(PatrolListView.ID).setCloseable(false);

	}

}

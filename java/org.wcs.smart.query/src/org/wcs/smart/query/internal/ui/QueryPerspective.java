package org.wcs.smart.query.internal.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.wcs.smart.query.ui.QueryDefView;

public class QueryPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.query.SmartQueryPerspective";
	@Override
	public void createInitialLayout(IPageLayout layout) {
		
		layout.setEditorAreaVisible(true);
		
		layout.addView(QueryDefView.ID, IPageLayout.BOTTOM, 0.8f, IPageLayout.ID_EDITOR_AREA);
		layout.getViewLayout(QueryDefView.ID).setCloseable(false);
		

	}

}

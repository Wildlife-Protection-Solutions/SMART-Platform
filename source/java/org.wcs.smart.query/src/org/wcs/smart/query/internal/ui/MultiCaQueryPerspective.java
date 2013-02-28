package org.wcs.smart.query.internal.ui;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;
import org.wcs.smart.query.ui.definition.QueryDefView;
import org.wcs.smart.query.ui.multi.ConservationAreaListView;
import org.wcs.smart.query.ui.queryfilter.QueryFilterView;
import org.wcs.smart.query.ui.querylist.QueryListView;

public class MultiCaQueryPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.query.SmartMultiCaQueryPerspective"; //$NON-NLS-1$
	
	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);

		layout.addView(QueryListView.ID, IPageLayout.LEFT, 0.2f, IPageLayout.ID_EDITOR_AREA);
		
		//right side - filters and layer manager
		IFolderLayout folder1 = layout.createFolder("org.wcs.smart.query.queryFolder1", IPageLayout.RIGHT, 0.8f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		folder1.addView(QueryFilterView.ID);
		folder1.addView("net.refractions.udig.project.ui.layerManager"); //$NON-NLS-1$
		
		//bottom query and info view
		IFolderLayout folder2 = layout.createFolder("org.wcs.smart.query.queryFolder2", IPageLayout.BOTTOM, 0.7f, IPageLayout.ID_EDITOR_AREA); //$NON-NLS-1$
		folder2.addView(QueryDefView.ID);
		folder2.addPlaceholder("net.refractions.udig.tool.info.infoView"); //$NON-NLS-1$

		layout.addView(ConservationAreaListView.ID, IPageLayout.TOP, .3f, QueryListView.ID); //$NON-NLS-1$
		
		layout.getViewLayout(QueryDefView.ID).setCloseable(false);
		layout.getViewLayout(QueryFilterView.ID).setCloseable(false);
		layout.getViewLayout(QueryListView.ID).setCloseable(false);
		layout.getViewLayout(ConservationAreaListView.ID).setCloseable(false);
		
	}

}

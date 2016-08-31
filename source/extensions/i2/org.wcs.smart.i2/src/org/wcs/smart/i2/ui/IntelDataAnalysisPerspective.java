package org.wcs.smart.i2.ui;

import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IPerspectiveFactory;


public class IntelDataAnalysisPerspective implements IPerspectiveFactory {

	public static final String ID = "org.wcs.smart.i2.IntelDataAnalysisPerspective"; //$NON-NLS-1$

	/**
	 * ID of placecholder folder for data lists
	 */
	public static final String DATA_ID = "org.wcs.smart.fielddata.datafolder"; //$NON-NLS-1$

	@Override
	public void createInitialLayout(IPageLayout layout) {
		layout.setEditorAreaVisible(true);

	}
}
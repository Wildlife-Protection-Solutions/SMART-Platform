package org.wcs.smart.paws.ui;

import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.IPartListener;
import org.wcs.smart.paws.ui.config.ConfigurationEditor;
import org.wcs.smart.paws.ui.run.RunEditor;
import org.wcs.smart.query.ui.QueryPerspective;
import org.wcs.smart.util.E3Utils;

/**
 * show & hide definition and item areas when paws editor (configuration or
 * results) as these are not part of the PAWS process
 */
public class HidePartsPartListener implements IPartListener {

	private static HidePartsPartListener instance = null;
	
	public static synchronized HidePartsPartListener getInstance(){
		if (instance == null) instance = new HidePartsPartListener();
		return instance;
	}
	private HidePartsPartListener() {
	
	}
	
	@Override
	public void partVisible(MPart part) {
		Object lpart = E3Utils.getSourceObject(part);
		if (lpart instanceof ConfigurationEditor || lpart instanceof RunEditor) {
			// hide definition and list area
			MUIElement element = part.getContext().get(EModelService.class).find(QueryPerspective.DEF_FOLDER, part.getContext().get(MApplication.class));
			element.getTags().add(IPresentationEngine.MINIMIZED);
			element = part.getContext().get(EModelService.class).find(QueryPerspective.ITEM_FOLDER, part.getContext().get(MApplication.class));
			element.getTags().add(IPresentationEngine.MINIMIZED);
		}
	}

	@Override
	public void partHidden(MPart part) {
		Object lpart = E3Utils.getSourceObject(part);
		if (lpart instanceof ConfigurationEditor || lpart instanceof RunEditor) {
			// show definition and list area
			MUIElement element = part.getContext().get(EModelService.class).find(QueryPerspective.DEF_FOLDER, part.getContext().get(MApplication.class));
			element.getTags().remove(IPresentationEngine.MINIMIZED);
			element = part.getContext().get(EModelService.class).find(QueryPerspective.ITEM_FOLDER, part.getContext().get(MApplication.class));
			element.getTags().remove(IPresentationEngine.MINIMIZED);
		}
	}

	@Override
	public void partDeactivated(MPart part) {
	}

	@Override
	public void partBroughtToTop(MPart part) {
	}

	@Override
	public void partActivated(MPart part) {

	}
}

/*
 * Copyright (C) 2019 Wildlife Conservation Society
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
 * The PAWS tools are integrated into the Query perspective, but do not 
 * use the definition or items area.  This listener hides those parts when a
 * PAWS editor (configuration or results) is activated.  This needs to be 
 * added to each applicable PAWS editor part.
 * 
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

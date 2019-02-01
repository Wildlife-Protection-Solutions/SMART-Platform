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
package org.wcs.smart.datagenerator.ui;

import javax.inject.Named;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.tools.compat.parts.DIHandler;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.MElementContainer;
import org.eclipse.e4.ui.model.application.ui.MUIElement;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.model.application.ui.advanced.MPlaceholder;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.swt.widgets.Shell;
import org.wcs.smart.PerspectiveEditorTracker;
import org.wcs.smart.observation.ui.FieldDataPerspective;
import org.wcs.smart.observation.ui.ShowFieldDataPerspective;

/**
 * Handler for showing data generator view
 * @author Emily
 *
 */
@SuppressWarnings("restriction")
public class DataGeneratorHandler {
	@SuppressWarnings("unchecked")
	@Execute
	public void execute(@Named(IServiceConstants.ACTIVE_SHELL) Shell activeShell,
			EPartService ePartService, EModelService mService, MApplication app, MWindow activeWindow) {
		
		(new ShowFieldDataPerspective()).execute(FieldDataPerspective.ID, activeWindow);

		MPart part = ePartService.findPart(DataGeneratorView.ID);
		if (part == null){
			part = ePartService.showPart(DataGeneratorView.ID, PartState.VISIBLE);
			part.getTags().add(PerspectiveEditorTracker.EDITOR_TAG);
			part.getTags().add(FieldDataPerspective.ID);
//			mPart.getTags().add(EPartService.REMOVE_ON_HIDE_TAG);
			
			
			//move to editor area if applicable
			MPerspective current = mService.getActivePerspective(app.getSelectedElement());
			MUIElement element = mService.find("org.eclipse.ui.editorss", current); //$NON-NLS-1$
			if (element.isToBeRendered()){
				mService.move(part, (MElementContainer<MUIElement>)((MArea)((MPlaceholder)element).getRef()).getChildren().get(0), 0);
				ePartService.showPart(part, PartState.ACTIVATE);
			}
		}else{
			ePartService.activate(part);
		}
		
	}
	
	public static class DataGeneratorHandlerWrapper extends DIHandler<DataGeneratorHandler>{
		public DataGeneratorHandlerWrapper() {
			super(DataGeneratorHandler.class);
		}
		
	}
}

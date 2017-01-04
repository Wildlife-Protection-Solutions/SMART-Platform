/*
 * Copyright (C) 2016 Wildlife Conservation Society
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
package org.wcs.smart.i2.ui.handler;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.basic.MBasicFactory;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.wcs.smart.i2.model.IntelAttachment;
import org.wcs.smart.i2.ui.views.AttachmentView;

/**
 * Opens a new view that displays the given attachment.
 * 
 * @author Emily
 *
 */
public class OpenAttachmentViewHandler {

	public void execute(IntelAttachment attachment, IEclipseContext context){
		IEclipseContext kid = context.createChild();
		kid.set(IntelAttachment.class, attachment);

		EPartService pService = context.get(EPartService.class);
		EModelService mService = context.get(EModelService.class);
		
		MPart viewPart = MBasicFactory.INSTANCE.createInputPart();
		viewPart.setContributionURI("bundleclass://org.wcs.smart.i2/org.wcs.smart.i2.ui.views.AttachmentView"); //$NON-NLS-1$
		viewPart.setCloseable(true);
		viewPart.setContext(kid);
		viewPart.setLabel(attachment.getFilename());
		viewPart.setElementId(AttachmentView.ID);
		
		List<MArea> areas2 = mService.findElements(context.get(MApplication.class),
				"org.eclipse.ui.editorss", MArea.class, null); //$NON-NLS-1$
		
		if (areas2.size() > 0){
			MArea editorArea = areas2.get(0);
			List<MPartSashContainerElement> kids = new ArrayList<>();
			kids.addAll(editorArea.getChildren());
			while(!kids.isEmpty()){
				MPartSashContainerElement e = kids.remove(0);
				if (e instanceof MPartStack ){
					((MPartStack )e).getChildren().add(viewPart);
					break;
				}
			}
		}
			
		pService.showPart(viewPart, PartState.VISIBLE);
		
	}
}

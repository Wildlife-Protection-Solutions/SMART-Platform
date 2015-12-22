/*
 * Copyright (C) 2012 Wildlife Conservation Society
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
package org.wcs.smart;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MArea;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainer;
import org.eclipse.e4.ui.model.application.ui.basic.MPartSashContainerElement;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.IPresentationEngine;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * Tracks part events; when a part is opened it adds the 
 * current perspective to the tags.  This allows to track
 * what parts were opened in what perspective.
 * 
 * @author Emily
 * @since 1.0.0
 */
public class PerspectiveEditorTracker implements EventHandler {

	private static final String PID_KEY = "smart.perspectiveid"; //$NON-NLS-1$
	
	@Inject private EModelService mService;

	@Inject private MApplication app;

	private MArea editorArea = null;

	@Inject
	public void appStart(@Optional @UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) Event event, IEclipseContext context) {
		if (event == null) return;
//		//find editor stack area
		List<MArea> areas2 = mService.findElements(app, "org.eclipse.ui.editorss", MArea.class,null); //$NON-NLS-1$
		if (areas2.size() > 0){
			editorArea = areas2.get(0);
			editorArea.getTags().add(IPresentationEngine.NO_AUTO_COLLAPSE);
		}
	}
	
	@Inject
	public void handleEvent(@Optional @UIEventTopic(UIEvents.UIElement.TOPIC_WIDGET) Event event) {
		if (event == null) return;
		
		Object x = event.getProperty(UIEvents.EventTags.ELEMENT);
		if (x instanceof MPart){
			if (!((MPart) x).getElementId().equals("org.eclipse.e4.ui.compatibility.editor")){ //$NON-NLS-1$
				return;
			}
			MWindow window = ((MPart)x).getContext().get(MWindow.class);
			if (window == null) return;

			String id = mService.getActivePerspective(window).getElementId();
			Object y = event.getProperty(UIEvents.EventTags.WIDGET);
			if (y != null){
				if (!((MPart)x).getTags().contains(PID_KEY)){
					((MPart) x).getTags().add(PID_KEY);
					((MPart) x).getTags().add(id);
				}
			}
		}
	}
	
	
	/**
	 * Gets the last active part in the editor stack
	 * @return
	 */
	public MStackElement getActivePart(){
		if (editorArea == null) return null;
		
		MPartSashContainerElement element = editorArea.getChildren().get(0);
		MStackElement lastPart = null;
		if (element instanceof MPartSashContainer){
			element = ((MPartSashContainer)element).getSelectedElement();
		}
		if (element instanceof MPartStack){
			lastPart = ((MPartStack)element).getSelectedElement();
		}
		
		if (lastPart != null && lastPart.isVisible()){
			return lastPart;
		}
		return null;
	}
	
	public void selectStackElement(MPart activate){
		if (editorArea == null) return;
		MPartStack pstack = null;
		MPartSashContainerElement element = editorArea.getChildren().get(0);
		
		int cnt = 0;
		while(element != null && element instanceof MPartSashContainer && cnt < 15){
			element = ((MPartSashContainer)element).getSelectedElement();
			cnt++;
		}
		if (element instanceof MPartStack){
			pstack = (MPartStack)element;
		}
		if (pstack != null && !(pstack.getTags().contains(IPresentationEngine.NO_AUTO_COLLAPSE))){
			pstack.getTags().add(IPresentationEngine.NO_AUTO_COLLAPSE);
		}
		
		pstack.setSelectedElement(null);
		if (activate != null && activate.getWidget() == null) return;
		pstack.setSelectedElement(activate);
	}

}

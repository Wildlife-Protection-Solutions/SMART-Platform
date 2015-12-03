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
package org.wcs.smart.connect.ui;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimBar;
import org.eclipse.e4.ui.model.application.ui.basic.MTrimElement;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

/**
 * E4 model process to move the status control
 * to the lower right after the display has been 
 * created.
 * 
 * @author Emily
 *
 */
public class StatusLineConfigProcessor {

	@Inject EModelService modelService;
	@Inject MApplication app;
	@Inject IEventBroker broker;
	
	@Execute
	public void execute(){
		
		broker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE,
		        new EventHandler() {
					
					@Override
					public void handleEvent(Event event) {
						try{
							configure();
						}finally{
							broker.unsubscribe(this);
						}
					}
		});
	}
	
	private void configure(){
		MTrimBar statusBar = (MTrimBar) modelService.find("org.eclipse.ui.trim.status", app); //$NON-NLS-1$
		MTrimElement toMove = null;
		for (MTrimElement trim : statusBar.getChildren()){
			if (trim.getElementId().equals("org.wcs.smart.connect.status") && trim.isVisible()){ //$NON-NLS-1$
				toMove = trim;
			}
		}
		if (toMove != null){
			statusBar.getChildren().remove(toMove);
			statusBar.getChildren().add(toMove);
		}
	}
}

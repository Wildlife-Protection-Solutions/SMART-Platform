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
package org.wcs.smart.connect.server.replication;

import javax.inject.Inject;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.hibernate.Session;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.wcs.smart.connect.ConnectHibernateManager;
import org.wcs.smart.connect.model.ConnectServer;
import org.wcs.smart.connect.model.ConnectServerOption;
import org.wcs.smart.connect.ui.server.SyncChangeLogHandler;
import org.wcs.smart.hibernate.HibernateManager;

/**
 * On startup sync processor.  Waits until the application
 * workbench has been created, then prompts the user to sync 
 * if necessary.
 * '
 * @author Emily
 *
 */
public class SyncOnStartupProcessor {

	@Inject IEventBroker broker;
	
	public SyncOnStartupProcessor() {
	}
	
	@Execute
	public void execute(){

		broker.subscribe(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE,
		        new EventHandler() {
					
					@Override
					public void handleEvent(Event event) {
						broker.unsubscribe(this);
						
						ConnectServer cs = null;
						Session s = HibernateManager.openSession();
						try{
							cs = ConnectHibernateManager.getConnectServer(s);
						}finally{
							s.close();
						}
						if (cs == null) return;
						
						if (cs.getOptionAsBoolean(ConnectServerOption.Option.SYNC_ON_STARTUP)){
							if (MessageDialog.openQuestion(Display.getDefault().getActiveShell(), 
									"SMART Connect - Sync Changes", 
									"Do you want to sync all changes with SMART Connect now?")){
								//initiate sync now
								System.out.println("sync now");
								(new SyncChangeLogHandler()).execute(Display.getDefault().getActiveShell());
							}
						}		
					}
				});
	}

}

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

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.menus.WorkbenchWindowControlContribution;
import org.wcs.smart.connect.ConnectPlugIn;
import org.wcs.smart.connect.internal.Messages;

/**
 * Status control that is displayed in the status bar
 * that informs the user of the status of the replication 
 * 
 * @author Emily
 *
 */
public class StatusLineControl extends WorkbenchWindowControlContribution {
	
	private IConnectStatusContribution[] contribs;

	public StatusLineControl() {	
	}

	@Override
	protected Control createControl(Composite parent) {
		contribs = getStatusContributions();
		
		Composite main = new Composite(parent, SWT.NONE);
		GridLayout gl = new GridLayout(contribs.length, false);
		gl.marginWidth = 0;
		gl.marginHeight = 0;
		main.setLayout(gl);
		
		//refresh now menu
		Menu refreshMenu = new Menu(parent.getShell(), SWT.POP_UP);
		MenuItem refreshNow = new MenuItem(refreshMenu, SWT.PUSH);
		refreshNow.setText(Messages.StatusLineControl_RefreshNowMneuItem);
		refreshNow.setImage(ConnectPlugIn.getDefault().getImageRegistry()
				.get(ConnectPlugIn.REFRESH_ICON));
		refreshNow.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				refreshNow();
			}
		});

		main.setMenu(refreshMenu);
				
		for (IConnectStatusContribution c : contribs){
			Control ctr = c.createControl(main);
			ctr.setMenu(refreshMenu);
		}
		
		
//		serverStatus.setMenu(refreshMenu);
//		localStatus.setMenu(refreshMenu);
		
		return main;
	}

	private void refreshNow(){
		for (IConnectStatusContribution c : contribs){
			c.refresh();
		}
		
	}
	
	private IConnectStatusContribution[] getStatusContributions(){	
		if (Platform.getExtensionRegistry() == null) return new IConnectStatusContribution[0];
		ArrayList<IConnectStatusContribution> items = new ArrayList<IConnectStatusContribution>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IConnectStatusContribution.EXTENSION_ID);
		for (IConfigurationElement e : config) {
			try{
				items.add((IConnectStatusContribution)e.createExecutableExtension("class")); //$NON-NLS-1$
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
		}
		return items.toArray(new IConnectStatusContribution[items.size()]);
	}
}

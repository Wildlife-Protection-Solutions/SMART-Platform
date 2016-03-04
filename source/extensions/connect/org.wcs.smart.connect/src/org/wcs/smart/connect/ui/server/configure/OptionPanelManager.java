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
package org.wcs.smart.connect.ui.server.configure;

import java.util.ArrayList;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.wcs.smart.ca.ConservationArea;
import org.wcs.smart.connect.ConnectPlugIn;

/**
 * Manager for option panels
 * @author Emily
 *
 */
public class OptionPanelManager {

	public synchronized static IServerOptionsPanel[] createOptionPanels(ConservationArea ca){
		if (Platform.getExtensionRegistry() == null) return new IServerOptionsPanel[0];
		ArrayList<IServerOptionsPanel> items = new ArrayList<IServerOptionsPanel>();
		IConfigurationElement[] config = Platform.getExtensionRegistry().getConfigurationElementsFor(IServerOptionsPanel.EXTENSION_ID);
		for (IConfigurationElement e : config) {
			try{ 
				IServerOptionsPanel pnl = (IServerOptionsPanel)e.createExecutableExtension("class"); //$NON-NLS-1$
				if (pnl.isSupported(ca)){
					items.add(pnl); 
				}
			}catch (Exception ex){
				ConnectPlugIn.log(ex.getMessage(), ex);
			}
		}
		return items.toArray(new IServerOptionsPanel[items.size()]);
	}
	
}

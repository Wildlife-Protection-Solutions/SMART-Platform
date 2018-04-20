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
package org.wcs.smart.connect.internal;

import java.util.List;

import org.hibernate.Session;
import org.wcs.smart.connect.model.ConnectServerStatus;
import org.wcs.smart.hibernate.HibernateManager;
import org.wcs.smart.p2.IPluginAdvisor;

/**
 * Disabling uninstalling plugins when connect is installed
 * 
 * @author Emily
 *
 */
public class PluginAdvisor implements IPluginAdvisor {

	public PluginAdvisor() {		
	}

	@Override
	public String canUninstall() {
		try(Session session = HibernateManager.openSession()){
			//if there is any conservation area with a active connect instance then we cannot uninstall plugins
			//if connect is installed but no plugins are replicating to connect then it's ok to uninstall
			String sql = "FROM ConnectServerStatus WHERE status in (:status) "; //$NON-NLS-1$
			
			List<ConnectServerStatus> cas = session.createQuery(sql, ConnectServerStatus.class)
					.setParameterList("status", new ConnectServerStatus.Status[] {ConnectServerStatus.Status.DONE, ConnectServerStatus.Status.UPLOAD}) //$NON-NLS-1$
					.list();
			if (cas.size() == 0) return null;
			
			
			StringBuilder sb = new StringBuilder();
			sb.append(Messages.PluginAdvisor_CannotInstallPlugins);
			for (ConnectServerStatus c : cas) {
				sb.append(c.getServer().getConservationArea().getNameLabel());
				sb.append(", "); //$NON-NLS-1$
			}
			sb.deleteCharAt(sb.length() - 1);
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
	}

}
